package ir.shadbib.app.ui.study.room

import ir.shadbib.app.core.int
import ir.shadbib.app.core.objects
import ir.shadbib.app.core.str
import org.json.JSONObject

/** یک نفر داخل اتاق مطالعه. */
data class RoomOccupant(
    val userId: Int,
    val username: String,
    val character: String,
    val state: String,
    val minutesToday: Int,
    /** چند ثانیه از آخرین ضربان گذشته. */
    val ago: Int,
) {
    companion object {
        fun from(o: JSONObject) = RoomOccupant(
            userId = o.int("user_id"),
            username = o.str("username"),
            character = o.str("character").ifBlank { "cat" },
            state = o.str("state").ifBlank { RoomState.IDLE },
            minutesToday = o.int("minutes_today"),
            ago = o.int("ago"),
        )
    }
}

/** یک تشویق که کسی برای ما فرستاده. سرور بعد از تحویل حذفش می‌کند. */
data class RoomCheer(
    val emoji: String,
    val from: String,
)

/** یک ردیف از تابلوی رکورد امروز. */
data class RoomTopEntry(
    val username: String,
    val character: String,
    val minutes: Int,
)

/**
 * آمار قاب‌های دیواری.
 *
 * این‌ها سنگین‌اند، پس فقط وقتی ضربان با `full=1` می‌رود پر می‌شوند —
 * یعنی موقع ورود و بعد هر چهار ضربان یک‌بار.
 */
data class RoomStats(
    val top: List<RoomTopEntry> = emptyList(),
    /** هفت روز گذشته، از قدیم به جدید. آخرین عضو یعنی امروز. */
    val weekly: List<Int> = List(7) { 0 },
    val totalMinutes: Int = 0,
    val todayMinutes: Int = 0,
    val streak: Int = 0,
    val loaded: Boolean = false,
) {
    companion object {
        /** اگر پاسخ آمار نداشت (ضربان سبک بود) null برمی‌گرداند تا آمار قبلی حفظ شود. */
        fun from(o: JSONObject): RoomStats? {
            if (!o.has("weekly") && !o.has("top")) return null

            val top = o.optJSONArray("top")?.objects()?.map {
                RoomTopEntry(
                    username = it.str("username"),
                    character = it.str("character").ifBlank { "cat" },
                    minutes = it.int("minutes"),
                )
            } ?: emptyList()

            val arr = o.optJSONArray("weekly")
            val weekly = ArrayList<Int>(7)
            for (i in 0 until 7) {
                weekly.add(if (arr != null && i < arr.length()) arr.optInt(i, 0) else 0)
            }

            return RoomStats(
                top = top,
                weekly = weekly,
                totalMinutes = o.optInt("total_minutes", 0),
                todayMinutes = o.optInt("today_minutes", 0),
                streak = o.optInt("streak", 0),
                loaded = true,
            )
        }
    }
}

data class RoomSnapshot(
    val online: Int = 0,
    val others: List<RoomOccupant> = emptyList(),
    val cheers: List<RoomCheer> = emptyList(),
    val loaded: Boolean = false,
) {
    companion object {
        fun from(o: JSONObject) = RoomSnapshot(
            online = o.int("online"),
            others = o.optJSONArray("others")?.objects()?.map { RoomOccupant.from(it) } ?: emptyList(),
            cheers = o.optJSONArray("cheers")?.objects()?.map {
                RoomCheer(emoji = it.str("emoji").ifBlank { "👏" }, from = it.str("from"))
            } ?: emptyList(),
            loaded = true,
        )
    }
}
