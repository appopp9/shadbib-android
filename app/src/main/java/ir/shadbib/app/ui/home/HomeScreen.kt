@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ir.shadbib.app.ui.home

import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.Icon3D
import ir.shadbib.app.core.NavBus
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.Course
import ir.shadbib.app.ui.components.AppCard
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.ProgressRow
import ir.shadbib.app.ui.components.SectionTitle
import ir.shadbib.app.ui.theme.brandGradient
import ir.shadbib.app.ui.theme.courseColor
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

    val centerNotifs by ir.shadbib.app.notify.NotifCenter.notifs.collectAsState()
    val unreadNotif = centerNotifs.count { !it.read }

    if (state.loading && state.today.totalMinutes == 0 && state.streak == 0) {
        ir.shadbib.app.ui.components.FullLoading()
        return
    }
    if (state.error != null && !state.loaded) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Spacer(Modifier.height(20.dp))
            Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, onClick = { NavBus.requestStudy() }) {
                Column(Modifier.fillMaxWidth().background(brandGradient()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("فضای مطالعه", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("حتی بدون اینترنت می‌تونی مطالعه‌ت رو ثبت کنی — بعداً همگام می‌شه", color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            ir.shadbib.app.ui.components.ErrorState(state.error ?: "خطا", onRetry = { vm.refresh() })
        }
        return
    }
    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header
        item {
            ir.shadbib.app.ui.components.FadeSlideIn(0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("سلام، $username", style = MaterialTheme.typography.headlineSmall)
                    Text(Fmt.todayFull(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FireStreak(state.streak) { showCelebrate = true }
                Spacer(Modifier.width(4.dp))
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

        // Study space launcher — کارت قهرمان با هالهٔ نور و دکمهٔ قرصی
        item {
            ir.shadbib.app.ui.components.FadeSlideIn(1) {
            Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent, onClick = { NavBus.requestStudy() }) {
                Column(Modifier.background(brandGradient()).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    val tGlow = rememberInfiniteTransition(label = "heroGlow")
                    val gs by tGlow.animateFloat(1f, 1.1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "gs")
                    Text("🌌", fontSize = 40.sp, modifier = Modifier.scale(gs))
                    Spacer(Modifier.height(6.dp))
                    Text("فضای مطالعه", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("کرنومتر · پومودورو · صدای محیط · موسیقی", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(14.dp))
                    Surface(shape = CircleShape, color = Color.White) {
                        Text("شروع مطالعه ▶", color = Color(0xFF06231A), style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp))
                    }
                    Spacer(Modifier.height(9.dp))
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.20f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                        onClick = { NavBus.requestRoom() }) {
                        Row(Modifier.padding(horizontal = 18.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🐬", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text("با بقیه درس بخون", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("امروز: ${Fmt.minutes(state.today.totalMinutes)}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                }
            }
            }
        }

        // میانبرها — هر چه از نوار پایین حذف شد، اینجا در دسترس است
        item {
            ir.shadbib.app.ui.components.FadeSlideIn(2) {
                QuickAccessGrid(
                    onTasks = { NavBus.requestRoute("tasks") },
                    onLibrary = { NavBus.requestRoute("library") },
                    onCommunity = { NavBus.requestRoute("community") },
                    onRoom = { NavBus.requestRoom() },
                    onStats = { NavBus.requestRoute("profile") },
                    onCourses = { showCourses = true },
                )
            }
        }

        // Daily leaderboard
        item { TopStudiersCard(onUser = { u -> NavBus.requestUser(u) }, onSeeAll = { NavBus.requestRoom() }) }

        // Announcement channel


        // Wakeup check-in
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("ساعت بیداری امروز", style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (state.wakeup != null) "${Fmt.hm(state.wakeup)} 🔒 ثبت شد" else "هنوز ثبت نشده",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.wakeup != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.wakeup == null) {
                        Button(onClick = {
                            vm.checkinWakeup { err -> Toast.makeText(ctx, err ?: "صبح بخیر ☀️ ساعت بیداری ثبت شد", Toast.LENGTH_SHORT).show() }
                        }, shape = MaterialTheme.shapes.medium) { Text("بیدار شدم ☀️") }
                    }
                }
            }
        }

        // Today study
        item { SectionTitle("مطالعه امروز", actionText = "مدیریت دروس") { showCourses = true } }
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.minutes(state.today.totalMinutes), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("مجموع مطالعه امروز", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("📚", fontSize = 34.sp)
                }
                if (state.today.courses.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    val maxM = state.today.courses.maxOf { it.minutes }.coerceAtLeast(1)
                    state.today.courses.forEach { c -> ProgressRow(c.icon, c.name, c.minutes, maxM, Fmt.minutes(c.minutes), MaterialTheme.colorScheme.primary) }
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text("امروز هنوز پارتی ثبت نکردی 🌱", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // Mood + status
        item {
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), onClick = { showMood = true }) {
                        Text(state.mood, fontSize = 26.sp, modifier = Modifier.padding(10.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f).clickable { showStatus = true }) {
                        Text("حال و هوای من", style = MaterialTheme.typography.titleSmall)
                        Text(state.statusText.ifBlank { "یه وضعیت بنویس…" }, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showStatus = true }) { Icon(Icons.Rounded.Edit, "ویرایش", modifier = Modifier.size(18.dp)) }
                }
            }
        }

        // Friends strip
        if (state.friends.isNotEmpty()) {
            item { SectionTitle("دنبال‌شده‌های درس‌خون 🫂") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.friends, key = { it.username }) { f ->
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))) {
                            Column(Modifier.width(150.dp).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Avatar(f.username, f.mood, size = 44.dp, online = f.isOnline, avatarUrl = f.avatar)
                                Spacer(Modifier.height(6.dp))
                                Text(f.username, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("📖 ${Fmt.minutes(f.todayMinutes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), onClick = { NavBus.requestDm(f.username) }) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.AutoMirrored.Rounded.Chat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("پیام", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
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
                        onClick = { ir.shadbib.app.notify.NotifCenter.markAllRead(ctx) }, modifier = Modifier.fillMaxWidth()) {
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
                        ir.shadbib.app.ui.components.Avatar(n.actor, size = 36.dp, avatarUrl = n.actorAvatar)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${n.emoji} ${n.title}", style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (!n.read) androidx.compose.ui.text.font.FontWeight.Bold else null)
                            if (!n.text.isNullOrBlank()) Text(n.text, style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
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

/*
 * The only streak surface left on Home.
 *
 * The old full width streak card was decoration that pushed the real actions
 * below the fold, so it is gone. The number still lives here and the
 * celebration is now something you opt into by tapping this chip.
 */
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
/*  میانبرهای صفحهٔ اصلی                                              */
/* ---------------------------------------------------------------------- */

private data class Shortcut(
    val label: String,
    val emoji: String,
    val tint: Color,
    val onClick: () -> Unit,
)

/**
 * Two rows of three tiles, standing in for the tabs that left the bottom bar.
 *
 * A tile is a 46.dp icon plate over a label and the whole cell is tappable, so
 * the target is far bigger than the 23.dp icon it replaces in a seven way bar.
 */
@Composable
private fun QuickAccessGrid(
    onTasks: () -> Unit,
    onLibrary: () -> Unit,
    onCommunity: () -> Unit,
    onRoom: () -> Unit,
    onStats: () -> Unit,
    onCourses: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    // Emoji, not vectors, so the 3D pack can take over each tile automatically.
    val items = listOf(
        Shortcut("تسک‌ها", "✅", cs.primary, onTasks),
        Shortcut("کتابخانه", "🎵", cs.tertiary, onLibrary),
        Shortcut("اجتماع", "👥", cs.secondary, onCommunity),
        Shortcut("با بقیه بخون", "📚", cs.primary, onRoom),
        Shortcut("آمار من", "📊", cs.tertiary, onStats),
        Shortcut("دروس من", "✏", cs.secondary, onCourses),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(3).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { i, sc ->
                    ShortcutTile(sc, delayMs = (rowIndex * 3 + i) * 55, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(sc: Shortcut, delayMs: Int, modifier: Modifier = Modifier) {
    // staggered entrance, so the grid assembles itself instead of popping in
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
            Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(sc.tint.copy(alpha = 0.26f), sc.tint.copy(alpha = 0.10f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon3D(sc.emoji, size = 30.dp, contentDescription = sc.label)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                sc.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/* ---------------------------------------------------------------------- */
/*  جشن استریک                                                            */
/* ---------------------------------------------------------------------- */

/**
 * Opens only when the streak chip is tapped.
 *
 * Three layers run together: a slowly rotating sweep halo, twenty four confetti
 * pieces on their own ballistic paths, and a spring scaled card. Everything is
 * driven by two Animatables and one infinite transition, so once the entrance
 * settles nothing else recomposes and the dialog stays cheap to keep on screen.
 */
@Composable
private fun CelebrationDialog(streak: Int, onDismiss: () -> Unit) {
    val enter = remember { Animatable(0f) }   // card entrance
    val burst = remember { Animatable(0f) }   // confetti travel, 0 = at the flame
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
                        // deterministic pseudo random, so no allocation per frame
                        val rad = ((i * 37.7f) % 360f) * (3.14159265f / 180f)
                        val speed = 130f + (i % 5) * 46f
                        val x = cx + cos(rad) * speed * p
                        val y = cy + sin(rad) * speed * p + 430f * p * p   // gravity
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
