package ir.darshub.app.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.darshub.app.core.Api
import ir.darshub.app.core.Jalali
import ir.darshub.app.core.fa
import ir.darshub.app.core.int
import ir.darshub.app.core.objects
import ir.darshub.app.core.strOrNull
import java.util.Calendar

/**
 * \u062a\u0642\u0648\u06cc\u0645 \u0634\u0645\u0633\u06cc \u0645\u0637\u0627\u0644\u0639\u0647 + \u062a\u062d\u0644\u06cc\u0644 \u062f\u0642\u06cc\u0642 (\u0631\u0648\u0632\u060c \u062f\u0631\u0633\u060c \u0633\u0627\u0639\u062a).
 *
 * \u0647\u0631 \u062f\u0648 \u0631\u0648\u06cc \u0647\u0645\u0627\u0646 \u062c\u062f\u0648\u0644 study_sessions \u06a9\u0627\u0631 \u0645\u06cc\u200c\u06a9\u0646\u0646\u062f\u061b \u0647\u06cc\u0686 \u062c\u062f\u0648\u0644 \u062a\u0627\u0632\u0647\u200c\u0627\u06cc \u0644\u0627\u0632\u0645 \u0646\u06cc\u0633\u062a.
 */
@Composable
fun StudyAnalyticsSection() {
    var byDate by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(Unit) {
        runCatching {
            val o = Api.obj(Api.get("study_calendar", "days" to "400"))
            val m = HashMap<String, Int>()
            o.optJSONArray("days")?.objects()?.forEach { d ->
                val date = d.strOrNull("date")
                if (date != null) m[date] = d.int("minutes")
            }
            m
        }.onSuccess { byDate = it }
    }

    JalaliCalendarCard(byDate)
    Spacer(Modifier.height(12.dp))
    AnalyticsCard()
}

// ==================== \u062a\u0642\u0648\u06cc\u0645 \u0634\u0645\u0633\u06cc ====================

private fun isoOf(gy: Int, gm: Int, gd: Int): String {
    val mm = if (gm < 10) "0" + gm else gm.toString()
    val dd = if (gd < 10) "0" + gd else gd.toString()
    return "" + gy + "-" + mm + "-" + dd
}

/** \u0637\u0648\u0644 \u0645\u0627\u0647 \u0634\u0645\u0633\u06cc \u0631\u0627 \u0628\u0627 \u0631\u0641\u062a\u200c\u0648\u0628\u0631\u06af\u0634\u062a \u0645\u06cc\u200c\u0633\u0646\u062c\u062f (\u0628\u0647 API \u062f\u0627\u062e\u0644\u06cc \u062f\u0633\u062a \u0646\u0645\u06cc\u200c\u0632\u0646\u062f). */
private fun daysInJalaliMonth(jy: Int, jm: Int): Int {
    var n = 29
    for (d in 29..31) {
        val g = Jalali.toGregorian(jy, jm, d)
        val back = Jalali.fromGregorian(g[0], g[1], g[2])
        if (back.year == jy && back.month == jm && back.day == d) n = d
    }
    return n
}

private fun weekDayIndexOf(gy: Int, gm: Int, gd: Int): Int {
    val c = Calendar.getInstance()
    c.set(gy, gm - 1, gd, 12, 0, 0)
    return Jalali.weekDayIndex(c.time)
}

@Composable
private fun JalaliCalendarCard(byDate: Map<String, Int>) {
    val today = remember { Jalali.today() }
    var year by remember { mutableStateOf(today.year) }
    var month by remember { mutableStateOf(today.month) }

    val len = daysInJalaliMonth(year, month)
    val firstG = Jalali.toGregorian(year, month, 1)
    val lead = weekDayIndexOf(firstG[0], firstG[1], firstG[2])

    // \u062f\u0642\u06cc\u0642\u0647\u200c\u0647\u0627\u06cc \u0647\u0631 \u0631\u0648\u0632 \u0627\u06cc\u0646 \u0645\u0627\u0647
    val minutesOfDay = IntArray(len + 1)
    for (d in 1..len) {
        val g = Jalali.toGregorian(year, month, d)
        minutesOfDay[d] = byDate[isoOf(g[0], g[1], g[2])] ?: 0
    }
    val monthTotal = minutesOfDay.sum()
    val activeDays = (1..len).count { minutesOfDay[it] > 0 }
    val maxDay = (1..len).maxOfOrNull { minutesOfDay[it] } ?: 0

    // \u0627\u0633\u062a\u0631\u06cc\u06a9: \u0631\u0648\u0632\u0647\u0627\u06cc \u067e\u0634\u062a\u200c\u0633\u0631\u0647\u0645 \u062a\u0627 \u0627\u0645\u0631\u0648\u0632
    val streak = remember(byDate) { computeStreak(byDate) }

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NavBtn("\u203A") {
                if (month == 12) { month = 1; year += 1 } else month += 1
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Jalali.monthName(month) + " " + year.fa(),
                    style = MaterialTheme.typography.titleSmall)
                Text("\uD83D\uDD25 " + streak.fa() + " \u0631\u0648\u0632 \u067e\u0634\u062a\u200c\u0633\u0631\u0647\u0645",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
            NavBtn("\u2039") {
                if (month == 1) { month = 12; year -= 1 } else month -= 1
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth()) {
            Jalali.weekDayNames.forEach { n ->
                Text(n.take(1), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(4.dp))

        var day = 1
        val rows = ((lead + len) + 6) / 7
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    if (cellIndex < lead || day > len) {
                        Box(Modifier.weight(1f).height(40.dp))
                    } else {
                        val d = day
                        val isToday = year == today.year && month == today.month && d == today.day
                        DayCell(d, minutesOfDay[d], maxDay, isToday, Modifier.weight(1f))
                        day += 1
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("\u062c\u0645\u0639 \u0645\u0627\u0647", hm(monthTotal))
            Stat("\u0631\u0648\u0632\u0647\u0627\u06cc \u0641\u0639\u0627\u0644", activeDays.fa())
            Stat("\u0628\u0647\u062a\u0631\u06cc\u0646 \u0631\u0648\u0632", hm(maxDay))
        }
    }
}

private fun computeStreak(byDate: Map<String, Int>): Int {
    val c = Calendar.getInstance()
    var n = 0
    // \u0627\u06af\u0631 \u0627\u0645\u0631\u0648\u0632 \u0647\u0646\u0648\u0632 \u0645\u0637\u0627\u0644\u0639\u0647 \u0646\u0634\u062f\u0647\u060c \u0627\u0632 \u062f\u06cc\u0631\u0648\u0632 \u0645\u06cc\u200c\u0634\u0645\u0627\u0631\u06cc\u0645
    val todayIso = isoOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    if ((byDate[todayIso] ?: 0) <= 0) c.add(Calendar.DAY_OF_MONTH, -1)
    while (true) {
        val iso = isoOf(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        if ((byDate[iso] ?: 0) > 0) { n += 1; c.add(Calendar.DAY_OF_MONTH, -1) } else break
        if (n > 900) break
    }
    return n
}

@Composable
private fun DayCell(day: Int, minutes: Int, maxDay: Int, isToday: Boolean, modifier: Modifier) {
    val frac = if (maxDay > 0) minutes.toFloat() / maxDay.toFloat() else 0f
    val base = MaterialTheme.colorScheme.primary
    val bg = when {
        minutes <= 0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        else -> base.copy(alpha = 0.22f + 0.62f * frac)
    }
    Box(modifier.height(40.dp).padding(2.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(9.dp)).background(bg)
                .then(
                    if (isToday) Modifier.border(2.dp, base, RoundedCornerShape(9.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(day.fa(), style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                if (minutes > 0) {
                    Text(minutes.fa(), fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ==================== \u062a\u062d\u0644\u06cc\u0644 \u062f\u0642\u06cc\u0642 ====================

private data class CourseStat(val name: String, val icon: String, val minutes: Int, val sessions: Int)

@Composable
private fun AnalyticsCard() {
    val ranges = listOf(7 to "\u06f7 \u0631\u0648\u0632", 30 to "\u06f3\u06f0 \u0631\u0648\u0632", 90 to "\u06f3 \u0645\u0627\u0647", 365 to "\u0633\u0627\u0644")
    var days by remember { mutableStateOf(30) }
    var courses by remember { mutableStateOf<List<CourseStat>>(emptyList()) }
    var weekdays by remember { mutableStateOf(List(7) { 0 }) }
    var hours by remember { mutableStateOf(List(24) { 0 }) }
    var total by remember { mutableStateOf(0) }
    var activeDays by remember { mutableStateOf(0) }
    var sessions by remember { mutableStateOf(0) }
    var best by remember { mutableStateOf(0) }

    LaunchedEffect(days) {
        runCatching {
            Api.obj(Api.get("study_by_course", "days" to days.toString()))
        }.onSuccess { o ->
            courses = o.optJSONArray("courses")?.objects()?.map { c ->
                CourseStat(
                    c.strOrNull("name") ?: "\u0628\u062f\u0648\u0646 \u062f\u0631\u0633",
                    c.strOrNull("icon") ?: "\uD83D\uDCD6",
                    c.int("minutes"),
                    c.int("sessions"),
                )
            } ?: emptyList()
            val wd = o.optJSONArray("weekdays")
            if (wd != null) weekdays = (0 until wd.length()).map { wd.optInt(it, 0) }
            val hr = o.optJSONArray("hours")
            if (hr != null) hours = (0 until hr.length()).map { hr.optInt(it, 0) }
            total = o.optInt("total_minutes", 0)
            activeDays = o.optInt("active_days", 0)
            sessions = o.optInt("sessions", 0)
            best = o.optInt("best_session", 0)
        }
    }

    Card {
        Text("\u062a\u062d\u0644\u06cc\u0644 \u0645\u0637\u0627\u0644\u0639\u0647", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ranges.forEach { (d, fa) ->
                val on = d == days
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clickable { days = d },
                ) {
                    Text(fa, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("\u062c\u0645\u0639", hm(total))
            Stat("\u0631\u0648\u0632 \u0641\u0639\u0627\u0644", activeDays.fa())
            Stat("\u062c\u0644\u0633\u0647", sessions.fa())
            Stat("\u0628\u0647\u062a\u0631\u06cc\u0646", hm(best))
        }

        if (activeDays > 0) {
            Spacer(Modifier.height(6.dp))
            Text("\u0645\u06cc\u0627\u0646\u06af\u06cc\u0646 \u0631\u0648\u0632\u0647\u0627\u06cc \u0641\u0639\u0627\u0644: " + hm(total / activeDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // ---- \u0628\u0631 \u0627\u0633\u0627\u0633 \u062f\u0631\u0633 ----
        if (courses.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("\u0628\u0631 \u0627\u0633\u0627\u0633 \u062f\u0631\u0633", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            val top = courses.maxOf { it.minutes }.coerceAtLeast(1)
            courses.take(10).forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(c.icon, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(c.name, style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(84.dp), maxLines = 1)
                    Box(
                        Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    ) {
                        Box(
                            Modifier.fillMaxWidth(c.minutes.toFloat() / top.toFloat()).height(14.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(hm(c.minutes), style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // ---- \u0628\u0631 \u0627\u0633\u0627\u0633 \u0631\u0648\u0632 \u0647\u0641\u062a\u0647 ----
        if (weekdays.size >= 7) {
            Spacer(Modifier.height(14.dp))
            Text("\u0628\u0631 \u0627\u0633\u0627\u0633 \u0631\u0648\u0632 \u0647\u0641\u062a\u0647", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            // MySQL DAYOFWEEK: 1=\u06cc\u06a9\u0634\u0646\u0628\u0647 ... 7=\u0634\u0646\u0628\u0647 \u2192 \u062a\u0631\u062a\u06cc\u0628 \u0634\u0645\u0633\u06cc \u0627\u0632 \u0634\u0646\u0628\u0647
            val order = listOf(6, 0, 1, 2, 3, 4, 5)
            val vals = order.map { weekdays.getOrElse(it) { 0 } }
            val mx = (vals.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(74.dp), verticalAlignment = Alignment.Bottom) {
                vals.forEachIndexed { i, v ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.fillMaxWidth(0.55f)
                                .height((6f + 48f * (v.toFloat() / mx.toFloat())).dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.primary),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(Jalali.weekDayNames[i].take(1), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ---- \u0633\u0627\u0639\u0627\u062a \u0634\u0628\u0627\u0646\u0647\u200c\u0631\u0648\u0632 ----
        if (hours.size >= 24 && hours.any { it > 0 }) {
            Spacer(Modifier.height(14.dp))
            Text("\u0633\u0627\u0639\u0627\u062a \u067e\u0631\u062a\u0645\u0631\u06a9\u0632 \u0634\u0628\u0627\u0646\u0647\u200c\u0631\u0648\u0632", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            val mx = (hours.maxOrNull() ?: 0).coerceAtLeast(1)
            val prim = MaterialTheme.colorScheme.primary
            val empty = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            Canvas(Modifier.fillMaxWidth().height(26.dp)) {
                val w = size.width / 24f
                for (h in 0 until 24) {
                    val v = hours[h]
                    val a = if (v <= 0) 0f else 0.25f + 0.7f * (v.toFloat() / mx.toFloat())
                    drawRect(
                        color = if (v <= 0) empty else prim.copy(alpha = a),
                        topLeft = androidx.compose.ui.geometry.Offset(h * w + 1f, 0f),
                        size = androidx.compose.ui.geometry.Size(w - 2f, size.height),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("\u06f0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u06f1\u06f2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("\u06f2\u06f3", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ==================== \u0627\u062c\u0632\u0627\u06cc \u06a9\u0648\u0686\u06a9 ====================

@Composable
private fun Card(content: @Composable ColumnScopeAlias.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) { ColumnScopeAlias.content() }
    }
}

/** \u0641\u0642\u0637 \u0628\u0631\u0627\u06cc \u0627\u06cc\u0646\u06a9\u0647 \u0628\u062f\u0646\u0647\u0654 \u06a9\u0627\u0631\u062a \u062f\u0631 \u06cc\u06a9 Column \u0642\u0631\u0627\u0631 \u0628\u06af\u06cc\u0631\u062f. */
private object ColumnScopeAlias

@Composable
private fun NavBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun hm(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> h.fa() + "\u0633 " + m.fa() + "\u062f"
        h > 0 -> h.fa() + "\u0633"
        else -> m.fa() + "\u062f"
    }
}
