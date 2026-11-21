package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import com.example.data.TajweedCatalog
import com.example.model.TajweedCategory
import com.example.model.TajweedRuleItem
import com.example.ui.components.IslamicGeometricOverlay
import com.example.ui.components.MushafParchmentFrame
import com.example.ui.theme.AmiriFontFamily
import kotlinx.coroutines.launch

private enum class TajweedTabMode(val labelAr: String) {
    RULES("أبواب التحفة"),
    FULL_MATN("المتن (٦١ بيتاً)"),
    QUIZ("اختبر التحفة")
}

@Composable
fun TajweedScreen(
    tajweedProgress: Map<String, Boolean>,
    quizCorrectToday: Long,
    onToggleRuleMastery: (String, Boolean) -> Unit,
    onQuizCorrectAnswer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(TajweedTabMode.RULES) }
    var selectedCategory by remember { mutableStateOf<TajweedCategory?>(null) }

    val masteredCount = tajweedProgress.values.count { it }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "تحفة الأطفال والتجويد",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "نظم الشيخ سليمان الجمزوري (${TajweedCatalog.tuhfahChapterCount} أبواب • ${TajweedCatalog.totalTuhfahVerses} بيتاً)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = "المتقن: $masteredCount / ${TajweedCatalog.rules.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TajweedTabMode.entries.forEachIndexed { idx, tab ->
                    SegmentedButton(
                        selected = mode == tab,
                        onClick = { mode = tab },
                        shape = SegmentedButtonDefaults.itemShape(idx, TajweedTabMode.entries.size),
                        modifier = Modifier.testTag("tajweed_mode_${tab.name}")
                    ) {
                        Text(tab.labelAr, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            if (mode == TajweedTabMode.RULES) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("جميع الأبواب") }
                        )
                    }
                    items(TajweedCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.titleAr) }
                        )
                    }
                }
            }
        }

        when (mode) {
            TajweedTabMode.RULES -> {
                val rules = remember(selectedCategory) {
                    if (selectedCategory == null) TajweedCatalog.rules
                    else TajweedCatalog.rules.filter { it.category == selectedCategory }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(rules, key = { it.key }) { rule ->
                        val mastered = tajweedProgress[rule.key] == true
                        TajweedRuleCard(
                            rule = rule,
                            mastered = mastered,
                            onToggleMastered = { onToggleRuleMastery(rule.key, !mastered) },
                            modifier = Modifier.widthIn(max = 640.dp)
                        )
                    }
                }
            }
            TajweedTabMode.FULL_MATN -> {
                TuhfahFullMatnView(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 660.dp)
                )
            }
            TajweedTabMode.QUIZ -> {
                TajweedQuizView(
                    quizCorrectToday = quizCorrectToday,
                    onCorrect = onQuizCorrectAnswer,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun TuhfahFullMatnView(
    modifier: Modifier = Modifier
) {
    var fontScale by remember { mutableDoubleStateOf(1.0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "متن تحفة الأطفال والغلمان كاملاً",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row {
                IconButton(
                    onClick = { fontScale = (fontScale - 0.1).coerceAtLeast(0.85) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.TextDecrease, contentDescription = "تصغير خط المتن")
                }
                IconButton(
                    onClick = { fontScale = (fontScale + 0.1).coerceAtMost(1.55) },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.TextIncrease, contentDescription = "تكبير خط المتن")
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(TajweedCatalog.tuhfahChapters.size) { index ->
                val ch = TajweedCatalog.tuhfahChapters[index]
                FilterChip(
                    selected = false,
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    },
                    label = { Text("${ch.number}. ${ch.titleAr}") }
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(TajweedCatalog.tuhfahChapters, key = { it.id }) { chapter ->
                MushafParchmentFrame(
                    modifier = Modifier.testTag("tuhfah_chapter_${chapter.number}")
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        IslamicGeometricOverlay(
                            modifier = Modifier.matchParentSize(),
                            lineColor = MaterialTheme.colorScheme.secondary,
                            alpha = 0.04f
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الباب ${chapter.number}: ${chapter.titleAr}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = chapter.verseRangeAr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))

                            chapter.verses.forEach { verse ->
                                Text(
                                    text = verse,
                                    fontFamily = AmiriFontFamily,
                                    fontSize = (20.0 * fontScale).sp,
                                    lineHeight = (36.0 * fontScale).sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
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
private fun TajweedRuleCard(
    rule: TajweedRuleItem,
    mastered: Boolean,
    onToggleMastered: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVerses by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (mastered) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (rule.matnChapterTitleAr.isNotBlank()) {
                            "${rule.category.titleAr} • ${rule.matnChapterTitleAr}"
                        } else rule.category.titleAr,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = rule.titleAr,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = onToggleMastered,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (mastered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = if (mastered) "تم إتقان الحكم" else "تحديد كمتقن",
                        tint = if (mastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "الحروف: ${rule.lettersAr}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Text(
                text = rule.explanationAr,
                style = MaterialTheme.typography.bodyLarge
            )

            if (rule.matnVerses.isNotEmpty()) {
                TextButton(
                    onClick = { showVerses = !showVerses },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        if (showVerses) "إخفاء أبيات التحفة (${rule.matnVerses.size})"
                        else "عرض أبيات متن تحفة الأطفال (${rule.matnVerses.size})"
                    )
                }
                AnimatedVisibility(visible = showVerses) {
                    MushafParchmentFrame {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            rule.matnVerses.forEach { verse ->
                                Text(
                                    text = verse,
                                    fontFamily = AmiriFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "أمثلة تطبيقية:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = rule.examplesAr,
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تنبيه: ${rule.commonMistakeAr}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun TajweedQuizView(
    quizCorrectToday: Long,
    onCorrect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var questions by remember { mutableStateOf(TajweedCatalog.quizQuestions) }
    var qIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var sessionCorrect by remember { mutableIntStateOf(0) }
    var sessionAnswered by remember { mutableIntStateOf(0) }

    val question = questions[qIndex]
    val answered = selectedOption != null
    val isCorrect = selectedOption == question.correctIndex

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "السؤال ${qIndex + 1} من ${questions.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "الجلسة: $sessionCorrect/$sessionAnswered • اليوم: $quizCorrectToday",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    questions = TajweedCatalog.quizQuestions.shuffled()
                                    qIndex = 0
                                    selectedOption = null
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("خلط", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }

                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { ((qIndex + 1).toFloat() / questions.size.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = question.promptAr,
                        fontFamily = AmiriFontFamily,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    question.options.forEachIndexed { idx, option ->
                        val chosen = selectedOption == idx
                        val bg = when {
                            !answered -> MaterialTheme.colorScheme.surface
                            idx == question.correctIndex -> MaterialTheme.colorScheme.primaryContainer
                            chosen -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surface
                        }
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                if (!answered) {
                                    selectedOption = idx
                                    sessionAnswered++
                                    if (idx == question.correctIndex) {
                                        sessionCorrect++
                                        onCorrect()
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    if (answered) {
                        Surface(
                            color = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${if (isCorrect) "أحسنت! " else "التوضيح: "}${question.rationaleAr}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = { selectedOption = null },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("إعادة")
                            }
                            Button(
                                onClick = {
                                    selectedOption = null
                                    qIndex = (qIndex + 1) % questions.size
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("السؤال التالي")
                            }
                        }
                    }
                }
            }
        }
    }
}
