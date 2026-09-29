package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

const val LOGIN_EMAIL_FIELD_TEST_TAG = "login_email_field"

@Composable
fun LoginWelcomeScreen(
    email: String,
    fieldError: String?,
    bannerError: String?,
    onEmailChanged: (String) -> Unit,
    onSubmitClick: () -> Unit,
    onRequestAccessClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 24.dp),
    ) {
        if (bannerError != null) {
            BannerError(bannerError)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.login_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.login_welcome_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.login_welcome_email_label),
                style = MaterialTheme.typography.labelLarge,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(LOGIN_EMAIL_FIELD_TEST_TAG),
                placeholder = { Text(stringResource(R.string.login_welcome_email_placeholder)) },
                isError = fieldError != null,
                supportingText = if (fieldError != null) {
                    { Text(text = fieldError, color = MaterialTheme.colorScheme.error) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmitClick() }),
            )
        }

        Text(
            text = stringResource(R.string.login_welcome_request_access),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onRequestAccessClick)
                .padding(vertical = 8.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSubmitClick,
            enabled = email.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = stringResource(R.string.login_welcome_action_primary))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BannerError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "!",
                    color = MaterialTheme.colorScheme.onError,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Preview(showBackground = true, name = "1. Empty")
@Composable
private fun LoginWelcomeEmptyPreview() {
    ResourceBinderTheme {
        LoginWelcomeScreen(
            email = "",
            fieldError = null,
            bannerError = null,
            onEmailChanged = {},
            onSubmitClick = {},
            onRequestAccessClick = {},
        )
    }
}

@Preview(showBackground = true, name = "2. Filled")
@Composable
private fun LoginWelcomeFilledPreview() {
    ResourceBinderTheme {
        LoginWelcomeScreen(
            email = "nikolai@civic-tech.design",
            fieldError = null,
            bannerError = null,
            onEmailChanged = {},
            onSubmitClick = {},
            onRequestAccessClick = {},
        )
    }
}

@Preview(showBackground = true, name = "3. Invalid email")
@Composable
private fun LoginWelcomeInvalidEmailPreview() {
    ResourceBinderTheme {
        LoginWelcomeScreen(
            email = "nikolai@1234567",
            fieldError = "Enter a valid email.",
            bannerError = null,
            onEmailChanged = {},
            onSubmitClick = {},
            onRequestAccessClick = {},
        )
    }
}

@Preview(showBackground = true, name = "4. Network error")
@Composable
private fun LoginWelcomeNetworkErrorPreview() {
    ResourceBinderTheme {
        LoginWelcomeScreen(
            email = "nikolai@civic-tech.design",
            fieldError = null,
            bannerError = "No internet connection. Try again.",
            onEmailChanged = {},
            onSubmitClick = {},
            onRequestAccessClick = {},
        )
    }
}
