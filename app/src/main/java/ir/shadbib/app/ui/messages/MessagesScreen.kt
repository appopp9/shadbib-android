@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.shadbib.app.ui.messages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.NavBus
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.Conversation
import ir.shadbib.app.data.UserResult
import ir.shadbib.app.data.StudyGroup
import ir.shadbib.app.ui.community.GroupChatScreen
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.ColumnScopeGlass
import ir.shadbib.app.ui.components.GlassAction
import ir.shadbib.app.ui.components.GlassDivider
import ir.shadbib.app.ui.components.GlassMenu
import ir.shadbib.app.ui.components.GlassReactions
import ir.shadbib.app.ui.components.LoadingBox
import kotlinx.coroutines.delay

private sealed class MsgRoute {
    object Inbox : MsgRoute()
    data class Dm(val username: String) : MsgRoute()
    data class Channel(val key: String, val title: String, val emoji: String) : MsgRoute()
    data class Group(val id: Int, val name: String) : MsgRoute()
    object GroupsHome : MsgRoute()
}

@Composable
fun MessagesScreen(vm: MessagesViewModel = viewModel()) {
    val sep = '\u0001'
    var route by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver(
        save = { r -> when (r) {
            is MsgRoute.Dm -> "dm$sep${r.username}"
            is MsgRoute.Channel -> "ch$sep${r.key}$sep${r.title}$sep${r.emoji}"
            is MsgRoute.Group -> "grp$sep${r.id}$sep${r.name}"
            is MsgRoute.GroupsHome -> "grphome"
            else -> "inbox"
        } },
        restore = { s -> runCatching {
            val p = s.split(sep)
            when (p.getOrNull(0)) {
                "dm" -> MsgRoute.Dm(p.getOrElse(1) { "" }).takeIf { it.username.isNotBlank() } ?: MsgRoute.Inbox
                "ch" -> MsgRoute.Channel(p.getOrElse(1) { "public" }, p.getOrElse(2) { "" }, p.getOrElse(3) { "" })
                "grphome" -> MsgRoute.GroupsHome
                "grp" -> p.getOrNull(1)?.toIntOrNull()?.let { gid -> MsgRoute.Group(gid, p.getOrElse(2) { "" }) } ?: MsgRoute.Inbox
                else -> MsgRoute.Inbox
            }
        }.getOrDefault(MsgRoute.Inbox) },
    )) { mutableStateOf<MsgRoute>(MsgRoute.Inbox) }

    // React to cross-tab "open DM" requests
    val openDm by NavBus.openDm.collectAsState()
    LaunchedEffect(openDm) {
        val u = openDm
        if (u != null) { route = MsgRoute.Dm(u); NavBus.consumeDm() }
    }
    val openCh by NavBus.openChannel.collectAsState()
    LaunchedEffect(openCh) {
        openCh?.let { route = MsgRoute.Channel(it.first, it.second, it.third); NavBus.consumeChannel() }
    }

    androidx.compose.animation.AnimatedContent(
        targetState = route,
        transitionSpec = {
            val enteringChat = targetState !is MsgRoute.Inbox
            val slideDir = if (enteringChat) -1 else 1
            (androidx.compose.animation.slideInHorizontally(androidx.compose.animation.core.tween(260)) { it / 6 * slideDir } + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)))
                .togetherWith(androidx.compose.animation.slideOutHorizontally(androidx.compose.animation.core.tween(200)) { -it / 8 * slideDir } + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(160)))
        },
        label = "msgRoute",
    ) { r ->
        when (r) {
            is MsgRoute.Inbox -> InboxScreen(vm,
                onOpenDm = { route = MsgRoute.Dm(it) },
                onOpenChannel = { key, title, emoji -> route = MsgRoute.Channel(key, title, emoji) },
                onOpenGroup = { id, name -> route = MsgRoute.Group(id, name) },
                onOpenGroupsHome = { route = MsgRoute.GroupsHome })
            is MsgRoute.Dm -> { BackHandler { route = MsgRoute.Inbox }; DmThreadScreen(r.username, onBack = { route = MsgRoute.Inbox }) }
            is MsgRoute.Channel -> { BackHandler { route = MsgRoute.Inbox }; ChannelChatScreen(r.key, r.title, r.emoji, onBack = { route = MsgRoute.Inbox }, onOpenDm = { route = MsgRoute.Dm(it) }) }
            is MsgRoute.Group -> { BackHandler { route = MsgRoute.Inbox }; GroupChatScreen(r.id, r.name, onBack = { route = MsgRoute.Inbox }) }
            is MsgRoute.GroupsHome -> {
                BackHandler { route = MsgRoute.Inbox }
                Column(Modifier.fillMaxSize()) {
                    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { route = MsgRoute.Inbox }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                            Text("گروه‌های مطالعه 👥", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    ir.shadbib.app.ui.community.GroupsTab()
                }
            }
        }
    }
}

@Composable
private fun InboxScreen(vm: MessagesViewModel, onOpenDm: (String) -> Unit, onOpenChannel: (String, String, String) -> Unit, onOpenGroup: (Int, String) -> Unit, onOpenGroupsHome: () -> Unit) {
    val state by vm.state.collectAsState()
    val prefs by ir.shadbib.app.core.Store.prefs.collectAsState()
    val channelMeta by vm.channelMeta.collectAsState()
    val groupMeta by vm.groupMeta.collectAsState()
    var chatMenu by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { while (true) { vm.poll(); delay(7000) } }
    LaunchedEffect(Unit) { ir.shadbib.app.core.RefreshBus.events.collect { if (it == "dm" || it == "groups" || it == "all") vm.poll() } }
    LaunchedEffect(state.query) { delay(400); if (state.query.isNotBlank()) vm.search() }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("پیام‌ها", style = MaterialTheme.typography.headlineSmall)
            Text("گفتگو با رفقای درس‌خون", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(value = state.query, onValueChange = { vm.setQuery(it) },
            placeholder = { Text("جستجوی کاربر برای شروع چت…") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true,
            shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        Spacer(Modifier.size(8.dp))

        val results = state.searchResults
        if (results != null) {
            if (results.isEmpty()) EmptyState("🔍", "کاربری پیدا نشد")
            else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.username }) { u -> UserRow(u) { vm.clearSearch(); onOpenDm(u.username) } }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                item {
                    fun sub(ch: String, def: String): String {
                        val m = channelMeta[ch] ?: return def
                        return if (m.first != null) "${m.second}: ${m.first}" else def
                    }
                    Column {
                        PinnedRow("💬", "چت همگانی", sub("public", "گفت‌وگوی عمومی همه کاربران"), Color(0xFF34D399), unread = channelMeta["public"]?.third ?: 0) { onOpenChannel("public", "چت همگانی", "💬") }
                        PinnedRow("❓", "رفع اشکال", sub("help", "سوال بپرس و به بقیه کمک کن"), Color(0xFF38BDF8), unread = channelMeta["help"]?.third ?: 0) { onOpenChannel("help", "رفع اشکال", "❓") }
                        PinnedRow("📢", "اطلاع‌رسانی", sub("news", "اخبار و اطلاعیه‌های رسمی"), Color(0xFFF59E0B), unread = channelMeta["news"]?.third ?: 0) { onOpenChannel("news", "اطلاع‌رسانی", "📢") }
                    }
                }
                item { Spacer(Modifier.size(4.dp)); Text("گفتگوها", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) }
                when {
                    state.loading -> item { LoadingBox(height = 120.dp) }
                    state.conversations.isEmpty() && state.groups.isEmpty() -> item { EmptyState("✉️", "هنوز گفتگویی نداری — یه کاربر رو جستجو کن!") }
                    else -> {
                        // ادغام تلگرامی: پیوی‌ها و گروه‌ها با هم، مرتب بر اساس آخرین پیام (پین‌ها اول)
                        val entries: List<InboxEntry> =
                            state.conversations.map { c -> InboxEntry.Dm(c, c.lastTime) } +
                            state.groups.map { g -> InboxEntry.Grp(g, groupMeta[g.id]?.time) }
                        val sorted = entries.sortedWith(
                            compareByDescending<InboxEntry> { e ->
                                val key = when (e) { is InboxEntry.Dm -> "dm:" + e.c.username; is InboxEntry.Grp -> "grp:" + e.g.id }
                                prefs.pinned.contains(key)
                            }.thenByDescending { it.time ?: "" })
                        items(sorted, key = { e -> when (e) { is InboxEntry.Dm -> e.c.username; is InboxEntry.Grp -> "g" + e.g.id } }) { e ->
                            Box(Modifier.animateItemPlacement()) {
                                when (e) {
                                    is InboxEntry.Dm -> ConversationRow(e.c, pinned = prefs.pinned.contains("dm:" + e.c.username),
                                        muted = prefs.muted.contains("dm:" + e.c.username),
                                        onClick = { onOpenDm(e.c.username) },
                                        onLongClick = { chatMenu = e.c.username })
                                    is InboxEntry.Grp -> {
                                        val gm = groupMeta[e.g.id]
                                        GroupInboxRow(e.g.name, gm?.sender, gm?.last, gm?.time, gm?.unread ?: 0, gm?.mention == true,
                                            pinned = prefs.pinned.contains("grp:" + e.g.id),
                                            onClick = { onOpenGroup(e.g.id, e.g.name) },
                                            onLongClick = { chatMenu = "grp:" + e.g.id })
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // گروه‌های مطالعه — دکمه شناور 👥
    Surface(shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 8.dp,
        onClick = onOpenGroupsHome,
        modifier = Modifier.align(Alignment.BottomStart).padding(18.dp)) {
        Text("👥", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(15.dp))
    }

    chatMenu?.let { cu ->
        val key = if (cu.startsWith("grp:")) cu else "dm:" + cu
        GlassMenu(onDismiss = { chatMenu = null }) {
            GlassAction(Icons.Rounded.PushPin,
                if (prefs.pinned.contains(key)) "برداشتن پین" else "پین کردن") { ir.shadbib.app.core.Store.togglePin(key) }
            GlassAction(Icons.Rounded.NotificationsOff,
                if (prefs.muted.contains(key)) "صدادار کردن" else "بی‌صدا کردن") { ir.shadbib.app.core.Store.toggleMute(key) }
        }
    }
    }
}

@Composable
private fun PinnedRow(emoji: String, title: String, subtitle: String, tint: Color, unread: Int = 0, onClick: () -> Unit) {
    Surface(color = Color.Transparent, onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = tint.copy(alpha = 0.13f),
                border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.22f))) {
                Text(emoji, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(6.dp))
                    Text("📌", style = MaterialTheme.typography.labelSmall)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (unread > 0) Surface(shape = androidx.compose.foundation.shape.CircleShape, color = tint) {
                Text(unread.fa(), color = Color.White, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, pinned: Boolean = false, muted: Boolean = false, onClick: () -> Unit, onLongClick: () -> Unit = {}) {
    Surface(color = if (pinned) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.username, c.mood, size = 52.dp, online = c.isOnline, avatarUrl = c.avatar)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.username, style = MaterialTheme.typography.titleSmall)
                    if (muted) { Spacer(Modifier.width(4.dp)); Text("🔕", style = MaterialTheme.typography.labelSmall) }
                    if (pinned) { Spacer(Modifier.width(4.dp)); Text("📌", style = MaterialTheme.typography.labelSmall) }
                    Spacer(Modifier.weight(1f))
                    Text(Fmt.relative(c.lastTime), style = MaterialTheme.typography.labelSmall,
                        color = if (c.unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((if (c.lastFromMe) "تو: " else "") + c.lastMessage, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (c.unread > 0) { Spacer(Modifier.width(8.dp)); Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(c.unread.fa()) } }
                }
            }
        }
    }
}

@Composable
private fun UserRow(u: UserResult, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(u.username, u.mood, size = 44.dp, online = u.isOnline, avatarUrl = u.avatar)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(u.username, style = MaterialTheme.typography.titleSmall)
                Text(u.statusText.ifBlank { if (u.isOnline) "آنلاین" else "آفلاین" }, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.AutoMirrored.Rounded.Send, "شروع چت", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}


/** آیتم اینباکس تلگرامی — پیوی یا گروه. */
sealed class InboxEntry(val time: String?) {
    class Dm(val c: ir.shadbib.app.data.Conversation, t: String?) : InboxEntry(t)
    class Grp(val g: ir.shadbib.app.data.StudyGroup, t: String?) : InboxEntry(t)
}

@Composable
private fun GroupInboxRow(name: String, sender: String?, last: String?, time: String?, unread: Int, mention: Boolean,
                          pinned: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Surface(color = if (pinned) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color.Transparent,
        shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = Color(0xFFA78BFA).copy(alpha = 0.13f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.22f))) {
                Text("👥", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.titleSmall)
                    if (pinned) { Spacer(Modifier.width(6.dp)); Text("📌", style = MaterialTheme.typography.labelSmall) }
                }
                Text((if (last != null) (sender ?: "") + ": " + last else "گروه مطالعه"),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (time != null) Text(Fmt.relative(time), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mention) Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.primary) {
                        Text("@", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    if (unread > 0) {
                        Spacer(Modifier.width(3.dp))
                        Surface(shape = androidx.compose.foundation.shape.CircleShape, color = Color(0xFFA78BFA)) {
                            Text(unread.fa(), color = Color.White, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
