package ir.shadbib.app.ui.study.room

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.fa
import ir.shadbib.app.core.int
import ir.shadbib.app.core.objects
import ir.shadbib.app.core.str
import ir.shadbib.app.core.strOrNull
import ir.shadbib.app.data.Course
import org.json.JSONObject

private val Ink = Color(0xFF2B1E16)
private val Cream = Color(0xFFF7F3E8)
private val Mint = Color(0xFF34D399)
private val Coral = Color(0xFFFF8A65)
private val Sand = Color(0xFFE8D5A3)

/** Shared neobrutalist overlay card used by every room sheet. */
@Composable
fun RoomSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickable { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Cream,
                border = BorderStroke(3.dp, Ink),
                modifier = Modifier.fillMaxWidth(0.92f).heightIn(max = 560.dp)
                    .pointerInput(Unit) { detectTapGestures { } },
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(Sand).clickable { onDismiss() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Close, "\u0628\u0633\u062a\u0646", tint = Ink, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

// ==================== \u0641\u0647\u0631\u0633\u062a \u06a9\u0627\u0645\u0644 \u062d\u0627\u0636\u0631\u0627\u0646 \u0627\u062a\u0627\u0642 ====================

/** Full room roster. The room only draws a few seats, so this is the real list. */
@Composable
fun OccupantSheet(onUser: (String) -> Unit, onDismiss: () -> Unit) {
    var users by remember { mutableStateOf<List<RoomOccupant>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            val o = Api.obj(Api.get("room_users", "room_id" to "1"))
            o.optJSONArray("users")?.objects()?.map { RoomOccupant.from(it) } ?: emptyList()
        }.onSuccess { users = it; loading = false }
            .onFailure { error = it.message ?: "\u062e\u0637\u0627"; loading = false }
    }

    RoomSheet("\u062d\u0627\u0636\u0631\u0627\u0646 \u0627\u062a\u0627\u0642 " + "(" + users.size.fa() + ")", onDismiss) {
        when {
            loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
            error != null -> Text(error!!, color = Coral, fontSize = 13.sp)
            users.isEmpty() -> Text("\u0641\u0639\u0644\u0627\u064b \u06a9\u0633\u06cc \u062f\u0631 \u0627\u062a\u0627\u0642 \u0646\u06cc\u0633\u062a", color = Ink, fontSize = 13.sp)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(users, key = { it.userId }) { u ->
                    PersonRow(
                        character = u.character,
                        name = u.username,
                        subtitle = stateFa(u.state),
                        minutes = u.minutesToday,
                        highlight = false,
                    ) { onUser(u.username) }
                }
            }
        }
    }
}

private fun stateFa(state: String): String = when (state) {
    RoomState.STUDYING -> "\u062f\u0631 \u062d\u0627\u0644 \u0645\u0637\u0627\u0644\u0639\u0647"
    RoomState.SLEEPING -> "\u062e\u0648\u0627\u0628\u06cc\u062f\u0647"
    else -> "\u0628\u06cc\u200c\u06a9\u0627\u0631"
}

// ==================== \u062c\u062f\u0648\u0644 \u0631\u062a\u0628\u0647\u200c\u0628\u0646\u062f\u06cc \u0645\u0637\u0627\u0644\u0639\u0647\u0654 \u06a9\u0644 ====================

private data class TopRow(
    val rank: Int,
    val username: String,
    val character: String,
    val minutes: Int,
    val isMe: Boolean,
)

/** Leaderboard on TOTAL study time (not room-only minutes). */
@Composable
fun LeaderboardSheet(onUser: (String) -> Unit, onDismiss: () -> Unit) {
    val scopes = listOf(
        "today" to "\u0627\u0645\u0631\u0648\u0632",
        "week" to "\u0627\u06cc\u0646 \u0647\u0641\u062a\u0647",
        "month" to "\u0645\u0627\u0647",
        "all" to "\u06a9\u0644",
    )
    var scope by remember { mutableStateOf("today") }
    var rows by remember { mutableStateOf<List<TopRow>>(emptyList()) }
    var myRank by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(scope) {
        loading = true
        runCatching {
            val o = Api.obj(Api.get("study_top", "scope" to scope, "limit" to "100"))
            val list = o.optJSONArray("top")?.objects()?.map { r ->
                TopRow(
                    rank = r.int("rank"),
                    username = r.strOrNull("username") ?: "?",
                    character = r.strOrNull("character") ?: "cat",
                    minutes = r.int("minutes"),
                    isMe = r.optInt("is_me", 0) == 1,
                )
            } ?: emptyList()
            list to o.optInt("my_rank", 0)
        }.onSuccess { rows = it.first; myRank = it.second; loading = false }
            .onFailure { loading = false }
    }

    RoomSheet("\u0628\u06cc\u0634\u062a\u0631\u06cc\u0646 \u0645\u0637\u0627\u0644\u0639\u0647", onDismiss) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            scopes.forEach { (k, fa) ->
                val on = k == scope
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = if (on) Mint else Cream,
                    border = BorderStroke(2.dp, Ink),
                    modifier = Modifier.clickable { scope = k },
                ) {
                    Text(fa, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (myRank > 0) {
            Text("\u0631\u062a\u0628\u0647\u0654 \u062a\u0648: " + myRank.fa(), color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp))
        }
        if (loading) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
        } else if (rows.isEmpty()) {
            Text("\u0647\u0646\u0648\u0632 \u0645\u0637\u0627\u0644\u0639\u0647\u200c\u0627\u06cc \u062b\u0628\u062a \u0646\u0634\u062f\u0647", color = Ink, fontSize = 13.sp)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows, key = { it.rank }) { r ->
                    PersonRow(
                        character = r.character,
                        name = medal(r.rank) + " " + r.username,
                        subtitle = "\u0631\u062a\u0628\u0647\u0654 " + r.rank.fa(),
                        minutes = r.minutes,
                        highlight = r.isMe,
                    ) { onUser(r.username) }
                }
            }
        }
    }
}

private fun medal(rank: Int): String = when (rank) {
    1 -> "\uD83E\uDD47"
    2 -> "\uD83E\uDD48"
    3 -> "\uD83E\uDD49"
    else -> ""
}

// ==================== \u0627\u0646\u062a\u062e\u0627\u0628 \u062f\u0631\u0633 \u067e\u06cc\u0634 \u0627\u0632 \u062b\u0628\u062a ====================

/** Asks which course the minutes belong to BEFORE logging them. */
/**
 * Daily goal picker.
 *
 * The ring at the top of the room used to change the goal on every tap with no
 * label and no confirmation, so the number under it looked random. Tapping now
 * opens this sheet, which says what the number means and lets the goal be
 * chosen deliberately.
 */
@Composable
fun GoalSheet(current: Int, done: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    RoomSheet("هدف روزانهٔ مطالعه", onDismiss) {
        Text(
            "امروز " + done.fa() + " دقیقه خوانده‌ای.",
            color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "حلقهٔ بالای صفحه درصد رسیدن به همین هدف را نشان می‌دهد.",
            color = Ink, fontSize = 11.sp,
        )
        Spacer(Modifier.height(14.dp))
        RoomPrefs.goalChoices.forEach { g ->
            val selected = g == current
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) Mint else Color.White,
                border = BorderStroke(2.dp, Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { onPick(g) },
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        g.fa() + " دقیقه",
                        color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    if (selected) Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun CoursePickerSheet(minutes: Int, onPick: (Int?) -> Unit, onDismiss: () -> Unit) {
    var courses by remember { mutableStateOf<List<Course>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching { Course.list(Api.arr(Api.get("courses"))) }
            .onSuccess { courses = it; loading = false }
            .onFailure { loading = false }
    }

    RoomSheet("\u0627\u06cc\u0646 " + minutes.fa() + " \u062f\u0642\u06cc\u0642\u0647 \u0628\u0631\u0627\u06cc \u06a9\u062f\u0627\u0645 \u062f\u0631\u0633 \u0628\u0648\u062f\u061f", onDismiss) {
        if (loading) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(courses, key = { it.id }) { c ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, Ink),
                        modifier = Modifier.fillMaxWidth().clickable { onPick(c.id) },
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(c.icon, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(c.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Sand,
                        border = BorderStroke(2.dp, Ink),
                        modifier = Modifier.fillMaxWidth().clickable { onPick(null) },
                    ) {
                        Text("\u0628\u062f\u0648\u0646 \u062f\u0631\u0633 \u062b\u0628\u062a \u06a9\u0646", color = Ink, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(11.dp))
                    }
                }
            }
        }
    }
}

// ==================== \u0641\u0631\u0648\u0634\u06af\u0627\u0647 \u0648\u0633\u0627\u06cc\u0644 ====================

/** Shop grouped by family (clocks / plants / mugs / lamps) with model switching. */
@Composable
fun ShopSheet(totalMinutes: Int, onDismiss: () -> Unit, onToast: (String) -> Unit) {
    val owned by RoomPrefs.owned.collectAsState()
    val active by RoomPrefs.active.collectAsState()
    val spent by RoomPrefs.spent.collectAsState()
    val coins = (totalMinutes - spent).coerceAtLeast(0)

    RoomSheet("\u0641\u0631\u0648\u0634\u06af\u0627\u0647 \u2014 " + coins.fa() + " \u0633\u06a9\u0647", onDismiss) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RoomPrefs.groups.forEach { g ->
                item(key = "h_" + g.key) {
                    Text(g.fa, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp))
                }
                items(RoomPrefs.itemsOf(g.key), key = { it.key }) { it2 ->
                    val isOwned = owned.contains(it2.key)
                    val isActive = active[g.key] == it2.key ||
                        (active[g.key] == null && RoomPrefs.activeOf(g.key) == it2.key)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isActive) Mint.copy(alpha = 0.35f) else Color.White,
                        border = BorderStroke(2.dp, Ink),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isOwned) {
                                RoomPrefs.setActive(g.key, it2.key)
                                onToast(it2.fa + " \u0631\u0648\u06cc \u0645\u06cc\u0632 \u06af\u0630\u0627\u0634\u062a\u0647 \u0634\u062f")
                            } else if (RoomPrefs.buy(it2, totalMinutes)) {
                                onToast(it2.fa + " \u062e\u0631\u06cc\u062f\u0647 \u0634\u062f \u2705")
                            } else {
                                onToast("\u0633\u06a9\u0647 \u06a9\u0627\u0641\u06cc \u0646\u062f\u0627\u0631\u06cc")
                            }
                        },
                    ) {
                        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(it2.fa, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isOwned) (if (isActive) "\u0631\u0648\u06cc \u0645\u06cc\u0632" else "\u062f\u0631 \u0627\u0646\u0628\u0627\u0631 \u2014 \u0628\u0632\u0646 \u062a\u0627 \u0628\u06af\u0630\u0627\u0631\u06cc")
                                    else it2.cost.fa() + " \u0633\u06a9\u0647",
                                    color = Ink.copy(alpha = 0.7f), fontSize = 11.sp,
                                )
                            }
                            if (isActive) Icon(Icons.Rounded.Check, null, tint = Ink, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== \u0631\u062f\u06cc\u0641 \u0645\u0634\u062a\u0631\u06a9 \u06a9\u0627\u0631\u0628\u0631 ====================

@Composable
private fun PersonRow(
    character: String,
    name: String,
    subtitle: String,
    minutes: Int,
    highlight: Boolean,
    onClick: () -> Unit,
) {
    val ch = RoomChars.of(character)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) Mint.copy(alpha = 0.35f) else Color.White,
        border = BorderStroke(2.dp, Ink),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(Sand),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(ch.idle),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(34.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = Ink.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1)
            }
            Text(minutes.fa() + " \u062f", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
