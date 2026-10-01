package org.sfcivictech.android.shared.resourcebinder.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.common.BackButton
import org.sfcivictech.android.shared.resourcebinder.ui.common.BannerError
import org.sfcivictech.android.shared.resourcebinder.ui.common.FieldLabel
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

const val REGISTER_NAME_FIELD_TEST_TAG = "register_name_field"
const val REGISTER_EMAIL_FIELD_TEST_TAG = "register_email_field"
const val REGISTER_ORGANIZATION_FIELD_TEST_TAG = "register_organization_field"

enum class RegisterField { NAME, EMAIL, ORGANIZATION }

@Composable
fun RegisterFormScreen(
    fullName: String,
    email: String,
    organization: String,
    nameError: String?,
    emailError: String?,
    organizationError: String?,
    bannerError: String?,
    submitEnabled: Boolean,
    onFullNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onOrganizationChanged: (String) -> Unit,
    onFieldFocusLost: (RegisterField) -> Unit,
    onSubmitClick: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 24.dp),
    ) {
        if (bannerError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            BannerError(bannerError)
        }

        Spacer(modifier = Modifier.height(8.dp))
        BackButton(onClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.register_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.register_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(24.dp))

            RegisterTextField(
                label = stringResource(R.string.register_name_label),
                description = stringResource(R.string.register_name_field_description),
                placeholder = stringResource(R.string.register_name_placeholder),
                value = fullName,
                error = nameError,
                onValueChange = onFullNameChanged,
                onFocusLost = { onFieldFocusLost(RegisterField.NAME) },
                testTag = REGISTER_NAME_FIELD_TEST_TAG,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(16.dp))

            RegisterTextField(
                label = stringResource(R.string.register_email_label),
                description = stringResource(R.string.register_email_field_description),
                placeholder = stringResource(R.string.register_email_placeholder),
                value = email,
                error = emailError,
                onValueChange = onEmailChanged,
                onFocusLost = { onFieldFocusLost(RegisterField.EMAIL) },
                testTag = REGISTER_EMAIL_FIELD_TEST_TAG,
                keyboardType = KeyboardType.Email,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(16.dp))

            RegisterTextField(
                label = stringResource(R.string.register_organization_label),
                description = stringResource(R.string.register_organization_field_description),
                placeholder = stringResource(R.string.register_organization_placeholder),
                value = organization,
                error = organizationError,
                onValueChange = onOrganizationChanged,
                onFocusLost = { onFieldFocusLost(RegisterField.ORGANIZATION) },
                testTag = REGISTER_ORGANIZATION_FIELD_TEST_TAG,
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
                onDone = { if (submitEnabled) onSubmitClick() },
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSubmitClick,
            enabled = submitEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.register_action_submit))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RegisterTextField(
    label: String,
    description: String,
    placeholder: String,
    value: String,
    error: String?,
    onValueChange: (String) -> Unit,
    onFocusLost: () -> Unit,
    testTag: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    autoCorrectEnabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    // A field only counts as "left" after it has actually held focus, so the initial
    // unfocused state doesn't trigger validation of an untouched field.
    var hadFocus by remember { mutableStateOf(false) }

    // The field announces `description` itself, so the visual label (with its "*") is
    // hidden from screen readers rather than read as a second, unlinked element.
    FieldLabel(text = label, modifier = Modifier.clearAndSetSemantics {})

    Spacer(modifier = Modifier.height(8.dp))

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
            .semantics { contentDescription = description }
            .onFocusChanged { focusState ->
                if (hadFocus && !focusState.isFocused) onFocusLost()
                hadFocus = focusState.isFocused
            },
        placeholder = { Text(placeholder) },
        isError = error != null,
        supportingText = if (error != null) {
            { Text(text = error, color = MaterialTheme.colorScheme.error) }
        } else {
            null
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            autoCorrectEnabled = autoCorrectEnabled,
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
    )
}

@Preview(showBackground = true, name = "1. Empty")
@Composable
private fun RegisterFormEmptyPreview() {
    ResourceBinderTheme {
        RegisterFormScreen(
            fullName = "",
            email = "",
            organization = "",
            nameError = null,
            emailError = null,
            organizationError = null,
            bannerError = null,
            submitEnabled = false,
            onFullNameChanged = {},
            onEmailChanged = {},
            onOrganizationChanged = {},
            onFieldFocusLost = {},
            onSubmitClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "2. Filled")
@Composable
private fun RegisterFormFilledPreview() {
    ResourceBinderTheme {
        RegisterFormScreen(
            fullName = "Nikolai K",
            email = "nikolai@civic-tech.design",
            organization = "City Outreach Department",
            nameError = null,
            emailError = null,
            organizationError = null,
            bannerError = null,
            submitEnabled = true,
            onFullNameChanged = {},
            onEmailChanged = {},
            onOrganizationChanged = {},
            onFieldFocusLost = {},
            onSubmitClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "3. Invalid fields")
@Composable
private fun RegisterFormInvalidPreview() {
    ResourceBinderTheme {
        RegisterFormScreen(
            fullName = "1",
            email = "nikolai@1234567",
            organization = "1234567",
            nameError = "Enter correct name",
            emailError = "Enter a valid email",
            organizationError = "Enter a valid organization name",
            bannerError = null,
            submitEnabled = false,
            onFullNameChanged = {},
            onEmailChanged = {},
            onOrganizationChanged = {},
            onFieldFocusLost = {},
            onSubmitClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true, name = "4. Network error")
@Composable
private fun RegisterFormNetworkErrorPreview() {
    ResourceBinderTheme {
        RegisterFormScreen(
            fullName = "Nikolai K",
            email = "nikolai@civic-tech.design",
            organization = "City Outreach Department",
            nameError = null,
            emailError = null,
            organizationError = null,
            bannerError = "We're having trouble connecting. Please try again in a moment.",
            submitEnabled = true,
            onFullNameChanged = {},
            onEmailChanged = {},
            onOrganizationChanged = {},
            onFieldFocusLost = {},
            onSubmitClick = {},
            onBack = {},
        )
    }
}
