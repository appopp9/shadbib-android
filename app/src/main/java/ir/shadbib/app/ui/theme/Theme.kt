package ir.shadbib.app.ui.theme

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.shadbib.app.R

// ---------- پالت برند «شب مطالعه» ----------
val Mint = Color(0xFF4ADE9F)
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

// ==================== تم‌های رنگی (سبک تلگرام) ====================
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
    AppPalette("mint", "نعنایی", "🌿", Mint, MintDark, Cyan, Color(0xFF0284C7)),
    AppPalette("ocean", "اقیانوس", "🌊", Color(0xFF38BDF8), Color(0xFF0369A1), Mint, MintDark),
    AppPalette("grape", "انگوری", "🍇", Color(0xFFA78BFA), Color(0xFF7C3AED), Color(0xFFF472B6), Color(0xFFDB2777)),
    AppPalette("sunset", "غروب", "🌅", Color(0xFFFB923C), Color(0xFFEA580C), Color(0xFFFBBF24), Color(0xFFB45309), tDark = Color(0xFFF472B6), tLight = Color(0xFFDB2777)),
    AppPalette("rose", "شکوفه", "🌸", Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFFA78BFA), Color(0xFF7C3AED)),
    AppPalette("gold", "طلایی", "✨", Color(0xFFFBBF24), Color(0xFFB45309), Color(0xFF38BDF8), Color(0xFF0369A1), tDark = Mint, tLight = MintDark),
)

fun paletteOf(id: String): AppPalette = AppPalettes.firstOrNull { it.id == id } ?: AppPalettes.first()

/** تم تیرهٔ عمیق «شب مطالعه» — پس‌زمینه سبزفام نزدیک به سیاه با کارت‌های لایه‌ای. */
fun darkSchemeOf(p: AppPalette): ColorScheme {
    val nBg = Color(0xFF0B100E); val nSurf = Color(0xFF131A16); val nVar = Color(0xFF1A2420)
    return darkColorScheme(
        primary = p.pDark,
        onPrimary = lerp(Color(0xFF06110C), p.pDark, 0.10f),
        primaryContainer = lerp(nSurf, p.pDark, 0.24f),
        onPrimaryContainer = lerp(Color.White, p.pDark, 0.30f),
        secondary = p.sDark,
        onSecondary = lerp(Color(0xFF06110C), p.sDark, 0.10f),
        secondaryContainer = lerp(nSurf, p.sDark, 0.22f),
        onSecondaryContainer = lerp(Color.White, p.sDark, 0.30f),
        tertiary = p.tDark,
        onTertiary = lerp(Color(0xFF06110C), p.tDark, 0.10f),
        tertiaryContainer = lerp(nSurf, p.tDark, 0.22f),
        onTertiaryContainer = lerp(Color.White, p.tDark, 0.30f),
        background = lerp(nBg, p.pDark, 0.035f),
        onBackground = lerp(Color(0xFFECF4EF), p.pDark, 0.04f),
        surface = lerp(nSurf, p.pDark, 0.045f),
        onSurface = lerp(Color(0xFFECF4EF), p.pDark, 0.04f),
        surfaceVariant = lerp(nVar, p.pDark, 0.08f),
        onSurfaceVariant = lerp(Color(0xFF8FA39A), p.pDark, 0.10f),
        outline = lerp(Color(0xFF3A4742), p.pDark, 0.15f),
        outlineVariant = lerp(Color(0xFF232E29), p.pDark, 0.12f),
        error = Rose,
        onError = Color(0xFF4C0519),
    )
}

fun lightSchemeOf(p: AppPalette): ColorScheme {
    return lightColorScheme(
        primary = p.pLight,
        onPrimary = Color.White,
        primaryContainer = lerp(Color.White, p.pLight, 0.14f),
        onPrimaryContainer = lerp(p.pLight, Color.Black, 0.42f),
        secondary = p.sLight,
        onSecondary = Color.White,
        secondaryContainer = lerp(Color.White, p.sLight, 0.13f),
        onSecondaryContainer = lerp(p.sLight, Color.Black, 0.42f),
        tertiary = p.tLight,
        onTertiary = Color.White,
        tertiaryContainer = lerp(Color.White, p.tLight, 0.15f),
        onTertiaryContainer = lerp(p.tLight, Color.Black, 0.42f),
        background = lerp(Color(0xFFF7FAF8), p.pLight, 0.03f),
        onBackground = Color(0xFF14201B),
        surface = Color.White,
        onSurface = Color(0xFF14201B),
        surfaceVariant = lerp(Color(0xFFECF2EF), p.pLight, 0.06f),
        onSurfaceVariant = lerp(Color(0xFF46564F), p.pLight, 0.12f),
        outline = lerp(Color(0xFFC2CEC9), p.pLight, 0.16f),
        outlineVariant = lerp(Color(0xFFDCE6E1), p.pLight, 0.12f),
        error = Color(0xFFE11D48),
        onError = Color.White,
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

/** گوشه‌های نرم‌تر و بزرگ‌تر — امضای بصری بازطراحی. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(34.dp),
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
