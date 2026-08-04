@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ir.shadbib.app.ui.profile
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale
import ir.shadbib.app.core.Prefs
import ir.shadbib.app.notify.Reminder
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.Switch
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.Jalali
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.core.int
import ir.shadbib.app.data.DailyReport
import ir.shadbib.app.data.HourlyPart
import ir.shadbib.app.data.ProfileData
import ir.shadbib.app.player.PlayerHolder
import ir.shadbib.app.ui.components.AppCard
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.ErrorState
import ir.shadbib.app.ui.components.LoadingBox
import ir.shadbib.app.ui.components.ProgressRow
import ir.shadbib.app.ui.components.SectionTitle
import ir.shadbib.app.ui.components.StatPill
import ir.shadbib.app.ui.theme.brandGradient
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import ir.shadbib.app.ui.components.GlassMenu
import ir.shadbib.app.ui.components.GlassAction
import androidx.compose.material.icons.rounded.Notifications

class ProfileViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val profile: ProfileData = ProfileData(0, 0, emptyList(), emptyList()),
        val streak: Int = 0,
        val followers: Int = 0,
        val following: Int = 0,
        val statusText: String = "",
        val report: DailyReport? = null,
        val hourlyToday: List<HourlyPart> = emptyList(),
        val hourlyYesterday: List<HourlyPart> = emptyList(),
        val weekMinutes: Int = 0,
    )

    val state = MutableStateFlow(State())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(error = null) }
            try {
                val profile = ProfileData.from(Api.obj(Api.get("profile")))
                val streak = runCatching { Api.obj(Api.get("streak")).int("streak") }.getOrDefault(0)
                val social = runCatching { Api.obj(Api.get("social_profile", "username" to (ir.shadbib.app.core.Store.username ?: ""))) }.getOrNull()
                val status = runCatching { Api.obj(Api.get("status")).optString("status_text", "") }.getOrDefault("")
                val week = runCatching { Api.obj(Api.get("analytics_daily")).int("total_week") }.getOrDefault(0)
                state.update {
                    it.copy(loading = false, profile = profile, streak = streak, statusText = status, weekMinutes = week,
                        followers = social?.optInt("followers", 0) ?: 0, following = social?.optInt("following", 0) ?: 0)
                }
            } catch (e: Exception) {
                state.update { it.copy(loading = false, error = e.message ?: "خطا") }
            }
        }
    }

    fun loadReport() {
        viewModelScope.launch {
            runCatching { DailyReport.from(Api.obj(Api.get("daily_report"))) }
                .onSuccess { r -> state.update { it.copy(report = r) } }
        }
    }

    fun loadHourly() {
        viewModelScope.launch {
            runCatching {
                val o = Api.obj(Api.get("analytics_hourly"))
                val today = o.optJSONArray("today_parts")?.let { HourlyPart.list(it) } ?: emptyList()
                val yest = o.optJSONArray("yesterday_parts")?.let { HourlyPart.list(it) } ?: emptyList()
                today to yest
            }.onSuccess { (t, y) -> state.update { it.copy(hourlyToday = t, hourlyYesterday = y) } }
        }
    }

    fun uploadAvatar(ctx: android.content.Context, uri: android.net.Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val bytes = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.readBytes() ?: throw Exception("خطا در خواندن عکس")
                }
                val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
                val res = Api.upload("avatar_upload", emptyMap(), listOf(ir.shadbib.app.core.Api.FilePart("file", "avatar.jpg", bytes, mime)))
                ir.shadbib.app.core.Api.obj(res).optString("avatar", "")
            }.onSuccess { path ->
                ir.shadbib.app.core.Store.setAvatar(path)
                state.update { it.copy(profile = it.profile.copy(avatar = path)) }
                onResult(null)
            }.onFailure { onResult(it.message ?: "خطا در آپلود") }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { Api.post("logout", JSONObject()) }
            PlayerHolder.stopAndClear()
            Store.clearSession()
        }
    }
}

@Composable
fun ProfileScreen(vm: ProfileViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { ir.shadbib.app.core.RefreshBus.events.collect { vm.refresh() } }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(40000); vm.refresh() } }
    val username = Store.username ?: ""
    var showReport by remember { mutableStateOf(false) }
    var showFollowList by remember { mutableStateOf<String?>(null) }
    var showHourly by remember { mutableStateOf(false) }
    var showLogout by remember { mutableStateOf(false) }

    if (state.loading) {
        LoadingBox(height = 300.dp)
        return
    }
    if (state.error != null) {
        ErrorState(state.error ?: "", onRetry = { vm.refresh() })
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AppCard {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                var avatarMenu by remember { mutableStateOf(false) }
                val avPick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    if (uri != null) vm.uploadAvatar(ctx, uri) { err -> android.widget.Toast.makeText(ctx, err ?: "عکس پروفایل به‌روز شد ✅", android.widget.Toast.LENGTH_SHORT).show() }
                }
                val avScope = rememberCoroutineScope()
                if (avatarMenu) {
                    GlassMenu(onDismiss = { avatarMenu = false }) {
                        GlassAction(Icons.Rounded.PhotoCamera, "تغییر عکس پروفایل") { avPick.launch("image/*") }
                        if (state.profile.avatar != null) {
                            GlassAction(Icons.Rounded.Delete, "حذف عکس", danger = true) {
                                avScope.launch {
                                    runCatching { Api.post("avatar_delete", org.json.JSONObject()) }
                                        .onSuccess { ir.shadbib.app.core.Store.setAvatar(null); vm.refresh() }
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // آواتار با رینگ گرادیانی برند (مطابق ماکاپ)
                    Box(
                        Modifier.size(66.dp)
                            .background(brandGradient(), CircleShape)
                            .padding(3.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.clip(CircleShape).clickable { avatarMenu = true }) {
                            Avatar(username, size = 56.dp, avatarUrl = state.profile.avatar)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(username, style = MaterialTheme.typography.titleLarge)
                        if (state.statusText.isNotBlank()) {
                            Text(
                                state.statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row {
                            Row(Modifier.clickable { showFollowList = "following" }, verticalAlignment = Alignment.CenterVertically) {
                                Text(state.following.fa(), style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.width(3.dp))
                                Text("دنبال‌شده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(14.dp))
                            Row(Modifier.clickable { showFollowList = "followers" }, verticalAlignment = Alignment.CenterVertically) {
                                Text(state.followers.fa(), style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.width(3.dp))
                                Text("دنبال‌کننده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.13f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.28f))) {
                        Text("🔥 ${state.streak.fa()}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill(Fmt.minutes(state.profile.totalMinutes), "کل مطالعه", Modifier.weight(1f))
                    StatPill(state.profile.studyDays.fa(), "روز مطالعه", Modifier.weight(1f), accent = MaterialTheme.colorScheme.secondary)
                    StatPill(Fmt.minutes(state.weekMinutes), "این هفته", Modifier.weight(1f), accent = MaterialTheme.colorScheme.tertiary)
                }
            }
        }

        item { SectionTitle("استریک من") }
        item {
            ir.shadbib.app.ui.components.StreakCard(
                streak = state.streak,
                todayMinutes = 0,
                byDate = state.profile.daily.associate { it.date to it.minutes },
            )
        }

        item { SectionTitle("نمودار هفتگی") }
        item {
            AppCard { WeeklyChart(state.profile.daily.associate { it.date to it.minutes }) }
        }

        item { Spacer(Modifier.height(2.dp)) }
        item { StudyAnalyticsSection() }

        if (state.profile.courses.isNotEmpty()) {
            item { SectionTitle("مجموع درس‌ها") }
            item {
                AppCard {
                    val maxM = state.profile.courses.maxOf { it.minutes }.coerceAtLeast(1)
                    state.profile.courses.sortedByDescending { it.minutes }.forEach { c ->
                        ProgressRow(c.icon, c.name, c.minutes, maxM, Fmt.minutes(c.minutes), MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { vm.loadReport(); showReport = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("گزارش امروز")
                }
                OutlinedButton(
                    onClick = { vm.loadHourly(); showHourly = true },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تایم‌لاین")
                }
            }
        }

        item { SettingsCard(vm) }

        item {
            OutlinedButton(
                onClick = { showLogout = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(6.dp))
                Text("خروج از حساب", color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Text(
                "شادبیب · نسخه ۱.۶.۰ · بازطراحی «شب مطالعه» 🌙",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            title = { Text("خروج از حساب") },
            text = { Text("مطمئنی می‌خوای خارج شی؟") },
            confirmButton = {
                TextButton(onClick = { showLogout = false; vm.logout() }) {
                    Text("خروج", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showLogout = false }) { Text("انصراف") } },
        )
    }

    showFollowList?.let { kind ->
        ir.shadbib.app.ui.feed.FollowListSheet(username = username, kind = kind,
            onDismiss = { showFollowList = null }, onOpenUser = { showFollowList = null })
    }

    if (showReport) {
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { showReport = false }) {
            val r = state.report
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 30.dp)) {
                Text("📋 گزارش امروز", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (r == null) {
                    LoadingBox(height = 120.dp)
                } else {
                    Surface(shape = MaterialTheme.shapes.large, color = Color.Transparent) {
                        Column(Modifier.background(brandGradient()).padding(18.dp)) {
                            Text(username, style = MaterialTheme.typography.titleLarge, color = Color.White)
                            Text(Fmt.todayFull(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.85f))
                            Spacer(Modifier.height(12.dp))
                            ReportLine("⏰ بیداری", if (r.wakeupTime != null) Fmt.hm(r.wakeupTime) else "ثبت نشده")
                            ReportLine("📚 مجموع مطالعه", Fmt.minutes(r.totalMinutes))
                            ReportLine("🔥 استریک", "${r.streak.fa()} روز")
                            if (r.firstStudy != null) ReportLine("🌅 اولین پارت", Fmt.hm(r.firstStudy))
                            if (r.lastStudy != null) ReportLine("🌙 آخرین پارت", Fmt.hm(r.lastStudy))
                        }
                    }
                    if (r.sessions.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        Text("پارت‌های امروز", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        r.sessions.forEach { s -> HourlyRow(s) }
                    }
                    Spacer(Modifier.height(16.dp))
                    val ctxR = androidx.compose.ui.platform.LocalContext.current
                    val cs = MaterialTheme.colorScheme
                    val isDark = cs.background.luminance() < 0.5f
                    var saving by remember { mutableStateOf(false) }
                    val scopeR = androidx.compose.runtime.rememberCoroutineScope()
                    val doSave: () -> Unit = {
                        saving = true
                        val prim = cs.primary.toArgb(); val sec = cs.secondary.toArgb()
                        scopeR.launch {
                            val err = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                ReportImage.save(ctxR, username, r, primary = prim, secondary = sec, dark = isDark)
                            }
                            saving = false
                            android.widget.Toast.makeText(ctxR, err ?: "✅ تصویر گزارش در گالری ذخیره شد (Pictures/Shadbib)", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                    val writePerm = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
                        if (ok) doSave() else android.widget.Toast.makeText(ctxR, "برای ذخیره تصویر، مجوز حافظه لازم است", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 29) doSave()
                            else writePerm.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        },
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                    ) {
                        Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (saving) "در حال ذخیره…" else "ذخیره عکس گزارش 📸")
                    }
                }
            }
        }
    }

    if (showHourly) {
        var tab by remember { mutableIntStateOf(0) }
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { showHourly = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 30.dp)) {
                Text("🕐 تایم‌لاین مطالعه", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("امروز") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("دیروز") })
                }
                Spacer(Modifier.height(12.dp))
                val parts = if (tab == 0) state.hourlyToday else state.hourlyYesterday
                if (parts.isEmpty()) {
                    EmptyState("😴", "پارتی ثبت نشده")
                } else {
                    DayStrip(parts)
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    parts.forEach { p -> HourlyRow(p) }
                }
            }
        }
    }
}

@Composable
private fun ReportLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall, color = Color.White)
    }
}

@Composable
private fun HourlyRow(p: HourlyPart) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(p.icon, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(p.course, style = MaterialTheme.typography.bodyLarge)
            Text(Fmt.minutes(p.minutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Text(
            "${Fmt.hm(p.start)} → ${Fmt.hm(p.end)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 24h horizontal strip showing when study parts happened. */
@Composable
private fun DayStrip(parts: List<HourlyPart>) {
    fun toMin(t: String): Int {
        val p = t.split(":")
        return if (p.size >= 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
    }
    val primary = MaterialTheme.colorScheme.primary
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        ) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                parts.forEach { p ->
                    val s = toMin(p.start) / 1440f
                    val e = (toMin(p.end).coerceAtLeast(toMin(p.start) + 8)) / 1440f
                    // RTL: mirror so morning is on the right
                    val x1 = w * (1f - e)
                    drawRoundRect(
                        color = primary,
                        topLeft = androidx.compose.ui.geometry.Offset(x1, 2.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(w * (e - s), size.height - 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("۰۰", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("۱۲", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("۲۴", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Last 7 days bar chart (pure Compose, no chart library). */
@Composable
private fun WeeklyChart(byDate: Map<String, Int>) {
    val days = remember(byDate) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        (0 until 7).map {
            val iso = Fmt.isoOf(cal.time)
            val j = Jalali.fromDate(cal.time)
            val weekday = Jalali.weekDayNames[Jalali.weekDayIndex(cal.time)].take(1)
            val label = "$weekday ${j.day.fa()}"
            cal.add(Calendar.DAY_OF_YEAR, 1)
            Triple(iso, label, byDate[iso] ?: 0)
        }
    }
    val maxV = days.maxOf { it.third }.coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEach { (_, label, v) ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (v > 0) {
                    Text(
                        v.fa(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height((6 + 100f * v / maxV).dp)
                        .background(
                            when {
                                v == maxV && v > 0 -> androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                                v > 0 -> androidx.compose.ui.graphics.SolidColor(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + 0.45f * v / maxV))
                                else -> androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant)
                            },
                            RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                        ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


@Composable
private fun SettingsCard(vm: ProfileViewModel) {
    val ctx = LocalContext.current
    val prefs by Store.prefs.collectAsState()
    var showTime by remember { mutableStateOf(false) }
    var showAutoDl by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.uploadAvatar(ctx, uri) { err ->
            android.widget.Toast.makeText(ctx, err ?: "عکس پروفایل به‌روز شد ✅", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        Store.setReminder(granted, prefs.reminderHour, prefs.reminderMinute)
        if (granted) Reminder.apply(ctx)
    }

    var hideSeen by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { hideSeen = ir.shadbib.app.core.Api.obj(ir.shadbib.app.core.Api.get("privacy")).optInt("hide_last_seen", 0) == 1 } }
    val privScope = rememberCoroutineScope()

    AppCard {
        Text("تنظیمات", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))

        // پست خودکار ساعت مطالعهٔ روزانه
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📚", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("پست خودکار مطالعهٔ روزانه", style = MaterialTheme.typography.bodyLarge)
                Text("هر روزی که مطالعه ثبت کنی، یک پست در صفحه‌ات ساخته و به‌روز می‌شود",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = prefs.autoStudyPost, onCheckedChange = { Store.setAutoStudyPost(it) })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        // حریم خصوصی: آخرین بازدید (تلگرامی — دوطرفه)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VisibilityOff, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("مخفی کردن آخرین بازدید", style = MaterialTheme.typography.bodyLarge)
                Text("اگر مخفی کنی، تو هم بازدید بقیه رو نمی‌بینی", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = hideSeen, onCheckedChange = { v ->
                hideSeen = v
                privScope.launch { runCatching { ir.shadbib.app.core.Api.post("privacy", org.json.JSONObject().put("hide_last_seen", if (v) 1 else 0)) } }
            })
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        // اعلان‌های پوش اجتماع — همه در مرکز اعلان می‌آیند؛ اینجا انتخاب می‌کنی کدام‌ها پوش شوند
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("اعلان‌های پوش", style = MaterialTheme.typography.bodyLarge)
        }
        Text("همه اعلان‌ها توی مرکز اعلان خانه میان؛ فقط انتخاب کن کدوم‌ها نوتیفیکیشن بشن", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("follow" to "دنبال‌شدن", "post_like" to "لایک پست", "post_reply" to "پاسخ پست", "repost" to "بازنشر",
                "mention" to "منشن گروه", "group_reply" to "پاسخ گروه", "reaction" to "ری‌اکشن پیام", "dm" to "پیام خصوصی").forEach { (k, l) ->
                androidx.compose.material3.FilterChip(selected = prefs.pushTypes.contains(k),
                    onClick = { ir.shadbib.app.core.Store.togglePushType(k) }, label = { Text(l, style = MaterialTheme.typography.labelSmall) })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 10.dp))
        Spacer(Modifier.height(4.dp))

        // theme
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Palette, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text("قالب رنگی", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("system" to "سیستم", "light" to "روشن", "dark" to "تیره").forEach { (k, l) ->
                androidx.compose.material3.FilterChip(selected = prefs.themeMode == k, onClick = { Store.setThemeMode(k) }, label = { Text(l) })
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("رنگ برنامه 🎨", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ir.shadbib.app.ui.theme.AppPalettes.forEach { pal ->
                val selected = prefs.themeColor == pal.id
                val scale by androidx.compose.animation.core.animateFloatAsState(if (selected) 1.12f else 1f, androidx.compose.animation.core.spring(dampingRatio = 0.55f), label = "palScale")
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { Store.setThemeColor(pal.id) }) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(52.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(listOf(pal.pDark, pal.sDark)),
                                androidx.compose.foundation.shape.CircleShape)
                            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (selected) Text("✓", color = Color.White, style = MaterialTheme.typography.titleMedium) else Text(pal.emoji) }
                    Spacer(Modifier.height(4.dp))
                    Text(pal.fa, style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // reminder
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("یادآوری مطالعه", style = MaterialTheme.typography.bodyLarge)
                Text(if (prefs.reminderEnabled) "هر روز ساعت ${timeStr(prefs.reminderHour, prefs.reminderMinute)}" else "خاموش",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = prefs.reminderEnabled, onCheckedChange = { on ->
                if (on && Build.VERSION.SDK_INT >= 33) {
                    notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Store.setReminder(on, prefs.reminderHour, prefs.reminderMinute); Reminder.apply(ctx)
                }
            })
        }
        if (prefs.reminderEnabled) {
            TextButton(onClick = { showTime = true }) { Text("تغییر ساعت یادآوری") }
        }
        Spacer(Modifier.height(10.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant); Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("دانلود خودکار مدیا", style = MaterialTheme.typography.bodyLarge)
                Text("عکس ${if (prefs.adImages) "روشن" else "خاموش"} · صدا ${if (prefs.adVoice) "روشن" else "خاموش"} · فایل ${if (prefs.adFiles) "روشن" else "خاموش"} · تا ${prefs.adMaxMb.fa()}MB",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { showAutoDl = true }) { Text("تنظیم") }
        }
    }

    if (showTime) {
        val tp = rememberTimePickerState(initialHour = prefs.reminderHour, initialMinute = prefs.reminderMinute, is24Hour = true)
        AlertDialog(onDismissRequest = { showTime = false }, title = { Text("ساعت یادآوری") },
            text = { TimePicker(state = tp) },
            confirmButton = { TextButton(onClick = { Store.setReminder(true, tp.hour, tp.minute); Reminder.apply(ctx); showTime = false }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("انصراف") } })
    }

    if (showAutoDl) {
        var img by remember { mutableStateOf(prefs.adImages) }
        var voc by remember { mutableStateOf(prefs.adVoice) }
        var fil by remember { mutableStateOf(prefs.adFiles) }
        var maxMb by remember { mutableStateOf(prefs.adMaxMb) }
        AlertDialog(onDismissRequest = { showAutoDl = false }, title = { Text("دانلود خودکار مدیا") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("عکس‌ها", Modifier.weight(1f)); Switch(checked = img, onCheckedChange = { img = it }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("پیام صوتی", Modifier.weight(1f)); Switch(checked = voc, onCheckedChange = { voc = it }) }
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("فایل‌ها", Modifier.weight(1f)); Switch(checked = fil, onCheckedChange = { fil = it }) }
                    Spacer(Modifier.height(8.dp))
                    Text("حداکثر حجم دانلود خودکار", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 5, 10, 20).forEach { mb ->
                            androidx.compose.material3.FilterChip(selected = maxMb == mb, onClick = { maxMb = mb }, label = { Text("${mb.fa()}MB") })
                        }
                    }
                    Text("اگر خاموش باشد، برای دانلود روی مدیا کلیک کن.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = { TextButton(onClick = { Store.setAutoDownload(img, voc, fil, maxMb); showAutoDl = false }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { showAutoDl = false }) { Text("انصراف") } })
    }
}

private fun timeStr(h: Int, m: Int): String = String.format(Locale.US, "%02d:%02d", h, m).fa()
