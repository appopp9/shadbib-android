@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ir.darshub.app.ui.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.darshub.app.core.Api
import ir.darshub.app.core.Store
import ir.darshub.app.core.fa
import ir.darshub.app.data.MusicTrack
import ir.darshub.app.data.Playlist
import ir.darshub.app.player.PlayerHolder
import ir.darshub.app.ui.components.EmptyState
import ir.darshub.app.ui.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class MusicViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val mode: Int = 0, // 0=all 1=popular 2=playlists
        val tracks: List<MusicTrack> = emptyList(),
        val popular: List<MusicTrack> = emptyList(),
        val playlists: List<Playlist> = emptyList(),
        val query: String = "",
        val searchResults: List<MusicTrack>? = null,
        val openPlaylist: Playlist? = null,
        val playlistTracks: List<MusicTrack> = emptyList(),
        val uploading: Boolean = false,
    )

    val state = MutableStateFlow(State())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            runCatching { MusicTrack.list(Api.arr(Api.get("music_list", "sort" to "newest"))) }
                .onSuccess { l -> state.update { it.copy(loading = false, tracks = l) } }
                .onFailure { state.update { it.copy(loading = false) } }
            runCatching { MusicTrack.list(Api.arr(Api.get("music_popular"))) }
                .onSuccess { l -> state.update { it.copy(popular = l) } }
            runCatching { Playlist.list(Api.arr(Api.get("playlist_list"))) }
                .onSuccess { l -> state.update { it.copy(playlists = l) } }
        }
    }

    fun setMode(m: Int) = state.update { it.copy(mode = m) }

    fun setQuery(q: String) {
        state.update { it.copy(query = q) }
        if (q.isBlank()) {
            state.update { it.copy(searchResults = null) }
        }
    }

    fun search() {
        val q = state.value.query.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            runCatching { MusicTrack.list(Api.arr(Api.get("music_search", "q" to q))) }
                .onSuccess { l -> state.update { it.copy(searchResults = l) } }
        }
    }

    fun like(track: MusicTrack, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.get("music_like", "id" to track.id.toString())
                onResult(null); refresh()
            } catch (e: Exception) {
                if ((e.message ?: "").contains("قبلا") || (e.message ?: "").contains("قبلاً")) {
                    runCatching { Api.get("music_unlike", "id" to track.id.toString()) }
                    onResult(null); refresh()
                } else onResult(e.message ?: "خطا")
            }
        }
    }

    fun delete(track: MusicTrack) {
        viewModelScope.launch {
            runCatching { Api.get("music_delete", "id" to track.id.toString()) }
            refresh()
        }
    }

    fun createPlaylist(name: String, isPublic: Boolean, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.post("playlist_create", JSONObject().put("name", name).put("is_public", if (isPublic) 1 else 0))
                onResult(null); refresh()
            } catch (e: Exception) { onResult(e.message ?: "خطا") }
        }
    }

    fun addToPlaylist(playlistId: Int, musicId: Int, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.post("playlist_add", JSONObject().put("playlist_id", playlistId).put("music_id", musicId))
                onResult(null); refresh()
            } catch (e: Exception) { onResult(e.message ?: "خطا") }
        }
    }

    fun openPlaylist(p: Playlist) {
        viewModelScope.launch {
            state.update { it.copy(openPlaylist = p, playlistTracks = emptyList()) }
            runCatching { MusicTrack.list(Api.arr(Api.get("playlist_music", "playlist_id" to p.id.toString()))) }
                .onSuccess { l -> state.update { it.copy(playlistTracks = l) } }
        }
    }

    fun closePlaylist() = state.update { it.copy(openPlaylist = null) }

    fun removeFromPlaylist(playlistId: Int, musicId: Int) {
        viewModelScope.launch {
            runCatching {
                Api.post("playlist_remove", JSONObject().put("playlist_id", playlistId).put("music_id", musicId))
            }
            state.value.openPlaylist?.let { openPlaylist(it) }
        }
    }

    fun deletePlaylist(id: Int) {
        viewModelScope.launch {
            runCatching { Api.get("playlist_delete", "id" to id.toString()) }
            closePlaylist(); refresh()
        }
    }

    fun renamePlaylist(id: Int, name: String) {
        viewModelScope.launch {
            runCatching { Api.post("playlist_rename", JSONObject().put("playlist_id", id).put("name", name)) }
            refresh()
        }
    }

    fun togglePublic(id: Int) {
        viewModelScope.launch {
            runCatching { Api.post("playlist_toggle_public", JSONObject().put("playlist_id", id)) }
            refresh()
        }
    }

    fun upload(
        ctx: Context, title: String, artist: String, musicUri: Uri, coverBytes: ByteArray?,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            state.update { it.copy(uploading = true) }
            try {
                val files = withContext(Dispatchers.IO) {
                    val list = mutableListOf<Api.FilePart>()
                    val musicBytes = ctx.contentResolver.openInputStream(musicUri)?.readBytes()
                        ?: throw Exception("خطا در خواندن فایل")
                    val musicName = fileNameOf(ctx, musicUri).ifBlank { "audio.mp3" }
                    val musicMime = ctx.contentResolver.getType(musicUri) ?: "audio/mpeg"
                    list.add(Api.FilePart("music_file", musicName, musicBytes, musicMime))
                    if (coverBytes != null && coverBytes.isNotEmpty()) {
                        list.add(Api.FilePart("cover_file", "cover.jpg", coverBytes, "image/jpeg"))
                    }
                    list
                }
                Api.upload("music_upload", mapOf("title" to title, "artist" to artist), files)
                onResult(null)
                refresh()
            } catch (e: Exception) {
                onResult(e.message ?: "خطا در آپلود")
            }
            state.update { it.copy(uploading = false) }
        }
    }
}

fun fileNameOf(ctx: Context, uri: Uri): String {
    var name = ""
    runCatching {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: ""
        }
    }
    return name
}

@Composable
fun MusicTab(openUploadSignal: Int = 0, vm: MusicViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val me = Store.username ?: ""
    var menuTrack by remember { mutableStateOf<MusicTrack?>(null) }
    var pickPlaylistFor by remember { mutableStateOf<MusicTrack?>(null) }
    var showUpload by remember { mutableStateOf(false) }
    LaunchedEffect(openUploadSignal) { if (openUploadSignal > 0) showUpload = true }
    var showCreatePlaylist by remember { mutableStateOf(false) }

    LaunchedEffect(state.query) {
        kotlinx.coroutines.delay(450)
        if (state.query.isNotBlank()) vm.search()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(selected = state.mode == 0, onClick = { vm.setMode(0) }, label = { Text("همه") })
                FilterChip(selected = state.mode == 1, onClick = { vm.setMode(1) }, label = { Text("محبوب 🔥") })
                FilterChip(selected = state.mode == 2, onClick = { vm.setMode(2) }, label = { Text("پلی‌لیست‌ها") })
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showUpload = true }) {
                    Icon(Icons.Rounded.Upload, contentDescription = "آپلود", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (state.mode == 0) {
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { vm.setQuery(it) },
                    placeholder = { Text("جستجوی آهنگ یا خواننده…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            val list = state.searchResults ?: state.tracks
            when {
                state.loading -> item { LoadingBox(height = 140.dp) }
                list.isEmpty() -> item { EmptyState("🎵", "موزیکی پیدا نشد — اولین موزیک رو تو آپلود کن!") }
                else -> items(list, key = { it.id }) { t ->
                    TrackRow(
                        t = t,
                        onPlay = { PlayerHolder.play(list, list.indexOf(t)) },
                        onLike = { vm.like(t) { err -> if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show() } },
                        onMenu = { menuTrack = t },
                    )
                }
            }
        }

        if (state.mode == 1) {
            when {
                state.popular.isEmpty() -> item { EmptyState("🔥", "هنوز موزیک محبوبی نیست") }
                else -> items(state.popular, key = { it.id }) { t ->
                    TrackRow(
                        t = t,
                        onPlay = { PlayerHolder.play(state.popular, state.popular.indexOf(t)) },
                        onLike = { vm.like(t) { err -> if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show() } },
                        onMenu = { menuTrack = t },
                    )
                }
            }
        }

        if (state.mode == 2) {
            item {
                OutlinedButton(
                    onClick = { showCreatePlaylist = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("➕ پلی‌لیست جدید") }
            }
            when {
                state.playlists.isEmpty() -> item { EmptyState("📋", "پلی‌لیستی وجود ندارد") }
                else -> items(state.playlists, key = { it.id }) { p ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        onClick = { vm.openPlaylist(p) },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (p.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${p.musicCount.fa()} آهنگ · ${p.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    // Track context menu
    if (menuTrack != null) {
        val t = menuTrack!!
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { menuTrack = null }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrackCover(t, 46.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(t.title, style = MaterialTheme.typography.titleSmall)
                        Text(t.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Surface(color = androidx.compose.ui.graphics.Color.Transparent, onClick = { pickPlaylistFor = t; menuTrack = null }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("افزودن به پلی‌لیست", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                if (t.username == me) {
                    Surface(color = androidx.compose.ui.graphics.Color.Transparent, onClick = { vm.delete(t); menuTrack = null }, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(10.dp))
                            Text("حذف موزیک", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    // Pick playlist for adding a track
    if (pickPlaylistFor != null) {
        val t = pickPlaylistFor!!
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { pickPlaylistFor = null }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
                Text("انتخاب پلی‌لیست", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                val myPlaylists = state.playlists.filter { it.username == me }
                if (myPlaylists.isEmpty()) {
                    EmptyState("📋", "هنوز پلی‌لیستی نساختی")
                } else {
                    myPlaylists.forEach { p ->
                        Surface(
                            color = androidx.compose.ui.graphics.Color.Transparent,
                            onClick = {
                                vm.addToPlaylist(p.id, t.id) { err ->
                                    Toast.makeText(ctx, err ?: "به «${p.name}» اضافه شد ✅", Toast.LENGTH_SHORT).show()
                                }
                                pickPlaylistFor = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(Modifier.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (p.isPublic) Icons.Rounded.Public else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(p.name, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { showCreatePlaylist = true; pickPlaylistFor = null },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("➕ پلی‌لیست جدید") }
            }
        }
    }

    // Playlist detail
    val openPl = state.openPlaylist
    if (openPl != null) {
        var renameText by remember(openPl.id) { mutableStateOf<String?>(null) }
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { vm.closePlaylist() }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(openPl.name, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${state.playlistTracks.size.fa()} آهنگ · ${if (openPl.isPublic) "عمومی" else "خصوصی"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (state.playlistTracks.isNotEmpty()) {
                        Button(
                            onClick = { PlayerHolder.play(state.playlistTracks, 0) },
                            shape = MaterialTheme.shapes.medium,
                        ) { Text("▶ پخش") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (openPl.username == me) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { renameText = openPl.name }, shape = MaterialTheme.shapes.small) { Text("تغییر نام") }
                        OutlinedButton(onClick = { vm.togglePublic(openPl.id) }, shape = MaterialTheme.shapes.small) {
                            Text(if (openPl.isPublic) "خصوصی کن" else "عمومی کن")
                        }
                        OutlinedButton(onClick = { vm.deletePlaylist(openPl.id) }, shape = MaterialTheme.shapes.small) {
                            Text("حذف", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                if (state.playlistTracks.isEmpty()) {
                    EmptyState("🎵", "این پلی‌لیست خالیه")
                } else {
                    state.playlistTracks.forEachIndexed { idx, t ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                color = androidx.compose.ui.graphics.Color.Transparent,
                                onClick = { PlayerHolder.play(state.playlistTracks, idx) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TrackCover(t, 40.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(t.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(t.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            if (openPl.username == me) {
                                IconButton(onClick = { vm.removeFromPlaylist(openPl.id, t.id) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (renameText != null) {
            AlertDialog(
                onDismissRequest = { renameText = null },
                title = { Text("تغییر نام پلی‌لیست") },
                text = {
                    OutlinedTextField(
                        value = renameText ?: "",
                        onValueChange = { renameText = it },
                        shape = MaterialTheme.shapes.medium,
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        vm.renamePlaylist(openPl.id, (renameText ?: "").trim())
                        renameText = null
                        vm.closePlaylist()
                    }) { Text("ذخیره") }
                },
                dismissButton = { TextButton(onClick = { renameText = null }) { Text("انصراف") } },
            )
        }
    }

    if (showCreatePlaylist) {
        var name by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showCreatePlaylist = false },
            title = { Text("پلی‌لیست جدید") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام پلی‌لیست") },
                        shape = MaterialTheme.shapes.medium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPublic) "عمومی 🔓" else "خصوصی 🔒")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createPlaylist(name.trim().ifBlank { "پلی‌لیست جدید" }, isPublic) { err ->
                        Toast.makeText(ctx, err ?: "ساخته شد ✅", Toast.LENGTH_SHORT).show()
                    }
                    showCreatePlaylist = false
                }) { Text("ساخت") }
            },
            dismissButton = { TextButton(onClick = { showCreatePlaylist = false }) { Text("انصراف") } },
        )
    }

    if (showUpload) {
        UploadMusicSheet(
            uploading = state.uploading,
            onUpload = { title, artist, musicUri, coverBytes ->
                vm.upload(ctx, title, artist, musicUri, coverBytes) { err ->
                    Toast.makeText(ctx, err ?: "موزیک آپلود شد 🎵", Toast.LENGTH_LONG).show()
                    if (err == null) showUpload = false
                }
            },
            onDismiss = { if (!state.uploading) showUpload = false },
        )
    }
}

@Composable
private fun TrackRow(t: MusicTrack, onPlay: () -> Unit, onLike: () -> Unit, onMenu: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onPlay,
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TrackCover(t, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${t.artist} · ${t.username}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onLike, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "لایک",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(t.likes.fa(), style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onMenu, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "منو", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun UploadMusicSheet(
    uploading: Boolean,
    onUpload: (String, String, Uri, ByteArray?) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var coverBytes by remember { mutableStateOf<ByteArray?>(null) }
    var scanning by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // 🪄 با انتخاب فایل: کاور، عنوان و خواننده از خود فایل خوانده می‌شود
    fun scanTags(uri: Uri) {
        scanning = true
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val r = android.media.MediaMetadataRetriever()
            runCatching {
                r.setDataSource(ctx, uri)
                val art = r.embeddedPicture
                val t = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_TITLE)
                val a = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    coverBytes = art
                    if (!t.isNullOrBlank() && title.isBlank()) title = t
                    if (!a.isNullOrBlank() && artist.isBlank()) artist = a
                }
            }
            runCatching { r.release() }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) { scanning = false }
        }
    }

    val pickMusic = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { musicUri = uri; coverBytes = null; scanTags(uri) }
    }

    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 28.dp)) {
            Text("آپلود موزیک 🎵", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                label = { Text("عنوان") }, singleLine = true,
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = artist, onValueChange = { artist = it },
                label = { Text("خواننده") }, singleLine = true,
                shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { pickMusic.launch("audio/*") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    if (musicUri != null) "🎵 ${fileNameOf(ctx, musicUri!!)}" else "انتخاب فایل صوتی (حداکثر ۲۰MB)",
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(10.dp))
            // پیش‌نمایش کاور خودکار
            androidx.compose.animation.AnimatedVisibility(visible = musicUri != null) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        val bmp = remember(coverBytes) { coverBytes?.let { runCatching { android.graphics.BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() } }
                        if (bmp != null) {
                            androidx.compose.foundation.Image(bitmap = bmp.asImageBitmap(), contentDescription = "کاور",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.size(52.dp).clip(MaterialTheme.shapes.small))
                        } else {
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                                Text("🎵", modifier = Modifier.padding(14.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                when { scanning -> "در حال خواندن اطلاعات فایل… ✨"
                                    bmp != null -> "کاور به‌صورت خودکار شناسایی شد ✅"
                                    else -> "این فایل کاور داخلی ندارد — بدون کاور آپلود می‌شود" },
                                style = MaterialTheme.typography.labelMedium,
                            )
                            Text("کاور، عنوان و خواننده از خود فایل خوانده می‌شود 🪄",
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (uploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    "در حال آپلود… چند لحظه صبر کن",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Button(
                    onClick = { musicUri?.let { onUpload(title.trim().ifBlank { "بدون عنوان" }, artist.trim().ifBlank { "ناشناس" }, it, coverBytes) } },
                    enabled = musicUri != null,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = MaterialTheme.shapes.medium,
                ) { Text("آپلود ⬆") }
            }
        }
    }
}
