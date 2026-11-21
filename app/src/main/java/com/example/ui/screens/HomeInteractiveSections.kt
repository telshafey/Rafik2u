package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.QuranCatalog
import com.example.model.DailyWorshipLog
import com.example.model.JuzMasteryLevel
import com.example.model.JuzProgress
import com.example.model.QuranQuestionType
import com.example.model.QuranQuizScopeMode
import com.example.model.QuranQuizTimerMode
import com.example.ui.components.IslamicGeometricOverlay
import com.example.ui.components.MushafParchmentFrame

data class QuickChallengePreset(
    val id: String,
    val titleAr: String,
    val subtitleAr: String,
    val badgeAr: String,
    val gradientColors: List<Color>,
    val icon: ImageVector,
    val scopeMode: QuranQuizScopeMode,
    val selectedJuz: Set<Int>,
    val questionTypes: Set<QuranQuestionType>,
    val questionCount: Int,
    val timerMode: QuranQuizTimerMode
)

private val quickPresets = listOf(
    QuickChallengePreset(
        id = "preset_amma_speed",
        titleAr = "تحدي جزء عمّ السريع",
        subtitleAr = "٥ أسئلة متنوعة • مؤقت ٣٠ ث/سؤال",
        badgeAr = "+٥٠٪ XP",
        gradientColors = listOf(Color(0xFF0E5A49), Color(0xFF19856C)),
        icon = Icons.Default.Timer,
        scopeMode = QuranQuizScopeMode.SINGLE_JUZ,
        selectedJuz = setOf(30),
        questionTypes = QuranQuestionType.entries.toSet(),
        questionCount = 5,
        timerMode = QuranQuizTimerMode.PER_QUESTION_30S
    ),
    QuickChallengePreset(
        id = "preset_open_close_mic",
        titleAr = "تسميع الأوائل والخواتيم",
        subtitleAr = "أول السورة وآخر ٥ آيات • مناسب للمايك",
        badgeAr = "بالمايك",
        gradientColors = listOf(Color(0xFF8E5D12), Color(0xFFC58927)),
        icon = Icons.Default.Mic,
        scopeMode = QuranQuizScopeMode.SINGLE_JUZ,
        selectedJuz = setOf(30),
        questionTypes = setOf(QuranQuestionType.SURAH_BEGINNING, QuranQuestionType.SURAH_LAST_5),
        questionCount = 4,
        timerMode = QuranQuizTimerMode.UNTIMED
    ),
    QuickChallengePreset(
        id = "preset_full_quran_race",
        titleAr = "سباق كامل القرآن",
        subtitleAr = "٣ دقائق شاملة من ٣٠ جزءاً",
        badgeAr = "+٤٠٪ XP",
        gradientColors = listOf(Color(0xFF144958), Color(0xFF1E7187)),
        icon = Icons.Default.Bolt,
        scopeMode = QuranQuizScopeMode.FULL_QURAN,
        selectedJuz = (1..30).toSet(),
        questionTypes = setOf(QuranQuestionType.MULTIPLE_CHOICE, QuranQuestionType.COMPLETE_AYAH),
        questionCount = 6,
        timerMode = QuranQuizTimerMode.TOTAL_EXAM_3M
    )
)

@Composable
fun DailyMissionsCard(
    dailyLog: DailyWorshipLog,
    didQuizToday: Boolean,
    readSurahsCount: Int,
    onAddQuickWirdPages: () -> Unit,
    onStartQuickQuiz: () -> Unit,
    onNavigateToTajweedOrMushaf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val wirdDone = dailyLog.pagesRevisedToday > 0L || dailyLog.morningAzkarDone || dailyLog.eveningAzkarDone
    val tajweedOrMushafDone = dailyLog.tajweedQuizCorrect > 0L || readSurahsCount > 0
    val completedCount = listOf(wirdDone, didQuizToday, tajweedOrMushafDone).count { it }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = "مهامك التشجيعية اليوم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Surface(
                    color = if (completedCount == 3) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text(
                        text = "$completedCount / ٣",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { completedCount / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (completedCount == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            DailyMissionRow(
                title = "ورد اليوم أو أذكار الصباح والمساء",
                done = wirdDone,
                actionLabel = "+٢ صفحة",
                onClick = onAddQuickWirdPages
            )
            DailyMissionRow(
                title = "اختبار حفظ سريع (بالصوت أو المؤقت)",
                done = didQuizToday,
                actionLabel = "ابدأ",
                onClick = onStartQuickQuiz
            )
            DailyMissionRow(
                title = "قراءة سورة بالمصحف أو تدريب التحفة",
                done = tajweedOrMushafDone,
                actionLabel = "تصفح",
                onClick = onNavigateToTajweedOrMushaf
            )
        }
    }
}

@Composable
private fun DailyMissionRow(
    title: String,
    done: Boolean,
    actionLabel: String,
    onClick: () -> Unit
) {
    Surface(
        color = if (done) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !done, onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            if (!done) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickChallengesCarousel(
    onLaunchPreset: (QuickChallengePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "انطلق بضغطة واحدة ⚡",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(quickPresets, key = { it.id }) { preset ->
                Box(
                    modifier = Modifier
                        .width(236.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.linearGradient(preset.gradientColors))
                        .clickable { onLaunchPreset(preset) }
                        .testTag(preset.id)
                ) {
                    IslamicGeometricOverlay(
                        modifier = Modifier.matchParentSize(),
                        lineColor = Color.White,
                        alpha = 0.08f
                    )
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0x33FFFFFF),
                                shape = CircleShape,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(preset.icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Surface(
                                color = Color(0xCCFFFFFF),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = preset.badgeAr,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = preset.gradientColors.first(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = preset.titleAr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = preset.subtitleAr,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE9F5F0)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color(0xFFFFE4A0), modifier = Modifier.size(16.dp))
                            Text(
                                text = "ابدأ فوراً",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFE4A0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KhatmahMosaicGridCard(
    progressMap: Map<Int, JuzProgress>,
    onQuickMarkJuzReviewed: (Int, JuzMasteryLevel, String) -> Unit,
    onQuizSingleJuz: (Int) -> Unit,
    onOpenFullPlan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedJuzNumber by remember { mutableStateOf<Int?>(null) }
    val masteredCount = progressMap.values.count { it.masteryLevel == JuzMasteryLevel.MASTERED.key }

    MushafParchmentFrame(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "خريطة الختمة البصرية (٣٠ جزءاً)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "أُتقن $masteredCount / ٣٠ • اضغط على أي جزء لاختباره أو مراجعته",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onOpenFullPlan) {
                    Text("التفاصيل")
                }
            }

            // 6x5 Mosaic Grid for Juz 1..30
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (col in 0 until 6) {
                            val juzNum = row * 6 + col + 1
                            val prog = progressMap[juzNum]
                            val level = JuzMasteryLevel.fromRaw(prog?.masteryLevel ?: "")
                            val (bgColor, textColor) = when (level) {
                                JuzMasteryLevel.MASTERED -> MaterialTheme.colorScheme.primary to Color.White
                                JuzMasteryLevel.NEEDS_REVIEW -> Color(0xFFD97706) to Color.White
                                JuzMasteryLevel.MEMORIZING -> MaterialTheme.colorScheme.tertiary to Color.White
                                JuzMasteryLevel.NOT_STARTED -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Surface(
                                color = bgColor,
                                shape = RoundedCornerShape(10.dp),
                                border = if (level == JuzMasteryLevel.NOT_STARTED) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.15f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedJuzNumber = juzNum }
                                    .testTag("khatmah_juz_tile_$juzNum")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$juzNum",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Compact Color Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendChip("متقن", MaterialTheme.colorScheme.primary)
                LegendChip("تثبيت", Color(0xFFD97706))
                LegendChip("قيد الحفظ", MaterialTheme.colorScheme.tertiary)
                LegendChip("جديد", MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    val chosenJuz = selectedJuzNumber
    if (chosenJuz != null) {
        val juzInfo = QuranCatalog.allJuz.firstOrNull { it.number == chosenJuz }
        val existingProg = progressMap[chosenJuz]
        val currentLvl = JuzMasteryLevel.fromRaw(existingProg?.masteryLevel ?: "")

        AlertDialog(
            onDismissRequest = { selectedJuzNumber = null },
            title = { Text(juzInfo?.nameAr ?: "الجزء $chosenJuz") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (juzInfo != null) {
                        Text(
                            text = juzInfo.surahRangeAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "الحالة الحالية: ${currentLvl.labelAr} • المراجعات: ${existingProg?.revisionCount ?: 0L}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedJuzNumber = null
                        onQuizSingleJuz(chosenJuz)
                    }
                ) {
                    Icon(Icons.Default.GraphicEq, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("اختبرني بالجزء الآن")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            onQuickMarkJuzReviewed(
                                chosenJuz,
                                JuzMasteryLevel.MASTERED,
                                existingProg?.mistakeNote ?: ""
                            )
                            selectedJuzNumber = null
                        }
                    ) {
                        Text("تحديد كمتقن (+٣٠)")
                    }
                    TextButton(onClick = { selectedJuzNumber = null }) {
                        Text("إغلاق")
                    }
                }
            }
        )
    }
}

@Composable
private fun LegendChip(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
