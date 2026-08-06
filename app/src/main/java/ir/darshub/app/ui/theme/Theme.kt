package ir.darshub.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.darshub.app.R

// ==================== DarsHub 2026 — «Aurora Study» ====================
// هویت جدید: شبِ عمیقِ بنفش-آبی با شفق‌های گرادیانی در تم تیره،
// و کاغذِ گرمِ عاجی با رگه‌های شفق در تم روشن. سطوح چندلایه، شیشه و عمق نرم.

// ---------- رنگ‌های برند (ثابت، برای سازگاری) ----------
val Mint = Color(0xFF34E0A1)
val MintDeep = Color(0xFF10B981)
val MintDark = Color(0xFF059669)
val Cyan = Color(0xFF38BDF8)
val Amber = Color(0xFFFBBF24)
val Rose = Color(0xFFFB7185)

val CoursePurple = Color(0xFFA78BFA)
val CourseRed = Color(0xFFF87171)
val CourseGreen = Color(0xFF4ADE9F)
val CourseOrange = Color(0xFFFB923C)

fun courseColor(key: String): Color = when (key) {
    "purple" -> CoursePurple
    "red" -> CourseRed
    "green" -> CourseGreen
    "orange" -> CourseOrange
    else -> CoursePurple
}

// ==================== پالت‌ها ====================
/** هر پالت با ۴ رنگ اصلی تعریف می‌شود؛ بقیه رنگ‌ها به‌صورت خودکار مشتق می‌شوند. */
data class AppPalette(
    val id: String,
    val fa: String,
    val emoji: String,
    val pDark: Color,
    val pLight: Color,
    val sDark: Color,
    val sLight: Color,
    val tDark: Color = Amber,
    val tLight: Color = Color(0xFFB45309),
)

val AppPalettes = listOf(
    AppPalette("mint", "نعنایی", "🌿", Color(0xFF34E0A1), Color(0xFF0E9F6E), Cyan, Color(0xFF0284C7)),
    AppPalette("ocean", "اقیانوس", "🌊", Color(0xFF4CC3F7), Color(0xFF0369A1), Color(0xFF34E0A1), Color(0xFF0E9F6E)),
    AppPalette("grape", "انگوری", "🍇", Color(0xFFB49BFF), Color(0xFF7C3AED), Color(0xFFF472B6), Color(0xFFDB2777)),
    AppPalette("sunset", "غروب", "🌅", Color(0xFFFFA05C), Color(0xFFEA580C), Color(0xFFFFD24D), Color(0xFFB45309), tDark = Color(0xFFF472B6), tLight = Color(0xFFDB2777)),
    AppPalette("rose", "شکوفه", "🌸", Color(0xFFFF7D92), Color(0xFFE11D48), Color(0xFFB49BFF), Color(0xFF7C3AED)),
    AppPalette("gold", "طلایی", "✨", Color(0xFFFFD24D), Color(0xFFB45309), Color(0xFF4CC3F7), Color(0xFF0369A1), tDark = Mint, tLight = MintDark),
)

fun paletteOf(id: String): AppPalette = AppPalettes.firstOrNull { it.id == id } ?: AppPalettes.first()

// ---------- سطوح مشتق‌شده از تم ----------
private val LightPaper = Color(0xFFF6F6F1)
private val LightInk = Color(0xFF191C21)
private val DarkMidnight = Color(0xFF070A0F)
private val DarkCloud = Color(0xFFE9EDF2)

/** تم تیرهٔ «نیمه‌شب شفق» — سرمه‌ای عمیق با سطوح چندلایهٔ شیشه‌ای. */
fun darkSchemeOf(p: AppPalette): ColorScheme {
    val bg = lerp(DarkMidnight, p.pDark, 0.045f)
    val surf = lerp(Color(0xFF0E1218), p.pDark, 0.035f)
    val v1 = lerp(Color(0xFF10151C), p.pDark, 0.05f)
    val v2 = lerp(Color(0xFF141A23), p.pDark, 0.06f)
    val v3 = lerp(Color(0xFF1A212C), p.pDark, 0.07f)
    val v4 = lerp(Color(0xFF212A37), p.pDark, 0.08f)
    return darkColorScheme(
        primary = lerp(p.pDark, Color.White, 0.06f),
        onPrimary = lerp(Color(0xFF050A08), p.pDark, 0.12f),
        primaryContainer = lerp(surf, p.pDark, 0.26f),
        onPrimaryContainer = lerp(Color.White, p.pDark, 0.34f),
        secondary = lerp(p.sDark, Color.White, 0.05f),
        onSecondary = lerp(Color(0xFF050A08), p.sDark, 0.12f),
        secondaryContainer = lerp(surf, p.sDark, 0.24f),
        onSecondaryContainer = lerp(Color.White, p.sDark, 0.34f),
        tertiary = lerp(p.tDark, Color.White, 0.05f),
        onTertiary = lerp(Color(0xFF050A08), p.tDark, 0.12f),
        tertiaryContainer = lerp(surf, p.tDark, 0.24f),
        onTertiaryContainer = lerp(Color.White, p.tDark, 0.34f),
        background = bg,
        onBackground = lerp(DarkCloud, p.pDark, 0.05f),
        surface = surf,
        onSurface = lerp(DarkCloud, p.pDark, 0.05f),
        surfaceTint = p.pDark,
        surfaceContainerLowest = lerp(Color(0xFF05070B), p.pDark, 0.03f),
        surfaceContainerLow = v1,
        surfaceContainer = v2,
        surfaceContainerHigh = v3,
        surfaceContainerHighest = v4,
        surfaceVariant = lerp(Color(0xFF1B232E), p.pDark, 0.09f),
        onSurfaceVariant = lerp(Color(0xFFA6B0BC), p.pDark, 0.10f),
        outline = lerp(Color(0xFF3C4754), p.pDark, 0.15f),
        outlineVariant = lerp(Color(0xFF242C37), p.pDark, 0.12f),
        error = Color(0xFFFF7A93),
        onError = Color(0xFF3B0713),
        inverseSurface = lerp(DarkCloud, p.pDark, 0.05f),
        inverseOnSurface = Color(0xFF11151B),
        inversePrimary = lerp(p.pLight, Color.Black, 0.12f),
        scrim = Color(0xFF000000),
    )
}

/** تم روشنِ «کاغذ شفق» — عاجی گرم با سطوح کاغذی و رگه‌های رنگی ظریف. */
fun lightSchemeOf(p: AppPalette): ColorScheme {
    return lightColorScheme(
        primary = p.pLight,
        onPrimary = Color.White,
        primaryContainer = lerp(Color.White, p.pLight, 0.13f),
        onPrimaryContainer = lerp(p.pLight, Color.Black, 0.42f),
        secondary = p.sLight,
        onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, p.sLight, 0.12f),
        onSecondaryContainer = lerp(p.sLight, Color.Black, 0.42f),
        tertiary = p.tLight,
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color.White, p.tLight, 0.14f),
        onTertiaryContainer = lerp(p.tLight, Color.Black, 0.42f),
        background = lerp(LightPaper, p.pLight, 0.025f),
        onBackground = LightInk,
        surface = Color.White,
        onSurface = LightInk,
        surfaceTint = p.pLight,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = lerp(Color(0xFFF2F2EC), p.pLight, 0.02f),
        surfaceContainer = lerp(Color(0xFFECECE5), p.pLight, 0.03f),
        surfaceContainerHigh = lerp(Color(0xFFE5E5DD), p.pLight, 0.04f),
        surfaceContainerHighest = lerp(Color(0xFFDFDFD6), p.pLight, 0.05f),
        surfaceVariant = lerp(Color(0xFFECECE5), p.pLight, 0.05f),
        onSurfaceVariant = lerp(Color(0xFF4B5056), p.pLight, 0.10f),
        outline = lerp(Color(0xFFC4C7C2), p.pLight, 0.14f),
        outlineVariant = lerp(Color(0xFFE3E4DD), p.pLight, 0.10f),
        error = Color(0xFFE11D48),
        onError = Color.White,
        inverseSurface = lerp(LightInk, p.pLight, 0.06f),
        inverseOnSurface = Color(0xFFF2F2ED),
        inversePrimary = lerp(p.pLight, Color.White, 0.18f),
        scrim = Color(0xFF000000),
    )
}

/** گرادیان برند مطابق با تم انتخابی کاربر. */
fun paletteGradient(p: AppPalette, dark: Boolean): Brush {
    val a = if (dark) p.pDark else p.pLight
    val b = if (dark) p.sDark else p.sLight
    return Brush.linearGradient(listOf(lerp(a, Color.Black, 0.18f), a, b))
}

/** گرادیان برند داخل کامپوز — از تم فعال می‌خواند. */
@Composable
fun brandGradient(): Brush {
    val cs = MaterialTheme.colorScheme
    return remember(cs.primary, cs.secondary) {
        Brush.linearGradient(listOf(lerp(cs.primary, Color.Black, 0.18f), cs.primary, cs.secondary))
    }
}

/** برای سازگاری با کدهای قدیمی (گرادیان پیش‌فرض نعنایی). */
val BrandGradient = Brush.linearGradient(listOf(MintDeep, Mint, Cyan))

/** پس‌زمینهٔ شفق: گرادیان‌های رادیال محو برند روی سطح — امضای «Aurora Study». */
@Composable
fun auroraBrush(dark: Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f): Brush {
    val cs = MaterialTheme.colorScheme
    return remember(cs.primary, cs.secondary, cs.background, dark) {
        Brush.radialGradient(
            colors = listOf(
                cs.primary.copy(alpha = if (dark) 0.15f else 0.09f),
                cs.secondary.copy(alpha = if (dark) 0.07f else 0.045f),
                cs.background,
            ),
            center = androidx.compose.ui.geometry.Offset(-0.35f, -0.5f),
            radius = 950f,
        )
    }
}

val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

private fun TextStyle.vazir(weight: FontWeight? = null) =
    copy(fontFamily = Vazir, fontWeight = weight ?: fontWeight)

private val base = Typography()
val AppTypography = Typography(
    displayLarge = base.displayLarge.vazir(FontWeight.Black),
    displayMedium = base.displayMedium.vazir(FontWeight.Black),
    displaySmall = base.displaySmall.vazir(FontWeight.Black),
    headlineLarge = base.headlineLarge.vazir(FontWeight.Black),
    headlineMedium = base.headlineMedium.vazir(FontWeight.Bold),
    headlineSmall = base.headlineSmall.vazir(FontWeight.Bold),
    titleLarge = base.titleLarge.vazir(FontWeight.Bold),
    titleMedium = base.titleMedium.vazir(FontWeight.Bold),
    titleSmall = base.titleSmall.vazir(FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.vazir(),
    bodyMedium = base.bodyMedium.vazir(),
    bodySmall = base.bodySmall.vazir(),
    labelLarge = base.labelLarge.vazir(FontWeight.Bold),
    labelMedium = base.labelMedium.vazir(FontWeight.SemiBold),
    labelSmall = base.labelSmall.vazir(FontWeight.Medium),
)

/** گوشه‌های نرم و بزرگ — امضای بصری ۲۰۲۶ (قرص‌گونه‌تر از قبل). */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(38.dp),
)

@Composable
fun DarsHubTheme(darkTheme: Boolean = isSystemInDarkTheme(), colorId: String = "mint", content: @Composable () -> Unit) {
    val palette = paletteOf(colorId)
    val scheme = remember(colorId, darkTheme) { if (darkTheme) darkSchemeOf(palette) else lightSchemeOf(palette) }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
