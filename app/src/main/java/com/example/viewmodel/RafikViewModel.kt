package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ArabicQuranNormalizer
import com.example.data.GamificationCatalog
import com.example.data.QuranCatalog
import com.example.data.QuranMushafRepository
import com.example.data.QuranQuizGenerator
import com.example.data.RafikDataSource
import com.example.data.SunnahDuaCatalog
import com.example.model.AyahGradingResult
import com.example.model.CustomZikr
import com.example.model.DailyWorshipLog
import com.example.model.GamificationProfile
import com.example.model.JuzMasteryLevel
import com.example.model.JuzProgress
import com.example.model.MoodTag
import com.example.model.MushafAyah
import com.example.model.QuizAttempt
import com.example.model.QuizSessionSummary
import com.example.model.QuranJuzInfo
import com.example.model.QuranQuestionType
import com.example.model.QuranQuizQuestion
import com.example.model.QuranQuizScopeMode
import com.example.model.QuranQuizTimerMode
import com.example.model.ReflectionEntry
import com.example.model.SurahInfo
import com.example.model.TasbihPhrase
import com.example.model.UiState
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TasbihSessionState(
    val selectedPhrase: TasbihPhrase = SunnahDuaCatalog.tasbihPhrases.first(),
    val currentCount: Int = 0,
    val targetCount: Int = 33
)

data class SmartJuzRecommendation(
    val juzInfo: QuranJuzInfo,
    val progress: JuzProgress?,
    val reasonAr: String
)

sealed interface CloudSyncStatus {
    data object Idle : CloudSyncStatus
    data object Checking : CloudSyncStatus
    data class Verified(val latencyMs: Long) : CloudSyncStatus
    data class Failed(val errorMessage: String) : CloudSyncStatus
}

private data class QuizLaunchConfig(
    val scopeMode: QuranQuizScopeMode,
    val selectedJuz: Set<Int>,
    val optionalSurahNumber: Int?,
    val selectedTypes: Set<QuranQuestionType>,
    val questionCount: Int,
    val timerMode: QuranQuizTimerMode = QuranQuizTimerMode.UNTIMED
)

sealed interface MushafReaderUiState {
    data object Index : MushafReaderUiState
    data class Loading(val surahInfo: SurahInfo) : MushafReaderUiState
    data class Reading(
        val surahInfo: SurahInfo,
        val ayahs: List<MushafAyah>,
        val usedOfflineFallback: Boolean
    ) : MushafReaderUiState
    data class Error(val message: String) : MushafReaderUiState
}

sealed interface QuranQuizArenaState {
    data object Setup : QuranQuizArenaState
    data class Loading(val scopeLabel: String) : QuranQuizArenaState
    data class Active(
        val scopeLabel: String,
        val isFullQuran: Boolean,
        val testedJuzNumbers: Set<Int>,
        val selectedTypes: Set<QuranQuestionType>,
        val usedOfflineFallback: Boolean,
        val questions: List<QuranQuizQuestion>,
        val currentIndex: Int = 0,
        val selectedMcOption: Int? = null,
        val writtenAnswer: String = "",
        val showHint: Boolean = false,
        val submitted: Boolean = false,
        val lastMcCorrect: Boolean = false,
        val lastGrading: AyahGradingResult? = null,
        val score: Int = 0,
        val currentCombo: Int = 0,
        val bestCombo: Int = 0,
        val openersSolved: Int = 0,
        val closersSolved: Int = 0,
        val timerMode: QuranQuizTimerMode = QuranQuizTimerMode.UNTIMED,
        val remainingSeconds: Int = 0,
        val totalTimerSeconds: Int = 0,
        val timedOutCount: Int = 0,
        val voiceCorrectIndices: Set<Int> = emptySet(),
        val lastAnsweredByVoice: Boolean = false
    ) : QuranQuizArenaState {
        val currentQuestion: QuranQuizQuestion get() = questions[currentIndex]
    }
    data class Completed(val summary: QuizSessionSummary) : QuranQuizArenaState
    data class Error(val message: String) : QuranQuizArenaState
}

class RafikViewModel(
    private val repository: RafikDataSource,
    private val mushafRepository: QuranMushafRepository,
    private val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    val todayKey: String = QuranCatalog.todayDateKey()

    private val _statusMessages = MutableSharedFlow<String>(extraBufferCapacity = 6)
    val statusMessages: SharedFlow<String> = _statusMessages.asSharedFlow()

    private val _zikrCounters = MutableStateFlow<Map<String, Int>>(emptyMap())
    val zikrCounters: StateFlow<Map<String, Int>> = _zikrCounters.asStateFlow()

    private val _tasbihState = MutableStateFlow(TasbihSessionState())
    val tasbihState: StateFlow<TasbihSessionState> = _tasbihState.asStateFlow()

    private val _quizArenaState = MutableStateFlow<QuranQuizArenaState>(QuranQuizArenaState.Setup)
    val quizArenaState: StateFlow<QuranQuizArenaState> = _quizArenaState.asStateFlow()

    private val _reviewedMutashabihatIds = MutableStateFlow<Set<String>>(emptySet())
    val reviewedMutashabihatIds: StateFlow<Set<String>> = _reviewedMutashabihatIds.asStateFlow()

    private val _mushafReaderState = MutableStateFlow<MushafReaderUiState>(MushafReaderUiState.Index)
    val mushafReaderState: StateFlow<MushafReaderUiState> = _mushafReaderState.asStateFlow()

    private val _lastReadSurahNumber = MutableStateFlow(1)
    val lastReadSurahNumber: StateFlow<Int> = _lastReadSurahNumber.asStateFlow()

    private val _readSurahsSet = MutableStateFlow<Set<Int>>(emptySet())
    val readSurahsCount: StateFlow<Int> = _readSurahsSet
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _cloudSyncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Idle)
    val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

    private var lastQuizConfig: QuizLaunchConfig? = null
    private var quizTimerJob: Job? = null
    private var pendingTasbihDelta: Long = 0L
    private var tasbihFlushJob: Job? = null

    val userProfileState: StateFlow<UiState<UserProfile>> =
        repository.observeUserProfile(currentUserId)
            .map<UserProfile?, UiState<UserProfile>> { profile ->
                UiState.Success(
                    profile ?: UserProfile(
                        userId = currentUserId,
                        displayName = initialDisplayName.ifBlank { "رفيق القرآن" }
                    )
                )
            }
            .unwrapFirestoreErrors("observeUserProfile")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val juzProgressState: StateFlow<UiState<Map<Int, JuzProgress>>> =
        repository.observeJuzProgress(currentUserId)
            .map<Map<Int, JuzProgress>, UiState<Map<Int, JuzProgress>>> { UiState.Success(it) }
            .unwrapFirestoreErrors("observeJuzProgress")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val tajweedProgressState: StateFlow<Map<String, Boolean>> =
        repository.observeTajweedProgress(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyMap())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyMap()
            )

    val dailyLogState: StateFlow<UiState<DailyWorshipLog>> =
        repository.observeDailyLog(currentUserId, todayKey)
            .map<DailyWorshipLog, UiState<DailyWorshipLog>> { UiState.Success(it) }
            .unwrapFirestoreErrors("observeDailyLog")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val customAzkarState: StateFlow<UiState<List<CustomZikr>>> =
        repository.observeCustomAzkar(currentUserId)
            .map<List<CustomZikr>, UiState<List<CustomZikr>>> { UiState.Success(it) }
            .unwrapFirestoreErrors("observeCustomAzkar")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val reflectionsState: StateFlow<UiState<List<ReflectionEntry>>> =
        repository.observeReflections(currentUserId)
            .map<List<ReflectionEntry>, UiState<List<ReflectionEntry>>> { UiState.Success(it) }
            .unwrapFirestoreErrors("observeReflections")
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val favoriteKeysState: StateFlow<Set<String>> =
        repository.observeFavoriteKeys(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val gamificationState: StateFlow<GamificationProfile> =
        repository.observeGamificationProfile(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(GamificationProfile(userId = currentUserId))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = GamificationProfile(userId = currentUserId)
            )

    val quizAttemptsState: StateFlow<List<QuizAttempt>> =
        repository.observeQuizAttempts(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, initialDisplayName)
                .onFailure {
                    _statusMessages.tryEmit("تعذر مزامنة الملف الشخصي: ${it.localizedMessage ?: "خطأ في الاتصال"}")
                }
        }
    }

    private fun <T> Flow<UiState<T>>.unwrapFirestoreErrors(label: String): Flow<UiState<T>> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error in $label", rootError)
            emit(UiState.Error(rootError.message ?: "حدث خطأ أثناء تحميل البيانات"))
        }

    // ---- Full 114-Surah Mushaf Reader Logic ----

    fun openSurahInMushaf(surahNumber: Int) {
        val safeNumber = surahNumber.coerceIn(1, 114)
        val surahInfo = QuranCatalog.allSurahs.firstOrNull { it.number == safeNumber }
            ?: QuranCatalog.allSurahs.first()
        _lastReadSurahNumber.value = safeNumber
        viewModelScope.launch {
            _mushafReaderState.value = MushafReaderUiState.Loading(surahInfo)
            try {
                val res = mushafRepository.loadSurahForReader(safeNumber)
                _mushafReaderState.value = MushafReaderUiState.Reading(
                    surahInfo = res.surahInfo,
                    ayahs = res.ayahs,
                    usedOfflineFallback = res.usedOfflineFallback
                )
                if (res.ayahs.isNotEmpty() && safeNumber !in _readSurahsSet.value) {
                    val updatedSet = _readSurahsSet.value + safeNumber
                    _readSurahsSet.value = updatedSet
                    awardActionXp(15L, mushafSurahsCountOverride = updatedSet.size)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed opening surah $safeNumber in Mushaf", e)
                _mushafReaderState.value = MushafReaderUiState.Error(
                    e.localizedMessage ?: "تعذر تحميل السورة حالياً"
                )
            }
        }
    }

    fun closeMushafSurah() {
        _mushafReaderState.value = MushafReaderUiState.Index
    }

    // ---- Quran Exam Arena Logic ----

    fun startQuranQuiz(
        scopeMode: QuranQuizScopeMode,
        selectedJuz: Set<Int>,
        optionalSurahNumber: Int?,
        selectedTypes: Set<QuranQuestionType>,
        questionCount: Int,
        timerMode: QuranQuizTimerMode = QuranQuizTimerMode.UNTIMED
    ) {
        quizTimerJob?.cancel()
        lastQuizConfig = QuizLaunchConfig(
            scopeMode = scopeMode,
            selectedJuz = selectedJuz,
            optionalSurahNumber = optionalSurahNumber,
            selectedTypes = selectedTypes,
            questionCount = questionCount,
            timerMode = timerMode
        )
        val targetJuzSet: Set<Int> = when (scopeMode) {
            QuranQuizScopeMode.SINGLE_JUZ -> setOf(selectedJuz.firstOrNull()?.coerceIn(1, 30) ?: 30)
            QuranQuizScopeMode.MULTI_JUZ -> selectedJuz.filter { it in 1..30 }.toSet().ifEmpty { setOf(30) }
            QuranQuizScopeMode.FULL_QURAN -> (1..30).shuffled().take(5).toSet()
        }

        val scopeLabel = when {
            optionalSurahNumber != null -> {
                val surahName = QuranCatalog.allSurahs.firstOrNull { it.number == optionalSurahNumber }?.nameAr ?: ""
                "سورة $surahName"
            }
            scopeMode == QuranQuizScopeMode.FULL_QURAN -> "كامل القرآن الكريم"
            targetJuzSet.size == 1 -> "الجزء ${targetJuzSet.first()}"
            else -> "${targetJuzSet.size} أجزاء مختارة (${targetJuzSet.sorted().take(3).joinToString("، ")}${if (targetJuzSet.size > 3) "..." else ""})"
        }

        viewModelScope.launch {
            _quizArenaState.value = QuranQuizArenaState.Loading(scopeLabel)
            try {
                val fetchResult = mushafRepository.loadScopeVerses(
                    juzNumbers = targetJuzSet,
                    specificSurahNumber = optionalSurahNumber
                )
                val questions = QuranQuizGenerator.generateQuestions(
                    fetchResult = fetchResult,
                    selectedTypes = selectedTypes,
                    questionCount = questionCount.coerceIn(3, 20),
                    optionalSurahNumber = optionalSurahNumber
                )
                if (questions.isEmpty()) {
                    _quizArenaState.value = QuranQuizArenaState.Error("لم نتمكن من تجهيز أسئلة لهذا النطاق. تأكد من الاتصال وجرب مرة أخرى.")
                    return@launch
                }
                val initialSeconds = timerMode.secondsLimit
                _quizArenaState.value = QuranQuizArenaState.Active(
                    scopeLabel = scopeLabel,
                    isFullQuran = scopeMode == QuranQuizScopeMode.FULL_QURAN,
                    testedJuzNumbers = if (scopeMode == QuranQuizScopeMode.FULL_QURAN) setOf(targetJuzSet.first()) else targetJuzSet,
                    selectedTypes = selectedTypes.ifEmpty { QuranQuestionType.entries.toSet() },
                    usedOfflineFallback = fetchResult.usedOfflineFallback,
                    questions = questions,
                    timerMode = timerMode,
                    remainingSeconds = initialSeconds,
                    totalTimerSeconds = initialSeconds
                )
                if (timerMode != QuranQuizTimerMode.UNTIMED) {
                    startQuizCountdownLoop()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quiz generation failed", e)
                _quizArenaState.value = QuranQuizArenaState.Error(e.localizedMessage ?: "تعذر بدء الاختبار")
            }
        }
    }

    private fun startQuizCountdownLoop() {
        quizTimerJob?.cancel()
        quizTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val active = _quizArenaState.value as? QuranQuizArenaState.Active ?: break
                if (active.timerMode == QuranQuizTimerMode.UNTIMED) break
                if (active.timerMode.isPerQuestion && active.submitted) continue
                val nextSec = (active.remainingSeconds - 1).coerceAtLeast(0)
                val updated = active.copy(remainingSeconds = nextSec)
                _quizArenaState.value = updated
                if (nextSec == 0) {
                    handleQuizTimeout(updated)
                    if (!updated.timerMode.isPerQuestion) break
                }
            }
        }
    }

    private fun handleQuizTimeout(active: QuranQuizArenaState.Active) {
        if (active.timerMode.isPerQuestion) {
            if (active.submitted) return
            val q = active.currentQuestion
            if (q.type != QuranQuestionType.MULTIPLE_CHOICE && active.writtenAnswer.isNotBlank()) {
                val grading = ArabicQuranNormalizer.gradeRecitation(
                    userAnswer = active.writtenAnswer,
                    expectedUthmani = q.expectedAnswerAr,
                    stripBasmala = q.type == QuranQuestionType.SURAH_BEGINNING
                )
                val passed = grading.passed
                val nextCombo = if (passed) active.currentCombo + 1 else 0
                _quizArenaState.value = active.copy(
                    submitted = true,
                    lastGrading = grading,
                    score = if (passed) active.score + 1 else active.score,
                    currentCombo = nextCombo,
                    bestCombo = maxOf(active.bestCombo, nextCombo),
                    openersSolved = active.openersSolved + if (passed && q.type == QuranQuestionType.SURAH_BEGINNING) 1 else 0,
                    closersSolved = active.closersSolved + if (passed && q.type == QuranQuestionType.SURAH_LAST_5) 1 else 0,
                    timedOutCount = active.timedOutCount + if (passed) 0 else 1
                )
            } else {
                _quizArenaState.value = active.copy(
                    submitted = true,
                    lastMcCorrect = false,
                    currentCombo = 0,
                    timedOutCount = active.timedOutCount + 1
                )
            }
            _statusMessages.tryEmit("انتهى وقت هذا السؤال!")
        } else {
            // Full exam timer expired — finalize immediately (grading partial written answer if present)
            val resolved = if (!active.submitted &&
                active.currentQuestion.type != QuranQuestionType.MULTIPLE_CHOICE &&
                active.writtenAnswer.isNotBlank()
            ) {
                val q = active.currentQuestion
                val grading = ArabicQuranNormalizer.gradeRecitation(
                    userAnswer = active.writtenAnswer,
                    expectedUthmani = q.expectedAnswerAr,
                    stripBasmala = q.type == QuranQuestionType.SURAH_BEGINNING
                )
                val passed = grading.passed
                val nextCombo = if (passed) active.currentCombo + 1 else 0
                active.copy(
                    submitted = true,
                    lastGrading = grading,
                    score = if (passed) active.score + 1 else active.score,
                    currentCombo = nextCombo,
                    bestCombo = maxOf(active.bestCombo, nextCombo),
                    openersSolved = active.openersSolved + if (passed && q.type == QuranQuestionType.SURAH_BEGINNING) 1 else 0,
                    closersSolved = active.closersSolved + if (passed && q.type == QuranQuestionType.SURAH_LAST_5) 1 else 0
                )
            } else {
                active
            }
            _statusMessages.tryEmit("انتهى وقت الاختبار الكلي!")
            finalizeQuizSession(resolved)
        }
    }

    fun updateWrittenQuizAnswer(text: String) {
        val current = _quizArenaState.value as? QuranQuizArenaState.Active ?: return
        if (current.submitted) return
        _quizArenaState.value = current.copy(writtenAnswer = text)
    }

    fun toggleQuizHint() {
        val current = _quizArenaState.value as? QuranQuizArenaState.Active ?: return
        _quizArenaState.value = current.copy(showHint = !current.showHint)
    }

    fun submitMultipleChoiceAnswer(optionIndex: Int, byVoice: Boolean = false) {
        val current = _quizArenaState.value as? QuranQuizArenaState.Active ?: return
        if (current.submitted) return
        val q = current.currentQuestion
        val isCorrect = optionIndex == q.correctOptionIndex
        val nextCombo = if (isCorrect) current.currentCombo + 1 else 0
        val nextBest = maxOf(current.bestCombo, nextCombo)

        _quizArenaState.value = current.copy(
            selectedMcOption = optionIndex,
            submitted = true,
            lastMcCorrect = isCorrect,
            score = if (isCorrect) current.score + 1 else current.score,
            currentCombo = nextCombo,
            bestCombo = nextBest,
            voiceCorrectIndices = if (isCorrect && byVoice) current.voiceCorrectIndices + current.currentIndex else current.voiceCorrectIndices,
            lastAnsweredByVoice = byVoice
        )
    }

    fun submitVoiceTranscriptForMc(spokenText: String) {
        val current = _quizArenaState.value as? QuranQuizArenaState.Active ?: return
        if (current.submitted) return
        val q = current.currentQuestion
        if (q.type != QuranQuestionType.MULTIPLE_CHOICE || q.options.isEmpty()) return

        val spokenNorm = ArabicQuranNormalizer.normalize(spokenText)
        if (spokenNorm.isBlank()) {
            _statusMessages.tryEmit("لم يتم التقاط صوت واضح — اختر الإجابة يدوياً أو أعد التسجيل")
            return
        }
        val spokenWords = spokenNorm.split(" ").filter { it.isNotBlank() }.toSet()

        var bestIndex = -1
        var bestScore = 0
        q.options.forEachIndexed { idx, opt ->
            val optNorm = ArabicQuranNormalizer.normalize(opt)
            val optWords = optNorm.split(" ").filter { it.isNotBlank() }.toSet()
            val overlap = spokenWords.intersect(optWords).size
            val gradingPct = ArabicQuranNormalizer.gradeRecitation(spokenText, opt).accuracyPercent
            val containsBoost = if (optNorm.contains(spokenNorm) || spokenNorm.contains(optNorm)) 40 else 0
            val totalScore = gradingPct + (overlap * 20) + containsBoost
            if (totalScore > bestScore) {
                bestScore = totalScore
                bestIndex = idx
            }
        }

        if (bestIndex >= 0 && bestScore >= 25) {
            submitMultipleChoiceAnswer(bestIndex, byVoice = true)
        } else {
            _statusMessages.tryEmit("سمعنا: «$spokenText» — لم يتطابق بوضوح مع خيار محدد، حاول مجدداً")
        }
    }

    fun submitWrittenQuizAnswer(byVoice: Boolean = false) {
        val current = _quizArenaState.value as? QuranQuizArenaState.Active ?: return
        if (current.submitted) return
        val q = current.currentQuestion
        val grading = ArabicQuranNormalizer.gradeRecitation(
            userAnswer = current.writtenAnswer,
            expectedUthmani = q.expectedAnswerAr,
            stripBasmala = q.type == QuranQuestionType.SURAH_BEGINNING
        )
        val passed = grading.passed
        val nextCombo = if (passed) current.currentCombo + 1 else 0
        val nextBest = maxOf(current.bestCombo, nextCombo)

        _quizArenaState.value = current.copy(
            submitted = true,
            lastGrading = grading,
            score = if (passed) current.score + 1 else current.score,
            currentCombo = nextCombo,
            bestCombo = nextBest,
            openersSolved = current.openersSolved + if (passed && q.type == QuranQuestionType.SURAH_BEGINNING) 1 else 0,
            closersSolved = current.closersSolved + if (passed && q.type == QuranQuestionType.SURAH_LAST_5) 1 else 0,
            voiceCorrectIndices = if (passed && byVoice) current.voiceCorrectIndices + current.currentIndex else current.voiceCorrectIndices,
            lastAnsweredByVoice = byVoice
        )
    }

    fun advanceNextQuizQuestion() {
        val current = _quizArenaState.value as? QuranQuizArenaState.Active ?: return
        if (current.currentIndex + 1 < current.questions.size) {
            val resetSeconds = if (current.timerMode.isPerQuestion) current.timerMode.secondsLimit else current.remainingSeconds
            val totalSec = if (current.timerMode.isPerQuestion) current.timerMode.secondsLimit else current.totalTimerSeconds
            _quizArenaState.value = current.copy(
                currentIndex = current.currentIndex + 1,
                selectedMcOption = null,
                writtenAnswer = "",
                showHint = false,
                submitted = false,
                lastMcCorrect = false,
                lastGrading = null,
                remainingSeconds = resetSeconds,
                totalTimerSeconds = totalSec,
                lastAnsweredByVoice = false
            )
        } else {
            finalizeQuizSession(current)
        }
    }

    fun resetQuizToSetup() {
        quizTimerJob?.cancel()
        _quizArenaState.value = QuranQuizArenaState.Setup
    }

    fun retakeLastQuranQuiz() {
        val cfg = lastQuizConfig
        if (cfg != null) {
            startQuranQuiz(
                scopeMode = cfg.scopeMode,
                selectedJuz = cfg.selectedJuz,
                optionalSurahNumber = cfg.optionalSurahNumber,
                selectedTypes = cfg.selectedTypes,
                questionCount = cfg.questionCount,
                timerMode = cfg.timerMode
            )
        } else {
            resetQuizToSetup()
        }
    }

    fun markMutashabihReviewed(mutashabihId: String) {
        if (mutashabihId in _reviewedMutashabihatIds.value) return
        _reviewedMutashabihatIds.value = _reviewedMutashabihatIds.value + mutashabihId
        awardActionXp(10L, isMutashabihIncrement = true)
        _statusMessages.tryEmit("تم تسجيل ضبط المتشابه (+١٠ نقاط إتقان)")
    }

    private fun finalizeQuizSession(active: QuranQuizArenaState.Active) {
        quizTimerJob?.cancel()
        val total = active.questions.size
        val score = active.score
        val baseXp = score * 20L
        val comboBonus = active.bestCombo * 5L
        val perfectBonus = if (score == total && total >= 3) 40L else 0L
        val timeBonus = if (active.timerMode != QuranQuizTimerMode.UNTIMED && score > 0) {
            (baseXp * active.timerMode.xpBonusPercent) / 100L
        } else 0L
        val voiceBonus = active.voiceCorrectIndices.size * 10L
        var totalXp = baseXp + comboBonus + perfectBonus + timeBonus + voiceBonus

        val existing = gamificationState.value
        val updatedStreak = GamificationCatalog.computeUpdatedStreak(
            lastActiveDateKey = existing.lastActiveDateKey,
            todayDateKey = todayKey,
            currentStreak = existing.currentStreakDays
        )
        val mergedJuzSet = (existing.testedJuzNumbers() + active.testedJuzNumbers).sorted()

        val draftProfile = existing.copy(
            userId = currentUserId,
            xp = existing.xp + totalXp,
            currentStreakDays = updatedStreak,
            longestQuizCombo = maxOf(existing.longestQuizCombo, active.bestCombo.toLong()),
            quizzesCompleted = existing.quizzesCompleted + 1L,
            perfectQuizzes = existing.perfectQuizzes + if (score == total && total >= 3) 1L else 0L,
            totalCorrectAnswers = existing.totalCorrectAnswers + score.toLong(),
            surahOpenersSolved = existing.surahOpenersSolved + active.openersSolved.toLong(),
            surahClosersSolved = existing.surahClosersSolved + active.closersSolved.toLong(),
            testedJuzCsv = mergedJuzSet.joinToString(","),
            fullQuranQuizDone = existing.fullQuranQuizDone || active.isFullQuran,
            lastActiveDateKey = todayKey
        )

        val tajweedCount = tajweedProgressState.value.values.count { it }
        val juzMap = (juzProgressState.value as? UiState.Success)?.data ?: emptyMap()
        val masteredJuz = juzMap.filterValues { it.masteryLevel == JuzMasteryLevel.MASTERED.key }.keys
        val timedPassedNow = active.timerMode != QuranQuizTimerMode.UNTIMED && total > 0 && (score * 100 >= total * 60)
        val voiceUsedNow = active.voiceCorrectIndices.isNotEmpty()

        val newBadges = GamificationCatalog.evaluateNewBadges(
            profile = draftProfile,
            masteredTajweedCount = tajweedCount,
            masteredJuzNumbers = masteredJuz,
            timedQuizPassedNow = timedPassedNow,
            voiceCorrectUsedNow = voiceUsedNow,
            mushafSurahsReadCount = _readSurahsSet.value.size
        )
        val badgeBonusXp = newBadges.sumOf { it.xpBonus }
        totalXp += badgeBonusXp

        val allBadgeIds = (existing.unlockedBadgeIds() + newBadges.map { it.id }).sorted()
        val finalProfile = draftProfile.copy(
            xp = draftProfile.xp + badgeBonusXp,
            unlockedBadgesCsv = allBadgeIds.joinToString(",")
        )

        val attemptScopeLabel = if (active.timerMode == QuranQuizTimerMode.UNTIMED) {
            active.scopeLabel
        } else {
            "${active.scopeLabel} • ${active.timerMode.labelAr}"
        }.take(118)

        val attempt = QuizAttempt(
            userId = currentUserId,
            scopeLabel = attemptScopeLabel,
            questionTypesCsv = active.selectedTypes.joinToString(",") { it.name },
            score = score.toLong(),
            totalQuestions = total.toLong(),
            xpEarned = totalXp,
            bestCombo = active.bestCombo.toLong(),
            dateKey = todayKey
        )

        viewModelScope.launch {
            repository.saveGamificationAndAttempt(currentUserId, finalProfile, attempt)
            if (newBadges.isNotEmpty()) {
                _statusMessages.tryEmit("مبارك! حصلت على شارة جديدة: ${newBadges.first().titleAr}")
            }
        }

        _quizArenaState.value = QuranQuizArenaState.Completed(
            QuizSessionSummary(
                scopeLabel = attemptScopeLabel,
                score = score,
                totalQuestions = total,
                bestCombo = active.bestCombo,
                baseXp = baseXp,
                comboBonusXp = comboBonus,
                perfectBonusXp = perfectBonus,
                timeBonusXp = timeBonus,
                voiceBonusXp = voiceBonus,
                totalXpEarned = totalXp,
                newlyUnlockedBadges = newBadges,
                usedOfflineFallback = active.usedOfflineFallback,
                timerModeLabelAr = active.timerMode.labelAr
            )
        )
    }

    private fun awardActionXp(
        xpDelta: Long,
        isMutashabihIncrement: Boolean = false,
        mushafSurahsCountOverride: Int? = null
    ) {
        viewModelScope.launch {
            val existing = gamificationState.value
            val updatedStreak = GamificationCatalog.computeUpdatedStreak(
                lastActiveDateKey = existing.lastActiveDateKey,
                todayDateKey = todayKey,
                currentStreak = existing.currentStreakDays
            )
            val draft = existing.copy(
                userId = currentUserId,
                xp = existing.xp + xpDelta,
                currentStreakDays = updatedStreak,
                mutashabihatSolved = existing.mutashabihatSolved + if (isMutashabihIncrement) 1L else 0L,
                lastActiveDateKey = todayKey
            )
            val tajweedCount = tajweedProgressState.value.values.count { it }
            val juzMap = (juzProgressState.value as? UiState.Success)?.data ?: emptyMap()
            val masteredJuz = juzMap.filterValues { it.masteryLevel == JuzMasteryLevel.MASTERED.key }.keys
            val newBadges = GamificationCatalog.evaluateNewBadges(
                profile = draft,
                masteredTajweedCount = tajweedCount,
                masteredJuzNumbers = masteredJuz,
                mushafSurahsReadCount = mushafSurahsCountOverride ?: _readSurahsSet.value.size
            )
            val badgeBonus = newBadges.sumOf { it.xpBonus }
            val allBadgeIds = (existing.unlockedBadgeIds() + newBadges.map { it.id }).sorted()
            val finalProfile = draft.copy(
                xp = draft.xp + badgeBonus,
                unlockedBadgesCsv = allBadgeIds.joinToString(",")
            )
            repository.saveGamificationAndAttempt(currentUserId, finalProfile, null)
            if (newBadges.isNotEmpty()) {
                _statusMessages.tryEmit("شارة جديدة مفتوحة: ${newBadges.first().titleAr}!")
            }
        }
    }

    // ---- Smart Juz Queue & Revision ----

    fun computeSmartQueue(progressMap: Map<Int, JuzProgress>): List<SmartJuzRecommendation> {
        val now = System.currentTimeMillis()
        val weekMs = 7L * 24 * 60 * 60 * 1000
        return QuranCatalog.allJuz.mapNotNull { juz ->
            val prog = progressMap[juz.number]
            val level = JuzMasteryLevel.fromRaw(prog?.masteryLevel ?: "")
            when {
                level == JuzMasteryLevel.NEEDS_REVIEW -> {
                    SmartJuzRecommendation(juz, prog, "موسوم بـ «يحتاج تثبيت» — أولوية قصوى") to 0
                }
                level == JuzMasteryLevel.MEMORIZING -> {
                    SmartJuzRecommendation(juz, prog, "قيد الحفظ — ثبّت الورد الجديد") to 1
                }
                level == JuzMasteryLevel.MASTERED && (prog?.lastReviewedEpochMs ?: 0L) > 0L && (now - (prog?.lastReviewedEpochMs ?: 0L)) > weekMs -> {
                    val days = ((now - (prog?.lastReviewedEpochMs ?: 0L)) / (24 * 60 * 60 * 1000)).toInt()
                    SmartJuzRecommendation(juz, prog, "متقن — لم يُراجع منذ $days يوماً") to 2
                }
                level == JuzMasteryLevel.MASTERED -> {
                    SmartJuzRecommendation(juz, prog, "دوريّة التعهد للحفاظ على الإتقان") to 3
                }
                else -> null
            }
        }
            .sortedWith(compareBy({ it.second }, { it.first.progress?.lastReviewedEpochMs ?: 0L }))
            .map { it.first }
            .ifEmpty {
                listOf(
                    SmartJuzRecommendation(
                        juzInfo = QuranCatalog.allJuz.last(),
                        progress = progressMap[30],
                        reasonAr = "ابدأ بتحديد مستوى ضبطك في جزء عمّ أو اختبر حفظك الآن"
                    )
                )
            }
            .take(4)
    }

    fun recordJuzRevision(
        juzNumber: Int,
        masteryLevel: JuzMasteryLevel,
        mistakeNote: String,
        incrementRevision: Boolean
    ) {
        viewModelScope.launch {
            repository.recordJuzRevision(
                userId = currentUserId,
                juzNumber = juzNumber,
                masteryLevel = masteryLevel,
                mistakeNote = mistakeNote,
                incrementRevision = incrementRevision,
                dateKey = todayKey
            ).onSuccess {
                if (incrementRevision) {
                    awardActionXp(30L)
                    _statusMessages.tryEmit("تم تسجيل مراجعة الجزء $juzNumber (+٣٠ نقطة إتقان)")
                } else {
                    _statusMessages.tryEmit("تم تحديث حالة الجزء $juzNumber")
                }
            }.onFailure {
                _statusMessages.tryEmit("تعذر حفظ مراجعة الجزء")
            }
        }
    }

    fun updatePagesRevisedToday(pages: Long) {
        viewModelScope.launch {
            repository.updatePagesRevisedToday(currentUserId, todayKey, pages)
                .onFailure { _statusMessages.tryEmit("تعذر تحديث صفحات المراجعة") }
        }
    }

    fun updateRevisionGoal(goalPages: Long) {
        viewModelScope.launch {
            repository.updateRevisionGoal(currentUserId, goalPages)
                .onSuccess { _statusMessages.tryEmit("تم تحديث هدف المراجعة اليومي") }
                .onFailure { _statusMessages.tryEmit("تعذر تحديث الهدف") }
        }
    }

    fun toggleTajweedMastery(ruleKey: String, mastered: Boolean) {
        viewModelScope.launch {
            repository.toggleTajweedMastery(currentUserId, ruleKey, mastered)
                .onSuccess {
                    if (mastered) awardActionXp(15L)
                }
                .onFailure { _statusMessages.tryEmit("تعذر حفظ تقدم التجويد") }
        }
    }

    fun recordTajweedQuizHit() {
        viewModelScope.launch {
            repository.recordTajweedQuizHit(currentUserId, todayKey)
            awardActionXp(10L)
        }
    }

    fun toggleAzkarStatus(field: String, value: Boolean) {
        viewModelScope.launch {
            repository.toggleAzkarStatus(currentUserId, todayKey, field, value)
            if (value) awardActionXp(10L)
        }
    }

    fun incrementZikrCounter(key: String, target: Int) {
        val current = _zikrCounters.value[key] ?: 0
        if (current < target) {
            _zikrCounters.value = _zikrCounters.value + (key to (current + 1))
        }
    }

    fun resetZikrCounter(key: String) {
        _zikrCounters.value = _zikrCounters.value - key
    }

    fun toggleFavorite(zikrKey: String, categoryKey: String, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavoriteZikr(currentUserId, zikrKey, categoryKey, currentlyFavorite)
                .onFailure { _statusMessages.tryEmit("تعذر تحديث المفضلة") }
        }
    }

    fun updateFontScale(scale: Double) {
        viewModelScope.launch {
            repository.updateFontScale(currentUserId, scale)
        }
    }

    fun selectTasbihPhrase(phrase: TasbihPhrase) {
        _tasbihState.value = TasbihSessionState(
            selectedPhrase = phrase,
            currentCount = 0,
            targetCount = phrase.defaultTarget
        )
    }

    fun tapTasbih(): Boolean {
        val current = _tasbihState.value
        val next = current.currentCount + 1
        val completed = current.targetCount > 0 && next >= current.targetCount
        _tasbihState.value = current.copy(currentCount = if (completed) 0 else next)

        pendingTasbihDelta += 1L
        tasbihFlushJob?.cancel()
        tasbihFlushJob = viewModelScope.launch {
            delay(500L)
            flushPendingTasbih()
        }
        return completed
    }

    private suspend fun flushPendingTasbih() {
        val delta = pendingTasbihDelta
        if (delta <= 0L) return
        pendingTasbihDelta = 0L
        withContext(NonCancellable) {
            repository.recordTasbihCount(currentUserId, todayKey, delta)
        }
    }

    fun addCustomZikr(text: String, virtue: String, targetCount: Long) {
        viewModelScope.launch {
            repository.addCustomZikr(currentUserId, text, virtue, targetCount)
                .onSuccess { _statusMessages.tryEmit("تمت إضافة الدعاء / الذكر الخاص") }
                .onFailure { _statusMessages.tryEmit(it.localizedMessage ?: "تعذر الإضافة") }
        }
    }

    fun incrementCustomZikr(zikrId: String) {
        viewModelScope.launch {
            repository.incrementCustomZikrCount(currentUserId, zikrId)
        }
    }

    fun deleteCustomZikr(zikrId: String) {
        viewModelScope.launch {
            repository.deleteCustomZikr(currentUserId, zikrId)
        }
    }

    fun addReflection(title: String, body: String, moodTag: MoodTag) {
        viewModelScope.launch {
            repository.addReflection(currentUserId, title, body, moodTag, todayKey)
                .onSuccess { _statusMessages.tryEmit("تم حفظ الملاحظة في سجل الضبط") }
                .onFailure { _statusMessages.tryEmit(it.localizedMessage ?: "تعذر الحفظ") }
        }
    }

    fun updateReflection(reflectionId: String, title: String, body: String, moodTag: MoodTag) {
        viewModelScope.launch {
            repository.updateReflection(currentUserId, reflectionId, title, body, moodTag)
                .onSuccess { _statusMessages.tryEmit("تم تحديث الملاحظة") }
                .onFailure { _statusMessages.tryEmit("تعذر التحديث") }
        }
    }

    fun deleteReflection(reflectionId: String) {
        viewModelScope.launch {
            repository.deleteReflection(currentUserId, reflectionId)
                .onSuccess { _statusMessages.tryEmit("تم حذف الملاحظة") }
        }
    }

    fun verifyCloudConnection() {
        viewModelScope.launch {
            _cloudSyncStatus.value = CloudSyncStatus.Checking
            repository.verifyCloudSync(currentUserId)
                .onSuccess { ms ->
                    _cloudSyncStatus.value = CloudSyncStatus.Verified(ms)
                    _statusMessages.tryEmit(
                        if (ms == 0L) "التخزين المحلي يعمل بكفاءة"
                        else "الاتصال السحابي مع Firebase يعمل بنجاح ($ms مللي ثانية)"
                    )
                }
                .onFailure { err ->
                    val msg = err.localizedMessage ?: "تعذر الاتصال بخادم Firebase"
                    _cloudSyncStatus.value = CloudSyncStatus.Failed(msg)
                    _statusMessages.tryEmit("فشل فحص الاتصال: $msg")
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        quizTimerJob?.cancel()
        tasbihFlushJob?.cancel()
    }

    private companion object {
        const val TAG = "RafikVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
