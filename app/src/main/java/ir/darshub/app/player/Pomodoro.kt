package ir.darshub.app.player

import android.content.Context
import android.media.MediaPlayer
import ir.darshub.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Global Pomodoro focus timer. Keeps running across screens.
 *  Emits [workCompleted] (with the work-block minutes) each time a work phase finishes,
 *  so the study session can be logged to the server. */
object Pomodoro {
    private fun sync() = runCatching { ir.darshub.app.notify.StudyNotifier.sync(ir.darshub.app.App.instance) }

    enum class Phase { IDLE, WORK, BREAK, LONG_BREAK }

    data class Config(
        val workMin: Int = 25,
        val breakMin: Int = 5,
        val longBreakMin: Int = 15,
        val cyclesBeforeLong: Int = 4,
    )

    data class State(
        val phase: Phase = Phase.IDLE,
        val remainingSec: Int = 25 * 60,
        val running: Boolean = false,
        val completedWork: Int = 0,
        val config: Config = Config(),
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: kotlinx.coroutines.Job? = null

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> get() = _state

    private val _workCompleted = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val workCompleted: SharedFlow<Int> get() = _workCompleted

    fun setConfig(c: Config) {
        val phaseLen = when (_state.value.phase) {
            Phase.WORK, Phase.IDLE -> c.workMin
            Phase.BREAK -> c.breakMin
            Phase.LONG_BREAK -> c.longBreakMin
        }
        _state.value = _state.value.copy(
            config = c,
            remainingSec = if (_state.value.running) _state.value.remainingSec else phaseLen * 60,
        )
    }

    fun start(context: Context) {
        // کرنومتر و پومودورو هرگز همزمان فعال نمی‌شوند
        if (Chrono.running) Chrono.pause()
        val s = _state.value
        if (s.phase == Phase.IDLE) {
            _state.value = s.copy(phase = Phase.WORK, remainingSec = s.config.workMin * 60, running = true)
        } else {
            _state.value = s.copy(running = true)
        }
        runTicker(context.applicationContext)
        sync()
    }

    fun pause() {
        _state.value = _state.value.copy(running = false)
        ticker?.cancel()
        sync()
    }

    fun reset() {
        ticker?.cancel()
        val c = _state.value.config
        _state.value = State(config = c, remainingSec = c.workMin * 60)
        sync()
    }

    fun skip(context: Context) {
        advancePhase(context.applicationContext, skipped = true)
    }

    private fun runTicker(context: Context) {
        ticker?.cancel()
        ticker = scope.launch {
            while (_state.value.running) {
                delay(1000)
                val s = _state.value
                if (!s.running) break
                if (s.remainingSec > 1) {
                    _state.value = s.copy(remainingSec = s.remainingSec - 1)
                } else {
                    advancePhase(context, skipped = false)
                }
            }
        }
    }

    private fun advancePhase(context: Context, skipped: Boolean) {
        val s = _state.value
        val c = s.config
        when (s.phase) {
            Phase.WORK -> {
                if (!skipped) {
                    _workCompleted.tryEmit(c.workMin)
                    playChime(context)
                }
                val done = s.completedWork + (if (skipped) 0 else 1)
                val long = done > 0 && done % c.cyclesBeforeLong == 0
                _state.value = s.copy(
                    phase = if (long) Phase.LONG_BREAK else Phase.BREAK,
                    remainingSec = (if (long) c.longBreakMin else c.breakMin) * 60,
                    completedWork = done,
                )
            }
            Phase.BREAK, Phase.LONG_BREAK -> {
                if (!skipped) playChime(context)
                _state.value = s.copy(phase = Phase.WORK, remainingSec = c.workMin * 60)
            }
            Phase.IDLE -> {
                _state.value = s.copy(phase = Phase.WORK, remainingSec = c.workMin * 60)
            }
        }
        if (_state.value.running) runTicker(context)
    }

    private fun playChime(context: Context) {
        runCatching {
            val mp = MediaPlayer.create(context.applicationContext, R.raw.chime) ?: return
            mp.setOnCompletionListener { it.release() }
            mp.start()
        }
    }
}
