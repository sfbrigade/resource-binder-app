package org.sfcivictech.android.shared.resourcebinder.ui.register

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.sfcivictech.android.shared.resourcebinder.AccountService
import org.sfcivictech.android.shared.resourcebinder.FakeAccountService
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.FormValidator
import org.sfcivictech.android.shared.resourcebinder.RequestAccessResult
import org.sfcivictech.android.shared.resourcebinder.VerifyInvitationResult

//what the user typed into the request access form
data class RegisterDetails(
    val fullName: String = "",
    val email: String = "",
    val organization: String = "",
)

sealed interface RegisterUiState {
    data class FormEntry(
        val details: RegisterDetails = RegisterDetails(),
        val nameError: String? = null,
        val emailError: String? = null,
        val organizationError: String? = null,
        val bannerError: String? = null,
    ) : RegisterUiState

    data class Submitting(val details: RegisterDetails) : RegisterUiState

    data class CheckingEmail(
        val details: RegisterDetails,
        val resendCooldownInSeconds: Int = 0,
    ) : RegisterUiState

    data class AccountAlreadyExists(val details: RegisterDetails) : RegisterUiState

    data class CreatingAccount(val details: RegisterDetails) : RegisterUiState

    data object Welcome : RegisterUiState

    data class InvitationExpired(val details: RegisterDetails) : RegisterUiState

    data class SomethingWentWrong(val details: RegisterDetails) : RegisterUiState
}

private const val RESEND_COOLDOWN_IN_SECONDS = 30

@Composable
fun RegisterFlow(
    accountService: AccountService = FakeAccountService(),
    initialEmail: String = "",
    onBack: () -> Unit = {},
    //receives the email entered so far so sign-in can start with it filled in
    onGoToSignIn: (email: String) -> Unit = {},
    onRegistered: () -> Unit = {},
) {
    //start the UI on the form, with the email carried over from the login screen
    //saveable so rotation, a theme change or process death doesn't wipe what was entered
    var uiState by rememberSaveable(stateSaver = RegisterUiStateSaver) {
        mutableStateOf<RegisterUiState>(
            RegisterUiState.FormEntry(details = RegisterDetails(email = initialEmail)),
        )
    }
    //remembers the coroutine calling accountService
    val scope = rememberCoroutineScope()

    val nameErrorMessage = stringResource(R.string.register_error_name)
    val emailErrorMessage = stringResource(R.string.register_error_email)
    val organizationErrorMessage = stringResource(R.string.register_error_organization)
    val networkErrorMessage = stringResource(R.string.register_error_network)

    fun errorFor(field: RegisterField, details: RegisterDetails): String? = when (field) {
        RegisterField.NAME ->
            nameErrorMessage.takeUnless { FormValidator.isValidFullName(details.fullName) }
        RegisterField.EMAIL ->
            emailErrorMessage.takeUnless { FormValidator.isValidEmail(details.email) }
        RegisterField.ORGANIZATION ->
            organizationErrorMessage.takeUnless { FormValidator.isValidOrganization(details.organization) }
    }

    //typing clears that field's error; it is checked again when the user leaves the field
    fun onFullNameChanged(value: String) {
        val current = uiState as? RegisterUiState.FormEntry ?: return
        uiState = current.copy(details = current.details.copy(fullName = value), nameError = null)
    }

    fun onEmailChanged(value: String) {
        val current = uiState as? RegisterUiState.FormEntry ?: return
        uiState = current.copy(details = current.details.copy(email = value), emailError = null)
    }

    fun onOrganizationChanged(value: String) {
        val current = uiState as? RegisterUiState.FormEntry ?: return
        uiState = current.copy(
            details = current.details.copy(organization = value),
            organizationError = null,
        )
    }

    //an empty field isn't flagged; the disabled submit button already says it's incomplete
    fun onFieldFocusLost(field: RegisterField) {
        val current = uiState as? RegisterUiState.FormEntry ?: return
        val details = current.details
        uiState = when (field) {
            RegisterField.NAME -> current.copy(
                nameError = errorFor(field, details).takeIf { details.fullName.isNotBlank() },
            )
            RegisterField.EMAIL -> current.copy(
                emailError = errorFor(field, details).takeIf { details.email.isNotBlank() },
            )
            RegisterField.ORGANIZATION -> current.copy(
                organizationError = errorFor(field, details).takeIf { details.organization.isNotBlank() },
            )
        }
    }

    fun submit(details: RegisterDetails) {
        val trimmed = details.trimmed()
        //switched before launching so a second tap can't start another request
        uiState = RegisterUiState.Submitting(trimmed)
        scope.launch {
            uiState = requestAccess(trimmed, accountService, networkErrorMessage)
        }
    }

    fun onSubmitClick() {
        val current = uiState as? RegisterUiState.FormEntry ?: return
        val details = current.details

        //the last field edited may never have lost focus, so check everything here too
        val validated = current.copy(
            nameError = errorFor(RegisterField.NAME, details),
            emailError = errorFor(RegisterField.EMAIL, details),
            organizationError = errorFor(RegisterField.ORGANIZATION, details),
            bannerError = null,
        )
        if (validated.nameError != null || validated.emailError != null || validated.organizationError != null) {
            uiState = validated
            return
        }

        submit(details)
    }

    fun onResendClick() {
        val current = uiState as? RegisterUiState.CheckingEmail ?: return
        submit(current.details)
    }

    fun onSendNewInvitationClick() {
        val current = uiState as? RegisterUiState.InvitationExpired ?: return
        submit(current.details)
    }

    //returns to the form with whatever the user had entered
    fun onReturnToForm(details: RegisterDetails) {
        uiState = RegisterUiState.FormEntry(details = details)
    }

    //debug-only stand-in for tapping the invitation link in an email client; bypasses
    //accountService entirely so every outcome is reachable, not just whatever the
    //fake happens to be configured to return
    fun onDebugSimulateVerify(result: VerifyInvitationResult) {
        val details = (uiState as? RegisterUiState.CheckingEmail)?.details ?: return
        scope.launch {
            uiState = RegisterUiState.CreatingAccount(details)
            delay(600)
            uiState = when (result) {
                VerifyInvitationResult.SUCCESS -> RegisterUiState.Welcome
                VerifyInvitationResult.EXPIRED -> RegisterUiState.InvitationExpired(details)
                VerifyInvitationResult.NETWORK_ERROR -> RegisterUiState.SomethingWentWrong(details)
            }
        }
    }

    //system back mirrors the on-screen back buttons; screens without one swallow it
    BackHandler {
        when (val state = uiState) {
            is RegisterUiState.FormEntry -> onBack()
            is RegisterUiState.CheckingEmail -> onReturnToForm(state.details)
            is RegisterUiState.AccountAlreadyExists -> onReturnToForm(state.details)
            is RegisterUiState.InvitationExpired -> onReturnToForm(state.details)
            is RegisterUiState.SomethingWentWrong -> onReturnToForm(state.details)
            is RegisterUiState.Submitting,
            is RegisterUiState.CreatingAccount,
            RegisterUiState.Welcome -> Unit
        }
    }

    //counts down the resend cooldown one second at a time
    LaunchedEffect(uiState) {
        val current = uiState as? RegisterUiState.CheckingEmail ?: return@LaunchedEffect
        if (current.resendCooldownInSeconds > 0) {
            delay(1000)
            uiState = current.copy(resendCooldownInSeconds = current.resendCooldownInSeconds - 1)
        }
    }

    //renders the current page
    when (val state = uiState) {
        is RegisterUiState.FormEntry -> RegisterFormScreen(
            fullName = state.details.fullName,
            email = state.details.email,
            organization = state.details.organization,
            nameError = state.nameError,
            emailError = state.emailError,
            organizationError = state.organizationError,
            bannerError = state.bannerError,
            submitEnabled = state.details.isComplete() &&
                state.nameError == null &&
                state.emailError == null &&
                state.organizationError == null,
            onFullNameChanged = ::onFullNameChanged,
            onEmailChanged = ::onEmailChanged,
            onOrganizationChanged = ::onOrganizationChanged,
            onFieldFocusLost = ::onFieldFocusLost,
            onSubmitClick = ::onSubmitClick,
            onBack = onBack,
        )
        is RegisterUiState.Submitting -> RegisterSubmittingScreen()
        is RegisterUiState.CheckingEmail -> RegisterCheckEmailScreen(
            email = state.details.email,
            resendCooldownSeconds = state.resendCooldownInSeconds,
            onResendClick = ::onResendClick,
            onBack = { onReturnToForm(state.details) },
            onDebugSimulateSuccessClick = { onDebugSimulateVerify(VerifyInvitationResult.SUCCESS) },
            onDebugSimulateExpiredClick = { onDebugSimulateVerify(VerifyInvitationResult.EXPIRED) },
            onDebugSimulateErrorClick = { onDebugSimulateVerify(VerifyInvitationResult.NETWORK_ERROR) },
        )
        is RegisterUiState.AccountAlreadyExists -> RegisterAccountExistsScreen(
            onUseDifferentEmailClick = { onReturnToForm(state.details) },
            onGoToSignInClick = { onGoToSignIn(state.details.email.trim()) },
            onBack = { onReturnToForm(state.details) },
        )
        is RegisterUiState.CreatingAccount -> RegisterCreatingAccountScreen()
        RegisterUiState.Welcome -> RegisterWelcomeScreen(onGetStartedClick = onRegistered)
        is RegisterUiState.InvitationExpired -> RegisterInvitationExpiredScreen(
            onSendNewInvitationClick = ::onSendNewInvitationClick,
        )
        is RegisterUiState.SomethingWentWrong -> RegisterSomethingWentWrongScreen(
            onBack = { onReturnToForm(state.details) },
        )
    }
}

private fun RegisterDetails.trimmed() = RegisterDetails(
    fullName = fullName.trim(),
    email = email.trim(),
    organization = organization.trim(),
)

private fun RegisterDetails.isComplete(): Boolean =
    fullName.isNotBlank() && email.isNotBlank() && organization.isNotBlank()

private suspend fun requestAccess(
    details: RegisterDetails,
    accountService: AccountService,
    networkErrorMessage: String,
): RegisterUiState {
    val result = accountService.requestAccess(
        fullName = details.fullName,
        email = details.email,
        organization = details.organization,
    )

    return when (result) {
        RequestAccessResult.SUCCESS ->
            RegisterUiState.CheckingEmail(
                details = details,
                resendCooldownInSeconds = RESEND_COOLDOWN_IN_SECONDS,
            )
        RequestAccessResult.ACCOUNT_ALREADY_EXISTS ->
            RegisterUiState.AccountAlreadyExists(details)
        RequestAccessResult.NETWORK_ERROR ->
            RegisterUiState.FormEntry(details = details, bannerError = networkErrorMessage)
    }
}

//Transient states are saved as the screen the user would resume from: nothing is still
//in flight after a restore, so Submitting falls back to the form (entries intact) and
//CreatingAccount to check-your-email, where the invitation link can be tapped again.
private val RegisterUiStateSaver = listSaver<RegisterUiState, Any?>(
    save = { state ->
        val details = when (state) {
            is RegisterUiState.FormEntry -> state.details
            is RegisterUiState.Submitting -> state.details
            is RegisterUiState.CheckingEmail -> state.details
            is RegisterUiState.AccountAlreadyExists -> state.details
            is RegisterUiState.CreatingAccount -> state.details
            is RegisterUiState.InvitationExpired -> state.details
            is RegisterUiState.SomethingWentWrong -> state.details
            RegisterUiState.Welcome -> RegisterDetails()
        }
        val form = state as? RegisterUiState.FormEntry
        val checking = state as? RegisterUiState.CheckingEmail
        listOf(
            when (state) {
                is RegisterUiState.FormEntry, is RegisterUiState.Submitting -> "form"
                is RegisterUiState.CheckingEmail, is RegisterUiState.CreatingAccount -> "checking"
                is RegisterUiState.AccountAlreadyExists -> "exists"
                is RegisterUiState.InvitationExpired -> "expired"
                is RegisterUiState.SomethingWentWrong -> "wrong"
                RegisterUiState.Welcome -> "welcome"
            },
            details.fullName,
            details.email,
            details.organization,
            form?.nameError,
            form?.emailError,
            form?.organizationError,
            form?.bannerError,
            checking?.resendCooldownInSeconds ?: 0,
        )
    },
    restore = { saved ->
        val details = RegisterDetails(
            fullName = saved[1] as String,
            email = saved[2] as String,
            organization = saved[3] as String,
        )
        when (saved[0] as String) {
            "checking" -> RegisterUiState.CheckingEmail(details, saved[8] as Int)
            "exists" -> RegisterUiState.AccountAlreadyExists(details)
            "expired" -> RegisterUiState.InvitationExpired(details)
            "wrong" -> RegisterUiState.SomethingWentWrong(details)
            "welcome" -> RegisterUiState.Welcome
            else -> RegisterUiState.FormEntry(
                details = details,
                nameError = saved[4] as String?,
                emailError = saved[5] as String?,
                organizationError = saved[6] as String?,
                bannerError = saved[7] as String?,
            )
        }
    },
)
