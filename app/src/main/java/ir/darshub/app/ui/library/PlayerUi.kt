@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.darshub.app.ui.library

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import ir.darshub.app.core.Api
import ir.darshub.app.core.fa
import ir.darshub.app.data.MusicTrack
import ir.darshub.app.player.PlayerHolder
import ir.darshub.app.ui.theme.DarsMotion
import ir.darshub.app.ui.theme.brandGradient
import ir.darshub.app.ui.theme.pressScale
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

/** چهار میلهٔ اکولایزر فنری — فقط وقتی پخش فعال است. */
@Composable
private fun EqualizerBars(active: Boolean, tint: Color) {
    val t = rememberInfiniteTransition(label = "eq")
    val h1 by t.animateFloat(0.4f, 1f, infiniteRepeatable(tween(520, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "eq1")
    val h2 by t.animateFloat(0.9f, 0.35f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(120)), label = "eq2")
    val h3 by t.animateFloat(0.5f, 1f, infiniteRepeatable(tween(640, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(240)), label = "eq3")
    val h4 by t.animateFloat(1f, 0.45f, infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(60)), label = "eq4")
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
        listOf(h1, h2, h3, h4).forEach { h ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(12.dp)
                    .graphicsLayer { scaleY = if (active) h else 0.35f }
                    .background(tint.copy(alpha = if (active) 0.9f else 0.35f), CircleShape)
            )
        }
    }
}

/** مینی‌پلیر شناور ۲۰۲۶: شیشه، سایهٔ رنگی، دکمهٔ گرادیانی و اکولایزر زنده. */
@Composable
fun MiniPlayer() {
    val queue by PlayerHolder.queue.collectAsState()
    val currentId by PlayerHolder.currentId.collectAsState()
    val isPlaying by PlayerHolder.isPlaying.collectAsState()
    val track = queue.find { it.id == currentId } ?: return
    var showFull by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .shadow(
                14.dp, RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            ),
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
            EqualizerBars(isPlaying, MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            val playInter = remember { MutableInteractionSource() }
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                onClick = { PlayerHolder.toggle() },
                interactionSource = playInter,
                modifier = Modifier
                    .size(40.dp)
                    .pressScale(playInter, pressedScale = 0.88f)
                    .shadow(
                        8.dp, CircleShape,
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    ),
            ) {
                Box(Modifier.background(brandGradient()), contentAlignment = Alignment.Center) {
                    Icon(
                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "پخش/توقف",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }

    if (showFull) FullPlayerSheet(onDismiss = { showFull = false })
}

private fun fmtMs(ms: Long): String {
    if (ms <= 0) return "۰:۰۰"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d", m, s).fa()
}

/** پلیر کامل: کاور بزرگ با هاله، کنترل‌ها و اسلایدر گرادیانی. */
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
            Box {
                TrackCover(track, 230.dp, corner = 30.dp)
                Box(
                    Modifier
                        .size(230.dp)
                        .graphicsLayer { alpha = if (isPlaying) 0.5f else 0.15f }
                        .background(
                            androidx.compose.ui.graphics.Brush.radialGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    androidx.compose.ui.graphics.Color.Transparent,
                                )
                            ),
                            RoundedCornerShape(30.dp),
                        )
                )
            }
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
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                ),
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
                val playInter = remember { MutableInteractionSource() }
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    onClick = { PlayerHolder.toggle() },
                    interactionSource = playInter,
                    modifier = Modifier
                        .pressScale(playInter, pressedScale = 0.9f)
                        .shadow(
                            16.dp, CircleShape,
                            ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                            spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        ),
                ) {
                    Box(Modifier.background(brandGradient())) {
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
