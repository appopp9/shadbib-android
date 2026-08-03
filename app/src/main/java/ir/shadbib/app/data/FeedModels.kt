package ir.shadbib.app.data

import ir.shadbib.app.core.int
import ir.shadbib.app.core.str
import ir.shadbib.app.core.strOrNull
import org.json.JSONArray
import org.json.JSONObject

/** پست شبکه اجتماعی (سبک توییتر). */
data class Post(
    val id: Int,
    val username: String,
    val avatar: String?,
    val mood: String?,
    val text: String?,
    val mediaPath: String?,
    val mediaType: String,        // none | image | video | music
    val musicTitle: String?,
    val musicArtist: String?,
    val musicCover: String?,
    val replyTo: Int?,
    val replyToUser: String?,
    val repostOf: Int?,
    val repostUser: String?,
    val repostText: String?,
    val likes: Int,
    val replies: Int,
    val reposts: Int,
    val views: Int,
    val liked: Boolean,
    val reposted: Boolean,
    val isFollowing: Boolean,
    val createdAt: String?,
) {
    companion object {
        fun from(o: JSONObject) = Post(
            id = o.int("id"),
            username = o.strOrNull("username") ?: "?",
            avatar = o.strOrNull("avatar"),
            mood = o.strOrNull("mood"),
            text = o.strOrNull("text"),
            mediaPath = o.strOrNull("media_path"),
            mediaType = o.strOrNull("media_type") ?: "none",
            musicTitle = o.strOrNull("music_title"),
            musicArtist = o.strOrNull("music_artist"),
            musicCover = o.strOrNull("music_cover"),
            replyTo = o.optInt("reply_to", 0).takeIf { it > 0 },
            replyToUser = o.strOrNull("reply_to_user"),
            repostOf = o.optInt("repost_of", 0).takeIf { it > 0 },
            repostUser = o.strOrNull("repost_user"),
            repostText = o.strOrNull("repost_text"),
            likes = o.int("likes_count"),
            replies = o.int("replies_count"),
            reposts = o.int("reposts_count"),
            views = o.int("views_count"),
            liked = o.optInt("liked", 0) == 1,
            reposted = o.optInt("reposted", 0) == 1,
            isFollowing = o.optInt("is_following", 0) == 1,
            createdAt = o.strOrNull("created_at"),
        )
        fun list(a: JSONArray): List<Post> = (0 until a.length()).map { from(a.getJSONObject(it)) }
    }
}

/** پروفایل عمومی کاربر (سبک توییتر). */
data class SocialProfile(
    val username: String,
    val avatar: String?,
    val mood: String?,
    val statusText: String?,
    val followers: Int,
    val following: Int,
    val postsCount: Int,
    val isFollowing: Boolean,
    val followsMe: Boolean,
    val todayMinutes: Int,
    val weekMinutes: Int,
    val streak: Int,
    val isOnline: Boolean,
    val joinedAt: String?,
    val banner: String,
    val iBlocked: Boolean = false,
    val lastSeenText: String? = null,
) {
    companion object {
        fun from(o: JSONObject) = SocialProfile(
            username = o.strOrNull("username") ?: "?",
            avatar = o.strOrNull("avatar"),
            mood = o.strOrNull("mood"),
            statusText = o.strOrNull("status_text"),
            followers = o.int("followers"),
            following = o.int("following"),
            postsCount = o.int("posts_count"),
            isFollowing = o.optInt("is_following", 0) == 1,
            followsMe = o.optInt("follows_me", 0) == 1,
            todayMinutes = o.int("today_minutes"),
            weekMinutes = o.int("week_minutes"),
            streak = o.int("streak"),
            isOnline = o.optInt("is_online", 0) == 1,
            joinedAt = o.strOrNull("created_at"),
            banner = o.strOrNull("banner") ?: "brand",
            iBlocked = o.optInt("i_blocked", 0) == 1,
            lastSeenText = o.strOrNull("last_seen_text"),
        )
    }
}

/** آیتم لیست فالوور/فالویینگ و نتیجه جستجوی کاربر. */
data class FollowUser(
    val username: String,
    val avatar: String?,
    val mood: String?,
    val isFollowing: Boolean,
    val followsMe: Boolean,
    val todayMinutes: Int,
) {
    companion object {
        fun from(o: JSONObject) = FollowUser(
            username = o.strOrNull("username") ?: "?",
            avatar = o.strOrNull("avatar"),
            mood = o.strOrNull("mood"),
            isFollowing = o.optInt("is_following", 0) == 1,
            followsMe = o.optInt("follows_me", 0) == 1,
            todayMinutes = o.int("today_minutes"),
        )
        fun list(a: JSONArray): List<FollowUser> = (0 until a.length()).map { from(a.getJSONObject(it)) }
    }
}
