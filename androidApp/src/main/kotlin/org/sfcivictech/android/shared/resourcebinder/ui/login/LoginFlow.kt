package org.sfcivictech.android.shared.resourcebinder.ui.login

import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.sfcivictech.android.shared.resourcebinder.AccountService
import org.sfcivictech.android.shared.resourcebinder.FakeAccountService
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.SendSignInLinkResult
import org.sfcivictech.android.shared.resourcebinder.VerifySignInLinkResult

sealed interface LoginUiState {
    data class EmailEntry(
        val email: String = "",
        val fieldError: String? = null,
        val bannerError: String? = null,
        val isSubmitting: Boolean = false,
    ) : LoginUiState

    data class SendingLink(
        val email: String = "",
    ) : LoginUiState

    data class CheckingEmail(
        val email: String = "",
        val resendCooldownInSeconds: Int = 0,
    ) : LoginUiState

    data object SigningIn : LoginUiState

    data object AccountNotFound : LoginUiState

    data object LinkExpired : LoginUiState

    data object LinkAlreadyUsed : LoginUiState
}

@Composable
fun LoginFlow(
    accountService: AccountService = FakeAccountService(),
    onRequestAccess: () -> Unit = {},
    onSignedIn: () -> Unit = {}
    ) {
    //start the UI on the enter email page
    var uiState by remember { mutableStateOf<LoginUiState>(LoginUiState.EmailEntry()) }
    //remembers the coroutine calling accountService
    val scope = rememberCoroutineScope()

    val invalidEmailMessage = stringResource(R.string.login_error_invalid_email)
    val networkErrorMessage = stringResource(R.string.login_error_network)

    fun onEmailChanged(newEmail: String) {
        val current = uiState as? LoginUiState.EmailEntry ?: return
        uiState = current.copy(email = newEmail, fieldError = null)
    }

    fun onSubmitClick() {
        val current = uiState as? LoginUiState.EmailEntry ?: return

        if (!isValidEmail(current.email)) {
            uiState = current.copy(fieldError = invalidEmailMessage)
            return
        }

        scope.launch {
            uiState = LoginUiState.SendingLink(current.email)
            uiState = submitEmail(current.email, accountService, networkErrorMessage)
        }
    }

    fun onResendClick() {
        val current = uiState as? LoginUiState.CheckingEmail ?: return

        scope.launch {
            uiState = LoginUiState.SendingLink(current.email)
            uiState = submitEmail(current.email, accountService, networkErrorMessage)
        }
    }

    //debug-only stand-in for tapping the sign-in link in an email client; bypasses
    //accountService entirely so every outcome is reachable, not just whatever the
    //fake happens to be configured to return
    fun onDebugSimulateVerify(result: VerifySignInLinkResult) {
        scope.launch {
            uiState = LoginUiState.SigningIn
            delay(600)
            when (result) {
                VerifySignInLinkResult.SUCCESS -> onSignedIn()
                VerifySignInLinkResult.EXPIRED -> uiState = LoginUiState.LinkExpired
                VerifySignInLinkResult.ALREADY_USED -> uiState = LoginUiState.LinkAlreadyUsed
                //no dedicated screen for this case; treat it the same as an expired link
                VerifySignInLinkResult.NETWORK_ERROR -> uiState = LoginUiState.LinkExpired
            }
        }
    }

    //returns to a blank email entry screen from any of the dead-end screens
    fun onStartOver() {
        uiState = LoginUiState.EmailEntry()
    }

    //counts down the resend cooldown one second at a time
    LaunchedEffect(uiState) {
        val current = uiState as? LoginUiState.CheckingEmail ?: return@LaunchedEffect
        if (current.resendCooldownInSeconds > 0) {
            delay(1000)
            uiState = current.copy(resendCooldownInSeconds = current.resendCooldownInSeconds - 1)
        }
    }

    //renders the current page
    when(val state = uiState) {
        is LoginUiState.EmailEntry -> LoginWelcomeScreen(
            email = state.email,
            fieldError = state.fieldError,
            bannerError = state.bannerError,
            onEmailChanged = ::onEmailChanged,
            onSubmitClick = ::onSubmitClick,
            onRequestAccessClick = onRequestAccess,
        )
        is LoginUiState.SendingLink -> LoginSendingLinkScreen()
        is LoginUiState.CheckingEmail -> LoginCheckEmailScreen(
            email = state.email,
            resendCooldownSeconds = state.resendCooldownInSeconds,
            onResendClick = ::onResendClick,
            onDebugSimulateSuccessClick = { onDebugSimulateVerify(VerifySignInLinkResult.SUCCESS) },
            onDebugSimulateExpiredClick = { onDebugSimulateVerify(VerifySignInLinkResult.EXPIRED) },
            onDebugSimulateAlreadyUsedClick = { onDebugSimulateVerify(VerifySignInLinkResult.ALREADY_USED) },
        )
        LoginUiState.SigningIn -> LoginSigningInScreen()
        LoginUiState.AccountNotFound -> LoginAccountNotFoundScreen(
            onTryAnotherEmailClick = ::onStartOver,
            onRequestAccessClick = onRequestAccess,
        )
        LoginUiState.LinkExpired -> LoginLinkExpiredScreen(onSendNewLinkClick = ::onStartOver)
        LoginUiState.LinkAlreadyUsed -> LoginLinkAlreadyUsedScreen(onSendNewLinkClick = ::onStartOver)
    }
}

private fun isValidEmail(email: String): Boolean {
    return !TextUtils.isEmpty(email) &&
                  android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}

private suspend fun submitEmail(
    email: String,
    accountService: AccountService,
    networkErrorMessage: String,
): LoginUiState {
    val result = accountService.sendSignInLink(email)

    return when (result) {
        SendSignInLinkResult.SUCCESS ->
            LoginUiState.CheckingEmail(email = email, resendCooldownInSeconds = 30)
        SendSignInLinkResult.ACCOUNT_NOT_FOUND ->
            LoginUiState.AccountNotFound
        SendSignInLinkResult.NETWORK_ERROR ->
            LoginUiState.EmailEntry(email = email, bannerError = networkErrorMessage)
    }
}
