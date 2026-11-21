package com.example.data

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

interface RafikDataSource {
    fun observeUserProfile(userId: String): Flow<UserProfile?>
    suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit>
    suspend fun updateRevisionGoal(userId: String, pagesGoal: Long): Result<Unit>
    suspend fun updateFontScale(userId: String, fontScale: Double): Result<Unit>

    fun observeJuzProgress(userId: String): Flow<Map<Int, JuzProgress>>
    suspend fun recordJuzRevision(
        userId: String,
        juzNumber: Int,
        masteryLevel: JuzMasteryLevel,
        mistakeNote: String,
        incrementRevision: Boolean,
        dateKey: String
    ): Result<Unit>

    fun observeTajweedProgress(userId: String): Flow<Map<String, Boolean>>
    suspend fun toggleTajweedMastery(userId: String, ruleKey: String, mastered: Boolean): Result<Unit>

    fun observeDailyLog(userId: String, dateKey: String): Flow<DailyWorshipLog>
    suspend fun updatePagesRevisedToday(userId: String, dateKey: String, pages: Long): Result<Unit>
    suspend fun toggleAzkarStatus(userId: String, dateKey: String, field: String, value: Boolean): Result<Unit>
    suspend fun recordTajweedQuizHit(userId: String, dateKey: String): Result<Unit>
    suspend fun recordTasbihCount(userId: String, dateKey: String, delta: Long): Result<Unit>

    fun observeCustomAzkar(userId: String): Flow<List<CustomZikr>>
    suspend fun addCustomZikr(userId: String, text: String, virtue: String, targetCount: Long): Result<Unit>
    suspend fun incrementCustomZikrCount(userId: String, zikrId: String): Result<Unit>
    suspend fun deleteCustomZikr(userId: String, zikrId: String): Result<Unit>

    fun observeReflections(userId: String): Flow<List<ReflectionEntry>>
    suspend fun addReflection(userId: String, title: String, body: String, moodTag: MoodTag, dateKey: String): Result<Unit>
    suspend fun updateReflection(userId: String, reflectionId: String, title: String, body: String, moodTag: MoodTag): Result<Unit>
    suspend fun deleteReflection(userId: String, reflectionId: String): Result<Unit>

    fun observeFavoriteKeys(userId: String): Flow<Set<String>>
    suspend fun toggleFavoriteZikr(userId: String, zikrKey: String, categoryKey: String, currentlyFavorite: Boolean): Result<Unit>

    fun observeGamificationProfile(userId: String): Flow<GamificationProfile>
    fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>>
    suspend fun saveGamificationAndAttempt(
        userId: String,
        updatedProfile: GamificationProfile,
        attempt: QuizAttempt?
    ): Result<Unit>

    suspend fun verifyCloudSync(userId: String): Result<Long>
}
