package ir.darshub.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import ir.darshub.app.core.Api
import ir.darshub.app.core.fa
import ir.darshub.app.ui.theme.DarsMotion
import ir.darshub.app.ui.theme.brandGradient
import ir.darshub.app.ui.theme.pressScale
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * کارت اصلی اپ — لایه‌دار با گوشهٔ بزرگ، سایهٔ نرم رنگی، افکت فشار فنری و
 * هپتیک. `tonal` حالت برجستهٔ سطح را فعال می‌کند.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val color =
        if (tonal) cs.surfaceContainerHigh.copy(alpha = 0.9f)
        else cs.surface
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && onClick != null) 0.972f else 1f,
        DarsMotion.springSnappy(), label = "cardPress")
    val haptic = LocalHapticFeedback.current
    val elev by androidx.compose.animation.core.animateDpAsState(if (pressed && onClick != null) 2.dp else 8.dp, DarsMotion.springGentle(), label = "cardElev")
    Surface(
        modifier = modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elev, MaterialTheme.shapes.large, ambientColor = cs.primary.copy(alpha = 0.16f), spotColor = cs.primary.copy(alpha = 0.22f)),
        shape = MaterialTheme.shapes.large,
        color = color,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.6f)),
    ) {
        val inner = Modifier
            .let {
                if (onClick != null) it.clickable(interaction, indication = null) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
                } else it
            }
            .padding(17.dp)
        Column(modifier = inner, content = content)
    }
}

/** عنوان بخش: نشانگر قرصی گرادیانی + عنوان + اکشن اختیاری. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(width = 4.dp, height = 18.dp)
                .background(brandGradient(), CircleShape)
        )
        Spacer(Modifier.size(9.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        if (actionText != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionText + " ←", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
        }
    }
}

/** دکمهٔ گرادیانی برند — قرصی، با سایهٔ رنگی، فشار فنری و هپتیک. */
@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.955f else 1f,
        DarsMotion.springSnappy(), label = "btnPress")
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .alpha(if (enabled) 1f else 0.45f)
            .shadow(if (enabled) 12.dp else 0.dp, CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            .clip(CircleShape)
            .background(brandGradient())
            .clickable(interaction, indication = null, enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onClick()
            }
            .padding(horizontal = 24.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** تب‌های قرصی (امروز / آینده / تاریخچه و ...) با نشانگر لغزان فنری. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val cs = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = cs.surfaceContainerLow,
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.5f)),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val itemW = maxWidth / options.size
            val idx = if (rtl) options.size - 1 - selected else selected
            val pillX by androidx.compose.animation.core.animateDpAsState(
                itemW * idx, DarsMotion.springSnappy(), label = "tabPill")
            val pillW by androidx.compose.animation.core.animateDpAsState(
                itemW - 8.dp, DarsMotion.springSnappy(), label = "tabPillW")
            Box(
                Modifier
                    .padding(4.dp)
                    .offset(x = pillX)
                    .width(pillW)
                    .height(38.dp)
                    .clip(CircleShape)
                    .background(brandGradient()),
            )
            Row(Modifier.fillMaxWidth()) {
                options.forEachIndexed { i, label ->
                    val sel = i == selected
                    val fg by animateColorAsState(
                        if (sel) cs.onPrimary else cs.onSurfaceVariant,
                        tween(DarsMotion.Fast), label = "tabFg")
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(CircleShape)
                            .clickable(remember { MutableInteractionSource() }, indication = null) {
                                if (!sel) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onSelect(i) }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1) }
                }
            }
        }
    }
}

/** اسکلتون شیمری برای لیست‌ها — با گوشه‌های نرم‌تر و فاصله‌گذاری بهتر. */
@Composable
fun LoadingBox(modifier: Modifier = Modifier, height: Dp = 120.dp) {
    val lines = (height.value / 48).toInt().coerceIn(1, 6)
    Column(modifier.fillMaxWidth().height(height), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        repeat(lines) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(shimmerBrush()))
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(if (i % 2 == 0) 0.62f else 0.45f).height(12.dp)
                        .clip(RoundedCornerShape(7.dp)).background(shimmerBrush()))
                    Box(Modifier.fillMaxWidth(if (i % 2 == 0) 0.36f else 0.55f).height(10.dp)
                        .clip(RoundedCornerShape(7.dp)).background(shimmerBrush()))
                }
            }
        }
    }
}

/** براش شیمر متحرک برای اسکلتون‌ها. */
@Composable
fun shimmerBrush(): Brush {
    val t = rememberInfiniteTransition(label = "shimmer")
    val x by t.animateFloat(0f, 1300f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "shimmerX")
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    return Brush.linearGradient(
        colors = listOf(base.copy(alpha = 0.55f), base.copy(alpha = 1f), base.copy(alpha = 0.55f)),
        start = Offset(x - 340f, 0f),
        end = Offset(x, 230f),
    )
}

/** لودر برند: کتاب تپنده داخل حلقهٔ گرادیانی چرخان + برچسب اختیاری. */
@Composable
fun FullLoading(label: String? = null) {
    val t = rememberInfiniteTransition(label = "fullload")
    val scale by t.animateFloat(0.9f, 1.08f,
        infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val rot by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "rot")
    val p = MaterialTheme.colorScheme.primary
    val s = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(88.dp).graphicsLayer { rotationZ = rot }) {
                    drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(width = 11f, cap = StrokeCap.Round))
                    drawArc(
                        brush = Brush.sweepGradient(listOf(p.copy(alpha = 0f), s, p)),
                        startAngle = 20f, sweepAngle = 280f, useCenter = false,
                        style = Stroke(width = 11f, cap = StrokeCap.Round))
                }
                Text("📚", fontSize = 32.sp, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
            }
            if (label != null) Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** ورود پلکانی و نرم برای بخش‌های صفحه (fade + slide + scale، منحنی پریمیوم). */
@Composable
fun FadeSlideIn(index: Int = 0, content: @Composable () -> Unit) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(45L * index.coerceAtMost(10)); on = true }
    AnimatedVisibility(
        visible = on,
        enter = fadeIn(tween(380, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 9 } +
                scaleIn(tween(380, easing = FastOutSlowInEasing), initialScale = 0.975f),
    ) { content() }
}

/** عدد شمارنده انیمیشنی — برای آمار و دقیقه‌ها. */
@Composable
fun CountUpText(target: Int, style: androidx.compose.ui.text.TextStyle, color: Color = Color.Unspecified, suffix: String = "") {
    val v by animateIntAsState(target, tween(700, easing = FastOutSlowInEasing), label = "count")
    Text(v.fa() + suffix, style = style, color = color)
}

/** حالت خطای زیبا — ابر قطع اتصال با دکمه تلاش دوباره. هیچ جزئیات فنی نمایش نمی‌دهد. */
@Composable
fun ErrorState(message: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null) {
    val t = rememberInfiniteTransition(label = "err")
    val fl by t.animateFloat(-6f, 6f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "float")
    Column(modifier.fillMaxWidth().padding(vertical = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(96.dp)
                .graphicsLayer { translationY = fl }
                .shadow(16.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("📡", fontSize = 40.sp) }
        Spacer(Modifier.height(14.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 32.dp))
        if (onRetry != null) {
            Spacer(Modifier.height(14.dp))
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, onClick = onRetry) {
                Text("تلاش دوباره 🔄", color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp))
            }
        }
    }
}

/** حالت خالی — ایموجی در حباب گرادیانی محو + متن. */
@Composable
fun EmptyState(emoji: String, text: String, modifier: Modifier = Modifier) {
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val a by animateFloatAsState(if (on) 1f else 0f, tween(500), label = "empty")
    Column(
        modifier.fillMaxWidth().padding(vertical = 30.dp).graphicsLayer { alpha = a; scaleX = 0.94f + 0.06f * a; scaleY = 0.94f + 0.06f * a },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            Modifier.size(78.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 34.sp) }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private val avatarPalette = listOf(
    Color(0xFF34E0A1), Color(0xFF38BDF8), Color(0xFFA78BFA), Color(0xFFFB7185),
    Color(0xFFFBBF24), Color(0xFF2DD4BF), Color(0xFFF472B6), Color(0xFF818CF8),
)

fun userColor(name: String): Color = avatarPalette[abs(name.hashCode()) % avatarPalette.size]

/** آواتار دایره‌ای با حرف اول + بج مود + رینگ گرادیانی هنگام آنلاین بودن. */
@Composable
fun Avatar(username: String, mood: String? = null, size: Dp = 44.dp, online: Boolean? = null, avatarUrl: String? = null) {
    val c = userColor(username)
    val ring = if (online == true)
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
    else
        Brush.linearGradient(listOf(c.copy(alpha = 0.6f), c.copy(alpha = 0.6f)))
    Box(Modifier.size(size + 6.dp)) {
        val url = Api.mediaUrl(avatarUrl)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = username,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .border(1.5.dp, ring, CircleShape),
            )
        } else {
            Box(
                Modifier
                    .size(size)
                    .align(Alignment.Center)
                    .background(c.copy(alpha = 0.16f), CircleShape)
                    .border(1.5.dp, ring, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    username.take(1).uppercase(),
                    color = c,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        if (!mood.isNullOrBlank()) {
            Text(
                mood,
                fontSize = (size.value * 0.34f).sp,
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
        if (online != null) {
            val dotScale by animateFloatAsState(if (online) 1f else 0.8f, DarsMotion.springBouncy(), label = "dot")
            Box(
                Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .graphicsLayer { scaleX = dotScale; scaleY = dotScale }
                    .background(
                        if (online) Color(0xFF22C55E) else MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }
}

/** پیل آماری: مقدار درشت + برچسب، با رنگ اکسنت، حاشیهٔ نرم و سایهٔ محو. */
@Composable
fun StatPill(value: String, label: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** نوار پیشرفت گرادیانی با انیمیشن پرشدن — برای تفکیک دروس. */
@Composable
fun ProgressRow(icon: String, name: String, minutes: Int, maxMinutes: Int, valueText: String, color: Color) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.size(6.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(valueText, style = MaterialTheme.typography.labelMedium, color = color)
        }
        Spacer(Modifier.size(7.dp))
        val target = if (maxMinutes <= 0) 0f else (minutes.toFloat() / maxMinutes).coerceIn(0.02f, 1f)
        val frac by animateFloatAsState(target, tween(750, easing = FastOutSlowInEasing), label = "progress")
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), CircleShape)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac)
                    .height(8.dp)
                    .background(Brush.horizontalGradient(listOf(color, lerp(color, Color.White, 0.3f))), CircleShape)
            )
        }
    }
}
