package ir.shadbib.app.data

import ir.shadbib.app.core.boolish
import ir.shadbib.app.core.int
import ir.shadbib.app.core.intOrNull
import ir.shadbib.app.core.objects
import ir.shadbib.app.core.str
import ir.shadbib.app.core.strOrNull
import org.json.JSONArray
import org.json.JSONObject

data class Conversation(
    val username: String,
    val mood: String,
    val lastSeen: String?,
    val lastSeenText: String? = null,
    val isOnline: Boolean,
    val avatar: String? = null,
    val lastMessage: String,
    val lastType: String,
    val lastTime: String,
    val lastFromMe: Boolean,
    val unread: Int,
) {
    companion object {
        fun from(o: JSONObject) = Conversation(
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.strOrNull("last_seen"),
            o.boolish("is_online"), o.strOrNull("avatar"), o.str("last_message"), o.str("last_type").ifEmpty { "text" },
            o.str("last_time"), o.boolish("last_from_me"), o.int("unread"),
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class DmMessage(
    val id: Int,
    val sender: String,
    val type: String,          // text | image | voice
    val message: String?,
    val mediaPath: String?,
    val duration: Int?,
    val fileName: String?,
    val fileSize: Int?,
    val replyTo: Int?,
    val replyMessage: String?,
    val replyType: String?,
    val replySender: String?,
    val isRead: Boolean,
    val edited: Boolean,
    val createdAt: String,
    val senderAvatar: String? = null,
    val reactions: List<Reaction> = emptyList(),
) {
    companion object {
        fun from(o: JSONObject) = DmMessage(
            o.int("id"), o.str("sender"), o.str("type").ifEmpty { "text" },
            o.strOrNull("message"), o.strOrNull("media_path"), o.intOrNull("duration"),
            o.strOrNull("file_name"), o.intOrNull("file_size"),
            o.intOrNull("reply_to"), o.strOrNull("reply_message"), o.strOrNull("reply_type"),
            o.strOrNull("reply_sender"), o.boolish("is_read"), o.boolish("edited"),
            o.str("created_at"), o.strOrNull("sender_avatar"),
            o.optJSONArray("reactions")?.let { Reaction.list(it) } ?: emptyList(),
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class DmPartner(
    val username: String,
    val mood: String,
    val statusText: String,
    val lastSeen: String?,
    val lastSeenText: String? = null,
    val isOnline: Boolean,
    val avatar: String? = null,
) {
    companion object {
        fun from(o: JSONObject) = DmPartner(
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.str("status_text"),
            o.strOrNull("last_seen"), o.boolish("is_online"), o.strOrNull("avatar"),
        )
    }
}

data class UserResult(
    val username: String,
    val mood: String,
    val statusText: String,
    val lastSeen: String?,
    val isOnline: Boolean,
    val isFollowing: Boolean,
    val avatar: String? = null,
) {
    companion object {
        fun from(o: JSONObject) = UserResult(
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.str("status_text"),
            o.strOrNull("last_seen"), o.boolish("is_online"), o.boolish("is_following"), o.strOrNull("avatar"),
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

/** ری‌اکشن روی پیام (سبک تلگرام). */
data class Reaction(val emoji: String, val count: Int, val mine: Boolean) {
    companion object {
        fun from(o: JSONObject) = Reaction(o.str("emoji"), o.int("count"), o.optInt("mine", 0) == 1)
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}
