package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.MushafAyah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class MushafFetchResult(
    val ayahs: List<MushafAyah>,
    val surahAyahsMap: Map<Int, List<MushafAyah>>,
    val usedOfflineFallback: Boolean
)

data class MushafReaderResult(
    val surahInfo: com.example.model.SurahInfo,
    val ayahs: List<MushafAyah>,
    val usedOfflineFallback: Boolean
)

class QuranMushafRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheDir: File by lazy {
        File(context.applicationContext.filesDir, "quran_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun loadScopeVerses(
        juzNumbers: Set<Int>,
        specificSurahNumber: Int?
    ): MushafFetchResult = withContext(Dispatchers.IO) {
        val targetJuz = juzNumbers.filter { it in 1..30 }.ifEmpty { listOf(30) }
        val loadedAyahs = mutableListOf<MushafAyah>()
        val surahMap = mutableMapOf<Int, List<MushafAyah>>()
        var anyOfflineFallback = false

        coroutineScope {
            val juzDeferred = targetJuz.map { juz ->
                async {
                    val fetched = fetchJuzAyahs(juz)
                    if (fetched != null) {
                        fetched to false
                    } else {
                        fallbackAyahsForJuz(juz) to true
                    }
                }
            }

            val surahNumbersToFetch = if (specificSurahNumber != null) {
                listOf(specificSurahNumber)
            } else {
                // Prioritize Surahs with >= 5 ayahs so both "أول السورة" and "آخر 5 آيات" always succeed
                val scopeSurahs = QuranCatalog.surahsForJuz(targetJuz.toSet())
                val longer = scopeSurahs.filter { it.ayahCount >= 5 }.shuffled()
                val shorter = scopeSurahs.filter { it.ayahCount < 5 }.shuffled()
                (longer + shorter).take(6).map { it.number }
            }

            val surahDeferred = surahNumbersToFetch.map { surahNum ->
                async {
                    val fetched = fetchSurahAyahs(surahNum)
                    surahNum to fetched
                }
            }

            juzDeferred.awaitAll().forEach { (list, usedFallback) ->
                loadedAyahs.addAll(list)
                if (usedFallback) anyOfflineFallback = true
            }

            surahDeferred.awaitAll().forEach { (surahNum, list) ->
                if (!list.isNullOrEmpty()) {
                    surahMap[surahNum] = list
                } else {
                    val fallback = fallbackSurahAyahs(surahNum)
                    if (fallback.isNotEmpty()) {
                        surahMap[surahNum] = fallback
                    }
                }
            }
        }

        // Also group loaded juz ayahs by surah if full surah wasn't separately fetched
        val groupedFromJuz = loadedAyahs.groupBy { it.surahNumber }
        groupedFromJuz.forEach { (surahNum, list) ->
            if (!surahMap.containsKey(surahNum)) {
                val expectedCount = QuranCatalog.allSurahs.firstOrNull { it.number == surahNum }?.ayahCount ?: 0
                if (list.size >= expectedCount && expectedCount > 0) {
                    surahMap[surahNum] = list.sortedBy { it.numberInSurah }
                }
            }
        }

        // Guarantee at least one Surah boundary slice if offline fallback left surahMap sparse
        if (surahMap.isEmpty() && specificSurahNumber == null) {
            fallbackBySurah.forEach { (sNum, list) ->
                if (list.any { it.juzNumber in targetJuz }) {
                    surahMap[sNum] = list
                }
            }
            if (surahMap.isEmpty()) {
                surahMap[1] = fallbackBySurah[1].orEmpty()
                surahMap[112] = fallbackBySurah[112].orEmpty()
                surahMap[114] = fallbackBySurah[114].orEmpty()
            }
        }

        MushafFetchResult(
            ayahs = loadedAyahs.distinctBy { it.surahNumber to it.numberInSurah },
            surahAyahsMap = surahMap,
            usedOfflineFallback = anyOfflineFallback
        )
    }

    suspend fun loadSurahForReader(surahNumber: Int): MushafReaderResult = withContext(Dispatchers.IO) {
        val safeNumber = surahNumber.coerceIn(1, 114)
        val surahInfo = QuranCatalog.allSurahs.firstOrNull { it.number == safeNumber }
            ?: QuranCatalog.allSurahs.first()
        val fetched = fetchSurahAyahs(safeNumber)
        if (!fetched.isNullOrEmpty()) {
            MushafReaderResult(
                surahInfo = surahInfo,
                ayahs = fetched,
                usedOfflineFallback = false
            )
        } else {
            val fallback = fallbackSurahAyahs(safeNumber)
            MushafReaderResult(
                surahInfo = surahInfo,
                ayahs = fallback,
                usedOfflineFallback = true
            )
        }
    }

    private fun fetchJuzAyahs(juzNumber: Int): List<MushafAyah>? {
        val cacheFile = File(cacheDir, "juz_${juzNumber}.json")
        val jsonStr = readCacheOrFetch(
            cacheFile = cacheFile,
            url = "https://api.alquran.cloud/v1/juz/$juzNumber/quran-uthmani"
        ) ?: return null

        return parseAyahsResponse(jsonStr, defaultJuz = juzNumber)
    }

    private fun fetchSurahAyahs(surahNumber: Int): List<MushafAyah>? {
        val cacheFile = File(cacheDir, "surah_${surahNumber}.json")
        val jsonStr = readCacheOrFetch(
            cacheFile = cacheFile,
            url = "https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani"
        ) ?: return null

        return parseSurahResponse(jsonStr)
    }

    private fun readCacheOrFetch(cacheFile: File, url: String): String? {
        if (cacheFile.exists() && cacheFile.length() > 100L) {
            return runCatching { cacheFile.readText(Charsets.UTF_8) }.getOrNull()
        }
        return try {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                if (json.optInt("code", 0) != 200) return null
                runCatching { cacheFile.writeText(body, Charsets.UTF_8) }
                body
            }
        } catch (e: Exception) {
            Log.w(TAG, "Mushaf API fetch failed for $url: ${e.message}")
            null
        }
    }

    private fun parseAyahsResponse(jsonStr: String, defaultJuz: Int): List<MushafAyah>? {
        return try {
            val root = JSONObject(jsonStr)
            val data = root.getJSONObject("data")
            val ayahsArray = data.getJSONArray("ayahs")
            val list = ArrayList<MushafAyah>(ayahsArray.length())
            for (i in 0 until ayahsArray.length()) {
                val obj = ayahsArray.getJSONObject(i)
                val surahObj = obj.getJSONObject("surah")
                val surahNum = surahObj.getInt("number")
                val rawName = surahObj.getString("name").removePrefix("سُورَةُ ").removePrefix("سورة ").trim()
                val text = cleanAyahText(obj.getString("text"), surahNum, obj.getInt("numberInSurah"))
                list.add(
                    MushafAyah(
                        surahNumber = surahNum,
                        surahNameAr = QuranCatalog.allSurahs.firstOrNull { it.number == surahNum }?.nameAr
                            ?: rawName.ifBlank { "السورة $surahNum" },
                        numberInSurah = obj.getInt("numberInSurah"),
                        juzNumber = obj.optInt("juz", defaultJuz),
                        textUthmani = text
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing juz JSON", e)
            null
        }
    }

    private fun parseSurahResponse(jsonStr: String): List<MushafAyah>? {
        return try {
            val root = JSONObject(jsonStr)
            val data = root.getJSONObject("data")
            val surahNum = data.getInt("number")
            val rawName = data.getString("name").removePrefix("سُورَةُ ").removePrefix("سورة ").trim()
            val cleanName = QuranCatalog.allSurahs.firstOrNull { it.number == surahNum }?.nameAr ?: rawName
            val ayahsArray = data.getJSONArray("ayahs")
            val list = ArrayList<MushafAyah>(ayahsArray.length())
            for (i in 0 until ayahsArray.length()) {
                val obj = ayahsArray.getJSONObject(i)
                val numInSurah = obj.getInt("numberInSurah")
                list.add(
                    MushafAyah(
                        surahNumber = surahNum,
                        surahNameAr = cleanName,
                        numberInSurah = numInSurah,
                        juzNumber = obj.optInt("juz", 1),
                        textUthmani = cleanAyahText(obj.getString("text"), surahNum, numInSurah)
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing surah JSON", e)
            null
        }
    }

    private fun cleanAyahText(raw: String, surahNumber: Int, numberInSurah: Int): String {
        val trimmed = raw.trim()
        return if (numberInSurah == 1 && surahNumber != 1 && surahNumber != 9) {
            ArabicQuranNormalizer.stripBasmalaIfPrefix(trimmed)
        } else {
            trimmed
        }
    }

    private fun fallbackAyahsForJuz(juz: Int): List<MushafAyah> =
        fallbackByJuz[juz]
            ?: when {
                juz in 1..4 -> fallbackByJuz[1]
                juz in 15..20 -> fallbackByJuz[15]
                juz in 21..29 -> fallbackByJuz[29]
                else -> fallbackByJuz[30]
            }.orEmpty()

    private fun fallbackSurahAyahs(surahNumber: Int): List<MushafAyah> =
        fallbackBySurah[surahNumber].orEmpty()

    private companion object {
        const val TAG = "QuranMushafRepo"

        val fallbackBySurah: Map<Int, List<MushafAyah>> = mapOf(
            1 to listOf(
                MushafAyah(1, "الفاتحة", 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"),
                MushafAyah(1, "الفاتحة", 2, 1, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ"),
                MushafAyah(1, "الفاتحة", 3, 1, "الرَّحْمَٰنِ الرَّحِيمِ"),
                MushafAyah(1, "الفاتحة", 4, 1, "مَالِكِ يَوْمِ الدِّينِ"),
                MushafAyah(1, "الفاتحة", 5, 1, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ"),
                MushafAyah(1, "الفاتحة", 6, 1, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ"),
                MushafAyah(1, "الفاتحة", 7, 1, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ")
            ),
            2 to listOf(
                MushafAyah(2, "البقرة", 1, 1, "الم"),
                MushafAyah(2, "البقرة", 2, 1, "ذَٰلِكَ الْكِتَابُ لَا رَيْبَ فِيهِ هُدًى لِّلْمُتَّقِينَ"),
                MushafAyah(2, "البقرة", 3, 1, "الَّذِينَ يُؤْمِنُونَ بِالْغَيْبِ وَيُقِيمُونَ الصَّلَاةَ وَمِمَّا رَزَقْنَاهُمْ يُنفِقُونَ"),
                MushafAyah(2, "البقرة", 4, 1, "وَالَّذِينَ يُؤْمِنُونَ بِمَا أُنزِلَ إِلَيْكَ وَمَا أُنزِلَ مِن قَبْلِكَ وَبِالْآخِرَةِ هُمْ يُوقِنُونَ"),
                MushafAyah(2, "البقرة", 5, 1, "أُولَٰئِكَ عَلَىٰ هُدًى مِّن رَّبِّهِمْ وَأُولَٰئِكَ هُمُ الْمُفْلِحُونَ")
            ),
            67 to listOf(
                MushafAyah(67, "الملك", 1, 29, "تَبَارَكَ الَّذِي بِيَدِهِ الْمُلْكُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ"),
                MushafAyah(67, "الملك", 2, 29, "الَّذِي خَلَقَ الْمَوْتَ وَالْحَيَاةَ لِيَبْلُوَكُمْ أَيُّكُمْ أَحْسَنُ عَمَلًا وَهُوَ الْعَزِيزُ الْغَفُورُ"),
                MushafAyah(67, "الملك", 3, 29, "الَّذِي خَلَقَ سَبْعَ سَمَاوَاتٍ طِبَاقًا مَّا تَرَىٰ فِي خَلْقِ الرَّحْمَٰنِ مِن تَفَاوُتٍ"),
                MushafAyah(67, "الملك", 4, 29, "ثُمَّ ارْجِعِ الْبَصَرَ كَرَّتَيْنِ يَنقَلِبْ إِلَيْكَ الْبَصَرُ خَاسِئًا وَهُوَ حَسِيرٌ"),
                MushafAyah(67, "الملك", 5, 29, "وَلَقَدْ زَيَّنَّا السَّمَاءَ الدُّنْيَا بِمَصَابِيحَ وَجَعَلْنَاهَا رُجُومًا لِّلشَّيَاطِينِ")
            ),
            94 to listOf(
                MushafAyah(94, "الشرح", 1, 30, "أَلَمْ نَشْرَحْ لَكَ صَدْرَكَ"),
                MushafAyah(94, "الشرح", 2, 30, "وَوَضَعْنَا عَنكَ وِزْرَكَ"),
                MushafAyah(94, "الشرح", 3, 30, "الَّذِي أَنقَضَ ظَهْرَكَ"),
                MushafAyah(94, "الشرح", 4, 30, "وَرَفَعْنَا لَكَ ذِكْرَكَ"),
                MushafAyah(94, "الشرح", 5, 30, "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا"),
                MushafAyah(94, "الشرح", 6, 30, "إِنَّ مَعَ الْعُسْرِ يُسْرًا"),
                MushafAyah(94, "الشرح", 7, 30, "فَإِذَا فَرَغْتَ فَانصَبْ"),
                MushafAyah(94, "الشرح", 8, 30, "وَإِلَىٰ رَبِّكَ فَارْغَب")
            ),
            97 to listOf(
                MushafAyah(97, "القدر", 1, 30, "إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ"),
                MushafAyah(97, "القدر", 2, 30, "وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ"),
                MushafAyah(97, "القدر", 3, 30, "لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ أَلْفِ شَهْرٍ"),
                MushafAyah(97, "القدر", 4, 30, "تَنَزَّلُ الْمَلَائِكَةُ وَالرُّوحُ فِيهَا بِإِذْنِ رَبِّهِم مِّن كُلِّ أَمْرٍ"),
                MushafAyah(97, "القدر", 5, 30, "سَلَامٌ هِيَ حَتَّىٰ مَطْلَعِ الْفَجْرِ")
            ),
            103 to listOf(
                MushafAyah(103, "العصر", 1, 30, "وَالْعَصْرِ"),
                MushafAyah(103, "العصر", 2, 30, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ"),
                MushafAyah(103, "العصر", 3, 30, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ")
            ),
            112 to listOf(
                MushafAyah(112, "الإخلاص", 1, 30, "قُلْ هُوَ اللَّهُ أَحَدٌ"),
                MushafAyah(112, "الإخلاص", 2, 30, "اللَّهُ الصَّمَدُ"),
                MushafAyah(112, "الإخلاص", 3, 30, "لَمْ يَلِدْ وَلَمْ يُولَدْ"),
                MushafAyah(112, "الإخلاص", 4, 30, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ")
            ),
            113 to listOf(
                MushafAyah(113, "الفلق", 1, 30, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ"),
                MushafAyah(113, "الفلق", 2, 30, "مِن شَرِّ مَا خَلَقَ"),
                MushafAyah(113, "الفلق", 3, 30, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ"),
                MushafAyah(113, "الفلق", 4, 30, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ"),
                MushafAyah(113, "الفلق", 5, 30, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ")
            ),
            114 to listOf(
                MushafAyah(114, "الناس", 1, 30, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ"),
                MushafAyah(114, "الناس", 2, 30, "مَلِكِ النَّاسِ"),
                MushafAyah(114, "الناس", 3, 30, "إِلَٰهِ النَّاسِ"),
                MushafAyah(114, "الناس", 4, 30, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ"),
                MushafAyah(114, "الناس", 5, 30, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ"),
                MushafAyah(114, "الناس", 6, 30, "مِنَ الْجِنَّةِ وَالنَّاسِ")
            )
        )

        val juz15Offline: List<MushafAyah> = listOf(
            MushafAyah(17, "الإسراء", 9, 15, "إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ وَيُبَشِّرُ الْمُؤْمِنِينَ الَّذِينَ يَعْمَلُونَ الصَّالِحَاتِ أَنَّ لَهُمْ أَجْرًا كَبِيرًا"),
            MushafAyah(17, "الإسراء", 23, 15, "وَقَضَىٰ رَبُّكَ أَلَّا تَعْبُدُوا إِلَّا إِيَّاهُ وَبِالْوَالِدَيْنِ إِحْسَانًا"),
            MushafAyah(18, "الكهف", 1, 15, "الْحَمْدُ لِلَّهِ الَّذِي أَنزَلَ عَلَىٰ عَبْدِهِ الْكِتَابَ وَلَمْ يَجْعَل لَّهُ عِوَجَا"),
            MushafAyah(18, "الكهف", 10, 15, "رَبَّنَا آتِنَا مِن لَّدُنكَ رَحْمَةً وَهَيِّئْ لَنَا مِنْ أَمْرِنَا رَشَدًا")
        )

        val fallbackByJuz: Map<Int, List<MushafAyah>> = mapOf(
            1 to (fallbackBySurah[1].orEmpty() + fallbackBySurah[2].orEmpty()),
            15 to juz15Offline,
            29 to fallbackBySurah[67].orEmpty(),
            30 to (
                fallbackBySurah[94].orEmpty() +
                    fallbackBySurah[97].orEmpty() +
                    fallbackBySurah[103].orEmpty() +
                    fallbackBySurah[112].orEmpty() +
                    fallbackBySurah[113].orEmpty() +
                    fallbackBySurah[114].orEmpty()
                )
        )
    }
}
