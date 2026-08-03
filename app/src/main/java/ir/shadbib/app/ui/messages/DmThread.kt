@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.shadbib.app.ui.messages

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.VoiceRecorder
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.DmMessage
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.LoadingBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ir.shadbib.app.ui.components.GlassMenu
import ir.shadbib.app.ui.components.GlassAction
import ir.shadbib.app.ui.components.GlassDivider
import ir.shadbib.app.ui.components.GlassReactions
import androidx.compose.material.icons.rounded.Download
import ir.shadbib.app.ui.components.ColumnScopeGlass

@Composable
fun DmThreadScreen(username: String, onBack: () -> Unit, vm: DmThreadViewModel = viewModel(key = "dm_" + username)) {
    val ctx = LocalContext.current
    val me = Store.username ?: ""
    val state by vm.state.collectAsState()
    val prefs by Store.prefs.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val muteKey = "dm:$username"
    val muted = prefs.muted.contains(muteKey)

    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<DmMessage?>(null) }
    var actionMsg by remember { mutableStateOf<DmMessage?>(null) }
    var forwardMsg by remember { mutableStateOf<DmMessage?>(null) }
    var editMsg by remember { mutableStateOf<DmMessage?>(null) }
    var viewImage by remember { mutableStateOf<String?>(null) }
    var showProfile by remember { mutableStateOf(false) }
    var showAttach by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }

    val recorder = remember { VoiceRecorder(ctx) }
    var recording by remember { mutableStateOf(false) }
    var recElapsed by remember { mutableIntStateOf(0) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.sendImage(ctx, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.sendFile(ctx, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }
    val musicFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.sendMusicFile(ctx, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { if (recorder.start()) { recording = true; recElapsed = 0 } } else Toast.makeText(ctx, "به مجوز میکروفون نیاز است", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(username) { vm.bind(username); vm.poll() }
    LaunchedEffect(username) { while (true) { vm.poll(); delay(4000) } }
    LaunchedEffect(state.messages.size) { val n = state.messages.size; if (n > 0) runCatching { listState.animateScrollToItem(n - 1) } }
    LaunchedEffect(recording) { while (recording) { recElapsed = recorder.elapsedSec(); delay(500) } }
    DisposableEffect(Unit) { onDispose { recorder.cancel() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                Box(Modifier.combinedClickable(onClick = { showProfile = true }, onLongClick = {})) {
                    Avatar(username, state.partner?.mood, size = 40.dp, online = state.partner?.isOnline, avatarUrl = state.partner?.avatar)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f).combinedClickable(onClick = { showProfile = true }, onLongClick = {})) {
                    Text(username, style = MaterialTheme.typography.titleSmall)
                    androidx.compose.animation.AnimatedContent(targetState = state.partnerTyping, label = "typing") { typing ->
                        Text(
                            when {
                                typing -> "در حال نوشتن… ✍️"
                                state.partner?.isOnline == true -> "آنلاین"
                                state.partner?.lastSeenText != null -> state.partner?.lastSeenText ?: ""
                                state.partner?.lastSeen != null -> "آخرین بازدید ${Fmt.relative(state.partner?.lastSeen)}"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (typing || state.partner?.isOnline == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = { Store.toggleMute(muteKey); Toast.makeText(ctx, if (!muted) "بی‌صدا شد 🔕" else "صدادار شد 🔔", Toast.LENGTH_SHORT).show() }) {
                    Icon(Icons.Rounded.NotificationsOff, "بی‌صدا", tint = if (muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val showJump by remember { androidx.compose.runtime.derivedStateOf { val li = listState.layoutInfo; li.totalItemsCount > 0 && (li.visibleItemsInfo.lastOrNull()?.index ?: 0) < li.totalItemsCount - 3 } }
            when {
                state.loading -> LoadingBox(height = 200.dp)
                state.messages.isEmpty() -> EmptyState("👋", "سلام بگو و گفتگو رو شروع کن!")
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    itemsIndexed(state.messages, key = { _, m -> m.id }) { idx, m ->
                        val prevDate = state.messages.getOrNull(idx - 1)?.createdAt?.substringBefore(' ')
                        val curDate = m.createdAt.substringBefore(' ')
                        Column(Modifier.animateItemPlacement()) {
                            if (prevDate != curDate) DateChip(Fmt.dayLabel(m.createdAt))
                            SwipeToReply(onReply = { replyTo = m }) {
                                DmBubble(m, m.sender == me, onTap = { actionMsg = m }, onLongPress = { actionMsg = m }, onImageClick = { viewImage = it }, onAvatar = { if (m.sender != me) showProfile = true })
                            }
                        }
                    }
                }
            }
            JumpToBottom(visible = showJump) { scope.launch { runCatching { listState.animateScrollToItem((state.messages.size - 1).coerceAtLeast(0)) } } }
        }

        state.uploadPercent?.let { UploadBar(it) }

        if (replyTo != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("پاسخ به ${replyTo?.sender}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(previewOf(replyTo), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { replyTo = null }) { Icon(Icons.Rounded.Close, "بستن", modifier = Modifier.size(18.dp)) }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface) {
            if (recording) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    IconButton(onClick = { recorder.cancel(); recording = false }) { Icon(Icons.Rounded.Delete, "لغو", tint = MaterialTheme.colorScheme.error) }
                    Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                    Text("در حال ضبط…  ${clockMin(recElapsed)}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, onClick = {
                        val res = recorder.stop(); recording = false
                        if (res != null) vm.sendVoice(res.first, res.second) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } else Toast.makeText(ctx, "ضبط خیلی کوتاه بود", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.AutoMirrored.Rounded.Send, "ارسال", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(12.dp).size(22.dp)) }
                }
            } else {
                ChatInputBar(
                    value = input, onValueChange = { input = it; if (it.isNotBlank()) vm.notifyTyping() }, placeholder = "پیام…",
                    onAttach = { showAttach = true },
                    onSend = {
                        val text = input.trim()
                        if (text.isNotEmpty()) { vm.sendText(text, replyTo?.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }; input = ""; replyTo = null }
                    },
                    onMic = {
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) { if (recorder.start()) { recording = true; recElapsed = 0 } }
                        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
            }
        }
    }

    if (showAttach) AttachSheet(
        onImage = { showAttach = false; imagePicker.launch("image/*") },
        onFile = { showAttach = false; filePicker.launch("*/*") },
        onMusic = { showAttach = false; showMusic = true },
        onMusicDevice = { showAttach = false; musicFilePicker.launch("audio/*") },
        onSticker = { showAttach = false; showStickers = true },
        onDismiss = { showAttach = false },
    )
    if (showStickers) StickerPickerSheet(onPick = { st -> showStickers = false; vm.sendSticker(st.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }, onDismiss = { showStickers = false })
    if (showMusic) ChatMusicPicker(onPick = { t -> showMusic = false; vm.sendMusic(t.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }, onDismiss = { showMusic = false })

    actionMsg?.let { m ->
        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
        val mine = m.sender == me
        GlassMenu(onDismiss = { actionMsg = null }) {
            GlassReactions(mineEmoji = m.reactions.firstOrNull { it.mine }?.emoji) { em -> vm.react(m.id, em) }
            GlassDivider()
            GlassAction(Icons.Rounded.Reply, "پاسخ") { replyTo = m }
            GlassAction(Icons.AutoMirrored.Rounded.Send, "هدایت") { forwardMsg = m }
            if (m.type == "text" && !m.message.isNullOrBlank()) {
                GlassAction(Icons.Rounded.ContentCopy, "کپی متن") {
                    clip.setText(androidx.compose.ui.text.AnnotatedString(m.message ?: ""))
                }
            }
            if (mine && m.type == "text") GlassAction(Icons.Rounded.Edit, "ویرایش") { editMsg = m }
            if (mine) GlassAction(Icons.Rounded.Delete, "حذف پیام", danger = true) { vm.delete(m.id) }
        }
    }

    if (editMsg != null) {
        var text by remember { mutableStateOf(editMsg?.message ?: "") }
        AlertDialog(onDismissRequest = { editMsg = null }, title = { Text("ویرایش پیام") },
            text = { OutlinedTextField(value = text, onValueChange = { text = it }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { vm.edit(editMsg!!.id, text) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }; editMsg = null }) { Text("ذخیره") } },
            dismissButton = { TextButton(onClick = { editMsg = null }) { Text("انصراف") } })
    }

    if (viewImage != null) {
        Dialog(onDismissRequest = { viewImage = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)), contentAlignment = Alignment.Center) {
                AsyncImage(model = Api.mediaUrl(viewImage), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                IconButton(onClick = { viewImage = null }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) { Icon(Icons.Rounded.Close, "بستن", tint = Color.White) }
                IconButton(onClick = { ChatMedia.download(ctx, Api.mediaUrl(viewImage), "shadbib_" + System.currentTimeMillis() + ".jpg") }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) { Icon(androidx.compose.material.icons.Icons.Rounded.Download, "دانلود", tint = Color.White) }
            }
        }
    }

    if (showProfile) {
        UserProfileSheet(username = username, onDismiss = { showProfile = false }, onMessage = { showProfile = false })
    }

    forwardMsg?.let { fm ->
        ForwardSheet(onPick = { toUser ->
            vm.forward(fm.id, toUser) { e -> Toast.makeText(ctx, e ?: "هدایت شد ↪", Toast.LENGTH_SHORT).show() }
            forwardMsg = null
        }, onDismiss = { forwardMsg = null })
    }
}

/** انتخاب گفتگو برای هدایت پیام (تلگرامی). */
@Composable
fun ForwardSheet(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var convs by remember { mutableStateOf<List<ir.shadbib.app.data.Conversation>?>(null) }
    LaunchedEffect(Unit) {
        runCatching { ir.shadbib.app.data.Conversation.list(Api.arr(Api.get("dm_conversations"))) }
            .onSuccess { convs = it }.onFailure { convs = emptyList() }
    }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text("هدایت به… ↪", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            when {
                convs == null -> LoadingBox(height = 120.dp)
                convs!!.isEmpty() -> EmptyState("✉️", "گفتگویی نداری")
                else -> Column(Modifier.height(360.dp).verticalScroll(rememberScrollState())) {
                    convs!!.forEach { c ->
                        Surface(color = Color.Transparent, onClick = { onPick(c.username) }, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(c.username, c.mood, size = 40.dp, avatarUrl = c.avatar)
                                Spacer(Modifier.width(10.dp))
                                Text(c.username, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** جداکننده تاریخ وسط چت. */
@Composable
fun DateChip(label: String) {
    if (label.isBlank()) return
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
        }
    }
}

/** سوایپ افقی روی حباب برای پاسخ سریع (تلگرامی). */
@Composable
fun SwipeToReply(onReply: () -> Unit, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val threshold = 130f
    Box(Modifier.fillMaxWidth()) {
        if (offset.value > 20f) {
            Icon(Icons.Rounded.Reply, "پاسخ", tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp).size(22.dp)
                    .alpha((offset.value / threshold).coerceIn(0f, 1f)))
        }
        Box(Modifier
            .draggable(
                state = rememberDraggableState { d ->
                    scope.launch { offset.snapTo((offset.value + d).coerceIn(0f, threshold + 30f)) }
                },
                orientation = Orientation.Horizontal,
                onDragStopped = {
                    if (offset.value > threshold * 0.7f) onReply()
                    scope.launch { offset.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 0.6f)) }
                })
            .graphicsLayer { translationX = offset.value }) { content() }
    }
}

fun previewOf(m: DmMessage?): String = when (m?.type) {
    "image" -> "📷 عکس"; "voice" -> "🎤 پیام صوتی"; "file" -> "📎 ${m.fileName ?: "فایل"}"
    "music" -> "🎵 ${m.message ?: "موزیک"}"; "sticker" -> "استیکر"; "gif" -> "GIF"
    else -> m?.message ?: ""
}

fun clockMin(sec: Int): String = String.format(java.util.Locale.US, "%d:%02d", sec / 60, sec % 60).fa()

@Composable
fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    Surface(color = Color.Transparent, onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DmBubble(m: DmMessage, isMine: Boolean, onTap: () -> Unit = {}, onLongPress: () -> Unit, onImageClick: (String) -> Unit, onAvatar: () -> Unit = {}) {
    val plain = m.type == "sticker" || m.type == "gif"
    ChatRow(isMine = isMine, avatar = { Box(Modifier.combinedClickable(onClick = onAvatar, onLongClick = {})) { Avatar(m.sender, size = 30.dp, avatarUrl = m.senderAvatar) } }) {
        if (plain) {
            Box(Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
                MediaBody(m.type, m.mediaPath, m.fileName, m.fileSize, m.duration, null, onImageClick, onLongPress = onLongPress)
            }
        } else {
            Box(Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
            BubbleBox(isMine) {
                if (m.replyMessage != null || m.replyType != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 4.dp)) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                            Text(m.replySender ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(when (m.replyType) { "image" -> "📷 عکس"; "voice" -> "🎤 پیام صوتی"; else -> m.replyMessage ?: "" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Box(Modifier.combinedClickable(onClick = onTap, onLongClick = onLongPress)) {
                    if (m.type == "text") Text(m.message ?: "", style = MaterialTheme.typography.bodyMedium)
                    else MediaBody(m.type, m.mediaPath, m.fileName, m.fileSize, m.duration, m.message, onImageClick, onLongPress = onLongPress)
                }
                if (m.reactions.isNotEmpty()) {
                    Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        m.reactions.forEach { r ->
                            Surface(shape = CircleShape,
                                color = if (r.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = if (r.mine) 0.3f else 0.12f))) {
                                Text("${r.emoji} ${if (r.count > 1) r.count.fa() else ""}".trim(),
                                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                            }
                        }
                    }
                }
                Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                    if (m.edited) { Text("ویرایش‌شده", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)); Spacer(Modifier.width(4.dp)) }
                    Text(Fmt.timeOf(m.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    if (isMine) {
                        Spacer(Modifier.width(3.dp))
                        Icon(if (m.isRead) Icons.Rounded.DoneAll else Icons.Rounded.Done, null, modifier = Modifier.size(15.dp),
                            tint = if (m.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            }
        }
    }
}
