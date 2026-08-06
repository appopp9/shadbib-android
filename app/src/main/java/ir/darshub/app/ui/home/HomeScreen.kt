@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ir.darshub.app.ui.home

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.darshub.app.core.Fmt
import ir.darshub.app.core.Icon3D
import ir.darshub.app.core.NavBus
import ir.darshub.app.core.Store
import ir.darshub.app.core.fa
import ir.darshub.app.data.Course
import ir.darshub.app.data.CourseMinutes
import ir.darshub.app.data.FriendStat
import ir.darshub.app.data.StudyGroup
import ir.darshub.app.ui.components.Avatar
import ir.darshub.app.ui.components.EmptyState
import ir.darshub.app.ui.components.ErrorState
import ir.darshub.app.ui.components.FadeSlideIn
import ir.darshub.app.ui.components.FullLoading
import ir.darshub.app.ui.components.SectionTitle
import ir.darshub.app.ui.theme.brandGradient
import ir.darshub.app.ui.theme.courseColor
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val GENERAL_MOODS = listOf(
    "📚" to "مطالعه", "📖" to "کتابخوانی", "✏️" to "یادداشت", "🧮" to "ریاضی",
    "🔬" to "علوم", "💻" to "برنامه‌نویسی", "🎯" to "تمرکز", "💪" to "پرتلاش",
    "🔥" to "پر انرژی", "☕" to "قهوه‌ای", "🎵" to "موزیک", "😴" to "خوابالو",
    "🌟" to "عالی", "🚀" to "سریع", "💡" to "ایده‌پرداز", "🌈" to "خلاق",
)
private val BOOK_MOODS = listOf(
    "🧬" to "زیست", "🧪" to "شیمی", "⚡" to "فیزیک", "📐" to "ریاضی",
    "🌍" to "زمین‌شناسی", "📊" to "حسابان", "📏" to "هندسه", "🔢" to "گسسته",
)
private val COURSE_ICONS = listOf("📖", "📚", "✏️", "🧮", "🔬", "💻", "🧬", "🧪", "⚡", "📐", "🌍", "📊", "📏", "🔢", "🧠", "🗒️")

/*
 * Home, redesigned around one question: "what does the student need right now?"
 *
 * Priority order on screen:
 *   1. Header   — greeting, mood, streak, notifications. Status lives here now.
 *   2. Hero     — today's minutes once, big and animated, with the start CTA.
 *   3. Wakeup   — a contextual banner that disappears after check-in.
 *   4. Shortcuts— one compact row; only destinations missing from the bottom bar.
 *   5. Friends  — the people the user follows, with today's progress.
 *   6. Groups   — the user's study groups with unread badges (new on Home).
 *   7. Leaders  — daily leaderboard for the competitive push.
 *
 * Gone: the separate "today study" card (minutes were shown three times), the
 * room/library/profile shortcut duplicates, and the always-visible wakeup card.
 */
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    val username = Store.username ?: ""

    var showMood by remember { mutableStateOf(false) }
    var showStatus by remember { mutableStateOf(false) }
    var showCourses by remember { mutableStateOf(false) }
    var showNotifs by remember { mutableStateOf(false) }
    var showCelebrate by remember { mutableStateOf(false) }

    val centerNotifs by ir.darshub.app.notify.NotifCenter.notifs.collectAsState()
    val unreadNotif = centerNotifs.count { !it.read }

    if (state.loading && state.today.totalMinutes == 0 && state.streak == 0) {
        FullLoading()
        return
    }
    if (state.error != null && !state.loaded) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(Modifier.height(20.dp))
            Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, onClick = { NavBus.requestStudy() }) {
                Column(Modifier.fillMaxWidth().background(brandGradient()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("فضای مطالعه", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("حتی بدون اینترنت می‌تونی مطالعه‌ت رو ثبت کنی — بعداً همگام می‌شه", color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
            }
            ErrorState(state.error ?: "خطا", onRetry = { vm.refresh() })
        }
        return
    }

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        /* ۱ ─ هدر فشرده: سلام، مود، استریک، اعلان */
        item {
            FadeSlideIn(0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("سلام، $username", style = MaterialTheme.typography.headlineSmall)
                        Text(Fmt.todayFull(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.statusText.isNotBlank()) {
                            Text(state.statusText, style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { showStatus = true })
                        } else {
                            Text("+ یه وضعیت بنویس…", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                modifier = Modifier.clickable { showStatus = true })
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f), onClick = { showMood = true }) {
                        Text(state.mood, fontSize = 17.sp, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    FireStreak(state.streak) { showCelebrate = true }
                    Spacer(Modifier.width(2.dp))
                    Box {
                        IconButton(onClick = { showNotifs = true }) {
                            Icon(Icons.Rounded.Notifications, contentDescription = "اعلان‌ها")
                        }
                        if (unreadNotif > 0) {
                            Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(10.dp)
                                .background(Color(0xFFEF4444), CircleShape))
                        }
                    }
                }
            }
        }

        /* ۲ ─ کارت قهرمان: امروز + شروع مطالعه */
        item {
            FadeSlideIn(1) { HeroTodayCard(state.today.totalMinutes, state.today.courses) }
        }

        /* ۳ ─ بنر بیداری: فقط تا وقتی ثبت نشده */
        if (state.wakeup == null) {
            item {
                FadeSlideIn(2) {
                    WakeupBanner(onCheckin = {
                        vm.checkinWakeup { err -> Toast.makeText(ctx, err ?: "صبح بخیر ☀️ ساعت بیداری ثبت شد", Toast.LENGTH_SHORT).show() }
                    })
                }
            }
        }

        /* ۴ ─ میانبرها: یک ردیف چهارتایی، بدون تکرار تب‌های پایین */
        item {
            FadeSlideIn(3) {
                QuickRow(
                    onTasks = { NavBus.requestRoute("tasks") },
                    onLibrary = { NavBus.requestRoute("library") },
                    onCommunity = { NavBus.requestRoute("community") },
                    onCourses = { showCourses = true },
                )
            }
        }

        /* ۵ ─ دوستان دنبال‌شده */
        item {
            FadeSlideIn(4) { FriendsSection(state.friends) }
        }

        /* ۶ ─ گروه‌های مطالعه‌ی کاربر */
        item {
            FadeSlideIn(5) { GroupsSection(state.groups, state.groupUnread) }
        }

        /* ۷ ─ برترین‌های امروز */
        item {
            FadeSlideIn(6) {
                TopStudiersCard(
                    myMinutes = state.today.totalMinutes,
                    onUser = { u -> NavBus.requestUser(u) },
                    onSeeAll = { NavBus.requestRoom("top") },
                )
            }
        }

        item { Spacer(Modifier.height(4.dp)) }
    }

    // ---- dialogs / sheets ----
    if (showCelebrate) CelebrationDialog(state.streak) { showCelebrate = false }

    if (showNotifs) {
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { showNotifs = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("🔔 اعلان‌ها", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                if (centerNotifs.any { !it.read }) {
                    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        onClick = { ir.darshub.app.notify.NotifCenter.markAllRead(ctx) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.DoneAll, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("علامت‌گذاری همه به‌عنوان خوانده‌شده", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (centerNotifs.isEmpty()) EmptyState("🔕", "فعلاً اعلانی نداری")
                else centerNotifs.take(30).forEach { n ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(n.actor, size = 36.dp, avatarUrl = n.actorAvatar)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${n.emoji} ${n.title}", style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!n.read) FontWeight.Bold else null)
                            if (!n.text.isNullOrBlank()) Text(n.text, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(Fmt.relative(n.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showMood) {
        var tab by remember { mutableStateOf(0) }
        var selected by remember { mutableStateOf(state.mood) }
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { showMood = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text("چه حسی داری؟", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("😊 عمومی") })
                    FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("📚 کتاب‌ها") })
                }
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if (tab == 0) GENERAL_MOODS else BOOK_MOODS).forEach { (e, l) ->
                        Surface(shape = MaterialTheme.shapes.medium,
                            color = if (selected == e) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            onClick = { selected = e }) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(e, fontSize = 22.sp); Text(l, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { vm.setMood(selected); showMood = false }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = MaterialTheme.shapes.medium) { Text("ذخیره") }
            }
        }
    }

    if (showStatus) {
        var text by remember { mutableStateOf(state.statusText) }
        AlertDialog(onDismissRequest = { showStatus = false }, title = { Text("متن وضعیت") },
            text = {
                Column {
                    OutlinedTextField(value = text, onValueChange = { if (it.length <= 50) text = it }, placeholder = { Text("مثلاً: امروز فقط زیست 🧬") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                    Text("${text.length.fa()}/۵۰", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { vm.setStatus(text) { err -> if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show() }; showStatus = false }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showStatus = false }) { Text("انصراف") } })
    }

    if (showCourses) CourseManagerSheet(vm, state.courses) { showCourses = false }
}

/* ---------------------------------------------------------------------- */
/*  قهرمان صفحه: دقایق امروز + شروع سریع                                */
/* ---------------------------------------------------------------------- */

/*
 * The one place today's total lives.
 *
 * Before the redesign the same number appeared in the hero badge, in a dedicated
 * "today study" card and again inside the leaderboard. Everything merged here:
 * a big animated counter, up to three per-course mini bars for context, and the
 * two actions that actually start a session. The room is a secondary tonal pill
 * because it already has its own bottom tab.
 */
@Composable
private fun HeroTodayCard(totalMinutes: Int, courses: List<CourseMinutes>) {
    val counted by animateFloatAsState(totalMinutes.toFloat(), tween(900), label = "heroCount")
    Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, onClick = { NavBus.requestStudy() }) {
        Column(Modifier.background(brandGradient()).padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("مطالعه‌ی امروز", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(counted.toInt().fa(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 40.sp, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Text("دقیقه", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 7.dp))
                    }
                }
                val t = rememberInfiniteTransition(label = "heroPulse")
                val pulse by t.animateFloat(1f, 1.09f,
                    infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
                Box(
                    Modifier.size(62.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .background(Color.White.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("▶", color = Color.White, fontSize = 21.sp)
                }
            }

            if (courses.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val maxM = courses.maxOf { it.minutes }.coerceAtLeast(1)
                courses.sortedByDescending { it.minutes }.take(3).forEach { c ->
                    val frac by animateFloatAsState(c.minutes.toFloat() / maxM, tween(700), label = "heroBar")
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.icon, fontSize = 13.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(c.name, color = Color.White.copy(alpha = 0.92f), style = MaterialTheme.typography.labelMedium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(88.dp))
                        Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f))) {
                            Box(Modifier.fillMaxWidth(frac).height(5.dp).clip(CircleShape).background(Color.White))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(Fmt.minutes(c.minutes), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White, onClick = { NavBus.requestStudy() }, modifier = Modifier.weight(1f)) {
                    Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("شروع مطالعه ▶", color = Color(0xFF06231A), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.40f)), onClick = { NavBus.requestRoom() }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🐬", fontSize = 14.sp)
                        Spacer(Modifier.width(5.dp))
                        Text("اتاق", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  بنر شرطی ساعت بیداری                                                  */
/* ---------------------------------------------------------------------- */

/* Shown only until the user checks in; after that it leaves no trace. */
@Composable
private fun WakeupBanner(onCheckin: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("☀️", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Text("ساعت بیداری امروزت رو ثبت کن", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary, onClick = onCheckin) {
                Text("بیدار شدم", color = MaterialTheme.colorScheme.onTertiary, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp))
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  میانبرها — یک ردیف فشرده                                             */
/* ---------------------------------------------------------------------- */

private data class Shortcut(
    val label: String,
    val emoji: String,
    val tint: Color,
    val onClick: () -> Unit,
)

/*
 * Four tiles, one row. Room and profile are deliberately absent — they already
 * sit one tap away in the bottom bar, so repeating them here was pure noise.
 */
@Composable
private fun QuickRow(onTasks: () -> Unit, onLibrary: () -> Unit, onCommunity: () -> Unit, onCourses: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val items = listOf(
        Shortcut("تسک‌ها", "✅", cs.primary, onTasks),
        Shortcut("کتابخانه", "🎵", cs.tertiary, onLibrary),
        Shortcut("اجتماع", "👥", cs.secondary, onCommunity),
        Shortcut("دروس", "✏️", cs.primary, onCourses),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items.forEachIndexed { i, sc -> ShortcutTile(sc, delayMs = i * 55, modifier = Modifier.weight(1f)) }
    }
}

@Composable
private fun ShortcutTile(sc: Shortcut, delayMs: Int, modifier: Modifier = Modifier) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        appear.animateTo(1f, tween(420, easing = EaseOutBack))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.93f else 1f,
        spring(dampingRatio = 0.4f, stiffness = 1000f), label = "tilePress",
    )
    val haptic = LocalHapticFeedback.current
    Surface(
        modifier = modifier
            .graphicsLayer {
                val a = appear.value
                scaleX = a * press
                scaleY = a * press
                alpha = a.coerceIn(0f, 1f)
            }
            .clip(MaterialTheme.shapes.large)
            .clickable(interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                sc.onClick()
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            Modifier.padding(vertical = 11.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(sc.tint.copy(alpha = 0.26f), sc.tint.copy(alpha = 0.10f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon3D(sc.emoji, size = 26.dp, contentDescription = sc.label)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                sc.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  دوستان دنبال‌شده                                                      */
/* ---------------------------------------------------------------------- */

/*
 * Compact friend cards: avatar with online ring and mood, today's minutes and a
 * slim progress bar relative to the most productive friend — social pressure at
 * a glance. Card tap opens the profile sheet, the pill opens a DM.
 */
@Composable
private fun FriendsSection(friends: List<FriendStat>) {
    Column {
        SectionTitle("رفیق‌های درس‌خون 🫂", actionText = "همه ›") { NavBus.requestRoute("community") }
        Spacer(Modifier.height(10.dp))
        if (friends.isEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                onClick = { NavBus.requestRoute("community") }, modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🔍", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("هنوز کسی رو دنبال نکردی", style = MaterialTheme.typography.titleSmall)
                        Text("از اجتماع، رفیق‌های درس‌خونت رو پیدا کن", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("‹", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            val maxM = friends.maxOf { it.todayMinutes }.coerceAtLeast(1)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(friends, key = { it.username }) { f -> FriendCard(f, maxM) }
            }
        }
    }
}

@Composable
private fun FriendCard(f: FriendStat, maxMinutes: Int) {
    val frac by animateFloatAsState((f.todayMinutes.toFloat() / maxMinutes).coerceIn(0f, 1f), tween(700), label = "frBar")
    Surface(
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        onClick = { NavBus.requestUser(f.username) },
    ) {
        Column(Modifier.width(128.dp).padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Avatar(f.username, f.mood, size = 46.dp, online = f.isOnline, avatarUrl = f.avatar)
            Spacer(Modifier.height(7.dp))
            Text(f.username, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Text(Fmt.minutes(f.todayMinutes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(7.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))) {
                Box(Modifier.fillMaxWidth(frac).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            }
            Spacer(Modifier.height(9.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), onClick = { NavBus.requestDm(f.username) }) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, "پیام", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("پیام", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  گروه‌های مطالعه — جدید در صفحه‌ی اصلی                                */
/* ---------------------------------------------------------------------- */

/*
 * The user's study groups were only reachable through the messages tab. They
 * are a core motivator, so they now live on Home with unread badges; one tap
 * jumps straight into the group chat via NavBus. A trailing tile offers create/
 * discover, and the empty state explains why groups matter.
 */
@Composable
private fun GroupsSection(groups: List<StudyGroup>, unread: Map<Int, Int>) {
    Column {
        SectionTitle("گروه‌های مطالعه 👥", actionText = "همه ›") { NavBus.requestGroupsHome() }
        Spacer(Modifier.height(10.dp))
        if (groups.isEmpty()) {
            Surface(
                shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                onClick = { NavBus.requestGroupsHome() }, modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🌱", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("هنوز عضو گروهی نیستی", style = MaterialTheme.typography.titleSmall)
                        Text("با بقیه بخون — انگیزه چند برابر می‌شه", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("‹", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(groups, key = { it.id }) { g -> GroupCard(g, unread[g.id] ?: 0) }
                item(key = "new_group_tile") { NewGroupTile() }
            }
        }
    }
}

@Composable
private fun GroupCard(g: StudyGroup, unread: Int) {
    Surface(
        shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        onClick = { NavBus.requestGroup(g.id, g.name) },
    ) {
        Column(Modifier.width(142.dp).padding(12.dp)) {
            Box {
                Avatar(g.username, size = 40.dp, avatarUrl = g.avatar)
                if (unread > 0) {
                    Surface(
                        shape = CircleShape, color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.TopStart),
                    ) {
                        Text(unread.fa(), color = MaterialTheme.colorScheme.onError, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(g.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(g.memberCount.fa() + " عضو", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NewGroupTile() {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        onClick = { NavBus.requestGroupsHome() },
    ) {
        Column(
            Modifier.width(102.dp).height(96.dp).padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("➕", fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text("گروه جدید", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  استریک                                                                  */
/* ---------------------------------------------------------------------- */

/* The only streak surface on Home; the celebration is opt-in via tap. */
@Composable
private fun FireStreak(streak: Int, onClick: () -> Unit) {
    val alive = streak > 0
    val t = rememberInfiniteTransition(label = "streakChip")
    val flicker by t.animateFloat(
        1f, if (alive) 1.16f else 1f,
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flicker",
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.42f, stiffness = 900f), label = "streakPress",
    )
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        modifier = Modifier
            .graphicsLayer { scaleX = press; scaleY = press }
            .clip(CircleShape)
            .clickable(interaction, indication = null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, "استریک",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(17.dp).graphicsLayer { scaleX = flicker; scaleY = flicker })
            Spacer(Modifier.width(5.dp))
            Text(streak.fa(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(3.dp))
            Text("روز", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  مدیریت دروس                                                            */
/* ---------------------------------------------------------------------- */

@Composable
private fun CourseManagerSheet(vm: HomeViewModel, courses: List<Course>, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("green") }
    var icon by remember { mutableStateOf("📖") }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("📚 مدیریت دروس", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام درس") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("purple" to "بنفش", "red" to "قرمز", "green" to "سبز", "orange" to "نارنجی").forEach { (k, l) ->
                    FilterChip(selected = color == k, onClick = { color = k }, label = { Text(l) },
                        leadingIcon = { Box(Modifier.size(12.dp).background(courseColor(k), CircleShape)) })
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(COURSE_ICONS) { e ->
                    Surface(shape = CircleShape, color = if (icon == e) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = { icon = e }) {
                        Text(e, fontSize = 20.sp, modifier = Modifier.padding(9.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { vm.addCourse(name.trim(), color, icon) { err -> Toast.makeText(ctx, err ?: "درس اضافه شد ✅", Toast.LENGTH_SHORT).show() }; name = "" },
                enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().height(48.dp), shape = MaterialTheme.shapes.medium) {
                Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(6.dp)); Text("افزودن درس")
            }
            Spacer(Modifier.height(14.dp))
            courses.forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(courseColor(c.color), CircleShape))
                    Spacer(Modifier.width(10.dp)); Text(c.icon, fontSize = 18.sp); Spacer(Modifier.width(8.dp))
                    Text(c.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { vm.deleteCourse(c.id) }) { Icon(Icons.Rounded.Delete, "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                }
            }
            if (courses.isEmpty()) EmptyState("🌱", "اولین درست رو بساز")
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  جشن استریک                                                            */
/* ---------------------------------------------------------------------- */

/*
 * Opens only when the streak chip is tapped.
 *
 * Three layers run together: a slowly rotating sweep halo, twenty four confetti
 * pieces on their own ballistic paths, and a spring scaled card. Everything is
 * driven by two Animatables and one infinite transition, so once the entrance
 * settles nothing else recomposes and the dialog stays cheap to keep on screen.
 */
@Composable
private fun CelebrationDialog(streak: Int, onDismiss: () -> Unit) {
    val enter = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { enter.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 260f)) }
        burst.animateTo(1f, tween(2200, easing = LinearEasing))
    }

    val t = rememberInfiniteTransition(label = "cel")
    val spin by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin",
    )
    val pulse by t.animateFloat(
        1f, 1.2f,
        infiniteRepeatable(tween(760, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse",
    )

    val msg = when {
        streak <= 0 -> "امروز شروع کن تا استریکت بالا بره! 💪"
        streak < 3 -> "شروع خوبیه! ادامه بده تا عادت بشه 🌱"
        streak < 7 -> "داری قوی می‌شی! نذار قطع شه 🔥"
        streak < 30 -> "عالیه! تو یه قهرمان درس‌خونی ⭐"
        else -> "افسانه‌ای! این استریک فوق‌العادهست 🏆"
    }

    val confetti = listOf(
        Color(0xFF4ADE9F), Color(0xFF38BDF8), Color(0xFFFBBF24),
        Color(0xFFFB7185), Color(0xFFA78BFA), Color(0xFFFB923C),
    )

    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.Center) {

            Canvas(Modifier.fillMaxWidth().height(430.dp)) {
                val p = burst.value
                if (p < 1f) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val fade = (1f - p).coerceIn(0f, 1f)
                    repeat(24) { i ->
                        val rad = ((i * 37.7f) % 360f) * (3.14159265f / 180f)
                        val speed = 130f + (i % 5) * 46f
                        val x = cx + cos(rad) * speed * p
                        val y = cy + sin(rad) * speed * p + 430f * p * p
                        drawCircle(
                            color = confetti[i % confetti.size].copy(alpha = fade),
                            radius = 5f + (i % 3) * 2.5f,
                            center = Offset(x, y),
                        )
                    }
                }
            }

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent,
                modifier = Modifier.graphicsLayer {
                    val e = enter.value
                    scaleX = e
                    scaleY = e
                    alpha = e.coerceIn(0f, 1f)
                },
            ) {
                Column(
                    Modifier.background(brandGradient()).padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .size(150.dp)
                                .graphicsLayer { rotationZ = spin }
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.30f),
                                            Color.White.copy(alpha = 0f),
                                            Color.White.copy(alpha = 0.22f),
                                            Color.White.copy(alpha = 0f),
                                        )
                                    ),
                                    CircleShape,
                                )
                        )
                        Icon3D("🔥", size = 92.dp, modifier = Modifier.scale(pulse))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        streak.fa() + " روز پیاپی!",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        msg,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(shape = CircleShape, color = Color.White, onClick = onDismiss) {
                        Text(
                            "بریم ادامه بدیم 🚀",
                            color = Color(0xFF06231A),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
