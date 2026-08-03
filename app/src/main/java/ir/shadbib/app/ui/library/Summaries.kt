@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package ir.shadbib.app.ui.library

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.CommentItem
import ir.shadbib.app.data.Summary
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.LoadingBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

val SUMMARY_CATEGORIES = listOf(
    "تجربی-زیست" to "🧬 زیست",
    "تجربی-شیمی" to "🧪 شیمی",
    "تجربی-فیزیک" to "⚡ فیزیک",
    "تجربی-ریاضی" to "🧮 ریاضی",
    "تجربی-زمین" to "🌍 زمین",
    "ریاضی-حسابان" to "📊 حسابان",
    "ریاضی-هندسه" to "📏 هندسه",
    "ریاضی-گسسته" to "🔢 گسسته",
    "ریاضی-فیزیک" to "⚡ فیزیک (ریاضی)",
    "ریاضی-شیمی" to "🧪 شیمی (ریاضی)",
)

fun categoryLabel(value: String): String =
    SUMMARY_CATEGORIES.find { it.first == value }?.second ?: value

class SummariesViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val summaries: List<Summary> = emptyList(),
        val category: String = "",
        val detail: Summary? = null,
        val comments: List<CommentItem> = emptyList(),
        val uploading: Boolean = false,
    )

    val state = MutableStateFlow(State())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val cat = state.value.category
            runCatching {
                val params = if (cat.isNotBlank()) arrayOf("category" to cat) else emptyArray()
                Summary.list(Api.arr(Api.get("summaries", *params)))
            }.onSuccess { l -> state.update { it.copy(loading = false, summaries = l) } }
                .onFailure { state.update { it.copy(loading = false) } }
        }
    }

    fun setCategory(cat: String) {
        state.update { it.copy(category = cat, loading = true) }
        refresh()
    }

    fun openDetail(s: Summary) {
        state.update { it.copy(detail = s, comments = emptyList()) }
        viewModelScope.launch {
            runCatching { CommentItem.list(Api.arr(Api.get("summary_comments", "summary_id" to s.id.toString()))) }
                .onSuccess { l -> state.update { it.copy(comments = l) } }
        }
    }

    fun closeDetail() = state.update { it.copy(detail = null) }

    fun like(s: Summary, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.get("summary_like", "id" to s.id.toString())
                onResult(null); refresh()
            } catch (e: Exception) { onResult(e.message ?: "خطا") }
        }
    }

    fun delete(s: Summary) {
        viewModelScope.launch {
            runCatching { Api.get("summary_delete", "id" to s.id.toString()) }
            closeDetail(); refresh()
        }
    }

    fun addComment(summaryId: Int, text: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                Api.post("summary_comment", JSONObject().put("summary_id", summaryId).put("comment", text))
                onResult(null)
                state.value.detail?.let { openDetail(it) }
            } catch (e: Exception) { onResult(e.message ?: "خطا") }
        }
    }

    fun upload(
        ctx: Context, title: String, desc: String, category: String, images: List<Uri>,
        onResult: (String?) -> Unit,
    ) {
        viewModelScope.launch {
            state.update { it.copy(uploading = true) }
            try {
                val files = withContext(Dispatchers.IO) {
                    images.mapIndexed { i, uri ->
                        val bytes = ctx.contentResolver.openInputStream(uri)?.readBytes()
                            ?: throw Exception("خطا در خواندن تصویر")
                        val name = fileNameOf(ctx, uri).ifBlank { "image_$i.jpg" }
                        val mime = ctx.contentResolver.getType(uri) ?: "image/jpeg"
                        Api.FilePart("images[]", name, bytes, mime)
                    }
                }
                Api.upload(
                    "summary_upload",
                    mapOf("title" to title, "description" to desc, "category" to category),
                    files,
                )
                onResult(null)
                refresh()
            } catch (e: Exception) {
                onResult(e.message ?: "خطا در آپلود")
            }
            state.update { it.copy(uploading = false) }
        }
    }
}

@Composable
fun SummariesTab(openUploadSignal: Int = 0, vm: SummariesViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    val me = Store.username ?: ""
    var showUpload by remember { mutableStateOf(false) }
    LaunchedEffect(openUploadSignal) { if (openUploadSignal > 0) showUpload = true }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "جزوه‌ها و خلاصه‌ها 📄",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { showUpload = true }) {
                    Icon(Icons.Rounded.Upload, contentDescription = "آپلود", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = state.category.isEmpty(),
                        onClick = { vm.setCategory("") },
                        label = { Text("همه") },
                    )
                }
                items(SUMMARY_CATEGORIES) { (value, label) ->
                    FilterChip(
                        selected = state.category == value,
                        onClick = { vm.setCategory(value) },
                        label = { Text(label) },
                    )
                }
            }
        }
        when {
            state.loading -> item { LoadingBox(height = 140.dp) }
            state.summaries.isEmpty() -> item { EmptyState("📄", "جزوه‌ای پیدا نشد — اولین جزوه رو تو آپلود کن!") }
            else -> items(state.summaries, key = { it.id }) { s ->
                SummaryCard(s) { vm.openDetail(s) }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }

    val detail = state.detail
    if (detail != null) {
        var commentText by remember(detail.id) { mutableStateOf("") }
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = { vm.closeDetail() }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().imePadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
            ) {
                item {
                    if (detail.images.isNotEmpty()) {
                        val pager = rememberPagerState { detail.images.size }
                        Box {
                            HorizontalPager(state = pager) { page ->
                                AsyncImage(
                                    model = Api.mediaUrl(detail.images[page]),
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                            ) {
                                Text(
                                    "${(pager.currentPage + 1).fa()}/${detail.images.size.fa()}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Text(detail.title, style = MaterialTheme.typography.titleLarge)
                    if (detail.description.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            detail.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Avatar(detail.username, size = 28.dp)
                        Text(detail.username, style = MaterialTheme.typography.labelLarge)
                        Text(
                            Fmt.relative(detail.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (detail.category.isNotBlank()) {
                            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                                Text(
                                    categoryLabel(detail.category),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                vm.like(detail) { err ->
                                    Toast.makeText(ctx, err ?: "لایک شد ❤️", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${detail.likes.fa()} لایک")
                        }
                        if (detail.username == me) {
                            OutlinedButton(onClick = { vm.delete(detail) }, shape = MaterialTheme.shapes.medium) {
                                Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("حذف", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("💬 کامنت‌ها", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                }
                if (state.comments.isEmpty()) {
                    item {
                        Text(
                            "هنوز کامنتی نیست",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(state.comments, key = { it.id }) { c ->
                        Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                            Avatar(c.username, size = 28.dp)
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(c.username, style = MaterialTheme.typography.labelLarge)
                                Text(c.comment, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = commentText,
                            onValueChange = { if (it.length <= 500) commentText = it },
                            placeholder = { Text("کامنت بنویس…") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                vm.addComment(detail.id, commentText.trim()) { err ->
                                    if (err != null) Toast.makeText(ctx, err, Toast.LENGTH_SHORT).show()
                                }
                                commentText = ""
                            },
                            enabled = commentText.isNotBlank(),
                            shape = MaterialTheme.shapes.medium,
                        ) { Text("ارسال") }
                    }
                }
            }
        }
    }

    if (showUpload) {
        UploadSummarySheet(
            uploading = state.uploading,
            onUpload = { title, desc, cat, images ->
                vm.upload(ctx, title, desc, cat, images) { err ->
                    Toast.makeText(ctx, err ?: "جزوه آپلود شد 📄", Toast.LENGTH_LONG).show()
                    if (err == null) showUpload = false
                }
            },
            onDismiss = { if (!state.uploading) showUpload = false },
        )
    }
}

@Composable
private fun SummaryCard(s: Summary, onClick: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        onClick = onClick,
    ) {
        Column {
            if (s.images.isNotEmpty()) {
                AsyncImage(
                    model = Api.mediaUrl(s.images.first()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        s.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (s.category.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                            Text(
                                categoryLabel(s.category),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        s.username,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(s.likes.fa(), style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(s.commentsCount.fa(), style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${s.images.size.fa()} صفحه",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UploadSummarySheet(
    uploading: Boolean,
    onUpload: (String, String, String, List<Uri>) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) images = uris
    }

    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 30.dp),
        ) {
            item {
                Text("آپلود جزوه 📄", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("عنوان کتاب/خلاصه") }, singleLine = true,
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = desc, onValueChange = { desc = it },
                    label = { Text("توضیحات (اختیاری)") }, minLines = 2, maxLines = 4,
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Text("دسته‌بندی (اختیاری)", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SUMMARY_CATEGORIES.forEach { (value, label) ->
                        FilterChip(
                            selected = category == value,
                            onClick = { category = if (category == value) "" else value },
                            label = { Text(label, fontSize = 11.sp) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { pickImages.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(if (images.isEmpty()) "🖼 انتخاب تصاویر جزوه" else "🖼 ${images.size.fa()} تصویر انتخاب شد")
                }
                Spacer(Modifier.height(14.dp))
                if (uploading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "در حال آپلود تصاویر…",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(
                        onClick = { onUpload(title.trim().ifBlank { "بدون عنوان" }, desc.trim(), category, images) },
                        enabled = images.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("آپلود جزوه ⬆") }
                }
            }
        }
    }
}
