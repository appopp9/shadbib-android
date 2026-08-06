package ir.darshub.app.ui.auth

import androidx.compose.runtime.Composable

/**
 * Entry point kept under the old name so MainActivity does not change.
 *
 * The old single form (username + password + invite) is gone: registration now
 * runs through the phone verification wizard in AuthFlow, because one phone can
 * own exactly one account and the account must survive a forgotten password.
 */
@Composable
fun AuthScreen() {
    AuthFlow()
}
