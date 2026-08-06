package ir.darshub.app.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.darshub.app.core.fa
import ir.darshub.app.ui.theme.DarsMotion
import ir.darshub.app.ui.theme.brandGradient
import ir.darshub.app.ui.theme.pressScale

/*
 * Design system for the entry screens — «Aurora» 2026.
 *
 * Speaks the same visual language as the rest of the app: Material 3 colour
 * scheme, AppShapes corners, Vazir typography, brandGradient accents and the
 * soft layered glass cards used on Home / Library / Profile.
 */

/* ---------------- background ---------------- */

/** Page shell: brand tinted gradient, three aurora pools + two floating orbs. */
@Composable
fun AuthBackdrop(content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "authOrbs")
    val f1 by t.animateFloat(-10f, 10f, infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse, startOffset = 200), label = "orbA")
    val f2 by t.animateFloat(9f, -9f, infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse, startOffset = 900), label = "orbB")
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(cs.background, cs.primary, 0.13f),
                        cs.background,
                        lerp(cs.background, cs.secondary, 0.09f),
                    )
                )
            )
    ) {
        /*
         * Three soft light pools + two floating glow orbs.
         * Softness comes from radial gradients that fade to fully transparent,
         * which every API level renders identically (Modifier.blur is a no-op
         * below API 31, so it is never used here).
         */
        Box(
            Modifier
                .size(320.dp)
                .offset(x = (-110).dp, y = (-140).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(cs.primary.copy(alpha = 0.20f), Color.Transparent),
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier
                .size(240.dp)
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = 90.dp)
                .graphicsLayer { translationY = f1 }
                .background(
                    Brush.radialGradient(
                        colors = listOf(cs.tertiary.copy(alpha = 0.14f), Color.Transparent),
                    ),
                    CircleShape,
                )
        )
        Box(
            Modifier
                .size(280.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 110.dp, y = 130.dp)
                .graphicsLayer { translationY = f2 }
                .background(
                    Brush.radialGradient(
                        colors = listOf(cs.secondary.copy(alpha = 0.16f), Color.Transparent),
                    ),
                    CircleShape,
                )
        )
        content()
    }
}

/** Brand mark, same rounded gradient tile as the splash screen, with a glow. */
@Composable
fun AuthLogo() {
    val t = rememberInfiniteTransition(label = "logo")
    val pulse by t.animateFloat(
        0.97f, 1.03f,
        infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "logoPulse",
    )
    Box(
        Modifier
            .size(78.dp)
            .graphicsLayer { scaleX = pulse; scaleY = pulse }
            .shadow(
                22.dp, RoundedCornerShape(26.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
            )
            .clip(RoundedCornerShape(26.dp))
            .background(brandGradient()),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.AutoStories,
            null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(38.dp),
        )
    }
}

/** The frosted glass card every step lives inside. */
@Composable
fun AuthCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.92f),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                )
            ),
        ),
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp), content = content)
    }
}

/* ---------------- feedback ---------------- */

/** Inline message strip. Errors use the theme error container, hints the tertiary one. */
@Composable
fun AuthBanner(text: String, error: Boolean) {
    val cs = MaterialTheme.colorScheme
    val bg = if (error) cs.error.copy(alpha = 0.12f) else cs.tertiary.copy(alpha = 0.13f)
    val fg = if (error) cs.error else cs.tertiary
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.28f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = fg,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}

/** Three segment wizard bar, filled with the brand gradient as it advances. */
@Composable
fun AuthSteps(current: Int, total: Int = 3) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        repeat(total) { i ->
            val done = i <= current
            val w by animateFloatAsState(if (i == current) 1f else 0.55f, tween(320), label = "segW")
            Box(
                Modifier
                    .weight(if (i == current) 1.5f else 1f)
                    .height(6.dp)
                    .graphicsLayer { alpha = if (done) 1f else 0.35f * w + 0.15f }
                    .clip(CircleShape)
                    .background(
                        if (done) brandGradient()
                        else SolidColor(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
                    )
            )
        }
    }
}

/* ---------------- buttons ---------------- */

/** Primary action: the app wide gradient pill, with an inline spinner. */
@Composable
fun AuthPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val live = enabled && !loading
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && live) 0.955f else 1f,
        spring(dampingRatio = 0.5f, stiffness = 900f),
        label = "authBtn",
    )
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (live) 1f else 0.45f }
            .shadow(
                if (live) 14.dp else 0.dp, CircleShape,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            )
            .clip(CircleShape)
            .background(brandGradient())
            .clickable(interaction, indication = null, enabled = live) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(21.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.4.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

/** Quiet text link used for the secondary paths. */
@Composable
fun AuthLink(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

/* ---------------- text fields ---------------- */

/** Standard outlined field, themed. Passwords get a reveal toggle. */
@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    password: Boolean = false,
    leading: ImageVector? = null,
) {
    var reveal by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        placeholder = if (placeholder == null) null else {
            {
                Text(
                    placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        },
        leadingIcon = if (leading == null) null else {
            { Icon(leading, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp)) }
        },
        trailingIcon = if (!password) null else {
            {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        if (reveal) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        visualTransformation =
            if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text
        ),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyLarge,
    )
}

/* ---------------- phone ---------------- */

/**
 * Groups the digits as 0912 345 6789 while typing.
 *
 * Only the rendering changes: the state still holds the plain 11 digits, so the
 * spaces never reach normalize() or the server.
 */
private object PhoneDigitsTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val d = text.text
        val out = buildString {
            d.forEachIndexed { i, c ->
                if (i == IranPhone.GROUP_1 || i == IranPhone.GROUP_2) append(' ')
                append(c)
            }
        }
        val mapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var o = offset
                if (offset > IranPhone.GROUP_1) o += 1
                if (offset > IranPhone.GROUP_2) o += 1
                return o.coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var o = offset
                if (offset > IranPhone.GROUP_1) o -= 1
                if (offset > IranPhone.GROUP_2 + 1) o -= 1
                return o.coerceIn(0, d.length)
            }
        }
        return androidx.compose.ui.text.input.TransformedText(androidx.compose.ui.text.AnnotatedString(out), mapping)
    }
}

/**
 * Phone entry.
 *
 * The whole row is pinned to LTR. Setting textAlign = Start would not be enough:
 * inside an RTL layout Start literally means right, so the prefix and the digits
 * would still run the wrong way.
 */
@Composable
fun PhoneInput(
    phone: String,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (focused) cs.primary else cs.outlineVariant.copy(alpha = 0.7f),
        tween(220), label = "phoneBorder",
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainerLow.copy(alpha = 0.85f),
        border = BorderStroke(if (focused) 2.dp else 1.dp, border),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "+98",
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(24.dp)
                        .background(cs.outlineVariant)
                )
                val digitStyle = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Start,
                    textDirection = TextDirection.Ltr,
                    letterSpacing = 0.5.sp,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (phone.isEmpty()) {
                        Text(
                            IranPhone.HINT,
                            style = digitStyle,
                            color = cs.onSurfaceVariant.copy(alpha = 0.42f),
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    BasicTextField(
                        value = phone,
                        onValueChange = { onPhoneChange(IranPhone.digitsOnly(it).take(11)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused },
                        textStyle = digitStyle.copy(color = cs.onSurface),
                        singleLine = true,
                        cursorBrush = SolidColor(cs.primary),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        visualTransformation = PhoneDigitsTransformation,
                    )
                }
            }
        }
    }
}

/* ---------------- otp cells ---------------- */

/**
 * Six read only cells.
 *
 * Deliberately not a text field: the code is driven only by CodeKeypad below,
 * so the system keyboard never opens on this step. Every other field in the
 * flow keeps the normal keyboard.
 */
@Composable
fun OtpCells(
    code: String,
    modifier: Modifier = Modifier,
    length: Int = 6,
    error: Boolean = false,
    shake: Int = 0,
) {
    val cs = MaterialTheme.colorScheme
    val nudge = remember { Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake > 0) {
            listOf(-15f, 13f, -10f, 8f, -4f, 0f).forEach { nudge.animateTo(it, tween(52)) }
        }
    }
    val t = rememberInfiniteTransition(label = "caret")
    val caret by t.animateFloat(
        1f, 0.15f,
        infiniteRepeatable(tween(620, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "caretAlpha",
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = nudge.value },
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            repeat(length) { i ->
                val filled = i < code.length
                val active = i == code.length
                val stroke = when {
                    error -> cs.error
                    active -> cs.primary
                    filled -> cs.primary.copy(alpha = 0.45f)
                    else -> cs.outlineVariant.copy(alpha = 0.7f)
                }
                val pop by animateFloatAsState(
                    if (filled) 1f else 0.94f,
                    spring(dampingRatio = 0.45f, stiffness = 700f),
                    label = "cellPop",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(0.82f)
                        .graphicsLayer { scaleX = pop; scaleY = pop },
                    shape = MaterialTheme.shapes.medium,
                    color = if (filled) cs.primary.copy(alpha = 0.10f) else cs.surfaceContainerLow.copy(alpha = 0.8f),
                    border = BorderStroke(if (active || error) 2.dp else 1.dp, stroke),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (filled) {
                            Text(
                                code[i].toString().fa(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = cs.onSurface,
                            )
                        } else if (active) {
                            Box(
                                Modifier
                                    .graphicsLayer { alpha = caret }
                                    .width(2.dp)
                                    .height(22.dp)
                                    .background(cs.primary, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- telegram style keypad ---------------- */

/** One key: no ripple, springy press, haptic tick. */
@Composable
private fun RowScopeKey(
    modifier: Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) 0.88f else 1f,
        spring(dampingRatio = 0.42f, stiffness = 1100f),
        label = "keyPress",
    )
    val bg by animateColorAsState(
        if (pressed && enabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f),
        tween(140), label = "keyBg",
    )
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.35f }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(interaction, indication = null, enabled = enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/**
 * The dedicated numeric pad for the verification code.
 *
 * Lives on the code step only. Because OtpCells is not focusable, this pad is
 * the single way to type the code and the soft keyboard stays down, which is
 * exactly how Telegram handles its login code.
 */
@Composable
fun CodeKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onPaste: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { d ->
                        RowScopeKey(
                            modifier = Modifier.weight(1f).height(56.dp),
                            onClick = { onDigit(d) },
                        ) {
                            Text(
                                d.toString().fa(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RowScopeKey(
                    modifier = Modifier.weight(1f).height(56.dp),
                    onClick = {
                        val raw = clipboard.getText()?.text.orEmpty()
                        val digits = IranPhone.digitsOnly(raw)
                        if (digits.isNotEmpty()) onPaste(digits)
                    },
                ) {
                    Icon(
                        Icons.Rounded.ContentPaste,
                        "چسباندن",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp),
                    )
                }
                RowScopeKey(
                    modifier = Modifier.weight(1f).height(56.dp),
                    onClick = { onDigit('0') },
                ) {
                    Text(
                        "0".fa(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                RowScopeKey(
                    modifier = Modifier.weight(1f).height(56.dp),
                    onClick = onBackspace,
                ) {
                    Icon(
                        Icons.Rounded.Backspace,
                        "حذف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
