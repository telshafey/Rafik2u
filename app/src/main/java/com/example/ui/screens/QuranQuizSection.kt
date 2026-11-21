package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.GamificationCatalog
import com.example.data.QuranCatalog
import com.example.model.GamificationProfile
import com.example.model.QuizAttempt
import com.example.model.QuranQuestionType
import com.example.model.QuranQuizScopeMode
import com.example.model.QuranQuizTimerMode
import com.example.model.SurahInfo
import com.example.ui.components.CelebrationParticlesOverlay
import com.example.ui.components.IslamicGeometricOverlay
import com.example.ui.components.rememberArabicVoiceController
import com.example.ui.theme.AmiriFontFamily
import com.example.viewmodel.QuranQuizArenaState

@Composable
fun QuranQuizSection(
    arenaState: QuranQuizArenaState,
    gamification: GamificationProfile,
    recentAttempts: List<QuizAttempt>,
    preselectedSurahNumber: Int? = null,
    onStartQuiz: (QuranQuizScopeMode, Set<Int>, Int?, Set<QuranQuestionType>, Int, QuranQuizTimerMode) -> Unit,
    onUpdateWrittenAnswer: (String) -> Unit,
    onToggleHint: () -> Unit,
    onSubmitMc: (Int) -> Unit,
    onSubmitVoiceMc: (String) -> Unit,
    onSubmitWritten: (Boolean) -> Unit,
    onNextQuestion: () -> Unit,
    onRetakeSameConfig: () -> Unit,
    onSaveMistakeToJournal: (String, String) -> Unit,
    onResetToSetup: () -> Unit,
    onOpenBadgesDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (arenaState) {
        is QuranQuizArenaState.Setup -> {
            QuizSetupView(
                gamification = gamification,
                recentAttempts = recentAttempts,
                preselectedSurahNumber = preselectedSurahNumber,
                onStartQuiz = onStartQuiz,
                onOpenBadgesDialog = onOpenBadgesDialog,
                modifier = modifier
            )
        }
        is QuranQuizArenaState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = "جاري تجهيز أسئلة المصحف لـ «${arenaState.scopeLabel}»...",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "يتم جلب الآيات المعتمدة وضبط الاختيارات",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is QuranQuizArenaState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = arenaState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = onResetToSetup) {
                            Text("العودة لإعداد الاختبار")
                        }
                    }
                }
            }
        }
        is QuranQuizArenaState.Active -> {
            BackHandler(onBack = onResetToSetup)
            ActiveQuizView(
                state = arenaState,
                onUpdateWrittenAnswer = onUpdateWrittenAnswer,
                onToggleHint = onToggleHint,
                onSubmitMc = onSubmitMc,
                onSubmitVoiceMc = onSubmitVoiceMc,
                onSubmitWritten = onSubmitWritten,
                onSaveMistakeToJournal = onSaveMistakeToJournal,
                onNextQuestion = onNextQuestion,
                onQuit = onResetToSetup,
                modifier = modifier
            )
        }
        is QuranQuizArenaState.Completed -> {
            BackHandler(onBack = onResetToSetup)
            QuizCompletedView(
                state = arenaState,
                gamification = gamification,
                onRetakeSameConfig = onRetakeSameConfig,
                onRetryNew = onResetToSetup,
                onOpenBadgesDialog = onOpenBadgesDialog,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizSetupView(
    gamification: GamificationProfile,
    recentAttempts: List<QuizAttempt>,
    preselectedSurahNumber: Int?,
    onStartQuiz: (QuranQuizScopeMode, Set<Int>, Int?, Set<QuranQuestionType>, Int, QuranQuizTimerMode) -> Unit,
    onOpenBadgesDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scopeMode by rememberSaveable { mutableStateOf(QuranQuizScopeMode.SINGLE_JUZ) }
    var singleJuz by rememberSaveable { mutableIntStateOf(30) }
    var multiJuz by remember { mutableStateOf(setOf(29, 30)) }
    var selectedSurahNumber by rememberSaveable { mutableStateOf<Int?>(null) }
    var selectedTypes by remember { mutableStateOf(QuranQuestionType.entries.toSet()) }
    var questionCount by rememberSaveable { mutableIntStateOf(5) }
    var timerMode by rememberSaveable { mutableStateOf(QuranQuizTimerMode.UNTIMED) }
    var showSurahPicker by remember { mutableStateOf(false) }

    LaunchedEffect(preselectedSurahNumber) {
        if (preselectedSurahNumber != null) {
            val info = QuranCatalog.allSurahs.firstOrNull { it.number == preselectedSurahNumber }
            if (info != null) {
                scopeMode = QuranQuizScopeMode.SINGLE_JUZ
                singleJuz = info.juzList.firstOrNull() ?: 1
                selectedSurahNumber = preselectedSurahNumber
            }
        }
    }

    val activeJuzScope = remember(scopeMode, singleJuz, multiJuz) {
        when (scopeMode) {
            QuranQuizScopeMode.SINGLE_JUZ -> setOf(singleJuz)
            QuranQuizScopeMode.MULTI_JUZ -> multiJuz.ifEmpty { setOf(30) }
            QuranQuizScopeMode.FULL_QURAN -> (1..30).toSet()
        }
    }
    val availableSurahs = remember(activeJuzScope) {
        QuranCatalog.surahsForJuz(activeJuzScope)
    }
    val chosenSurahInfo = remember(selectedSurahNumber, availableSurahs) {
        availableSurahs.firstOrNull { it.number == selectedSurahNumber }
    }

    val rank = GamificationCatalog.rankForXp(gamification.xp)
    val badgeCount = gamification.unlockedBadgeIds().size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini Gamification Banner
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenBadgesDialog)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text(
                                    text = "${rank.titleAr} • ${gamification.xp} XP",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "الشارات: $badgeCount / ${GamificationCatalog.badges.size} • اضغط للعرض",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFD96B27), modifier = Modifier.size(18.dp))
                            Text(
                                text = "${gamification.currentStreakDays}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (preselectedSurahNumber == null) {
                    QuickChallengesCarousel(
                        onLaunchPreset = { preset ->
                            onStartQuiz(
                                preset.scopeMode,
                                preset.selectedJuz,
                                null,
                                preset.questionTypes,
                                preset.questionCount,
                                preset.timerMode
                            )
                        }
                    )
                }

                // Step 1: Scope Mode
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "١. اختر نطاق الاختبار",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            QuranQuizScopeMode.entries.forEachIndexed { idx, mode ->
                                SegmentedButton(
                                    selected = scopeMode == mode,
                                    onClick = {
                                        scopeMode = mode
                                        selectedSurahNumber = null
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(idx, QuranQuizScopeMode.entries.size),
                                    modifier = Modifier.testTag("quiz_scope_${mode.name}")
                                ) {
                                    Text(mode.labelAr, maxLines = 1)
                                }
                            }
                        }

                        if (scopeMode == QuranQuizScopeMode.SINGLE_JUZ) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..30).forEach { juz ->
                                    FilterChip(
                                        selected = singleJuz == juz,
                                        onClick = {
                                            singleJuz = juz
                                            selectedSurahNumber = null
                                        },
                                        label = { Text("ج$juz") }
                                    )
                                }
                            }
                        } else if (scopeMode == QuranQuizScopeMode.MULTI_JUZ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { multiJuz = setOf(28, 29, 30); selectedSurahNumber = null },
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("٢٨-٣٠") }
                                OutlinedButton(
                                    onClick = { multiJuz = (1..5).toSet(); selectedSurahNumber = null },
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("١-٥") }
                                OutlinedButton(
                                    onClick = { multiJuz = (1..10).toSet(); selectedSurahNumber = null },
                                    shape = RoundedCornerShape(10.dp)
                                ) { Text("١-١٠") }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..30).forEach { juz ->
                                    val selected = juz in multiJuz
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            multiJuz = if (selected && multiJuz.size > 1) multiJuz - juz else multiJuz + juz
                                            selectedSurahNumber = null
                                        },
                                        label = { Text("ج$juz") }
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "تحدي كامل القرآن الكريم (٣٠ جزءاً • ١١٤ سورة) — أسئلة متنوعة من شتى أجزاء المصحف.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        // Optional Specific Surah Filter
                        OutlinedButton(
                            onClick = { showSurahPicker = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (chosenSurahInfo != null) {
                                    "السورة المختارة: سورة ${chosenSurahInfo.nameAr} (تغيير)"
                                } else {
                                    "تخصيص سورة معينة (اختياري • الآن: عشوائي من النطاق)"
                                }
                            )
                        }
                    }
                }

                // Step 2: Question Types & Count
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "٢. أنماط الأسئلة وعددها",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuranQuestionType.entries.forEach { qType ->
                                val isSelected = qType in selectedTypes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTypes = if (isSelected && selectedTypes.size > 1) {
                                            selectedTypes - qType
                                        } else {
                                            selectedTypes + qType
                                        }
                                    },
                                    label = { Text(qType.labelAr) },
                                    modifier = Modifier.testTag("quiz_type_${qType.name}")
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("عدد الأسئلة:", style = MaterialTheme.typography.labelLarge)
                            listOf(5, 10, 15).forEach { count ->
                                FilterChip(
                                    selected = questionCount == count,
                                    onClick = { questionCount = count },
                                    label = { Text("$count") }
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "نظام المؤقت (مكافأة XP إضافية):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuranQuizTimerMode.entries.forEach { tMode ->
                                FilterChip(
                                    selected = timerMode == tMode,
                                    onClick = { timerMode = tMode },
                                    label = { Text(tMode.labelAr) },
                                    modifier = Modifier.testTag("quiz_timer_${tMode.name}")
                                )
                            }
                        }
                        Text(
                            text = timerMode.shortDescAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Button(
                    onClick = {
                        onStartQuiz(
                            scopeMode,
                            activeJuzScope,
                            chosenSurahInfo?.number,
                            selectedTypes,
                            questionCount,
                            timerMode
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_quran_quiz_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = if (timerMode == QuranQuizTimerMode.UNTIMED) "ابدأ الاختبار الآن"
                        else "ابدأ التحدي المؤقت (${timerMode.labelAr})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (recentAttempts.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "آخر اختباراتك",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            recentAttempts.take(4).forEach { attempt ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${attempt.scopeLabel} (${attempt.score}/${attempt.totalQuestions})",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "+${attempt.xpEarned} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSurahPicker) {
        SurahPickerDialog(
            surahs = availableSurahs,
            currentSurahNumber = chosenSurahInfo?.number,
            onSelect = { surahNum ->
                selectedSurahNumber = surahNum
                showSurahPicker = false
            },
            onDismiss = { showSurahPicker = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveQuizView(
    state: QuranQuizArenaState.Active,
    onUpdateWrittenAnswer: (String) -> Unit,
    onToggleHint: () -> Unit,
    onSubmitMc: (Int) -> Unit,
    onSubmitVoiceMc: (String) -> Unit,
    onSubmitWritten: (Boolean) -> Unit,
    onSaveMistakeToJournal: (String, String) -> Unit,
    onNextQuestion: () -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val q = state.currentQuestion
    val progress = (state.currentIndex + 1).toFloat() / state.questions.size.toFloat()
    var savedForIndex by remember(state.currentIndex, q.id) { mutableStateOf(false) }

    val voiceController = rememberArabicVoiceController(
        onPartialText = { partial ->
            if (!state.submitted && q.type != QuranQuestionType.MULTIPLE_CHOICE) {
                onUpdateWrittenAnswer(partial)
            }
        },
        onFinalResult = { spoken ->
            if (!state.submitted) {
                if (q.type == QuranQuestionType.MULTIPLE_CHOICE) {
                    onSubmitVoiceMc(spoken)
                } else {
                    onUpdateWrittenAnswer(spoken)
                    onSubmitWritten(true)
                }
            }
        }
    )

    LaunchedEffect(state.currentIndex, state.submitted) {
        if (state.submitted) {
            voiceController.stopListening()
        } else {
            voiceController.clearMessage()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live HUD Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سؤال ${state.currentIndex + 1} / ${state.questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (state.currentCombo >= 2) {
                            Surface(
                                color = Color(0xFFFFEDE0),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFD96B27), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = " ×${state.currentCombo}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB34700)
                                    )
                                }
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "الصحيح: ${state.score}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        TextButton(onClick = onQuit) { Text("إنهاء") }
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                // Live Countdown Timer HUD
                if (state.timerMode != QuranQuizTimerMode.UNTIMED && state.totalTimerSeconds > 0) {
                    val mins = state.remainingSeconds / 60
                    val secs = state.remainingSeconds % 60
                    val timeFormatted = "%02d:%02d".format(mins, secs)
                    val timerFraction = (state.remainingSeconds.toFloat() / state.totalTimerSeconds.toFloat()).coerceIn(0f, 1f)
                    val isCritical = state.remainingSeconds <= 5
                    val isWarning = state.remainingSeconds <= 10
                    val timerTint = when {
                        isCritical -> MaterialTheme.colorScheme.error
                        isWarning -> Color(0xFFD96B27)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        color = if (isCritical) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quiz_timer_hud")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Timer, null, tint = timerTint, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = if (state.timerMode.isPerQuestion) "مؤقت السؤال" else "المؤقت الكلي للاختبار",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = timerTint
                                    )
                                }
                                Text(
                                    text = timeFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = timerTint
                                )
                            }
                            LinearProgressIndicator(
                                progress = { timerFraction },
                                color = timerTint,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }

                if (state.usedOfflineFallback) {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "وضع عدم الاتصال: يتم عرض نماذج محفوظة محلياً. اتصل بالإنترنت لجلب جميع آيات المصحف.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // Question Card
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = q.type.labelAr,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "الجزء ${q.juzNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = q.headerLabelAr,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = q.promptTextAr,
                                fontFamily = AmiriFontFamily,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        if (!state.submitted) {
                            TextButton(onClick = onToggleHint) {
                                Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text(if (state.showHint) "إخفاء التلميح" else "تلميح مساعد")
                            }
                            AnimatedVisibility(visible = state.showHint) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = q.hintTextAr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }

                        // Answer Section
                        if (q.type == QuranQuestionType.MULTIPLE_CHOICE) {
                            q.options.forEachIndexed { idx, opt ->
                                val isPicked = state.selectedMcOption == idx
                                val isCorrectOpt = idx == q.correctOptionIndex
                                val bgColor = when {
                                    !state.submitted -> MaterialTheme.colorScheme.surface
                                    isCorrectOpt -> MaterialTheme.colorScheme.primaryContainer
                                    isPicked -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                Surface(
                                    color = bgColor,
                                    shape = RoundedCornerShape(14.dp),
                                    border = if (isPicked && !state.submitted) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = !state.submitted) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSubmitMc(idx)
                                        }
                                        .minimumInteractiveComponentSize()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = opt,
                                            fontFamily = AmiriFontFamily,
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (state.submitted && isCorrectOpt) {
                                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                                        } else if (state.submitted && isPicked) {
                                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                            if (!state.submitted) {
                                OutlinedButton(
                                    onClick = { voiceController.toggleListening() },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (voiceController.isListening) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("quiz_voice_mc_btn")
                                ) {
                                    Icon(
                                        imageVector = if (voiceController.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (voiceController.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.size(8.dp))
                                    Text(
                                        text = if (voiceController.isListening) {
                                            "جاري الاستماع... اضغط للإيقاف"
                                        } else {
                                            "أجب بصوتك عبر المايك (+١٠ XP)"
                                        }
                                    )
                                }
                            }
                        } else {
                            if (!state.submitted) {
                                Surface(
                                    color = if (voiceController.isListening) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                voiceController.toggleListening()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (voiceController.isListening) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.secondary
                                                }
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("quiz_voice_recite_btn")
                                        ) {
                                            Icon(
                                                imageVector = if (voiceController.isListening) Icons.Default.Stop else Icons.Default.Mic,
                                                contentDescription = null
                                            )
                                            Spacer(Modifier.size(8.dp))
                                            Text(
                                                text = if (voiceController.isListening) {
                                                    "إيقاف التسجيل الصوتي الآن"
                                                } else {
                                                    "تسميع بالصوت (المايك) (+١٠ XP)"
                                                },
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        val msg = voiceController.statusMessage
                                        if (!msg.isNullOrBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(Icons.Default.GraphicEq, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text(
                                                    text = msg,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = state.writtenAnswer,
                                onValueChange = onUpdateWrittenAnswer,
                                enabled = !state.submitted,
                                label = { Text("اكتب أو سمّع الآيات بالمايك (بدون تشكيل مطلوب)...") },
                                minLines = 3,
                                maxLines = 7,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quiz_written_input")
                            )

                            if (!state.submitted) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSubmitWritten(false)
                                    },
                                    enabled = state.writtenAnswer.isNotBlank(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("quiz_submit_written_btn")
                                ) {
                                    Text("تحقق من الضبط")
                                }
                            }
                        }

                        if (!state.submitted && q.type == QuranQuestionType.MULTIPLE_CHOICE && !voiceController.statusMessage.isNullOrBlank()) {
                            Text(
                                text = voiceController.statusMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Grading Result for Written / MC
                        if (state.submitted) {
                            HorizontalDivider()
                            val grading = state.lastGrading
                            if (grading != null) {
                                Surface(
                                    color = if (grading.passed) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = grading.masteryVerdictAr,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${grading.accuracyPercent}%",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "النص المصحفي (${q.ayahRangeLabel}):",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            grading.tokens.forEach { tok ->
                                                Surface(
                                                    color = if (tok.matched) {
                                                        Color(0x331B7A43)
                                                    } else {
                                                        Color(0x33C62828)
                                                    },
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = tok.expectedWord,
                                                        fontFamily = AmiriFontFamily,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = if (tok.matched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    color = if (state.lastMcCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (state.lastMcCorrect) {
                                            "أحسنت! إجابة صحيحة (${q.ayahRangeLabel})"
                                        } else {
                                            "الإجابة الصحيحة (${q.ayahRangeLabel}): ${q.expectedAnswerAr}"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!savedForIndex) {
                                        savedForIndex = true
                                        onSaveMistakeToJournal(
                                            "موضع ضبط: ${q.ayahRangeLabel}",
                                            "الآيات: ${q.expectedAnswerAr}\n(من اختبار ${q.type.labelAr})"
                                        )
                                    }
                                },
                                enabled = !savedForIndex,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.BookmarkAdd, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(if (savedForIndex) "تم حفظ الموضع في سجل الضبط ✓" else "تدوين هذا الموضع في سجل الضبط")
                            }

                            Button(
                                onClick = onNextQuestion,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("quiz_next_btn")
                            ) {
                                Text(
                                    if (state.currentIndex + 1 < state.questions.size) "السؤال التالي"
                                    else "إنهاء وعرض النتيجة والشارات"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizCompletedView(
    state: QuranQuizArenaState.Completed,
    gamification: GamificationProfile,
    onRetakeSameConfig: () -> Unit,
    onRetryNew: () -> Unit,
    onOpenBadgesDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = state.summary
    val pct = if (summary.totalQuestions > 0) (summary.score * 100) / summary.totalQuestions else 0
    val rank = GamificationCatalog.rankForXp(gamification.xp)
    val animatedPct by animateFloatAsState(
        targetValue = (pct / 100f).coerceIn(0f, 1f),
        label = "quiz_completion_ring"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier.widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            IslamicGeometricOverlay(
                                modifier = Modifier.matchParentSize(),
                                lineColor = MaterialTheme.colorScheme.primary,
                                alpha = 0.06f
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(88.dp)
                                ) {
                                    val ringTrack = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    val ringActive = MaterialTheme.colorScheme.secondary
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = ringTrack,
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = ringActive,
                                            startAngle = -90f,
                                            sweepAngle = 360f * animatedPct,
                                            useCenter = false,
                                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondary,
                                        shape = CircleShape,
                                        modifier = Modifier.size(58.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(30.dp))
                                        }
                                    }
                                }
                                Text(
                                    text = if (pct >= 80) "تثبيت مبارك ما شاء الله!" else "استمر في التعهد والمراجعة!",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${summary.scopeLabel} • النتيجة ${summary.score} / ${summary.totalQuestions} ($pct%)",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "رتبتك الحالية: ${rank.titleAr} (${gamification.xp} XP)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                // XP Breakdown
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "نقاط الإتقان المكتسبة (+${summary.totalXpEarned} XP)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        XpBreakdownRow("إجابات صحيحة (${summary.score})", "+${summary.baseXp}")
                        XpBreakdownRow("مكافأة التتابع (أعلى سلسلة ×${summary.bestCombo})", "+${summary.comboBonusXp}")
                        if (summary.perfectBonusXp > 0) {
                            XpBreakdownRow("مكافأة الضبط الكامل ١٠٠٪", "+${summary.perfectBonusXp}")
                        }
                        if (summary.timeBonusXp > 0) {
                            XpBreakdownRow("مكافأة تحدي الوقت (${summary.timerModeLabelAr})", "+${summary.timeBonusXp}")
                        }
                        if (summary.voiceBonusXp > 0) {
                            XpBreakdownRow("مكافأة التسميع بالصوت (المايك)", "+${summary.voiceBonusXp}")
                        }
                    }
                }

                if (summary.newlyUnlockedBadges.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = "شارات جديدة فُتحت لك الآن!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            summary.newlyUnlockedBadges.forEach { badge ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.secondary)
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(badge.titleAr, fontWeight = FontWeight.Bold)
                                            Text(badge.descriptionAr, style = MaterialTheme.typography.bodySmall)
                                        }
                                        Text("+${badge.xpBonus} XP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRetakeSameConfig,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("quiz_retake_same_btn")
                    ) {
                        Icon(Icons.Default.Replay, null, Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("إعادة نفس الاختبار بأسئلة جديدة")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenBadgesDialog,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.EmojiEvents, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("كل الشارات")
                        }
                        OutlinedButton(
                            onClick = onRetryNew,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("تغيير النطاق")
                        }
                    }
                }
            }
        }
        }

        CelebrationParticlesOverlay(
            active = pct >= 60,
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
private fun XpBreakdownRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun SurahPickerDialog(
    surahs: List<SurahInfo>,
    currentSurahNumber: Int?,
    onSelect: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var filter by remember { mutableStateOf("") }
    val filtered = remember(filter, surahs) {
        if (filter.isBlank()) surahs
        else surahs.filter { it.nameAr.contains(filter, ignoreCase = true) || it.number.toString() == filter.trim() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر سورة من النطاق") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = filter,
                    onValueChange = { filter = it },
                    placeholder = { Text("ابحث باسم السورة أو رقمها...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { onSelect(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("اختيار عشوائي من جميع سور النطاق")
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered.size) { idx ->
                        val s = filtered[idx]
                        val selected = s.number == currentSurahNumber
                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelect(s.number) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${s.number}. سورة ${s.nameAr}", fontWeight = FontWeight.SemiBold)
                                Text("${s.ayahCount} آية", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    )
}
