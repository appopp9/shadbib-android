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

data class RoomSnapshot(
    val online: Int = 0,
    val others: List<RoomOccupant> = emptyList(),
    val loaded: Boolean = false,
) {
    companion object {
        fun from(o: JSONObject) = RoomSnapshot(
            online = o.int("online"),
            others = o.optJSONArray("others")?.objects()?.map { RoomOccupant.from(it) } ?: emptyList(),
            loaded = true,
        )
    }
}
