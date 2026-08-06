package ir.darshub.app.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.darshub.app.core.Api
import ir.darshub.app.data.Course
import ir.darshub.app.player.Pomodoro
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Backs the study space (chronometer + pomodoro). Course selection is REQUIRED;
 *  nothing is logged without a course. Pomodoro work blocks accumulate and are
 *  logged on demand (user can do many pomodoros then register the total). */
class StudyViewModel : ViewModel() {

    private val _courses = MutableStateFlow<List<Course>>(emptyList())
    val courses: StateFlow<List<Course>> get() = _courses

    private val _selectedCourse = MutableStateFlow<Int?>(null)
    val selectedCourse: StateFlow<Int?> get() = _selectedCourse

    // minutes finished in pomodoro work blocks but not yet logged
    private val _pendingMinutes = MutableStateFlow(0)
    val pendingMinutes: StateFlow<Int> get() = _pendingMinutes

    init {
        loadCourses()
        viewModelScope.launch {
            Pomodoro.workCompleted.collect { minutes -> _pendingMinutes.value += minutes }
        }
        // A course created anywhere in the app must be selectable here at once.
        viewModelScope.launch {
            ir.darshub.app.core.RefreshBus.events.collect { tag ->
                if (tag == "courses" || tag == "study" || tag == "all") loadCourses()
            }
        }
    }

    fun loadCourses() {
        viewModelScope.launch {
            runCatching { Course.list(Api.arr(Api.get("courses"))) }
                .onSuccess { list ->
                    _courses.value = list
                    if (_selectedCourse.value == null && list.isNotEmpty()) _selectedCourse.value = list.first().id
                }
        }
    }

    fun selectCourse(id: Int?) { _selectedCourse.value = id }

    /** Logs [minutes] to the selected course. Returns error text or null on success. */
    fun log(minutes: Int, onResult: (String?) -> Unit) {
        val course = _selectedCourse.value
        if (course == null) { onResult("اول یک درس انتخاب کن"); return }
        if (minutes < 1) { onResult("زمانی برای ثبت نیست"); return }
        viewModelScope.launch {
            runCatching {
                Api.post("study", JSONObject().put("minutes", minutes).put("course_id", course)
                    .put("share_post", if (ir.darshub.app.core.Store.prefs.value.autoStudyPost) 1 else 0))
            }.onSuccess { onResult(null); ir.darshub.app.core.RefreshBus.emit("study") }
                .onFailure { onResult(it.message ?: "خطا در ثبت") }
        }
    }

    fun clearPending() { _pendingMinutes.value = 0 }
}
