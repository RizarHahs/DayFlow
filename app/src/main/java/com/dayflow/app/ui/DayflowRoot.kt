package com.dayflow.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dayflow.app.domain.model.ItemKind
import com.dayflow.app.domain.model.NewItem
import com.dayflow.app.domain.model.TaskKind
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dayflow.app.ui.feature.insights.InsightsScreen
import com.dayflow.app.ui.feature.messages.MessageEditor
import com.dayflow.app.ui.feature.messages.MessagesScreen
import com.dayflow.app.ui.feature.review.ReviewScreen
import com.dayflow.app.ui.feature.alarm.AlarmEditor
import com.dayflow.app.ui.feature.alarm.AlarmsScreen
import com.dayflow.app.ui.feature.settings.SettingsScreen
import com.dayflow.app.ui.feature.timer.TimerMiniBar
import com.dayflow.app.ui.feature.timer.TimerScreen
import com.dayflow.app.ui.feature.timer.TimerViewModel
import com.dayflow.app.ui.feature.editor.HabitEditor
import com.dayflow.app.ui.feature.editor.IdeaEditor
import com.dayflow.app.ui.feature.editor.RoadmapEditor
import com.dayflow.app.ui.feature.editor.TaskEditor
import com.dayflow.app.ui.feature.home.DayActions
import com.dayflow.app.ui.feature.home.HomeScreen
import com.dayflow.app.ui.feature.ideas.IdeasScreen
import com.dayflow.app.ui.feature.quick.QuickActionFab
import com.dayflow.app.ui.feature.quick.QuickAddSheet
import com.dayflow.app.ui.feature.quick.QuickKind
import com.dayflow.app.ui.feature.roadmap.RoadmapScreen
import com.dayflow.app.ui.feature.tasks.Lane
import com.dayflow.app.ui.feature.tasks.TasksScreen
import com.dayflow.app.ui.nav.DayflowBottomBar
import com.dayflow.app.ui.nav.Routes
import com.dayflow.app.ui.nav.Tab
import com.dayflow.app.ui.theme.Df

@Composable
fun DayflowRoot(
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    vm: DayViewModel = hiltViewModel(),
    timerVm: TimerViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val timer by timerVm.current.collectAsStateWithLifecycle()
    val attention by vm.setupAttention.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshSetup() }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = route == null || Tab.entries.any { it.route == route }
    val currentTab = Tab.fromRoute(route)

    var fabExpanded by rememberSaveable { mutableStateOf(false) }
    var sheetKind by rememberSaveable { mutableStateOf<QuickKind?>(null) }
    var requestedLane by rememberSaveable { mutableStateOf<Lane?>(null) }

    fun goTab(tab: Tab) {
        fabExpanded = false
        if (tab.route != route) {
            nav.navigate(tab.route) {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Not remembered on purpose: closures must see the current route.
    val actions = run {
        DayActions(
            toggleHabit = vm::toggleHabit,
            toggleTask = vm::toggleTask,
            addNormal = { title -> vm.add(NewItem(ItemKind.NORMAL, title)) },
            togglePin = vm::togglePin,
            captureIdea = { sheetKind = QuickKind.IDEA },
            openLane = { lane -> requestedLane = lane; goTab(Tab.TASKS) },
            openIdea = { id -> nav.navigate(Routes.idea(id)) },
            openIdeas = { goTab(Tab.IDEAS) },
            openAlarms = { nav.navigate(Routes.ALARMS) },
            openSettings = { nav.navigate(Routes.SETTINGS) },
            openReview = { nav.navigate(Routes.review(vm.todayNow())) },
            openMessages = { nav.navigate(Routes.MESSAGES) },
        )
    }

    // Floating bar (68dp) + gaps + FAB (60dp) + breathing room, plus the system gesture area.
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 176.dp
    val close: () -> Unit = { nav.popBackStack() }

    BackHandler(enabled = fabExpanded) { fabExpanded = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(Df.colors.background)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0x2410B981), Color.Transparent),
                        center = Offset(0f, 0f),
                        radius = size.width * 1.15f,
                    )
                )
            }
    ) {
        NavHost(
            navController = nav,
            startDestination = Tab.HOME.route,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Tab.HOME.route) {
                state?.let { HomeScreen(it, actions, bottomPadding, attention) }
            }
            composable(Tab.ROADMAP.route) {
                RoadmapScreen(
                    bottomPadding = bottomPadding,
                    onAdd = { nav.navigate(Routes.slot(0)) },
                    onEdit = { id -> nav.navigate(Routes.slot(id)) },
                )
            }
            composable(Tab.TASKS.route) {
                TasksScreen(
                    requestedLane = requestedLane,
                    onLaneShown = { requestedLane = null },
                    bottomPadding = bottomPadding,
                    onEditTask = { id, kind -> nav.navigate(Routes.task(id, kind)) },
                    onNewTask = { kind -> nav.navigate(Routes.task(0, kind)) },
                    onEditHabit = { id -> nav.navigate(Routes.habit(id)) },
                    onNewHabit = { nav.navigate(Routes.habit(0)) },
                )
            }
            composable(Tab.IDEAS.route) {
                IdeasScreen(bottomPadding = bottomPadding, onOpenIdea = { id -> nav.navigate(Routes.idea(id)) })
            }
            composable(Tab.INSIGHTS.route) {
                InsightsScreen(bottomPadding = bottomPadding, onOpenDay = { day -> nav.navigate(Routes.review(day)) })
            }

            val idArg = navArgument("id") { type = NavType.LongType }
            composable(Routes.HABIT, arguments = listOf(idArg)) { HabitEditor(onClose = close) }
            composable(
                Routes.TASK,
                arguments = listOf(idArg, navArgument("kind") { type = NavType.StringType }),
            ) { TaskEditor(onClose = close) }
            composable(Routes.IDEA, arguments = listOf(idArg)) { IdeaEditor(onClose = close) }
            composable(Routes.SLOT, arguments = listOf(idArg)) { RoadmapEditor(onClose = close) }
            composable(Routes.ALARM, arguments = listOf(idArg)) { AlarmEditor(onClose = close) }
            composable(Routes.ALARMS) {
                AlarmsScreen(
                    onBack = close,
                    onAdd = { nav.navigate(Routes.alarm(0)) },
                    onEdit = { id -> nav.navigate(Routes.alarm(id)) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.REVIEW, arguments = listOf(navArgument("day") { type = NavType.IntType })) { ReviewScreen(onBack = close) }
            composable(Routes.MESSAGES) {
                MessagesScreen(
                    onBack = close,
                    onNew = { nav.navigate(Routes.message(0)) },
                    onEdit = { id -> nav.navigate(Routes.message(id)) },
                )
            }
            composable(Routes.MESSAGE, arguments = listOf(idArg)) { MessageEditor(onClose = close) }
            composable(Routes.TIMER) { TimerScreen(timerVm, onBack = close) }
            composable(Routes.SETTINGS) { SettingsScreen(onBack = close) }
        }

        // Notification taps (e.g. the daily review reminder). Only known, well-formed routes are accepted.
        LaunchedEffect(deepLink) {
            val link = deepLink ?: return@LaunchedEffect
            if (Regex("review/-?\\d+|messages").matches(link)) nav.navigate(link) { launchSingleTop = true }
            onDeepLinkHandled()
        }

        if (onTab) {
            AnimatedVisibility(visible = fabExpanded, enter = fadeIn(tween(160)), exit = fadeOut(tween(120))) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0xD90E0F12))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            fabExpanded = false
                        }
                )
            }

            DayflowBottomBar(
                current = currentTab,
                onSelect = ::goTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )

            timer?.let { t ->
                TimerMiniBar(
                    session = t, bootCount = timerVm.bootCount,
                    onOpen = { fabExpanded = false; nav.navigate(Routes.TIMER) },
                    onPause = timerVm::pause, onResume = timerVm::resume, onDismiss = timerVm::dismiss,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 88.dp, bottom = 12.dp + 68.dp + 10.dp),
                )
            }

            QuickActionFab(
                expanded = fabExpanded,
                onToggle = { fabExpanded = !fabExpanded },
                onPick = { kind -> fabExpanded = false; sheetKind = kind },
                onTimer = { fabExpanded = false; nav.navigate(Routes.TIMER) },
                onAlarm = { fabExpanded = false; nav.navigate(Routes.alarm(0)) },
                onMessage = { fabExpanded = false; nav.navigate(Routes.message(0)) },
                directKind = if (currentTab == Tab.IDEAS) QuickKind.IDEA else null,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 12.dp + 68.dp + 12.dp),
            )
        }
    }

    sheetKind?.let { kind ->
        QuickAddSheet(kind = kind, onDismiss = { sheetKind = null }, onSubmit = { vm.add(it) })
    }
}
