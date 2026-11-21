package com.example.data

import com.example.model.AyahGradingResult
import com.example.model.WordComparisonToken

object ArabicQuranNormalizer {

    // Covers Arabic diacritics, dagger alif (0670), Tatweel (0640), and Quranic annotation/pause marks (06D6-06ED)
    private val DIACRITICS_REGEX = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val PUNCT_REGEX = Regex("[^\\u0621-\\u064A\\s]")
    private val MULTI_SPACE_REGEX = Regex("\\s+")

    // Common Uthmani script words where silent Waw precedes Dagger Alif (e.g., ٱلصَّلَوٰةَ -> الصلاه)
    // paired with their standard Imla'i normalized forms (e.g., الصلاه)
    private val UTHMANI_STEM_EQUIVALENTS = mapOf(
        "صلوه" to "صلاه",
        "زكوه" to "زكاه",
        "حيوه" to "حياه",
        "مشكوه" to "مشكاه",
        "نجوه" to "نجاه",
        "غدوه" to "غداه",
        "منوه" to "مناه",
        "ربوا" to "ربا",
        "ربو" to "ربا",
        "يبنوم" to "يا ابن ام"
    )

    fun stripBasmalaIfPrefix(text: String): String {
        val trimmed = text.trim()
        val prefixes = listOf(
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "بسم الله الرحمن الرحيم"
        )
        for (p in prefixes) {
            if (trimmed.startsWith(p) && trimmed.length > p.length + 2) {
                return trimmed.removePrefix(p).trim()
            }
        }
        val norm = normalize(trimmed)
        val normBasmala = "بسم الله الرحمن الرحيم"
        if (norm.startsWith(normBasmala) && norm.length > normBasmala.length + 2) {
            return norm.removePrefix(normBasmala).trim()
        }
        return trimmed
    }

    fun normalize(input: String): String {
        val noDiacritics = input.replace(DIACRITICS_REGEX, "")
        val mapped = buildString(noDiacritics.length) {
            for (ch in noDiacritics) {
                when (ch) {
                    'أ', 'إ', 'آ', 'ٱ' -> append('ا')
                    'ى' -> append('ي')
                    'ة' -> append('ه')
                    'ؤ' -> append('و')
                    'ئ' -> append('ي')
                    else -> append(ch)
                }
            }
        }
        val cleaned = mapped
            .replace("ءا", "ا")
            .replace(PUNCT_REGEX, " ")
            .replace(MULTI_SPACE_REGEX, " ")
            .trim()

        if (cleaned.isEmpty()) return ""

        // Expand joined Uthmani forms & normalize Rasm/Imla'i stem variants per word
        return cleaned.split(' ').joinToString(" ") { rawWord ->
            normalizeSingleWord(rawWord)
        }.replace(MULTI_SPACE_REGEX, " ").trim()
    }

    private fun normalizeSingleWord(word: String): String {
        var w = word
        // Split joined Uthmani vocatives: ياأيها -> يا ايها, هاأنتم -> ها انتم
        if (w.startsWith("يايها")) {
            w = "يا ايها" + w.removePrefix("يايها")
        } else if (w.startsWith("ياايها")) {
            w = "يا ايها" + w.removePrefix("ياايها")
        } else if (w.startsWith("هاانتم")) {
            w = "ها انتم" + w.removePrefix("هاانتم")
        } else if (w == "ياقوم") {
            w = "يا قوم"
        } else if (w == "يااهل") {
            w = "يا اهل"
        } else if (w == "يااولي") {
            w = "يا اولي"
        } else if (w == "يابني") {
            w = "يا بني"
        } else if (w == "ياسماء" || w == "ياارض") {
            w = "يا " + w.removePrefix("يا")
        }

        // Replace Uthmani Waw-for-Alif stems even with prefixes/suffixes (الصلوه -> الصلاه, صلاتهم)
        for ((uth, imla) in UTHMANI_STEM_EQUIVALENTS) {
            if (w == uth) return imla
            if (w.contains(uth)) {
                w = w.replace(uth, imla)
            }
        }
        return w
    }

    fun normalizeWords(input: String): List<String> {
        val clean = normalize(input)
        return if (clean.isEmpty()) emptyList() else clean.split(' ')
    }

    fun gradeRecitation(
        userAnswer: String,
        expectedUthmani: String,
        stripBasmala: Boolean = false
    ): AyahGradingResult {
        val cleanedUser = if (stripBasmala) stripBasmalaIfPrefix(userAnswer) else userAnswer
        val cleanedExpected = if (stripBasmala) stripBasmalaIfPrefix(expectedUthmani) else expectedUthmani

        val userWords = normalizeWords(cleanedUser)
        // Pair each display word with its normalized form and drop pure annotation/stop-mark tokens (e.g. ۞, ۖ, ۩)
        // before LCS alignment so token indices stay strictly aligned with expectedNormWords.
        val expectedTokenPairs = cleanedExpected
            .trim()
            .split(MULTI_SPACE_REGEX)
            .filter { it.isNotBlank() }
            .map { displayWord -> displayWord to normalize(displayWord) }
            .filter { (_, norm) -> norm.isNotEmpty() }

        val expectedDisplayWords = expectedTokenPairs.map { it.first }
        // Flatten if a single Uthmani word expands into 2 normalized tokens (e.g., يَٰٓأَيُّهَا -> يا + ايها)
        val expectedNormTokensPerWord = expectedTokenPairs.map { (_, norm) ->
            norm.split(' ').filter { it.isNotEmpty() }
        }
        val expectedNormWords = expectedNormTokensPerWord.map { it.joinToString("") }
        val flattenedUserWords = mergeVocativePairsIfNeeded(userWords, expectedNormTokensPerWord)

        if (expectedNormWords.isEmpty()) {
            return AyahGradingResult(
                accuracyPercent = 0,
                passed = false,
                masteryVerdictAr = "تعذر التحقق من النص",
                tokens = emptyList()
            )
        }
        if (flattenedUserWords.isEmpty()) {
            return AyahGradingResult(
                accuracyPercent = 0,
                passed = false,
                masteryVerdictAr = "اكتب إجابتك أولاً للتحقق",
                tokens = expectedDisplayWords.map { WordComparisonToken(it, matched = false) }
            )
        }

        // Longest Common Subsequence (LCS) alignment so a single skipped/extra word doesn't break the whole verse
        val n = expectedNormWords.size
        val m = flattenedUserWords.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) {
            for (j in 1..m) {
                dp[i][j] = if (isWordSimilar(expectedNormWords[i - 1], flattenedUserWords[j - 1])) {
                    dp[i - 1][j - 1] + 1
                } else {
                    maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val matchedExpectedIndices = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            if (isWordSimilar(expectedNormWords[i - 1], flattenedUserWords[j - 1])) {
                matchedExpectedIndices[i - 1] = true
                i--
                j--
            } else if (dp[i - 1][j] >= dp[i][j - 1]) {
                i--
            } else {
                j--
            }
        }

        val matchedCount = matchedExpectedIndices.count { it }
        // Penalize large extra hallucinated word counts mildly
        val extraPenalty = if (m > n + 3) ((m - n - 3) * 0.5).toInt() else 0
        val rawPercent = ((matchedCount.toFloat() / n.toFloat()) * 100f).toInt() - extraPenalty
        val accuracy = rawPercent.coerceIn(0, 100)
        val passed = accuracy >= 75

        val verdict = when {
            accuracy >= 95 -> "ضبط متقن ما شاء الله!"
            accuracy >= 80 -> "حفظ جيد جداً — راجع المواضع المظللة"
            accuracy >= 65 -> "قريب — يحتاج مزيد تثبيت"
            else -> "يحتاج مراجعة وتثبيتاً من المصحف"
        }

        val tokens = expectedDisplayWords.mapIndexed { idx, displayWord ->
            val isMatched = if (idx < matchedExpectedIndices.size) matchedExpectedIndices[idx] else false
            WordComparisonToken(expectedWord = displayWord, matched = isMatched)
        }

        return AyahGradingResult(
            accuracyPercent = accuracy,
            passed = passed,
            masteryVerdictAr = verdict,
            tokens = tokens
        )
    }

    private fun mergeVocativePairsIfNeeded(
        userWords: List<String>,
        expectedTokens: List<List<String>>
    ): List<String> {
        val hasJoinedUthmaniVocative = expectedTokens.any { it.size > 1 }
        if (!hasJoinedUthmaniVocative) return userWords

        val out = mutableListOf<String>()
        var idx = 0
        while (idx < userWords.size) {
            val cur = userWords[idx]
            val next = userWords.getOrNull(idx + 1)
            if ((cur == "يا" || cur == "ها") && next != null && (next == "ايها" || next == "انتم" || next == "قوم" || next == "اهل" || next == "اولي" || next == "بني")) {
                val merged = if (cur == "يا" && next == "ايها") "يايها" else cur + next
                out.add(merged)
                idx += 2
            } else {
                out.add(cur)
                idx += 1
            }
        }
        return out
    }

    private fun isWordSimilar(expected: String, actual: String): Boolean {
        if (expected == actual) return true
        // Allow small Uthmani script alif-khunjariyah spelling differences (e.g. الرحمن vs الرحمان, ذلك vs ذالك, هذا vs هاذا, لكن vs لاكن)
        val expNoAlif = expected.replace("ا", "")
        val actNoAlif = actual.replace("ا", "")
        if (expNoAlif.length >= 2 && expNoAlif == actNoAlif) return true

        // Allow minor Uthmani Hamza seat differences (e.g. اولئك vs اوليك / السيئات vs السيات / مائة vs مئة)
        val expNoWeak = expected.replace(Regex("[اويء]"), "")
        val actNoWeak = actual.replace(Regex("[اويء]"), "")
        if (expNoWeak.length >= 3 && expNoWeak == actNoWeak && kotlin.math.abs(expected.length - actual.length) <= 2) {
            return true
        }
        return false
    }
}
