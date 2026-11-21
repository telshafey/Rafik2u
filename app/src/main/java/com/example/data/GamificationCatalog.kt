package com.example.data

import com.example.model.GamificationProfile
import com.example.model.HafizBadge
import com.example.model.HafizRank
import com.example.model.HafizReward
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object GamificationCatalog {

    val ranks: List<HafizRank> = listOf(
        HafizRank(1, "طالب التعهد", 0L),
        HafizRank(2, "ملازم الورد", 250L),
        HafizRank(3, "حافظ مثابر", 700L),
        HafizRank(4, "ضابط الأجزاء", 1500L),
        HafizRank(5, "قارئ التحفة", 2600L),
        HafizRank(6, "سند ومتقن", 4000L),
        HafizRank(7, "فارس المتشابهات", 6000L),
        HafizRank(8, "حافظ متمكّن", 8500L),
        HafizRank(9, "مقرئ مجاز", 11500L),
        HafizRank(10, "إمام الحفاظ", 15000L)
    )

    val badges: List<HafizBadge> = listOf(
        HafizBadge("first_test", "خطوة البداية", "أكمل أول اختبار قرآني", 30L),
        HafizBadge("perfect_score", "الضبط التام", "أنهِ اختباراً كاملاً بنسبة ١٠٠٪", 80L),
        HafizBadge("streak_5_combo", "سيل التلاوة", "حقق ٥ إجابات صحيحة متتالية في اختبار واحد", 50L),
        HafizBadge("surah_opener", "فاتح السور", "أجب عن ٣ أسئلة «أول السورة» بنجاح", 60L),
        HafizBadge("surah_closer", "ضابط الخواتيم", "أجب عن ٣ أسئلة «آخر ٥ آيات» بنجاح", 75L),
        HafizBadge("juz_amma_champ", "نجم جزء عمّ", "اختبر نفسك في الجزء ٣٠ أو سجّل إتقانه", 50L),
        HafizBadge("khatmah_explorer", "جوال المصحف", "اختبر حفظك في ٥ أجزاء مختلفة على الأقل", 100L),
        HafizBadge("full_quran_challenger", "تحدي المصحف", "خض اختباراً في نطاق «كامل القرآن»", 120L),
        HafizBadge("mutashabihat_hawk", "قناص الضبط", "أجب عن ١٠ أسئلة حفظ بنجاح", 70L),
        HafizBadge("tajweed_master", "سند التجويد", "أتقن ٨ أبواب في دليل التجويد", 80L),
        HafizBadge("daily_streak_3", "المتعاهد المثابر", "واصل التعهد ٣ أيام متتالية", 90L),
        HafizBadge("xp_1000", "ألفية الحافظ", "اجمع ١٠٠٠ نقطة إتقان (XP)", 100L),
        // Expanded badges
        HafizBadge("timed_challenger", "فارس الوقت", "أنهِ اختباراً مبنياً على وقت بنسبة ٦٠٪ فأكثر", 90L),
        HafizBadge("voice_reciter", "تلاوة بالصوت", "أجب عن سؤال تسميع صحيح باستخدام المايك", 75L),
        HafizBadge("tuhfah_scholar", "حافظ التحفة", "أتقن جميع أبواب متن تحفة الأطفال (٩ أبواب)", 130L),
        HafizBadge("mushaf_explorer", "رفيق المصحف", "استعرض واقرأ ٣ سور من المصحف الكامل", 60L),
        HafizBadge("mutashabihat_collector", "جامع المتشابهات", "اضبط ٨ مواضع متشابهة في قسم المتشابهات", 90L),
        HafizBadge("half_khatmah", "شطر المصحف", "أتقن أو اختبر ١٥ جزءاً مختلفاً", 160L),
        HafizBadge("daily_streak_7", "أسبوع التعهد", "واصل التعهد والمراجعة ٧ أيام متتالية", 150L),
        HafizBadge("full_khatmah_master", "خاتم متقن", "سجّل إتقان ٣٠ جزءاً في خطة القرآن", 350L)
    )

    val rewards: List<HafizReward> = listOf(
        HafizReward(
            id = "rew_starter_scroll",
            titleAr = "شهادة بداية الحافظ",
            subtitleAr = "وسام الانطلاقة في درب المراجعة والتعهد",
            requirementAr = "الوصول إلى ١٠٠ XP أو إكمال أول اختبار",
            minXp = 100L,
            requiredBadgeId = "first_test"
        ),
        HafizReward(
            id = "rew_voice_medal",
            titleAr = "وسام المسمّع بالصوت",
            subtitleAr = "شهادة تقدير لإتقان التسميع الشفوي عبر المايك",
            requirementAr = "الحصول على شارة «تلاوة بالصوت»",
            requiredBadgeId = "voice_reciter"
        ),
        HafizReward(
            id = "rew_speed_shield",
            titleAr = "درع الاستحضار السريع",
            subtitleAr = "تكريم على استحضار الآيات تحت ضغط المؤقت",
            requirementAr = "الحصول على شارة «فارس الوقت»",
            requiredBadgeId = "timed_challenger"
        ),
        HafizReward(
            id = "rew_tuhfah_ijazah",
            titleAr = "وثيقة إتمام تحفة الأطفال",
            subtitleAr = "وسام ضبط أحكام النون والميم والمدود من متن الجمزوري",
            requirementAr = "إتقان جميع أبواب متن تحفة الأطفال",
            requiredBadgeId = "tuhfah_scholar"
        ),
        HafizReward(
            id = "rew_sanad_crown",
            titleAr = "وسام السند المتين",
            subtitleAr = "تاج الاستمرارية لحفاظ القرآن المتعاهدين",
            requirementAr = "الوصول إلى ٤٠٠٠ نقطة إتقان (رتبة سند ومتقن)",
            minXp = 4000L
        ),
        HafizReward(
            id = "rew_khatim_honor",
            titleAr = "إجازة الحفظ والتعهد الشرفية",
            subtitleAr = "أعلى جائزة رقمية في رفيق لخاتمي ٣٠ جزءاً",
            requirementAr = "الوصول إلى ١٠٠٠٠ XP أو إتمام ٣٠ جزءاً",
            minXp = 10000L,
            requiredBadgeId = "full_khatmah_master"
        )
    )

    fun rankForXp(xp: Long): HafizRank =
        ranks.lastOrNull { xp >= it.minXp } ?: ranks.first()

    fun nextRankForXp(xp: Long): HafizRank? =
        ranks.firstOrNull { it.minXp > xp }

    fun badgeById(id: String): HafizBadge? =
        badges.firstOrNull { it.id == id }

    fun isRewardUnlocked(reward: HafizReward, profile: GamificationProfile): Boolean {
        val badgesSet = profile.unlockedBadgeIds()
        val meetsXp = reward.minXp > 0L && profile.xp >= reward.minXp
        val meetsBadge = reward.requiredBadgeId != null && reward.requiredBadgeId in badgesSet
        return when {
            reward.minXp > 0L && reward.requiredBadgeId != null -> meetsXp || meetsBadge
            reward.requiredBadgeId != null -> meetsBadge
            else -> meetsXp
        }
    }

    fun computeUpdatedStreak(lastActiveDateKey: String, todayDateKey: String, currentStreak: Long): Long {
        if (lastActiveDateKey == todayDateKey) return currentStreak.coerceAtLeast(1L)
        if (lastActiveDateKey.isBlank()) return 1L
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        cal.time = fmt.parse(todayDateKey) ?: Date()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = fmt.format(cal.time)
        return if (lastActiveDateKey == yesterdayKey) {
            currentStreak + 1L
        } else {
            1L
        }
    }

    fun evaluateNewBadges(
        profile: GamificationProfile,
        masteredTajweedCount: Int,
        masteredJuzNumbers: Set<Int>,
        timedQuizPassedNow: Boolean = false,
        voiceCorrectUsedNow: Boolean = false,
        mushafSurahsReadCount: Int = 0
    ): List<HafizBadge> {
        val already = profile.unlockedBadgeIds()
        val testedJuz = profile.testedJuzNumbers()
        val combinedJuz = testedJuz + masteredJuzNumbers
        val newlyEarned = mutableListOf<HafizBadge>()

        fun check(id: String, condition: Boolean) {
            if (id !in already && condition) {
                badgeById(id)?.let { newlyEarned.add(it) }
            }
        }

        check("first_test", profile.quizzesCompleted >= 1L)
        check("perfect_score", profile.perfectQuizzes >= 1L)
        check("streak_5_combo", profile.longestQuizCombo >= 5L)
        check("surah_opener", profile.surahOpenersSolved >= 3L)
        check("surah_closer", profile.surahClosersSolved >= 3L)
        check("juz_amma_champ", 30 in testedJuz || 30 in masteredJuzNumbers)
        check("khatmah_explorer", testedJuz.size >= 5)
        check("full_quran_challenger", profile.fullQuranQuizDone)
        check("mutashabihat_hawk", profile.totalCorrectAnswers >= 10L)
        check("tajweed_master", masteredTajweedCount >= 8)
        check("daily_streak_3", profile.currentStreakDays >= 3L)
        check("xp_1000", profile.xp >= 1000L)

        check("timed_challenger", timedQuizPassedNow)
        check("voice_reciter", voiceCorrectUsedNow)
        check("tuhfah_scholar", masteredTajweedCount >= TajweedCatalog.tuhfahChapterCount)
        check("mushaf_explorer", mushafSurahsReadCount >= 3)
        check("mutashabihat_collector", profile.mutashabihatSolved >= 8L)
        check("half_khatmah", combinedJuz.size >= 15)
        check("daily_streak_7", profile.currentStreakDays >= 7L)
        check("full_khatmah_master", masteredJuzNumbers.size >= 30)

        return newlyEarned
    }
}
