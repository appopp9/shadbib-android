package ir.shadbib.app.ui.auth

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.str
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/*
 * The whole entry experience, rebuilt in the neobrutalist style.
 *
 * Server contract, exactly as api.php and otp.php expose it:
 *   register  step 1  otp_request_register  { phone }
 *   register  step 2  otp_verify_register   { phone, code }        -> ticket
 *   register  step 3  register              { ticket, username, password } -> token
 *
 * Registration is open: no invite code, so INVITE_CODE_REQUIRED must stay false
 * in config.php or every request from this app is rejected before the sms is
 * even attempted. The sms budget is protected server side by the per number,
 * per ip and global daily caps in otp_rate_check().
 *   reset     step 1  otp_request_reset     { phone }
 *   reset     step 2  otp_verify_reset      { phone, code }        -> ticket
 *   reset     step 3  reset_password        { ticket, new_password }
 *
 * Only Iranian mobile numbers are accepted, checked here and again on the server.
 */

private enum class Step { LOGIN, REG_PHONE, REG_CODE, REG_PROFILE, RESET_PHONE, RESET_CODE, RESET_PASS }

/** Device fields, so the sessions screen can name the phone the user is holding. */
private fun deviceFields(o: JSONObject): JSONObject = o
    .put("platform", "android")
    .put("device_label", (Build.MANUFACTURER + " " + Build.MODEL).trim().take(60))

@Composable
fun AuthFlow() {
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(Step.LOGIN) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }

    // shared form state
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var ticket by remember { mutableStateOf("") }

    // sms countdowns, both driven by the server response
    var resendIn by remember { mutableStateOf(0) }
    var expiresIn by remember { mutableStateOf(0) }
    var shake by remember { mutableStateOf(0) }

    // one ticking loop for both counters
    LaunchedEffect(step) {
        while (true) {
            delay(1000)
            if (resendIn > 0) resendIn -= 1
            if (expiresIn > 0) expiresIn -= 1
        }
    }

    fun fail(e: Throwable) {
        error = e.message ?: "خطایی پیش آمد"
        shake += 1
        loading = false
    }

    fun run(block: suspend () -> Unit) {
        scope.launch {
            loading = true; error = null
            try {
                block()
                loading = false
            } catch (e: Exception) {
                fail(e)
            }
        }
    }

    fun doLogin(u: String, p: String) = run {
        val res = Api.obj(Api.post("login", deviceFields(JSONObject().put("username", u.trim()).put("password", p))))
        Store.saveSession(res.str("token"), res.str("username"))
    }

    fun askCode(purpose: Step) = run {
        val normalized = IranPhone.normalize(phone)
            ?: throw Exception("شماره موبایل ایرانی معتبر وارد کن")
        val action = if (purpose == Step.REG_CODE) "otp_request_register" else "otp_request_reset"
        val res = Api.obj(Api.post(action, JSONObject().put("phone", normalized)))
        resendIn = res.optInt("resend_in", 60)
        expiresIn = res.optInt("expires_in", 120)

        /*
         * dev_code only exists while SMS_ENABLED is false on the server. On a
         * live server the field is absent, this branch never runs and the user
         * sees the normal "we sent you a code" screen.
         *
         * While it does exist we prefill the boxes instead of printing the code
         * and making the tester copy it by hand.
         */
        val dev = res.optString("dev_code", "")
        if (dev.length == 6) {
            code = dev
            hint = "پیامک روی سرور خاموش است — کد خودکار پر شد"
        } else {
            code = ""
            hint = null
        }
        step = purpose
    }

    fun verifyCode() = run {
        val normalized = IranPhone.normalize(phone) ?: throw Exception("شماره موبایل معتبر نیست")
        val forRegister = step == Step.REG_CODE
        val action = if (forRegister) "otp_verify_register" else "otp_verify_reset"
        val res = Api.obj(Api.post(action, JSONObject().put("phone", normalized).put("code", code)))
        ticket = res.str("ticket")
        hint = null
        step = if (forRegister) Step.REG_PROFILE else Step.RESET_PASS
    }

    fun createAccount() = run {
        val body = JSONObject()
            .put("ticket", ticket)
            .put("username", username.trim())
            .put("password", password)
        val res = Api.obj(Api.post("register", deviceFields(body)))
        // The server logs us straight in, so no second round trip.
        Store.saveSession(res.str("token"), res.str("username"))
    }

    fun savedNewPassword() = run {
        Api.post("reset_password", JSONObject().put("ticket", ticket).put("new_password", password))
        hint = "رمز جدید ثبت شد. حالا وارد شو 🎉"
        code = ""; ticket = ""; password = ""
        step = Step.LOGIN
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Neo.Cream),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- brand block: flat square, black outline, hard shadow ----
            Box(
                Modifier
                    .size(88.dp)
                    .neoBlock(Neo.Mint, lift = 7.dp, radius = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("\ud83d\udcda", style = MaterialTheme.typography.displaySmall)
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "\u0634\u0627\u062f\u0628\u06cc\u0628",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Neo.Ink,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (step) {
                    Step.LOGIN -> "\u062e\u0648\u0634 \u0628\u0631\u06af\u0634\u062a\u06cc! \u0628\u06cc\u0627 \u0627\u062f\u0627\u0645\u0647\u0654 \u062f\u0631\u0633 \u062e\u0648\u0646\u062f\u0646"
                    Step.REG_PHONE, Step.REG_CODE, Step.REG_PROFILE -> "\u0633\u0627\u062e\u062a \u062d\u0633\u0627\u0628 \u062c\u062f\u06cc\u062f"
                    else -> "\u0628\u0627\u0632\u06cc\u0627\u0628\u06cc \u0631\u0645\u0632 \u0639\u0628\u0648\u0631"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Neo.Ink.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))

            // ---- wizard progress ----
            val wizardStep = when (step) {
                Step.REG_PHONE, Step.RESET_PHONE -> 0
                Step.REG_CODE, Step.RESET_CODE -> 1
                Step.REG_PROFILE, Step.RESET_PASS -> 2
                else -> -1
            }
            if (wizardStep >= 0) {
                NeoSteps(wizardStep, 3)
                Spacer(Modifier.height(18.dp))
            }

            if (error != null) {
                NeoBanner(error ?: "", fill = Neo.Coral)
                Spacer(Modifier.height(12.dp))
            } else if (hint != null) {
                NeoBanner(hint ?: "", fill = Neo.Sand)
                Spacer(Modifier.height(12.dp))
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(tween(300)) { w -> -w / 3 } + fadeIn(tween(220)))
                        .togetherWith(slideOutHorizontally(tween(300)) { w -> w / 3 } + fadeOut(tween(160)))
                },
                label = "authStep",
            ) { current ->
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (current) {
                        Step.LOGIN -> {
                            NeoField(
                                value = username,
                                onValueChange = { username = it },
                                label = "\u0646\u0627\u0645 \u06a9\u0627\u0631\u0628\u0631\u06cc",
                                placeholder = "hasan",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            NeoField(
                                value = password,
                                onValueChange = { password = it },
                                label = "\u0631\u0645\u0632 \u0639\u0628\u0648\u0631",
                                password = true,
                                accent = Neo.Sky,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(20.dp))
                            NeoButton(
                                text = "\u0648\u0631\u0648\u062f \u0628\u0647 \u0634\u0627\u062f\u0628\u06cc\u0628",
                                loading = loading,
                                enabled = username.isNotBlank() && password.length >= 6,
                            ) { doLogin(username, password) }
                            Spacer(Modifier.height(6.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                NeoGhostButton("\u062d\u0633\u0627\u0628 \u0646\u062f\u0627\u0631\u0645") {
                                    error = null; hint = null; step = Step.REG_PHONE
                                }
                                NeoGhostButton("\u0631\u0645\u0632\u0645 \u0631\u0627 \u0641\u0631\u0627\u0645\u0648\u0634 \u06a9\u0631\u062f\u0645") {
                                    error = null; hint = null; phone = ""; step = Step.RESET_PHONE
                                }
                            }
                        }

                        Step.REG_PHONE, Step.RESET_PHONE -> {
                            PhoneField(
                                phone = phone,
                                onPhoneChange = { phone = IranPhone.digitsOnly(it).take(11) },
                            )
                            Spacer(Modifier.height(20.dp))
                            NeoButton(
                                text = "\u0627\u0631\u0633\u0627\u0644 \u06a9\u062f \u062a\u0623\u06cc\u06cc\u062f",
                                loading = loading,
                                enabled = IranPhone.isValid(phone),
                            ) {
                                askCode(if (current == Step.REG_PHONE) Step.REG_CODE else Step.RESET_CODE)
                            }
                            NeoGhostButton("\u0628\u0627\u0632\u06af\u0634\u062a \u0628\u0647 \u0648\u0631\u0648\u062f") {
                                error = null; hint = null; step = Step.LOGIN
                            }
                        }

                        Step.REG_CODE, Step.RESET_CODE -> {
                            Text(
                                "\u06a9\u062f \u06f6 \u0631\u0642\u0645\u06cc \u0631\u0627 \u0628\u0647 " + IranPhone.maskLtr(phone) + " \u0641\u0631\u0633\u062a\u0627\u062f\u06cc\u0645",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Neo.Ink,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(16.dp))
                            NeoOtpBoxes(
                                code = code,
                                onCodeChange = { code = it },
                                shake = shake,
                                modifier = Modifier.fillMaxWidth(),
                            ) { verifyCode() }
                            Spacer(Modifier.height(14.dp))
                            Text(
                                if (expiresIn > 0)
                                    "\u0627\u0639\u062a\u0628\u0627\u0631 \u06a9\u062f: " + expiresIn + " \u062b\u0627\u0646\u06cc\u0647"
                                else
                                    "\u06a9\u062f \u0645\u0646\u0642\u0636\u06cc \u0634\u062f\u060c \u062f\u0648\u0628\u0627\u0631\u0647 \u0628\u06af\u06cc\u0631",
                                style = MaterialTheme.typography.labelMedium,
                                color = Neo.Ink.copy(alpha = 0.6f),
                            )
                            Spacer(Modifier.height(16.dp))
                            NeoButton(
                                text = "\u062a\u0623\u06cc\u06cc\u062f \u06a9\u062f",
                                loading = loading,
                                enabled = code.length == 6,
                            ) { verifyCode() }
                            if (resendIn > 0) {
                                Text(
                                    "\u0627\u0631\u0633\u0627\u0644 \u0645\u062c\u062f\u062f \u062a\u0627 " + resendIn + " \u062b\u0627\u0646\u06cc\u0647",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Neo.Ink.copy(alpha = 0.45f),
                                    modifier = Modifier.padding(top = 10.dp),
                                )
                            } else {
                                NeoGhostButton("\u0627\u0631\u0633\u0627\u0644 \u0645\u062c\u062f\u062f \u06a9\u062f") {
                                    askCode(current)
                                }
                            }
                            NeoGhostButton("\u0648\u06cc\u0631\u0627\u06cc\u0634 \u0634\u0645\u0627\u0631\u0647") {
                                error = null; hint = null; code = ""
                                step = if (current == Step.REG_CODE) Step.REG_PHONE else Step.RESET_PHONE
                            }
                        }

                        Step.REG_PROFILE -> {
                            NeoField(
                                value = username,
                                onValueChange = { username = it },
                                label = "\u0646\u0627\u0645 \u06a9\u0627\u0631\u0628\u0631\u06cc",
                                placeholder = "\u06f3 \u062a\u0627 \u06f3\u06f0 \u06a9\u0627\u0631\u0627\u06a9\u062a\u0631",
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(14.dp))
                            NeoField(
                                value = password,
                                onValueChange = { password = it },
                                label = "\u0631\u0645\u0632 \u0639\u0628\u0648\u0631",
                                placeholder = "\u062d\u062f\u0627\u0642\u0644 \u06f6 \u06a9\u0627\u0631\u0627\u06a9\u062a\u0631",
                                password = true,
                                accent = Neo.Sky,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(20.dp))
                            NeoButton(
                                text = "\u0633\u0627\u062e\u062a \u062d\u0633\u0627\u0628 \u0648 \u0648\u0631\u0648\u062f",
                                loading = loading,
                                enabled = username.trim().length >= 3 && password.length >= 6,
                            ) { createAccount() }
                        }

                        Step.RESET_PASS -> {
                            NeoField(
                                value = password,
                                onValueChange = { password = it },
                                label = "\u0631\u0645\u0632 \u062c\u062f\u06cc\u062f",
                                placeholder = "\u062d\u062f\u0627\u0642\u0644 \u06f6 \u06a9\u0627\u0631\u0627\u06a9\u062a\u0631",
                                password = true,
                                accent = Neo.Sky,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(Modifier.height(20.dp))
                            NeoButton(
                                text = "\u062b\u0628\u062a \u0631\u0645\u0632 \u062c\u062f\u06cc\u062f",
                                loading = loading,
                                enabled = password.length >= 6,
                            ) { savedNewPassword() }
                        }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                "\u062a\u0646\u0647\u0627 \u0634\u0645\u0627\u0631\u0647\u0654 \u0645\u0648\u0628\u0627\u06cc\u0644 \u0627\u06cc\u0631\u0627\u0646 \u067e\u0630\u06cc\u0631\u0641\u062a\u0647 \u0645\u06cc\u200c\u0634\u0648\u062f \ud83c\uddee\ud83c\uddf7",
                style = MaterialTheme.typography.labelSmall,
                color = Neo.Ink.copy(alpha = 0.4f),
            )
        }
    }
}
