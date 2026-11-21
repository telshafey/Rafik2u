package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val dailyRevisionTargetPages: Long = 20L,
    val zikrFontScale: Double = 1.0,
    val totalRevisionsCount: Long = 0L,
    val totalTasbihCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class JuzMasteryLevel(val key: String, val labelAr: String) {
    NOT_STARTED("not_started", "لم يبدأ"),
    MEMORIZING("memorizing", "قيد الحفظ"),
    NEEDS_REVIEW("needs_review", "يحتاج تثبيت"),
    MASTERED("mastered", "متقن");

    companion object {
        fun fromRaw(raw: String): JuzMasteryLevel =
            entries.firstOrNull { it.key == raw } ?: NOT_STARTED
    }
}

@Keep
@IgnoreExtraProperties
data class JuzProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val juzNumber: Long = 1L,
    val masteryLevel: String = JuzMasteryLevel.NOT_STARTED.key,
    val revisionCount: Long = 0L,
    val mistakeNote: String = "",
    val lastReviewedEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TajweedProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ruleKey: String = "",
    val mastered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyWorshipLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val pagesRevisedToday: Long = 0L,
    val morningAzkarDone: Boolean = false,
    val eveningAzkarDone: Boolean = false,
    val tasbihDoneToday: Long = 0L,
    val tajweedQuizCorrect: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CustomZikr(
    @DocumentId val id: String = "",
    val userId: String = "",
    val text: String = "",
    val virtue: String = "",
    val targetCount: Long = 33L,
    val totalCompleted: Long = 0L,
    val favorite: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class MoodTag(val key: String, val labelAr: String) {
    AYAH_FIX("ayah_fix", "ضبط آية"),
    MUTASHABIHAT("mutashabihat", "متشابهات"),
    TAJWEED("tajweed", "تجويد"),
    GRATEFUL("grateful", "خاطرة وشكر");

    companion object {
        fun fromRaw(raw: String): MoodTag =
            entries.firstOrNull { it.key == raw } ?: AYAH_FIX
    }
}

@Keep
@IgnoreExtraProperties
data class ReflectionEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val moodTag: String = MoodTag.AYAH_FIX.key,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FavoriteZikr(
    @DocumentId val id: String = "",
    val userId: String = "",
    val zikrKey: String = "",
    val categoryKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class QuranJuzInfo(
    val number: Int,
    val nameAr: String,
    val surahRangeAr: String,
    val pageCount: Int = 20
)

data class MutashabihItem(
    val id: String,
    val title: String,
    val firstPlace: String,
    val secondPlace: String,
    val memoryTip: String,
    val juzRangeAr: String = ""
)

enum class TajweedCategory(val key: String, val titleAr: String) {
    NOON_TANWEEN("noon", "النون الساكنة والتنوين"),
    MEEM_SHADDAH("meem", "الميم الساكنة والغنة"),
    MADD("madd", "أحكام المدود"),
    QALQALAH_TAFKHIM("tafkheem", "القلقلة والتفخيم"),
    WAQF("waqf", "علامات الوقف والمخارج")
}

data class TajweedRuleItem(
    val key: String,
    val category: TajweedCategory,
    val titleAr: String,
    val lettersAr: String,
    val explanationAr: String,
    val examplesAr: String,
    val commonMistakeAr: String,
    val matnChapterTitleAr: String = "",
    val matnVerses: List<String> = emptyList()
)

data class TajweedQuestion(
    val id: String,
    val promptAr: String,
    val options: List<String>,
    val correctIndex: Int,
    val rationaleAr: String
)

enum class SunnahDuaCategory(val key: String, val titleAr: String) {
    QURAN_VIRTUES("quran_virtues", "فضائل القرآن وتعاهده"),
    SAHIH_SUNNAH("sahih_sunnah", "جوامع البخاري ومسلم"),
    MEMORIZATION_DUAS("mem_duas", "أدعية التثبيت والقرآن"),
    DAILY_AZKAR("daily_azkar", "الأذكار الصحيحة")
}

data class SunnahDuaItem(
    val key: String,
    val category: SunnahDuaCategory,
    val titleAr: String,
    val arabicText: String,
    val sourceAr: String,
    val benefitAr: String,
    val repeatCount: Int = 1
)

data class TasbihPhrase(
    val id: String,
    val text: String,
    val virtue: String,
    val defaultTarget: Int,
    val isCustom: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class GamificationProfile(
    @DocumentId val id: String = "summary",
    val userId: String = "",
    val xp: Long = 0L,
    val currentStreakDays: Long = 0L,
    val longestQuizCombo: Long = 0L,
    val quizzesCompleted: Long = 0L,
    val perfectQuizzes: Long = 0L,
    val totalCorrectAnswers: Long = 0L,
    val surahOpenersSolved: Long = 0L,
    val surahClosersSolved: Long = 0L,
    val mutashabihatSolved: Long = 0L,
    val testedJuzCsv: String = "",
    val fullQuranQuizDone: Boolean = false,
    val unlockedBadgesCsv: String = "",
    val lastActiveDateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun unlockedBadgeIds(): Set<String> =
        unlockedBadgesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    fun testedJuzNumbers(): Set<Int> =
        testedJuzCsv.split(',').mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..30 }.toSet()
}

@Keep
@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val scopeLabel: String = "",
    val questionTypesCsv: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 1L,
    val xpEarned: Long = 0L,
    val bestCombo: Long = 0L,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class SurahInfo(
    val number: Int,
    val nameAr: String,
    val ayahCount: Int,
    val juzList: List<Int>
)

data class MushafAyah(
    val surahNumber: Int,
    val surahNameAr: String,
    val numberInSurah: Int,
    val juzNumber: Int,
    val textUthmani: String
)

enum class QuranQuizScopeMode(val labelAr: String) {
    SINGLE_JUZ("جزء معين"),
    MULTI_JUZ("أجزاء معينة"),
    FULL_QURAN("كامل القرآن")
}

enum class QuranQuizTimerMode(
    val labelAr: String,
    val shortDescAr: String,
    val secondsLimit: Int,
    val isPerQuestion: Boolean,
    val xpBonusPercent: Int
) {
    UNTIMED("بلا وقت", "مراجعة هادئة دون مؤقت", 0, false, 0),
    PER_QUESTION_30S("٣٠ ث/سؤال", "تحدي السرعة (+٥٠٪ XP)", 30, true, 50),
    PER_QUESTION_60S("٦٠ ث/سؤال", "تحدي التركيز (+٢٥٪ XP)", 60, true, 25),
    TOTAL_EXAM_3M("٣ دقائق للكل", "سباق الاختبار الكامل (+٤٠٪ XP)", 180, false, 40)
}

enum class QuranQuestionType(val labelAr: String, val shortDescAr: String) {
    COMPLETE_AYAH("أكمل الآية", "تبدأ بالآية وتكملها كتابةً أو صوتاً"),
    MULTIPLE_CHOICE("اختيار من متعدد", "اختر التكملة أو الكلمة الصحيحة"),
    SURAH_BEGINNING("أول السورة", "اكتب أو سمّع أول آيات من سورة"),
    SURAH_LAST_5("آخر ٥ آيات", "اكتب أو سمّع آخر ٥ آيات من سورة")
}

data class QuranQuizQuestion(
    val id: String,
    val type: QuranQuestionType,
    val juzNumber: Int,
    val surahNumber: Int,
    val surahNameAr: String,
    val ayahRangeLabel: String,
    val headerLabelAr: String,
    val promptTextAr: String,
    val hintTextAr: String,
    val expectedAnswerAr: String,
    val options: List<String> = emptyList(),
    val correctOptionIndex: Int = -1
)

data class WordComparisonToken(
    val expectedWord: String,
    val matched: Boolean
)

data class AyahGradingResult(
    val accuracyPercent: Int,
    val passed: Boolean,
    val masteryVerdictAr: String,
    val tokens: List<WordComparisonToken>
)

data class HafizRank(
    val level: Int,
    val titleAr: String,
    val minXp: Long
)

data class HafizBadge(
    val id: String,
    val titleAr: String,
    val descriptionAr: String,
    val xpBonus: Long
)

data class HafizReward(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val requirementAr: String,
    val minXp: Long = 0L,
    val requiredBadgeId: String? = null
)

data class QuizSessionSummary(
    val scopeLabel: String,
    val score: Int,
    val totalQuestions: Int,
    val bestCombo: Int,
    val baseXp: Long,
    val comboBonusXp: Long,
    val perfectBonusXp: Long,
    val timeBonusXp: Long = 0L,
    val voiceBonusXp: Long = 0L,
    val totalXpEarned: Long,
    val newlyUnlockedBadges: List<HafizBadge>,
    val usedOfflineFallback: Boolean,
    val timerModeLabelAr: String = ""
)

