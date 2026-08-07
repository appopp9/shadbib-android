package ir.darshub.app.core

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

/*
 * Automatic OTP fill, built on the SMS User Consent API.
 *
 * Why this one and not SMS Retriever: Retriever is fully invisible but it only
 * works if the message body ends with an 11 character hash derived from our
 * signing key. Our text comes out of the Melipayamak pattern (MELI_BODY_ID),
 * so changing it means editing the panel template and re-editing it every time
 * the signing key changes. User Consent needs nothing in the message at all.
 *
 * What the app is allowed to see: nothing, until the user taps allow on a
 * system dialog, and then exactly one message. There is no READ_SMS, no
 * RECEIVE_SMS, no manifest entry and no runtime permission request. Play
 * Services holds the messages the whole time; we only ever receive the single
 * body the user released to us.
 *
 * Everything here degrades quietly. On a handset with no Google Play Services,
 * startSmsUserConsent simply fails and the keypad stays the only way in.
 */

private const val TAG = "DarsHubSms"

/** Latin, Persian and Arabic-Indic digits, so any sender template parses. */
private fun latinDigits(s: String): String = buildString {
    for (c in s) {
        val code = c.code
        when {
            c.isDigit() -> append(c)
            code in 0x06F0..0x06F9 -> append(('0' + (code - 0x06F0)))
            code in 0x0660..0x0669 -> append(('0' + (code - 0x0660)))
            else -> append(c)
        }
    }
}

/** First standalone 6 digit run in the message. */
internal fun extractOtp(body: String, length: Int = 6): String? =
    Regex("(?<!\\d)\\d{" + length + "}(?!\\d)").find(latinDigits(body))?.value

/**
 * Listens for the verification sms while [enabled] is true.
 *
 * @param enabled  true only on the code screen, so we never hold a listener open
 *                 in the background.
 * @param restartKey bump this on every resend. The consent window closes after
 *                 five minutes or after one delivered message, so a resend has
 *                 to open a fresh one.
 * @param onCode   called on the main thread with the six digits.
 */
@Composable
fun SmsCodeAutoFill(
    enabled: Boolean,
    restartKey: Int = 0,
    onCode: (String) -> Unit,
) {
    val context = LocalContext.current
    val latestOnCode by rememberUpdatedState(onCode)

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val body = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return@rememberLauncherForActivityResult
        extractOtp(body)?.let { latestOnCode(it) }
    }

    val receiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != SmsRetriever.SMS_RETRIEVED_ACTION) return
                val extras = intent.extras ?: return
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return
                if (status.statusCode != CommonStatusCodes.SUCCESS) return
                // The dialog itself. We never see the body unless the user allows it.
                val consent: Intent = extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT) ?: return
                runCatching { consentLauncher.launch(consent) }
                    .onFailure { Log.w(TAG, "consent dialog refused to open", it) }
            }
        }
    }

    DisposableEffect(enabled, restartKey) {
        if (!enabled) return@DisposableEffect onDispose { }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            SmsRetriever.SEND_PERMISSION,   // only Play Services may deliver this
            null,
            ContextCompat.RECEIVER_EXPORTED,
        )

        // null sender = accept the code from whichever gateway the panel used
        SmsRetriever.getClient(context).startSmsUserConsent(null)
            .addOnFailureListener { Log.w(TAG, "sms consent unavailable, manual entry only", it) }

        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
}
