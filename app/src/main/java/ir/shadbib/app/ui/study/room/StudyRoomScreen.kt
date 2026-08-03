package ir.shadbib.app.ui.study.room

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.shadbib.app.R
import ir.shadbib.app.core.fa
import ir.shadbib.app.player.Chrono
import ir.shadbib.app.player.Pomodoro
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
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

// ---------------------------------------------------------------- ساعت دیواری
// مختصات ساعت روی تصویر پس‌زمینه (۷۶۸×۱۳۷۶)، کسری از ابعاد خود تصویر.
private const val BG_W = 768f
private const val BG_H = 1376f
private const val CLOCK_CX = 0.4993f
private const val CLOCK_CY = 0.4978f
private const val CLOCK_R = 0.0749f // شعاع حلقهٔ بیرونی، کسری از عرض تصویر
private val ClockFaceDay = Color(0xFFF7E9CD)
private val ClockFaceNight = Color(0xFFDCC5A0)

@Composable
fun StudyRoomScreen(vm: StudyRoomViewModel = viewModel()) {
    val context = LocalContext.current
    val snap by vm.snapshot.collectAsState()
    val myChar by vm.character.collectAsState()
    val myState by vm.myState.collectAsState()
    val err by vm.error.collectAsState()
    val pomo by Pomodoro.state.collectAsState()

    var showPicker by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    // 0 = پومودورو، 1 = کرنومتر
    var mode by remember { mutableIntStateOf(0) }

    // ---------------- حضور وابسته به چرخهٔ عمر ----------------
    // تا وقتی این صفحه جلوی چشم کاربر است حضور برقرار است؛ همین که اپ مینیمایز
    // شود یا صفحه عوض شود، room_leave می‌رود و فوراً از اتاق حذف می‌شویم.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> vm.enter()
                Lifecycle.Event.ON_STOP -> vm.leave()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        vm.enter()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.leave()
        }
    }

    // تیک یک‌ثانیه‌ای — هم کرنومتر را می‌خواند و هم ساعت دیواری را به‌روز می‌کند
    var chronoSec by remember { mutableIntStateOf(0) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var chronoRunning by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            chronoSec = (Chrono.elapsedMs() / 1000).toInt()
            chronoRunning = Chrono.running
            nowMs = System.currentTimeMillis()
            delay(1000)
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

    // حالت شب/روز از ساعت واقعی دستگاه — ولی قابل تغییر دستی
    val startHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    var night by remember { mutableStateOf(startHour < 6 || startHour >= 18) }

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

        // ---------------- لایه ۱.۵: عقربه‌های ساعت واقعی ----------------
        WallClock(nowMs = nowMs, night = night)

        // ---------------- لایه ۲: همسایه‌ها (ردیف عقب) ----------------
        val others = snap.others.take(BACK_SEATS.size)
        others.forEachIndexed { i, occ ->
            Seat(
                seatId = "back$i",
                characterKey = occ.character,
                state = occ.state,
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
            seatId = "me",
            characterKey = myChar,
            state = myState,
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

        // ---------------- لایه ۶: پنل کنترل — کاملاً بالا ----------------
        // همهٔ دکمه‌ها بالا جمع شدند تا هیچ‌وقت روی کاراکتر خود کاربر نیفتند.
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NbCard(fill = Cream, modifier = Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.Center) {
                        ModeTab("پومودورو", mode == 0) { mode = 0 }
                        Spacer(Modifier.width(8.dp))
                        ModeTab("کرنومتر", mode == 1) { mode = 1 }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (mode == 0) clockMS(pomo.remainingSec) else clockHMS(chronoSec),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val running = if (mode == 0) pomo.running else chronoRunning
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
                            chronoRunning = Chrono.running
                            vm.pulse()
                        }
                        Spacer(Modifier.width(10.dp))
                        NbIconButton(icon = Icons.Rounded.Refresh, fill = Color.White) {
                            if (mode == 0) Pomodoro.reset() else Chrono.reset()
                            chronoRunning = Chrono.running
                            vm.pulse()
                        }
                        Spacer(Modifier.width(10.dp))
                        NbTextButton(text = "ثبت مطالعه", fill = Coral) {
                            val minutes =
                                if (mode == 0) pomo.completedWork * pomo.config.workMin
                                else chronoSec / 60
                            vm.logMinutes(minutes, null) { e ->
                                toast = e ?: "${minutes.fa()} دقیقه ثبت شد ✅"
                                if (e == null) {
                                    if (mode == 1) Chrono.reset() else Pomodoro.reset()
                                    chronoRunning = Chrono.running
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NbChip(text = snap.online.fa() + " نفر در اتاق", fill = Mint)
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
            if (toast != null) {
                Spacer(Modifier.height(8.dp))
                NbChip(text = toast ?: "", fill = Cream)
                LaunchedEffect(toast) {
                    delay(2600)
                    toast = null
                }
            }
        }

        // ---------------- لایه ۷: انتخاب شخصیت ----------------
        if (showPicker) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Ink.copy(alpha = 0.62f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        showPicker = false
                    },
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

/**
 * عقربه‌های ساعت دیواری — زمان واقعی دستگاه.
 *
 * ساعت در خود تصویر پس‌زمینه نقاشی شده و عقربه‌هایش ثابت‌اند؛ پس اول صفحهٔ
 * ساعت را با همان رنگ کرمی پوشانده، مایل‌ها را دوباره می‌کشیم و عقربه‌های
 * درست را رویش می‌اندازیم.
 *
 * مختصات دقیقاً همان تبدیلی را می‌خورند که ContentScale.Crop با لنگر
 * BottomCenter پلوس زوم [BG_ZOOM] روی پس‌زمینه اعمال می‌کند.
 */
@Composable
private fun WallClock(nowMs: Long, night: Boolean) {
    val cal = remember(nowMs / 1000) {
        Calendar.getInstance().apply { timeInMillis = nowMs }
    }
    val hh = cal.get(Calendar.HOUR_OF_DAY) % 12
    val mm = cal.get(Calendar.MINUTE)
    val ss = cal.get(Calendar.SECOND)
    val face = if (night) ClockFaceNight else ClockFaceDay

    Canvas(Modifier.fillMaxSize()) {
        val sw = size.width
        val sh = size.height
        // مقیاس نهایی پس‌زمینه = مقیاس Crop × زوم، لنگر پایین-وسط
        val s = max(sw / BG_W, sh / BG_H) * BG_ZOOM
        val dw = BG_W * s
        val dh = BG_H * s
        val left = (sw - dw) / 2f
        val top = sh - dh

        val cx = left + dw * CLOCK_CX
        val cy = top + dh * CLOCK_CY
        val rOuter = dw * CLOCK_R
        val rFace = rOuter * 0.80f

        // اگر ساعت بیرون کادر افتاد، چیزی نمی‌کشیم
        if (cy + rOuter < 0f || cy - rOuter > sh) return@Canvas

        // صفحهٔ ساعت
        drawCircle(color = face, radius = rFace, center = Offset(cx, cy))

        // مایل‌های ۱۲گانه
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val r1 = rFace * 0.80f
            val r2 = rFace * 0.93f
            drawLine(
                color = Ink,
                start = Offset(cx + (r1 * cos(a)).toFloat(), cy + (r1 * sin(a)).toFloat()),
                end = Offset(cx + (r2 * cos(a)).toFloat(), cy + (r2 * sin(a)).toFloat()),
                strokeWidth = rFace * (if (i % 3 == 0) 0.10f else 0.055f),
                cap = StrokeCap.Round,
            )
        }

        // عقربهٔ ساعت
        val ha = Math.toRadians((hh + mm / 60.0) * 30.0 - 90.0)
        drawLine(
            color = Ink,
            start = Offset(cx, cy),
            end = Offset(cx + (rFace * 0.46f * cos(ha)).toFloat(), cy + (rFace * 0.46f * sin(ha)).toFloat()),
            strokeWidth = rFace * 0.13f,
            cap = StrokeCap.Round,
        )
        // عقربهٔ دقیقه
        val ma = Math.toRadians((mm + ss / 60.0) * 6.0 - 90.0)
        drawLine(
            color = Ink,
            start = Offset(cx, cy),
            end = Offset(cx + (rFace * 0.70f * cos(ma)).toFloat(), cy + (rFace * 0.70f * sin(ma)).toFloat()),
            strokeWidth = rFace * 0.09f,
            cap = StrokeCap.Round,
        )
        // عقربهٔ ثانیه
        val sa = Math.toRadians(ss * 6.0 - 90.0)
        drawLine(
            color = Coral,
            start = Offset(cx, cy),
            end = Offset(cx + (rFace * 0.78f * cos(sa)).toFloat(), cy + (rFace * 0.78f * sin(sa)).toFloat()),
            strokeWidth = rFace * 0.05f,
            cap = StrokeCap.Round,
        )
        // پیچ وسط
        drawCircle(color = Ink, radius = rFace * 0.09f, center = Offset(cx, cy))
    }
}

@Composable
private fun Seat(
    seatId: String,
    characterKey: String,
    state: String,
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

    // پلک‌زدن مختصّ همین صندلی — قبلاً یک تایمر مشترک بود و همه با هم پلک
    // می‌زدند؛ حالا هر کس ریتم خودش را دارد. در حالت مطالعه/خواب اصلاً پلک نمی‌زند.
    var blinking by remember(seatId) { mutableStateOf(false) }
    LaunchedEffect(seatId, state) {
        if (state != RoomState.IDLE) {
            blinking = false
            return@LaunchedEffect
        }
        delay(Random.nextLong(900, 3000))
        while (true) {
            blinking = true
            delay(120)
            blinking = false
            delay(Random.nextLong(3400, 7200))
        }
    }

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
    Box(
        Modifier
            .offset(x = parentW * x0, y = parentH * top)
            .width(parentW * (x1 - x0))
            .height(parentH * thickness)
            .clip(RoundedCornerShape(radius))
            .background(DeskFill)
            .border(stroke, Ink, RoundedCornerShape(radius))
    )
}

@Composable
private fun NameTag(text: String, cx: Float, y: Float, parentW: Dp, parentH: Dp) {
    Box(
        Modifier
            .offset(x = parentW * cx - 34.dp, y = parentH * y)
            .width(68.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Cream)
            .border(2.dp, Ink, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Ink,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun NbCard(fill: Color, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(4.dp, Ink, RoundedCornerShape(18.dp))
    ) { content() }
}

@Composable
private fun NbChip(text: String, fill: Color) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(fill)
            .border(3.dp, Ink, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun NbIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fill: Color,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    val s = if (big) 56.dp else 42.dp
    Box(
        Modifier
            .size(s)
            .clip(CircleShape)
            .background(fill)
            .border(if (big) 4.dp else 3.dp, Ink, CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(if (big) 30.dp else 22.dp))
    }
}

@Composable
private fun NbTextButton(text: String, fill: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(fill)
            .border(3.dp, Ink, RoundedCornerShape(14.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(text, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun ModeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) Mint else Color.White)
            .border(if (selected) 3.dp else 2.dp, Ink, RoundedCornerShape(11.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

private fun clockMS(sec: Int): String =
    String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60).fa()

private fun clockHMS(sec: Int): String =
    String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60).fa()
