package ir.shadbib.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.core.int
import ir.shadbib.app.core.str
import ir.shadbib.app.ui.components.AppCard
import ir.shadbib.app.ui.components.Avatar

private data class TopStudier(val rank: Int, val username: String, val minutes: Int, val isMe: Boolean)

private fun medal(rank: Int): String = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> ""
}

/** Today's leaderboard, sorted by total study minutes. Tap a row to open that profile. */
@Composable
fun TopStudiersCard(onUser: (String) -> Unit, onSeeAll: () -> Unit = {}) {
    var rows by remember { mutableStateOf<List<TopStudier>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val o = Api.obj(Api.get("study_top", "scope" to "today", "limit" to "10"))
            val arr = o.optJSONArray("top")
            val me = Store.username
            val out = ArrayList<TopStudier>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    val name = r.str("username")
                    out.add(TopStudier(r.int("rank"), name, r.int("minutes"), name == me))
                }
            }
            rows = out
        } catch (e: Exception) {
            error = ir.shadbib.app.core.Api.humanizeError(e)
        }
        loading = false
    }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏆", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("برترین‌های امروز", style = MaterialTheme.typography.titleSmall)
                Text("رتبه‌بندی بر اساس مجموع مطالعه‌ی امروز", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), onClick = onSeeAll) {
                Text("همه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading -> Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            }
            error != null -> Text(error ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            rows.isEmpty() -> Text("هنوز کسی امروز مطالعه‌ای ثبت نکرده — اولین نفر تو باش!",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> {
                val maxM = rows.maxOf { it.minutes }.coerceAtLeast(1)
                rows.forEach { r ->
                    val frac by animateFloatAsState(r.minutes.toFloat() / maxM, tween(700), label = "topBar")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (r.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
                        onClick = { onUser(r.username) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                                    if (r.rank <= 3) Text(medal(r.rank), fontSize = 16.sp)
                                    else Text(r.rank.fa(), style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.width(6.dp))
                                Avatar(r.username, size = 32.dp)
                                Spacer(Modifier.width(9.dp))
                                Text(
                                    if (r.isMe) "${r.username} (خودت)" else r.username,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (r.isMe) FontWeight.Bold else null,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(Fmt.minutes(r.minutes), style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape),
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(frac)
                                        .height(5.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (r.isMe) 1f else 0.65f), CircleShape),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
