package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.QuranCatalog
import com.example.model.GamificationProfile
import com.example.model.JuzMasteryLevel
import com.example.model.JuzProgress
import com.example.model.MutashabihItem
import com.example.model.QuizAttempt
import com.example.model.QuranJuzInfo
import com.example.model.QuranQuestionType
import com.example.model.QuranQuizScopeMode
import com.example.model.QuranQuizTimerMode
import com.example.model.UiState
import com.example.ui.theme.AmiriFontFamily
import com.example.viewmodel.MushafReaderUiState
import com.example.viewmodel.QuranQuizArenaState

enum class QuranTabMode(val labelAr: String) {
    MUSHAF("المصحف"),
    QUIZ_ARENA("الاختبار"),
    JUZ_PLAN("الأجزاء"),
    MUTASHABIHAT("المتشابهات")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranRevisionScreen(
    juzProgressState: UiState<Map<Int, JuzProgress>>,
    quizArenaState: QuranQuizArenaState,
    mushafReaderState: MushafReaderUiState,
    lastReadSurahNumber: Int,
    readSurahsCount: Int,
    gamification: GamificationProfile,
    recentAttempts: List<QuizAttempt>,
    reviewedMutashabihatIds: Set<String> = emptySet(),
    initialMode: QuranTabMode = QuranTabMode.QUIZ_ARENA,
    onOpenSurahInMushaf: (Int) -> Unit,
    onCloseMushafSurah: () -> Unit,
    onSaveJuzRevision: (Int, JuzMasteryLevel, String, Boolean) -> Unit,
    onStartQuiz: (QuranQuizScopeMode, Set<Int>, Int?, Set<QuranQuestionType>, Int, QuranQuizTimerMode) -> Unit,
    onUpdateWrittenAnswer: (String) -> Unit,
    onToggleHint: () -> Unit,
    onSubmitMc: (Int) -> Unit,
    onSubmitVoiceMc: (String) -> Unit,
    onSubmitWritten: (Boolean) -> Unit,
    onNextQuestion: () -> Unit,
    onRetakeSameQuiz: () -> Unit,
    onResetQuizToSetup: () -> Unit,
    onMarkMutashabihReviewed: (String) -> Unit,
    onSaveNoteToJournal: (String, String, com.example.model.MoodTag) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var searchQuery by remember { mutableStateOf("") }
    var levelFilter by remember { mutableStateOf<JuzMasteryLevel?>(null) }
    var editingJuz by remember { mutableStateOf<Pair<QuranJuzInfo, JuzProgress?>?>(null) }
    var showBadgesDialog by remember { mutableStateOf(false) }
    var preselectedSurahForQuiz by remember { mutableStateOf<Int?>(null) }

    val progressMap = (juzProgressState as? UiState.Success)?.data ?: emptyMap()

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "المصحف ومراجعة الحفظ",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                QuranTabMode.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = mode == tab,
                        onClick = { mode = tab },
                        shape = SegmentedButtonDefaults.itemShape(idx, QuranTabMode.entries.size),
                        modifier = Modifier.testTag("quran_mode_${tab.name}")
                    ) {
                        Text(
                            text = tab.labelAr,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (mode == QuranTabMode.JUZ_PLAN || mode == QuranTabMode.MUTASHABIHAT) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            if (mode == QuranTabMode.JUZ_PLAN) "ابحث بالجزء أو السورة..."
                            else "ابحث في المتشابهات..."
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (mode == QuranTabMode.JUZ_PLAN) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = levelFilter == null,
                        onClick = { levelFilter = null },
                        label = { Text("الكل (٣٠)") }
                    )
                    JuzMasteryLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = levelFilter == lvl,
                            onClick = { levelFilter = if (levelFilter == lvl) null else lvl },
                            label = { Text(lvl.labelAr) }
                        )
                    }
                }
            }
        }

        when (mode) {
            QuranTabMode.MUSHAF -> {
                MushafBrowserSection(
                    readerState = mushafReaderState,
                    lastReadSurahNumber = lastReadSurahNumber,
                    readSurahsCount = readSurahsCount,
                    onOpenSurah = onOpenSurahInMushaf,
                    onCloseSurah = onCloseMushafSurah,
                    onQuizThisSurah = { surahNum ->
                        preselectedSurahForQuiz = surahNum
                        onResetQuizToSetup()
                        mode = QuranTabMode.QUIZ_ARENA
                    },
                    onSaveAyahToJournal = onSaveNoteToJournal,
                    modifier = Modifier.weight(1f)
                )
            }
            QuranTabMode.QUIZ_ARENA -> {
                QuranQuizSection(
                    arenaState = quizArenaState,
                    gamification = gamification,
                    recentAttempts = recentAttempts,
                    preselectedSurahNumber = preselectedSurahForQuiz,
                    onStartQuiz = onStartQuiz,
                    onUpdateWrittenAnswer = onUpdateWrittenAnswer,
                    onToggleHint = onToggleHint,
                    onSubmitMc = onSubmitMc,
                    onSubmitVoiceMc = onSubmitVoiceMc,
                    onSubmitWritten = onSubmitWritten,
                    onNextQuestion = onNextQuestion,
                    onRetakeSameConfig = onRetakeSameQuiz,
                    onSaveMistakeToJournal = { title, body ->
                        onSaveNoteToJournal(title, body, com.example.model.MoodTag.AYAH_FIX)
                    },
                    onResetToSetup = onResetQuizToSetup,
                    onOpenBadgesDialog = { showBadgesDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
            QuranTabMode.JUZ_PLAN -> {
                if (juzProgressState is UiState.Loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredJuz = remember(searchQuery, levelFilter, progressMap) {
                        QuranCatalog.allJuz.filter { info ->
                            val currentLvl = JuzMasteryLevel.fromRaw(progressMap[info.number]?.masteryLevel ?: "")
                            val matchesLevel = levelFilter == null || currentLvl == levelFilter
                            val matchesQuery = searchQuery.isBlank() ||
                                info.nameAr.contains(searchQuery, ignoreCase = true) ||
                                info.surahRangeAr.contains(searchQuery, ignoreCase = true)
                            matchesLevel && matchesQuery
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredJuz, key = { it.number }) { info ->
                            val prog = progressMap[info.number]
                            JuzCard(
                                info = info,
                                progress = prog,
                                onQuickReview = {
                                    val current = JuzMasteryLevel.fromRaw(prog?.masteryLevel ?: "")
                                    val next = if (current == JuzMasteryLevel.NOT_STARTED) JuzMasteryLevel.NEEDS_REVIEW else current
                                    onSaveJuzRevision(info.number, next, prog?.mistakeNote ?: "", true)
                                },
                                onEditDetails = { editingJuz = info to prog },
                                modifier = Modifier.widthIn(max = 640.dp)
                            )
                        }
                    }
                }
            }
            QuranTabMode.MUTASHABIHAT -> {
                val filteredMutashabihat = remember(searchQuery) {
                    QuranCatalog.mutashabihatList.filter {
                        searchQuery.isBlank() ||
                            it.title.contains(searchQuery, ignoreCase = true) ||
                            it.firstPlace.contains(searchQuery, ignoreCase = true) ||
                            it.secondPlace.contains(searchQuery, ignoreCase = true) ||
                            it.juzRangeAr.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredMutashabihat, key = { it.id }) { item ->
                        MutashabihCard(
                            item = item,
                            reviewed = item.id in reviewedMutashabihatIds,
                            onMarkReviewed = { onMarkMutashabihReviewed(item.id) },
                            onSaveToJournal = {
                                onSaveNoteToJournal(
                                    item.title,
                                    "${item.firstPlace}\n${item.secondPlace}\nالضابط: ${item.memoryTip}",
                                    com.example.model.MoodTag.MUTASHABIHAT
                                )
                            },
                            modifier = Modifier.widthIn(max = 640.dp)
                        )
                    }
                }
            }
        }
    }

    val editing = editingJuz
    if (editing != null) {
        EditJuzDialog(
            info = editing.first,
            progress = editing.second,
            onDismiss = { editingJuz = null },
            onSave = { level, note, increment ->
                onSaveJuzRevision(editing.first.number, level, note, increment)
                editingJuz = null
            }
        )
    }

    if (showBadgesDialog) {
        BadgesShowcaseDialog(
            gamification = gamification,
            onDismiss = { showBadgesDialog = false }
        )
    }
}

@Composable
private fun JuzCard(
    info: QuranJuzInfo,
    progress: JuzProgress?,
    onQuickReview: () -> Unit,
    onEditDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val level = JuzMasteryLevel.fromRaw(progress?.masteryLevel ?: "")
    val containerColor = when (level) {
        JuzMasteryLevel.MASTERED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        JuzMasteryLevel.NEEDS_REVIEW -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        JuzMasteryLevel.MEMORIZING -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        JuzMasteryLevel.NOT_STARTED -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    val daysSince = remember(progress?.lastReviewedEpochMs) {
        val ts = progress?.lastReviewedEpochMs ?: 0L
        if (ts <= 0L) null
        else ((System.currentTimeMillis() - ts) / (24L * 60 * 60 * 1000)).toInt()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.nameAr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = info.surahRangeAr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = level.labelAr,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            val noteText = progress?.mistakeNote.orEmpty()
            if (noteText.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "موضع يحتاج ضبطاً: $noteText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        append("المراجعات: ${progress?.revisionCount ?: 0L}")
                        if (daysSince != null) {
                            append(if (daysSince == 0) " • راجعته اليوم" else " • منذ $daysSince يوم")
                        }
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onEditDetails,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = "تعديل الحالة والملاحظة")
                    }
                    OutlinedButton(
                        onClick = onQuickReview,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("juz_review_btn_${info.number}")
                    ) {
                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(4.dp))
                        Text("+١ مراجعة")
                    }
                }
            }
        }
    }
}

@Composable
private fun MutashabihCard(
    item: MutashabihItem,
    reviewed: Boolean,
    onMarkReviewed: () -> Unit,
    onSaveToJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var revealed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reviewed) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Psychology, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (item.juzRangeAr.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = item.juzRangeAr,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.firstPlace,
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = item.secondPlace,
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            OutlinedButton(
                onClick = { revealed = !revealed },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Visibility, null, Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(6.dp))
                Text(if (revealed) "إخفاء القاعدة الضابطة" else "اختبر تذكرك — أظهر الضابط")
            }

            AnimatedVisibility(visible = revealed) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.memoryTip,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveToJournal,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.BookmarkAdd, null, Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text("تدوين بسجلي")
                }
                Button(
                    onClick = onMarkReviewed,
                    enabled = !reviewed,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(if (reviewed) "تم ضبطه ✓" else "ضبطته (+١٠ XP)")
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditJuzDialog(
    info: QuranJuzInfo,
    progress: JuzProgress?,
    onDismiss: () -> Unit,
    onSave: (JuzMasteryLevel, String, Boolean) -> Unit
) {
    var selectedLevel by remember {
        mutableStateOf(JuzMasteryLevel.fromRaw(progress?.masteryLevel ?: ""))
    }
    var mistakeNote by remember { mutableStateOf(progress?.mistakeNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(info.nameAr) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = info.surahRangeAr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text("مستوى الإتقان والضبط:", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    JuzMasteryLevel.entries.forEach { lvl ->
                        FilterChip(
                            selected = selectedLevel == lvl,
                            onClick = { selectedLevel = lvl },
                            label = { Text(lvl.labelAr) }
                        )
                    }
                }
                OutlinedTextField(
                    value = mistakeNote,
                    onValueChange = { if (it.length <= 400) mistakeNote = it },
                    label = { Text("ملاحظة ضبط / آية تحتاج تثبيتاً") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedLevel, mistakeNote, true) }) {
                Text("حفظ وتسجيل مراجعة")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onSave(selectedLevel, mistakeNote, false) }) {
                    Text("حفظ الحالة فقط")
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}
