package ir.shadbib.app.player

import ir.shadbib.app.core.Store
import ir.shadbib.app.core.TimerPersist

/** Count-up stopwatch backed by the persisted timer in Store. */
object Chrono {
    private fun notifySync() = runCatching { ir.shadbib.app.notify.StudyNotifier.sync(ir.shadbib.app.App.instance) }
    fun elapsedMs(): Long {
        val t = Store.timer.value
        return t.accumMs + if (t.running) (System.currentTimeMillis() - t.startEpochMs).coerceAtLeast(0) else 0L
    }

    val running: Boolean get() = Store.timer.value.running

    fun start() {
        // کرنومتر و پومودورو هرگز همزمان فعال نمی‌شوند
        if (Pomodoro.state.value.running) Pomodoro.pause()
        val t = Store.timer.value
        Store.saveTimer(TimerPersist(true, System.currentTimeMillis(), t.accumMs, t.courseId))
        notifySync()
    }

    fun pause() {
        val t = Store.timer.value
        if (t.running) {
            Store.saveTimer(t.copy(running = false, accumMs = t.accumMs + (System.currentTimeMillis() - t.startEpochMs).coerceAtLeast(0)))
        }
        notifySync()
    }

    fun reset() { Store.saveTimer(TimerPersist(false, 0L, 0L, Store.timer.value.courseId)); notifySync() }

    fun setCourse(courseId: Int?) = Store.saveTimer(Store.timer.value.copy(courseId = courseId))
}
