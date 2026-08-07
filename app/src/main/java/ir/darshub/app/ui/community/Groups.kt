@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.darshub.app.ui.community

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonRemove
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ir.darshub.app.core.Api
import ir.darshub.app.core.Fmt
import ir.darshub.app.core.Store
import ir.darshub.app.core.fa
import ir.darshub.app.data.GroupDetail
import ir.darshub.app.data.GroupMessage
import ir.darshub.app.data.StudyGroup
import ir.darshub.app.ui.components.Avatar
import ir.darshub.app.ui.components.EmptyState
import ir.darshub.app.ui.components.GlassAction
import ir.darshub.app.ui.components.GlassDivider
import ir.darshub.app.ui.components.GlassMenu
import ir.darshub.app.ui.components.GlassReactions
import ir.darshub.app.ui.components.LoadingBox
import ir.darshub.app.ui.components.ProgressRow
import ir.darshub.app.ui.components.userColor
import ir.darshub.app.ui.messages.AttachSheet
import ir.darshub.app.ui.messages.BubbleBox
import ir.darshub.app.ui.messages.ChatInputBar
import ir.darshub.app.ui.messages.ChatMedia
import ir.darshub.app.ui.messages.ChatMusicPicker
import ir.darshub.app.ui.messages.ChatRow
import ir.darshub.app.ui.messages.JumpToBottom
import ir.darshub.app.ui.messages.MediaBody
import ir.darshub.app.ui.messages.SheetRow
import ir.darshub.app.ui.messages.StickerPickerSheet
import ir.darshub.app.ui.messages.UploadBar
import ir.darshub.app.ui.messages.UserProfileSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

class GroupsViewModel : ViewModel() {
    val groups = MutableStateFlow<List<StudyGroup>>(emptyList())
    val loading = MutableStateFlow(true)
    val detail = MutableStateFlow<GroupDetail?>(null)
    val messages = MutableStateFlow<List<GroupMessage>>(emptyList())
    val uploadPercent = MutableStateFlow<Int?>(null)
    private fun target(id: Int) = mapOf("group_id" to id.toString())

    fun refresh() {
        viewModelScope.launch {
            runCatching { StudyGroup.list(Api.arr(Api.get("group_list"))) }
                .onSuccess { groups.value = it; loading.value = false }.onFailure { loading.value = false }
        }
    }
    fun create(name: String, username: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { runCatching { Api.post("group_create", JSONObject().put("name", name).put("username", username)) }.onSuccess { onResult(null); refresh() }.onFailure { onResult(it.message ?: "خطا") } }
    }
    fun join(username: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { runCatching { Api.post("group_join", JSONObject().put("username", username)) }.onSuccess { onResult(null); refresh() }.onFailure { onResult(it.message ?: "خطا") } }
    }
    fun loadDetail(id: Int) { viewModelScope.launch { runCatching { GroupDetail.from(Api.obj(Api.get("group_detail", "id" to id.toString()))) }.onSuccess { detail.value = it } } }
    fun leave(id: Int, onDone: () -> Unit) { viewModelScope.launch { runCatching { Api.post("group_leave", JSONObject().put("group_id", id)) }; refresh(); onDone() } }
    fun deleteGroup(id: Int, onDone: () -> Unit) { viewModelScope.launch { runCatching { Api.get("group_delete", "id" to id.toString()) }; refresh(); onDone() } }
    fun kick(id: Int, username: String) { viewModelScope.launch { runCatching { Api.post("group_kick", JSONObject().put("group_id", id).put("username", username)) }; loadDetail(id) } }
    fun rename(id: Int, name: String) { viewModelScope.launch { runCatching { Api.post("group_update", JSONObject().put("group_id", id).put("name", name)) }; loadDetail(id); refresh() } }
    fun pollChat(id: Int) { viewModelScope.launch { runCatching { GroupMessage.list(Api.arr(Api.get("group_chat", "group_id" to id.toString()))) }.onSuccess { messages.value = it } } }
    fun sendChat(id: Int, text: String, replyTo: Int? = null, onErr: (String) -> Unit) { viewModelScope.launch { runCatching { val b = JSONObject().put("group_id", id).put("message", text); if (replyTo != null) b.put("reply_to", replyTo); Api.post("group_chat", b) }.onFailure { onErr(it.message ?: "خطا") }; pollChat(id) } }
    fun react(gid: Int, msgId: Int, emoji: String) { viewModelScope.launch { runCatching { Api.post("react", JSONObject().put("scope", "group").put("id", msgId).put("emoji", emoji)) }; pollChat(gid) } }

    fun deleteMsg(id: Int, msgId: Int) { viewModelScope.launch { runCatching { Api.delete("group_chat", "group_id" to id.toString(), "id" to msgId.toString()) }; pollChat(id) } }
    private fun sendMediaUri(ctx: Context, id: Int, type: String, uri: Uri, onErr: (String) -> Unit) {
        viewModelScope.launch { uploadPercent.value = 0; val e = ChatMedia.sendMedia(ctx, "group", target(id), type, uri, null) { uploadPercent.value = it }; uploadPercent.value = null; if (e != null) onErr(e); pollChat(id) }
    }
    fun sendImage(ctx: Context, id: Int, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, id, "image", uri, onErr)
    fun sendFile(ctx: Context, id: Int, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, id, "file", uri, onErr)
    fun sendMusicFile(ctx: Context, id: Int, uri: Uri, onErr: (String) -> Unit) = sendMediaUri(ctx, id, "music", uri, onErr)
    fun sendMusic(id: Int, musicId: Int, onErr: (String) -> Unit) { viewModelScope.launch { ChatMedia.sendRef("group", target(id), "music", musicId = musicId)?.let(onErr); pollChat(id) } }
    fun sendSticker(id: Int, stickerId: Int, onErr: (String) -> Unit) { viewModelScope.launch { ChatMedia.sendRef("group", target(id), "sticker", stickerId = stickerId)?.let(onErr); pollChat(id) } }
}

private sealed class GRoute {
    object List : GRoute()
    data class Detail(val id: Int) : GRoute()
    data class Chat(val id: Int, val name: String) : GRoute()
}

@Composable
fun GroupsTab(vm: GroupsViewModel = viewModel()) {
    var route by remember { mutableStateOf<GRoute>(GRoute.List) }
    LaunchedEffect(Unit) { vm.refresh() }
    when (val r = route) {
        is GRoute.List -> GroupsList(vm, onOpen = { route = GRoute.Detail(it) })
        is GRoute.Detail -> { BackHandler { route = GRoute.List }; GroupDetailScreen(vm, r.id, onBack = { route = GRoute.List }, onChat = { name -> route = GRoute.Chat(r.id, name) }) }
        is GRoute.Chat -> { BackHandler { route = GRoute.Detail(r.id) }; GroupChatScreen(r.id, r.name, onBack = { route = GRoute.Detail(r.id) }) }
    }
}

@Composable
private fun GroupsList(vm: GroupsViewModel, onOpen: (Int) -> Unit) {
    val ctx = LocalContext.current
    val groups by vm.groups.collectAsState()
    val loading by vm.loading.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showJoin by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showCreate = true }, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("گروه جدید") }
                OutlinedButton(onClick = { showJoin = true }, shape = MaterialTheme.shapes.medium, modifier = Modifier.weight(1f)) { Icon(Icons.Rounded.Groups, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("عضویت") }
            }
        }
        when {
            loading -> item { LoadingBox(height = 140.dp) }
            groups.isEmpty() -> item { EmptyState("👥", "هنوز عضو گروهی نیستی — یه گروه بساز یا با شناسه‌ی گروه عضو شو") }
            else -> items(groups, key = { it.id }) { g -> GroupCard(g) { onOpen(g.id) } }
        }
    }
    if (showCreate) CreateGroupDialog(onDismiss = { showCreate = false }, onCreate = { n, u -> vm.create(n, u) { err -> Toast.makeText(ctx, err ?: "گروه ساخته شد ✅", Toast.LENGTH_SHORT).show(); if (err == null) showCreate = false } })
    if (showJoin) JoinGroupDialog(onDismiss = { showJoin = false }, onJoin = { u -> vm.join(u) { err -> Toast.makeText(ctx, err ?: "عضو شدی 🎉", Toast.LENGTH_SHORT).show(); if (err == null) showJoin = false } })
}

@Composable
private fun GroupCard(g: StudyGroup, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)), onClick = onClick) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            GroupAvatar(g.avatar, g.username, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(g.name, style = MaterialTheme.typography.titleSmall); Text("@${g.username}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(g.memberCount.fa(), style = MaterialTheme.typography.titleMedium); Text("عضو", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
private fun GroupAvatar(avatar: String?, key: String, size: androidx.compose.ui.unit.Dp) {
    val url = Api.mediaUrl(avatar)
    if (url != null) AsyncImage(model = url, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(size).clip(CircleShape))
    else Surface(shape = CircleShape, color = userColor(key).copy(alpha = 0.18f), modifier = Modifier.size(size)) { Box(contentAlignment = Alignment.Center) { Text("👥", fontSize = (size.value * 0.42f).sp) } }
}

@Composable
private fun GroupDetailScreen(vm: GroupsViewModel, id: Int, onBack: () -> Unit, onChat: (String) -> Unit) {
    val ctx = LocalContext.current
    val detail by vm.detail.collectAsState()
    LaunchedEffect(id) { vm.loadDetail(id) }
    val d = detail
    var showEdit by remember { mutableStateOf(false) }
    var memberProfile by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
            Text(d?.name ?: "گروه", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (d != null) Button(onClick = { onChat(d.name) }, shape = MaterialTheme.shapes.medium) { Icon(Icons.AutoMirrored.Rounded.Send, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("چت گروه") }
        }
        if (d == null) { LoadingBox(height = 200.dp) } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            GroupAvatar(d.avatar, d.username, 72.dp)
                            Spacer(Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(d.name, style = MaterialTheme.typography.titleLarge)
                                if (d.isOwner) IconButton(onClick = { showEdit = true }) { Icon(Icons.Rounded.Edit, "ویرایش", modifier = Modifier.size(18.dp)) }
                            }
                            Text("@${d.username}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text("${d.memberCount.fa()} عضو · سازنده ${d.ownerName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item { Text("اعضا و مطالعه امروز", style = MaterialTheme.typography.titleSmall) }
                val maxM = (d.members.maxOfOrNull { it.todayMinutes } ?: 0).coerceAtLeast(1)
                items(d.members) { m ->
                    Row(Modifier.fillMaxWidth().clickable { memberProfile = m.username }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(m.username, m.mood, size = 40.dp, online = m.isOnline, avatarUrl = m.avatar)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.username, style = MaterialTheme.typography.bodyLarge)
                            ProgressRow("📖", "مطالعه امروز", m.todayMinutes, maxM, Fmt.minutes(m.todayMinutes), MaterialTheme.colorScheme.primary)
                        }
                        if (d.isOwner && m.username != d.ownerName) IconButton(onClick = { vm.kick(id, m.username) }) { Icon(Icons.Rounded.PersonRemove, "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp)) }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    if (d.isOwner) OutlinedButton(onClick = { vm.deleteGroup(d.id) { onBack() } }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Text("حذف گروه", color = MaterialTheme.colorScheme.error) }
                    else OutlinedButton(onClick = { vm.leave(d.id) { onBack() } }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Text("خروج از گروه", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        if (memberProfile != null) {
            val pu = memberProfile!!
            UserProfileSheet(username = pu, onDismiss = { memberProfile = null }, onMessage = { memberProfile = null; ir.darshub.app.core.NavBus.requestDm(pu) })
        }
        if (showEdit && d != null) {
            var name by remember { mutableStateOf(d.name) }
            AlertDialog(onDismissRequest = { showEdit = false }, title = { Text("ویرایش گروه") },
                text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام گروه") }, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) },
                confirmButton = { TextButton(onClick = { vm.rename(id, name.trim()); showEdit = false }) { Text("ذخیره") } },
                dismissButton = { TextButton(onClick = { showEdit = false }) { Text("انصراف") } })
        }
    }
}

/** Reusable group chat screen (also opened from the Messages tab). */
@Composable
fun GroupChatScreen(id: Int, name: String, onBack: () -> Unit, vm: GroupsViewModel = viewModel(key = "grpchat_" + id)) {
    val ctx = LocalContext.current
    val me = Store.username ?: ""
    val messages by vm.messages.collectAsState()
    val uploadPercent by vm.uploadPercent.collectAsState()
    val prefs by Store.prefs.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val muteKey = "grp:$id"; val muted = prefs.muted.contains(muteKey)
    var input by remember { mutableStateOf("") }
    var replyTo by remember { mutableStateOf<GroupMessage?>(null) }
    var actionMsg by remember { mutableStateOf<GroupMessage?>(null) }
    var profileUser by remember { mutableStateOf<String?>(null) }
    val detail by vm.detail.collectAsState()
    LaunchedEffect(id) { if (detail?.id != id) vm.loadDetail(id) }
    // تشخیص «@در حال تایپ» برای پیشنهاد اعضا (تلگرامی)
    // «دیده‌شده» روی خود دستگاه ذخیره می‌شود تا با بستن و باز کردن چت دوباره ظاهر نشود
    var mentionSeenId by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) { mentionSeenId = ir.darshub.app.core.Store.groupSeen(id) }
    var flashId by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(flashId) { if (flashId != null) { kotlinx.coroutines.delay(2500); flashId = null } }
    val mentionQuery = remember(input) {
        val m = Regex("@([\\w\\u0600-\\u06FF]*)$").find(input)
        m?.groupValues?.get(1)
    }
    var showAttach by remember { mutableStateOf(false) }
    var showStickers by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }
    var viewImage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.sendImage(ctx, id, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.sendFile(ctx, id, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }
    val musicFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> if (uri != null) vm.sendMusicFile(ctx, id, uri) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }

    LaunchedEffect(id) { vm.pollChat(id) }
    LaunchedEffect(id) { while (true) { vm.pollChat(id); delay(3000) } }
    LaunchedEffect(messages.size) { val n = messages.size; if (n > 0) runCatching { listState.animateScrollToItem(n - 1) } }

    Column(Modifier.fillMaxSize().imePadding()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                Text("👥  $name", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { Store.toggleMute(muteKey); Toast.makeText(ctx, if (!muted) "بی‌صدا شد 🔕" else "صدادار شد 🔔", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Rounded.NotificationsOff, "بی‌صدا", tint = if (muted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val showJump by remember { androidx.compose.runtime.derivedStateOf { val li = listState.layoutInfo; li.totalItemsCount > 0 && (li.visibleItemsInfo.lastOrNull()?.index ?: 0) < li.totalItemsCount - 3 } }
            if (messages.isEmpty()) EmptyState("💬", "اولین پیام گروه رو بفرست!")
            else LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(messages, key = { it.id }) { m ->
                    Box(Modifier.animateItemPlacement()) {
                        // فلاش تلگرامی: فقط چند ثانیه بعد از پرش هایلایت می‌شود
                        val bg by androidx.compose.animation.animateColorAsState(
                            if (flashId == m.id) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                            androidx.compose.animation.core.tween(500), label = "flash")
                        Box(Modifier.background(bg, RoundedCornerShape(14.dp))) {
                            GroupBubble(m, m.sender == me, onLongPress = { actionMsg = m }, onAvatar = { if (m.sender != me) profileUser = m.sender }, onImageClick = { viewImage = it })
                        }
                    }
                }
            }
            JumpToBottom(visible = showJump) { scope.launch { runCatching { listState.animateScrollToItem((messages.size - 1).coerceAtLeast(0)) } } }
        }
        uploadPercent?.let { UploadBar(it) }
        run {
            val meIdx = messages.indexOfLast { mm -> mm.sender != me && (mm.replySender == me || mm.message.contains("@$me")) }
            val meMsgId = if (meIdx >= 0) messages[meIdx].id else 0
            // اگر خود پیام در صفحه دیده شد → منشن دیده‌شده حساب می‌شود و دکمه محو می‌شود
            LaunchedEffect(meMsgId) {
                if (meMsgId > 0) androidx.compose.runtime.snapshotFlow { listState.layoutInfo.visibleItemsInfo.map { it.index } }
                    .collect { vis ->
                        if (meIdx in vis && meMsgId > mentionSeenId) {
                            mentionSeenId = meMsgId
                            ir.darshub.app.core.Store.setGroupSeen(id, meMsgId)
                        }
                    }
            }
            if (meIdx >= 0 && meMsgId > mentionSeenId) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.End) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 4.dp,
                        onClick = {
                            mentionSeenId = meMsgId
                            ir.darshub.app.core.Store.setGroupSeen(id, meMsgId)
                            flashId = meMsgId
                            scope.launch { listState.animateScrollToItem(meIdx) }
                        }) {
                        Text("@", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                }
            }
        }
        if (mentionQuery != null && detail != null) {
            val cands = detail!!.members.map { it.username }
                .filter { it != me && (mentionQuery.isEmpty() || it.contains(mentionQuery, true)) }.take(6)
            if (cands.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    cands.forEach { u ->
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer,
                            onClick = { input = input.replace(Regex("@([\\w\\u0600-\\u06FF]*)$"), "@$u ") },
                            modifier = Modifier.padding(end = 6.dp)) {
                            Text("@$u", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }
        }
        if (replyTo != null) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("پاسخ به " + (replyTo?.sender ?: ""), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        Text(replyTo?.message?.ifBlank { "📎 رسانه" } ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { replyTo = null }) { Icon(Icons.Rounded.Close, "بستن", modifier = Modifier.size(18.dp)) }
                }
            }
        }
        ChatInputBar(
            value = input, onValueChange = { input = it }, placeholder = "پیام گروه…",
            onAttach = { showAttach = true },
            onSend = { val t = input.trim(); if (t.isNotEmpty()) { vm.sendChat(id, t, replyTo?.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }; input = ""; replyTo = null } },
        )
    }
    if (showAttach) AttachSheet(onImage = { showAttach = false; imagePicker.launch("image/*") }, onFile = { showAttach = false; filePicker.launch("*/*") }, onMusic = { showAttach = false; showMusic = true }, onMusicDevice = { showAttach = false; musicFilePicker.launch("audio/*") }, onSticker = { showAttach = false; showStickers = true }, onDismiss = { showAttach = false })
    if (showStickers) StickerPickerSheet(onPick = { st -> showStickers = false; vm.sendSticker(id, st.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }, onDismiss = { showStickers = false })
    if (showMusic) ChatMusicPicker(onPick = { t -> showMusic = false; vm.sendMusic(id, t.id) { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() } }, onDismiss = { showMusic = false })

    actionMsg?.let { m ->
        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
        GlassMenu(onDismiss = { actionMsg = null }) {
            GlassReactions(mineEmoji = m.reactions.firstOrNull { it.mine }?.emoji) { em -> vm.react(id, m.id, em) }
            GlassDivider()
            GlassAction(Icons.Rounded.Reply, "پاسخ") { replyTo = m }
            if (m.sender != me) GlassAction(Icons.Rounded.Person, "پروفایل ${m.sender}") { profileUser = m.sender }
            if (m.message.isNotBlank()) GlassAction(Icons.Rounded.ContentCopy, "کپی متن") {
                clip.setText(androidx.compose.ui.text.AnnotatedString(m.message))
            }
            if (m.sender == me) GlassAction(Icons.Rounded.Delete, "حذف پیام", danger = true) { vm.deleteMsg(id, m.id) }
        }
    }
    if (viewImage != null) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { viewImage = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.95f)), contentAlignment = Alignment.Center) {
                AsyncImage(model = Api.mediaUrl(viewImage), contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                IconButton(onClick = { viewImage = null }, modifier = Modifier.align(Alignment.TopStart).padding(12.dp)) { Icon(Icons.Rounded.Close, "بستن", tint = androidx.compose.ui.graphics.Color.White) }
                IconButton(onClick = { ChatMedia.download(ctx, Api.mediaUrl(viewImage), "darshub_" + System.currentTimeMillis() + ".jpg") }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) { Icon(androidx.compose.material.icons.Icons.Rounded.Download, "دانلود", tint = androidx.compose.ui.graphics.Color.White) }
            }
        }
    }
    if (profileUser != null) {
        val pu = profileUser!!
        UserProfileSheet(username = pu, onDismiss = { profileUser = null }, onMessage = { profileUser = null; ir.darshub.app.core.NavBus.requestDm(pu) })
    }
}

@Composable
private fun GroupBubble(m: GroupMessage, isMine: Boolean, onLongPress: () -> Unit, onAvatar: () -> Unit, onImageClick: (String) -> Unit) {
    val plain = m.type == "sticker" || m.type == "gif"
    ChatRow(isMine = isMine, avatar = { Box(Modifier.combinedClickable(onClick = onAvatar, onLongClick = {})) { Avatar(m.sender, size = 32.dp, avatarUrl = m.senderAvatar) } }) {
        if (plain) Box(Modifier.combinedClickable(onClick = onLongPress, onLongClick = onLongPress)) { MediaBody(m.type, m.mediaPath, m.fileName, m.fileSize, m.duration, null, onImageClick, onLongPress = onLongPress) }
        else BubbleBox(isMine) {
            if (!isMine) Text(m.sender, style = MaterialTheme.typography.labelMedium, color = userColor(m.sender), modifier = Modifier.combinedClickable(onClick = onAvatar, onLongClick = {}))
            if (m.replyMessage != null || m.replySender != null) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 3.dp)) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                        Text(m.replySender ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text(m.replyMessage?.ifBlank { "📎 رسانه" } ?: "📎 رسا��ه", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun CreateGroupDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("گروه جدید") },
        text = { Column {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام گروه") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = username, onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } }, label = { Text("شناسه گروه (انگلیسی)") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Text("با این شناسه بقیه می‌تونن عضو بشن", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank() && username.length >= 3) onCreate(name.trim(), username.trim()) }) { Text("ساخت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}

@Composable
private fun JoinGroupDialog(onDismiss: () -> Unit, onJoin: (String) -> Unit) {
    var username by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("عضویت در گروه") },
        text = { Column {
            OutlinedTextField(value = username, onValueChange = { username = it.lowercase().filter { c -> c.isLetterOrDigit() || c == '_' } }, label = { Text("شناسه گروه") }, singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
            Text("شناسه گروه رو از سازنده‌اش بگیر", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } },
        confirmButton = { TextButton(onClick = { if (username.length >= 3) onJoin(username.trim()) }) { Text("عضویت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } })
}
