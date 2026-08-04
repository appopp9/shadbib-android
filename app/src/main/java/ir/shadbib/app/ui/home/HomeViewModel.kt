package ir.shadbib.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.TimerPersist
import ir.shadbib.app.core.int
import ir.shadbib.app.core.str
import ir.shadbib.app.core.strOrNull
import ir.shadbib.app.data.Course
import ir.shadbib.app.data.FriendStat
import ir.shadbib.app.data.NotificationItem
import ir.shadbib.app.data.StudyToday
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/** یک ردیف از جدول برترین‌های امروز. */
data class Leader(val rank: Int, val username: String, val minutes: Int, val isMe: Boolean)

class HomeViewModel : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val loaded: Boolean = false,
        val error: String? = null,
        val today: StudyToday = StudyToday(0, emptyList()),
        val streak: Int = 0,
        val wakeup: String? = null,
        val statusText: String = "",
        val mood: String = "😊",
        val friends: List<FriendStat> = emptyList(),
        val courses: List<Course> = emptyList(),
        val notifications: List<NotificationItem> = emptyList(),
        // --- رتبه‌بندی امروز (برای مقایسهٔ مطالعهٔ من با بقیه) ---
        val leaders: List<Leader> = emptyList(),
        val myRank: Int? = null,
        val participants: Int = 0,
        val avgMinutes: Int = 0,
        val topMinutes: Int = 0,
    )

    val state = MutableStateFlow(State())

    // Timer UI state (persisted through Store)
    val timerElapsedSec = MutableStateFlow(0L)
    val timerRunning = MutableStateFlow(false)
    val timerCourseId = MutableStateFlow<Int?>(null)

    init {
        refresh()
        // آپدیت لحظه‌ای: هر تغییری در اپ (ثبت مطالعه، تسک و…) خانه را تازه می‌کند
        viewModelScope.launch { ir.shadbib.app.core.RefreshBus.events.collect { refresh() } }
        // رفرش دوره‌ای برای داده‌های زنده (دوستان آنلاین، اعلان‌ها، استریک)
        viewModelScope.launch { while (true) { kotlinx.coroutines.delay(25000); refresh() } }
        viewModelScope.launch {
            while (true) {
                val t = Store.timer.value
                val elapsedMs = t.accumMs + if (t.running) (System.currentTimeMillis() - t.startEpochMs).coerceAtLeast(0) else 0L
                timerElapsedSec.value = elapsedMs / 1000
                timerRunning.value = t.running
                timerCourseId.value = t.courseId
                delay(300)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(error = null) }
            try {
                coroutineScope {
                    val todayD = async { StudyToday.from(Api.obj(Api.get("study_today"))) }
                    val streakD = async { runCatching { Api.obj(Api.get("streak")).int("streak") }.getOrDefault(0) }
                    val wakeD = async { runCatching { Api.obj(Api.get("wakeup")).strOrNull("wakeup_time") }.getOrNull() }
                    val statusD = async { runCatching { Api.obj(Api.get("status")).str("status_text") }.getOrDefault("") }
                    val friendsD = async { runCatching { FriendStat.list(Api.arr(Api.get("following_stats"))) }.getOrDefault(emptyList()) }
                    val coursesD = async { runCatching { Course.list(Api.arr(Api.get("courses"))) }.getOrDefault(emptyList()) }
                    val notifD = async { runCatching { NotificationItem.list(Api.arr(Api.get("notifications"))) }.getOrDefault(emptyList()) }
                    val leadersD = async { runCatching { loadLeaders() }.getOrDefault(emptyList()) }
                    val leaders = leadersD.await()
                    val mine = leaders.firstOrNull { l -> l.isMe }
                    val avg = if (leaders.isEmpty()) 0 else leaders.sumOf { l -> l.minutes } / leaders.size
                    val top = leaders.maxOfOrNull { l -> l.minutes } ?: 0
                    state.update {
                        it.copy(
                            loading = false,
                            loaded = true,
                            leaders = leaders,
                            myRank = mine?.rank,
                            participants = leaders.size,
                            avgMinutes = avg,
                            topMinutes = top,
                            today = todayD.await(),
                            streak = streakD.await(),
                            wakeup = wakeD.await(),
                            statusText = statusD.await(),
                            friends = friendsD.await(),
                            courses = coursesD.await(),
                            notifications = notifD.await(),
                        )
                    }
                }
            } catch (e: Exception) {
                state.update { it.copy(loading = false, error = e.message ?: "خطا در دریافت اطلاعات") }
            }
        }
    }

    /** جدول برترین‌های امروز — برای نمایش رتبهٔ کاربر و مقایسه با میانگین. */
    private suspend fun loadLeaders(): List<Leader> {
        val o = Api.obj(Api.get("study_top", "scope" to "today", "limit" to "20"))
        val arr = o.optJSONArray("top") ?: return emptyList()
        val me = Store.username
        val out = ArrayList<Leader>()
        for (i in 0 until arr.length()) {
            val r = arr.getJSONObject(i)
            val name = r.str("username")
            out.add(Leader(r.optInt("rank", i + 1), name, r.int("minutes"), name == me))
        }
        return out
    }

    // ---------- Timer ----------
    fun startTimer() {
        val t = Store.timer.value
        Store.saveTimer(TimerPersist(true, System.currentTimeMillis(), t.accumMs, t.courseId))
    }

    fun pauseTimer() {
        val t = Store.timer.value
        if (t.running) {
            Store.saveTimer(t.copy(running = false, accumMs = t.accumMs + (System.currentTimeMillis() - t.startEpochMs).coerceAtLeast(0)))
        }
    }

    fun resetTimer() {
        Store.saveTimer(TimerPersist(false, 0L, 0L, null))
    }

    fun setTimerCourse(courseId: Int?) {
        Store.saveTimer(Store.timer.value.copy(courseId = courseId))
    }

    // ---------- Actions ----------
    fun submitStudy(minutes: Int, courseId: Int?, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val body = JSONObject().put("minutes", minutes)
                if (courseId != null) body.put("course_id", courseId)
                body.put("share_post", if (ir.shadbib.app.core.Store.prefs.value.autoStudyPost) 1 else 0)
                Api.post("study", body)
                onResult(null)
                refresh()
                ir.shadbib.app.core.RefreshBus.emit("study")
            } catch (e: Exception) {
                onResult(e.message ?: "خطا در ثبت مطالعه")
            }
        }
    }

    fun setWakeup(time: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.post("wakeup", JSONObject().put("wakeup_time", time))
                state.update { it.copy(wakeup = time) }
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "خطا")
            }
        }
    }

    fun checkinWakeup(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val res = Api.obj(Api.post("wakeup_checkin", JSONObject()))
                val t = res.optString("wakeup_time", "")
                state.update { it.copy(wakeup = t) }
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "خطا")
            }
        }
    }

    fun setMood(emoji: String) {
        viewModelScope.launch {
            runCatching { Api.post("mood", JSONObject().put("mood", emoji)) }
            state.update { it.copy(mood = emoji) }
        }
    }

    fun setStatus(text: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.post("status", JSONObject().put("status_text", text))
                state.update { it.copy(statusText = text) }
                onResult(null)
            } catch (e: Exception) {
                onResult(e.message ?: "خطا")
            }
        }
    }

    fun addCourse(name: String, color: String, icon: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.post("courses", JSONObject().put("name", name).put("color", color).put("icon", icon))
                onResult(null)
                refreshCourses()
            } catch (e: Exception) {
                onResult(e.message ?: "خطا")
            }
        }
    }

    fun deleteCourse(id: Int) {
        viewModelScope.launch {
            runCatching { Api.delete("courses", "id" to id.toString()) }
            refreshCourses()
        }
    }

    private fun refreshCourses() {
        viewModelScope.launch {
            runCatching { Course.list(Api.arr(Api.get("courses"))) }
                .onSuccess { list -> state.update { it.copy(courses = list) } }
        }
    }
}
