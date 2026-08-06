@file:OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)

package ir.darshub.app.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ir.darshub.app.ui.components.SegmentedTabs
import ir.darshub.app.ui.theme.auroraBrush
import ir.darshub.app.ui.theme.brandGradient

/** کتابخانه ۲۰۲۶ — هدر شفقی + کارت‌های آپلود بزرگ و واضح. */
@Composable
fun LibraryScreen() {
    var tab by remember { mutableIntStateOf(0) }
    var uploadSignal by remember { mutableIntStateOf(0) } // با هر کلیک CTA زیاد می‌شود تا شیت آپلود تب فعال باز شود

    Column(Modifier.fillMaxSize().background(auroraBrush())) {
        // هدر فشرده: تب‌ها + دکمه آپلود کوچک و حرفه‌ای
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("کتابخانه", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(20.dp), color = Color.Transparent, onClick = { uploadSignal++ },
                modifier = Modifier.shadow(
                    10.dp, RoundedCornerShape(20.dp),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                ),
            ) {
                Row(Modifier.background(brandGradient(), RoundedCornerShape(20.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("+", color = Color.White, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.width(4.dp))
                    Text(if (tab == 0) "موزیک جدید" else "جزوه جدید", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        SegmentedTabs(
            options = listOf("موزیک", "جزوه‌ها"),
            selected = tab,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) { tab = it }
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (fadeIn(tween(260, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 26 })
                    .togetherWith(fadeOut(tween(140)))
            },
            label = "libTab",
        ) { t ->
            when (t) {
                0 -> MusicTab(openUploadSignal = uploadSignal)
                else -> SummariesTab(openUploadSignal = uploadSignal)
            }
        }
    }
}
