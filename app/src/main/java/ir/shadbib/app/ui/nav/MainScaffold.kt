package ir.shadbib.app.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.NavBus
import ir.shadbib.app.core.fa
import ir.shadbib.app.player.Chrono
import ir.shadbib.app.player.Pomodoro
import ir.shadbib.app.ui.community.CommunityScreen
import ir.shadbib.app.ui.home.HomeScreen
import ir.shadbib.app.ui.library.LibraryScreen
import ir.shadbib.app.ui.library.MiniPlayer
import ir.shadbib.app.ui.messages.MessagesScreen
import ir.shadbib.app.ui.profile.ProfileScreen
import ir.shadbib.app.ui.study.StudySpaceDialog
import ir.shadbib.app.ui.study.room.StudyRoomScreen
import ir.shadbib.app.ui.tasks.TasksScreen
import ir.shadbib.app.ui.theme.brandGradient
import kotlinx.coroutines.delay
import java.util.Locale

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/*
 * Only four tabs now.
 *
 * Seven pills in one bar left every target under the 48.dp touch minimum and
 * the labels never had room to appear. Tasks, community and library are
 * one tap away from the Home shortcut grid instead, where they get a real
 * label and a comfortable hit area.
 */
private val tabs = listOf(
    Tab("home", "خانه", Icons.Rounded.Home),
    Tab("room", "اتاق", Icons.Rounded.MenuBook),
    Tab("messages", "پیام‌ها", Icons.AutoMirrored.Rounded.Chat),
    Tab("profile", "پروفایل", Icons.Rounded.Person),
)

@Composable
fun MainScaffold() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    var unread by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            runCatching { Api.obj(Api.get("dm_unread")).optInt("count", 0) }.onSuccess { unread = it }
            delay(10000)
        }
    }
    LaunchedEffect(Unit) {
        ir.shadbib.app.core.RefreshBus.events.collect {
            if (it == "dm" || it == "all") runCatching { Api.obj(Api.get("dm_unread")).optInt("count", 0) }.onSuccess { c -> unread = c }
        }
    }

    val openDm by NavBus.openDm.collectAsState()
    val openCh by NavBus.openChannel.collectAsState()
    LaunchedEffect(openDm, openCh) {
        if ((openDm != null || openCh != null) && currentRoute != "messages") {
            nav.navigate("messages") {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true; restoreState = true
            }
        }
    }

    val openUserReq by NavBus.openUser.collectAsState()
    LaunchedEffect(openUserReq) {
        if (openUserReq != null && currentRoute != "community") {
            nav.navigate("community") {
                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true; restoreState = true
            }
        }
    }

    val openRoomReq by NavBus.openRoom.collectAsState()
    LaunchedEffect(openRoomReq) {
        if (openRoomReq) {
            NavBus.consumeRoom()
            if (currentRoute != "room") {
                nav.navigate("room") {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }
        }
    }

    // shortcut tiles on Home ask for a route that no longer lives in the bar
    val routeReq by NavBus.openRoute.collectAsState()
    LaunchedEffect(routeReq) {
        val r = routeReq
        if (r != null) {
            NavBus.consumeRoute()
            if (currentRoute != r) {
                nav.navigate(r) {
                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true; restoreState = true
                }
            }
        }
    }

    val openStudy by NavBus.openStudy.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                MiniPlayer()
                FloatingNavBar(currentRoute, unread) { route ->
                    nav.navigate(route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true; restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            NavHost(
                navController = nav, startDestination = "home",
                enterTransition = {
                    fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 24 }
                },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(240)) },
                popExitTransition = { fadeOut(tween(150)) },
            ) {
                composable("home") { HomeScreen() }
                composable("messages") { MessagesScreen() }
                composable("tasks") { TasksScreen() }
                composable("room") { StudyRoomScreen() }
                composable("community") { CommunityScreen() }
                composable("library") { LibraryScreen() }
                composable("profile") { ProfileScreen() }
            }
            // در اتاق مطالعه نوار شناور پنهان می‌شود — آن صفحه تایمر خودش را دارد
            if (currentRoute != "room") {
                StudyFloatingBar(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)) { NavBus.requestStudy() }
            }
        }
    }

    if (openStudy) StudySpaceDialog(onClose = { NavBus.consumeStudy() })
}

/** نوار ناوبری قرصی شناور — امضای بازطراحی «شب مطالعه». */
@Composable
private fun FloatingNavBar(currentRoute: String?, unread: Int, onSelect: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .height(66.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 16.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                NavPill(
                    tab = tab,
                    selected = currentRoute == tab.route,
                    badge = if (tab.route == "messages") unread else 0,
                    modifier = Modifier.weight(if (currentRoute == tab.route) 1.45f else 1f),
                ) { onSelect(tab.route) }
            }
        }
    }
}

@Composable
private fun NavPill(tab: Tab, selected: Boolean, badge: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        tween(280), label = "navBg")
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(280), label = "navFg")
    val scale by animateFloatAsState(
        if (selected) 1.08f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 480f), label = "navScale")
    Box(
        modifier
            .padding(horizontal = 3.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(remember { MutableInteractionSource() }, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }) {
                Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(23.dp))
            }
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(tween(260)) + fadeIn(tween(300)),
                exit = shrinkHorizontally(tween(200)) + fadeOut(tween(140)),
            ) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
                }
            }
            // بج نخوانده — کنار آیتم، بدون روی‌هم‌افتادگی با متن
            if (badge > 0) {
                Spacer(Modifier.width(4.dp))
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error) {
                    Text(if (badge > 99) "+۹۹" else badge.fa(), color = MaterialTheme.colorScheme.onError,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun StudyFloatingBar(modifier: Modifier = Modifier, onClick: () -> Unit) {
    var label by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            val pomo = Pomodoro.state.value
            label = when {
                pomo.running -> "پومودورو  " + clockMs(pomo.remainingSec)
                Chrono.running -> "کرنومتر  " + clockH((Chrono.elapsedMs() / 1000))
                else -> null
            }
            delay(1000)
        }
    }
    val haptic = LocalHapticFeedback.current
    AnimatedVisibility(visible = label != null, modifier = modifier) {
        Surface(shape = CircleShape, color = Color.Transparent, shadowElevation = 10.dp) {
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(brandGradient())
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
                    }
                    .padding(horizontal = 18.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.onPrimary, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("📚 " + (label ?: ""), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

private fun clockMs(sec: Int): String = String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60).fa()
private fun clockH(sec: Long): String = String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60).fa()
