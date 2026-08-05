package ir.shadbib.app.ui.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * Neobrutalism kit for the auth flow.
 *
 * House rules, straight from the project style DNA:
 *   - thick pure black outlines, one uniform stroke weight
 *   - flat solid fills, never a gradient
 *   - hard offset shadows, solid black, zero blur
 *   - motion is snappy and springy, never a slow fade
 */

object Neo {
    val Ink = Color(0xFF0B130E)
    val Mint = Color(0xFF34D399)
    val Cream = Color(0xFFF2FAF5)
    val Sand = Color(0xFFE8D5A3)
    val Coral = Color(0xFFFF8A65)
    val Sky = Color(0xFF7DD3FC)
    val Line = Color(0xFF0B130E)

    val stroke = 3.dp
    val shadow = 6.dp
    val radius = 18.dp
}

/**
 * The signature block: flat fill, black outline, hard offset shadow.
 * [lift] drives the shadow depth, so a pressed control can visually sink.
 */
fun Modifier.neoBlock(
    fill: Color,
    lift: Dp = Neo.shadow,
    radius: Dp = Neo.radius,
    stroke: Dp = Neo.stroke,
    outline: Color = Neo.Line,
): Modifier = this
    .drawBehind {
        val r = radius.toPx()
        val off = lift.toPx()
        if (off > 0f) {
            drawRoundRect(
                color = Neo.Line,
                topLeft = androidx.compose.ui.geometry.Offset(off, off),
                size = size,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r),
            )
        }
    }
    .background(fill, RoundedCornerShape(radius))
    .border(stroke, outline, RoundedCornerShape(radius))

/**
 * Primary action. On press the block sinks into its own shadow, which is the
 * whole charm of the style, and it doubles as loading feedback.
 */
@Composable
fun NeoButton(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = Neo.Mint,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val active = enabled && !loading
    val lift by animateDpAsState(
        targetValue = if (pressed && active) 1.dp else Neo.shadow,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 1200f),
        label = "neoLift",
    )
    val nudge = Neo.shadow - lift

    Box(
        modifier
            .fillMaxWidth()
            .padding(end = Neo.shadow, bottom = Neo.shadow)
            .offset(x = nudge, y = nudge)
            .neoBlock(if (active) fill else fill.copy(alpha = 0.45f), lift = lift)
            .clickable(
                enabled = active,
                interactionSource = interaction,
                indication = null,
            ) { onClick() }
            .height(58.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Neo.Ink,
                strokeWidth = 3.dp,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(
                text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Neo.Ink,
            )
        }
    }
}

/** Flat ghost action, for "back" and "I already have an account". */
@Composable
fun NeoGhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Neo.Ink.copy(alpha = 0.72f),
        )
    }
}

/**
 * Custom flat text field. No Material outline, no floating label: the label is
 * its own little black chip sitting on top of the block.
 */
@Composable
fun NeoField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    fill: Color = Neo.Cream,
    accent: Color = Neo.Mint,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null,
    onImeAction: () -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    var reveal by remember { mutableStateOf(false) }
    val lift by animateDpAsState(
        targetValue = if (focused) 2.dp else Neo.shadow,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "fieldLift",
    )
    val bg by animateColorAsState(
        targetValue = if (focused) accent.copy(alpha = 0.16f) else fill,
        animationSpec = tween(220),
        label = "fieldBg",
    )
    val nudge = Neo.shadow - lift

    Column(modifier) {
        Box(
            Modifier
                .padding(start = 6.dp, bottom = 6.dp)
                .neoBlock(accent, lift = 0.dp, radius = 10.dp, stroke = 2.dp)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Neo.Ink,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(end = Neo.shadow, bottom = Neo.shadow)
                .offset(x = nudge, y = nudge)
                .neoBlock(bg, lift = lift)
                .height(58.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Neo.Ink.copy(alpha = 0.35f),
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Neo.Ink,
                            fontWeight = FontWeight.Bold,
                        ),
                        cursorBrush = SolidColor(Neo.Ink),
                        visualTransformation = if (password && !reveal) PasswordVisualTransformation() else VisualTransformation.None,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { onImeAction() },
                            onGo = { onImeAction() },
                            onNext = { onImeAction() },
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused },
                    )
                }
                if (password) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .neoBlock(if (reveal) Neo.Sand else Neo.Cream, lift = 0.dp, radius = 8.dp, stroke = 2.dp)
                            .clickable { reveal = !reveal }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            if (reveal) "\u0645\u062e\u0641\u06cc" else "\u0646\u0645\u0627\u06cc\u0634",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Neo.Ink,
                        )
                    }
                } else if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                }
            }
        }
    }
}

/**
 * Six boxes for the sms code.
 *
 * One invisible BasicTextField owns the text, the boxes are pure drawing. Each
 * filled digit pops in with a spring, the waiting box breathes, and the whole
 * row shakes when the server rejects the code.
 */
@Composable
fun NeoOtpBoxes(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    shake: Int = 0,
    onFilled: () -> Unit = {},
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    // Shake is driven by a counter, so every new error replays the animation.
    val shakeX = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(shake) {
        if (shake > 0) {
            for (step in listOf(-14f, 12f, -8f, 5f, 0f)) {
                shakeX.animateTo(step, tween(55, easing = LinearEasing))
            }
        }
    }

    val breathe = rememberInfiniteTransition(label = "otpBreathe")
    val pulse by breathe.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
        label = "otpPulse",
    )

    Box(modifier) {
        BasicTextField(
            value = code,
            onValueChange = { raw ->
                val digits = IranPhone.digitsOnly(raw).take(length)
                onCodeChange(digits)
                if (digits.length == length) onFilled()
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .focusRequester(focus),
        )

        // The comment below was already the intent, but inside an rtl app the Row
        // silently flipped and the first typed digit landed in the rightmost box.
        // Pinning the layout direction makes the intent actually hold.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            Modifier
                .fillMaxWidth()
                .offset(x = shakeX.value.dp)
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    runCatching { focus.requestFocus() }
                },
            horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        ) {
            // Left to right on purpose: codes are read like numbers, not like text.
            for (i in 0 until length) {
                val filled = i < code.length
                val isNext = i == code.length
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.92f,
                    animationSpec = spring(dampingRatio = 0.42f, stiffness = 700f),
                    label = "otpScale",
                )
                Box(
                    Modifier
                        .width(46.dp)
                        .padding(end = 4.dp, bottom = 4.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .neoBlock(
                            fill = if (filled) Neo.Mint else Neo.Cream,
                            lift = if (filled) 4.dp else 2.dp,
                            radius = 12.dp,
                        )
                        .height(58.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (filled) {
                        Text(
                            code[i].toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Neo.Ink,
                            textAlign = TextAlign.Center,
                        )
                    } else if (isNext) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .background(Neo.Ink.copy(alpha = pulse)),
                        )
                    }
                }
            }
        }
        }
    }
}

/** Small flat notice block. Coral for errors, sand for hints. */
@Composable
fun NeoBanner(text: String, fill: Color = Neo.Coral, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(end = 4.dp, bottom = 4.dp)
            .neoBlock(fill, lift = 4.dp, radius = 12.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Neo.Ink,
        )
    }
}

/** Step dots for the three step wizard. The active step is a wide pill. */
@Composable
fun NeoSteps(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until total) {
            val active = i == step
            val w by animateDpAsState(
                targetValue = if (active) 34.dp else 12.dp,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
                label = "stepW",
            )
            Box(
                Modifier
                    .width(w)
                    .height(12.dp)
                    .neoBlock(
                        fill = if (i <= step) Neo.Ink else Neo.Cream,
                        lift = 0.dp,
                        radius = 6.dp,
                        stroke = 2.dp,
                    ),
            )
        }
    }
}

/**
 * Groups the raw digits as 0912 345 6789 while typing.
 *
 * The offset mapping is the delicate part: Compose validates every cursor
 * translation and throws if the two directions disagree, so both functions are
 * derived from the same two split points and clamped to the real string length.
 */
private object PhoneDigitsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val d = text.text.take(11)
        val out = when {
            d.length <= 4 -> d
            d.length <= 7 -> d.substring(0, 4) + " " + d.substring(4)
            else -> d.substring(0, 4) + " " + d.substring(4, 7) + " " + d.substring(7)
        }

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var o = offset
                if (offset > 4) o += 1
                if (offset > 7) o += 1
                return o.coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                var o = offset
                if (offset > 4) o -= 1
                if (offset > 8) o -= 1
                return o.coerceIn(0, d.length)
            }
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/**
 * Dedicated Iranian phone input.
 *
 * The +98 chip is fixed and not editable, digits are always shown latin and
 * grouped, and the block turns mint the moment the number becomes valid, so the
 * user knows before pressing anything.
 *
 * Direction: the rest of the app is right to left, but a phone number is not
 * Persian text, it is a number. Typed right aligned it reads backwards and the
 * caret jumps to the wrong end. The whole input row is therefore pinned to left
 * to right with LocalLayoutDirection, and the text style carries an explicit
 * TextDirection.Ltr so the +98 chip stays on the left and digits fill rightward,
 * exactly like every phone dialer.
 */
@Composable
fun PhoneField(
    phone: String,
    onPhoneChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val valid = IranPhone.isValid(phone)
    val plausible = IranPhone.couldBecomeValid(phone)

    val lift by animateDpAsState(
        targetValue = if (focused) 2.dp else Neo.shadow,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "phoneLift",
    )
    val fill by animateColorAsState(
        targetValue = when {
            !plausible -> Neo.Coral.copy(alpha = 0.35f)
            valid -> Neo.Mint.copy(alpha = 0.35f)
            focused -> Neo.Sky.copy(alpha = 0.18f)
            else -> Neo.Cream
        },
        animationSpec = tween(240),
        label = "phoneFill",
    )
    val nudge = Neo.shadow - lift

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .padding(start = 6.dp, bottom = 6.dp)
                .neoBlock(Neo.Sky, lift = 0.dp, radius = 10.dp, stroke = 2.dp)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                "\u0634\u0645\u0627\u0631\u0647\u0654 \u0645\u0648\u0628\u0627\u06cc\u0644",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Neo.Ink,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(end = Neo.shadow, bottom = Neo.shadow)
                .offset(x = nudge, y = nudge)
                .neoBlock(fill, lift = lift)
                .height(58.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
          CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // fixed country chip
                Box(
                    Modifier
                        .neoBlock(Neo.Sand, lift = 0.dp, radius = 8.dp, stroke = 2.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        "+98",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Neo.Ink,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (phone.isEmpty()) {
                        Text(
                            "0912 345 6789",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                textAlign = TextAlign.Start,
                                textDirection = TextDirection.Ltr,
                            ),
                            color = Neo.Ink.copy(alpha = 0.3f),
                        )
                    }
                    BasicTextField(
                        value = phone,
                        onValueChange = { onPhoneChange(IranPhone.digitsOnly(it).take(11)) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Neo.Ink,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Start,
                            textDirection = TextDirection.Ltr,
                            letterSpacing = 1.sp,
                        ),
                        visualTransformation = PhoneDigitsTransformation,
                        cursorBrush = SolidColor(Neo.Ink),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focused = it.isFocused },
                    )
                }
                // live validity mark, springs in when the number completes
                val markScale by animateFloatAsState(
                    targetValue = if (valid) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = 700f),
                    label = "phoneMark",
                )
                Box(
                    Modifier
                        .size(30.dp)
                        .graphicsLayer { scaleX = markScale; scaleY = markScale }
                        .neoBlock(Neo.Mint, lift = 0.dp, radius = 15.dp, stroke = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "\u2713",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Neo.Ink,
                    )
                }
            }
          }
        }
        if (!plausible) {
            Text(
                "\u0641\u0642\u0637 \u0634\u0645\u0627\u0631\u0647\u0654 \u0645\u0648\u0628\u0627\u06cc\u0644 \u0627\u06cc\u0631\u0627\u0646 \u2014 \u0645\u062b\u0644 " + IranPhone.ltr("09123456789"),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Neo.Coral,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp),
            )
        }
    }
}
