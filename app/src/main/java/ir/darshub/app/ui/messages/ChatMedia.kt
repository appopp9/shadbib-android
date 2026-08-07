@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.darshub.app.ui.messages

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ir.darshub.app.core.Api
import ir.darshub.app.core.Store
import ir.darshub.app.core.fa
import ir.darshub.app.data.MusicTrack
import ir.darshub.app.data.Sticker
import ir.darshub.app.ui.media.MediaViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Shared chat media helpers + composables used by DM, group, and channel chats. */
object ChatMedia {
    /** target: dm -> {"to":username}, group -> {"group_id":id}, channel -> {"channel":key} */
    suspend fun sendMedia(ctx: Context, scope: String, target: Map<String, String>, type: String, uri: Uri, duration: Int?, onProgress: (Int) -> Unit): String? {
        return try {
            val part = withContext(Dispatchers.IO) {
                val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes() ?: throw Exception("خطا در خواندن فایل")
                val name = ir.darshub.app.ui.library.fileNameOf(ctx, uri).ifBlank { "file" }
                val mime = ctx.contentResolver.getType(uri) ?: "application/octet-stream"
                Api.FilePart("file", name, bytes, mime)
            }
            val fields = HashMap<String, String>()
            fields["scope"] = scope; fields["type"] = type
            fields.putAll(target)
            if (duration != null) fields["duration"] = duration.toString()
            Api.upload("send_media", fields, listOf(part), onProgress)
            null
        } catch (e: Exception) { e.message ?: "خطا در ارسال" }
    }

    suspend fun sendRef(scope: String, target: Map<String, String>, kind: String, musicId: Int? = null, stickerId: Int? = null, code: String? = null): String? {
        return try {
            val b = JSONObject().put("scope", scope).put("kind", kind)
            target.forEach { (k, v) -> b.put(k, v) }
            if (musicId != null) b.put("music_id", musicId)
            if (stickerId != null) b.put("sticker_id", stickerId)
            if (code != null) b.put("code", code)
            Api.post("send_ref", b)
            null
        } catch (e: Exception) { e.message ?: "خطا در ارسال" }
    }

    fun autoOk(type: String, sizeBytes: Int?): Boolean {
        val p = Store.prefs.value
        val mb = (sizeBytes ?: 0) / (1024 * 1024)
        return when (type) {
            "image", "gif" -> p.adImages && (sizeBytes == null || mb <= p.adMaxMb)
            "voice" -> p.adVoice
            "music" -> true
            "sticker" -> true
            "file" -> false // فایل همیشه با لمس باز/دانلود می‌شود
            else -> true
        }
    }

    /** باز کردن URL با برنامه مناسب (مرورگر / نمایشگر). */
    fun openUrl(ctx: Context, url: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        }.onFailure { Toast.makeText(ctx, "برنامه‌ای برای باز کردن پیدا نشد", Toast.LENGTH_SHORT).show() }
    }

    /** دانلود فایل در پوشه Downloads با نوتیفیکیشن سیستم. */
    fun download(ctx: Context, url: String?, name: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val safeName = (name ?: url.substringAfterLast('/')).ifBlank { "darshub_" + System.currentTimeMillis() }
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle(safeName)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, safeName)
            dm.enqueue(req)
            Toast.makeText(ctx, "دانلود شروع شد ⬇️ (پوشه Downloads)", Toast.LENGTH_SHORT).show()
        }.onFailure { openUrl(ctx, url) }
    }
}

private fun humanSize(bytes: Int?): String {
    val b = bytes ?: return ""
    return if (b >= 1024 * 1024) "${(b / (1024 * 1024)).fa()} مگابایت" else "${(b / 1024).coerceAtLeast(1).fa()} کیلوبایت"
}

/** Renders a media message body. Every element supports long-press (for reply/delete menu),
 *  files open with a tap and download with the download icon. */
@Composable
fun MediaBody(
    type: String,
    mediaPath: String?,
    fileName: String?,
    fileSize: Int?,
    duration: Int?,
    caption: String?,
    onOpenImage: (String) -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var loaded by remember(mediaPath) { mutableStateOf(ChatMedia.autoOk(type, fileSize)) }
    val url = Api.mediaUrl(mediaPath)
    val lp = onLongPress ?: {}
    when (type) {
        "sticker", "gif" -> {
            if (url != null) AsyncImage(model = url, contentDescription = type,
                modifier = Modifier.size(140.dp).combinedClickable(onClick = {}, onLongClick = lp))
        }
        "image" -> {
            if (loaded && url != null) {
                AsyncImage(model = url, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.widthIn(max = 240.dp).height(240.dp).clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .combinedClickable(onClick = { onOpenImage(mediaPath!!) }, onLongClick = lp))
            } else {
                DownloadTile("📷 عکس", humanSize(fileSize), onLongPress = lp) { loaded = true }
            }
            if (!caption.isNullOrBlank()) { Spacer(Modifier.height(4.dp)); Text(caption, style = MaterialTheme.typography.bodyMedium) }
        }
        "video" -> {
            var vfull by remember(mediaPath) { mutableStateOf(false) }
            Surface(shape = RoundedCornerShape(14.dp), color = Color.Black.copy(alpha = 0.85f),
                modifier = Modifier.size(width = 240.dp, height = 190.dp)
                    .combinedClickable(onClick = { vfull = true }, onLongClick = lp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.PlayArrow, "پخش ویدئو", tint = Color.White, modifier = Modifier.size(46.dp))
                }
            }
            if (vfull) MediaViewer(mediaPath, "video") { vfull = false }
            if (!caption.isNullOrBlank()) { Spacer(Modifier.height(4.dp)); Text(caption, style = MaterialTheme.typography.bodyMedium) }
        }
        "voice" -> AudioBubble(url, duration ?: 0, "🎤", onLongPress = lp)
        "music" -> {
            AudioBubble(url, duration ?: 0, "🎵", onLongPress = lp)
            if (!caption.isNullOrBlank()) Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        "file" -> {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(min = 190.dp)
                .combinedClickable(onClick = { ChatMedia.openUrl(ctx, url) }, onLongClick = lp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                    Icon(Icons.Rounded.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(fileName ?: "فایل", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(listOf(humanSize(fileSize), "لمس = باز کردن").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { ChatMedia.download(ctx, url, fileName) }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Download, "دانلود", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            if (!caption.isNullOrBlank()) { Spacer(Modifier.height(2.dp)); Text(caption, style = MaterialTheme.typography.bodyMedium) }
        }
        else -> Text(caption ?: "", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun DownloadTile(label: String, size: String, onLongPress: () -> Unit = {}, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        modifier = Modifier.size(width = 200.dp, height = 120.dp).combinedClickable(onClick = onClick, onLongClick = onLongPress)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Rounded.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text("$label — دانلود", style = MaterialTheme.typography.labelLarge)
            if (size.isNotBlank()) Text(size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** پلیر صوتی سبک: MediaPlayer فقط وقتی کاربر پخش را بزند ساخته/آماده می‌شود
 *  (نه هنگام نمایش لیست) — چت را روان می‌کند و کرش نمی‌دهد. */
@Composable
fun AudioBubble(url: String?, duration: Int, icon: String, onLongPress: (() -> Unit)? = null) {
    var player by remember(url) { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember(url) { mutableStateOf(false) }
    var preparing by remember(url) { mutableStateOf(false) }
    var prepared by remember(url) { mutableStateOf(false) }
    var progress by remember(url) { mutableLongStateOf(0L) }
    var totalMs by remember(url) { mutableLongStateOf((duration * 1000L).coerceAtLeast(1L)) }

    DisposableEffect(url) { onDispose { runCatching { player?.release() }; player = null } }
    LaunchedEffect(playing) { while (playing) { progress = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L); delay(200) } }

    fun toggle() {
        val p = player
        if (p == null) {
            if (url == null || preparing) return
            preparing = true
            val np = MediaPlayer()
            player = np
            runCatching {
                np.setOnPreparedListener {
                    prepared = true; preparing = false
                    totalMs = runCatching { np.duration.toLong() }.getOrDefault(duration * 1000L).coerceAtLeast(1L)
                    runCatching { np.start() }; playing = true
                }
                np.setOnCompletionListener { playing = false; progress = 0 }
                np.setOnErrorListener { _, _, _ -> preparing = false; playing = false; true }
                np.setDataSource(url)
                np.prepareAsync()
            }.onFailure { preparing = false; playing = false }
        } else if (prepared) {
            if (playing) { runCatching { p.pause() }; playing = false }
            else { runCatching { p.start() }; playing = true }
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(2.dp).widthIn(min = 170.dp)
            .combinedClickable(onClick = { toggle() }, onLongClick = onLongPress ?: {})) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, onClick = { toggle() }) {
            Box(Modifier.padding(8.dp).size(20.dp), contentAlignment = Alignment.Center) {
                if (preparing) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "پخش", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            LinearProgressIndicator(progress = { (progress.toFloat() / totalMs).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(4.dp))
            Spacer(Modifier.height(4.dp))
            Text("$icon ${clockMin(if (playing || prepared) (progress / 1000).toInt() else duration)}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** نوار ورودی مشترک و مدرن چت‌ها: فیلد گرد + دکمه ارسال/میکروفون انیمیشنی. */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    onAttach: () -> Unit,
    onSend: () -> Unit,
    onMic: (() -> Unit)? = null,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), onClick = onAttach) {
                Icon(Icons.Rounded.Add, "پیوست", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp).size(22.dp))
            }
            TextField(
                value = value, onValueChange = onValueChange,
                placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
                shape = RoundedCornerShape(24.dp), maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f),
            )
            val micMode = value.isBlank() && onMic != null
            Surface(shape = CircleShape, color = Color.Transparent, shadowElevation = 3.dp,
                modifier = Modifier.clip(CircleShape).background(ir.darshub.app.ui.theme.brandGradient()),
                onClick = { if (micMode) onMic!!() else onSend() }) {
                AnimatedContent(targetState = micMode, transitionSpec = { (scaleIn(tween(150)) + fadeIn(tween(150))).togetherWith(scaleOut(tween(120)) + fadeOut(tween(120))) }, label = "sendmic") { mic ->
                    Icon(if (mic) Icons.Rounded.Mic else Icons.AutoMirrored.Rounded.Send, "ارسال",
                        tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(13.dp).size(22.dp))
                }
            }
        }
    }
}

/** دکمه شناور «پرش به آخرین پیام» وقتی کاربر بالا اسکرول کرده. */
@Composable
fun androidx.compose.foundation.layout.BoxScope.JumpToBottom(visible: Boolean, onClick: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut(),
        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), onClick = onClick) {
            Icon(Icons.Rounded.KeyboardArrowDown, "پایین", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
        }
    }
}

/** Attach menu: image / file / music / sticker. */
@Composable
fun AttachSheet(onImage: () -> Unit, onFile: () -> Unit, onMusic: () -> Unit, onMusicDevice: () -> Unit, onSticker: () -> Unit, onDismiss: () -> Unit) {
    var musicExpand by remember { mutableStateOf(false) }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 18.dp).padding(bottom = 26.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AttachCircle("عکس", Icons.Rounded.Image, Color(0xFF34D399), Color(0xFF0EA5E9)) { onImage() }
                AttachCircle("فایل و جزوه", Icons.Rounded.Description, Color(0xFFA78BFA), Color(0xFF6366F1)) { onFile() }
                AttachCircle("موزیک", Icons.Rounded.MusicNote, Color(0xFFFB7185), Color(0xFFF59E0B)) { musicExpand = !musicExpand }
                AttachCircle("استیکر", Icons.Rounded.EmojiEmotions, Color(0xFFFBBF24), Color(0xFFFB923C)) { onSticker() }
            }
            androidx.compose.animation.AnimatedVisibility(visible = musicExpand) {
                Column(Modifier.padding(top = 14.dp)) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = { onMusic() }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp)); Text("از کتابخانه درس هاب", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = { onMusicDevice() }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LibraryMusic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp)); Text("از فایل‌های دستگاه", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachCircle(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, c1: Color, c2: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(6.dp)) {
        Box(Modifier.size(58.dp).background(Brush.linearGradient(listOf(c1, c2)), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AttachItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), onClick = onClick, modifier = Modifier.size(width = 96.dp, height = 84.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Sticker/GIF picker. */
@Composable
fun StickerPickerSheet(onPick: (Sticker) -> Unit, onDismiss: () -> Unit) {
    var stickers by remember { mutableStateOf<List<Sticker>>(emptyList()) }
    LaunchedEffect(Unit) { runCatching { Sticker.list(Api.arr(Api.get("sticker_list"))) }.onSuccess { stickers = it } }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 20.dp)) {
            Text("استیکر و گیف", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(340.dp)) {
                items(stickers, key = { it.id }) { st ->
                    AsyncImage(model = Api.mediaUrl(st.filepath), contentDescription = st.type,
                        modifier = Modifier.padding(6.dp).size(76.dp).combinedClickable(onClick = { onPick(st) }, onLongClick = {}))
                }
            }
        }
    }
}

/** Pick a track from the library to send into a chat. */
@Composable
fun ChatMusicPickerInline(onPick: (MusicTrack) -> Unit) {
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    LaunchedEffect(Unit) { runCatching { MusicTrack.list(Api.arr(Api.get("music_list", "sort" to "newest"))) }.onSuccess { tracks = it } }
    Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
        tracks.forEach { t ->
            Surface(color = Color.Transparent, onClick = { onPick(t) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${t.artist} · ${t.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMusicPicker(onPick: (MusicTrack) -> Unit, onDismiss: () -> Unit) {
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    LaunchedEffect(Unit) { runCatching { MusicTrack.list(Api.arr(Api.get("music_list", "sort" to "newest"))) }.onSuccess { tracks = it } }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("ارسال موزیک", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                tracks.forEach { t ->
                    Surface(color = Color.Transparent, onClick = { onPick(t) }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${t.artist} · ${t.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Small upload progress bar shown above the input while sending. */
@Composable
fun UploadBar(percent: Int) {
    Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("در حال ارسال… ${percent.fa()}٪", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(10.dp))
            LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.weight(1f))
        }
    }
}

/** Unified chat row: others on the LEFT (avatar left of bubble), mine on the RIGHT. */
@Composable
fun ChatRow(
    isMine: Boolean,
    avatar: (@Composable () -> Unit)? = null,
    bubble: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 1.dp),
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (!isMine && avatar != null) { avatar(); Spacer(Modifier.width(6.dp)) }
            bubble()
        }
    }
}

/** Bubble container: گرادیان لطیف برای پیام‌های خودم، سایه نرم و گوشه‌های تلگرامی. */
@Composable
fun BubbleBox(isMine: Boolean, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val shape = if (isMine) RoundedCornerShape(20.dp, 6.dp, 20.dp, 20.dp) else RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp)
    // حباب من: گرادیان سبز→آبی برند (سبک تلگرام)؛ حباب مخاطب: سطح لایه‌ای با حاشیه نرم
    val bg = if (isMine) Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.primaryContainer,
        androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer, 0.65f),
    )) else Brush.linearGradient(listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
    ))
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl
    ) {
        Column(
            Modifier.widthIn(max = 300.dp).clip(shape).background(bg)
                .then(if (!isMine) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f), shape) else Modifier)
                .padding(horizontal = 11.dp, vertical = 8.dp),
            content = content,
        )
    }
}
