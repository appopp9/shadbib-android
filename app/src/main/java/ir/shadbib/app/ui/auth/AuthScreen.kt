package ir.shadbib.app.ui.auth
import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.str
import ir.shadbib.app.ui.theme.brandGradient
import ir.shadbib.app.ui.components.FadeSlideIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.lerp
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun AuthScreen() {
    val scope = rememberCoroutineScope()
    var isRegister by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    var inviteOk by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPass by remember { mutableStateOf(false) }

    fun login(u: String, p: String) {
        scope.launch {
            loading = true; error = null
            try {
                val res = Api.obj(Api.post("login", JSONObject().put("username", u.trim()).put("password", p)))
                Store.saveSession(res.str("token"), res.str("username"))
            } catch (e: Exception) {
                error = e.message ?: "خطا در ورود"
            }
            loading = false
        }
    }

    fun verifyInvite() {
        scope.launch {
            loading = true; error = null
            try {
                Api.post("verify_invite", JSONObject().put("invite_code", invite.trim()))
                inviteOk = true
            } catch (e: Exception) {
                error = e.message ?: "کد دعوت نامعتبر"
            }
            loading = false
        }
    }

    fun register() {
        scope.launch {
            loading = true; error = null
            try {
                Api.post(
                    "register",
                    JSONObject().put("username", username.trim()).put("password", password).put("invite_code", invite.trim())
                )
                val res = Api.obj(Api.post("login", JSONObject().put("username", username.trim()).put("password", password)))
                Store.saveSession(res.str("token"), res.str("username"))
            } catch (e: Exception) {
                error = e.message ?: "خطا در ثبت‌نام"
            }
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.radialGradient(
                    listOf(lerp(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary, 0.08f), MaterialTheme.colorScheme.background),
                    radius = 1400f,
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FadeSlideIn(0) {
            Box(
                Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(brandGradient()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
            }
            }
            Spacer(Modifier.height(18.dp))
            FadeSlideIn(1) { Text("شادبیب", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onBackground) }
            Spacer(Modifier.height(6.dp))
            FadeSlideIn(2) {
            Text(
                "با هم درس می‌خونیم، با هم رشد می‌کنیم ✨",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            }
            Spacer(Modifier.height(28.dp))

            // Mode switch — قرصی با هایلایت نرم
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
            ) {
                Row(Modifier.padding(5.dp)) {
                    ModePill("ورود", !isRegister) { isRegister = false; error = null }
                    ModePill("ثبت‌نام", isRegister) { isRegister = true; error = null }
                }
            }
            Spacer(Modifier.height(22.dp))

            if (isRegister && !inviteOk) {
                OutlinedTextField(
                    value = invite,
                    onValueChange = { invite = it },
                    label = { Text("کد دعوت") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton(text = "بررسی کد دعوت", loading = loading, enabled = invite.isNotBlank()) { verifyInvite() }
            } else {
                if (isRegister) {
                    Text(
                        "✅ کد دعوت تایید شد",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("نام کاربری") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("رمز عبور") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(18.dp))
                PrimaryButton(
                    text = if (isRegister) "ساخت حساب" else "ورود به شادبیب",
                    loading = loading,
                    enabled = username.isNotBlank() && password.length >= 6,
                ) {
                    if (isRegister) register() else login(username, password)
                }
                if (isRegister) {
                    TextButton(onClick = { inviteOk = false }) {
                        Text("تغییر کد دعوت", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ModePill(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
        androidx.compose.animation.core.tween(240), label = "modeBg")
    val fg by androidx.compose.animation.animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        androidx.compose.animation.core.tween(240), label = "modeFg")
    Surface(shape = CircleShape, color = bg, onClick = onClick) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 30.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = fg,
        )
    }
}

@Composable
private fun PrimaryButton(text: String, loading: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    // دکمهٔ گرادیانی برند با فشار فنری
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        if (pressed && enabled && !loading) 0.96f else 1f,
        androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 900f), label = "authBtn")
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        onClick = { if (enabled && !loading) onClick() },
        interactionSource = interaction,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f },
    ) {
        Box(
            Modifier.background(brandGradient()),
            contentAlignment = Alignment.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
