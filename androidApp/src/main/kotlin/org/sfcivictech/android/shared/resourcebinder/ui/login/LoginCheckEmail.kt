package org.sfcivictech.android.shared.resourcebinder.ui.login

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.BuildConfig
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

const val LOGIN_DEBUG_VERIFY_SUCCESS_TEST_TAG = "login_debug_verify_success"
const val LOGIN_DEBUG_VERIFY_EXPIRED_TEST_TAG = "login_debug_verify_expired"
const val LOGIN_DEBUG_VERIFY_ALREADY_USED_TEST_TAG = "login_debug_verify_already_used"

@Composable
fun LoginCheckEmailScreen(
    email: String,
    resendCooldownSeconds: Int,
    onResendClick: () -> Unit,
    onDebugSimulateSuccessClick: () -> Unit = {},
    onDebugSimulateExpiredClick: () -> Unit = {},
    onDebugSimulateAlreadyUsedClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()
            .padding(horizontal = 24.dp)
    ) {
        if (BuildConfig.DEBUG) {
            Text(
                text = "DEBUG: simulate tapping the email link as…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = "Success",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .testTag(LOGIN_DEBUG_VERIFY_SUCCESS_TEST_TAG)
                        .clickable(onClick = onDebugSimulateSuccessClick),
                )
                Text(
                    text = "Expired",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .testTag(LOGIN_DEBUG_VERIFY_EXPIRED_TEST_TAG)
                        .clickable(onClick = onDebugSimulateExpiredClick),
                )
                Text(
                    text = "Already used",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .testTag(LOGIN_DEBUG_VERIFY_ALREADY_USED_TEST_TAG)
                        .clickable(onClick = onDebugSimulateAlreadyUsedClick),
                )
            }
        }

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
                text = stringResource(R.string.login_check_email_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.login_check_email_description, email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            text = stringResource(R.string.login_check_email_resend_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onResendClick,
            enabled = resendCooldownSeconds <= 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            val label = if (resendCooldownSeconds > 0) {
                stringResource(R.string.login_check_email_resend_cooldown, resendCooldownSeconds)
            } else {
                stringResource(R.string.login_check_email_resend_action)
            }
            Text(text = label)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview(showBackground = true, name = "1. Counting down")
@Composable
private fun LoginCheckEmailCountingDownPreview() {
    ResourceBinderTheme {
        LoginCheckEmailScreen(
            email = "nikolai@civic-tech.design",
            resendCooldownSeconds = 27,
            onResendClick = {},
        )
    }
}

@Preview(showBackground = true, name = "2. Ready to resend")
@Composable
private fun LoginCheckEmailReadyPreview() {
    ResourceBinderTheme {
        LoginCheckEmailScreen(
            email = "nikolai@civic-tech.design",
            resendCooldownSeconds = 0,
            onResendClick = {},
        )
    }
}
