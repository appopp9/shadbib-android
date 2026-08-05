package ir.shadbib.app.ui.auth

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.shadbib.app.core.Api
import ir.shadbib.app.core.SmsCodeAutoFill
import ir.shadbib.app.core.Store
import ir.shadbib.app.core.fa
import ir.shadbib.app.core.str
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/*
 * The whole entry experience.
 *
 * Server contract, exactly as api.php and otp.php expose it:
 *   register  step 1  otp_request_register  { phone }
 *   register  step 2  otp_verify_register   { phone, code }        -> ticket
 *   register  step 3  register              { ticket, username, password } -> token
 *   reset     step 1  otp_request_reset     { phone }
 *   reset     step 2  otp_verify_reset      { phone, code }        -> ticket
 *   reset     step 3  reset_password        { ticket, new_password }
 *
 * Registration is open: no invite code, so INVITE_CODE_REQUIRED must stay false
 * in config.php or every request from this app is rejected before the sms is
 * even attempted. The sms budget is protected server side by the per number,
 * per ip and global daily caps in otp_rate_check().
 *
 * Visual layer lives in AuthUi.kt and now follows the same Material 3 language
 * as the rest of the app.
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

    // the last code we fired automatically, so a full box never resubmits twice
    var autoSent by remember { mutableStateOf("") }

    // bumped on every send, so a resend reopens the sms consent window
    var smsSession by remember { mutableStateOf(0) }

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
            ?: throw Exception("شماره موبایل معتبر وارد کن")
        val action = if (purpose == Step.REG_CODE) "otp_request_register" else "otp_request_reset"
        val res = Api.obj(Api.post(action, JSONObject().put("phone", normalized)))
        resendIn = res.optInt("resend_in", 60)
        expiresIn = res.optInt("expires_in", 120)

        /*
         * dev_code only exists while SMS_ENABLED is false on the server. On a
         * live server the field is absent and this branch never runs.
         *
         * While it does exist we prefill the cells, and mark the code as already
         * auto sent so the flow pauses instead of skipping the screen entirely.
         */
        val dev = res.optString("dev_code", "")
        if (dev.length == 6) {
            code = dev
            autoSent = dev
            hint = "پیامک روی سرور خاموش است — کد خودکار پر شد"
        } else {
            code = ""
            autoSent = ""
            hint = null
        }
        smsSession += 1
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
        hint = "رمز جدید ثبت شد. حالا وارد شو \ud83c\udf89"
        code = ""; ticket = ""; password = ""
        step = Step.LOGIN
    }

    /*
     * Reads the code out of the incoming sms so the user does not have to.
     *
     * Live only while the code screen is showing. Whatever it produces lands in
     * the same `code` state the keypad writes to, so the auto submit below
     * fires identically whether the digits were typed or captured.
     */
    SmsCodeAutoFill(
        enabled = step == Step.REG_CODE || step == Step.RESET_CODE,
        restartKey = smsSession,
    ) { received ->
        if (received != code) {
            error = null
            code = received
        }
    }

    // six digits typed on the pad submit on their own, like every otp screen
    LaunchedEffect(code, step) {
        val onCode = step == Step.REG_CODE || step == Step.RESET_CODE
        if (onCode && code.length == 6 && code != autoSent && !loading) {
            autoSent = code
            delay(140)
            verifyCode()
        }
    }

    AuthBackdrop {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AuthLogo()
            Spacer(Modifier.height(14.dp))
            Text(
                "شادبیب",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (step) {
                    Step.LOGIN -> "خوش برگشتی! بیا ادامهٔ درس خوندن"
                    Step.REG_PHONE, Step.REG_CODE, Step.REG_PROFILE -> "ساخت حساب جدید"
                    else -> "بازیابی رمز عبور"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            Box(Modifier.widthIn(max = 420.dp)) {
                AuthCard {
                    val wizardStep = when (step) {
                        Step.REG_PHONE, Step.RESET_PHONE -> 0
                        Step.REG_CODE, Step.RESET_CODE -> 1
                        Step.REG_PROFILE, Step.RESET_PASS -> 2
                        else -> -1
                    }
                    if (wizardStep >= 0) {
                        AuthSteps(wizardStep)
                        Spacer(Modifier.height(18.dp))
                    }

                    if (error != null) {
                        AuthBanner(error ?: "", error = true)
                        Spacer(Modifier.height(14.dp))
                    } else if (hint != null) {
                        AuthBanner(hint ?: "", error = false)
                        Spacer(Modifier.height(14.dp))
                    }

                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (slideInHorizontally(tween(300)) { w -> -w / 4 } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(300)) { w -> w / 4 } + fadeOut(tween(160)))
                        },
                        label = "authStep",
                    ) { current ->
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            when (current) {
                                Step.LOGIN -> {
                                    AuthField(
                                        value = username,
                                        onValueChange = { username = it },
                                        label = "نام کاربری",
                                        placeholder = "hasan",
                                        leading = Icons.Rounded.Person,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    AuthField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = "رمز عبور",
                                        password = true,
                                        leading = Icons.Rounded.Lock,
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    AuthPrimaryButton(
                                        text = "ورود به شادبیب",
                                        loading = loading,
                                        enabled = username.isNotBlank() && password.length >= 6,
                                    ) { doLogin(username, password) }
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        AuthLink("حساب ندارم") {
                                            error = null; hint = null; step = Step.REG_PHONE
                                        }
                                        AuthLink("رمزم را فراموش کردم") {
                                            error = null; hint = null; phone = ""; step = Step.RESET_PHONE
                                        }
                                    }
                                }

                                Step.REG_PHONE, Step.RESET_PHONE -> {
                                    Text(
                                        "شمارهٔ موبایلت را وارد کن",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    PhoneInput(
                                        phone = phone,
                                        onPhoneChange = { phone = it },
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        "یک کد ۶ رقمی برایت پیامک می‌شود — خودکار خوانده می‌شود",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(18.dp))
                                    AuthPrimaryButton(
                                        text = "ارسال کد تأیید",
                                        loading = loading,
                                        enabled = IranPhone.isValid(phone),
                                    ) {
                                        askCode(if (current == Step.REG_PHONE) Step.REG_CODE else Step.RESET_CODE)
                                    }
                                    AuthLink("بازگشت به ورود") {
                                        error = null; hint = null; step = Step.LOGIN
                                    }
                                }

                                Step.REG_CODE, Step.RESET_CODE -> {
                                    Text(
                                        "کد ۶ رقمی را به " + IranPhone.maskLtr(phone) + " فرستادیم",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    OtpCells(
                                        code = code,
                                        error = error != null,
                                        shake = shake,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        if (expiresIn > 0)
                                            "اعتبار کد: " + expiresIn.fa() + " ثانیه"
                                        else
                                            "کد منقضی شد، دوباره بگیر",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    CodeKeypad(
                                        onDigit = { d ->
                                            if (code.length < 6) {
                                                error = null
                                                code += d
                                            }
                                        },
                                        onBackspace = {
                                            if (code.isNotEmpty()) code = code.dropLast(1)
                                        },
                                        onPaste = { digits ->
                                            error = null
                                            code = digits.take(6)
                                        },
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    AuthPrimaryButton(
                                        text = "تأیید کد",
                                        loading = loading,
                                        enabled = code.length == 6,
                                    ) { verifyCode() }
                                    if (resendIn > 0) {
                                        Text(
                                            "ارسال مجدد تا " + resendIn.fa() + " ثانیه",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(top = 12.dp),
                                        )
                                    } else {
                                        AuthLink("ارسال مجدد کد") { askCode(current) }
                                    }
                                    AuthLink("ویرایش شماره") {
                                        error = null; hint = null; code = ""; autoSent = ""
                                        step = if (current == Step.REG_CODE) Step.REG_PHONE else Step.RESET_PHONE
                                    }
                                }

                                Step.REG_PROFILE -> {
                                    AuthField(
                                        value = username,
                                        onValueChange = { username = it },
                                        label = "نام کاربری",
                                        placeholder = "۳ تا ۳۰ کاراکتر",
                                        leading = Icons.Rounded.Person,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    AuthField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = "رمز عبور",
                                        placeholder = "حداقل ۶ کاراکتر",
                                        password = true,
                                        leading = Icons.Rounded.Lock,
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    AuthPrimaryButton(
                                        text = "ساخت حساب و ورود",
                                        loading = loading,
                                        enabled = username.trim().length >= 3 && password.length >= 6,
                                    ) { createAccount() }
                                }

                                Step.RESET_PASS -> {
                                    AuthField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = "رمز جدید",
                                        placeholder = "حداقل ۶ کاراکتر",
                                        password = true,
                                        leading = Icons.Rounded.Lock,
                                    )
                                    Spacer(Modifier.height(20.dp))
                                    AuthPrimaryButton(
                                        text = "ثبت رمز جدید",
                                        loading = loading,
                                        enabled = password.length >= 6,
                                    ) { savedNewPassword() }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
        }
    }
}
