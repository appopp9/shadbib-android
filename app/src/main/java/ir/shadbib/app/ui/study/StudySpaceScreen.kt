@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ir.shadbib.app.ui.study

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.MusicTrack
import ir.shadbib.app.data.Playlist
import ir.shadbib.app.player.AmbientMixer
import ir.shadbib.app.player.Chrono
import ir.shadbib.app.player.PlayerHolder
import ir.shadbib.app.player.Pomodoro
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/* رنگ‌های فضای تمرکز — هماهنگ با پالت «شب مطالعه» */
private val FocusBg = Brush.verticalGradient(listOf(Color(0xFF0B100E), Color(0xFF12241C), Color(0xFF0A121A)))
private val FocusMint = Color(0xFF4ADE9F)
private val FocusCyan = Color(0xFF38BDF8)
private val FocusInk = Color(0xFF06110C)
private val FocusGrad = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF38BDF8)))

@Composable
fun StudySpaceDialog(onClose: () -> Unit, vm: StudyViewModel = viewModel()) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)) {
        StudySpaceContent(onClose, vm)
    }
}

@Composable
private fun StudySpaceContent(onClose: () -> Unit, vm: StudyViewModel) {
    val ctx = LocalContext.current
    val courses by vm.courses.collectAsState()
    val selectedCourse by vm.selectedCourse.collectAsState()
    val soundStates by AmbientMixer.states.collectAsState()
    val activeCount by AmbientMixer.activeCount.collectAsState()

    var mode by remember { mutableStateOf(if (Pomodoro.state.value.running || Pomodoro.state.value.phase != Pomodoro.Phase.IDLE) 1 else Store.prefs.value.lastStudyMode) } // 0=chrono 1=pomodoro
    var showMusic by remember { mutableStateOf(false) }
    val hasCourse = selectedCourse != null

    Box(Modifier.fillMaxSize().background(FocusBg)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "بستن", tint = Color.White) }
                    Spacer(Modifier.weight(1f))
                    Text("فضای مطالعه", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { showMusic = true }) { Icon(Icons.Rounded.LibraryMusic, "موسیقی", tint = Color.White) }
                }
            }

            // mode toggle
            item {
                Spacer(Modifier.height(8.dp))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.07f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))) {
                    Row(Modifier.padding(4.dp)) {
                        SegBtn("کرنومتر", mode == 0) { mode = 0; Store.setLastStudyMode(0) }
                        SegBtn("پومودورو", mode == 1) { mode = 1; Store.setLastStudyMode(1) }
                    }
                }
            }

            // course chips (required)
            item {
                Spacer(Modifier.height(14.dp))
                if (courses.isEmpty()) {
                    Text("اول از خانه یک درس بساز تا بتونی مطالعه ثبت کنی", color = Color(0xFFFB7185), style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(courses) { c ->
                            CourseChip("${c.icon} ${c.name}", selectedCourse == c.id) { vm.selectCourse(c.id) }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                if (mode == 0) ChronoPanel(hasCourse) { mins ->
                    vm.log(mins) { err ->
                        Toast.makeText(ctx, err ?: "ثبت شد 🎉 ($mins دقیقه)", Toast.LENGTH_SHORT).show()
                        if (err == null) Chrono.reset()
                    }
                } else PomodoroPanel(vm, hasCourse)
            }

            // ambient
            item {
                Spacer(Modifier.height(26.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("صداهای محیط", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    AnimatedVisibility(activeCount > 0) {
                        Text("قطع همه (${activeCount.fa()})", color = Color(0xFFFB7185),
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFB7185).copy(alpha = 0.14f))
                                .clickable { AmbientMixer.stopAll() }.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            items(AmbientMixer.sounds.chunked(2)) { rowSounds ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowSounds.forEach { snd ->
                        val st = soundStates[snd.key] ?: AmbientMixer.SoundState()
                        SoundCard(snd.emoji, snd.label, st.active, st.volume, Modifier.weight(1f),
                            { AmbientMixer.toggle(ctx, snd.key) }, { AmbientMixer.setVolume(snd.key, it) })
                    }
                    if (rowSounds.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }

    if (showMusic) MusicPickerSheet(onDismiss = { showMusic = false })
}

@Composable
private fun ChronoPanel(hasCourse: Boolean, onLog: (Int) -> Unit) {
    val ctx = LocalContext.current
    var elapsed by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { elapsed = Chrono.elapsedMs(); running = Chrono.running; delay(400) } }
    val sec = elapsed / 1000

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // رینگ گرادیانی دور تایمر (مطابق ماکاپ) — هر دقیقه یک دور کامل
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
            val frac = (sec % 60) / 60f
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = 16.dp.toPx(); val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(Color.White.copy(alpha = 0.08f), -90f, 360f, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
                if (sec > 0) drawArc(
                    Brush.linearGradient(listOf(FocusMint, FocusCyan)),
                    -90f, 360f * (if (running) frac.coerceAtLeast(0.01f) else frac), false, tl, Size(d, d),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(clockHms(sec), color = Color.White, fontWeight = FontWeight.Black, fontSize = 46.sp)
                Text(if (running) "در حال مطالعه…" else if (sec > 0) "متوقف شده" else "آماده شروع",
                    color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleCtl(Icons.Filled.Refresh, "ریست", 52.dp, Color.White.copy(alpha = 0.12f), Color.White) { Chrono.reset() }
            GradCtl(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, "شروع", 76.dp) {
                if (running) Chrono.pause() else Chrono.start()
            }
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.height(16.dp))
        LogButton(enabled = hasCourse && sec >= 60, text = "ثبت مطالعه (${((sec / 60).toInt()).fa()} دقیقه)") {
            Chrono.pause(); onLog((sec / 60).toInt())
        }
        if (!hasCourse) Text("برای ثبت باید یک درس انتخاب کنی", color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun PomodoroPanel(vm: StudyViewModel, hasCourse: Boolean) {
    val ctx = LocalContext.current
    val pomo by Pomodoro.state.collectAsState()
    val pending by vm.pendingMinutes.collectAsState()
    val phaseTotal = when (pomo.phase) {
        Pomodoro.Phase.WORK, Pomodoro.Phase.IDLE -> pomo.config.workMin * 60
        Pomodoro.Phase.BREAK -> pomo.config.breakMin * 60
        Pomodoro.Phase.LONG_BREAK -> pomo.config.longBreakMin * 60
    }.coerceAtLeast(1)
    val progress = 1f - pomo.remainingSec.toFloat() / phaseTotal
    val isBreak = pomo.phase == Pomodoro.Phase.BREAK || pomo.phase == Pomodoro.Phase.LONG_BREAK
    val ring = if (isBreak) FocusCyan else FocusMint
    val phaseLabel = when (pomo.phase) {
        Pomodoro.Phase.WORK -> "تمرکز"; Pomodoro.Phase.BREAK -> "استراحت کوتاه"
        Pomodoro.Phase.LONG_BREAK -> "استراحت بلند"; Pomodoro.Phase.IDLE -> "آماده تمرکز"
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val stroke = 16.dp.toPx(); val d = size.minDimension - stroke
                val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                drawArc(Color.White.copy(alpha = 0.08f), -90f, 360f, false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(
                    if (isBreak) Brush.linearGradient(listOf(FocusCyan, Color(0xFF818CF8))) else Brush.linearGradient(listOf(FocusMint, FocusCyan)),
                    -90f, 360f * progress.coerceIn(0f, 1f), false, tl, Size(d, d), style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(phaseLabel, color = ring, style = MaterialTheme.typography.titleMedium)
                Text(clockMs(pomo.remainingSec), color = Color.White, fontWeight = FontWeight.Black, fontSize = 52.sp)
                Text("دوره ${pomo.completedWork.fa()} ✓", color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleCtl(Icons.Filled.Refresh, "ریست", 52.dp, Color.White.copy(alpha = 0.12f), Color.White) { Pomodoro.reset() }
            GradCtl(if (pomo.running) Icons.Filled.Pause else Icons.Filled.PlayArrow, "شروع", 76.dp) {
                if (pomo.running) Pomodoro.pause() else Pomodoro.start(ctx)
            }
            CircleCtl(Icons.Filled.SkipNext, "بعدی", 52.dp, Color.White.copy(alpha = 0.12f), Color.White) { Pomodoro.skip(ctx) }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetChip("۲۵ / ۵", pomo.config.workMin == 25) { Pomodoro.setConfig(Pomodoro.Config(25, 5)) }
            PresetChip("۵۰ / ۱۰", pomo.config.workMin == 50) { Pomodoro.setConfig(Pomodoro.Config(50, 10)) }
            PresetChip("۹۰ / ۲۰", pomo.config.workMin == 90) { Pomodoro.setConfig(Pomodoro.Config(90, 20, 30)) }
        }
        Spacer(Modifier.height(16.dp))
        val partial = if (pomo.phase == Pomodoro.Phase.WORK) ((pomo.config.workMin * 60 - pomo.remainingSec) / 60).coerceAtLeast(0) else 0
        val loggable = pending + partial
        Text("مطالعه ثبت‌نشده: ${loggable.fa()} دقیقه", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        LogButton(enabled = hasCourse && loggable >= 1, text = "ثبت مطالعه تا اینجا (${loggable.fa()} دقیقه)") {
            vm.log(loggable) { err ->
                Toast.makeText(ctx, err ?: "ثبت شد 🎉", Toast.LENGTH_SHORT).show()
                if (err == null) { vm.clearPending(); Pomodoro.reset() }
            }
        }
        if (!hasCourse) Text("برای ثبت باید یک درس انتخاب کنی", color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun MusicPickerSheet(onDismiss: () -> Unit) {
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching { MusicTrack.list(Api.arr(Api.get("music_list", "sort" to "newest"))) }.onSuccess { tracks = it }
        runCatching { Playlist.list(Api.arr(Api.get("playlist_list"))) }.onSuccess { playlists = it }
    }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp)) {
            item { Text("موسیقی مطالعه 🎧", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(10.dp)) }
            if (playlists.isNotEmpty()) {
                item { Text("پلی‌لیست‌ها", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(6.dp)) }
                items(playlists) { p ->
                    Surface(color = Color.Transparent, onClick = {
                        scope.launch {
                            runCatching { MusicTrack.list(Api.arr(Api.get("playlist_music", "playlist_id" to p.id.toString()))) }
                                .onSuccess { if (it.isNotEmpty()) PlayerHolder.play(it, 0) }
                        }
                        onDismiss()
                    }, modifier = Modifier.fillMaxWidth()) {
                        PlaylistRow(p)
                    }
                }
            }
            item { Spacer(Modifier.height(10.dp)); Text("همه آهنگ‌ها", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(6.dp)) }
            items(tracks) { t ->
                Surface(color = Color.Transparent, onClick = { PlayerHolder.play(tracks, tracks.indexOf(t)) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${t.artist} · ${t.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(p: Playlist) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.LibraryMusic, null, tint = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(10.dp))
        Text(p.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("${p.musicCount.fa()} آهنگ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/* ---------- small helpers ---------- */

private fun clockHms(totalSec: Long): String {
    val h = totalSec / 3600; val m = (totalSec % 3600) / 60; val s = totalSec % 60
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s).fa()
}
private fun clockMs(totalSec: Int): String = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60).fa()

@Composable
private fun SegBtn(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.Transparent, onClick = onClick) {
        Box(Modifier.clip(CircleShape).background(if (selected) FocusGrad else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))) {
            Text(text, color = if (selected) FocusInk else Color.White,
                style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun CourseChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.Transparent, onClick = onClick,
        border = if (!selected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null) {
        Box(Modifier.clip(CircleShape).background(if (selected) FocusGrad else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))))) {
            Text(text, color = if (selected) FocusInk else Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PresetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = if (selected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f), onClick = onClick) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
private fun LogButton(enabled: Boolean, text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        onClick = { if (enabled) onClick() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
    ) {
        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(
            if (enabled) FocusGrad else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.10f))))) {
            Text(text, color = if (enabled) FocusInk else Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp))
        }
    }
}

@Composable
private fun CircleCtl(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, bg: Color, fg: Color, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = bg, onClick = onClick, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, desc, tint = fg, modifier = Modifier.size(size * 0.42f)) }
    }
}

/** دکمهٔ اصلی گرادیانی تایمر (پخش/توقف). */
@Composable
private fun GradCtl(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = Color.Transparent, onClick = onClick, shadowElevation = 10.dp, modifier = Modifier.size(size)) {
        Box(Modifier.background(FocusGrad), contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = FocusInk, modifier = Modifier.size(size * 0.46f))
        }
    }
}

@Composable
private fun SoundCard(emoji: String, label: String, active: Boolean, volume: Float, modifier: Modifier = Modifier, onToggle: () -> Unit, onVolume: (Float) -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(22.dp),
        color = if (active) FocusMint.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) FocusMint.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.08f)),
        onClick = onToggle) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.height(6.dp))
            Text(label, color = if (active) Color.White else Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelLarge)
            AnimatedVisibility(active) {
                Slider(value = volume, onValueChange = onVolume, modifier = Modifier.height(28.dp),
                    colors = SliderDefaults.colors(thumbColor = FocusMint, activeTrackColor = FocusMint, inactiveTrackColor = Color.White.copy(alpha = 0.2f)))
            }
        }
    }
}
