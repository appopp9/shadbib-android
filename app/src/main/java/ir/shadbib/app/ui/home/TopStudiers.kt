package ir.shadbib.app.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.shadbib.app.core.Fmt
import ir.shadbib.app.core.fa
import ir.shadbib.app.ui.components.AppCard
import ir.shadbib.app.ui.components.Avatar
import ir.shadbib.app.ui.components.LoadingBox

private fun medal(rank: Int): String = when (rank) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> ""
}

/**
 * جدول برترین‌های امروز — فشرده (۵ نفر اول) و همیشه شاملِ ردیف خودِ کاربر،
 * حتی اگر رتبه‌اش پایین‌تر از پنج باشد. داده از HomeViewModel می‌آید (بدون فراخوانی تکراری شبکه).
 */
@Composable
fun TopStudiersCard(
    rows: List<Leader>,
    loading: Boolean = false,
    limit: Int = 5,
    onUser: (String) -> Unit,
    onSeeAll: () -> Unit = {},
) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🏆", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("برترین‌های امروز", style = MaterialTheme.typography.titleSmall)
                Text(
                    "بر اساس مجموع مطالعهٔ امروز",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), onClick = onSeeAll) {
                Text(
                    "همه",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading && rows.isEmpty() -> LoadingBox(height = 92.dp)
            rows.isEmpty() -> Text(
                "هنوز کسی امروز مطالعه‌ای ثبت نکرده — اولین نفر تو باش!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                val maxM = rows.maxOf { it.minutes }.coerceAtLeast(1)
                val head = rows.take(limit)
                head.forEach { r -> LeaderRow(r, maxM, onUser) }
                val mine = rows.firstOrNull { it.isMe }
                if (mine != null && head.none { it.isMe }) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    LeaderRow(mine, maxM, onUser)
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(r: Leader, maxMinutes: Int, onUser: (String) -> Unit) {
    val frac by animateFloatAsState(r.minutes.toFloat() / maxMinutes, tween(700), label = "topBar")
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (r.isMe) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent,
        onClick = { onUser(r.username) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                    if (r.rank <= 3) {
                        Text(medal(r.rank), fontSize = 16.sp)
                    } else {
                        Text(
                            r.rank.fa(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Avatar(r.username, size = 32.dp)
                Spacer(Modifier.width(9.dp))
                Text(
                    if (r.isMe) "${r.username} (خودت)" else r.username,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (r.isMe) FontWeight.Bold else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    Fmt.minutes(r.minutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
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
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = if (r.isMe) 1f else 0.65f),
                            CircleShape,
                        ),
                )
            }
        }
    }
}
