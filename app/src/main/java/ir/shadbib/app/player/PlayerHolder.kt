package ir.shadbib.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import ir.shadbib.app.core.Api
import ir.shadbib.app.data.MusicTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** App-wide media controller bound to [PlaybackService]. */
object PlayerHolder {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    var controller: MediaController? = null
        private set

    private val _queue = MutableStateFlow<List<MusicTrack>>(emptyList())
    val queue: StateFlow<List<MusicTrack>> get() = _queue

    private val _currentId = MutableStateFlow<Int?>(null)
    val currentId: StateFlow<Int?> get() = _currentId

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> get() = _isPlaying

    private val _shuffleOn = MutableStateFlow(false)
    val shuffleOn: StateFlow<Boolean> get() = _shuffleOn

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> get() = _repeatMode

    fun init(context: Context) {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        _isPlaying.value = player.isPlaying
                        _currentId.value = player.currentMediaItem?.mediaId?.toIntOrNull()
                        _shuffleOn.value = player.shuffleModeEnabled
                        _repeatMode.value = player.repeatMode
                    }
                })
            } catch (_: Exception) {
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun currentTrack(): MusicTrack? = _currentId.value?.let { id -> _queue.value.find { it.id == id } }

    private fun MusicTrack.toMediaItem(): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist.ifBlank { username })
            .apply { Api.mediaUrl(cover)?.let { setArtworkUri(Uri.parse(it)) } }
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(Api.mediaUrl(filepath))
            .setMediaMetadata(meta)
            .build()
    }

    fun play(tracks: List<MusicTrack>, index: Int) {
        val c = controller ?: return
        if (tracks.isEmpty() || index !in tracks.indices) return
        _queue.value = tracks
        c.setMediaItems(tracks.map { it.toMediaItem() }, index, 0L)
        c.prepare()
        c.play()
    }

    fun toggle() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.seekToPreviousMediaItem() }
    fun seekTo(positionMs: Long) { controller?.seekTo(positionMs) }

    fun toggleShuffle() {
        controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled }
    }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    fun position(): Pair<Long, Long> {
        val c = controller ?: return 0L to 0L
        return c.currentPosition.coerceAtLeast(0L) to c.duration.coerceAtLeast(0L)
    }

    fun stopAndClear() {
        controller?.let {
            it.stop()
            it.clearMediaItems()
        }
        _queue.value = emptyList()
        _currentId.value = null
        _isPlaying.value = false
    }
}
