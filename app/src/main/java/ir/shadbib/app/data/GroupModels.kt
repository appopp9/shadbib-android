package ir.shadbib.app.data

import ir.shadbib.app.core.boolish
import ir.shadbib.app.core.int
import ir.shadbib.app.core.intOrNull
import ir.shadbib.app.core.objects
import ir.shadbib.app.core.str
import ir.shadbib.app.core.strOrNull
import org.json.JSONArray
import org.json.JSONObject

data class StudyGroup(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: String?,
    val memberCount: Int,
    val ownerName: String,
    val isOwner: Boolean,
) {
    companion object {
        fun from(o: JSONObject) = StudyGroup(
            o.int("id"), o.str("name"), o.str("username"), o.strOrNull("avatar"),
            o.int("member_count"), o.str("owner_name"), o.boolish("is_owner"),
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class GroupMember(
    val username: String,
    val mood: String,
    val avatar: String?,
    val todayMinutes: Int,
    val isOnline: Boolean,
) {
    companion object {
        fun from(o: JSONObject) = GroupMember(
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.strOrNull("avatar"),
            o.int("today_minutes"), o.boolish("is_online"),
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class GroupDetail(
    val id: Int,
    val name: String,
    val username: String,
    val avatar: String?,
    val memberCount: Int,
    val ownerName: String,
    val isOwner: Boolean,
    val isMember: Boolean,
    val members: List<GroupMember>,
) {
    companion object {
        fun from(o: JSONObject) = GroupDetail(
            o.int("id"), o.str("name"), o.str("username"), o.strOrNull("avatar"),
            o.int("member_count"), o.str("owner_name"), o.boolish("is_owner"), o.boolish("is_member"),
            o.optJSONArray("members")?.let { GroupMember.list(it) } ?: emptyList(),
        )
    }
}

data class GroupMessage(
    val id: Int,
    val sender: String,
    val senderAvatar: String?,
    val message: String,
    val replyTo: Int?,
    val replyMessage: String?,
    val replySender: String?,
    val createdAt: String,
    val type: String = "text",
    val mediaPath: String? = null,
    val fileName: String? = null,
    val fileSize: Int? = null,
    val duration: Int? = null,
    val reactions: List<ir.shadbib.app.data.Reaction> = emptyList(),
) {
    companion object {
        fun from(o: JSONObject) = GroupMessage(
            o.int("id"), o.str("sender"), o.strOrNull("sender_avatar"), o.str("message"),
            o.intOrNull("reply_to"), o.strOrNull("reply_message"), o.strOrNull("reply_sender"),
            o.str("created_at"),
            o.str("type").ifEmpty { "text" }, o.strOrNull("media_path"),
            o.strOrNull("file_name"), o.intOrNull("file_size"), o.intOrNull("duration"),
            o.optJSONArray("reactions")?.let { ir.shadbib.app.data.Reaction.list(it) } ?: emptyList(),
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class Sticker(
    val id: Int,
    val code: String,
    val filepath: String,
    val type: String,
) {
    companion object {
        fun from(o: JSONObject) = Sticker(o.int("id"), o.str("code"), o.str("filepath"), o.str("type").ifEmpty { "sticker" })
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}
