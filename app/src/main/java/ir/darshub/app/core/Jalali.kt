package ir.darshub.app.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Jalali (Shamsi) calendar conversion — faithful port of jalaali-js. */
object Jalali {

    data class JDate(val year: Int, val month: Int, val day: Int)

    val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    val weekDayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    /** Safe month name — never throws, even for out-of-range values. */
    fun monthName(month: Int): String = monthNames.getOrElse(month - 1) { "" }

    private val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
    )

    // IMPORTANT: jalaali-js uses ~~(a/b) — truncation toward zero, NOT floor.
    // Kotlin's Int division/remainder already truncate toward zero, matching exactly.
    private fun div(a: Int, b: Int) = a / b
    private fun mod(a: Int, b: Int) = a - (a / b) * b

    private fun jalCal(jy: Int): IntArray {
        val bl = breaks.size
        val gy = jy + 621
        var leapJ = -14
        var jp = breaks[0]
        var jump = 0
        for (i in 1 until bl) {
            val jm = breaks[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return intArrayOf(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
                div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): IntArray {
        var j = 4 * jdn + 139361631
        j += div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return intArrayOf(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val r = jalCal(jy)
        return g2d(r[1], 3, r[2]) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): JDate {
        val gy = d2g(jdn)[0]
        var jy = gy - 621
        val r = jalCal(jy)
        val jdn1f = g2d(gy, 3, r[2])
        var k = jdn - jdn1f
        if (k >= 0) {
            if (k <= 185) {
                return JDate(jy, 1 + div(k, 31), mod(k, 31) + 1)
            } else k -= 186
        } else {
            jy -= 1
            k += 179
            if (r[0] == 1) k += 1
        }
        return JDate(jy, 7 + div(k, 30), mod(k, 30) + 1)
    }

    fun fromGregorian(gy: Int, gm: Int, gd: Int): JDate = d2j(g2d(gy, gm, gd))

    fun toGregorian(jy: Int, jm: Int, jd: Int): IntArray = d2g(j2d(jy, jm, jd))

    fun fromDate(date: Date): JDate {
        val c = Calendar.getInstance().apply { time = date }
        return fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    fun today(): JDate = fromDate(Date())

    /** Iranian weekday index: 0=شنبه … 6=جمعه */
    fun weekDayIndex(date: Date): Int {
        val c = Calendar.getInstance().apply { time = date }
        return when (c.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0; Calendar.SUNDAY -> 1; Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3; Calendar.WEDNESDAY -> 4; Calendar.THURSDAY -> 5
            else -> 6
        }
    }
}

/* ---------- Persian formatting helpers ---------- */

private val faDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

fun String.fa(): String = buildString {
    for (ch in this@fa) append(if (ch in '0'..'9') faDigits[ch - '0'] else ch)
}

fun Int.fa(): String = toString().fa()
fun Long.fa(): String = toString().fa()

object Fmt {
    private val mysqlFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val isoFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parseMysql(s: String?): Date? = try {
        if (s.isNullOrBlank()) null else mysqlFmt.parse(s)
    } catch (e: Exception) { null }

    fun parseIso(s: String?): Date? = try {
        if (s.isNullOrBlank()) null else isoFmt.parse(s)
    } catch (e: Exception) { null }

    fun isoOf(date: Date): String = isoFmt.format(date)

    /** "۲ ساعت و ۱۵ دقیقه" */
    fun minutes(total: Int): String {
        val h = total / 60
        val m = total % 60
        return when {
            h > 0 && m > 0 -> "${h.fa()} ساعت و ${m.fa()} دقیقه"
            h > 0 -> "${h.fa()} ساعت"
            else -> "${m.fa()} دقیقه"
        }
    }

    /** Stopwatch "HH:MM:SS" */
    fun clock(totalSec: Long): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s).fa()
    }

    /** "08:17:00" or "08:17" → "۰۸:۱۷" */
    fun hm(time: String?): String {
        if (time.isNullOrBlank()) return "--:--"
        val parts = time.split(":")
        return if (parts.size >= 2) "${parts[0]}:${parts[1]}".fa() else time.fa()
    }

    /** "پنجشنبه ۲ مرداد ۱۴۰۵" — never throws. */
    fun todayFull(): String = runCatching {
        val now = Date()
        val j = Jalali.fromDate(now)
        val wd = Jalali.weekDayNames[Jalali.weekDayIndex(now)]
        "$wd ${j.day.fa()} ${Jalali.monthName(j.month)} ${j.year.fa()}"
    }.getOrDefault("")

    /** "2026-07-24" → "۲ مرداد" — never throws. */
    fun jalaliShort(iso: String?): String = runCatching {
        val d = parseIso(iso) ?: return ""
        val j = Jalali.fromDate(d)
        "${j.day.fa()} ${Jalali.monthName(j.month)}"
    }.getOrDefault("")

    /** "2026-07-24" → "پنجشنبه ۲ مرداد" — never throws. */
    fun jalaliWithDay(iso: String?): String = runCatching {
        val d = parseIso(iso) ?: return ""
        val j = Jalali.fromDate(d)
        "${Jalali.weekDayNames[Jalali.weekDayIndex(d)]} ${j.day.fa()} ${Jalali.monthName(j.month)}"
    }.getOrDefault("")

    /** Relative time for chat/feed — never throws. */
    /** برچسب روز برای جداکننده‌های چت: امروز / دیروز / تاریخ شمسی. */
    fun dayLabel(mysqlDateTime: String?): String = runCatching {
        val datePart = (mysqlDateTime ?: return "").substringBefore(' ')
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val yesterday = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 86400_000L))
        when (datePart) {
            today -> "امروز"
            yesterday -> "دیروز"
            else -> {
                val parts = datePart.split("-").map { it.toInt() }
                val j = Jalali.fromGregorian(parts[0], parts[1], parts[2])
                "${j.day.fa()} ${Jalali.monthName(j.month)}"
            }
        }
    }.getOrDefault("")

    fun relative(mysqlDateTime: String?): String = runCatching {
        val d = parseMysql(mysqlDateTime) ?: return ""
        val diff = (System.currentTimeMillis() - d.time) / 1000
        when {
            diff < 60 -> "همین الان"
            diff < 3600 -> "${(diff / 60).fa()} دقیقه پیش"
            diff < 86400 -> "${(diff / 3600).fa()} ساعت پیش"
            diff < 172800 -> "دیروز"
            else -> {
                val j = Jalali.fromDate(d)
                "${j.day.fa()} ${Jalali.monthName(j.month)}"
            }
        }
    }.getOrDefault("")

    /** Time-of-day for chat bubbles: "۱۴:۰۵" */
    fun timeOf(mysqlDateTime: String?): String {
        val d = parseMysql(mysqlDateTime) ?: return ""
        val c = Calendar.getInstance().apply { time = d }
        return String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)).fa()
    }

    /** Next [count] days as (isoDate, jalaliLabel) for date pickers. */
    fun upcomingDays(count: Int): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        return (0 until count).map { i ->
            val date = cal.time
            val label = when (i) {
                0 -> "امروز"
                1 -> "فردا"
                else -> jalaliWithDay(isoOf(date))
            }
            val pair = isoOf(date) to label
            cal.add(Calendar.DAY_OF_YEAR, 1)
            pair
        }
    }
}
