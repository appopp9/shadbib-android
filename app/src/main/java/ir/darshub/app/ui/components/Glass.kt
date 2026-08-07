package ir.darshub.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** منوی شیشه‌ای تلگرامی: کارت نیمه‌شفاف با حاشیه نور و ورود فنری. */
@Composable
fun GlassMenu(onDismiss: () -> Unit, content: @Composable ColumnScopeGlass.() -> Unit) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val scale by animateFloatAsState(if (on) 1f else 0.82f, spring(dampingRatio = 0.62f, stiffness = 900f), label = "glassScale")
    val alpha by animateFloatAsState(if (on) 1f else 0f, spring(stiffness = 1200f), label = "glassAlpha")
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f)))),
                shadowElevation = 18.dp,
                modifier = Modifier.widthIn(min = 250.dp, max = 320.dp),
            ) {
                Column(Modifier.padding(vertical = 8.dp)) { ColumnScopeGlass(onDismiss).content() }
            }
        }
    }
}

class ColumnScopeGlass(val dismiss: () -> Unit)

/** آیتم منوی شیشه‌ای. */
@Composable
fun ColumnScopeGlass.GlassAction(icon: ImageVector, label: String, danger: Boolean = false, onClick: () -> Unit) {
    val tint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Surface(color = Color.Transparent, onClick = { onClick(); dismiss() }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}

/** ردیف ری‌اکشن‌های تلگرامی: ایموجی‌های بزرگ در کپسول شیشه‌ای با فشار فنری. */
@Composable
fun ColumnScopeGlass.GlassReactions(mineEmoji: String?, onPick: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            listOf(
                listOf("❤️", "👍", "👎", "😂", "🔥", "🍓"),
                listOf("🍌", "🖕", "😢", "🤯", "💯", "🙏"),
            ).forEach { emojis ->
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically, ) {
            emojis.forEach { em ->
                val src = remember { MutableInteractionSource() }
                val pressed by src.collectIsPressedAsState()
                val sc by animateFloatAsState(if (pressed) 1.45f else if (mineEmoji == em) 1.15f else 1f,
                    spring(dampingRatio = 0.4f, stiffness = 600f), label = "emScale")
                Surface(
                    shape = CircleShape,
                    color = if (mineEmoji == em) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else Color.Transparent,
                    onClick = { onPick(em); dismiss() },
                    interactionSource = src,
                ) {
                    Text(em, fontSize = 23.sp, modifier = Modifier.padding(5.dp).graphicsLayer { scaleX = sc; scaleY = sc })
                }
            }
        }
            }
        }
    }
}

/** جداکننده ظریف منو. */
@Composable
fun ColumnScopeGlass.GlassDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.outlineVariant, Color.Transparent))))
    }
}
