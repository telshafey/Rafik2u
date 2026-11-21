package com.example

import com.example.data.ArabicQuranNormalizer
import com.example.data.GamificationCatalog
import com.example.data.MushafFetchResult
import com.example.data.QuranCatalog
import com.example.data.QuranQuizGenerator
import com.example.data.SunnahDuaCatalog
import com.example.data.TajweedCatalog
import com.example.model.GamificationProfile
import com.example.model.MushafAyah
import com.example.model.QuranQuestionType
import com.example.model.SunnahDuaCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranQuizAndGamificationTest {

    @Test
    fun normalizer_gradesRecitationWithoutTashkeelAccurately() {
        val expected = "قُلْ هُوَ اللَّهُ أَحَدٌ اللَّهُ الصَّمَدُ"
        val userInput = "قل هو الله احد الله الصمد"
        val result = ArabicQuranNormalizer.gradeRecitation(userInput, expected)
        assertTrue(result.passed)
        assertEquals(100, result.accuracyPercent)
        assertTrue(result.tokens.all { it.matched })
    }

    @Test
    fun normalizer_handlesUthmaniRasmAndStopMarksCleanly() {
        val expectedWithMarks = "۞ وَأَقِيمُوا ٱلصَّلَوٰةَ وَءَاتُوا ٱلزَّكَوٰةَ ۖ يَـٰٓأَيُّهَا ٱلَّذِينَ ءَامَنُوا"
        val standardArabicTyping = "واقيموا الصلاة واتوا الزكاة يا ايها الذين امنوا"
        val result = ArabicQuranNormalizer.gradeRecitation(standardArabicTyping, expectedWithMarks)
        assertTrue("Expected >= 90% accuracy, got ${result.accuracyPercent}", result.accuracyPercent >= 90)
        assertTrue(result.passed)
        assertFalse(result.tokens.any { it.expectedWord == "۞" || it.expectedWord == "ۖ" })
        assertTrue(result.tokens.all { it.matched })
    }

    @Test
    fun normalizer_flagsMissedWordsAndLowScore() {
        val expected = "قُلْ أَعُوذُ بِرَبِّ النَّاسِ مَلِكِ النَّاسِ إِلَٰهِ النَّاسِ"
        val userInput = "قل اعوذ برب الفلق"
        val result = ArabicQuranNormalizer.gradeRecitation(userInput, expected)
        assertFalse(result.passed)
        assertTrue(result.accuracyPercent < 75)
    }

    @Test
    fun catalog_coversAll114Surahs30JuzCompleteTuhfahAndBukhariMuslim() {
        assertEquals(30, QuranCatalog.allJuz.size)
        assertEquals(114, QuranCatalog.allSurahs.size)
        for (juz in 1..30) {
            assertTrue("Juz $juz should have Surahs", QuranCatalog.surahsForJuz(setOf(juz)).isNotEmpty())
        }
        assertTrue(QuranCatalog.mutashabihatList.size >= 20)

        // Complete Tuhfat al-Atfal checks (9 chapters, 61 verses)
        assertEquals(9, TajweedCatalog.tuhfahChapterCount)
        assertEquals(61, TajweedCatalog.totalTuhfahVerses)
        assertTrue(TajweedCatalog.rules.size >= 12)
        assertTrue(TajweedCatalog.quizQuestions.size >= 20)

        // Strictly Bukhari & Muslim verification on Prophetic Hadith categories
        assertTrue(SunnahDuaCatalog.items.size >= 28)
        for (cat in SunnahDuaCategory.entries) {
            assertTrue("Category $cat should have items", SunnahDuaCatalog.items.any { it.category == cat })
        }
        val hadithItems = SunnahDuaCatalog.items.filter {
            it.category == SunnahDuaCategory.QURAN_VIRTUES || it.category == SunnahDuaCategory.SAHIH_SUNNAH
        }
        hadithItems.forEach { item ->
            val fromSahihayn = item.sourceAr.contains("البخاري") ||
                item.sourceAr.contains("مسلم") ||
                item.sourceAr.contains("متفق عليه")
            assertTrue("Hadith '${item.titleAr}' must be from Bukhari/Muslim, was: ${item.sourceAr}", fromSahihayn)
        }
    }

    @Test
    fun quizGenerator_createsWordCompletionMcForShortFilteredSurah() {
        val asr = listOf(
            MushafAyah(103, "العصر", 1, 30, "وَالْعَصْرِ"),
            MushafAyah(103, "العصر", 2, 30, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ"),
            MushafAyah(103, "العصر", 3, 30, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ")
        )
        val extraPool = listOf(
            MushafAyah(112, "الإخلاص", 1, 30, "قُلْ هُوَ اللَّهُ أَحَدٌ"),
            MushafAyah(112, "الإخلاص", 2, 30, "اللَّهُ الصَّمَدُ"),
            MushafAyah(112, "الإخلاص", 3, 30, "لَمْ يَلِدْ وَلَمْ يُولَدْ"),
            MushafAyah(112, "الإخلاص", 4, 30, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ")
        )
        val fetch = MushafFetchResult(
            ayahs = asr + extraPool,
            surahAyahsMap = mapOf(103 to asr, 112 to extraPool),
            usedOfflineFallback = true
        )
        val generated = QuranQuizGenerator.generateQuestions(
            fetchResult = fetch,
            selectedTypes = setOf(QuranQuestionType.MULTIPLE_CHOICE),
            questionCount = 2,
            optionalSurahNumber = 103
        )
        assertTrue(generated.isNotEmpty())
        assertTrue(generated.none { it.headerLabelAr.contains("في أي سورة") })
    }

    @Test
    fun gamification_unlocksExpandedBadgesRanksAndRewards() {
        assertEquals(10, GamificationCatalog.ranks.size)
        assertEquals(20, GamificationCatalog.badges.size)
        assertEquals(6, GamificationCatalog.rewards.size)

        val profile = GamificationProfile(
            xp = 4200L,
            currentStreakDays = 7L,
            longestQuizCombo = 5L,
            quizzesCompleted = 2L,
            perfectQuizzes = 1L,
            totalCorrectAnswers = 12L,
            surahOpenersSolved = 3L,
            surahClosersSolved = 3L,
            mutashabihatSolved = 8L,
            testedJuzCsv = "1,2,3,4,5,6,7,8,9,10,11,12,13,14,30",
            fullQuranQuizDone = true
        )
        val unlocked = GamificationCatalog.evaluateNewBadges(
            profile = profile,
            masteredTajweedCount = 9,
            masteredJuzNumbers = setOf(30),
            timedQuizPassedNow = true,
            voiceCorrectUsedNow = true,
            mushafSurahsReadCount = 3
        ).map { it.id }.toSet()

        assertTrue("first_test" in unlocked)
        assertTrue("perfect_score" in unlocked)
        assertTrue("timed_challenger" in unlocked)
        assertTrue("voice_reciter" in unlocked)
        assertTrue("tuhfah_scholar" in unlocked)
        assertTrue("mushaf_explorer" in unlocked)
        assertTrue("mutashabihat_collector" in unlocked)
        assertTrue("half_khatmah" in unlocked)
        assertTrue("daily_streak_7" in unlocked)

        val updatedProfile = profile.copy(unlockedBadgesCsv = unlocked.joinToString(","))
        val unlockedRewards = GamificationCatalog.rewards.filter {
            GamificationCatalog.isRewardUnlocked(it, updatedProfile)
        }.map { it.id }.toSet()
        assertTrue("rew_voice_medal" in unlockedRewards)
        assertTrue("rew_speed_shield" in unlockedRewards)
        assertTrue("rew_tuhfah_ijazah" in unlockedRewards)
        assertTrue("rew_sanad_crown" in unlockedRewards)
        assertEquals(6, GamificationCatalog.rankForXp(4200L).level)
    }
}
