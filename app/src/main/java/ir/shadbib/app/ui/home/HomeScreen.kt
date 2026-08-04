@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ir.shadbib.app.ui.home
import androidx.compose.foundation.layout.fillMaxSize

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.shadbib.app.core.Fmt
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
                    Spacer(Modifier.height(10.dp))
                    Text("امروز: ${Fmt.minutes(state.today.totalMinutes)}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelLarge)
                }
            }
            }
        }

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

@Composable
private fun FireStreak(streak: Int, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocalFireDepartment, "استریک",
                tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(streak.fa(), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
            Spacer(Modifier.width(3.dp))
            Text("روز", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun CelebrationDialog(streak: Int, onDismiss: () -> Unit) {
    val t = rememberInfiniteTransition(label = "cel")
    val s by t.animateFloat(1f, 1.3f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "s")
    val msg = when {
        streak <= 0 -> "امروز شروع کن تا استریکت بالا بره! 💪"
        streak < 3 -> "شروع خوبیه! ادامه بده تا عادت بشه 🌱"
        streak < 7 -> "داری قوی می‌شی! نذار قطع شه 🔥"
        streak < 30 -> "عالیه! تو یه قهرمان درس‌خونی ⭐"
        else -> "افسانه‌ای! این استریک فوق‌العادست 🏆"
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = Color.Transparent) {
            Column(Modifier.background(brandGradient()).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", fontSize = 80.sp, modifier = Modifier.scale(s))
                Spacer(Modifier.height(8.dp))
                Text("${streak.fa()} روز پیاپی!", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(6.dp))
                Text(msg, color = Color.White, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(6.dp))
                Text("✨ 🎉 ⭐ 🌟 🎊 ✨", fontSize = 22.sp)
                Spacer(Modifier.height(16.dp))
                Surface(shape = CircleShape, color = Color.White, onClick = onDismiss) {
                    Text("بریم ادامه بدیم 🚀", color = Color(0xFF06231A), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
                }
            }
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
