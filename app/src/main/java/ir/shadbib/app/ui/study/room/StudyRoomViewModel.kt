package ir.shadbib.app.ui.study.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.RefreshBus
import ir.shadbib.app.core.humanizeError
import ir.shadbib.app.core.str
import ir.shadbib.app.player.Chrono
import ir.shadbib.app.player.Pomodoro
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * مغز اتاق مطالعه.
 *
 * روی هاست اشتراکی PHP وب‌ساکت نداریم؛ پس حضور با «ضربان» هر [BEAT_SEC] ثانیه
 * نگه داشته می‌شود. همان یک درخواست هم وضعیت ما را می‌فرستد و هم لیست بقیه را
 * برمی‌گرداند — یعنی یک رفت‌وبرگشت در هر چرخه، نه دو تا.
 *
 * حضور فقط تا وقتی ادامه دارد که کاربر روی این صفحه و اپ جلوی چشمش باشد؛
 * صفحه که عوض شود یا اپ برود پس‌زمینه، [leave] صدا می‌شود.
 */
class StudyRoomViewModel : ViewModel() {

    companion object {
        const val BEAT_SEC = 25L
        const val PUBLIC_ROOM = 1
    }

    private val _snapshot = MutableStateFlow(RoomSnapshot())
    val snapshot: StateFlow<RoomSnapshot> get() = _snapshot

    private val _character = MutableStateFlow("cat")
    val character: StateFlow<String> get() = _character

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> get() = _error

    /**
     * وضعیت خودمان، به صورت StateFlow.
     *
     * قبلاً این یک تابع ساده بود که در حین ترکیب صدا می‌شد. مشکلش این بود که
     * `Chrono.running` یک مقدار معمولی است نه State؛ پس Compose نمی‌فهمید که عوض شده
     * و کاراکتر در حالت بیکار می‌ماند. حالا هر ثانیه خوانده و در فلو ریخته می‌شود.
     */
    private val _myState = MutableStateFlow(RoomState.IDLE)
    val myState: StateFlow<String> get() = _myState

    private var beatJob: Job? = null
    private var watchJob: Job? = null

    private fun computeMyState(): String {
        val p = Pomodoro.state.value
        return when {
            p.running && p.phase == Pomodoro.Phase.WORK -> RoomState.STUDYING
            Chrono.running -> RoomState.STUDYING
            else -> RoomState.IDLE
        }
    }

    fun enter() {
        _myState.value = computeMyState()

        if (beatJob?.isActive != true) {
            beatJob = viewModelScope.launch {
                while (isActive) {
                    beat()
                    delay(BEAT_SEC * 1000)
                }
            }
        }

        // ناظر یک‌ثانیه‌ای: تا تایمر را بزنی کاراکتر فوراً عوض می‌شود و
        // همان لحظه یک ضربان می‌رود تا بقیه هم زود ببینند.
        if (watchJob?.isActive != true) {
            watchJob = viewModelScope.launch {
                while (isActive) {
                    val s = computeMyState()
                    if (s != _myState.value) {
                        _myState.value = s
                        beat()
                    }
                    delay(1000)
                }
            }
        }
    }

    /** خروج از اتاق — صفحه عوض شد یا اپ رفت پس‌زمینه. */
    fun leave() {
        beatJob?.cancel(); beatJob = null
        watchJob?.cancel(); watchJob = null
        _snapshot.value = RoomSnapshot()
        viewModelScope.launch {
            runCatching { Api.post("room_leave", JSONObject().put("room_id", PUBLIC_ROOM)) }
        }
    }

    /** ضربان دستی — بعد از استارت/استاپ تایمر تا وضعیت فوری در اتاق عوض شود. */
    fun pulse() {
        _myState.value = computeMyState()
        viewModelScope.launch { beat() }
    }

    private suspend fun beat() {
        val body = JSONObject()
            .put("room_id", PUBLIC_ROOM)
            .put("state", _myState.value)
            .put("character", _character.value)
        runCatching { Api.obj(Api.post("room_beat", body)) }
            .onSuccess { o ->
                _error.value = null
                val mine = o.str("character")
                if (mine.isNotBlank()) _character.value = mine
                _snapshot.value = RoomSnapshot.from(o)
            }
            .onFailure { _error.value = humanizeError(it) }
    }

    fun selectCharacter(key: String) {
        _character.value = key
        viewModelScope.launch {
            runCatching { Api.post("character_set", JSONObject().put("character", key)) }
            beat()
        }
    }

    /** ثبت دقیقه‌های مطالعه در همان جدول قدیمی `study_sessions` — بدون جدول موازی. */
    fun logMinutes(minutes: Int, courseId: Int?, onResult: (String?) -> Unit) {
        if (minutes < 1) {
            onResult("زمانی برای ثبت نیست")
            return
        }
        viewModelScope.launch {
            val body = JSONObject().put("minutes", minutes)
            if (courseId != null) body.put("course_id", courseId)
            runCatching { Api.post("study", body) }
                .onSuccess {
                    RefreshBus.emit("study")
                    beat()
                    onResult(null)
                }
                .onFailure { onResult(humanizeError(it)) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        beatJob?.cancel()
        watchJob?.cancel()
    }
}
