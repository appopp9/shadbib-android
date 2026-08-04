@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.shadbib.app.ui.library
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.MusicTrack
import ir.shadbib.app.player.PlayerHolder
import ir.shadbib.app.ui.theme.brandGradient
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TrackCover(track: MusicTrack?, size: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp = 12.dp) {
    val cover = Api.mediaUrl(track?.cover)
    if (cover != null) {
        AsyncImage(
            model = cover,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(corner))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            Modifier.size(size).background(brandGradient(), RoundedCornerShape(corner)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

@Composable
fun MiniPlayer() {
    val queue by PlayerHolder.queue.collectAsState()
    val currentId by PlayerHolder.currentId.collectAsState()
    val isPlaying by PlayerHolder.isPlaying.collectAsState()
    val track = queue.find { it.id == currentId } ?: return
    var showFull by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier
                .clickable { showFull = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TrackCover(track, 42.dp, corner = 14.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(),
                )
                Text(
                    track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { PlayerHolder.prev() }) {
                Icon(Icons.Rounded.SkipPrevious, contentDescription = "قبلی")
            }
            Surface(shape = CircleShape, color = androidx.compose.ui.graphics.Color.Transparent, onClick = { PlayerHolder.toggle() }) {
                Box(Modifier.background(brandGradient())) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "پخش/توقف",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(8.dp).size(22.dp),
                    )
                }
            }
            IconButton(onClick = { PlayerHolder.next() }) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "بعدی")
            }
            IconButton(onClick = { PlayerHolder.stopAndClear() }) {
                Icon(Icons.Rounded.Close, contentDescription = "بستن", modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showFull) {
        FullPlayerSheet(onDismiss = { showFull = false })
    }
}

private fun fmtMs(ms: Long): String {
    if (ms <= 0) return "۰:۰۰"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d", m, s).fa()
}

@Composable
fun FullPlayerSheet(onDismiss: () -> Unit) {
    val queue by PlayerHolder.queue.collectAsState()
    val currentId by PlayerHolder.currentId.collectAsState()
    val isPlaying by PlayerHolder.isPlaying.collectAsState()
    val shuffle by PlayerHolder.shuffleOn.collectAsState()
    val repeat by PlayerHolder.repeatMode.collectAsState()
    val track = queue.find { it.id == currentId }

    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            val (p, d) = PlayerHolder.position()
            pos = p; dur = d
            delay(500)
        }
    }

    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TrackCover(track, 230.dp, corner = 30.dp)
            Spacer(Modifier.height(18.dp))
            Text(
                track?.title ?: "",
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
            Text(
                track?.artist ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "آپلود: ${track?.username ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(14.dp))
            Slider(
                value = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                onValueChange = { f -> if (dur > 0) PlayerHolder.seekTo((f * dur).toLong()) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtMs(pos), style = MaterialTheme.typography.labelSmall)
                Text(fmtMs(dur), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconButton(onClick = { PlayerHolder.toggleShuffle() }) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "شافل",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { PlayerHolder.next() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "بعدی", modifier = Modifier.size(34.dp))
                }
                Surface(
                    shape = CircleShape,
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    shadowElevation = 10.dp,
                    onClick = { PlayerHolder.toggle() },
                ) {
                    Box(Modifier.background(ir.shadbib.app.ui.theme.brandGradient())) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "پخش/توقف",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(18.dp).size(34.dp),
                        )
                    }
                }
                IconButton(onClick = { PlayerHolder.prev() }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "قبلی", modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = { PlayerHolder.cycleRepeat() }) {
                    Icon(
                        if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "تکرار",
                        tint = if (repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
