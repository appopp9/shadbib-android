@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package ir.shadbib.app.ui.tasks

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.fa
import ir.shadbib.app.data.TaskItem
import ir.shadbib.app.ui.components.EmptyState
import ir.shadbib.app.ui.components.ErrorState
import ir.shadbib.app.ui.components.FadeSlideIn
import ir.shadbib.app.ui.components.LoadingBox
import ir.shadbib.app.ui.components.SegmentedTabs
import ir.shadbib.app.ui.theme.brandGradient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

class TasksViewModel : ViewModel() {
    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val today: List<TaskItem> = emptyList(),
        val upcoming: List<TaskItem> = emptyList(),
        val history: List<TaskItem> = emptyList(),
    )

    val state = MutableStateFlow(State())

    /** ids of rows with an in-flight request */
    val busy = MutableStateFlow<Set<Int>>(emptySet())
    val adding = MutableStateFlow(false)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            state.update { it.copy(error = null) }
            try {
                val today = TaskItem.list(Api.arr(Api.get("tasks")))
                val upcoming = TaskItem.list(Api.arr(Api.get("tasks_upcoming")))
                val history = TaskItem.list(Api.arr(Api.get("tasks_history")))
                state.update { it.copy(loading = false, today = today, upcoming = upcoming, history = history) }
            } catch (e: Exception) {
                state.update { it.copy(loading = false, error = e.message ?: "خطا") }
            }
        }
    }

    fun add(title: String, date: String, priority: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            adding.value = true
            try {
                Api.post("tasks", JSONObject().put("title", title).put("task_date", date).put("priority", priority))
                refreshNow()
                onResult(null)
                ir.shadbib.app.core.RefreshBus.emit("home")
            } catch (e: Exception) {
                onResult(e.message ?: "خطا")
            } finally {
                adding.value = false
            }
        }
    }

    fun toggle(task: TaskItem) {
        viewModelScope.launch {
            busy.update { it + task.id }
            // optimistic flip so the checkbox reacts instantly
            state.update { s ->
                fun flip(l: List<TaskItem>) = l.map { if (it.id == task.id) it.copy(done = !it.done) else it }
                s.copy(today = flip(s.today), upcoming = flip(s.upcoming), history = flip(s.history))
            }
            runCatching {
                Api.put("tasks", JSONObject().put("done", if (task.done) 0 else 1), "id" to task.id.toString())
            }
            refreshNow()
            busy.update { it - task.id }
            ir.shadbib.app.core.RefreshBus.emit("home")
        }
    }

    fun delete(task: TaskItem) {
        viewModelScope.launch {
            busy.update { it + task.id }
            runCatching { Api.delete("tasks", "id" to task.id.toString()) }
            refreshNow()
            busy.update { it - task.id }
            ir.shadbib.app.core.RefreshBus.emit("home")
        }
    }

    private suspend fun refreshNow() {
        try {
            val today = TaskItem.list(Api.arr(Api.get("tasks")))
            val upcoming = TaskItem.list(Api.arr(Api.get("tasks_upcoming")))
            val history = TaskItem.list(Api.arr(Api.get("tasks_history")))
            state.update { it.copy(loading = false, error = null, today = today, upcoming = upcoming, history = history) }
        } catch (e: Exception) {
            state.update { it.copy(loading = false) }
        }
    }
}

@Composable
fun TasksScreen(vm: TasksViewModel = viewModel()) {
    val ctx = LocalContext.current
    val state by vm.state.collectAsState()
    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(Unit) { ir.shadbib.app.core.RefreshBus.events.collect { if (it == "tasks" || it == "all") vm.refresh() } }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    val busy by vm.busy.collectAsState()
    val adding by vm.adding.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                shadowElevation = 12.dp,
                modifier = Modifier.size(60.dp),
                onClick = { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); showAdd = true },
            ) {
                Box(Modifier.background(brandGradient()), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Add, "تسک جدید", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                }
            }
        },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            val doneCount = state.today.count { it.done }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("تسک‌ها", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        if (state.today.isEmpty()) "برنامهٔ امروزت رو بچین" else "${doneCount.fa()} از ${state.today.size.fa()} تسک امروز انجام شد",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.today.isNotEmpty()) {
                    val frac by androidx.compose.animation.core.animateFloatAsState(
                        doneCount.toFloat() / state.today.size,
                        androidx.compose.animation.core.tween(700), label = "taskRing")
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp)) {
                        val ringColor = MaterialTheme.colorScheme.primary
                        val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                            val stroke = 5.dp.toPx()
                            drawArc(track, -90f, 360f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                            drawArc(ringColor, -90f, 360f * frac, false, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                        }
                        Text("${(frac * 100).toInt().fa()}٪", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            SegmentedTabs(
                options = listOf("امروز", "آینده", "تاریخچه"),
                selected = tab,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) { tab = it }
            Spacer(Modifier.height(4.dp))
            val list = when (tab) {
                0 -> state.today
                1 -> state.upcoming
                else -> state.history
            }
            when {
                state.loading -> LoadingBox(height = 200.dp)
                state.error != null -> ErrorState(state.error ?: "", onRetry = { vm.refresh() })
                list.isEmpty() -> EmptyState(
                    if (tab == 0) "🌤" else if (tab == 1) "🔮" else "📜",
                    if (tab == 0) "امروز تسکی نداری — یکی بساز!" else if (tab == 1) "تسک آینده‌ای ثبت نشده" else "تاریخچه‌ای موجود نیست",
                )
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(list, key = { _, t -> t.id }) { i, task ->
                        Box(Modifier.animateItemPlacement(androidx.compose.animation.core.tween(320))) {
                            FadeSlideIn(i) {
                                TaskRow(
                                    task = task,
                                    showDate = tab != 0,
                                    busy = busy.contains(task.id),
                                    onToggle = { vm.toggle(task) },
                                    onDelete = { vm.delete(task) },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAdd) {
        AddTaskSheet(
            adding = adding,
            onAdd = { title, date, priority ->
                vm.add(title, date, priority) { err ->
                    Toast.makeText(ctx, err ?: "تسک اضافه شد ✅", Toast.LENGTH_SHORT).show()
                    if (err == null) showAdd = false
                }
            },
            onDismiss = { if (!adding) showAdd = false },
        )
    }
}

@Composable
private fun TaskRow(task: TaskItem, showDate: Boolean, busy: Boolean = false, onToggle: () -> Unit, onDelete: () -> Unit) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (task.done) MaterialTheme.colorScheme.primary.copy(alpha = 0.07f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (task.done) MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val checkShape = RoundedCornerShape(9.dp)
            val checkScale by androidx.compose.animation.core.animateFloatAsState(
                if (task.done) 1f else 0f,
                androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 700f), label = "check")
            Box(
                Modifier
                    .size(27.dp)
                    .background(if (task.done) MaterialTheme.colorScheme.primary else Color.Transparent, checkShape)
                    .border(
                        2.dp,
                        when {
                            task.done -> MaterialTheme.colorScheme.primary
                            task.priority == "high" -> MaterialTheme.colorScheme.error.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.outline
                        },
                        checkShape,
                    )
                    .clickable(enabled = !busy) { haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove); onToggle() },
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = if (task.done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                    )
                } else if (task.done) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(17.dp).scale(checkScale),
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (task.priority) {
                        "high" -> PriorityPill("فوری", MaterialTheme.colorScheme.error)
                        "low" -> PriorityPill("کم", MaterialTheme.colorScheme.primary)
                    }
                    if (showDate && task.date.isNotBlank()) {
                        Text(
                            Fmt.jalaliWithDay(task.date),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            IconButton(onClick = onDelete, enabled = !busy) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "حذف",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = if (busy) 0.3f else 0.8f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun PriorityPill(text: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.11f)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp))
    }
}

@Composable
private fun AddTaskSheet(adding: Boolean, onAdd: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("normal") }
    val days = remember { Fmt.upcomingDays(21) }
    var dateIso by remember { mutableStateOf(days.first().first) }

    ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("تسک جدید", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان تسک") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("اولویت", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = priority == "normal", onClick = { priority = "normal" }, label = { Text("معمولی") })
                FilterChip(selected = priority == "high", onClick = { priority = "high" }, label = { Text("🔴 مهم") })
                FilterChip(selected = priority == "low", onClick = { priority = "low" }, label = { Text("🟢 کم") })
            }
            Spacer(Modifier.height(12.dp))
            Text("تاریخ", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { (iso, label) ->
                    FilterChip(selected = dateIso == iso, onClick = { dateIso = iso }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onAdd(title.trim(), dateIso, priority) },
                enabled = title.isNotBlank() && !adding,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (adding) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("دارم اضافه می‌کنم…", fontSize = 15.sp)
                } else {
                    Text("افزودن تسک ➕", fontSize = 15.sp)
                }
            }
        }
    }
}
