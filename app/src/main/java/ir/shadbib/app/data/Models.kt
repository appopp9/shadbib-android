package ir.shadbib.app.data

import ir.shadbib.app.core.boolish
import ir.shadbib.app.core.int
import ir.shadbib.app.core.intOrNull
import ir.shadbib.app.core.objects
import ir.shadbib.app.core.str
import ir.shadbib.app.core.strings
import ir.shadbib.app.core.strOrNull
import org.json.JSONArray
import org.json.JSONObject

data class Course(val id: Int, val name: String, val color: String, val icon: String) {
    companion object {
        fun from(o: JSONObject) = Course(o.int("id"), o.str("name"), o.str("color"), o.str("icon").ifEmpty { "📖" })
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class CourseMinutes(val name: String, val icon: String, val minutes: Int) {
    companion object {
        fun from(o: JSONObject) = CourseMinutes(
            o.strOrNull("name") ?: "بدون درس",
            o.strOrNull("icon") ?: "📖",
            o.int("minutes").takeIf { it > 0 } ?: o.int("total_minutes")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class StudyToday(val totalMinutes: Int, val courses: List<CourseMinutes>) {
    companion object {
        fun from(o: JSONObject) = StudyToday(
            o.int("total_minutes"),
            o.optJSONArray("courses")?.let { CourseMinutes.list(it) } ?: emptyList()
        )
    }
}

data class TaskItem(
    val id: Int, val title: String, val done: Boolean,
    val date: String, val priority: String
) {
    companion object {
        fun from(o: JSONObject) = TaskItem(
            o.int("id"), o.str("title"), o.boolish("done"),
            o.str("task_date"), o.str("priority").ifEmpty { "normal" }
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class FriendStat(
    val username: String, val mood: String, val statusText: String,
    val wakeupTime: String?, val lastSeen: String?, val lastSeenText: String? = null, val todayMinutes: Int,
    val tasksCount: Int, val tasksDone: Int, val isOnline: Boolean, val avatar: String? = null
) {
    companion object {
        fun from(o: JSONObject) = FriendStat(
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.str("status_text"),
            o.strOrNull("wakeup_time"), o.strOrNull("last_seen"), o.strOrNull("last_seen_text"), o.int("today_minutes"),
            o.int("tasks_count"), o.int("tasks_done"), o.boolish("is_online"), o.strOrNull("avatar")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class FriendDetail(
    val username: String, val mood: String, val statusText: String,
    val wakeupTime: String?, val lastSeen: String?, val isOnline: Boolean,
    val todayMinutes: Int, val tasks: List<TaskItem>, val courses: List<CourseMinutes>,
    val avatar: String? = null
) {
    companion object {
        fun from(o: JSONObject) = FriendDetail(
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.str("status_text"),
            o.strOrNull("wakeup_time"), o.strOrNull("last_seen"), o.boolish("is_online"),
            o.int("today_minutes"),
            o.optJSONArray("tasks")?.let { TaskItem.list(it) } ?: emptyList(),
            o.optJSONArray("courses")?.let { CourseMinutes.list(it) } ?: emptyList(),
            o.strOrNull("avatar")
        )
    }
}

data class ChatMessage(
    val id: Int, val message: String, val replyTo: Int?, val createdAt: String,
    val sender: String, val replyMessage: String?, val replySender: String?,
    val senderAvatar: String? = null, val senderMood: String? = null,
    val type: String = "text", val mediaPath: String? = null,
    val fileName: String? = null, val fileSize: Int? = null, val duration: Int? = null,
    val reactions: List<ir.shadbib.app.data.Reaction> = emptyList()
) {
    companion object {
        fun from(o: JSONObject) = ChatMessage(
            o.int("id"), o.str("message"), o.intOrNull("reply_to"), o.str("created_at"),
            o.str("sender"), o.strOrNull("reply_message"), o.strOrNull("reply_sender"),
            o.strOrNull("sender_avatar"), o.strOrNull("sender_mood"),
            o.str("type").ifEmpty { "text" }, o.strOrNull("media_path"),
            o.strOrNull("file_name"), o.intOrNull("file_size"), o.intOrNull("duration"),
            o.optJSONArray("reactions")?.let { ir.shadbib.app.data.Reaction.list(it) } ?: emptyList()
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class NotificationItem(
    val id: Int, val chatId: Int?, val sender: String, val message: String,
    val read: Boolean, val createdAt: String
) {
    companion object {
        fun from(o: JSONObject) = NotificationItem(
            o.int("id"), o.intOrNull("chat_id"), o.str("sender"), o.str("message"),
            o.boolish("read"), o.str("created_at")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class Note(
    val id: Int, val content: String, val likes: Int, val createdAt: String,
    val username: String, val mood: String, val commentsCount: Int
) {
    companion object {
        fun from(o: JSONObject) = Note(
            o.int("id"), o.str("content"), o.int("likes"), o.str("created_at"),
            o.str("username"), o.str("mood").ifEmpty { "😊" }, o.int("comments_count")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class CommentItem(val id: Int, val username: String, val comment: String, val createdAt: String) {
    companion object {
        fun from(o: JSONObject) = CommentItem(
            o.int("id"), o.str("username"), o.str("comment"), o.str("created_at")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class MusicTrack(
    val id: Int, val title: String, val artist: String, val filepath: String,
    val cover: String?, val likes: Int, val username: String, val createdAt: String
) {
    companion object {
        fun from(o: JSONObject) = MusicTrack(
            o.int("id"), o.str("title"), o.str("artist"), o.str("filepath"),
            o.strOrNull("cover"),
            if (o.has("likes_count")) o.int("likes_count") else o.int("likes"),
            o.str("username"), o.str("created_at")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class Playlist(
    val id: Int, val name: String, val isPublic: Boolean,
    val username: String, val musicCount: Int
) {
    companion object {
        fun from(o: JSONObject) = Playlist(
            o.int("id"), o.str("name"), o.boolish("is_public"),
            o.str("username"), o.int("music_count")
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class Summary(
    val id: Int, val title: String, val description: String, val category: String,
    val likes: Int, val createdAt: String, val username: String,
    val commentsCount: Int, val images: List<String>
) {
    companion object {
        fun from(o: JSONObject) = Summary(
            o.int("id"), o.str("title"), o.str("description"), o.str("category"),
            o.int("likes"), o.str("created_at"), o.str("username"),
            o.int("comments_count"),
            o.optJSONArray("images")?.strings() ?: emptyList()
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class HourlyPart(val course: String, val icon: String, val minutes: Int, val start: String, val end: String) {
    companion object {
        fun from(o: JSONObject) = HourlyPart(
            o.strOrNull("course") ?: "بدون درس", o.strOrNull("icon") ?: "📖", o.int("minutes"),
            o.str("start_time").ifEmpty { o.str("start") },
            o.str("end_time").ifEmpty { o.str("end") }
        )
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class DayMinutes(val date: String, val minutes: Int) {
    companion object {
        fun from(o: JSONObject) = DayMinutes(o.str("study_date"), o.int("minutes"))
        fun list(a: JSONArray) = a.objects().map { from(it) }
    }
}

data class ProfileData(
    val totalMinutes: Int, val studyDays: Int,
    val courses: List<CourseMinutes>, val daily: List<DayMinutes>,
    val avatar: String? = null, val mood: String = "😊"
) {
    companion object {
        fun from(o: JSONObject) = ProfileData(
            o.int("total_minutes"), o.int("study_days"),
            o.optJSONArray("courses")?.let { CourseMinutes.list(it) } ?: emptyList(),
            o.optJSONArray("daily")?.let { DayMinutes.list(it) } ?: emptyList(),
            o.strOrNull("avatar"), o.str("mood").ifEmpty { "😊" }
        )
    }
}

data class DailyReport(
    val wakeupTime: String?, val totalMinutes: Int, val streak: Int,
    val sessions: List<HourlyPart>, val firstStudy: String?, val lastStudy: String?,
    val courses: List<CourseMinutes>
) {
    companion object {
        fun from(o: JSONObject) = DailyReport(
            o.strOrNull("wakeup_time"), o.int("total_minutes"), o.int("streak"),
            o.optJSONArray("sessions")?.let { HourlyPart.list(it) } ?: emptyList(),
            o.strOrNull("first_study"), o.strOrNull("last_study"),
            o.optJSONArray("courses")?.let { CourseMinutes.list(it) } ?: emptyList()
        )
    }
}
