package com.example.data

import android.content.Context
import com.example.model.CustomZikr
import com.example.model.DailyWorshipLog
import com.example.model.GamificationProfile
import com.example.model.JuzMasteryLevel
import com.example.model.JuzProgress
import com.example.model.MoodTag
import com.example.model.QuizAttempt
import com.example.model.ReflectionEntry
import com.example.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Local SharedPreferences-backed data source used ONLY in Development Quick-Entry Mode
 * when the user skips Google Sign-In during active development. Never calls Firebase/Firestore.
 */
class LocalDevRafikRepository(context: Context) : RafikDataSource {

    private val prefs = context.applicationContext
        .getSharedPreferences("rafik_dev_local_store", Context.MODE_PRIVATE)

    private val profileFlow = MutableStateFlow(loadProfile())
    private val juzFlow = MutableStateFlow(loadJuzMap())
    private val tajweedFlow = MutableStateFlow(loadTajweedMap())
    private val dailyFlow = MutableStateFlow(loadDailyMap())
    private val customAzkarFlow = MutableStateFlow(loadCustomAzkar())
    private val reflectionsFlow = MutableStateFlow(loadReflections())
    private val favoritesFlow = MutableStateFlow(loadFavorites())
    private val gamificationFlow = MutableStateFlow(loadGamification())
    private val attemptsFlow = MutableStateFlow(loadAttempts())

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = profileFlow

    override suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val current = profileFlow.value
        if (current.userId != userId) {
            val next = current.copy(
                id = userId,
                userId = userId,
                displayName = displayName.ifBlank { "رفيق القرآن (تطوير)" }
            )
            profileFlow.value = next
            saveProfile(next)
        }
        return Result.success(Unit)
    }

    override suspend fun updateRevisionGoal(userId: String, pagesGoal: Long): Result<Unit> {
        val next = profileFlow.value.copy(dailyRevisionTargetPages = pagesGoal.coerceIn(1L, 100L))
        profileFlow.value = next
        saveProfile(next)
        return Result.success(Unit)
    }

    override suspend fun updateFontScale(userId: String, fontScale: Double): Result<Unit> {
        val next = profileFlow.value.copy(zikrFontScale = fontScale.coerceIn(0.8, 1.8))
        profileFlow.value = next
        saveProfile(next)
        return Result.success(Unit)
    }

    override fun observeJuzProgress(userId: String): Flow<Map<Int, JuzProgress>> = juzFlow

    override suspend fun recordJuzRevision(
        userId: String,
        juzNumber: Int,
        masteryLevel: JuzMasteryLevel,
        mistakeNote: String,
        incrementRevision: Boolean,
        dateKey: String
    ): Result<Unit> {
        val existing = juzFlow.value[juzNumber]
        val now = System.currentTimeMillis()
        val updated = JuzProgress(
            id = "juz_$juzNumber",
            userId = userId,
            juzNumber = juzNumber.toLong(),
            masteryLevel = masteryLevel.key,
            revisionCount = (existing?.revisionCount ?: 0L) + if (incrementRevision) 1L else 0L,
            mistakeNote = mistakeNote.trim().take(400),
            lastReviewedEpochMs = if (incrementRevision) now else (existing?.lastReviewedEpochMs ?: 0L)
        )
        val nextMap = juzFlow.value + (juzNumber to updated)
        juzFlow.value = nextMap
        saveJuzMap(nextMap)

        if (incrementRevision) {
            val nextProfile = profileFlow.value.copy(totalRevisionsCount = profileFlow.value.totalRevisionsCount + 1L)
            profileFlow.value = nextProfile
            saveProfile(nextProfile)

            val dayLog = dailyFlow.value[dateKey] ?: DailyWorshipLog(userId = userId, dateKey = dateKey)
            val updatedDay = dayLog.copy(pagesRevisedToday = (dayLog.pagesRevisedToday + 20L).coerceIn(0L, 604L))
            val nextDaily = dailyFlow.value + (dateKey to updatedDay)
            dailyFlow.value = nextDaily
            saveDailyMap(nextDaily)
        }
        return Result.success(Unit)
    }

    override fun observeTajweedProgress(userId: String): Flow<Map<String, Boolean>> = tajweedFlow

    override suspend fun toggleTajweedMastery(userId: String, ruleKey: String, mastered: Boolean): Result<Unit> {
        val next = tajweedFlow.value + (ruleKey to mastered)
        tajweedFlow.value = next
        saveTajweedMap(next)
        return Result.success(Unit)
    }

    override fun observeDailyLog(userId: String, dateKey: String): Flow<DailyWorshipLog> =
        dailyFlow.map { it[dateKey] ?: DailyWorshipLog(id = dateKey, userId = userId, dateKey = dateKey) }

    override suspend fun updatePagesRevisedToday(userId: String, dateKey: String, pages: Long): Result<Unit> {
        val cur = dailyFlow.value[dateKey] ?: DailyWorshipLog(id = dateKey, userId = userId, dateKey = dateKey)
        val nextMap = dailyFlow.value + (dateKey to cur.copy(pagesRevisedToday = pages.coerceIn(0L, 604L)))
        dailyFlow.value = nextMap
        saveDailyMap(nextMap)
        return Result.success(Unit)
    }

    override suspend fun toggleAzkarStatus(userId: String, dateKey: String, field: String, value: Boolean): Result<Unit> {
        val cur = dailyFlow.value[dateKey] ?: DailyWorshipLog(id = dateKey, userId = userId, dateKey = dateKey)
        val updated = if (field == "morningAzkarDone") cur.copy(morningAzkarDone = value) else cur.copy(eveningAzkarDone = value)
        val nextMap = dailyFlow.value + (dateKey to updated)
        dailyFlow.value = nextMap
        saveDailyMap(nextMap)
        return Result.success(Unit)
    }

    override suspend fun recordTajweedQuizHit(userId: String, dateKey: String): Result<Unit> {
        val cur = dailyFlow.value[dateKey] ?: DailyWorshipLog(id = dateKey, userId = userId, dateKey = dateKey)
        val nextMap = dailyFlow.value + (dateKey to cur.copy(tajweedQuizCorrect = cur.tajweedQuizCorrect + 1L))
        dailyFlow.value = nextMap
        saveDailyMap(nextMap)
        return Result.success(Unit)
    }

    override suspend fun recordTasbihCount(userId: String, dateKey: String, delta: Long): Result<Unit> {
        val cur = dailyFlow.value[dateKey] ?: DailyWorshipLog(id = dateKey, userId = userId, dateKey = dateKey)
        val nextMap = dailyFlow.value + (dateKey to cur.copy(tasbihDoneToday = cur.tasbihDoneToday + delta))
        dailyFlow.value = nextMap
        saveDailyMap(nextMap)

        val nextProfile = profileFlow.value.copy(totalTasbihCount = profileFlow.value.totalTasbihCount + delta)
        profileFlow.value = nextProfile
        saveProfile(nextProfile)
        return Result.success(Unit)
    }

    override fun observeCustomAzkar(userId: String): Flow<List<CustomZikr>> = customAzkarFlow

    override suspend fun addCustomZikr(userId: String, text: String, virtue: String, targetCount: Long): Result<Unit> {
        val item = CustomZikr(
            id = UUID.randomUUID().toString(),
            userId = userId,
            text = text.trim(),
            virtue = virtue.trim(),
            targetCount = targetCount.coerceIn(1L, 10000L)
        )
        val next = listOf(item) + customAzkarFlow.value
        customAzkarFlow.value = next
        saveCustomAzkar(next)
        return Result.success(Unit)
    }

    override suspend fun incrementCustomZikrCount(userId: String, zikrId: String): Result<Unit> {
        val next = customAzkarFlow.value.map {
            if (it.id == zikrId) it.copy(totalCompleted = it.totalCompleted + 1L) else it
        }
        customAzkarFlow.value = next
        saveCustomAzkar(next)
        return Result.success(Unit)
    }

    override suspend fun deleteCustomZikr(userId: String, zikrId: String): Result<Unit> {
        val next = customAzkarFlow.value.filterNot { it.id == zikrId }
        customAzkarFlow.value = next
        saveCustomAzkar(next)
        return Result.success(Unit)
    }

    override fun observeReflections(userId: String): Flow<List<ReflectionEntry>> = reflectionsFlow

    override suspend fun addReflection(userId: String, title: String, body: String, moodTag: MoodTag, dateKey: String): Result<Unit> {
        val item = ReflectionEntry(
            id = UUID.randomUUID().toString(),
            userId = userId,
            title = title.trim(),
            body = body.trim(),
            moodTag = moodTag.key,
            dateKey = dateKey
        )
        val next = listOf(item) + reflectionsFlow.value
        reflectionsFlow.value = next
        saveReflections(next)
        return Result.success(Unit)
    }

    override suspend fun updateReflection(userId: String, reflectionId: String, title: String, body: String, moodTag: MoodTag): Result<Unit> {
        val next = reflectionsFlow.value.map {
            if (it.id == reflectionId) it.copy(title = title.trim(), body = body.trim(), moodTag = moodTag.key) else it
        }
        reflectionsFlow.value = next
        saveReflections(next)
        return Result.success(Unit)
    }

    override suspend fun deleteReflection(userId: String, reflectionId: String): Result<Unit> {
        val next = reflectionsFlow.value.filterNot { it.id == reflectionId }
        reflectionsFlow.value = next
        saveReflections(next)
        return Result.success(Unit)
    }

    override fun observeFavoriteKeys(userId: String): Flow<Set<String>> = favoritesFlow

    override suspend fun toggleFavoriteZikr(userId: String, zikrKey: String, categoryKey: String, currentlyFavorite: Boolean): Result<Unit> {
        val next = if (currentlyFavorite) favoritesFlow.value - zikrKey else favoritesFlow.value + zikrKey
        favoritesFlow.value = next
        prefs.edit().putStringSet("favorites", next).apply()
        return Result.success(Unit)
    }

    override fun observeGamificationProfile(userId: String): Flow<GamificationProfile> = gamificationFlow

    override fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> = attemptsFlow

    override suspend fun saveGamificationAndAttempt(
        userId: String,
        updatedProfile: GamificationProfile,
        attempt: QuizAttempt?
    ): Result<Unit> {
        val normalized = updatedProfile.copy(userId = userId)
        gamificationFlow.value = normalized
        saveGamification(normalized)

        if (attempt != null) {
            val savedAttempt = attempt.copy(
                id = attempt.id.ifBlank { UUID.randomUUID().toString() },
                userId = userId
            )
            val nextAttempts = (listOf(savedAttempt) + attemptsFlow.value).take(25)
            attemptsFlow.value = nextAttempts
            saveAttempts(nextAttempts)
        }
        return Result.success(Unit)
    }

    override suspend fun verifyCloudSync(userId: String): Result<Long> = Result.success(0L)

    // ---- JSON Persistence Helpers ----

    private fun loadProfile(): UserProfile {
        val raw = prefs.getString("profile", null) ?: return UserProfile(displayName = "حافظ القرآن")
        return runCatching {
            val o = JSONObject(raw)
            UserProfile(
                id = o.optString("userId", ""),
                userId = o.optString("userId", ""),
                displayName = o.optString("displayName", "حافظ القرآن"),
                dailyRevisionTargetPages = o.optLong("dailyRevisionTargetPages", 20L),
                zikrFontScale = o.optDouble("zikrFontScale", 1.0),
                totalRevisionsCount = o.optLong("totalRevisionsCount", 0L),
                totalTasbihCount = o.optLong("totalTasbihCount", 0L)
            )
        }.getOrDefault(UserProfile(displayName = "حافظ القرآن"))
    }

    private fun saveProfile(p: UserProfile) {
        val o = JSONObject().apply {
            put("userId", p.userId)
            put("displayName", p.displayName)
            put("dailyRevisionTargetPages", p.dailyRevisionTargetPages)
            put("zikrFontScale", p.zikrFontScale)
            put("totalRevisionsCount", p.totalRevisionsCount)
            put("totalTasbihCount", p.totalTasbihCount)
        }
        prefs.edit().putString("profile", o.toString()).apply()
    }

    private fun loadJuzMap(): Map<Int, JuzProgress> {
        val raw = prefs.getString("juz_map", null) ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val num = o.getInt("juzNumber")
                    put(
                        num,
                        JuzProgress(
                            id = "juz_$num",
                            userId = o.optString("userId"),
                            juzNumber = num.toLong(),
                            masteryLevel = o.optString("masteryLevel", JuzMasteryLevel.NOT_STARTED.key),
                            revisionCount = o.optLong("revisionCount", 0L),
                            mistakeNote = o.optString("mistakeNote", ""),
                            lastReviewedEpochMs = o.optLong("lastReviewedEpochMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveJuzMap(map: Map<Int, JuzProgress>) {
        val arr = JSONArray()
        map.values.forEach { j ->
            arr.put(JSONObject().apply {
                put("userId", j.userId)
                put("juzNumber", j.juzNumber)
                put("masteryLevel", j.masteryLevel)
                put("revisionCount", j.revisionCount)
                put("mistakeNote", j.mistakeNote)
                put("lastReviewedEpochMs", j.lastReviewedEpochMs)
            })
        }
        prefs.edit().putString("juz_map", arr.toString()).apply()
    }

    private fun loadTajweedMap(): Map<String, Boolean> {
        val raw = prefs.getString("tajweed_map", null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { k -> put(k, o.getBoolean(k)) }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveTajweedMap(map: Map<String, Boolean>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, v) }
        prefs.edit().putString("tajweed_map", o.toString()).apply()
    }

    private fun loadDailyMap(): Map<String, DailyWorshipLog> {
        val raw = prefs.getString("daily_map", null) ?: return emptyMap()
        return runCatching {
            val o = JSONObject(raw)
            buildMap {
                o.keys().forEach { key ->
                    val d = o.getJSONObject(key)
                    put(
                        key,
                        DailyWorshipLog(
                            id = key,
                            userId = d.optString("userId"),
                            dateKey = key,
                            pagesRevisedToday = d.optLong("pagesRevisedToday", 0L),
                            morningAzkarDone = d.optBoolean("morningAzkarDone", false),
                            eveningAzkarDone = d.optBoolean("eveningAzkarDone", false),
                            tasbihDoneToday = d.optLong("tasbihDoneToday", 0L),
                            tajweedQuizCorrect = d.optLong("tajweedQuizCorrect", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun saveDailyMap(map: Map<String, DailyWorshipLog>) {
        val o = JSONObject()
        map.forEach { (key, d) ->
            o.put(key, JSONObject().apply {
                put("userId", d.userId)
                put("pagesRevisedToday", d.pagesRevisedToday)
                put("morningAzkarDone", d.morningAzkarDone)
                put("eveningAzkarDone", d.eveningAzkarDone)
                put("tasbihDoneToday", d.tasbihDoneToday)
                put("tajweedQuizCorrect", d.tajweedQuizCorrect)
            })
        }
        prefs.edit().putString("daily_map", o.toString()).apply()
    }

    private fun loadCustomAzkar(): List<CustomZikr> {
        val raw = prefs.getString("custom_azkar", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CustomZikr(
                    id = o.getString("id"),
                    userId = o.optString("userId"),
                    text = o.getString("text"),
                    virtue = o.optString("virtue"),
                    targetCount = o.optLong("targetCount", 33L),
                    totalCompleted = o.optLong("totalCompleted", 0L),
                    favorite = o.optBoolean("favorite", false)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveCustomAzkar(list: List<CustomZikr>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("userId", item.userId)
                put("text", item.text)
                put("virtue", item.virtue)
                put("targetCount", item.targetCount)
                put("totalCompleted", item.totalCompleted)
                put("favorite", item.favorite)
            })
        }
        prefs.edit().putString("custom_azkar", arr.toString()).apply()
    }

    private fun loadReflections(): List<ReflectionEntry> {
        val raw = prefs.getString("reflections", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ReflectionEntry(
                    id = o.getString("id"),
                    userId = o.optString("userId"),
                    title = o.getString("title"),
                    body = o.getString("body"),
                    moodTag = o.optString("moodTag", MoodTag.AYAH_FIX.key),
                    dateKey = o.optString("dateKey")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveReflections(list: List<ReflectionEntry>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("userId", item.userId)
                put("title", item.title)
                put("body", item.body)
                put("moodTag", item.moodTag)
                put("dateKey", item.dateKey)
            })
        }
        prefs.edit().putString("reflections", arr.toString()).apply()
    }

    private fun loadFavorites(): Set<String> =
        prefs.getStringSet("favorites", emptySet())?.toSet() ?: emptySet()

    private fun loadGamification(): GamificationProfile {
        val raw = prefs.getString("gamification", null) ?: return GamificationProfile()
        return runCatching {
            val o = JSONObject(raw)
            GamificationProfile(
                userId = o.optString("userId"),
                xp = o.optLong("xp", 0L),
                currentStreakDays = o.optLong("currentStreakDays", 0L),
                longestQuizCombo = o.optLong("longestQuizCombo", 0L),
                quizzesCompleted = o.optLong("quizzesCompleted", 0L),
                perfectQuizzes = o.optLong("perfectQuizzes", 0L),
                totalCorrectAnswers = o.optLong("totalCorrectAnswers", 0L),
                surahOpenersSolved = o.optLong("surahOpenersSolved", 0L),
                surahClosersSolved = o.optLong("surahClosersSolved", 0L),
                mutashabihatSolved = o.optLong("mutashabihatSolved", 0L),
                testedJuzCsv = o.optString("testedJuzCsv", ""),
                fullQuranQuizDone = o.optBoolean("fullQuranQuizDone", false),
                unlockedBadgesCsv = o.optString("unlockedBadgesCsv", ""),
                lastActiveDateKey = o.optString("lastActiveDateKey", "")
            )
        }.getOrDefault(GamificationProfile())
    }

    private fun saveGamification(g: GamificationProfile) {
        val o = JSONObject().apply {
            put("userId", g.userId)
            put("xp", g.xp)
            put("currentStreakDays", g.currentStreakDays)
            put("longestQuizCombo", g.longestQuizCombo)
            put("quizzesCompleted", g.quizzesCompleted)
            put("perfectQuizzes", g.perfectQuizzes)
            put("totalCorrectAnswers", g.totalCorrectAnswers)
            put("surahOpenersSolved", g.surahOpenersSolved)
            put("surahClosersSolved", g.surahClosersSolved)
            put("mutashabihatSolved", g.mutashabihatSolved)
            put("testedJuzCsv", g.testedJuzCsv)
            put("fullQuranQuizDone", g.fullQuranQuizDone)
            put("unlockedBadgesCsv", g.unlockedBadgesCsv)
            put("lastActiveDateKey", g.lastActiveDateKey)
        }
        prefs.edit().putString("gamification", o.toString()).apply()
    }

    private fun loadAttempts(): List<QuizAttempt> {
        val raw = prefs.getString("quiz_attempts", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                QuizAttempt(
                    id = o.getString("id"),
                    userId = o.optString("userId"),
                    scopeLabel = o.getString("scopeLabel"),
                    questionTypesCsv = o.optString("questionTypesCsv"),
                    score = o.optLong("score"),
                    totalQuestions = o.optLong("totalQuestions", 1L),
                    xpEarned = o.optLong("xpEarned"),
                    bestCombo = o.optLong("bestCombo"),
                    dateKey = o.optString("dateKey")
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun saveAttempts(list: List<QuizAttempt>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("userId", a.userId)
                put("scopeLabel", a.scopeLabel)
                put("questionTypesCsv", a.questionTypesCsv)
                put("score", a.score)
                put("totalQuestions", a.totalQuestions)
                put("xpEarned", a.xpEarned)
                put("bestCombo", a.bestCombo)
                put("dateKey", a.dateKey)
            })
        }
        prefs.edit().putString("quiz_attempts", arr.toString()).apply()
    }
}
