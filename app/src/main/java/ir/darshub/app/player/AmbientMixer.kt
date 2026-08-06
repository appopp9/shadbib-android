package ir.darshub.app.player

import android.content.Context
import android.media.MediaPlayer
import ir.darshub.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted

/** A single ambient sound definition. */
data class AmbientSound(
    val key: String,
    val label: String,
    val emoji: String,
    val rawResId: Int,
)

/** Global looping ambient-sound mixer. Multiple sounds play simultaneously,
 *  each with an independent volume, and keep playing across screens. */
object AmbientMixer {

    val sounds: List<AmbientSound> = listOf(
        AmbientSound("rain", "باران", "🌧️", R.raw.amb_rain),
        AmbientSound("fire", "آتش", "🔥", R.raw.amb_fire),
        AmbientSound("thunder", "رعد و برق", "⛈️", R.raw.amb_thunder),
        AmbientSound("wind", "باد", "🍃", R.raw.amb_wind),
        AmbientSound("ocean", "موج دریا", "🌊", R.raw.amb_ocean),
        AmbientSound("forest", "جنگل", "🌲", R.raw.amb_forest),
        AmbientSound("night", "شب", "🌙", R.raw.amb_night),
        AmbientSound("cafe", "کافه", "☕", R.raw.amb_cafe),
        AmbientSound("noise", "نویز سفید", "📻", R.raw.amb_whitenoise),
    )

    data class SoundState(val active: Boolean = false, val volume: Float = 0.6f)

    private val players = HashMap<String, MediaPlayer>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _states = MutableStateFlow(sounds.associate { it.key to SoundState() })
    val states: StateFlow<Map<String, SoundState>> get() = _states

    val activeCount: StateFlow<Int> = _states
        .map { m -> m.values.count { it.active } }
        .stateIn(scope, SharingStarted.Eagerly, 0)

    fun stateOf(key: String): SoundState = _states.value[key] ?: SoundState()

    fun toggle(context: Context, key: String) {
        val cur = stateOf(key)
        if (cur.active) {
            players[key]?.let { runCatching { it.stop(); it.release() } }
            players.remove(key)
            update(key) { it.copy(active = false) }
        } else {
            val res = sounds.find { it.key == key } ?: return
            runCatching {
                val mp = MediaPlayer.create(context.applicationContext, res.rawResId)
                mp.isLooping = true
                mp.setVolume(cur.volume, cur.volume)
                mp.start()
                players[key] = mp
            }
            update(key) { it.copy(active = true) }
        }
    }

    fun setVolume(key: String, volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        players[key]?.setVolume(v, v)
        update(key) { it.copy(volume = v) }
    }

    fun stopAll() {
        players.values.forEach { runCatching { it.stop(); it.release() } }
        players.clear()
        _states.value = sounds.associate { it.key to SoundState() }
    }

    private inline fun update(key: String, block: (SoundState) -> SoundState) {
        val m = _states.value.toMutableMap()
        m[key] = block(m[key] ?: SoundState())
        _states.value = m
    }
}
