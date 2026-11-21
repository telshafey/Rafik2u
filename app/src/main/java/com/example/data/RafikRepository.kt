package com.example.data

import android.content.Context
import com.example.R
import com.example.model.CustomZikr
import com.example.model.DailyWorshipLog
import com.example.model.FavoriteZikr
import com.example.model.GamificationProfile
import com.example.model.JuzMasteryLevel
import com.example.model.JuzProgress
import com.example.model.MoodTag
import com.example.model.QuizAttempt
import com.example.model.ReflectionEntry
import com.example.model.TajweedProgress
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Locale

class RafikRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) : RafikDataSource {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null) {
            check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user operation." }
        }
        return uid
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        userId: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    override fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .logFirestoreErrors(uid, OperationType.GET, docRef.path)
        )
    }

    override suspend fun ensureUserProfile(userId: String, displayName: String): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifEmpty { "رفيق القرآن" }.take(60)
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "dailyRevisionTargetPages" to 20L,
                        "zikrFontScale" to 1.0,
                        "totalRevisionsCount" to 0L,
                        "totalTasbihCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun ensureUserDocIfMissing(uid: String) {
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val safeName = (auth.currentUser?.displayName ?: "").trim().ifEmpty { "رفيق القرآن" }.take(60)
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "dailyRevisionTargetPages" to 20L,
                    "zikrFontScale" to 1.0,
                    "totalRevisionsCount" to 0L,
                    "totalTasbihCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    override suspend fun updateRevisionGoal(userId: String, pagesGoal: Long): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val clamped = pagesGoal.coerceIn(1L, 100L)
            ensureUserDocIfMissing(uid)
            db.collection("users").document(uid).update(
                mapOf(
                    "dailyRevisionTargetPages" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override suspend fun updateFontScale(userId: String, fontScale: Double): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val clamped = fontScale.coerceIn(0.8, 1.8)
            ensureUserDocIfMissing(uid)
            db.collection("users").document(uid).update(
                mapOf(
                    "zikrFontScale" to clamped,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    override fun observeJuzProgress(userId: String): Flow<Map<Int, JuzProgress>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/juzProgress"
        emitAll(
            db.collection("users").document(uid).collection("juzProgress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        JuzProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associateBy { it.juzNumber.toInt() }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    override suspend fun recordJuzRevision(
        userId: String,
        juzNumber: Int,
        masteryLevel: JuzMasteryLevel,
        mistakeNote: String,
        incrementRevision: Boolean,
        dateKey: String
    ): Result<Unit> {
        val juzDocId = String.format(Locale.US, "juz_%02d", juzNumber)
        val path = "users/$userId/juzProgress/$juzDocId"
        return try {
            require(juzNumber in 1..30) { "Juz number must be between 1 and 30" }
            val uid = requireUserId(userId)
            val cleanNote = mistakeNote.trim().take(400)
            val nowMs = System.currentTimeMillis()
            val juzRef = db.collection("users").document(uid).collection("juzProgress").document(juzDocId)
            val snap = juzRef.get().await()

            if (!snap.exists()) {
                juzRef.set(
                    mapOf(
                        "userId" to uid,
                        "juzNumber" to juzNumber.toLong(),
                        "masteryLevel" to masteryLevel.key,
                        "revisionCount" to if (incrementRevision) 1L else 0L,
                        "mistakeNote" to cleanNote,
                        "lastReviewedEpochMs" to if (incrementRevision) nowMs else 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "masteryLevel" to masteryLevel.key,
                    "mistakeNote" to cleanNote,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (incrementRevision) {
                    updates["revisionCount"] = FieldValue.increment(1L)
                    updates["lastReviewedEpochMs"] = nowMs
                }
                juzRef.update(updates).await()
            }

            if (incrementRevision) {
                ensureUserDocIfMissing(uid)
                db.collection("users").document(uid).update(
                    mapOf(
                        "totalRevisionsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                addRevisedPagesToday(uid, dateKey, 20L)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeTajweedProgress(userId: String): Flow<Map<String, Boolean>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/tajweedProgress"
        emitAll(
            db.collection("users").document(uid).collection("tajweedProgress")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        TajweedProgress::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).associate { it.ruleKey to it.mastered }
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    override suspend fun toggleTajweedMastery(
        userId: String,
        ruleKey: String,
        mastered: Boolean
    ): Result<Unit> {
        val path = "users/$userId/tajweedProgress/$ruleKey"
        return try {
            val uid = requireUserId(userId)
            require(ruleKey.matches(Regex("^[a-zA-Z0-9_\\-]{1,60}$"))) { "Invalid ruleKey" }
            val docRef = db.collection("users").document(uid).collection("tajweedProgress").document(ruleKey)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "ruleKey" to ruleKey,
                        "mastered" to mastered,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "mastered" to mastered,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeDailyLog(userId: String, dateKey: String): Flow<DailyWorshipLog> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        DailyWorshipLog::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: DailyWorshipLog(userId = uid, dateKey = dateKey)
                }
                .logFirestoreErrors(uid, OperationType.GET, docRef.path)
        )
    }

    private suspend fun addRevisedPagesToday(uid: String, dateKey: String, deltaPages: Long) {
        val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
        val snap = logRef.get().await()
        if (!snap.exists()) {
            logRef.set(
                mapOf(
                    "userId" to uid,
                    "dateKey" to dateKey,
                    "pagesRevisedToday" to deltaPages.coerceIn(0L, 604L),
                    "morningAzkarDone" to false,
                    "eveningAzkarDone" to false,
                    "tasbihDoneToday" to 0L,
                    "tajweedQuizCorrect" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val current = snap.getLong("pagesRevisedToday") ?: 0L
            val next = (current + deltaPages).coerceIn(0L, 604L)
            logRef.update(
                mapOf(
                    "pagesRevisedToday" to next,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    override suspend fun updatePagesRevisedToday(userId: String, dateKey: String, pages: Long): Result<Unit> {
        val path = "users/$userId/dailyLogs/$dateKey"
        return try {
            val uid = requireUserId(userId)
            val clamped = pages.coerceIn(0L, 604L)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            val snap = logRef.get().await()
            if (!snap.exists()) {
                logRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "pagesRevisedToday" to clamped,
                        "morningAzkarDone" to false,
                        "eveningAzkarDone" to false,
                        "tasbihDoneToday" to 0L,
                        "tajweedQuizCorrect" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                logRef.update(
                    mapOf(
                        "pagesRevisedToday" to clamped,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun toggleAzkarStatus(
        userId: String,
        dateKey: String,
        field: String,
        value: Boolean
    ): Result<Unit> {
        val path = "users/$userId/dailyLogs/$dateKey"
        return try {
            require(field == "morningAzkarDone" || field == "eveningAzkarDone")
            val uid = requireUserId(userId)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            val snap = logRef.get().await()
            if (!snap.exists()) {
                val base = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "dateKey" to dateKey,
                    "pagesRevisedToday" to 0L,
                    "morningAzkarDone" to false,
                    "eveningAzkarDone" to false,
                    "tasbihDoneToday" to 0L,
                    "tajweedQuizCorrect" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                base[field] = value
                logRef.set(base).await()
            } else {
                logRef.update(
                    mapOf(
                        field to value,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordTajweedQuizHit(userId: String, dateKey: String): Result<Unit> {
        val path = "users/$userId/dailyLogs/$dateKey"
        return try {
            val uid = requireUserId(userId)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            val snap = logRef.get().await()
            if (!snap.exists()) {
                logRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "pagesRevisedToday" to 0L,
                        "morningAzkarDone" to false,
                        "eveningAzkarDone" to false,
                        "tasbihDoneToday" to 0L,
                        "tajweedQuizCorrect" to 1L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                logRef.update(
                    mapOf(
                        "tajweedQuizCorrect" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun recordTasbihCount(userId: String, dateKey: String, delta: Long): Result<Unit> {
        val path = "users/$userId/dailyLogs/$dateKey"
        return try {
            require(delta in 1L..1000L)
            val uid = requireUserId(userId)
            val logRef = db.collection("users").document(uid).collection("dailyLogs").document(dateKey)
            val userRef = db.collection("users").document(uid)
            val snap = logRef.get().await()
            if (!snap.exists()) {
                logRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "pagesRevisedToday" to 0L,
                        "morningAzkarDone" to false,
                        "eveningAzkarDone" to false,
                        "tasbihDoneToday" to delta,
                        "tajweedQuizCorrect" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                logRef.update(
                    mapOf(
                        "tasbihDoneToday" to FieldValue.increment(delta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            ensureUserDocIfMissing(uid)
            userRef.update(
                mapOf(
                    "totalTasbihCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override fun observeCustomAzkar(userId: String): Flow<List<CustomZikr>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/customAzkar"
        emitAll(
            db.collection("users").document(uid).collection("customAzkar")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomZikr::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    override suspend fun addCustomZikr(
        userId: String,
        text: String,
        virtue: String,
        targetCount: Long
    ): Result<Unit> {
        val path = "users/$userId/customAzkar"
        return try {
            val uid = requireUserId(userId)
            val cleanText = text.trim()
            require(cleanText.length in 2..500) { "النص يجب أن يكون بين حرفين و٥٠٠ حرف." }
            val cleanVirtue = virtue.trim().take(300)
            val safeTarget = targetCount.coerceIn(1L, 10000L)
            val docRef = db.collection("users").document(uid).collection("customAzkar").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "text" to cleanText,
                    "virtue" to cleanVirtue,
                    "targetCount" to safeTarget,
                    "totalCompleted" to 0L,
                    "favorite" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun incrementCustomZikrCount(userId: String, zikrId: String): Result<Unit> {
        val path = "users/$userId/customAzkar/$zikrId"
        return try {
            val uid = requireUserId(userId)
            require(zikrId.isNotBlank())
            db.collection("users").document(uid).collection("customAzkar").document(zikrId)
                .update(
                    mapOf(
                        "totalCompleted" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteCustomZikr(userId: String, zikrId: String): Result<Unit> {
        val path = "users/$userId/customAzkar/$zikrId"
        return try {
            val uid = requireUserId(userId)
            require(zikrId.isNotBlank())
            db.collection("users").document(uid).collection("customAzkar").document(zikrId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override fun observeReflections(userId: String): Flow<List<ReflectionEntry>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/reflections"
        emitAll(
            db.collection("users").document(uid).collection("reflections")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ReflectionEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    override suspend fun addReflection(
        userId: String,
        title: String,
        body: String,
        moodTag: MoodTag,
        dateKey: String
    ): Result<Unit> {
        val path = "users/$userId/reflections"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanBody = body.trim()
            require(cleanTitle.length in 1..100) { "العنوان مطلوب (حتى ١٠٠ حرف)." }
            require(cleanBody.length in 1..1500) { "الملاحظة مطلوبة (حتى ١٥٠٠ حرف)." }
            val docRef = db.collection("users").document(uid).collection("reflections").document()
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "body" to cleanBody,
                    "moodTag" to moodTag.key,
                    "dateKey" to dateKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    override suspend fun updateReflection(
        userId: String,
        reflectionId: String,
        title: String,
        body: String,
        moodTag: MoodTag
    ): Result<Unit> {
        val path = "users/$userId/reflections/$reflectionId"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanBody = body.trim()
            require(reflectionId.isNotBlank())
            require(cleanTitle.length in 1..100)
            require(cleanBody.length in 1..1500)
            db.collection("users").document(uid).collection("reflections").document(reflectionId)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "body" to cleanBody,
                        "moodTag" to moodTag.key,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    override suspend fun deleteReflection(userId: String, reflectionId: String): Result<Unit> {
        val path = "users/$userId/reflections/$reflectionId"
        return try {
            val uid = requireUserId(userId)
            require(reflectionId.isNotBlank())
            db.collection("users").document(uid).collection("reflections").document(reflectionId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    override fun observeFavoriteKeys(userId: String): Flow<Set<String>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/favoriteAzkar"
        emitAll(
            db.collection("users").document(uid).collection("favoriteAzkar")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FavoriteZikr::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).map { it.zikrKey }.toSet()
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    override suspend fun toggleFavoriteZikr(
        userId: String,
        zikrKey: String,
        categoryKey: String,
        currentlyFavorite: Boolean
    ): Result<Unit> {
        val path = "users/$userId/favoriteAzkar/$zikrKey"
        return try {
            val uid = requireUserId(userId)
            require(zikrKey.matches(Regex("^[a-zA-Z0-9_\\-]{1,80}$")))
            val docRef = db.collection("users").document(uid).collection("favoriteAzkar").document(zikrKey)
            if (currentlyFavorite) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "zikrKey" to zikrKey,
                        "categoryKey" to categoryKey.take(40),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override fun observeGamificationProfile(userId: String): Flow<GamificationProfile> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("gamification").document("summary")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        GamificationProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: GamificationProfile(userId = uid)
                }
                .logFirestoreErrors(uid, OperationType.GET, docRef.path)
        )
    }

    override fun observeQuizAttempts(userId: String): Flow<List<QuizAttempt>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/quizAttempts"
        emitAll(
            db.collection("users").document(uid).collection("quizAttempts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(20)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        QuizAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .logFirestoreErrors(uid, OperationType.LIST, path)
        )
    }

    override suspend fun saveGamificationAndAttempt(
        userId: String,
        updatedProfile: GamificationProfile,
        attempt: QuizAttempt?
    ): Result<Unit> {
        val path = "users/$userId/gamification/summary"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("gamification").document("summary")
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "xp" to updatedProfile.xp.coerceIn(0L, 10_000_000L),
                        "currentStreakDays" to updatedProfile.currentStreakDays.coerceIn(0L, 10_000L),
                        "longestQuizCombo" to updatedProfile.longestQuizCombo.coerceIn(0L, 10_000L),
                        "quizzesCompleted" to updatedProfile.quizzesCompleted.coerceIn(0L, 1_000_000L),
                        "perfectQuizzes" to updatedProfile.perfectQuizzes.coerceIn(0L, 1_000_000L),
                        "totalCorrectAnswers" to updatedProfile.totalCorrectAnswers.coerceIn(0L, 10_000_000L),
                        "surahOpenersSolved" to updatedProfile.surahOpenersSolved.coerceIn(0L, 1_000_000L),
                        "surahClosersSolved" to updatedProfile.surahClosersSolved.coerceIn(0L, 1_000_000L),
                        "mutashabihatSolved" to updatedProfile.mutashabihatSolved.coerceIn(0L, 1_000_000L),
                        "testedJuzCsv" to updatedProfile.testedJuzCsv.take(300),
                        "fullQuranQuizDone" to updatedProfile.fullQuranQuizDone,
                        "unlockedBadgesCsv" to updatedProfile.unlockedBadgesCsv.take(1000),
                        "lastActiveDateKey" to updatedProfile.lastActiveDateKey,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "xp" to updatedProfile.xp.coerceIn(0L, 10_000_000L),
                        "currentStreakDays" to updatedProfile.currentStreakDays.coerceIn(0L, 10_000L),
                        "longestQuizCombo" to updatedProfile.longestQuizCombo.coerceIn(0L, 10_000L),
                        "quizzesCompleted" to updatedProfile.quizzesCompleted.coerceIn(0L, 1_000_000L),
                        "perfectQuizzes" to updatedProfile.perfectQuizzes.coerceIn(0L, 1_000_000L),
                        "totalCorrectAnswers" to updatedProfile.totalCorrectAnswers.coerceIn(0L, 10_000_000L),
                        "surahOpenersSolved" to updatedProfile.surahOpenersSolved.coerceIn(0L, 1_000_000L),
                        "surahClosersSolved" to updatedProfile.surahClosersSolved.coerceIn(0L, 1_000_000L),
                        "mutashabihatSolved" to updatedProfile.mutashabihatSolved.coerceIn(0L, 1_000_000L),
                        "testedJuzCsv" to updatedProfile.testedJuzCsv.take(300),
                        "fullQuranQuizDone" to updatedProfile.fullQuranQuizDone,
                        "unlockedBadgesCsv" to updatedProfile.unlockedBadgesCsv.take(1000),
                        "lastActiveDateKey" to updatedProfile.lastActiveDateKey,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (attempt != null) {
                db.collection("users").document(uid).collection("quizAttempts").document().set(
                    mapOf(
                        "userId" to uid,
                        "scopeLabel" to attempt.scopeLabel.take(120).ifBlank { "اختبار قرآني" },
                        "questionTypesCsv" to attempt.questionTypesCsv.take(120).ifBlank { "MIXED" },
                        "score" to attempt.score.coerceIn(0L, 100L),
                        "totalQuestions" to attempt.totalQuestions.coerceIn(1L, 100L),
                        "xpEarned" to attempt.xpEarned.coerceIn(0L, 50_000L),
                        "bestCombo" to attempt.bestCombo.coerceIn(0L, 100L),
                        "dateKey" to attempt.dateKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    override suspend fun verifyCloudSync(userId: String): Result<Long> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val startMs = System.currentTimeMillis()
            val snap = db.collection("users").document(uid).get(Source.SERVER).await()
            if (!snap.exists()) {
                ensureUserDocIfMissing(uid)
            }
            val elapsedMs = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            Result.success(elapsedMs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }
}
