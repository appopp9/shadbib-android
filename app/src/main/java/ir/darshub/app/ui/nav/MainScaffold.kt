package ir.darshub.app.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ir.darshub.app.core.Api
import ir.darshub.app.core.NavBus
import ir.darshub.app.core.fa
import ir.darshub.app.player.Chrono
import ir.darshub.app.player.Pomodoro
import ir.darshub.app.ui.community.CommunityScreen
import ir.darshub.app.ui.home.HomeScreen
import ir.darshub.app.ui.library.LibraryScreen
import ir.darshub.app.ui.library.MiniPlayer
import ir.darshub.app.ui.messages.MessagesScreen
import ir.darshub.app.ui.profile.ProfileScreen
import ir.darshub.app.ui.study.StudySpaceDialog
import ir.darshub.app.ui.study.room.StudyRoomScreen
import ir.darshub.app.ui.tasks.TasksScreen
import ir.darshub.app.ui.theme.DarsMotion
import ir.darshub.app.ui.theme.brandGradient
import ir.darshub.app.ui.theme.pressScale
import kotlinx.coroutines.delay
import java.util.Locale

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/*
 * چهار تب اصلی در نوار شناور ۲۰۲۶. تسک‌ها، انجمن و کتابخانه از گرید
 * میان‌برهای خانه در دسترس‌اند (یک لمس، با برچسب و سطح لمس راحت).
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
        ir.darshub.app.core.RefreshBus.events.collect {
            if (it == "dm" || it == "all") runCatching { Api.obj(Api.get("dm_unread")).optInt("count", 0) }.onSuccess { c -> unread = c }
        }
    }

    val openDm by NavBus.openDm.collectAsState()
    val openCh by NavBus.openChannel.collectAsState()
    val openGrp by NavBus.openGroup.collectAsState()
    val openGrpHome by NavBus.openGroupsHome.collectAsState()
    LaunchedEffect(openDm, openCh, openGrp, openGrpHome) {
        if ((openDm != null || openCh != null || openGrp != null || openGrpHome) && currentRoute != "messages") {
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
                    fadeIn(tween(DarsMotion.Medium, easing = DarsMotion.Emphasized)) +
                            slideInVertically(tween(DarsMotion.Medium, easing = DarsMotion.Emphasized)) { it / 26 } +
                            scaleIn(tween(DarsMotion.Medium, easing = DarsMotion.Emphasized), initialScale = 0.985f)
                },
                exitTransition = { fadeOut(tween(DarsMotion.Fast)) },
                popEnterTransition = { fadeIn(tween(DarsMotion.Medium, easing = DarsMotion.Emphasized)) },
                popExitTransition = { fadeOut(tween(DarsMotion.Fast)) },
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

/** نوار ناوبری قرصی شناور ۲۰۲۶ — شیشه با نشانگر گرادیانی لغزان فنری. */
@Composable
private fun FloatingNavBar(currentRoute: String?, unread: Int, onSelect: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .height(68.dp)
            .shadow(
                20.dp, CircleShape,
                ambientColor = cs.primary.copy(alpha = 0.18f),
                spotColor = cs.primary.copy(alpha = 0.24f),
            ),
        shape = CircleShape,
        color = cs.surfaceContainer.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, BrushBorder()),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemW = maxWidth / tabs.size
            val selectedIdx = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
            val sign = if (rtl) -1f else 1f
            val pillX by androidx.compose.animation.core.animateDpAsState(
                itemW * selectedIdx * sign, DarsMotion.springBouncy(), label = "navPill")
            Box(
                Modifier
                    .padding(5.dp)
                    .offset(x = pillX)
                    .width(itemW - 10.dp)
                    .height(58.dp)
                    .clip(CircleShape)
                    .background(brandGradient())
                    .shadow(10.dp, CircleShape,
                        ambientColor = cs.primary.copy(alpha = 0.35f),
                        spotColor = cs.primary.copy(alpha = 0.4f)),
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
                    NavPill(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        badge = if (tab.route == "messages") unread else 0,
                        modifier = Modifier.weight(1f),
                    ) { onSelect(tab.route) }
                }
            }
        }
    }
}

private fun BrushBorder() = androidx.compose.ui.graphics.Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.4f), Color.White.copy(alpha = 0.08f)))

@Composable
private fun NavPill(tab: Tab, selected: Boolean, badge: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        tween(DarsMotion.Fast), label = "navFg")
    val iconScale by animateFloatAsState(
        if (selected) 1.12f else 1f,
        DarsMotion.springBouncy(), label = "navIconScale")
    Box(
        modifier
            .padding(horizontal = 3.dp, vertical = 5.dp)
            .clip(CircleShape)
            .pressScale(interaction, pressedScale = 0.92f)
            .clickable(interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.graphicsLayer { scaleX = iconScale; scaleY = iconScale }) {
                Icon(tab.icon, contentDescription = tab.label, tint = fg, modifier = Modifier.size(24.dp))
            }
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(tween(DarsMotion.Base, easing = DarsMotion.Emphasized)) + fadeIn(tween(DarsMotion.Base)),
                exit = shrinkHorizontally(tween(DarsMotion.Fast)) + fadeOut(tween(DarsMotion.Fast)),
            ) {
                Row {
                    Spacer(Modifier.width(6.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1)
                }
            }
            // بج نخوانده — با ورود فنری، بدون روی‌هم‌افتادگی با متن
            if (badge > 0) {
                Spacer(Modifier.width(4.dp))
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(DarsMotion.springBouncy(), initialScale = 0.4f) + fadeIn(tween(DarsMotion.Fast)),
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error,
                        shadowElevation = 4.dp) {
                        Text(if (badge > 99) "+۹۹" else badge.fa(), color = MaterialTheme.colorScheme.onError,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
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
    AnimatedVisibility(
        visible = label != null,
        modifier = modifier,
        enter = scaleIn(DarsMotion.springBouncy(), initialScale = 0.6f) + fadeIn(tween(DarsMotion.Fast)),
        exit = fadeOut(tween(DarsMotion.Fast)),
    ) {
        Surface(
            shape = CircleShape, color = Color.Transparent,
            modifier = Modifier.shadow(
                14.dp, CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            ),
        ) {
            val interaction = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(brandGradient())
                    .pressScale(interaction, pressedScale = 0.94f)
                    .clickable(interaction, indication = null) {
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
