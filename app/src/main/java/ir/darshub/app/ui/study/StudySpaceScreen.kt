@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ir.darshub.app.ui.study

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.darshub.app.core.Api
import ir.darshub.app.core.Store
import ir.darshub.app.core.fa
import ir.darshub.app.data.MusicTrack
import ir.darshub.app.data.Playlist
import ir.darshub.app.player.AmbientMixer
import ir.darshub.app.player.Chrono
import ir.darshub.app.player.PlayerHolder
import ir.darshub.app.player.Pomodoro
import ir.darshub.app.ui.theme.DarsMotion
import ir.darshub.app.ui.theme.pressScale
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/* رنگ‌های فضای تمرکز ۲۰۲۶ — نیمه‌شبِ عمیق با شفق نعنایی/آبی */
private val FocusBg = Brush.radialGradient(
    listOf(Color(0xFF0F1F17), Color(0xFF0B100E), Color(0xFF081018)),
    center = Offset(300f, 200f), radius = 900f,
)
private val FocusMint = Color(0xFF4ADE9F)
private val FocusCyan = Color(0xFF38BDF8)
private val FocusInk = Color(0xFF06110C)
private val FocusGrad = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF38BDF8)))

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
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, "بستن", tint = Color.White.copy(alpha = 0.85f)) }
                    Spacer(Modifier.weight(1f))
                    Text("فضای مطالعه", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.weight(1f))
                    val musicInter = remember { MutableInteractionSource() }
                    IconButton(onClick = { showMusic = true }, modifier = Modifier.pressScale(musicInter, pressedScale = 0.88f), interactionSource = musicInter) {
                        Icon(Icons.Rounded.LibraryMusic, "موسیقی", tint = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            // mode toggle
            item {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = CircleShape, color = Color.White.copy(alpha = 0.07f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                    shadowElevation = 0.dp,
                ) {
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
    var elapsed by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { elapsed = Chrono.elapsedMs(); running = Chrono.running; delay(400) } }
    val sec = elapsed / 1000
    val minutes = (sec / 60).toInt()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TimerRing(progress = (sec % 60) / 60f, running = running, ringA = FocusMint, ringB = FocusCyan) {
            Text(clockHms(sec), color = Color.White, fontWeight = FontWeight.Black, fontSize = 44.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                if (running) "در حال مطالعه…"
                else if (sec > 0) "متوقف شده"
                else "آمادهٔ شروع",
                color = if (running) FocusMint else Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
            if (minutes > 0) {
                Spacer(Modifier.height(7.dp))
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.10f)) {
                    Text(
                        minutes.fa() + " دقیقهٔ قابل ثبت",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleCtl(Icons.Filled.Refresh, "ریست", 54.dp, Color.White.copy(alpha = 0.10f), Color.White) { Chrono.reset() }
            GradCtl(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, "شروع", 82.dp, running) {
                if (running) Chrono.pause() else Chrono.start()
            }
            Spacer(Modifier.size(54.dp))
        }
        Spacer(Modifier.height(20.dp))
        LogButton(
            enabled = hasCourse && sec >= 60,
            text = "ثبت مطالعه (" + minutes.fa() + " دقیقه)",
        ) {
            Chrono.pause()
            onLog(minutes)
        }
        if (!hasCourse) Text(
            "برای ثبت باید یک درس انتخاب کنی",
            color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
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
    val ringA = if (isBreak) FocusCyan else FocusMint
    val ringB = if (isBreak) Color(0xFF818CF8) else FocusCyan
    val phaseLabel = when (pomo.phase) {
        Pomodoro.Phase.WORK -> "تمرکز"
        Pomodoro.Phase.BREAK -> "استراحت کوتاه"
        Pomodoro.Phase.LONG_BREAK -> "استراحت بلند"
        Pomodoro.Phase.IDLE -> "آمادهٔ تمرکز"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TimerRing(progress = progress.coerceIn(0f, 1f), running = pomo.running, ringA = ringA, ringB = ringB) {
            Surface(shape = CircleShape, color = ringA.copy(alpha = 0.16f)) {
                Text(
                    phaseLabel, color = ringA, style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(clockMs(pomo.remainingSec), color = Color.White, fontWeight = FontWeight.Black, fontSize = 50.sp, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            CycleDots(pomo.completedWork)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleCtl(Icons.Filled.Refresh, "ریست", 54.dp, Color.White.copy(alpha = 0.10f), Color.White) { Pomodoro.reset() }
            GradCtl(if (pomo.running) Icons.Filled.Pause else Icons.Filled.PlayArrow, "شروع", 82.dp, pomo.running) {
                if (pomo.running) Pomodoro.pause() else Pomodoro.start(ctx)
            }
            CircleCtl(Icons.Filled.SkipNext, "بعدی", 54.dp, Color.White.copy(alpha = 0.10f), Color.White) { Pomodoro.skip(ctx) }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetChip("۲۵ / ۵", pomo.config.workMin == 25) { Pomodoro.setConfig(Pomodoro.Config(25, 5)) }
            PresetChip("۵۰ / ۱۰", pomo.config.workMin == 50) { Pomodoro.setConfig(Pomodoro.Config(50, 10)) }
            PresetChip("۹۰ / ۲۰", pomo.config.workMin == 90) { Pomodoro.setConfig(Pomodoro.Config(90, 20, 30)) }
        }
        Spacer(Modifier.height(18.dp))
        val partial = if (pomo.phase == Pomodoro.Phase.WORK) ((pomo.config.workMin * 60 - pomo.remainingSec) / 60).coerceAtLeast(0) else 0
        val loggable = pending + partial
        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.07f)) {
            Text(
                "مطالعهٔ ثبت‌نشده: " + loggable.fa() + " دقیقه",
                color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        LogButton(
            enabled = hasCourse && loggable >= 1,
            text = "ثبت مطالعه تا اینجا (" + loggable.fa() + " دقیقه)",
        ) {
            vm.log(loggable) { err ->
                Toast.makeText(ctx, err ?: "ثبت شد 🎉", Toast.LENGTH_SHORT).show()
                if (err == null) { vm.clearPending(); Pomodoro.reset() }
            }
        }
        if (!hasCourse) Text(
            "برای ثبت باید یک درس انتخاب کنی",
            color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
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

/** دکمهٔ سگمنت حالت: قرص گرادیانی وقتی انتخاب شده. */
@Composable
private fun SegBtn(text: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape,
        color = if (selected) FocusGrad else Color.White.copy(alpha = 0.06f),
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier.pressScale(interaction, pressedScale = 0.95f),
    ) {
        Text(
            text,
            color = if (selected) FocusInk else Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun CourseChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(shape = CircleShape, color = Color.Transparent, onClick = onClick, interactionSource = interaction,
        border = if (!selected) BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)) else null,
        modifier = Modifier.pressScale(interaction, pressedScale = 0.95f)) {
        Box(Modifier.clip(CircleShape).background(if (selected) FocusGrad else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))))) {
            Text(text, color = if (selected) FocusInk else Color.White, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PresetChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (selected) 1.05f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "preset",
    )
    Surface(
        shape = CircleShape,
        color = if (selected) FocusMint.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, if (selected) FocusMint else Color.White.copy(alpha = 0.12f)),
        onClick = onClick,
        modifier = Modifier.scale(scale),
    ) {
        Text(
            text,
            color = if (selected) FocusMint else Color.White.copy(alpha = 0.7f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/**
 * The button the whole screen exists for.
 *
 * While armed, a light sweep travels across it so the eye is pulled there the
 * moment enough minutes have accumulated; pressing springs it inward with a
 * haptic tick, and the drop shadow fades in and out with the enabled state.
 */
@Composable
private fun LogButton(enabled: Boolean, text: String, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.96f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "logScale",
    )
    val glow by animateFloatAsState(if (enabled) 1f else 0f, tween(400), label = "logGlow")
    val inf = rememberInfiniteTransition(label = "logShine")
    val shine by inf.animateFloat(
        -1f, 1f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "shine",
    )

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        enabled = enabled,
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .scale(scale)
            .shadow(
                (16f * glow).dp, RoundedCornerShape(22.dp),
                ambientColor = FocusMint.copy(alpha = 0.4f * glow),
                spotColor = FocusCyan.copy(alpha = 0.45f * glow),
            ),
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (enabled) FocusGrad
                    else Brush.linearGradient(listOf(Color.White.copy(alpha = 0.07f), Color.White.copy(alpha = 0.07f)))
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (enabled) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { translationX = shine * size.width }
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent)
                            )
                        )
                )
            }
            Text(
                text,
                color = if (enabled) FocusInk else Color.White.copy(alpha = 0.32f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 18.dp),
            )
        }
    }
}

@Composable
private fun CircleCtl(icon: ImageVector, desc: String, size: Dp, bg: Color, fg: Color, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape, color = bg, onClick = onClick, modifier = Modifier.size(size),
        interactionSource = interaction,
    ) {
        Box(
            Modifier.fillMaxSize().pressScale(interaction, pressedScale = 0.88f),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, desc, tint = fg, modifier = Modifier.size(size * 0.42f)) }
    }
}

/** دکمهٔ اصلی گرادیانی تایمر (پخش/توقف) با هالهٔ نرم. */
@Composable
private fun GradCtl(icon: ImageVector, desc: String, box: Dp, active: Boolean = false, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.90f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "gradScale",
    )
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interaction,
        modifier = Modifier
            .size(box)
            .scale(scale)
            .shadow(
                18.dp, CircleShape,
                ambientColor = FocusMint.copy(alpha = 0.5f),
                spotColor = FocusCyan.copy(alpha = 0.55f),
            ),
    ) {
        Box(Modifier.background(FocusGrad), contentAlignment = Alignment.Center) {
            Icon(icon, desc, tint = FocusInk, modifier = Modifier.size(box * 0.42f))
        }
    }
}

/** کارت صدای محیطی: ایموجی + برچسب + اسلایدر حجم وقتی فعال است. */
@Composable
private fun SoundCard(emoji: String, label: String, active: Boolean, volume: Float, modifier: Modifier = Modifier, onToggle: () -> Unit, onVolume: (Float) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = if (active) FocusMint.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, if (active) FocusMint.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.09f)),
        onClick = onToggle,
        interactionSource = interaction,
        modifier = modifier.pressScale(interaction, pressedScale = 0.96f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, color = if (active) FocusMint else Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            AnimatedVisibility(visible = active) {
                Slider(
                    value = volume,
                    onValueChange = onVolume,
                    valueRange = 0f..1f,
                    modifier = Modifier.padding(horizontal = 2.dp).fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = FocusMint,
                        activeTrackColor = FocusMint,
                        inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                    ),
                )
            }
        }
    }
}

/**
 * حلقهٔ تایمر: ۶۰ خط ساعت، قوس پیشرفت گرادیانی و هالهٔ تنفس.
 */
@Composable
private fun TimerRing(
    progress: Float,
    running: Boolean,
    ringA: Color,
    ringB: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val breath by animateFloatAsState(
        if (running) 1.015f else 1f,
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "breath",
    )
    val shown by animateFloatAsState(progress.coerceIn(0f, 1f), tween(500), label = "ringProgress")
    val haloAlpha by animateFloatAsState(if (running) 0.22f else 0.07f, tween(700), label = "halo")

    Box(
        modifier = Modifier.size(264.dp).scale(breath),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(264.dp)
                .background(Brush.radialGradient(listOf(ringA.copy(alpha = haloAlpha), Color.Transparent)), CircleShape)
        )
        Canvas(Modifier.size(232.dp)) {
            val stroke = 16.dp.toPx()
            val d = size.minDimension - stroke * 2.6f
            val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rBase = d / 2f + stroke * 1.15f
            for (i in 0 until 60) {
                val a = Math.toRadians((i * 6).toDouble())
                val major = i % 5 == 0
                val len = if (major) stroke * 0.5f else stroke * 0.26f
                val r1 = rBase
                val r0 = r1 - len
                drawLine(
                    color = Color.White.copy(alpha = if (major) 0.22f else 0.09f),
                    start = Offset(cx + (r0 * cos(a)).toFloat(), cy + (r0 * sin(a)).toFloat()),
                    end = Offset(cx + (r1 * cos(a)).toFloat(), cy + (r1 * sin(a)).toFloat()),
                    strokeWidth = if (major) stroke * 0.15f else stroke * 0.08f,
                    cap = StrokeCap.Round,
                )
            }
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            if (shown > 0.001f) drawArc(
                brush = Brush.sweepGradient(listOf(ringA, ringB, ringA)),
                startAngle = -90f, sweepAngle = 360f * shown, useCenter = false,
                topLeft = tl, size = Size(d, d),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

/** Four dots showing where in the current pomodoro set the user is. */
@Composable
private fun CycleDots(done: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 4) {
            val filled = if (done > 0 && done % 4 == 0) true else (done % 4) > i
            val a by animateFloatAsState(if (filled) 1f else 0.22f, tween(400), label = "dot")
            Box(Modifier.size(if (filled) 9.dp else 6.dp).background(FocusMint.copy(alpha = a), CircleShape))
        }
        Spacer(Modifier.width(6.dp))
        Text(
            "دورهٔ " + done.fa(),
            color = Color.White.copy(alpha = 0.6f),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
