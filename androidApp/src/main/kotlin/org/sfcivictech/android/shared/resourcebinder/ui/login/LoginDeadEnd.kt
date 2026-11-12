package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

@Composable
fun LoginAccountNotFoundScreen(
    onTryAnotherEmailClick: () -> Unit,
    onRequestAccessClick: () -> Unit,
) {
    LoginDeadEndScreen(
        title = stringResource(R.string.login_account_not_found_title),
        description = stringResource(R.string.login_account_not_found_description),
        secondaryButtonLabel = stringResource(R.string.login_account_not_found_action_secondary),
        onSecondaryClick = onTryAnotherEmailClick,
        primaryButtonLabel = stringResource(R.string.login_account_not_found_action_primary),
        onPrimaryClick = onRequestAccessClick,
    )
}

@Composable
fun LoginLinkExpiredScreen(onSendNewLinkClick: () -> Unit) {
    LoginDeadEndScreen(
        title = stringResource(R.string.login_link_expired_title),
        description = stringResource(R.string.login_link_expired_description),
        primaryButtonLabel = stringResource(R.string.login_link_expired_action),
        onPrimaryClick = onSendNewLinkClick,
    )
}

@Composable
fun LoginLinkAlreadyUsedScreen(onSendNewLinkClick: () -> Unit) {
    LoginDeadEndScreen(
        title = stringResource(R.string.login_link_already_used_title),
        description = stringResource(R.string.login_link_already_used_description),
        primaryButtonLabel = stringResource(R.string.login_link_already_used_action),
        onPrimaryClick = onSendNewLinkClick,
    )
}

@Composable
private fun LoginDeadEndScreen(
    title: String,
    description: String,
    primaryButtonLabel: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonLabel: String? = null,
    onSecondaryClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (secondaryButtonLabel != null) {
            Button(
                onClick = onSecondaryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ),
            ) {
                Text(text = secondaryButtonLabel)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onPrimaryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(text = primaryButtonLabel)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, name = "1. Account not found")
@Composable
private fun LoginAccountNotFoundPreview() {
    ResourceBinderTheme {
        LoginAccountNotFoundScreen(
            onTryAnotherEmailClick = {},
            onRequestAccessClick = {},
        )
    }
}

@Preview(showBackground = true, name = "2. Link expired")
@Composable
private fun LoginLinkExpiredPreview() {
    ResourceBinderTheme {
        LoginLinkExpiredScreen(onSendNewLinkClick = {})
    }
}

@Preview(showBackground = true, name = "3. Link already used")
@Composable
private fun LoginLinkAlreadyUsedPreview() {
    ResourceBinderTheme {
        LoginLinkAlreadyUsedScreen(onSendNewLinkClick = {})
    }
}
