package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.GamificationCatalog
import com.example.data.QuranCatalog
import com.example.data.SunnahDuaCatalog
import com.example.data.TajweedCatalog
import com.example.model.DailyWorshipLog
import com.example.model.GamificationProfile
import com.example.model.JuzMasteryLevel
import com.example.model.JuzProgress
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.IslamicGeometricOverlay
import com.example.ui.components.MushafParchmentFrame
import com.example.ui.theme.AmiriFontFamily
import com.example.viewmodel.SmartJuzRecommendation
import java.util.Calendar

@Composable
fun HomeScreen(
    userProfileState: UiState<UserProfile>,
    juzProgressState: UiState<Map<Int, JuzProgress>>,
    tajweedProgress: Map<String, Boolean>,
    dailyLogState: UiState<DailyWorshipLog>,
    gamification: GamificationProfile,
    smartQueue: List<SmartJuzRecommendation>,
    onQuickMarkJuzReviewed: (Int, JuzMasteryLevel, String) -> Unit,
    onUpdatePagesRevised: (Long) -> Unit,
    onToggleAzkar: (String, Boolean) -> Unit,
    onStartQuizShortcut: () -> Unit,
    onOpenMushafShortcut: () -> Unit,
    onLaunchQuickPreset: (QuickChallengePreset) -> Unit = {},
    onQuizSingleJuz: (Int) -> Unit = {},
    didQuizToday: Boolean = false,
    readSurahsCount: Int = 0,
    onNavigateToQuran: () -> Unit,
    onNavigateToTajweed: () -> Unit,
    onNavigateToSunnah: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (userProfileState as? UiState.Success)?.data
    val juzMap = (juzProgressState as? UiState.Success)?.data ?: emptyMap()
    val dailyLog = (dailyLogState as? UiState.Success)?.data ?: DailyWorshipLog()
    var showBadgesDialog by remember { mutableStateOf(false) }

    val hijriDate = remember { QuranCatalog.formatHijriDateAr() }
    val gregorianDate = remember { QuranCatalog.formatGregorianDateAr() }
    val dailyHadith = remember {
        val virtues = SunnahDuaCatalog.items.filter { it.category == com.example.model.SunnahDuaCategory.QURAN_VIRTUES }
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        virtues[day % virtues.size]
    }

    val masteredJuzCount = juzMap.values.count { it.masteryLevel == JuzMasteryLevel.MASTERED.key }
    val activeJuzCount = juzMap.values.count { it.masteryLevel != JuzMasteryLevel.NOT_STARTED.key }
    val tajweedMasteredCount = tajweedProgress.values.count { it }
    val currentRank = GamificationCatalog.rankForXp(gamification.xp)
    val nextRank = GamificationCatalog.nextRankForXp(gamification.xp)
    val unlockedBadgesCount = gamification.unlockedBadgeIds().size
    val unlockedRewardsCount = GamificationCatalog.rewards.count {
        GamificationCatalog.isRewardUnlocked(it, gamification)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Hafiz Oasis Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(195.dp)
                        .clip(RoundedCornerShape(26.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_oasis),
                        contentDescription = "لوحة التعهد والقرآن",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0x55062B22), Color(0xE0062B22))
                                )
                            )
                    )
                    IslamicGeometricOverlay(
                        lineColor = Color(0xFFFFF4D6),
                        alpha = 0.08f,
                        modifier = Modifier.fillMaxSize()
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0x33FFFFFF),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = hijriDate,
                                    color = Color(0xFFFFF4D6),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Surface(
                                color = Color(0xCCBA8326),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = "مجموع المراجعات: ${profile?.totalRevisionsCount ?: 0L}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column {
                            val greetingSuffix = profile?.displayName?.takeIf { it.isNotBlank() }?.let { "، $it" }.orEmpty()
                            Text(
                                text = "أهلاً بك يا رفيق القرآن$greetingSuffix",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$gregorianDate • المتقن $masteredJuzCount / ٣٠ جزءاً",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE7F5EE)
                            )
                        }
                    }
                }

                // Gamification Rank & Badges Card + Quiz Shortcut
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .clickable { showBadgesDialog = true },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f))
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
                                Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.secondary)
                                Column {
                                    Text(
                                        text = "الرتبة: ${currentRank.titleAr}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${gamification.xp} XP • شارات $unlockedBadgesCount/${GamificationCatalog.badges.size} • جوائز $unlockedRewardsCount/${GamificationCatalog.rewards.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                                        text = " ${gamification.currentStreakDays} يوم",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB34700)
                                    )
                                }
                            }
                        }

                        if (nextRank != null) {
                            val span = (nextRank.minXp - currentRank.minXp).coerceAtLeast(1L)
                            val rankProgress = ((gamification.xp - currentRank.minXp).toFloat() / span.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { rankProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }

                        Button(
                            onClick = onStartQuizShortcut,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("home_start_quiz_cta")
                        ) {
                            Icon(Icons.Default.Quiz, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("اختبر حفظك الآن (بالمايك أو المؤقت)")
                        }

                        OutlinedButton(
                            onClick = onOpenMushafShortcut,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("home_open_mushaf_cta")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("استعراض المصحف الكامل (١١٤ سورة)")
                        }
                    }
                }

                // 1-Tap Quick Challenges Carousel
                QuickChallengesCarousel(onLaunchPreset = onLaunchQuickPreset)

                // Daily Motivational Missions
                DailyMissionsCard(
                    dailyLog = dailyLog,
                    didQuizToday = didQuizToday,
                    readSurahsCount = readSurahsCount,
                    onAddQuickWirdPages = { onUpdatePagesRevised(dailyLog.pagesRevisedToday + 2) },
                    onStartQuickQuiz = onStartQuizShortcut,
                    onNavigateToTajweedOrMushaf = onOpenMushafShortcut
                )

                // Mastery Ring & Today's Revision Target Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val goal = (profile?.dailyRevisionTargetPages ?: 20L).coerceAtLeast(1L)
                        val revised = dailyLog.pagesRevisedToday
                        val goalRatio = (revised.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
                        val animatedRatio by animateFloatAsState(targetValue = goalRatio, label = "daily_pages_ring")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ورد التعهد اليومي",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "راجعت اليوم $revised من $goal صفحة",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = CircleShape
                                    ) {
                                        IconButton(
                                            onClick = { if (revised > 0) onUpdatePagesRevised(revised - 2) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("home_pages_minus")
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص صفحتين")
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    ) {
                                        IconButton(
                                            onClick = { onUpdatePagesRevised(revised + 2) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("home_pages_plus")
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = "إضافة صفحتين",
                                                tint = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.size(76.dp)
                            ) {
                                val track = MaterialTheme.colorScheme.surfaceContainerHigh
                                val active = MaterialTheme.colorScheme.secondary
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = track,
                                        startAngle = -90f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                    drawArc(
                                        color = active,
                                        startAngle = -90f,
                                        sweepAngle = 360f * animatedRatio,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                                Text(
                                    text = "${(goalRatio * 100).toInt()}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = dailyLog.morningAzkarDone,
                                onClick = { onToggleAzkar("morningAzkarDone", !dailyLog.morningAzkarDone) },
                                label = { Text("أذكار الصباح") },
                                leadingIcon = if (dailyLog.morningAzkarDone) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = dailyLog.eveningAzkarDone,
                                onClick = { onToggleAzkar("eveningAzkarDone", !dailyLog.eveningAzkarDone) },
                                label = { Text("أذكار المساء") },
                                leadingIcon = if (dailyLog.eveningAzkarDone) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }

                // Quick Stats Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(
                        title = "أجزاء نشطة",
                        value = "$activeJuzCount / ٣٠",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = onNavigateToQuran,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "إتقان التجويد",
                        value = "$tajweedMasteredCount / ${TajweedCatalog.rules.size}",
                        icon = Icons.Default.GraphicEq,
                        onClick = onNavigateToTajweed,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        title = "تسبيح اليوم",
                        value = "${dailyLog.tasbihDoneToday}",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onNavigateToSunnah,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Visual 30-Juz Khatmah Mosaic
                KhatmahMosaicGridCard(
                    progressMap = juzMap,
                    onQuickMarkJuzReviewed = onQuickMarkJuzReviewed,
                    onQuizSingleJuz = onQuizSingleJuz,
                    onOpenFullPlan = onNavigateToQuran
                )

                // Smart Revision Queue (أولوية التعهد اليوم)
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(
                                text = "أولويات التعهد والمراجعة الآن",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "خطة ٣٠ جزءاً",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.clickable(onClick = onNavigateToQuran)
                            )
                        }

                        smartQueue.forEach { rec ->
                            val currentLevel = JuzMasteryLevel.fromRaw(rec.progress?.masteryLevel ?: "")
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.juzInfo.nameAr,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = rec.juzInfo.surahRangeAr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = rec.reasonAr,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Button(
                                        onClick = {
                                            val nextLevel = if (currentLevel == JuzMasteryLevel.NOT_STARTED) {
                                                JuzMasteryLevel.NEEDS_REVIEW
                                            } else currentLevel
                                            onQuickMarkJuzReviewed(
                                                rec.juzInfo.number,
                                                nextLevel,
                                                rec.progress?.mistakeNote ?: ""
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Text("راجعته")
                                    }
                                }
                            }
                        }
                    }
                }

                // Hadith of the Day on Quran Revision in parchment manuscript frame
                MushafParchmentFrame {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "حديث التعهد — ${dailyHadith.titleAr}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = dailyHadith.arabicText,
                            fontFamily = AmiriFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${dailyHadith.sourceAr} • ${dailyHadith.benefitAr}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (masteredJuzCount / 30f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            }
        }
    }

    if (showBadgesDialog) {
        BadgesShowcaseDialog(
            gamification = gamification,
            onDismiss = { showBadgesDialog = false }
        )
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
