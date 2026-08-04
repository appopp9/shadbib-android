@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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
import ir.shadbib.app.core.NavBus
import ir.shadbib.app.core.fa
import ir.shadbib.app.player.AmbientMixer
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
private val Sky = Color(0xFF7DD3FC)
private val Sand = Color(0xFFE8D5A3)

// ---------------------------------------------------------------- هندسهٔ صحنه
// همه کسری از عرض/ارتفاع صفحه‌اند — روی هر سایزی یکسان درمی‌آید.
private const val BG_ZOOM = 1.20f
private val BACK_SEATS = listOf(0.24f, 0.50f, 0.76f)
private const val BACK_BASELINE = 0.800f

// بوم اسپرایت‌ها از ۴۲۰×۵۶۰ به ۴۸۰×۴۸۰ عوض شد؛ این دو عدد طوری دوباره
// حساب شده‌اند که اندازهٔ دیده‌شدهٔ شخصیت‌ها روی صفحه دقیقاً همان قبلی بماند.
private const val BACK_CANVAS_H = 0.2323f
private const val FRONT_CANVAS_H = 0.3223f

private const val BACK_DESK_TOP = 0.760f
private const val BACK_DESK_H = 0.028f
private const val FRONT_BASELINE = 0.995f
private const val FRONT_DESK_TOP = 0.939f

// ---------------------------------------------------------------- ساعت دیواری
// مختصات ساعت روی تصویر پس‌زمینه (۷۶۸×۱۳۷۶)، کسری از ابعاد خود تصویر.
private const val BG_W = 768f
private const val BG_H = 1376f
private const val CLOCK_CX = 0.4993f
private const val CLOCK_CY = 0.4978f
private const val CLOCK_R = 0.0749f
private val ClockFaceDay = Color(0xFFF7E9CD)
private val ClockFaceNight = Color(0xFFDCC5A0)

@Composable
fun StudyRoomScreen(vm: StudyRoomViewModel = viewModel()) {
    val context = LocalContext.current
    remember(context) { RoomPrefs.init(context); true }

    val snap by vm.snapshot.collectAsState()
    val stats by vm.stats.collectAsState()
    val myChar by vm.character.collectAsState()
    val myState by vm.myState.collectAsState()
    val err by vm.error.collectAsState()
    val pomo by Pomodoro.state.collectAsState()

    val goal by RoomPrefs.goal.collectAsState()
    val owned by RoomPrefs.owned.collectAsState()
    val activeItems by RoomPrefs.active.collectAsState()
    val spent by RoomPrefs.spent.collectAsState()
    val coins = (stats.totalMinutes - spent).coerceAtLeast(0)

    // "" | "char" | "sound" | "shop" | "cheer"
    var overlay by remember { mutableStateOf("") }
    var pendingMinutes by remember { mutableStateOf(0) }
    var cheerTarget by remember { mutableStateOf<RoomOccupant?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    // اگر از جایی مانند میانبر «همه» در کارت برترین‌های صفحهٔ اصلی وارد شدیم،
    // همان شیت خواسته‌شده (مثلاً «بیشترین مطالعه») بلافاصله بالا می‌آید.
    val roomSheetReq by NavBus.openRoomSheet.collectAsState()
    LaunchedEffect(roomSheetReq) {
        val s = roomSheetReq
        if (s != null) {
            NavBus.consumeRoomSheet()
            overlay = s
        }
    }

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

    // چرخش دیالوگ — هر چند ثانیه جملهٔ همه عوض می‌شود
    var talkTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(9000)
            talkTick += 1
        }
    }

    // ایموجی‌های شناور تشویق که از سرور رسیده‌اند
    var floaters by remember { mutableStateOf<List<FloatItem>>(emptyList()) }
    var floatSeq by remember { mutableLongStateOf(0L) }
    LaunchedEffect(snap.cheers) {
        val incoming = snap.cheers
        if (incoming.isNotEmpty()) {
            val add = ArrayList<FloatItem>(incoming.size)
            incoming.forEach { c ->
                floatSeq += 1
                add.add(FloatItem(floatSeq, c.emoji, 0.30f + Random.nextFloat() * 0.40f))
            }
            floaters = floaters + add
            toast = incoming.first().from + " برات فرستاد " + incoming.first().emoji
        }
    }

    // کانفتی پایان هر پومودورو
    var confettiId by remember { mutableIntStateOf(0) }
    var lastCompleted by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pomo.completedWork) {
        if (lastCompleted >= 0 && pomo.completedWork > lastCompleted) {
            confettiId += 1
            toast = "یک پومودورو تمام شد! 🎉"
        }
        lastCompleted = pomo.completedWork
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

        // همان تبدیلی که ContentScale.Crop با لنگر BottomCenter بعلاوهٔ زوم روی
        // پس‌زمینه می‌زند — تا قاب‌ها همیشه دقیقاً روی دیوار بنشینند.
        val bgS = max(w.value / BG_W, h.value / BG_H) * BG_ZOOM
        val bgDW = BG_W * bgS
        val bgDH = BG_H * bgS
        val clockCx = (w.value - bgDW) / 2f + bgDW * CLOCK_CX
        val clockCy = (h.value - bgDH) + bgDH * CLOCK_CY
        val clockR = bgDW * CLOCK_R

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

        // ---------------- لایه ۲: عقربه‌های ساعت واقعی ----------------
        WallClock(nowMs = nowMs, night = night)

        // ---------------- لایه ۳: قاب‌های دیواری ----------------
        val frameW = clockR * 2.45f
        val frameH = clockR * 1.85f
        val frameY = clockCy - frameH / 2f
        val gap = clockR * 3.0f

        // پومودورو — چپ ساعت
        WallFrame(
            xDp = clockCx - gap - frameW / 2f, yDp = frameY, wDp = frameW, hDp = frameH,
            fill = if (pomo.running) Mint else Cream,
            onClick = {
                if (pomo.running) Pomodoro.pause() else Pomodoro.start(context)
                vm.pulse()
            },
            onLongClick = {
                Pomodoro.reset()
                vm.pulse()
                toast = "پومودورو صفر شد"
            },
        ) {
            FrameBody(
                title = "پومودورو",
                value = clockMS(pomo.remainingSec),
                running = pomo.running,
            )
        }

        // کرنومتر — راست ساعت
        WallFrame(
            xDp = clockCx + gap - frameW / 2f, yDp = frameY, wDp = frameW, hDp = frameH,
            fill = if (chronoRunning) Mint else Cream,
            onClick = {
                if (Chrono.running) Chrono.pause() else Chrono.start()
                chronoRunning = Chrono.running
                vm.pulse()
            },
            onLongClick = {
                Chrono.reset()
                chronoRunning = Chrono.running
                vm.pulse()
                toast = "کرنومتر صفر شد"
            },
        ) {
            FrameBody(
                title = "کرنومتر",
                value = clockHMS(chronoSec),
                running = chronoRunning,
            )
        }

        // قاب‌های کوچک ردیف بالا
        val smW = clockR * 2.15f
        val smH = clockR * 1.65f
        val smY = frameY - smH - clockR * 0.5f
        val centers = listOf(w.value * 0.20f, w.value * 0.50f, w.value * 0.80f)

        WallFrame(centers[0] - smW / 2f, smY, smW, smH, fill = Sand, onClick = { overlay = "top" }) {
            TopBoardBody(stats.top)
        }
        WallFrame(centers[1] - smW / 2f, smY, smW, smH, fill = Cream) {
            WeeklyBody(stats.weekly)
        }
        WallFrame(centers[2] - smW / 2f, smY, smW, smH, fill = Sky) {
            StreakBody(streak = stats.streak, totalMinutes = stats.totalMinutes)
        }

        // ---------------- لایه ۴: همسایه‌ها (ردیف عقب) ----------------
        val others = snap.others.take(BACK_SEATS.size)
        others.forEachIndexed { i, occ ->
            Seat(
                seatId = "back" + i,
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
                onClick = {
                    cheerTarget = occ
                    overlay = "cheer"
                },
            )
            SpeechBubble(
                text = RoomDialog.line(occ.state, talkTick + i + 1),
                cx = BACK_SEATS[i],
                bottomY = BACK_BASELINE - BACK_CANVAS_H * RoomChars.BODY_FRACTION,
                parentW = w,
                parentH = h,
                seed = i + 1,
                small = true,
            )
        }

        // ---------------- لایه ۵: میز عقب ----------------
        Desk(0.04f, 0.96f, BACK_DESK_TOP, BACK_DESK_H, w, h, radius = 9.dp, stroke = 3.dp)

        others.forEachIndexed { i, occ ->
            NameTag(
                text = occ.username,
                cx = BACK_SEATS[i],
                y = BACK_DESK_TOP + BACK_DESK_H + 0.006f,
                parentW = w,
                parentH = h,
            )
        }

        // ---------------- لایه ۶: خود کاربر ----------------
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
            onClick = null,
        )
        SpeechBubble(
            text = RoomDialog.line(myState, talkTick),
            cx = 0.50f,
            bottomY = FRONT_BASELINE - FRONT_CANVAS_H * RoomChars.BODY_FRACTION,
            parentW = w,
            parentH = h,
            seed = 0,
            small = false,
        )

        // ---------------- لایه ۷: میز جلو و وسایل روی میز ----------------
        Desk(-0.03f, 1.03f, FRONT_DESK_TOP, 1.10f - FRONT_DESK_TOP, w, h, radius = 14.dp, stroke = 4.dp)
        DeskItems(owned, activeItems)

        // ---------------- لایه ۸: تشویق‌های شناور و کانفتی ----------------
        floaters.forEach { f ->
            key(f.id) {
                FloatingEmoji(
                    emoji = f.emoji,
                    xDp = w * f.xFrac,
                    baseY = h * 0.58f,
                    onDone = { floaters = floaters.filter { it.id != f.id } },
                )
            }
        }
        if (confettiId > 0) {
            key(confettiId) { Confetti() }
        }

        // ---------------- لایه ۹: پنل کوچک بالای صفحه ----------------
        // تایمرها رفتند روی دیوار، پس این باکس دیگر فقط چند دکمه دارد و
        // می‌تواند جمع‌وجور وسط بالا بنشیند.
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NbCard(fill = Cream) {
                Row(
                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GoalRing(
                        done = stats.todayMinutes,
                        goal = goal,
                        onClick = { RoomPrefs.cycleGoal() },
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier.clickable(
                            remember { MutableInteractionSource() },
                            indication = null,
                        ) { overlay = "roster" }
                    ) {
                        NbChip(text = snap.online.fa() + " 👥", fill = Mint)
                    }
                    Spacer(Modifier.width(6.dp))
                    NbIconButton(
                        icon = if (night) Icons.Rounded.Bedtime else Icons.Rounded.WbSunny,
                        fill = Color.White,
                        box = 34.dp,
                    ) { night = !night }
                    Spacer(Modifier.width(5.dp))
                    NbIconButton(icon = Icons.Rounded.Face, fill = Coral, box = 34.dp) {
                        overlay = "char"
                    }
                    Spacer(Modifier.width(5.dp))
                    NbIconButton(icon = Icons.Rounded.MusicNote, fill = Sky, box = 34.dp) {
                        overlay = "sound"
                    }
                    Spacer(Modifier.width(5.dp))
                    NbIconButton(icon = Icons.Rounded.ShoppingCart, fill = Sand, box = 34.dp) {
                        overlay = "shop"
                    }
                    Spacer(Modifier.width(5.dp))
                    NbIconButton(icon = Icons.Rounded.Check, fill = Mint, box = 34.dp) {
                        val minutes = pomo.completedWork * pomo.config.workMin + chronoSec / 60
                        if (minutes < 1) {
                            toast = "زمانی برای ثبت نیست"
                        } else {
                            pendingMinutes = minutes
                            overlay = "course"
                        }
                    }
                }
            }

            if (err != null) {
                Spacer(Modifier.height(6.dp))
                NbChip(text = err ?: "", fill = Color(0xFFFCA5A5))
            }
            if (toast != null) {
                Spacer(Modifier.height(6.dp))
                NbChip(text = toast ?: "", fill = Cream)
                LaunchedEffect(toast) {
                    delay(2600)
                    toast = null
                }
            }
        }

        // ---------------- لایه ۱۰: پنجره‌ها ----------------
        val sheetOverlay = overlay == "shop" || overlay == "roster" ||
            overlay == "top" || overlay == "course"
        if (overlay.isNotEmpty() && !sheetOverlay) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Ink.copy(alpha = 0.62f))
                    .clickable(remember { MutableInteractionSource() }, indication = null) {
                        overlay = ""
                        cheerTarget = null
                    },
            )
        }

        when (overlay) {
            "char" -> NbCard(
                fill = Cream,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("شخصیتت رو انتخاب کن", fontWeight = FontWeight.Black, color = Ink, fontSize = 18.sp)
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
                                        overlay = ""
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

            "sound" -> NbCard(
                fill = Cream,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.92f),
            ) {
                val soundStates by AmbientMixer.states.collectAsState()
                Column(Modifier.padding(14.dp)) {
                    Text("صدای محیط", fontWeight = FontWeight.Black, color = Ink, fontSize = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    AmbientMixer.sounds.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { s ->
                                val on = soundStates[s.key]?.active == true
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (on) Mint else Color.White)
                                        .border(if (on) 3.dp else 2.dp, Ink, RoundedCornerShape(12.dp))
                                        .clickable { AmbientMixer.toggle(context, s.key) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        s.emoji + "  " + s.label,
                                        color = Ink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                    )
                                }
                            }
                            // پر کردن جای خالی ردیف آخر تا ستون‌ها کشیده نشوند
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    NbTextButton(text = "خاموش کردن همه", fill = Coral) { AmbientMixer.stopAll() }
                }
            }

            "shop" -> ShopSheet(
                totalMinutes = stats.totalMinutes,
                onDismiss = { overlay = "" },
                onToast = { t -> toast = t },
            )

            "roster" -> OccupantSheet(
                onUser = { u -> overlay = ""; NavBus.requestUser(u) },
                onDismiss = { overlay = "" },
            )

            "top" -> LeaderboardSheet(
                onUser = { u -> overlay = ""; NavBus.requestUser(u) },
                onDismiss = { overlay = "" },
            )

            "course" -> CoursePickerSheet(
                minutes = pendingMinutes,
                onPick = { cid ->
                    val m = pendingMinutes
                    overlay = ""
                    vm.logMinutes(m, cid) { e ->
                        toast = e ?: (m.fa() + " دقیقه ثبت شد ✅")
                        if (e == null) {
                            Pomodoro.reset()
                            Chrono.reset()
                            chronoRunning = false
                        }
                    }
                },
                onDismiss = { overlay = "" },
            )

            "cheer" -> {
                val target = cheerTarget
                if (target != null) {
                    NbCard(
                        fill = Cream,
                        modifier = Modifier.align(Alignment.Center).fillMaxWidth(0.86f),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "تشویق " + target.username,
                                fontWeight = FontWeight.Black,
                                color = Ink,
                                fontSize = 16.sp,
                            )
                            Text(
                                "امروز " + target.minutesToday.fa() + " دقیقه",
                                color = Ink,
                                fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StudyRoomViewModel.CHEER_EMOJIS.forEach { e ->
                                    Box(
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(3.dp, Ink, CircleShape)
                                            .clickable {
                                                vm.cheer(target.userId, e)
                                                overlay = ""
                                                cheerTarget = null
                                                toast = "فرستاده شد " + e
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(e, fontSize = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================================================================== اجزا

private data class FloatItem(val id: Long, val emoji: String, val xFrac: Float)

/**
 * یک قاب عکس روی دیوار، با سایهٔ سخت نئوبروتالیستی.
 * مختصات برحسب dp خام‌اند تا بتوانیم دقیقاً کنار ساعت دیواری بچینیمشان.
 */
@Composable
private fun WallFrame(
    xDp: Float,
    yDp: Float,
    wDp: Float,
    hDp: Float,
    fill: Color = Cream,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }

    Box(
        Modifier
            .offset(x = (xDp + 4f).dp, y = (yDp + 5f).dp)
            .size(wDp.dp, hDp.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Ink.copy(alpha = 0.5f))
    )

    var m: Modifier = Modifier
        .offset(x = xDp.dp, y = yDp.dp)
        .size(wDp.dp, hDp.dp)
        .clip(RoundedCornerShape(10.dp))
        .background(fill)
        .border(3.dp, Ink, RoundedCornerShape(10.dp))
    if (onClick != null) {
        m = m.combinedClickable(
            interactionSource = source,
            indication = null,
            onLongClick = onLongClick,
            onClick = onClick,
        )
    }
    Box(m, contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun FrameBody(title: String, value: String, running: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Icon(
            if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Ink,
            modifier = Modifier.size(13.dp),
        )
    }
}

@Composable
private fun TopBoardBody(top: List<RoomTopEntry>) {
    Column(
        Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🏆 امروز", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        if (top.isEmpty()) {
            Text("هنوز کسی نخونده", color = Ink, fontSize = 8.sp, maxLines = 1)
        } else {
            top.take(3).forEachIndexed { i, e ->
                Text(
                    (i + 1).fa() + ". " + e.username + " " + e.minutes.fa() + "د",
                    color = Ink,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun WeeklyBody(weekly: List<Int>) {
    Column(
        Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📈 هفته", color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Canvas(Modifier.fillMaxWidth().height(26.dp).padding(top = 3.dp)) {
            val n = weekly.size
            if (n == 0) return@Canvas
            val peak = (weekly.maxOrNull() ?: 0).coerceAtLeast(1).toFloat()
            val slot = size.width / n
            val bw = slot * 0.6f
            for (i in 0 until n) {
                val v = weekly[i].toFloat() / peak
                val bh = (size.height * v).coerceAtLeast(size.height * 0.06f)
                drawRect(
                    color = if (i == n - 1) Coral else Mint,
                    topLeft = Offset(i * slot + (slot - bw) / 2f, size.height - bh),
                    size = Size(bw, bh),
                )
            }
        }
        Text(
            (weekly.sum()).fa() + " دقیقه",
            color = Ink,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun StreakBody(streak: Int, totalMinutes: Int) {
    val badge = when {
        totalMinutes >= 6000 -> "💎 استاد"
        totalMinutes >= 3000 -> "🥇 طلایی"
        totalMinutes >= 1200 -> "🥈 نقره‌ای"
        totalMinutes >= 300 -> "🥉 برنزی"
        else -> "🌱 تازه‌کار"
    }
    Column(
        Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🔥 " + streak.fa(), color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text("روز پشت‌سرهم", color = Ink, fontSize = 8.sp, maxLines = 1)
        Text(badge, color = Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** حلقهٔ هدف روزانه. ضربه بزنی، هدف به گزینهٔ بعدی می‌رود. */
@Composable
private fun GoalRing(done: Int, goal: Int, onClick: () -> Unit) {
    val frac = if (goal <= 0) 0f else (done.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    Box(
        Modifier
            .size(34.dp)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = size.minDimension * 0.16f
            val inset = sw / 2f
            drawArc(
                color = Color.White,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(width = sw),
            )
            drawArc(
                color = if (frac >= 1f) Mint else Coral,
                startAngle = -90f,
                sweepAngle = 360f * frac,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - sw, size.height - sw),
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Text(
            (frac * 100f).toInt().fa(),
            color = Ink,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            maxLines = 1,
        )
    }
}

/** حباب دیالوگ — چند ثانیه پیدا، چند ثانیه غیب؛ تا صحنه شلوغ نشود. */
@Composable
private fun SpeechBubble(
    text: String,
    cx: Float,
    bottomY: Float,
    parentW: Dp,
    parentH: Dp,
    seed: Int,
    small: Boolean,
) {
    var visible by remember(seed) { mutableStateOf(false) }
    LaunchedEffect(seed) {
        delay(600L + seed * 1400L)
        while (true) {
            visible = true
            delay(5200)
            visible = false
            delay(4800)
        }
    }
    if (!visible) return

    val bw = if (small) 96.dp else 120.dp
    val bh = if (small) 22.dp else 26.dp
    Box(
        Modifier
            .offset(x = parentW * cx - bw / 2, y = parentH * bottomY - bh - 4.dp)
            .width(bw)
            .height(bh)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .border(2.dp, Ink, RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = Ink,
            fontSize = if (small) 9.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/** ایموجی تشویق که بالا می‌رود و محو می‌شود. */
@Composable
private fun FloatingEmoji(emoji: String, xDp: Dp, baseY: Dp, onDone: () -> Unit) {
    var p by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val steps = 34
        for (i in 1..steps) {
            p = i / steps.toFloat()
            delay(48)
        }
        onDone()
    }
    Text(
        emoji,
        fontSize = 30.sp,
        modifier = Modifier
            .offset(x = xDp, y = baseY - 110.dp * p)
            .graphicsLayer { alpha = 1f - p * p },
    )
}

/** جشن پایان پومودورو. */
@Composable
private fun Confetti() {
    val pieces = remember {
        List(30) {
            Triple(Random.nextFloat(), Random.nextFloat() * 0.45f, Random.nextInt(4))
        }
    }
    var p by remember { mutableFloatStateOf(0f) }
    var alive by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val steps = 44
        for (i in 1..steps) {
            p = i / steps.toFloat()
            delay(50)
        }
        alive = false
    }
    if (!alive) return

    val colors = listOf(Mint, Coral, Sky, Color(0xFFFDE68A))
    Canvas(Modifier.fillMaxSize()) {
        pieces.forEach { piece ->
            val start = piece.second
            val t = ((p - start) / (1f - start)).coerceIn(0f, 1f)
            if (t > 0f) {
                drawRect(
                    color = colors[piece.third],
                    topLeft = Offset(piece.first * size.width, t * (size.height + 60f) - 60f),
                    size = Size(size.width * 0.028f, size.width * 0.042f),
                )
            }
        }
    }
}

/** وسایل روی میز جلو — با سکهٔ مطالعه باز می‌شوند. */
@Composable
private fun DeskItems(owned: Set<String>, active: Map<String, String>) {
    if (owned.isEmpty()) return
    val lamp = RoomPrefs.activeOf("lamp")
    val mug = RoomPrefs.activeOf("mug")
    val plant = RoomPrefs.activeOf("plant")
    val clock = RoomPrefs.activeOf("clock")
    Canvas(Modifier.fillMaxSize()) {
        val deskY = size.height * FRONT_DESK_TOP + size.height * 0.004f
        val u = size.width * 0.052f
        val sw = size.width * 0.006f
        when (lamp) {
            "lamp" -> drawLamp(size.width * 0.13f, deskY, u, sw, Coral)
            "lamp_neon" -> drawLamp(size.width * 0.13f, deskY, u, sw, Mint)
        }
        when (mug) {
            "mug_coffee" -> drawMug(size.width * 0.30f, deskY, u, sw, Color.White, Color(0xFF7B4A2B), false)
            "mug_tea" -> drawMug(size.width * 0.30f, deskY, u, sw, Cream, Color(0xFFC97B26), true)
            "mug_matcha" -> drawMug(size.width * 0.30f, deskY, u, sw, Color.White, Color(0xFF6DBE45), false)
            "mug_cocoa" -> drawMug(size.width * 0.30f, deskY, u, sw, Coral, Color(0xFF4A2C1A), true)
        }
        if (clock != null) drawDeskClock(size.width * 0.70f, deskY, u, sw, clock)
        if (plant != null) drawPlantVariant(size.width * 0.87f, deskY, u, sw, plant)
    }
}

/** ماگ یا استکان — بدنه، مایع و دسته. */
private fun DrawScope.drawMug(
    cx: Float,
    baseY: Float,
    u: Float,
    sw: Float,
    body: Color,
    liquid: Color,
    tall: Boolean,
) {
    val h = if (tall) u * 1.15f else u * 0.85f
    val wBody = if (tall) u * 0.70f else u * 0.86f
    nbRect(cx - wBody / 2f, baseY - h, wBody, h, body, u * 0.14f, sw)
    nbRect(cx - wBody / 2f + sw, baseY - h + sw * 1.4f, wBody - sw * 2f, u * 0.20f, liquid, u * 0.08f, sw * 0.7f)
    drawArc(
        color = Ink,
        startAngle = -70f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx + wBody / 2f - u * 0.16f, baseY - h * 0.78f),
        size = Size(u * 0.42f, u * 0.46f),
        style = Stroke(width = sw),
    )
}

/** گلدان‌های مختلف. */
private fun DrawScope.drawPlantVariant(cx: Float, baseY: Float, u: Float, sw: Float, kind: String) {
    val potW = u * 0.95f
    val potH = u * 0.62f
    nbRect(cx - potW / 2f, baseY - potH, potW, potH, Coral, u * 0.10f, sw)
    val top = baseY - potH
    when (kind) {
        "plant_cactus" -> {
            nbRect(cx - u * 0.20f, top - u * 1.25f, u * 0.40f, u * 1.30f, Mint, u * 0.18f, sw)
            nbRect(cx + u * 0.16f, top - u * 0.95f, u * 0.34f, u * 0.22f, Mint, u * 0.10f, sw)
            nbRect(cx - u * 0.50f, top - u * 0.72f, u * 0.34f, u * 0.20f, Mint, u * 0.10f, sw)
        }
        "plant_monstera" -> {
            nbCircle(cx - u * 0.34f, top - u * 0.86f, u * 0.40f, Mint, sw)
            nbCircle(cx + u * 0.34f, top - u * 0.92f, u * 0.40f, Mint, sw)
            nbCircle(cx, top - u * 1.34f, u * 0.44f, Mint, sw)
        }
        "plant_bonsai" -> {
            nbRect(cx - u * 0.08f, top - u * 0.80f, u * 0.16f, u * 0.85f, Color(0xFF7B4A2B), u * 0.05f, sw)
            nbCircle(cx - u * 0.30f, top - u * 1.00f, u * 0.34f, Mint, sw)
            nbCircle(cx + u * 0.32f, top - u * 1.12f, u * 0.36f, Mint, sw)
            nbCircle(cx, top - u * 1.40f, u * 0.32f, Mint, sw)
        }
        else -> {
            nbCircle(cx - u * 0.26f, top - u * 0.62f, u * 0.34f, Mint, sw)
            nbCircle(cx + u * 0.26f, top - u * 0.62f, u * 0.34f, Mint, sw)
            nbCircle(cx, top - u * 1.02f, u * 0.36f, Mint, sw)
        }
    }
}

/** ساعت‌های رومیزی. */
private fun DrawScope.drawDeskClock(cx: Float, baseY: Float, u: Float, sw: Float, kind: String) {
    when (kind) {
        "clock_hourglass" -> {
            nbRect(cx - u * 0.44f, baseY - u * 0.16f, u * 0.88f, u * 0.16f, Sand, u * 0.05f, sw)
            nbRect(cx - u * 0.44f, baseY - u * 1.34f, u * 0.88f, u * 0.16f, Sand, u * 0.05f, sw)
            val p = androidx.compose.ui.graphics.Path()
            p.moveTo(cx - u * 0.34f, baseY - u * 1.18f)
            p.lineTo(cx + u * 0.34f, baseY - u * 1.18f)
            p.lineTo(cx + u * 0.04f, baseY - u * 0.68f)
            p.lineTo(cx + u * 0.34f, baseY - u * 0.16f)
            p.lineTo(cx - u * 0.34f, baseY - u * 0.16f)
            p.lineTo(cx - u * 0.04f, baseY - u * 0.68f)
            p.close()
            drawPath(p, color = Cream)
            drawPath(p, color = Ink, style = Stroke(width = sw))
        }
        "clock_digital" -> {
            nbRect(cx - u * 0.60f, baseY - u * 0.70f, u * 1.20f, u * 0.70f, Ink, u * 0.10f, sw)
            nbRect(cx - u * 0.44f, baseY - u * 0.56f, u * 0.88f, u * 0.40f, Mint, u * 0.06f, sw * 0.6f)
        }
        "clock_pendulum" -> {
            nbRect(cx - u * 0.40f, baseY - u * 1.70f, u * 0.80f, u * 1.70f, Color(0xFF7B4A2B), u * 0.10f, sw)
            nbCircle(cx, baseY - u * 1.30f, u * 0.26f, Cream, sw)
            nbRect(cx - u * 0.04f, baseY - u * 1.00f, u * 0.08f, u * 0.55f, Sand, u * 0.03f, sw * 0.7f)
            nbCircle(cx, baseY - u * 0.38f, u * 0.16f, Sand, sw)
        }
        else -> {
            nbCircle(cx, baseY - u * 0.62f, u * 0.50f, Cream, sw)
            nbRect(cx - u * 0.30f, baseY - u * 0.14f, u * 0.60f, u * 0.14f, Ink, u * 0.05f, sw * 0.7f)
            drawLine(
                color = Ink,
                start = Offset(cx, baseY - u * 0.62f),
                end = Offset(cx, baseY - u * 0.94f),
                strokeWidth = sw,
            )
            drawLine(
                color = Ink,
                start = Offset(cx, baseY - u * 0.62f),
                end = Offset(cx + u * 0.24f, baseY - u * 0.62f),
                strokeWidth = sw,
            )
        }
    }
}

private fun DrawScope.nbRect(x: Float, y: Float, w: Float, h: Float, fill: Color, r: Float, sw: Float) {
    drawRoundRect(
        color = fill,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r),
    )
    drawRoundRect(
        color = Ink,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r),
        style = Stroke(width = sw),
    )
}

private fun DrawScope.nbCircle(cx: Float, cy: Float, r: Float, fill: Color, sw: Float) {
    drawCircle(color = fill, radius = r, center = Offset(cx, cy))
    drawCircle(color = Ink, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
}

private fun DrawScope.drawLamp(cx: Float, baseY: Float, u: Float, sw: Float, shade: Color) {
    nbRect(cx - u * 0.8f, baseY - u * 0.32f, u * 1.6f, u * 0.32f, Ink.copy(alpha = 0.85f), u * 0.12f, sw)
    nbRect(cx - u * 0.11f, baseY - u * 1.8f, u * 0.22f, u * 1.5f, Sand, u * 0.06f, sw)
    nbRect(cx - u * 0.75f, baseY - u * 2.7f, u * 1.5f, u * 0.9f, shade, u * 0.2f, sw)
}

private fun DrawScope.drawCoffee(cx: Float, baseY: Float, u: Float, sw: Float) {
    nbRect(cx - u * 0.62f, baseY - u * 0.16f, u * 1.24f, u * 0.16f, Cream, u * 0.07f, sw)
    nbRect(cx - u * 0.45f, baseY - u * 0.9f, u * 0.9f, u * 0.74f, Color.White, u * 0.14f, sw)
    drawCircle(
        color = Ink,
        radius = u * 0.24f,
        center = Offset(cx + u * 0.6f, baseY - u * 0.52f),
        style = Stroke(width = sw),
    )
    nbRect(cx - u * 0.38f, baseY - u * 0.86f, u * 0.76f, u * 0.16f, Color(0xFF7A4A2B), u * 0.06f, sw)
}

private fun DrawScope.drawPlant(cx: Float, baseY: Float, u: Float, sw: Float) {
    nbRect(cx - u * 0.42f, baseY - u * 0.72f, u * 0.84f, u * 0.72f, Coral, u * 0.12f, sw)
    nbCircle(cx, baseY - u * 1.5f, u * 0.42f, Mint, sw)
    nbCircle(cx - u * 0.42f, baseY - u * 1.12f, u * 0.32f, Mint, sw)
    nbCircle(cx + u * 0.42f, baseY - u * 1.12f, u * 0.32f, Mint, sw)
}

/**
 * عقربه‌های ساعت دیواری — زمان واقعی دستگاه.
 *
 * ساعت در خود تصویر پس‌زمینه نقاشی شده و عقربه‌هایش ثابت‌اند؛ پس اول صفحهٔ
 * ساعت را با همان رنگ کرمی پوشانده، مایل‌ها را دوباره می‌کشیم و عقربه‌های
 * درست را رویش می‌اندازیم.
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
        val s = max(sw / BG_W, sh / BG_H) * BG_ZOOM
        val dw = BG_W * s
        val dh = BG_H * s
        val left = (sw - dw) / 2f
        val top = sh - dh

        val cx = left + dw * CLOCK_CX
        val cy = top + dh * CLOCK_CY
        val rOuter = dw * CLOCK_R
        val rFace = rOuter * 0.80f

        if (cy + rOuter < 0f || cy - rOuter > sh) return@Canvas

        drawCircle(color = face, radius = rFace, center = Offset(cx, cy))

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

        val ha = Math.toRadians((hh + mm / 60.0) * 30.0 - 90.0)
        drawLine(
            color = Ink,
            start = Offset(cx, cy),
            end = Offset(cx + (rFace * 0.46f * cos(ha)).toFloat(), cy + (rFace * 0.46f * sin(ha)).toFloat()),
            strokeWidth = rFace * 0.13f,
            cap = StrokeCap.Round,
        )
        val ma = Math.toRadians((mm + ss / 60.0) * 6.0 - 90.0)
        drawLine(
            color = Ink,
            start = Offset(cx, cy),
            end = Offset(cx + (rFace * 0.70f * cos(ma)).toFloat(), cy + (rFace * 0.70f * sin(ma)).toFloat()),
            strokeWidth = rFace * 0.09f,
            cap = StrokeCap.Round,
        )
        val sa = Math.toRadians(ss * 6.0 - 90.0)
        drawLine(
            color = Coral,
            start = Offset(cx, cy),
            end = Offset(cx + (rFace * 0.78f * cos(sa)).toFloat(), cy + (rFace * 0.78f * sin(sa)).toFloat()),
            strokeWidth = rFace * 0.05f,
            cap = StrokeCap.Round,
        )
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
    onClick: (() -> Unit)? = null,
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
    var m: Modifier = Modifier
        .offset(x = parentW * cx - wDp / 2, y = parentH * baseline - hDp)
        .size(wDp, hDp)
    if (onClick != null) {
        m = m.clickable(remember { MutableInteractionSource() }, indication = null) { onClick() }
    }
    Box(m) {
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
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun NbIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    fill: Color,
    box: Dp = 42.dp,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(box)
            .clip(CircleShape)
            .background(fill)
            .border(3.dp, Ink, CircleShape)
            .clickable(remember { MutableInteractionSource() }, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(box * 0.52f))
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
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(text, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

private fun clockMS(sec: Int): String =
    String.format(Locale.US, "%02d:%02d", sec / 60, sec % 60).fa()

private fun clockHMS(sec: Int): String =
    String.format(Locale.US, "%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60).fa()
