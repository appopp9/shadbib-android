package ir.shadbib.app.data

import ir.shadbib.app.core.int
import ir.shadbib.app.core.strOrNull
import org.json.JSONArray
import org.json.JSONObject

/** اعلان یکپارچه (اجتماع + پیام + ری‌اکشن). */
data class Notif(
    val id: Int,
    val type: String,
    val refId: Int,
    val text: String?,
    val actor: String,
    val actorAvatar: String?,
    val read: Boolean,
    val createdAt: String?,
) {
    val emoji: String get() = when (type) {
        "follow" -> "🫂"; "post_like" -> "❤️"; "post_reply" -> "💬"; "repost" -> "🔁"
        "mention" -> "📣"; "group_reply" -> "↩️"; "reaction" -> "😍"; "dm" -> "✉️"; else -> "🔔"
    }
    val title: String get() = when (type) {
        "follow" -> "$actor دنبالت کرد"
        "post_like" -> "$actor پستت رو لایک کرد"
        "post_reply" -> "$actor به پستت پاسخ داد"
        "repost" -> "$actor پستت رو بازنشر کرد"
        "mention" -> "$actor تو گروه منشنت کرد"
        "group_reply" -> "$actor تو گروه بهت پاسخ داد"
        "reaction" -> "$actor به پیامت ری‌اکشن داد"
        "dm" -> "پیام جدید از $actor"
        else -> actor
    }
    companion object {
        fun from(o: JSONObject) = Notif(
            o.int("id"), o.strOrNull("type") ?: "", o.int("ref_id"), o.strOrNull("text"),
            o.strOrNull("actor") ?: "?", o.strOrNull("actor_avatar"),
            o.optInt("read", 0) == 1, o.strOrNull("created_at"),
        )
        fun list(a: JSONArray): List<Notif> = (0 until a.length()).map { from(a.getJSONObject(it)) }
    }
}
