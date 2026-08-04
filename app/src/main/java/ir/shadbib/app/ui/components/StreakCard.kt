package ir.shadbib.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.shadbib.app.core.Jalali
import ir.shadbib.app.core.fa
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Flame1 = Color(0xFFFF7043)
private val Flame2 = Color(0xFFFFC107)
private val Ash = Color(0xFF3A2A20)

private val MILESTONES = listOf(3, 7, 14, 30, 60, 100, 200, 365)

private data class DayDot(val label: String, val active: Boolean, val isToday: Boolean)

/** Big animated streak card. Shows the flame, progress to the next milestone and the last 7 days. */
@Composable
fun StreakCard(
    streak: Int,
    todayMinutes: Int,
    byDate: Map<String, Int> = emptyMap(),
    onClick: () -> Unit = {},
) {
    val next = MILESTONES.firstOrNull { it > streak } ?: (streak + 1)
    val prev = MILESTONES.lastOrNull { it <= streak } ?: 0
    val target = if (next > prev) ((streak - prev).toFloat() / (next - prev)).coerceIn(0f, 1f) else 0f
    val frac by animateFloatAsState(target, tween(900), label = "streakRing")

    val tier = when {
        streak <= 0 -> "شعله خاموشه"
        streak < 3 -> "جرقه"
        streak < 7 -> "شعله"
        streak < 14 -> "آتش"
        streak < 30 -> "کوره"
        streak < 100 -> "آتشفشان"
        else -> "افسانه"
    }
    val emoji = when {
        streak <= 0 -> "🌱"
        streak < 7 -> "🔥"
        streak < 30 -> "🔥"
        streak < 100 -> "🌋"
        else -> "🏆"
    }

    val t = rememberInfiniteTransition(label = "streak")
    val pulse by t.animateFloat(1f, 1.14f, infiniteRepeatable(tween(880), RepeatMode.Reverse), label = "pulse")
    val glow by t.animateFloat(0.18f, 0.52f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "glow")

    val dots = remember(byDate, todayMinutes) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val out = ArrayList<DayDot>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val iso = fmt.format(c.time)
            val label = Jalali.weekDayNames[Jalali.weekDayIndex(c.time)].take(1)
            val active = (byDate[iso] ?: 0) > 0 || (i == 0 && todayMinutes > 0)
            out.add(DayDot(label, active, i == 0))
        }
        out
    }

    val todayDone = todayMinutes > 0 || dots.lastOrNull()?.active == true

    Surface(shape = RoundedCornerShape(22.dp), color = Color.Transparent, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Ash, Color(0xFF120C09))))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(96.dp)) {
                    Box(
                        Modifier
                            .size(78.dp)
                            .background(Flame1.copy(alpha = glow * 0.45f), CircleShape)
                            .scale(pulse),
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        val stroke = 7.dp.toPx()
                        drawArc(
                            Color.White.copy(alpha = 0.10f), -90f, 360f, false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                        drawArc(
                            Brush.sweepGradient(listOf(Flame1, Flame2, Flame1)),
                            -90f, 360f * frac, false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                        )
                    }
                    Text(emoji, fontSize = 34.sp, modifier = Modifier.scale(pulse))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            streak.fa(),
                            color = Color.White,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "روز پشت‌سرهم",
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Surface(shape = CircleShape, color = Flame2.copy(alpha = 0.18f)) {
                        Text(
                            tier,
                            color = Flame2,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (streak >= next) "رکورد تازه!" else "${(next - streak).fa()} روز تا نشان ${next.fa()} روزه",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                dots.forEach { d ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(if (d.isToday) 30.dp else 26.dp)
                                .background(
                                    if (d.active) Flame1.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f),
                                    CircleShape,
                                )
                                .border(
                                    if (d.isToday) 2.dp else 0.dp,
                                    if (d.isToday) Flame2 else Color.Transparent,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (d.active) Text("🔥", fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(d.label, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.07f), modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (todayDone) "امروز ثبت شد — شعله روشن موند ✅" else "امروز هنوز مطالعه‌ای ثبت نشده؛ نذار شعله خاموش شه",
                    color = if (todayDone) Flame2 else Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 8.dp),
                )
            }
        }
    }
}

@Suppress("unused")
private fun unusedDateHelper(): Date = Date()
