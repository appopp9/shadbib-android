@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.shadbib.app.ui.messages
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.draw.clip

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.NavBus
import ir.shadbib.app.core.Store
import ir.shadbib.app.data.ChatMessage
import ir.shadbib.app.data.FriendDetail
import ir.shadbib.app.core.fa
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.LoadingBox
import ir.shadbib.app.ui.components.ProgressRow
import ir.shadbib.app.ui.components.StatPill
import ir.shadbib.app.ui.components.userColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import ir.shadbib.app.ui.components.GlassMenu
import ir.shadbib.app.ui.components.GlassAction
import ir.shadbib.app.ui.components.GlassDivider
import ir.shadbib.app.ui.components.GlassReactions
import androidx.compose.material.icons.rounded.Download

class ChannelChatViewModel : ViewModel() {
    var channel: String = "public"
    private fun target() = mapOf("channel" to channel)
    val messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val loading = MutableStateFlow(true)
    val uploadPercent = MutableStateFlow<Int?>(null)
    val canPost = MutableStateFlow(true)

    fun bind(ch: String) { channel = ch; canPost.value = (ch != "news") || (Store.username == "habib") }

    fun poll() {
        viewModelScope.launch {
            runCatching { ChatMessage.list(Api.arr(Api.get("chat", "channel" to channel))) }
                .onSuccess { messages.value = it; loading.value = false }.onFailure { loading.value = false }
        }
    }

    fun send(text: String, replyTo: Int?, onErr: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val b = JSONObject().put("message", text).put("channel", channel)
                if (replyTo != null) b.put("reply_to", replyTo)
                Api.post("chat", b)
            }.onFailure { onErr(it.message ?: "خطا") }
            poll()
        }
    }

    private fun sendMediaUri(ctx: Context, type: String, uri: Uri, onErr: (String) -> Unit) {
        viewModelScope.launch {
            uploadPercent.value = 0
            val err = ChatMedia.sendMedia(ctx, "channel", target(), type, uri, null) { uploadPercent.value = it }
            uploadPercent.value = null
            if (err != null) onErr(err); poll()
        }
    }
    fun sendImage(ctx: Context, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, "image", uri, onErr)
    fun sendFile(ctx: Context, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, "file", uri, onErr)
    fun sendMusicFile(ctx: Context, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, "music", uri, onErr)
    fun sendMusic(id: Int, onErr: (String) -> Unit) { viewModelScope.launch { ChatMedia.sendRef("channel", target(), "music", musicId = id)?.let(onErr); poll() } }
    fun sendSticker(id: Int, onErr: (String) -> Unit) { viewModelScope.launch { ChatMedia.sendRef("channel", target(), "sticker", stickerId = id)?.let(onErr); poll() } }
    fun react(msgId: Int, emoji: String) { viewModelScope.launch { runCatching { Api.post("react", JSONObject().put("scope", "channel").put("id", msgId).put("emoji", emoji)) }; poll() } }

    fun delete(id: Int) { viewModelScope.launch { runCatching { Api.delete("chat", "id" to id.toString()) }; poll() } }
}

@Composable
fun ChannelChatScreen(channel: String, title: String, emoji: String, onBack: () -> Unit, onOpenDm: (String) -> Unit = { NavBus.requestDm(it) }, vm: ChannelChatViewModel = viewModel(key = "ch_" + channel)) {
    val ctx = LocalContext.current
    val me = Store.username ?: ""
    val messages by vm.messages.collectAsState()
    val loading by vm.loading.collectAsState()
    val canPost by vm.canPost.collectAsState()
    val uploadPercent by vm.uploadPercent.collectAsState()
    val prefs by Store.prefs.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val muteKey = "ch:$channel"
    val muted = prefs.muted.contains(muteKey)

    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<ChatMessage?>(null) }
    var actionMsg by remember { mutableStateOf<ChatMessage?>(null) }
    var profileUser by remember { mutableStateOf<String?>(null) }
    var viewImage by remember { mutableStateOf<String?>(null) }
    var showAttach by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.sendImage(ctx, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.sendFile(ctx, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }
    val musicFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.sendMusicFile(ctx, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }

    LaunchedEffect(channel) { vm.bind(channel); vm.poll() }
    LaunchedEffect(channel) { while (true) { vm.poll(); delay(3000) } }
    LaunchedEffect(messages.size) { val n = messages.size; if (n > 0) runCatching { listState.animateScrollToItem(n - 1) } }

    // ریپلای «ندیده» — بدون تگ/منشن؛ فقط برای پیام‌هایی که کسی به من ریپلای کرده (تلگرامی، ماندگار روی دستگاه)
    var replySeenId by remember { mutableStateOf(0) }
    LaunchedEffect(channel) { replySeenId = Store.channelReplySeen(channel) }
    var flashId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(flashId) { if (flashId != null) { delay(2500); flashId = null } }

    Column(Modifier.fillMaxSize().imePadding()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                Text("$emoji  $title", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { Store.toggleMute(muteKey) }) {
                    Icon(Icons.Rounded.NotificationsOff, "بی‌صدا", tint = if (muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val showJump by remember { androidx.compose.runtime.derivedStateOf { val li = listState.layoutInfo; li.totalItemsCount > 0 && (li.visibleItemsInfo.lastOrNull()?.index ?: 0) < li.totalItemsCount - 3 } }
            when {
                loading -> LoadingBox(height = 200.dp)
                messages.isEmpty() -> EmptyState("💬", if (canPost) "هنوز پیامی نیست — اولین نفر باش!" else "هنوز پیامی نیست")
                else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(messages, key = { it.id }) { m ->
                        Box(Modifier.animateItemPlacement()) {
                            val bg by androidx.compose.animation.animateColorAsState(
                                if (flashId == m.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                                androidx.compose.animation.core.tween(500), label = "flash")
                            Box(Modifier.background(bg, RoundedCornerShape(14.dp))) {
                                ChannelBubble(m, m.sender == me, onLongPress = { actionMsg = m }, onAvatar = { if (m.sender != me) profileUser = m.sender }, onImageClick = { viewImage = it })
                            }
                        }
                    }
                }
            }
            run {
                val replyIdx = messages.indexOfLast { mm -> mm.sender != me && mm.replySender == me }
                val replyMsgId = if (replyIdx >= 0) messages[replyIdx].id else 0
                LaunchedEffect(replyMsgId) {
                    if (replyMsgId > 0) androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
                        .collect { vis ->
                            if (replyIdx in vis && replyMsgId > replySeenId) {
                                replySeenId = replyMsgId
                                Store.setChannelReplySeen(channel, replyMsgId)
                            }
                        }
                }
                if (replyIdx >= 0 && replyMsgId > replySeenId) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp,
                        onClick = {
                            replySeenId = replyMsgId
                            Store.setChannelReplySeen(channel, replyMsgId)
                            flashId = replyMsgId
                            scope.launch { listState.animateScrollToItem(replyIdx) }
                        }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 14.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Rounded.Reply, "ریپلای جدید", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("پاسخ به تو", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            JumpToBottom(visible = showJump) { scope.launch { runCatching { listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0)) } } }
        }
        uploadPercent?.let { UploadBar(it) }
        if (replyTo != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("پاسخ به ${replyTo?.sender}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(replyTo?.message ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { replyTo = null }) { Icon(Icons.Rounded.Close, "بستن", modifier = Modifier.size(18.dp)) }
                }
            }
        }
        if (canPost) {
            ChatInputBar(
                value = input, onValueChange = { input = it }, placeholder = "پیامت رو بنویس…",
                onAttach = { showAttach = true },
                onSend = { val t = input.trim(); if (t.isNotEmpty()) { vm.send(t, replyTo?.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }; input = ""; replyTo = null } },
            )
        } else {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Text("📢 فقط ادمین می‌تواند در این کانال پیام بدهد", modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (showAttach) AttachSheet(
        onImage = { showAttach = false; imagePicker.launch("image/*") },
        onFile = { showAttach = false; filePicker.launch("*/*") },
        onMusic = { showAttach = false; showMusic = true },
        onMusicDevice = { showAttach = false; musicFilePicker.launch("audio/*") },
        onSticker = { showAttach = false; showStickers = true },
        onDismiss = { showAttach = false })
    if (showStickers) StickerPickerSheet(onPick = { st -> showStickers = false; vm.sendSticker(st.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }, onDismiss = { showStickers = false })
    if (showMusic) ChatMusicPicker(onPick = { t -> showMusic = false; vm.sendMusic(t.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }, onDismiss = { showMusic = false })

    actionMsg?.let { m ->
        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
        GlassMenu(onDismiss = { actionMsg = null }) {
            GlassReactions(mineEmoji = m.reactions.firstOrNull { it.mine }?.emoji) { em -> vm.react(m.id, em) }
            GlassDivider()
            if (canPost) GlassAction(Icons.AutoMirrored.Rounded.Send, "پاسخ") { replyTo = m }
            if (m.sender != me) GlassAction(Icons.AutoMirrored.Rounded.Chat, "پروفایل ${m.sender}") { profileUser = m.sender }
            if (m.message.isNotBlank()) GlassAction(Icons.Rounded.ContentCopy, "کپی متن") {
                clip.setText(androidx.compose.ui.text.AnnotatedString(m.message))
            }
            if (m.sender == me) GlassAction(Icons.Rounded.Close, "حذف پیام", danger = true) { vm.delete(m.id) }
        }
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
    if (profileUser != null) {
        val pu = profileUser!!
        UserProfileSheet(username = pu, onDismiss = { profileUser = null }, onMessage = { profileUser = null; onOpenDm(pu) })
    }
}

@Composable
private fun ChannelBubble(m: ChatMessage, isMine: Boolean, onLongPress: () -> Unit, onAvatar: () -> Unit, onImageClick: (String) -> Unit) {
    val plain = m.type == "sticker" || m.type == "gif"
    ChatRow(isMine = isMine, avatar = { Box(Modifier.clickable { onAvatar() }) { Avatar(m.sender, m.senderMood, size = 32.dp, avatarUrl = m.senderAvatar) } }) {
        if (plain) {
            Box(Modifier.combinedClickable(onClick = onLongPress, onLongClick = onLongPress)) { MediaBody(m.type, m.mediaPath, m.fileName, m.fileSize, m.duration, null, onImageClick, onLongPress = onLongPress) }
        } else BubbleBox(isMine) {
            if (!isMine) Text(m.sender, style = MaterialTheme.typography.labelMedium, color = userColor(m.sender), modifier = Modifier.clickable { onAvatar() })
            if (m.replyMessage != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 3.dp)) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(m.replySender ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(m.replyMessage, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Box(Modifier.combinedClickable(onClick = onLongPress, onLongClick = onLongPress)) {
                if (m.type == "text") Text(m.message, style = MaterialTheme.typography.bodyMedium)
                else MediaBody(m.type, m.mediaPath, m.fileName, m.fileSize, m.duration, m.message, onImageClick, onLongPress = onLongPress)
            }
            if (m.reactions.isNotEmpty()) {
                Row(Modifier.padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    m.reactions.forEach { r ->
                        Surface(shape = CircleShape,
                            color = if (r.mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = if (r.mine) 0.3f else 0.12f))) {
                            Text((r.emoji + " " + (if (r.count > 1) r.count.fa() else "")).trim(),
                                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                        }
                    }
                }
            }
            Text(Fmt.timeOf(m.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), modifier = Modifier.align(Alignment.End))
        }
    }
}

@Composable
fun UserProfileSheet(username: String, onDismiss: () -> Unit, onMessage: () -> Unit) {
    // مستقیم صفحه اجتماعی کاربر باز می‌شود — بدون شیت واسطه
    ir.shadbib.app.ui.feed.SocialProfileDialog(username = username, onDismiss = onDismiss)
}
