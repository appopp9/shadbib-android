package ir.darshub.app.core
import androidx.compose.material.icons.filled.Download

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore(name = "darshub")

data class SessionState(val token: String?, val username: String?)

data class TimerPersist(
    val running: Boolean,
    val startEpochMs: Long,
    val accumMs: Long,
    val courseId: Int?,
)

/** "system" | "dark" | "light" */
data class Prefs(
    val themeMode: String = "system",
    val themeColor: String = "mint",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val avatar: String? = null,
    val muted: Set<String> = emptySet(),
    val pinned: Set<String> = emptySet(),
    val pushTypes: Set<String> = setOf("follow", "post_like", "post_reply", "repost", "mention", "group_reply", "reaction", "dm"),
    val adImages: Boolean = true,
    val adVoice: Boolean = true,
    val adFiles: Boolean = false,
    val adMaxMb: Int = 10,
    val lastStudyMode: Int = 0,
    val autoStudyPost: Boolean = true,
)

object Store {
    private val KEY_TOKEN = stringPreferencesKey("token")
    private val KEY_USER = stringPreferencesKey("username")
    private val KEY_T_RUNNING = booleanPreferencesKey("timer_running")
    private val KEY_T_START = longPreferencesKey("timer_start")
    private val KEY_T_ACCUM = longPreferencesKey("timer_accum")
    private val KEY_T_COURSE = intPreferencesKey("timer_course")
    private val KEY_THEME = stringPreferencesKey("theme_mode")
    private val KEY_THEME_COLOR = stringPreferencesKey("theme_color")
    private val KEY_REM_EN = booleanPreferencesKey("reminder_enabled")
    private val KEY_REM_H = intPreferencesKey("reminder_hour")
    private val KEY_REM_M = intPreferencesKey("reminder_minute")
    private val KEY_AVATAR = stringPreferencesKey("avatar")
    private val KEY_DM_SEEN = intPreferencesKey("dm_seen_count")
    private val KEY_MUTED = stringSetPreferencesKey("muted")
    private val KEY_PINNED = stringSetPreferencesKey("pinned")
    private val KEY_PUSH_TYPES = stringSetPreferencesKey("push_types")
    private val KEY_LAST_NOTIF = intPreferencesKey("last_notif_id")
    private val KEY_AD_IMG = booleanPreferencesKey("ad_images")
    private val KEY_AD_VOICE = booleanPreferencesKey("ad_voice")
    private val KEY_AD_FILE = booleanPreferencesKey("ad_files")
    private val KEY_AD_MAX = intPreferencesKey("ad_max_mb")
    private val KEY_STUDY_MODE = intPreferencesKey("study_mode")
    private val KEY_AUTO_POST = booleanPreferencesKey("auto_study_post")

    private lateinit var appContext: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _session = MutableStateFlow(SessionState(null, null))
    val session: StateFlow<SessionState> get() = _session

    private val _timer = MutableStateFlow(TimerPersist(false, 0L, 0L, null))
    val timer: StateFlow<TimerPersist> get() = _timer

    private val _prefs = MutableStateFlow(Prefs())
    val prefs: StateFlow<Prefs> get() = _prefs

    val token: String? get() = _session.value.token
    val username: String? get() = _session.value.username

    fun init(context: Context) {
        appContext = context.applicationContext
        runBlocking {
            val p = appContext.dataStore.data.first()
            _session.value = SessionState(p[KEY_TOKEN], p[KEY_USER])
            _timer.value = TimerPersist(
                running = p[KEY_T_RUNNING] ?: false,
                startEpochMs = p[KEY_T_START] ?: 0L,
                accumMs = p[KEY_T_ACCUM] ?: 0L,
                courseId = p[KEY_T_COURSE]?.takeIf { it > 0 },
            )
            _prefs.value = Prefs(
                themeMode = p[KEY_THEME] ?: "system",
                themeColor = p[KEY_THEME_COLOR] ?: "mint",
                reminderEnabled = p[KEY_REM_EN] ?: false,
                reminderHour = p[KEY_REM_H] ?: 20,
                reminderMinute = p[KEY_REM_M] ?: 0,
                avatar = p[KEY_AVATAR],
                muted = p[KEY_MUTED] ?: emptySet(),
                pinned = p[KEY_PINNED] ?: emptySet(),
                pushTypes = p[KEY_PUSH_TYPES] ?: setOf("follow", "post_like", "post_reply", "repost", "mention", "group_reply", "reaction", "dm"),
                adImages = p[KEY_AD_IMG] ?: true,
                adVoice = p[KEY_AD_VOICE] ?: true,
                adFiles = p[KEY_AD_FILE] ?: false,
                adMaxMb = p[KEY_AD_MAX] ?: 10,
                lastStudyMode = p[KEY_STUDY_MODE] ?: 0,
                autoStudyPost = p[KEY_AUTO_POST] ?: true,
            )
        }
    }

    fun saveSession(token: String, username: String) {
        _session.value = SessionState(token, username)
        scope.launch { appContext.dataStore.edit { it[KEY_TOKEN] = token; it[KEY_USER] = username } }
    }

    fun clearSession() {
        _session.value = SessionState(null, null)
        scope.launch { appContext.dataStore.edit { it.remove(KEY_TOKEN); it.remove(KEY_USER) } }
    }

    fun saveTimer(t: TimerPersist) {
        _timer.value = t
        scope.launch {
            appContext.dataStore.edit {
                it[KEY_T_RUNNING] = t.running; it[KEY_T_START] = t.startEpochMs
                it[KEY_T_ACCUM] = t.accumMs; it[KEY_T_COURSE] = t.courseId ?: 0
            }
        }
    }

    fun setThemeMode(mode: String) {
        _prefs.value = _prefs.value.copy(themeMode = mode)
        scope.launch { appContext.dataStore.edit { it[KEY_THEME] = mode } }
    }

    fun setThemeColor(color: String) {
        _prefs.value = _prefs.value.copy(themeColor = color)
        scope.launch { appContext.dataStore.edit { it[KEY_THEME_COLOR] = color } }
    }

    fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        _prefs.value = _prefs.value.copy(reminderEnabled = enabled, reminderHour = hour, reminderMinute = minute)
        scope.launch {
            appContext.dataStore.edit {
                it[KEY_REM_EN] = enabled; it[KEY_REM_H] = hour; it[KEY_REM_M] = minute
            }
        }
    }

    fun setAvatar(path: String?) {
        _prefs.value = _prefs.value.copy(avatar = path)
        scope.launch { appContext.dataStore.edit { if (path != null) it[KEY_AVATAR] = path else it.remove(KEY_AVATAR) } }
    }

    fun togglePushType(t: String) {
        val cur = _prefs.value.pushTypes.toMutableSet()
        if (!cur.remove(t)) cur.add(t)
        _prefs.value = _prefs.value.copy(pushTypes = cur)
        scope.launch { appContext.dataStore.edit { it[KEY_PUSH_TYPES] = cur } }
    }

    /** بزرگ‌ترین شناسه نوتیفی که پوش شده — برای جلوگیری از پوش تکراری. */
    fun lastNotifId(): Int = runBlocking { appContext.dataStore.data.first()[KEY_LAST_NOTIF] ?: 0 }
    fun setLastNotifId(id: Int) { scope.launch { appContext.dataStore.edit { it[KEY_LAST_NOTIF] = id } } }

    /** آخرین شناسهٔ پیامِ «منشن/ریپلای‌شدهٔ من» که کاربر دیده — ماندگار روی دستگاه، تا با بستن و باز کردن چت دوباره ظاهر نشود. */
    fun groupSeen(groupId: Int): Int = runBlocking { appContext.dataStore.data.first()[intPreferencesKey("grp_seen_$groupId")] ?: 0 }
    fun setGroupSeen(groupId: Int, msgId: Int) { scope.launch { appContext.dataStore.edit { it[intPreferencesKey("grp_seen_$groupId")] = msgId } } }

    /** همان مفهوم برای ریپلای‌های کانال (چت عمومی/رفع‌اشکال) — بدون تگ، فقط ریپلای. */
    fun channelReplySeen(channel: String): Int = runBlocking { appContext.dataStore.data.first()[intPreferencesKey("ch_reply_seen_$channel")] ?: 0 }
    fun setChannelReplySeen(channel: String, msgId: Int) { scope.launch { appContext.dataStore.edit { it[intPreferencesKey("ch_reply_seen_$channel")] = msgId } } }

    fun togglePin(key: String) {
        val cur = _prefs.value.pinned.toMutableSet()
        if (!cur.remove(key)) cur.add(key)
        _prefs.value = _prefs.value.copy(pinned = cur)
        scope.launch { appContext.dataStore.edit { it[KEY_PINNED] = cur } }
    }

    fun toggleMute(key: String) {
        val cur = _prefs.value.muted.toMutableSet()
        if (!cur.add(key)) cur.remove(key)
        _prefs.value = _prefs.value.copy(muted = cur)
        scope.launch { appContext.dataStore.edit { it[KEY_MUTED] = cur } }
    }

    fun isMuted(key: String): Boolean = _prefs.value.muted.contains(key)

    fun setAutoDownload(images: Boolean, voice: Boolean, files: Boolean, maxMb: Int) {
        _prefs.value = _prefs.value.copy(adImages = images, adVoice = voice, adFiles = files, adMaxMb = maxMb)
        scope.launch { appContext.dataStore.edit { it[KEY_AD_IMG] = images; it[KEY_AD_VOICE] = voice; it[KEY_AD_FILE] = files; it[KEY_AD_MAX] = maxMb } }
    }

    fun setAutoStudyPost(v: Boolean) {
        _prefs.value = _prefs.value.copy(autoStudyPost = v)
        scope.launch { appContext.dataStore.edit { it[KEY_AUTO_POST] = v } }
    }

    fun setLastStudyMode(m: Int) {
        _prefs.value = _prefs.value.copy(lastStudyMode = m)
        scope.launch { appContext.dataStore.edit { it[KEY_STUDY_MODE] = m } }
    }

    // ---- DM seen count (for background notification worker) ----
    suspend fun dmSeenCount(): Int = appContext.dataStore.data.first()[KEY_DM_SEEN] ?: 0
    fun setDmSeenCount(n: Int) { scope.launch { appContext.dataStore.edit { it[KEY_DM_SEEN] = n } } }

    /** Token read directly from disk — for use in background workers where the object may be fresh. */
    suspend fun tokenFromDisk(): String? = appContext.dataStore.data.first()[KEY_TOKEN]
}
