package ir.shadbib.app.ui.study.room

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.shadbib.app.R
import ir.shadbib.app.core.fa
import ir.shadbib.app.player.Chrono
import ir.shadbib.app.player.Pomodoro
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

// ---------------------------------------------------------------- پالت نئوبروتالیست
private val Ink = Color(0xFF2B1E16)
private val DeskFill = Color(0xFFCE965F)
private val Cream = Color(0xFFF7F3E8)
private val Mint = Color(0xFF34D399)
private val Coral = Color(0xFFFF8A65)

// ---------------------------------------------------------------- هندسهٔ صحنه
// همه کسری از عرض/ارتفاع صفحه‌اند — روی هر سایزی یکسان درمی‌آید.
private const val BG_ZOOM = 1.20f
private val BACK_SEATS = listOf(0.24f, 0.50f, 0.76f)
private const val BACK_BASELINE = 0.800f
private const val BACK_CANVAS_H = 0.271f
private const val BACK_DESK_TOP = 0.760f
private const val BACK_DESK_H = 0.028f
private const val FRONT_BASELINE = 0.995f
private const val FRONT_CANVAS_H = 0.376f
private const val FRONT_DESK_TOP = 0.939f

@Composable
fun StudyRoomScreen(vm: StudyRoomViewModel = viewModel()) {
    val context = LocalContext.current
    val snap by vm.snapshot.collectAsState()
    val myChar by vm.character.collectAsState()
    val err by vm.error.collectAsState()
    val pomo by Pomodoro.state.collectAsState()

    var showPicker by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    // 0 = پومودورو، 1 = کرنومتر
    var mode by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        vm.enter()
        onDispose { vm.leave() }
    }

    // تیک یک‌ثانیه‌ای فقط برای بازخوانی کرنومتر (پومودورو خودش StateFlow دارد)
    var chronoSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            chronoSec = (Chrono.elapsedMs() / 1000).toInt()
            delay(1000)
        }
    }

    // پلک‌زدن — تصادفی تا همه با هم پلک نزنند
    var blinkOn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2600, 6200))
            blinkOn = true
            delay(130)
            blinkOn = false
        }
    }

    val inf = rememberInfiniteTransition(label = "room")
    val breath by inf.animateFloat(
        1f, 1.035f,
        infiniteRepeatable(tween(2300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val bob by inf.animateFloat(
        0f, -6f,
        infiniteRepeatable(tween(3100, easing = LinearEasing), RepeatMode.Reverse),
        label = "bob",
    )

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    var night by remember { mutableStateOf(hour < 6 || hour >= 18) }

    val myState = vm.myState()

    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val w = maxWidth
        val h = maxHeight

        // ---------------- لایه ۱: پس‌زمینه ----------------
        Image(
            painter = painterResource(
                if (night) R.drawable.room_library_night else R.drawable.room_library_day
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = BG_ZOOM
                    scaleY = BG_ZOOM
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
        )

        // ---------------- لایه ۲: همسایه‌ها (ردیف عقب) ----------------
        val others = snap.others.take(BACK_SEATS.size)
        others.forEachIndexed { i, occ ->
            Seat(
                characterKey = occ.character,
                state = occ.state,
                blinking = blinkOn,
                cx = BACK_SEATS[i],
                baseline = BACK_BASELINE,
                canvasH = BACK_CANVAS_H,
                parentW = w,
                parentH = h,
                breath = if (occ.state == RoomState.SLEEPING) 1f else breath,
                bobPx = if (occ.state == RoomState.SLEEPING) 0f else bob * 0.6f,
                dim = occ.state == RoomState.SLEEPING,
            )
        }

        // ---------------- لایه ۳: میز عقب (روی پایین‌تنه می‌افتد) ----------------
        Desk(0.04f, 0.96f, BACK_DESK_TOP, BACK_DESK_H, w, h, radius = 9.dp, stroke = 3.dp)

        // نام همسایه‌ها — روی میز، تا زیر میز گم نشود
        others.forEachIndexed { i, occ ->
            NameTag(
                text = occ.username,
                cx = BACK_SEATS[i],
                y = BACK_DESK_TOP + BACK_DESK_H + 0.006f,
                parentW = w,
                parentH = h,
            )
        }

        // ---------------- لایه ۴: خود کاربر ----------------
        Seat(
            characterKey = myChar,
            state = myState,
            blinking = blinkOn,
            cx = 0.50f,
            baseline = FRONT_BASELINE,
            canvasH = FRONT_CANVAS_H,
            parentW = w,
            parentH = h,
            breath = breath,
            bobPx = bob,
            dim = false,
        )

        // ---------------- لایه ۵: میز جلو ----------------
        Desk(-0.03f, 1.03f, FRONT_DESK_TOP, 1.10f - FRONT_DESK_TOP, w, h, radius = 14.dp, stroke = 4.dp)

        // ---------------- لایه ۶: HUD ----------------
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NbCard(fill = Cream, modifier = Modifier.fillMaxWidth(0.68f)) {
                Text(
                    text = if (mode == 0) clockMS(pomo.remainingSec) else clockHMS(chronoSec),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NbChip(
                    text = "۰۱".let { _ -> snap.online.fa() + " نفر در اتاق" },
                    fill = Mint,
                )
                Spacer(Modifier.width(8.dp))
                NbIconButton(
                    icon = if (night) Icons.Rounded.Bedtime else Icons.Rounded.WbSunny,
                    fill = Cream,
                ) { night = !night }
                Spacer(Modifier.width(8.dp))
                NbIconButton(icon = Icons.Rounded.Face, fill = Coral) { showPicker = true }
            }
            if (err != null) {
                Spacer(Modifier.height(8.dp))
                NbChip(text = err ?: "", fill = Color(0xFFFCA5A5))
            }
        }

        // ---------------- لایه ۷: کنترل‌ها ----------------
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                ModeTab("پومودورو", mode == 0) { mode = 0 }
                Spacer(Modifier.width(8.dp))
                ModeTab("کرنومتر", mode == 1) { mode = 1 }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val running = if (mode == 0) pomo.running else Chrono.running
                NbIconButton(
                    icon = if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    fill = Mint,
                    big = true,
                ) {
                    if (mode == 0) {
                        if (pomo.running) Pomodoro.pause() else Pomodoro.start(context)
                    } else {
                        if (Chrono.running) Chrono.pause() else Chrono.start()
                    }
                    vm.pulse()
                }
                Spacer(Modifier.width(12.dp))
                NbIconButton(icon = Icons.Rounded.Refresh, fill = Cream) {
                    if (mode == 0) Pomodoro.reset() else Chrono.reset()
                    vm.pulse()
                }
                Spacer(Modifier.width(12.dp))
                NbTextButton(text = "ثبت مطالعه", fill = Coral) {
                    val minutes =
                        if (mode == 0) pomo.completedWork * pomo.config.workMin
                        else chronoSec / 60
                    vm.logMinutes(minutes, null) { e ->
                        toast = e ?: "${minutes.fa()} دقیقه ثبت شد ✅"
                        if (e == null) {
                            if (mode == 1) Chrono.reset() else Pomodoro.reset()
                        }
                    }
                }
            }
            if (toast != null) {
                Spacer(Modifier.height(10.dp))
                NbChip(text = toast ?: "", fill = Cream)
                LaunchedEffect(toast) {
                    delay(2600)
                    toast = null
                }
            }
        }

        // ---------------- لایه ۸: انتخاب شخصیت ----------------
        if (showPicker) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Ink.copy(alpha = 0.62f))
                    .clickable { showPicker = false },
            )
            NbCard(
                fill = Cream,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "شخصیتت رو انتخاب کن",
                        fontWeight = FontWeight.Black,
                        color = Ink,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(RoomChars.all, key = { it.key }) { c ->
                            val selected = c.key == myChar
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) Mint else Color.White)
                                    .border(
                                        if (selected) 4.dp else 2.dp,
                                        Ink,
                                        RoundedCornerShape(14.dp),
                                    )
                                    .clickable {
                                        vm.selectCharacter(c.key)
                                        showPicker = false
                                    }
                                    .padding(6.dp),
                            ) {
                                Image(
                                    painter = painterResource(c.idle),
                                    contentDescription = c.fa,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(66.dp),
                                )
                                Text(c.fa, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================================================================== اجزا

@Composable
private fun Seat(
    characterKey: String,
    state: String,
    blinking: Boolean,
    cx: Float,
    baseline: Float,
    canvasH: Float,
    parentW: Dp,
    parentH: Dp,
    breath: Float,
    bobPx: Float,
    dim: Boolean,
) {
    val ch = RoomChars.of(characterKey)
    val hDp = parentH * canvasH
    val wDp = hDp * RoomChars.ASPECT
    Box(
        Modifier
            .offset(x = parentW * cx - wDp / 2, y = parentH * baseline - hDp)
            .size(wDp, hDp)
    ) {
        Image(
            painter = painterResource(RoomChars.frame(ch, state, blinking)),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = breath
                    scaleY = breath
                    translationY = bobPx
                    alpha = if (dim) 0.6f else 1f
                    transformOrigin = TransformOrigin(0.5f, 1f)
                },
        )
    }
}

@Composable
private fun Desk(
    x0: Float,
    x1: Float,
    top: Float,
    thickness: Float,
    parentW: Dp,
    parentH: Dp,
    radius: Dp,
    stroke: Dp,
) {
    val shape = RoundedCornerShape(radius)
    val wDp = parentW * (x1 - x0)
    val tDp = parentH * thickness
    Box(Modifier.offset(x = parentW * x0, y = parentH * top)) {
        // سایهٔ سخت و جابجاشده — امضای نئوبروتالیسم
        Box(Modifier.offset(x = 4.dp, y = 5.dp).size(wDp, tDp).background(Ink, shape))
        Box(Modifier.size(wDp, tDp).background(DeskFill, shape).border(stroke, Ink, shape))
    }
}

@Composable
private fun NameTag(text: String, cx: Float, y: Float, parentW: Dp, parentH: Dp) {
    val wDp = parentW * 0.24f
    Box(
        Modifier
            .offset(x = parentW * cx - wDp / 2, y = parentH * y)
            .width(wDp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Cream,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(CircleShape)
                .background(Ink.copy(alpha = 0.72f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun NbCard(fill: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(modifier) {
        Box(Modifier.matchParentSize().offset(x = 5.dp, y = 6.dp).background(Ink, shape))
        Box(Modifier.fillMaxWidth().background(fill, shape).border(4.dp, Ink, shape)) { content() }
    }
}

@Composable
private fun NbChip(text: String, fill: Color) {
    Box {
        Box(Modifier.matchParentSize().offset(x = 3.dp, y = 4.dp).background(Ink, CircleShape))
        Text(
            text = text,
            color = Ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clip(CircleShape)
                .background(fill)
                .border(3.dp, Ink, CircleShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun NbIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fill: Color,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    val s = if (big) 62.dp else 46.dp
    Box(Modifier.size(s)) {
        Box(Modifier.matchParentSize().offset(x = 3.dp, y = 4.dp).background(Ink, CircleShape))
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(fill)
                .border(3.dp, Ink, CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(if (big) 32.dp else 24.dp))
        }
    }
}

@Composable
private fun NbTextButton(text: String, fill: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box {
        Box(Modifier.matchParentSize().offset(x = 3.dp, y = 4.dp).background(Ink, shape))
        Text(
            text = text,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(shape)
                .background(fill)
                .border(3.dp, Ink, shape)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        color = Ink,
        fontSize = 13.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Mint else Cream)
            .border(3.dp, Ink, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

private fun clockMS(sec: Int): String =
    String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60).fa()

private fun clockHMS(sec: Int): String =
    String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60).fa()
