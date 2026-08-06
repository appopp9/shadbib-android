package ir.darshub.app.core

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp

/*
 * 3D icon layer, backed by Microsoft Fluent Emoji (MIT).
 *
 * The whole point of this file is that the app never hard codes an R.drawable
 * id. Assets are looked up by name at runtime, so:
 *
 *   - the project compiles and runs perfectly with zero 3D assets present,
 *   - dropping the pngs into res/drawable-nodpi upgrades every call site at
 *     once with no code change,
 *   - a single missing icon degrades to the plain emoji instead of crashing.
 *
 * Naming is derived from the emoji itself, so there is no lookup table to keep
 * in sync. Fire is U+1F525, so the file is ic3d_1f525.png. Variation selectors
 * and zero width joiners are dropped, since the renders do not distinguish
 * them. tools/fetch_3d_icons.py produces exactly these names.
 */

/** ic3d_1f525 for the fire emoji. Skips U+FE0F and U+200D. */
internal fun icon3dName(emoji: String): String {
    val sb = StringBuilder("ic3d")
    var i = 0
    while (i < emoji.length) {
        val cp = emoji.codePointAt(i)
        i += Character.charCount(cp)
        if (cp == 0xFE0F || cp == 0x200D) continue
        sb.append('_').append(Integer.toHexString(cp))
    }
    return sb.toString()
}

/**
 * Draws [emoji] as a 3D render when the asset exists, otherwise as the emoji
 * glyph at a matching optical size.
 *
 * @param size the box the icon occupies. Use 40.dp and up. Below roughly 32.dp
 *   a 3D render turns to mush and a flat vector icon reads better, which is why
 *   the bottom bar deliberately still uses Material icons.
 */
@Composable
fun Icon3D(
    emoji: String,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val context = LocalContext.current
    val resId = remember(emoji) {
        @Suppress("DiscouragedApi")
        context.resources.getIdentifier(icon3dName(emoji), "drawable", context.packageName)
    }

    if (resId != 0) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDescription ?: emoji,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
        )
    } else {
        // Fallback. 0.78 keeps the glyph inside the same box the render fills.
        Box(modifier.size(size), contentAlignment = Alignment.Center) {
            Text(
                text = emoji,
                style = LocalTextStyle.current.copy(
                    fontSize = TextUnit(size.value * 0.78f, TextUnitType.Sp),
                ),
            )
        }
    }
}

/** True when the 3D pack has been installed for this emoji. */
@Composable
fun hasIcon3D(emoji: String): Boolean {
    val context = LocalContext.current
    return remember(emoji) {
        @Suppress("DiscouragedApi")
        context.resources.getIdentifier(icon3dName(emoji), "drawable", context.packageName) != 0
    }
}
