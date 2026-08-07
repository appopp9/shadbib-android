package ir.darshub.app.ui.media

import android.view.SurfaceView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import ir.darshub.app.core.Api
import ir.darshub.app.core.fa
import ir.darshub.app.player.PlayerHolder
import ir.darshub.app.ui.messages.ChatMedia
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * نمایشگر مدیای درون‌برنامه‌ای. هیچ محتوایی دیگر در مرورگر باز نمی‌شود.
 * عکس: بزرگنمایی با دو انگشت و دوباره‌لمس. ویدئو: ExoPlayer با کنترل‌های اختصاصی.
 */
@Composable
fun MediaViewer(
    mediaPath: String?,
    type: String,
    onDismiss: () -> Unit,
) {
    if (mediaPath.isNullOrBlank()) { onDismiss(); return }
    val url = Api.mediaUrl(mediaPath) ?: run { onDismiss(); return }
    val ctx = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (type == "video") VideoStage(url) else ImageStage(url)

            // نوار بالا: بستن و دانلود
            Row(
                Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundBtn(Icons.Rounded.Close, "بستن", onDismiss)
                Spacer(Modifier.weight(1f))
                RoundBtn(Icons.Rounded.Download, "دانلود") {
                    ChatMedia.download(ctx, url, mediaPath.substringAfterLast('/'))
                }
            }
        }
    }
}

@Composable
private fun ImageStage(url: String) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offX by remember { mutableFloatStateOf(0f) }
    var offY by remember { mutableFloatStateOf(0f) }
    val state = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        if (scale > 1f) { offX += pan.x; offY += pan.y } else { offX = 0f; offY = 0f }
    }
    val animScale by animateFloatAsState(scale, label = "zoom")
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .graphicsLayer {
                scaleX = animScale; scaleY = animScale
                translationX = offX; translationY = offY
            }
            .transformable(state)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offX = 0f; offY = 0f } else scale = 2.5f
                })
            },
    )
}

@Composable
private fun VideoStage(url: String) {
    val ctx = LocalContext.current
    var playing by remember { mutableStateOf(true) }
    var ended by remember { mutableStateOf(false) }
    var posMs by remember { mutableLongStateOf(0L) }
    var durMs by remember { mutableLongStateOf(0L) }

    val player = remember {
        // پخش موسیقی را متوقف کن تا دو صدا روی هم نیفتد
        runCatching { PlayerHolder.controller?.pause() }
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) ended = true
                if (state == Player.STATE_READY) ended = false
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }

    LaunchedEffect(player) {
        while (true) {
            posMs = player.currentPosition.coerceAtLeast(0L)
            durMs = player.duration.let { if (it > 0L) it else 0L }
            delay(400)
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { c -> SurfaceView(c).also { player.setVideoSurfaceView(it) } },
            modifier = Modifier.fillMaxSize(),
        )
        // لمس وسط = پخش/توقف
        Box(Modifier.fillMaxSize().clickable {
            if (ended) { player.seekTo(0L); player.play() } else if (playing) player.pause() else player.play()
        })

        val icon = if (ended) Icons.Rounded.Replay else if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow
        if (!playing || ended) {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(40.dp)) }
        }

        // نوار پایین: زمان و پیشروی
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f)).padding(14.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.25f)),
            ) {
                val frac = if (durMs > 0L) (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f) else 0f
                Box(
                    Modifier.fillMaxWidth(frac).height(5.dp)
                        .clip(RoundedCornerShape(3.dp)).background(Color(0xFF34D399)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(hms(posMs), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(hms(durMs), color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun RoundBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier.size(40.dp).clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(22.dp)) }
}

private fun hms(ms: Long): String {
    val s = (ms / 1000L).coerceAtLeast(0L)
    val h = s / 3600L
    val m = (s % 3600L) / 60L
    val sec = s % 60L
    val raw = if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.US, "%d:%02d", m, sec)
    return raw.fa()
}

/** بنرِ ویدئو در فید/چت: بندانگشتی + دکمهٔ پخش، لمس → پلیر درون‌برنامه‌ای */
@Composable
fun VideoThumb(
    mediaPath: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF101418)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = Api.mediaUrl(mediaPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.PlayArrow, "پخش", tint = Color.White, modifier = Modifier.size(32.dp)) }
    }
}
