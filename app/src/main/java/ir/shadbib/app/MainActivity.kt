package ir.shadbib.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import ir.shadbib.app.core.Store
import ir.shadbib.app.player.PlayerHolder
import ir.shadbib.app.ui.auth.AuthScreen
import ir.shadbib.app.ui.nav.MainScaffold
import ir.shadbib.app.ui.theme.DarsHubTheme
import ir.shadbib.app.ui.theme.brandGradient
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by Store.prefs.collectAsState()
            val dark = when (prefs.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                DarsHubTheme(darkTheme = dark, colorId = prefs.themeColor) {
                    AppRoot()
                }
            }
        }
    }
}

/** ریشهٔ اپ + اسپلش انیمیشنی برند با ترنزیشن نرم به محتوا. */
@Composable
private fun AppRoot() {
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(1600); splash = false }
    Box(Modifier.fillMaxSize()) {
        Root()
        AnimatedVisibility(
            visible = splash,
            exit = fadeOut(tween(520, easing = FastOutSlowInEasing)) +
                    scaleOut(tween(520, easing = FastOutSlowInEasing), targetScale = 1.08f),
        ) { AnimatedSplash() }
    }
}

@Composable
private fun Root() {
    val session by Store.session.collectAsState()
    if (session.token == null) {
        AuthScreen()
    } else {
        LaunchedEffect(Unit) { PlayerHolder.init(App.instance) }
        MainScaffold()
    }
}

/** اسپلش برند: لوگوی گرادیانی تپنده، حلقهٔ گرادیانی چرخان، وردمارک و سه نقطهٔ چشمک‌زن. */
@Composable
private fun AnimatedSplash() {
    val cs = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "splash")
    val rot by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rot")
    val pulse by t.animateFloat(0.94f, 1.06f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulse")
    val d1 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "d1")
    val d2 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse, StartOffset(180)), label = "d2")
    val d3 by t.animateFloat(0.25f, 1f, infiniteRepeatable(tween(550), RepeatMode.Reverse, StartOffset(360)), label = "d3")
    var on by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { on = true }
    val enter by animateFloatAsState(if (on) 1f else 0f, tween(650, easing = FastOutSlowInEasing), label = "enter")

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(lerp(cs.background, cs.primary, 0.07f), cs.background))
        ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.graphicsLayer {
                alpha = enter
                scaleX = 0.9f + 0.1f * enter; scaleY = 0.9f + 0.1f * enter
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(150.dp).graphicsLayer { rotationZ = rot }) {
                    drawArc(
                        color = cs.onSurface.copy(alpha = 0.06f),
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        style = Stroke(width = 13f, cap = StrokeCap.Round))
                    drawArc(
                        brush = Brush.sweepGradient(listOf(cs.primary.copy(alpha = 0f), cs.secondary, cs.primary)),
                        startAngle = 15f, sweepAngle = 300f, useCenter = false,
                        style = Stroke(width = 13f, cap = StrokeCap.Round))
                }
                Box(
                    Modifier
                        .size(92.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .clip(RoundedCornerShape(28.dp))
                        .background(brandGradient()),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.AutoStories, null, tint = cs.onPrimary, modifier = Modifier.size(46.dp))
                }
            }
            Spacer(Modifier.height(18.dp))
            Text("درس هاب", style = MaterialTheme.typography.headlineLarge, color = cs.onBackground)
            Text("مطالعه، با هم قشنگ‌تره", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).graphicsLayer { alpha = d1 }.background(cs.primary, CircleShape))
                Box(Modifier.size(7.dp).graphicsLayer { alpha = d2 }.background(cs.primary, CircleShape))
                Box(Modifier.size(7.dp).graphicsLayer { alpha = d3 }.background(cs.primary, CircleShape))
            }
        }
    }
}
