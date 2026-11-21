package com.example.data

import com.example.model.MushafAyah
import com.example.model.QuranQuestionType
import com.example.model.QuranQuizQuestion
import kotlin.random.Random

object QuranQuizGenerator {

    fun generateQuestions(
        fetchResult: MushafFetchResult,
        selectedTypes: Set<QuranQuestionType>,
        questionCount: Int,
        optionalSurahNumber: Int?
    ): List<QuranQuizQuestion> {
        val types = selectedTypes.ifEmpty { QuranQuestionType.entries.toSet() }.toList()
        val allAyahs = if (optionalSurahNumber != null) {
            fetchResult.surahAyahsMap[optionalSurahNumber]
                ?: fetchResult.ayahs.filter { it.surahNumber == optionalSurahNumber }
        } else {
            fetchResult.ayahs
        }

        if (allAyahs.isEmpty()) return emptyList()

        val questions = mutableListOf<QuranQuizQuestion>()
        val usedSignatures = mutableSetOf<String>()

        var attempts = 0
        val maxAttempts = questionCount * 12
        while (questions.size < questionCount && attempts < maxAttempts) {
            attempts++
            val chosenType = types[(questions.size + attempts) % types.size]
            val q = when (chosenType) {
                QuranQuestionType.COMPLETE_AYAH -> buildCompleteAyahQuestion(allAyahs, usedSignatures)
                QuranQuestionType.MULTIPLE_CHOICE -> buildMultipleChoiceQuestion(
                    ayahs = allAyahs,
                    poolForDistractors = fetchResult.ayahs.ifEmpty { allAyahs },
                    optionalSurahNumber = optionalSurahNumber,
                    usedSignatures = usedSignatures
                )
                QuranQuestionType.SURAH_BEGINNING -> buildSurahBeginningQuestion(fetchResult, optionalSurahNumber, usedSignatures)
                QuranQuestionType.SURAH_LAST_5 -> buildSurahLast5Question(fetchResult, optionalSurahNumber, usedSignatures)
            }
            if (q != null) {
                questions.add(q)
            }
        }

        // Fill remaining slots cleanly if Surah openers/closers were exhausted for small scopes
        var fallbackTries = 0
        while (questions.size < questionCount && fallbackTries < questionCount * 10) {
            fallbackTries++
            val q = buildMultipleChoiceQuestion(
                ayahs = allAyahs,
                poolForDistractors = fetchResult.ayahs.ifEmpty { allAyahs },
                optionalSurahNumber = optionalSurahNumber,
                usedSignatures = usedSignatures
            ) ?: buildCompleteAyahQuestion(allAyahs, usedSignatures)
            if (q != null) questions.add(q)
        }

        return questions
    }

    private fun buildCompleteAyahQuestion(
        ayahs: List<MushafAyah>,
        usedSignatures: MutableSet<String>
    ): QuranQuizQuestion? {
        val candidates = ayahs.filter { wordsOf(it.textUthmani).size >= 6 }
            .ifEmpty { ayahs.filter { wordsOf(it.textUthmani).size >= 3 } }
        if (candidates.isEmpty()) return null

        val ayah = candidates[Random.nextInt(candidates.size)]
        val sig = "COMP_${ayah.surahNumber}_${ayah.numberInSurah}"
        if (!usedSignatures.add(sig)) return null

        val words = wordsOf(ayah.textUthmani)
        val splitPoint = (words.size / 2).coerceIn(2, 5)
        val promptPrefix = words.take(splitPoint).joinToString(" ")
        val expectedRest = words.drop(splitPoint).joinToString(" ")
        val firstHintWord = words.getOrNull(splitPoint).orEmpty()

        return QuranQuizQuestion(
            id = sig,
            type = QuranQuestionType.COMPLETE_AYAH,
            juzNumber = ayah.juzNumber,
            surahNumber = ayah.surahNumber,
            surahNameAr = ayah.surahNameAr,
            ayahRangeLabel = "سورة ${ayah.surahNameAr} • آية ${ayah.numberInSurah}",
            headerLabelAr = "أكمل الآية الكريمة",
            promptTextAr = "$promptPrefix ...",
            hintTextAr = "الجزء ${ayah.juzNumber} • سورة ${ayah.surahNameAr} • تبدأ التكملة بـ: «$firstHintWord»",
            expectedAnswerAr = expectedRest
        )
    }

    private fun buildMultipleChoiceQuestion(
        ayahs: List<MushafAyah>,
        poolForDistractors: List<MushafAyah>,
        optionalSurahNumber: Int?,
        usedSignatures: MutableSet<String>
    ): QuranQuizQuestion? {
        val sorted = ayahs.sortedWith(compareBy({ it.surahNumber }, { it.numberInSurah }))
        val pairCandidates = mutableListOf<Pair<MushafAyah, MushafAyah>>()
        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]
            if (a.surahNumber == b.surahNumber && b.numberInSurah == a.numberInSurah + 1) {
                pairCandidates.add(a to b)
            }
        }

        val distractorPool = if (sorted.size >= 5) sorted else poolForDistractors
        if (pairCandidates.isNotEmpty() && distractorPool.size >= 5 && Random.nextInt(10) < 7) {
            val (current, next) = pairCandidates[Random.nextInt(pairCandidates.size)]
            val sig = "MC_NEXT_${current.surahNumber}_${current.numberInSurah}"
            if (usedSignatures.add(sig)) {
                val correctOption = shortenOption(next.textUthmani)
                val distractors = distractorPool
                    .filter { it.textUthmani != next.textUthmani && it.textUthmani != current.textUthmani }
                    .map { shortenOption(it.textUthmani) }
                    .distinct()
                    .filter { it != correctOption }
                    .shuffled()
                    .take(3)
                if (distractors.size == 3) {
                    val allOptions = (distractors + correctOption).shuffled()
                    return QuranQuizQuestion(
                        id = sig,
                        type = QuranQuestionType.MULTIPLE_CHOICE,
                        juzNumber = current.juzNumber,
                        surahNumber = current.surahNumber,
                        surahNameAr = current.surahNameAr,
                        ayahRangeLabel = "سورة ${current.surahNameAr} • بعد آية ${current.numberInSurah}",
                        headerLabelAr = "اختر الآية التالية الصحيحة",
                        promptTextAr = current.textUthmani,
                        hintTextAr = "سورة ${current.surahNameAr} • الجزء ${current.juzNumber}",
                        expectedAnswerAr = next.textUthmani,
                        options = allOptions,
                        correctOptionIndex = allOptions.indexOf(correctOption)
                    )
                }
            }
        }

        // Fill-in-the-blank word Multiple Choice (works great even for short 3-4 ayah Surahs!)
        val wordMcCandidates = sorted.filter { wordsOf(it.textUthmani).size >= 4 }
        if (wordMcCandidates.isNotEmpty()) {
            val target = wordMcCandidates[Random.nextInt(wordMcCandidates.size)]
            val sig = "MC_WORD_${target.surahNumber}_${target.numberInSurah}"
            if (usedSignatures.add(sig)) {
                val words = wordsOf(target.textUthmani)
                val blankIdx = (words.size - 1).coerceAtLeast(2)
                val targetWord = words[blankIdx]
                val maskedPrompt = words.mapIndexed { i, w -> if (i == blankIdx) "﴿ ...... ﴾" else w }.joinToString(" ")

                val distractorWords = distractorPool
                    .flatMap { wordsOf(it.textUthmani) }
                    .filter { it.length >= 3 && ArabicQuranNormalizer.normalize(it) != ArabicQuranNormalizer.normalize(targetWord) }
                    .distinctBy { ArabicQuranNormalizer.normalize(it) }
                    .shuffled()
                    .take(3)

                if (distractorWords.size == 3) {
                    val options = (distractorWords + targetWord).shuffled()
                    return QuranQuizQuestion(
                        id = sig,
                        type = QuranQuestionType.MULTIPLE_CHOICE,
                        juzNumber = target.juzNumber,
                        surahNumber = target.surahNumber,
                        surahNameAr = target.surahNameAr,
                        ayahRangeLabel = "سورة ${target.surahNameAr} • آية ${target.numberInSurah}",
                        headerLabelAr = "اختر الكلمة الصحيحة لإتمام الآية",
                        promptTextAr = maskedPrompt,
                        hintTextAr = "سورة ${target.surahNameAr} • الآية ${target.numberInSurah}",
                        expectedAnswerAr = targetWord,
                        options = options,
                        correctOptionIndex = options.indexOf(targetWord)
                    )
                }
            }
        }

        // Only ask "which Surah is this?" when scope isn't pinned to a single Surah
        if (optionalSurahNumber == null) {
            val target = sorted[Random.nextInt(sorted.size)]
            val sig = "MC_SURAH_${target.surahNumber}_${target.numberInSurah}"
            if (!usedSignatures.add(sig)) return null
            val otherSurahs = QuranCatalog.allSurahs
                .filter { it.number != target.surahNumber }
                .shuffled()
                .take(3)
                .map { "سورة ${it.nameAr}" }
            val correctStr = "سورة ${target.surahNameAr}"
            val options = (otherSurahs + correctStr).shuffled()
            return QuranQuizQuestion(
                id = sig,
                type = QuranQuestionType.MULTIPLE_CHOICE,
                juzNumber = target.juzNumber,
                surahNumber = target.surahNumber,
                surahNameAr = target.surahNameAr,
                ayahRangeLabel = "سورة ${target.surahNameAr} • آية ${target.numberInSurah}",
                headerLabelAr = "في أي سورة وردت هذه الآية؟",
                promptTextAr = target.textUthmani,
                hintTextAr = "تقع في الجزء ${target.juzNumber}",
                expectedAnswerAr = correctStr,
                options = options,
                correctOptionIndex = options.indexOf(correctStr)
            )
        }
        return null
    }

    private fun buildSurahBeginningQuestion(
        fetchResult: MushafFetchResult,
        optionalSurahNumber: Int?,
        usedSignatures: MutableSet<String>
    ): QuranQuizQuestion? {
        val availableSurahs = if (optionalSurahNumber != null) {
            listOfNotNull(fetchResult.surahAyahsMap[optionalSurahNumber]?.let { optionalSurahNumber to it })
        } else {
            fetchResult.surahAyahsMap.entries
                .filter { it.value.isNotEmpty() && it.value.first().numberInSurah == 1 }
                .map { it.key to it.value }
                .shuffled()
        }

        for ((surahNum, ayahs) in availableSurahs) {
            val ordered = ayahs.sortedBy { it.numberInSurah }
            if (ordered.firstOrNull()?.numberInSurah != 1) continue
            val sig = "BEG_$surahNum"
            if (!usedSignatures.add(sig)) continue

            val countToRecite = minOf(3, ordered.size)
            val slice = ordered.take(countToRecite)
            val surahName = slice.first().surahNameAr
            val expectedText = slice.joinToString(" ") { it.textUthmani }
            val firstWords = wordsOf(slice.first().textUthmani).take(2).joinToString(" ")

            return QuranQuizQuestion(
                id = sig,
                type = QuranQuestionType.SURAH_BEGINNING,
                juzNumber = slice.first().juzNumber,
                surahNumber = surahNum,
                surahNameAr = surahName,
                ayahRangeLabel = "سورة $surahName • الآيات ١ إلى $countToRecite",
                headerLabelAr = "اكتب من أول السورة",
                promptTextAr = "اكتب أول $countToRecite آيات من سورة «$surahName»",
                hintTextAr = "أول كلمتين في السورة: «$firstWords ...»",
                expectedAnswerAr = expectedText
            )
        }
        return null
    }

    private fun buildSurahLast5Question(
        fetchResult: MushafFetchResult,
        optionalSurahNumber: Int?,
        usedSignatures: MutableSet<String>
    ): QuranQuizQuestion? {
        val availableSurahs = if (optionalSurahNumber != null) {
            listOfNotNull(fetchResult.surahAyahsMap[optionalSurahNumber]?.let { optionalSurahNumber to it })
        } else {
            fetchResult.surahAyahsMap.entries
                .filter { it.value.size >= 4 }
                .map { it.key to it.value }
                .shuffled()
        }

        for ((surahNum, ayahs) in availableSurahs) {
            val ordered = ayahs.sortedBy { it.numberInSurah }
            if (ordered.size < 4) continue
            val sig = "LAST5_$surahNum"
            if (!usedSignatures.add(sig)) continue

            val takeN = minOf(5, ordered.size)
            val slice = ordered.takeLast(takeN)
            val surahName = slice.first().surahNameAr
            val startAyah = slice.first().numberInSurah
            val endAyah = slice.last().numberInSurah
            val expectedText = slice.joinToString(" ") { it.textUthmani }
            val startHint = wordsOf(slice.first().textUthmani).take(3).joinToString(" ")

            return QuranQuizQuestion(
                id = sig,
                type = QuranQuestionType.SURAH_LAST_5,
                juzNumber = slice.last().juzNumber,
                surahNumber = surahNum,
                surahNameAr = surahName,
                ayahRangeLabel = "سورة $surahName • الآيات $startAyah إلى $endAyah",
                headerLabelAr = "اكتب آخر $takeN آيات من السورة",
                promptTextAr = "اكتب آخر $takeN آيات من سورة «$surahName»",
                hintTextAr = "تبدأ الآية ($startAyah) بـ: «$startHint ...»",
                expectedAnswerAr = expectedText
            )
        }
        return null
    }

    private fun wordsOf(text: String): List<String> =
        text.trim().split(Regex("\\s+")).filter { it.isNotBlank() && ArabicQuranNormalizer.normalize(it).isNotEmpty() }

    private fun shortenOption(text: String): String {
        val words = wordsOf(text)
        return if (words.size > 14) words.take(14).joinToString(" ") + " ..." else text
    }
}
