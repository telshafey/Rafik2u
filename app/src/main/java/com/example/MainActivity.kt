package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.LocalDevRafikRepository
import com.example.data.QuranMushafRepository
import com.example.data.RafikRepository
import com.example.model.QuranQuestionType
import com.example.model.QuranQuizScopeMode
import com.example.model.QuranQuizTimerMode
import com.example.model.UiState
import com.example.notifications.RafikNotificationManager
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JournalScreen
import com.example.ui.screens.QuranRevisionScreen
import com.example.ui.screens.QuranTabMode
import com.example.ui.screens.SunnahDuasScreen
import com.example.ui.screens.TajweedScreen
import com.example.ui.theme.RafikTheme
import com.example.viewmodel.RafikViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object RafikRoutes {
    const val HOME = "home"
    const val QURAN = "quran"
    const val TAJWEED = "tajweed"
    const val SUNNAH = "sunnah"
    const val JOURNAL = "journal"
}

private data class NavTabItem(
    val route: String,
    val labelAr: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val rafikTabs = listOf(
    NavTabItem(RafikRoutes.HOME, "التعهد", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_home"),
    NavTabItem(RafikRoutes.QURAN, "القرآن", Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook, "nav_quran"),
    NavTabItem(RafikRoutes.TAJWEED, "التجويد", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq, "nav_tajweed"),
    NavTabItem(RafikRoutes.SUNNAH, "السنة", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_sunnah"),
    NavTabItem(RafikRoutes.JOURNAL, "سجلي", Icons.Filled.EditNote, Icons.Outlined.EditNote, "nav_journal")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RafikNotificationManager.ensureNotificationChannel(this)
        RafikNotificationManager.rescheduleAllEnabled(this)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                RafikTheme {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val sessionPrefs = remember(context) {
        context.applicationContext.getSharedPreferences("rafik_session_prefs", Context.MODE_PRIVATE)
    }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var isDevMode by remember { mutableStateOf(sessionPrefs.getBoolean("dev_quick_mode", false)) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                isDevMode = false
                sessionPrefs.edit().putBoolean("dev_quick_mode", false).apply()
            }
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    when {
        user != null -> {
            val uid = user.uid
            key("cloud_$uid") {
                RafikShell(
                    currentUserId = uid,
                    userDisplayName = user.displayName ?: "رفيق القرآن",
                    userEmail = user.email,
                    isDevMode = false,
                    onExitDevModeToAuth = {}
                )
            }
        }
        isDevMode -> {
            key("dev_local_session") {
                RafikShell(
                    currentUserId = "dev_local_hafiz",
                    userDisplayName = "حافظ القرآن (وضع التطوير)",
                    userEmail = null,
                    isDevMode = true,
                    onExitDevModeToAuth = {
                        isDevMode = false
                        sessionPrefs.edit().putBoolean("dev_quick_mode", false).apply()
                    }
                )
            }
        }
        else -> {
            AuthScreen(
                onAuthSuccess = {
                    isDevMode = false
                    sessionPrefs.edit().putBoolean("dev_quick_mode", false).apply()
                    currentUser = auth.currentUser
                },
                onDevQuickEntry = {
                    isDevMode = true
                    sessionPrefs.edit().putBoolean("dev_quick_mode", true).apply()
                }
            )
        }
    }
}

@Composable
private fun RafikShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    isDevMode: Boolean,
    onExitDevModeToAuth: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModelKey = "${if (isDevMode) "dev" else "cloud"}_$currentUserId"
    val viewModel: RafikViewModel = viewModel(
        key = viewModelKey,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val dataSource = if (isDevMode) {
                    LocalDevRafikRepository(app)
                } else {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    RafikRepository(db)
                }
                RafikViewModel(
                    repository = dataSource,
                    mushafRepository = QuranMushafRepository(app),
                    currentUserId = currentUserId,
                    initialDisplayName = userDisplayName
                )
            }
        }
    )

    val userProfileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val juzProgressState by viewModel.juzProgressState.collectAsStateWithLifecycle()
    val tajweedProgress by viewModel.tajweedProgressState.collectAsStateWithLifecycle()
    val dailyLogState by viewModel.dailyLogState.collectAsStateWithLifecycle()
    val customAzkarState by viewModel.customAzkarState.collectAsStateWithLifecycle()
    val reflectionsState by viewModel.reflectionsState.collectAsStateWithLifecycle()
    val favoriteKeys by viewModel.favoriteKeysState.collectAsStateWithLifecycle()
    val zikrCounters by viewModel.zikrCounters.collectAsStateWithLifecycle()
    val tasbihState by viewModel.tasbihState.collectAsStateWithLifecycle()
    val gamification by viewModel.gamificationState.collectAsStateWithLifecycle()
    val quizAttempts by viewModel.quizAttemptsState.collectAsStateWithLifecycle()
    val quizArenaState by viewModel.quizArenaState.collectAsStateWithLifecycle()
    val reviewedMutashabihatIds by viewModel.reviewedMutashabihatIds.collectAsStateWithLifecycle()
    val mushafReaderState by viewModel.mushafReaderState.collectAsStateWithLifecycle()
    val lastReadSurahNumber by viewModel.lastReadSurahNumber.collectAsStateWithLifecycle()
    val readSurahsCount by viewModel.readSurahsCount.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    var requestedQuranTabMode by remember { mutableStateOf(QuranTabMode.QUIZ_ARENA) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.statusMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                        rafikTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.labelAr
                                    )
                                },
                                label = { Text(tab.labelAr) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isDevMode) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "وضع التطوير المحلي — محفوظ على الجهاز",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onExitDevModeToAuth) {
                                Text("ربط بحساب Google")
                            }
                        }
                    }
                }

                Row(modifier = Modifier.weight(1f)) {
                    if (useRail) {
                        NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                            rafikTabs.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelAr
                                        )
                                    },
                                    label = { Text(tab.labelAr) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = RafikRoutes.HOME
                        ) {
                            composable(RafikRoutes.HOME) {
                                val juzMap = (juzProgressState as? UiState.Success)?.data ?: emptyMap()
                                val smartQueue = remember(juzMap) { viewModel.computeSmartQueue(juzMap) }
                                val didQuizToday = remember(quizAttempts, viewModel.todayKey) {
                                    quizAttempts.any { it.dateKey == viewModel.todayKey }
                                }
                                HomeScreen(
                                    userProfileState = userProfileState,
                                    juzProgressState = juzProgressState,
                                    tajweedProgress = tajweedProgress,
                                    dailyLogState = dailyLogState,
                                    gamification = gamification,
                                    smartQueue = smartQueue,
                                    onQuickMarkJuzReviewed = { juzNumber, level, note ->
                                        viewModel.recordJuzRevision(juzNumber, level, note, true)
                                    },
                                    onUpdatePagesRevised = viewModel::updatePagesRevisedToday,
                                    onToggleAzkar = viewModel::toggleAzkarStatus,
                                    onStartQuizShortcut = {
                                        requestedQuranTabMode = QuranTabMode.QUIZ_ARENA
                                        navigateToTab(RafikRoutes.QURAN)
                                    },
                                    onOpenMushafShortcut = {
                                        requestedQuranTabMode = QuranTabMode.MUSHAF
                                        navigateToTab(RafikRoutes.QURAN)
                                    },
                                    onLaunchQuickPreset = { preset ->
                                        viewModel.startQuranQuiz(
                                            preset.scopeMode,
                                            preset.selectedJuz,
                                            null,
                                            preset.questionTypes,
                                            preset.questionCount,
                                            preset.timerMode
                                        )
                                        requestedQuranTabMode = QuranTabMode.QUIZ_ARENA
                                        navigateToTab(RafikRoutes.QURAN)
                                    },
                                    onQuizSingleJuz = { juzNum ->
                                        viewModel.startQuranQuiz(
                                            QuranQuizScopeMode.SINGLE_JUZ,
                                            setOf(juzNum),
                                            null,
                                            QuranQuestionType.entries.toSet(),
                                            5,
                                            QuranQuizTimerMode.UNTIMED
                                        )
                                        requestedQuranTabMode = QuranTabMode.QUIZ_ARENA
                                        navigateToTab(RafikRoutes.QURAN)
                                    },
                                    didQuizToday = didQuizToday,
                                    readSurahsCount = readSurahsCount,
                                    onNavigateToQuran = {
                                        requestedQuranTabMode = QuranTabMode.JUZ_PLAN
                                        navigateToTab(RafikRoutes.QURAN)
                                    },
                                    onNavigateToTajweed = { navigateToTab(RafikRoutes.TAJWEED) },
                                    onNavigateToSunnah = { navigateToTab(RafikRoutes.SUNNAH) }
                                )
                            }
                            composable(RafikRoutes.QURAN) {
                                QuranRevisionScreen(
                                    juzProgressState = juzProgressState,
                                    quizArenaState = quizArenaState,
                                    mushafReaderState = mushafReaderState,
                                    lastReadSurahNumber = lastReadSurahNumber,
                                    readSurahsCount = readSurahsCount,
                                    gamification = gamification,
                                    recentAttempts = quizAttempts,
                                    reviewedMutashabihatIds = reviewedMutashabihatIds,
                                    initialMode = requestedQuranTabMode,
                                    onOpenSurahInMushaf = viewModel::openSurahInMushaf,
                                    onCloseMushafSurah = viewModel::closeMushafSurah,
                                    onSaveJuzRevision = viewModel::recordJuzRevision,
                                    onStartQuiz = viewModel::startQuranQuiz,
                                    onUpdateWrittenAnswer = viewModel::updateWrittenQuizAnswer,
                                    onToggleHint = viewModel::toggleQuizHint,
                                    onSubmitMc = { opt -> viewModel.submitMultipleChoiceAnswer(opt, byVoice = false) },
                                    onSubmitVoiceMc = viewModel::submitVoiceTranscriptForMc,
                                    onSubmitWritten = viewModel::submitWrittenQuizAnswer,
                                    onNextQuestion = viewModel::advanceNextQuizQuestion,
                                    onRetakeSameQuiz = viewModel::retakeLastQuranQuiz,
                                    onResetQuizToSetup = viewModel::resetQuizToSetup,
                                    onMarkMutashabihReviewed = viewModel::markMutashabihReviewed,
                                    onSaveNoteToJournal = viewModel::addReflection
                                )
                            }
                            composable(RafikRoutes.TAJWEED) {
                                val quizToday = (dailyLogState as? UiState.Success)?.data?.tajweedQuizCorrect ?: 0L
                                TajweedScreen(
                                    tajweedProgress = tajweedProgress,
                                    quizCorrectToday = quizToday,
                                    onToggleRuleMastery = viewModel::toggleTajweedMastery,
                                    onQuizCorrectAnswer = viewModel::recordTajweedQuizHit
                                )
                            }
                            composable(RafikRoutes.SUNNAH) {
                                val fontScale = (userProfileState as? UiState.Success)?.data?.zikrFontScale ?: 1.0
                                val totalTasbih = (userProfileState as? UiState.Success)?.data?.totalTasbihCount ?: 0L
                                SunnahDuasScreen(
                                    zikrCounters = zikrCounters,
                                    favoriteKeys = favoriteKeys,
                                    customAzkarState = customAzkarState,
                                    tasbihState = tasbihState,
                                    totalTasbihCount = totalTasbih,
                                    fontScale = fontScale,
                                    onIncrementZikr = viewModel::incrementZikrCounter,
                                    onResetZikr = viewModel::resetZikrCounter,
                                    onToggleFavorite = viewModel::toggleFavorite,
                                    onAddCustomZikr = viewModel::addCustomZikr,
                                    onIncrementCustomZikr = viewModel::incrementCustomZikr,
                                    onDeleteCustomZikr = viewModel::deleteCustomZikr,
                                    onUpdateFontScale = viewModel::updateFontScale,
                                    onSelectTasbihPhrase = viewModel::selectTasbihPhrase,
                                    onTapTasbih = viewModel::tapTasbih
                                )
                            }
                            composable(RafikRoutes.JOURNAL) {
                                JournalScreen(
                                    userProfileState = userProfileState,
                                    reflectionsState = reflectionsState,
                                    userEmail = userEmail,
                                    isDevMode = isDevMode,
                                    cloudSyncStatus = cloudSyncStatus,
                                    onVerifyCloudSync = viewModel::verifyCloudConnection,
                                    onShowStatusMessage = { msg ->
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    },
                                    onUpdateRevisionGoal = viewModel::updateRevisionGoal,
                                    onAddReflection = viewModel::addReflection,
                                    onUpdateReflection = viewModel::updateReflection,
                                    onDeleteReflection = viewModel::deleteReflection,
                                    onSignOutClick = {
                                        if (isDevMode) {
                                            onExitDevModeToAuth()
                                        } else {
                                            signOut(credentialManager = credentialManager, scope = scope)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
