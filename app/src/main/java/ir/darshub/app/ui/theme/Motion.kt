package ir.darshub.app.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

// ==================== DarsHub Motion — توکن‌های حرکت ۲۰۲۶ ====================
// یک زبان حرکت واحد: سریع‌تر از پیش‌فرض‌های Material، فنری در ورودها،
// نرم در خروج‌ها. همهٔ انیمیشن‌های اپ از همین توکن‌ها می‌خوانند.

object DarsMotion {
    /** مدت‌زمان‌ها (ms) */
    const val Instant = 80
    const val Fast = 140
    const val Base = 240
    const val Medium = 360
    const val Slow = 520

    /** منحنی‌های حرکت — تأکیدی (Emphasized) نسخهٔ ۲۰۲۶ برای ورود و خروج */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val EmphasizedAccel = CubicBezierEasing(0.3f, 0f, 1f, 1f)
    val Standard = FastOutSlowInEasing

    /** اسپرینگ‌ها — جنریک؛ برای Float، Dp و هر نوع قابل انیمیشن */
    fun <T> springSnappy() = spring<T>(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow * 1.4f)
    fun <T> springBouncy() = spring<T>(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow)
    fun <T> springGentle() = spring<T>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)

    /** استانداردهای tween — جنریک */
    fun <T> inOut(duration: Int = Base) = tween<T>(duration, easing = Emphasized)
    fun <T> fade(duration: Int = Base) = tween<T>(duration, easing = Standard)
}

/**
 * میکرو-اینترکشن فشار: وقتی المان لمس می‌شود به‌آرامی جمع می‌شود و با
 * اسپرینگ برمی‌گردد. باید با همان [MutableInteractionSource] که به
 * clickable داده می‌شود صدا زده شود.
 */
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.965f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = DarsMotion.springSnappy(),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** چرخش فنری برای آیکون‌های کوچک (مثلاً مثلث پخش که به مربع تبدیل می‌شود). */
@Composable
fun rememberRotate(active: Boolean): Float {
    val anim = remember { Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(active) {
        anim.animateTo(if (active) 90f else 0f, spring(dampingRatio = 0.6f, stiffness = 700f))
    }
    return anim.value
}
