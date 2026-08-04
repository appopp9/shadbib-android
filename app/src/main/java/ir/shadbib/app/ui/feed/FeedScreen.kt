@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material.ExperimentalMaterialApi::class)

package ir.shadbib.app.ui.feed

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.LibraryMusic
import ir.shadbib.app.ui.components.GlassMenu
import ir.shadbib.app.ui.components.GlassAction
import ir.shadbib.app.ui.components.GlassDivider
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.NavBus
import ir.shadbib.app.core.RefreshBus
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.data.FollowUser
import ir.shadbib.app.data.MusicTrack
import ir.shadbib.app.data.Post
import ir.shadbib.app.data.SocialProfile
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.ErrorState
import ir.shadbib.app.ui.components.LoadingBox
import ir.shadbib.app.ui.components.SegmentedTabs
import ir.shadbib.app.ui.messages.AudioBubble
import ir.shadbib.app.ui.messages.ChatMedia
import ir.shadbib.app.ui.library.fileNameOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

// ==================== ViewModel ====================
class FeedViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val forYou: List<Post> = emptyList(),
        val following: List<Post> = emptyList(),
        val searchUsers: List<FollowUser> = emptyList(),
        val searchPosts: List<Post> = emptyList(),
        val searching: Boolean = false,
        val posting: Boolean = false,
        val uploadPercent: Int? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> get() = _state
    private var searchJob: Job? = null

    init {
        refresh()
        viewModelScope.launch { RefreshBus.events.collect { if (it == "feed" || it == "all") refresh(silent = true) } }
        viewModelScope.launch { while (true) { delay(45000); refresh(silent = true) } }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _state.value = _state.value.copy(loading = _state.value.forYou.isEmpty(), error = null)
            runCatching {
                val fy = Post.list(Api.arr(Api.get("feed", "tab" to "foryou")))
                val fl = Post.list(Api.arr(Api.get("feed", "tab" to "following")))
                _state.value = _state.value.copy(loading = false, error = null, forYou = fy, following = fl)
                markViewed(fy.take(20).map { it.id })
            }.onFailure { e ->
                _state.value = _state.value.copy(loading = false,
                    error = if (_state.value.forYou.isEmpty()) (e.message ?: "خطا") else null)
            }
        }
    }

    private fun markViewed(ids: List<Int>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { runCatching { Api.post("post_view", JSONObject().put("ids", org.json.JSONArray(ids))) } }
    }

    private fun patchEverywhere(id: Int, f: (Post) -> Post) {
        _state.value = _state.value.copy(
            forYou = _state.value.forYou.map { if (it.id == id) f(it) else it },
            following = _state.value.following.map { if (it.id == id) f(it) else it },
            searchPosts = _state.value.searchPosts.map { if (it.id == id) f(it) else it },
        )
    }

    fun toggleLike(p: Post) {
        patchEverywhere(p.id) { it.copy(liked = !it.liked, likes = (it.likes + if (it.liked) -1 else 1).coerceAtLeast(0)) }
        viewModelScope.launch { runCatching { Api.post("post_like", JSONObject().put("id", p.id)) } }
    }

    fun repost(p: Post, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { Api.post("post_create", JSONObject().put("repost_of", p.repostOf ?: p.id)) }
                .onSuccess { patchEverywhere(p.id) { it.copy(reposted = true, reposts = it.reposts + 1) }; refresh(silent = true); onDone(null) }
                .onFailure { onDone(it.message) }
        }
    }

    fun delete(p: Post, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            runCatching { Api.delete("post_delete", "id" to p.id.toString()) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        forYou = _state.value.forYou.filter { it.id != p.id },
                        following = _state.value.following.filter { it.id != p.id })
                    onDone(null)
                }.onFailure { onDone(it.message) }
        }
    }

    fun search(q: String) {
        searchJob?.cancel()
        if (q.isBlank()) { _state.value = _state.value.copy(searchUsers = emptyList(), searchPosts = emptyList(), searching = false); return }
        searchJob = viewModelScope.launch {
            delay(350)
            _state.value = _state.value.copy(searching = true)
            runCatching {
                val users = FollowUser.list(Api.arr(Api.get("social_search", "q" to q, "kind" to "users")))
                val posts = Post.list(Api.arr(Api.get("social_search", "q" to q, "kind" to "posts")))
                _state.value = _state.value.copy(searchUsers = users, searchPosts = posts, searching = false)
            }.onFailure { _state.value = _state.value.copy(searching = false) }
        }
    }

    fun createPost(ctx: android.content.Context, text: String, mediaUri: Uri?, mediaType: String?, musicId: Int?, replyTo: Int?, repostOf: Int? = null, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(posting = true, uploadPercent = if (mediaUri != null) 0 else null)
            runCatching {
                if (mediaUri != null && mediaType != null) {
                    val bytes = ctx.contentResolver.openInputStream(mediaUri)?.readBytes() ?: throw Exception("خطا در خواندن فایل")
                    val name = fileNameOf(ctx, mediaUri).ifBlank { "media" }
                    val mime = ctx.contentResolver.getType(mediaUri) ?: "application/octet-stream"
                    val fields = HashMap<String, String>()
                    if (text.isNotBlank()) fields["text"] = text
                    fields["media_type"] = mediaType
                    if (replyTo != null) fields["reply_to"] = replyTo.toString()
                    if (repostOf != null) fields["repost_of"] = repostOf.toString()
                    Api.upload("post_create", fields, listOf(Api.FilePart("file", name, bytes, mime))) { p ->
                        _state.value = _state.value.copy(uploadPercent = p)
                    }
                } else {
                    val b = JSONObject()
                    if (text.isNotBlank()) b.put("text", text)
                    if (musicId != null) b.put("music_id", musicId)
                    if (replyTo != null) b.put("reply_to", replyTo)
                    if (repostOf != null) b.put("repost_of", repostOf)
                    Api.post("post_create", b)
                }
            }.onSuccess {
                _state.value = _state.value.copy(posting = false, uploadPercent = null)
                refresh(silent = true); RefreshBus.emit("feed"); onDone(null)
            }.onFailure {
                _state.value = _state.value.copy(posting = false, uploadPercent = null)
                onDone(it.message ?: "خطا در ارسال")
            }
        }
    }
}

class PostDetailViewModel : ViewModel() {
    data class State(val loading: Boolean = true, val post: Post? = null, val replies: List<Post> = emptyList(), val error: String? = null)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> get() = _state
    private var pid = -1

    fun load(id: Int) {
        pid = id
        viewModelScope.launch {
            runCatching {
                val o = Api.obj(Api.get("post_detail", "id" to id.toString()))
                _state.value = State(loading = false,
                    post = Post.from(o.getJSONObject("post")),
                    replies = o.optJSONArray("replies")?.let { Post.list(it) } ?: emptyList())
            }.onFailure { _state.value = State(loading = false, error = it.message) }
        }
    }
    fun reload() { if (pid > 0) load(pid) }
    fun toggleLike(p: Post) {
        val f = { x: Post -> if (x.id == p.id) x.copy(liked = !x.liked, likes = (x.likes + if (x.liked) -1 else 1).coerceAtLeast(0)) else x }
        _state.value = _state.value.copy(post = _state.value.post?.let(f), replies = _state.value.replies.map(f))
        viewModelScope.launch { runCatching { Api.post("post_like", JSONObject().put("id", p.id)) } }
    }
}

class SocialProfileViewModel : ViewModel() {
    data class State(val loading: Boolean = true, val profile: SocialProfile? = null, val posts: List<Post> = emptyList(), val error: String? = null)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> get() = _state
    private var user = ""

    fun load(username: String) {
        user = username
        viewModelScope.launch {
            runCatching {
                val o = Api.obj(Api.get("social_profile", "username" to username))
                _state.value = State(loading = false,
                    profile = SocialProfile.from(o),
                    posts = o.optJSONArray("posts")?.let { Post.list(it) } ?: emptyList())
            }.onFailure { _state.value = State(loading = false, error = it.message) }
        }
    }

    fun toggleFollow(onDone: (String?) -> Unit = {}) {
        val p = _state.value.profile ?: return
        _state.value = _state.value.copy(profile = p.copy(
            isFollowing = !p.isFollowing,
            followers = (p.followers + if (p.isFollowing) -1 else 1).coerceAtLeast(0)))
        viewModelScope.launch {
            runCatching { Api.post(if (p.isFollowing) "unfollow" else "follow", JSONObject().put("username", p.username)) }
                .onSuccess { RefreshBus.emit("feed"); RefreshBus.emit("home"); onDone(null) }
                .onFailure { load(user); onDone(it.message) }
        }
    }

    fun toggleLike(p2: Post) {
        _state.value = _state.value.copy(posts = _state.value.posts.map {
            if (it.id == p2.id) it.copy(liked = !it.liked, likes = (it.likes + if (it.liked) -1 else 1).coerceAtLeast(0)) else it
        })
        viewModelScope.launch { runCatching { Api.post("post_like", JSONObject().put("id", p2.id)) } }
    }
}

// ==================== مسیرهای داخلی ====================
private sealed class FeedRoute {
    object Home : FeedRoute()
    data class Detail(val id: Int) : FeedRoute()
    data class Profile(val username: String) : FeedRoute()
}

// ==================== صفحه اصلی فید ====================
@Composable
fun FeedScreen(vm: FeedViewModel = viewModel()) {
    var route by remember { mutableStateOf<FeedRoute>(FeedRoute.Home) }
    // درخواست باز کردن پروفایل از تب‌های دیگر (مثلاً اتاق مطالعه)
    val userReq by NavBus.openUser.collectAsState()
    LaunchedEffect(userReq) {
        val u = userReq
        if (u != null) { route = FeedRoute.Profile(u); NavBus.consumeUser() }
    }
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            (fadeIn(tween(240, easing = FastOutSlowInEasing)) + slideInVertically(tween(240)) { it / 22 })
                .togetherWith(fadeOut(tween(130)))
        }, label = "feedRoute",
    ) { r ->
        when (r) {
            is FeedRoute.Home -> FeedHome(vm,
                onOpenPost = { route = FeedRoute.Detail(it) },
                onOpenUser = { route = FeedRoute.Profile(it) })
            is FeedRoute.Detail -> {
                BackHandler { route = FeedRoute.Home }
                PostDetailScreen(r.id, onBack = { route = FeedRoute.Home }, onOpenUser = { route = FeedRoute.Profile(it) })
            }
            is FeedRoute.Profile -> {
                BackHandler { route = FeedRoute.Home }
                SocialProfileScreen(r.username, onBack = { route = FeedRoute.Home },
                    onOpenPost = { route = FeedRoute.Detail(it) },
                    onOpenUser = { route = FeedRoute.Profile(it) })
            }
        }
    }
}

@Composable
private fun FeedHome(vm: FeedViewModel, onOpenPost: (Int) -> Unit, onOpenUser: (String) -> Unit) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var showComposer by remember { mutableStateOf(false) }
    var replyTarget by remember { mutableStateOf<Post?>(null) }
    var menuPost by remember { mutableStateOf<Post?>(null) }
    var editPost by remember { mutableStateOf<Post?>(null) }
    var quotePost by remember { mutableStateOf<Post?>(null) }
    var likersPost by remember { mutableStateOf<Post?>(null) }
    val me = Store.username ?: ""
    val listState = rememberLazyListState()
    var refreshing by remember { mutableStateOf(false) }
    val scopeRf = androidx.compose.runtime.rememberCoroutineScope()
    val pullState = rememberPullRefreshState(refreshing, {
        refreshing = true
        vm.refresh(silent = true)
        scopeRf.launch { kotlinx.coroutines.delay(900); refreshing = false }
    })

    Box(Modifier.fillMaxSize().pullRefresh(pullState)) {
        Column(Modifier.fillMaxSize()) {
            // هدر + جستجو (جایگزین بخش دوستان)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                TextField(
                    value = query, onValueChange = { query = it; vm.search(it) },
                    placeholder = { Text("جستجوی کاربر یا پست…", style = MaterialTheme.typography.bodyMedium) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingIcon = { if (query.isNotBlank()) IconButton(onClick = { query = ""; vm.search("") }) { Icon(Icons.Rounded.Close, "پاک کردن", modifier = Modifier.size(18.dp)) } },
                    singleLine = true, shape = RoundedCornerShape(20.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (query.isNotBlank()) {
                SearchResults(state, onOpenUser, onOpenPost, vm)
            } else {
                SegmentedTabs(options = listOf("برای شما ✨", "دنبال‌شده‌ها"), selected = tab, modifier = Modifier.padding(horizontal = 16.dp)) { tab = it }
                Spacer(Modifier.height(4.dp))
                val posts = if (tab == 0) state.forYou else state.following
                when {
                    state.loading -> Column(Modifier.padding(16.dp)) { LoadingBox(height = 300.dp) }
                    state.error != null -> ErrorState(state.error!!, onRetry = { vm.refresh() })
                    posts.isEmpty() && tab == 1 -> EmptyState("🫂", "هنوز کسی رو دنبال نکردی!\nاز تب «برای شما» درس‌خون‌ها رو پیدا کن")
                    posts.isEmpty() -> EmptyState("🌱", "هنوز پستی نیست — اولین نفر باش!")
                    else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(posts, key = { "p${it.id}" }) { p ->
                            Box(Modifier.animateItemPlacement()) {
                                PostCard(p, me,
                                    onOpen = { onOpenPost(p.id) },
                                    onUser = { onOpenUser(it) },
                                    onLike = { vm.toggleLike(p) },
                                    onReply = { replyTarget = p; showComposer = true },
                                    onRepost = { vm.repost(p) { e -> Toast.makeText(ctx, e ?: "بازنشر شد 🔁", Toast.LENGTH_SHORT).show() } },
                                    onLikers = { likersPost = p },
                                    onLongPress = { menuPost = p })
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        }
                        item { Spacer(Modifier.height(90.dp)) }
                    }
                }
            }
        }

        PullRefreshIndicator(refreshing, pullState,
            Modifier.align(Alignment.TopCenter),
            backgroundColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary)

        // دکمه شناور ساخت پست (سبک توییتر)
        Surface(
            shape = CircleShape, color = MaterialTheme.colorScheme.primary, shadowElevation = 8.dp,
            onClick = { replyTarget = null; showComposer = true },
            modifier = Modifier.align(Alignment.BottomStart).padding(18.dp),
        ) {
            Row(Modifier.padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, "پست جدید", tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(4.dp))
                Text("پست", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleSmall)
            }
        }
    }

    if (showComposer) {
        ComposerSheet(vm, replyTo = replyTarget, quoteOf = quotePost,
            onDismiss = { showComposer = false; replyTarget = null; quotePost = null })
    }

    editPost?.let { p ->
        EditPostSheet(p, onDismiss = { editPost = null }, onSaved = { editPost = null; vm.refresh(silent = true) })
    }

    likersPost?.let { p ->
        LikersSheet(p.repostOf ?: p.id, onDismiss = { likersPost = null }, onOpenUser = { u -> likersPost = null; onOpenUser(u) })
    }

    menuPost?.let { p ->
        val clip = androidx.compose.ui.platform.LocalClipboardManager.current
        GlassMenu(onDismiss = { menuPost = null }) {
            if (p.username == me) {
                GlassAction(Icons.Rounded.Edit, "ویرایش پست") { editPost = p }
                GlassAction(Icons.Rounded.Delete, "حذف پست", danger = true) {
                    vm.delete(p) { e -> Toast.makeText(ctx, e ?: "حذف شد", Toast.LENGTH_SHORT).show() }
                }
            } else {
                GlassAction(Icons.Rounded.Person, "دیدن پروفایل ${p.username}") { onOpenUser(p.username) }
                GlassAction(Icons.AutoMirrored.Rounded.Send, "پیام خصوصی") { NavBus.requestDm(p.username) }
            }
            GlassDivider()
            GlassAction(Icons.Rounded.FormatQuote, "نقل‌قول با متن") { quotePost = p; showComposer = true }
            if (!p.text.isNullOrBlank()) GlassAction(Icons.Rounded.ContentCopy, "کپی متن") {
                clip.setText(androidx.compose.ui.text.AnnotatedString(p.text))
                Toast.makeText(ctx, "کپی شد", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun SearchResults(state: FeedViewModel.State, onOpenUser: (String) -> Unit, onOpenPost: (Int) -> Unit, vm: FeedViewModel) {
    val me = Store.username ?: ""
    LazyColumn(Modifier.fillMaxSize()) {
        if (state.searching) item { LoadingBox(height = 120.dp, modifier = Modifier.padding(16.dp)) }
        if (state.searchUsers.isNotEmpty()) {
            item { Text("کاربران", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            items(state.searchUsers, key = { "u${it.username}" }) { u ->
                Surface(color = Color.Transparent, onClick = { onOpenUser(u.username) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(u.username, u.mood, size = 42.dp, avatarUrl = u.avatar)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(u.username, style = MaterialTheme.typography.titleSmall)
                            Text("امروز ${Fmt.minutes(u.todayMinutes)}" + if (u.followsMe) " · دنبالت می‌کنه" else "",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (u.username != me) FollowChip(u.isFollowing) { }
                    }
                }
            }
        }
        if (state.searchPosts.isNotEmpty()) {
            item { Text("پست‌ها", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) }
            items(state.searchPosts, key = { "sp${it.id}" }) { p ->
                PostCard(p, me, onOpen = { onOpenPost(p.id) }, onUser = onOpenUser, onLike = { vm.toggleLike(p) }, onReply = { onOpenPost(p.id) }, onRepost = {}, onLongPress = {})
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            }
        }
        if (!state.searching && state.searchUsers.isEmpty() && state.searchPosts.isEmpty()) {
            item { EmptyState("🔍", "چیزی پیدا نشد") }
        }
    }
}

// ==================== کارت پست (سبک توییتر) ====================
@Composable
fun PostCard(
    p: Post, me: String,
    onOpen: () -> Unit, onUser: (String) -> Unit, onLike: () -> Unit,
    onReply: () -> Unit, onRepost: () -> Unit, onLongPress: () -> Unit,
    onLikers: () -> Unit = {},
    clickable: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()
        .combinedClickable(enabled = clickable, onClick = onOpen, onLongClick = onLongPress)
        .padding(horizontal = 14.dp, vertical = 10.dp)) {
        // بازنشر با متن (نقل‌قول): متن خودِ کاربر بدنه است و پست اصلی در کارت نقل‌قول می‌آید
        val isQuote = p.repostOf != null && !p.text.isNullOrBlank()
        val asOriginal = p.repostOf != null && !isQuote
        if (asOriginal) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp, start = 44.dp)) {
                Icon(Icons.Rounded.Repeat, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("${p.username} بازنشر کرد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val author = if (asOriginal && p.repostUser != null) p.repostUser else p.username
        val bodyText = if (asOriginal) p.repostText else p.text
        Row {
            Box(Modifier.clickable { onUser(author) }) { Avatar(author, p.mood.takeIf { !asOriginal }, size = 42.dp, avatarUrl = if (!asOriginal) p.avatar else null) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(author, style = MaterialTheme.typography.titleSmall, modifier = Modifier.clickable { onUser(author) })
                    Spacer(Modifier.width(6.dp))
                    Text(Fmt.relative(p.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Rounded.MoreHoriz, "گزینه‌ها", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onLongPress() })
                }
                if (p.replyTo != null && p.replyToUser != null) {
                    Text("در پاسخ به ${p.replyToUser}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (!bodyText.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(bodyText, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp)
                }
                if (isQuote) {
                    Spacer(Modifier.height(7.dp))
                    QuoteCard(p.repostUser, p.repostText) { p.repostUser?.let { u -> onUser(u) } }
                }
                PostMedia(p)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    ActionStat(Icons.Rounded.ChatBubbleOutline, p.replies, MaterialTheme.colorScheme.onSurfaceVariant, onReply)
                    ActionStat(Icons.Rounded.Repeat, p.reposts,
                        if (p.reposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, onRepost)
                    LikeStat(p.liked, p.likes, onLike, onCount = { onLikers() })
                    ActionStat(Icons.Rounded.BarChart, p.views, MaterialTheme.colorScheme.onSurfaceVariant) { }
                }
            }
        }
    }
}

@Composable
private fun QuoteCard(author: String?, text: String?, onAuthor: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(author ?: "?", null, size = 20.dp)
                Spacer(Modifier.width(6.dp))
                Text(author ?: "کاربر", style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clickable { onAuthor() })
            }
            if (!text.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text, style = MaterialTheme.typography.bodySmall, lineHeight = 19.sp, maxLines = 6)
            }
        }
    }
}

@Composable
private fun ActionStat(icon: androidx.compose.ui.graphics.vector.ImageVector, count: Int, tint: Color, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 5.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(4.dp))
        Text(if (count > 0) count.fa() else "", style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun LikeStat(liked: Boolean, count: Int, onClick: () -> Unit, onCount: () -> Unit = {}) {
    val scale by animateFloatAsState(if (liked) 1.15f else 1f, spring(dampingRatio = 0.4f, stiffness = 700f), label = "likeScale")
    val tint = if (liked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "لایک", tint = tint,
            modifier = Modifier.size(25.dp).clip(CircleShape).clickable { onClick() }.padding(4.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale })
        // مثل توییتر: کلیک روی عدد → چه کسانی لایک کردند
        Text(if (count > 0) count.fa() else "", style = MaterialTheme.typography.labelSmall, color = tint,
            modifier = Modifier.clip(CircleShape).clickable(enabled = count > 0) { onCount() }.padding(horizontal = 5.dp, vertical = 4.dp))
    }
}

@Composable
private fun PostMedia(p: Post) {
    val ctx = LocalContext.current
    when (p.mediaType) {
        "image" -> {
            var full by remember { mutableStateOf(false) }
            Spacer(Modifier.height(8.dp))
            AsyncImage(model = Api.mediaUrl(p.mediaPath), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant).clickable { full = true })
            if (full) androidx.compose.ui.window.Dialog(onDismissRequest = { full = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).clickable { full = false }, contentAlignment = Alignment.Center) {
                    AsyncImage(model = Api.mediaUrl(p.mediaPath), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        "video" -> {
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.85f))
                .clickable { ChatMedia.openUrl(ctx, Api.mediaUrl(p.mediaPath)) }, contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.92f)) {
                    Icon(Icons.Rounded.Videocam, "پخش ویدئو", tint = Color.Black, modifier = Modifier.padding(16.dp).size(26.dp))
                }
                Text("🎬 ویدئو — برای پخش لمس کن", color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter).padding(10.dp))
            }
        }
        "music" -> {
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (p.musicCover != null) AsyncImage(model = Api.mediaUrl(p.musicCover), contentDescription = null,
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                        else Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(p.musicTitle ?: "موزیک", style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(p.musicArtist ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    AudioBubble(Api.mediaUrl(p.mediaPath), 0, "🎵")
                }
            }
        }
    }
}

// ==================== جزئیات پست + پاسخ‌ها ====================
@Composable
fun PostDetailScreen(id: Int, onBack: () -> Unit, onOpenUser: (String) -> Unit, vm: PostDetailViewModel = viewModel(key = "post_$id")) {
    var dMenu by remember { mutableStateOf<Post?>(null) }
    var dEdit by remember { mutableStateOf<Post?>(null) }
    var dLikers by remember { mutableStateOf<Post?>(null) }

    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val feedVm: FeedViewModel = viewModel()
    var showReply by remember { mutableStateOf(false) }
    val me = Store.username ?: ""
    LaunchedEffect(id) { vm.load(id) }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                Text("پست", style = MaterialTheme.typography.titleMedium)
            }
        }
        when {
            state.loading -> Column(Modifier.padding(16.dp)) { LoadingBox(height = 220.dp) }
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.reload() })
            state.post != null -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    PostCard(state.post!!, me, onOpen = {}, onUser = onOpenUser,
                        onLike = { vm.toggleLike(state.post!!) },
                        onReply = { showReply = true },
                        onRepost = { feedVm.repost(state.post!!) { e -> Toast.makeText(ctx, e ?: "بازنشر شد 🔁", Toast.LENGTH_SHORT).show(); vm.reload() } },
                        onLikers = { dLikers = state.post }, onLongPress = { dMenu = state.post }, clickable = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.fillMaxWidth().clickable { showReply = true }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(me, size = 32.dp, avatarUrl = Store.prefs.value.avatar)
                        Spacer(Modifier.width(10.dp))
                        Text("پاسخت رو بنویس…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                items(state.replies, key = { "r${it.id}" }) { r ->
                    PostCard(r, me, onOpen = {}, onUser = onOpenUser, onLike = { vm.toggleLike(r) },
                        onReply = { showReply = true }, onRepost = {}, onLikers = { dLikers = r }, onLongPress = { dMenu = r }, clickable = false)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                if (state.replies.isEmpty()) item { EmptyState("💬", "هنوز پاسخی نیست — اولین نفر باش!") }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
    if (showReply) ComposerSheet(feedVm, replyTo = state.post, onDismiss = { showReply = false; vm.reload() })

    dMenu?.let { mp ->
        PostGlassMenu(mp, me, onDismiss = { dMenu = null }, onOpenUser = onOpenUser,
            onEdit = { dEdit = it }, onQuote = { }, onDeleted = { onBack() })
    }
    dEdit?.let { ep -> EditPostSheet(ep, onDismiss = { dEdit = null }, onSaved = { dEdit = null; vm.load(id) }) }
    dLikers?.let { lp -> LikersSheet(lp.repostOf ?: lp.id, onDismiss = { dLikers = null }, onOpenUser = { u -> dLikers = null; onOpenUser(u) }) }
}

// ==================== پروفایل کاربر (سبک توییتر) ====================
@Composable
fun SocialProfileScreen(
    username: String, onBack: () -> Unit, onOpenPost: (Int) -> Unit, onOpenUser: (String) -> Unit,
    vm: SocialProfileViewModel = viewModel(key = "soc_$username"),
) {
    var userMenu by remember { mutableStateOf(false) }
    val profScope = androidx.compose.runtime.rememberCoroutineScope()
    var profMenu by remember { mutableStateOf<Post?>(null) }
    var profEdit by remember { mutableStateOf<Post?>(null) }
    var profQuote by remember { mutableStateOf<Post?>(null) }
    var profLikers by remember { mutableStateOf<Post?>(null) }

    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val feedVm: FeedViewModel = viewModel()
    val me = Store.username ?: ""
    var followList by remember { mutableStateOf<String?>(null) } // "followers" | "following"
    LaunchedEffect(username) { vm.load(username) }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "بازگشت") }
                Text(username, style = MaterialTheme.typography.titleMedium)
            }
        }
        when {
            state.loading -> Column(Modifier.padding(16.dp)) { LoadingBox(height = 260.dp) }
            state.error != null -> ErrorState(state.error!!, onRetry = { vm.load(username) })
            state.profile != null -> {
                val p = state.profile!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        var showBanner by remember { mutableStateOf(false) }
                        var bannerId by remember(p.banner) { mutableStateOf(p.banner) }
                        Box(Modifier.fillMaxWidth().height(110.dp).background(bannerBrush(bannerId))) {
                            if (p.username == me) {
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f),
                                    onClick = { showBanner = true }, modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                                    Text("تغییر هدر 🎨", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                }
                            } else {
                                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.35f),
                                    onClick = { userMenu = true }, modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                                    Icon(Icons.Rounded.MoreHoriz, "گزینه‌ها", tint = Color.White,
                                        modifier = Modifier.padding(6.dp).size(18.dp))
                                }
                            }
                        }
                        if (showBanner) BannerPickerSheet(bannerId, onPicked = { bannerId = it; showBanner = false }, onDismiss = { showBanner = false })
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.graphicsLayer { translationY = -46f }) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface) {
                                    Box(Modifier.padding(4.dp)) { Avatar(p.username, p.mood, size = 76.dp, online = p.isOnline, avatarUrl = p.avatar) }
                                }
                                Spacer(Modifier.weight(1f))
                                if (p.username != me) {
                                    Row {
                                        OutlinedButton(onClick = { NavBus.requestDm(p.username) }, shape = CircleShape) { Text("پیام 💬") }
                                        Spacer(Modifier.width(8.dp))
                                        FollowChip(p.isFollowing) { vm.toggleFollow { e -> if (e != null) Toast.makeText(ctx, e, Toast.LENGTH_SHORT).show() } }
                                    }
                                }
                            }
                            Column(Modifier.graphicsLayer { translationY = -34f }) {
                                Text("${p.username} ${p.mood ?: ""}", style = MaterialTheme.typography.titleLarge)
                                if (!p.statusText.isNullOrBlank()) Text(p.statusText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (p.followsMe && p.username != me) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text("دنبالت می‌کنه", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    CountLabel(p.following, "دنبال‌شده") { followList = "following" }
                                    CountLabel(p.followers, "دنبال‌کننده") { followList = "followers" }
                                    CountLabel(p.postsCount, "پست") { }
                                }
                                Spacer(Modifier.height(12.dp))
                                // آمار مطالعه — فقط برای دنبال‌شده‌ها یا خود کاربر نمایش کامل
                                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                        StudyStat("امروز", if (p.isFollowing || p.username == me) minutesNum(p.todayMinutes) else "🔒")
                                        StudyStat("این هفته", if (p.isFollowing || p.username == me) minutesNum(p.weekMinutes) else "🔒")
                                        StudyStat("استریک", p.streak.fa())
                                    }
                                }
                                if (!p.isFollowing && p.username != me) {
                                    Text("برای دیدن ساعت مطالعه‌ش دنبالش کن 🔓", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    items(state.posts, key = { "pp${it.id}" }) { post ->
                        PostCard(post, me, onOpen = { onOpenPost(post.id) }, onUser = { if (it != username) onOpenUser(it) },
                            onLike = { vm.toggleLike(post) }, onReply = { onOpenPost(post.id) },
                            onRepost = { feedVm.repost(post) { e -> Toast.makeText(ctx, e ?: "بازنشر شد 🔁", Toast.LENGTH_SHORT).show() } },
                            onLikers = { profLikers = post },
                            onLongPress = { profMenu = post })
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                    if (state.posts.isEmpty()) item { EmptyState("📝", "هنوز پستی نذاشته") }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
    followList?.let { kind ->
        FollowListSheet(username = username, kind = kind, onDismiss = { followList = null }, onOpenUser = { u -> followList = null; onOpenUser(u) })
    }

    profMenu?.let { mp ->
        PostGlassMenu(mp, me, onDismiss = { profMenu = null }, onOpenUser = { if (it != username) onOpenUser(it) },
            onEdit = { profEdit = it }, onQuote = { profQuote = it },
            onDeleted = { vm.load(username) })
    }
    profEdit?.let { ep -> EditPostSheet(ep, onDismiss = { profEdit = null }, onSaved = { profEdit = null; vm.load(username) }) }
    profLikers?.let { lp -> LikersSheet(lp.repostOf ?: lp.id, onDismiss = { profLikers = null }, onOpenUser = { u -> profLikers = null; if (u != username) onOpenUser(u) }) }
    profQuote?.let { qp ->
        ComposerSheet(feedVm, replyTo = null, quoteOf = qp, onDismiss = { profQuote = null })
    }

    if (userMenu) {
        val blocked = state.profile?.iBlocked == true
        GlassMenu(onDismiss = { userMenu = false }) {
            GlassAction(Icons.Rounded.Block, if (blocked) "آنبلاک $username" else "بلاک کردن $username", danger = !blocked) {
                profScope.launch {
                    runCatching { Api.post(if (blocked) "unblock" else "block", JSONObject().put("username", username)) }
                        .onSuccess {
                            Toast.makeText(ctx, if (blocked) "آنبلاک شد" else "بلاک شد 🚫", Toast.LENGTH_SHORT).show()
                            vm.load(username)
                        }
                        .onFailure { Toast.makeText(ctx, it.message ?: "خطا", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
}

@Composable
private fun StudyStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CountLabel(count: Int, label: String, onClick: () -> Unit) {
    Row(Modifier.clip(CircleShape).clickable { onClick() }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(count.fa(), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FollowChip(following: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (following) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
        onClick = onClick,
    ) {
        Text(if (following) "دنبال می‌کنی ✓" else "دنبال کن +",
            color = if (following) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
    }
}

/** لیست فالوورها/فالویینگ‌ها — از پروفایل اپ هم استفاده می‌شود. */
@Composable
fun FollowListSheet(username: String, kind: String, onDismiss: () -> Unit, onOpenUser: (String) -> Unit) {
    val ctx = LocalContext.current
    var list by remember { mutableStateOf<List<FollowUser>?>(null) }
    val me = Store.username ?: ""
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(username, kind) {
        runCatching { FollowUser.list(Api.arr(Api.get(kind, "username" to username))) }
            .onSuccess { list = it }.onFailure { list = emptyList() }
    }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text(if (kind == "followers") "دنبال‌کننده‌ها 🫂" else "دنبال‌شده‌ها ✨", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            when {
                list == null -> LoadingBox(height = 140.dp)
                list!!.isEmpty() -> EmptyState("🍃", "هنوز کسی اینجا نیست")
                else -> LazyColumn(Modifier.height(400.dp)) {
                    items(list!!, key = { it.username }) { u ->
                        var isF by remember { mutableStateOf(u.isFollowing) }
                        Row(Modifier.fillMaxWidth().clickable { onOpenUser(u.username) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(u.username, u.mood, size = 40.dp, avatarUrl = u.avatar)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.username, style = MaterialTheme.typography.titleSmall)
                                if (u.followsMe) Text("دنبالت می‌کنه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (u.username != me) FollowChip(isF) {
                                isF = !isF
                                scope.launch {
                                    runCatching { Api.post(if (!isF) "unfollow" else "follow", JSONObject().put("username", u.username)) }
                                        .onSuccess { RefreshBus.emit("feed"); RefreshBus.emit("home") }
                                        .onFailure { isF = !isF; Toast.makeText(ctx, it.message ?: "خطا", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==================== کامپوزر پست (سبک توییتر) ====================
@Composable
private fun ComposerSheet(vm: FeedViewModel, replyTo: Post?, quoteOf: Post? = null, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    var text by remember { mutableStateOf("") }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf<String?>(null) }
    var musicPick by remember { mutableStateOf<MusicTrack?>(null) }
    var showMusicPicker by remember { mutableStateOf(false) }
    val me = Store.username ?: ""
    val maxLen = 500

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) { mediaUri = u; mediaType = "image"; musicPick = null } }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) { mediaUri = u; mediaType = "video"; musicPick = null } }

    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { if (!state.posting) onDismiss() }) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (quoteOf != null) "نقل‌قول 🔁" else if (replyTo != null) "پاسخ به ${replyTo.username} 💬" else "پست جدید ✍️", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${text.length.fa()}/${maxLen.fa()}", style = MaterialTheme.typography.labelSmall,
                    color = if (text.length > maxLen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            (quoteOf ?: replyTo)?.let { qp ->
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Text("${qp.username}: ${(qp.text ?: "📎 رسانه").take(80)}", style = MaterialTheme.typography.bodySmall,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(10.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Avatar(me, size = 38.dp, avatarUrl = Store.prefs.value.avatar)
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = text, onValueChange = { if (it.length <= maxLen + 40) text = it },
                    placeholder = { Text(if (replyTo != null) "پاسخت رو بنویس…" else "چه خبر از درس و مشق؟ 📖") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
            }
            // پیش‌نمایش مدیا
            AnimatedVisibility(visible = mediaUri != null || musicPick != null, enter = fadeIn() + scaleIn(initialScale = 0.95f), exit = fadeOut() + scaleOut()) {
                Box(Modifier.padding(vertical = 6.dp)) {
                    when {
                        mediaType == "image" && mediaUri != null -> AsyncImage(model = mediaUri, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)))
                        mediaType == "music" && mediaUri != null -> Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("🎵 ${fileNameOf(ctx, mediaUri!!)}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        mediaType == "video" && mediaUri != null -> Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Videocam, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("🎬 ${fileNameOf(ctx, mediaUri!!)}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        musicPick != null -> Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Text("🎵 ${musicPick!!.title} — ${musicPick!!.artist}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f),
                        onClick = { mediaUri = null; mediaType = null; musicPick = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                        Icon(Icons.Rounded.Close, "حذف", tint = Color.White, modifier = Modifier.padding(5.dp).size(16.dp))
                    }
                }
            }
            state.uploadPercent?.let {
                LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ComposerAttach(Icons.Rounded.Image, "عکس") { pickImage.launch("image/*") }
                ComposerAttach(Icons.Rounded.Videocam, "ویدئو") { pickVideo.launch("video/*") }
                ComposerAttach(Icons.Rounded.MusicNote, "موزیک") { showMusicPicker = true }
                Spacer(Modifier.weight(1f))
                val canSend = !state.posting && (text.isNotBlank() || mediaUri != null || musicPick != null) && text.length <= maxLen
                Surface(shape = CircleShape, color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = {
                        if (!canSend) return@Surface
                        vm.createPost(ctx, text.trim(), mediaUri, mediaType, musicPick?.id, replyTo?.id, repostOf = quoteOf?.let { it.repostOf ?: it.id }) { e ->
                            if (e == null) { Toast.makeText(ctx, "منتشر شد 🎉", Toast.LENGTH_SHORT).show(); onDismiss() }
                            else Toast.makeText(ctx, e, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                    if (state.posting) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(12.dp).size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("انتشار", color = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp))
                }
            }
        }
    }
    if (showMusicPicker) {
        MusicSourceSheet(
            onLibrary = { t -> musicPick = t; mediaUri = null; mediaType = null; showMusicPicker = false },
            onDevice = { u -> mediaUri = u; mediaType = "music"; musicPick = null; showMusicPicker = false },
            onDismiss = { showMusicPicker = false })
    }
}

@Composable
private fun ComposerAttach(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), onClick = onClick, modifier = Modifier.padding(end = 8.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}


/** انتخاب موزیک یکپارچه: از کتابخانه شادبیب یا فایل‌های دستگاه — یک بخش برای هر دو. */
@Composable
fun MusicSourceSheet(onLibrary: (MusicTrack) -> Unit, onDevice: (Uri) -> Unit, onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    val pickAudio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { u -> if (u != null) onDevice(u) }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 20.dp)) {
            Text("انتخاب موزیک 🎵", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            SegmentedTabs(options = listOf("کتابخانه شادبیب", "فایل‌های دستگاه"), selected = tab) { t ->
                tab = t
                if (t == 1) pickAudio.launch("audio/*")
            }
            Spacer(Modifier.height(8.dp))
            if (tab == 0) {
                ir.shadbib.app.ui.messages.ChatMusicPickerInline(onPick = onLibrary)
            } else {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    onClick = { pickAudio.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("انتخاب فایل صوتی از دستگاه", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

/** لیست لایک‌کننده‌های پست — مثل توییتر. */
@Composable
fun LikersSheet(postId: Int, onDismiss: () -> Unit, onOpenUser: (String) -> Unit) {
    var list by remember { mutableStateOf<List<FollowUser>?>(null) }
    LaunchedEffect(postId) {
        runCatching { FollowUser.list(Api.arr(Api.get("post_likers", "id" to postId.toString()))) }
            .onSuccess { list = it }.onFailure { list = emptyList() }
    }
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text("لایک‌ها ❤️", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            when {
                list == null -> LoadingBox(height = 140.dp)
                list!!.isEmpty() -> EmptyState("💔", "هنوز کسی لایک نکرده")
                else -> LazyColumn(Modifier.height(380.dp)) {
                    items(list!!, key = { it.username }) { u ->
                        Row(Modifier.fillMaxWidth().clickable { onOpenUser(u.username) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Avatar(u.username, u.mood, size = 40.dp, avatarUrl = u.avatar)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(u.username, style = MaterialTheme.typography.titleSmall)
                                if (u.followsMe) Text("دنبالت می‌کنه", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** ویرایش پست خود کاربر. */
@Composable
fun EditPostSheet(p: Post, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(p.text ?: "") }
    var saving by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp).imePadding()) {
            Text("ویرایش پست ✏️", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TextField(value = text, onValueChange = { if (it.length <= 500) text = it },
                modifier = Modifier.fillMaxWidth().height(130.dp), shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent))
            Spacer(Modifier.height(10.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, onClick = {
                if (text.isBlank() || saving) return@Surface
                saving = true
                scope.launch {
                    runCatching { Api.post("post_edit", JSONObject().put("id", p.id).put("text", text.trim())) }
                        .onSuccess { Toast.makeText(ctx, "ویرایش شد ✅", Toast.LENGTH_SHORT).show(); onSaved() }
                        .onFailure { saving = false; Toast.makeText(ctx, it.message ?: "خطا", Toast.LENGTH_SHORT).show() }
                }
            }, modifier = Modifier.align(Alignment.End)) {
                Text(if (saving) "در حال ذخیره…" else "ذخیره", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp))
            }
        }
    }
}


/** پریست‌های هدر پروفایل — قابل انتخاب توسط کاربر. */
val BannerPresets = listOf(
    "brand" to listOf(Color(0xFF059669), Color(0xFF0EA5E9)),
    "night" to listOf(Color(0xFF312E81), Color(0xFF7C3AED)),
    "sunset" to listOf(Color(0xFFB45309), Color(0xFFE11D48)),
    "ocean" to listOf(Color(0xFF075985), Color(0xFF06B6D4)),
    "rose" to listOf(Color(0xFFBE185D), Color(0xFFF472B6)),
    "gold" to listOf(Color(0xFF92400E), Color(0xFFFBBF24)),
    "forest" to listOf(Color(0xFF14532D), Color(0xFF4ADE80)),
    "mono" to listOf(Color(0xFF1F2937), Color(0xFF6B7280)),
)
fun bannerBrush(id: String): androidx.compose.ui.graphics.Brush {
    val c = BannerPresets.firstOrNull { it.first == id }?.second ?: BannerPresets.first().second
    return androidx.compose.ui.graphics.Brush.linearGradient(c)
}

/** زمان مطالعه به‌صورت عددی خلاصه: «۳:۴۵». */
fun minutesNum(total: Int): String {
    val h = total / 60; val m = total % 60
    return if (h > 0) "${h.fa()}:${m.toString().padStart(2, '0').fa()}" else "${m.fa()} د"
}

/** انتخاب هدر پروفایل. */
@Composable
fun BannerPickerSheet(current: String, onPicked: (String) -> Unit, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 26.dp)) {
            Text("انتخاب هدر پروفایل 🎨", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            BannerPresets.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth()) {
                    rowItems.forEach { (id, colors) ->
                        Box(Modifier.weight(1f).padding(5.dp).height(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(colors))
                            .clickable {
                                scope.launch {
                                    runCatching { Api.post("profile_banner", JSONObject().put("banner", id)) }
                                        .onSuccess { onPicked(id) }
                                        .onFailure { Toast.makeText(ctx, it.message ?: "خطا", Toast.LENGTH_SHORT).show() }
                                }
                            }, contentAlignment = Alignment.Center) {
                            if (current == id) Text("✓", color = Color.White, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

/** پروفایل اجتماعی به‌صورت دیالوگ تمام‌صفحه — از هر جای اپ (چت، کانال، گروه) باز می‌شود. */
@Composable
fun SocialProfileDialog(username: String, onDismiss: () -> Unit) {
    var postId by remember { mutableStateOf<Int?>(null) }
    var user by remember { mutableStateOf(username) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val pid = postId
            if (pid != null) {
                PostDetailScreen(pid, onBack = { postId = null }, onOpenUser = { user = it; postId = null })
            } else {
                SocialProfileScreen(user, onBack = onDismiss, onOpenPost = { postId = it }, onOpenUser = { user = it })
            }
        }
    }
}


/** منوی شیشه‌ای مشترک پست — در فید، پروفایل و جزئیات پست استفاده می‌شود. */
@Composable
fun PostGlassMenu(p: Post, me: String, onDismiss: () -> Unit,
                  onOpenUser: (String) -> Unit, onEdit: (Post) -> Unit, onQuote: (Post) -> Unit,
                  onDeleted: () -> Unit) {
    val ctx = LocalContext.current
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    GlassMenu(onDismiss = onDismiss) {
        if (p.username == me) {
            GlassAction(Icons.Rounded.Edit, "ویرایش پست") { onEdit(p) }
            GlassAction(Icons.Rounded.Delete, "حذف پست", danger = true) {
                scope.launch {
                    runCatching { Api.delete("post_delete", "id" to p.id.toString()) }
                        .onSuccess { Toast.makeText(ctx, "حذف شد", Toast.LENGTH_SHORT).show(); onDeleted() }
                        .onFailure { Toast.makeText(ctx, it.message ?: "خطا", Toast.LENGTH_SHORT).show() }
                }
            }
        } else {
            GlassAction(Icons.Rounded.Person, "دیدن پروفایل ${p.username}") { onOpenUser(p.username) }
            GlassAction(Icons.AutoMirrored.Rounded.Send, "پیام خصوصی") { NavBus.requestDm(p.username) }
        }
        GlassDivider()
        GlassAction(Icons.Rounded.FormatQuote, "نقل‌قول با متن") { onQuote(p) }
        if (!p.text.isNullOrBlank()) GlassAction(Icons.Rounded.ContentCopy, "کپی متن") {
            clip.setText(androidx.compose.ui.text.AnnotatedString(p.text))
            Toast.makeText(ctx, "کپی شد", Toast.LENGTH_SHORT).show()
        }
    }
}
