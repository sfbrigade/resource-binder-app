package org.sfcivictech.android.shared.resourcebinder.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme
import org.sfcivictech.android.shared.resourcebinder.ui.theme.SyncPendingColor

enum class CarePlanStatus(@StringRes val labelRes: Int) {
    IN_PROGRESS(R.string.home_status_in_progress),
    SYNC_PENDING(R.string.home_status_sync_pending),
    SHARED(R.string.home_status_shared),
}

data class CarePlanSummary(
    val number: String,
    val services: List<String>,
    val timeLabel: String,
    val status: CarePlanStatus,
)

fun carePlanRowTestTag(number: String) = "care_plan_row_$number"

// Gap above and below the headline block, matching the design
private val SECTION_SPACING = 120.dp

@StringRes
fun greetingForHour(hourOfDay: Int): Int = when (hourOfDay) {
    in 5..11 -> R.string.home_greeting_morning
    in 12..16 -> R.string.home_greeting_afternoon
    else -> R.string.home_greeting_evening
}

@Composable
fun HomeScreen(
    greeting: String,
    userName: String,
    teamName: String,
    recentCarePlans: List<CarePlanSummary>,
    onStartCarePlanClick: () -> Unit,
    onMenuClick: () -> Unit,
    onCarePlanClick: (CarePlanSummary) -> Unit,
    onAllCarePlansClick: () -> Unit,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // The sheet is stretched to the bottom of the screen even when it has
        // little content, so measure the section above it to know how tall that is.
        val screenHeight = maxHeight
        var topSectionHeightPx by remember { mutableIntStateOf(0) }
        val topSectionHeight = with(LocalDensity.current) { topSectionHeightPx.toDp() }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier
                    .onSizeChanged { topSectionHeightPx = it.height }
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                HomeHeader(userName = userName, teamName = teamName, onMenuClick = onMenuClick)

                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Spacer(modifier = Modifier.height(SECTION_SPACING))

                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(SECTION_SPACING))

                    Button(
                        onClick = onStartCarePlanClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(text = stringResource(R.string.home_action_start))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            RecentCarePlans(
                carePlans = recentCarePlans,
                onCarePlanClick = onCarePlanClick,
                onAllCarePlansClick = onAllCarePlansClick,
                modifier = Modifier.heightIn(
                    min = (screenHeight - topSectionHeight).coerceAtLeast(0.dp),
                ),
            )
        }
    }
}

@Composable
private fun HomeHeader(userName: String, teamName: String, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Placeholder until users have profile photos
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )

        Spacer(modifier = Modifier.size(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            if (userName.isNotBlank()) {
                Text(text = userName, style = MaterialTheme.typography.bodyMedium)
            }
            if (teamName.isNotBlank()) {
                Text(
                    text = teamName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val menuLabel = stringResource(R.string.home_action_menu)
        Box(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(
                    onClickLabel = menuLabel,
                    role = Role.Button,
                    onClick = onMenuClick,
                )
                .semantics { contentDescription = menuLabel },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "≡",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun RecentCarePlans(
    carePlans: List<CarePlanSummary>,
    onCarePlanClick: (CarePlanSummary) -> Unit,
    onAllCarePlansClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            // Runs to the bottom edge of the screen; only its content clears the nav bar
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_recent_title),
            style = MaterialTheme.typography.bodySmall,
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (carePlans.isEmpty()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.home_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                carePlans.forEach { carePlan ->
                    CarePlanRow(carePlan = carePlan, onClick = { onCarePlanClick(carePlan) })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.home_action_all_care_plans),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(role = Role.Button, onClick = onAllCarePlansClick)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CarePlanRow(carePlan: CarePlanSummary, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(carePlanRowTestTag(carePlan.number))
            .clip(shape)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                carePlan.services.forEachIndexed { index, service ->
                    if (index > 0) ServiceSeparator()
                    Text(text = service, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            CarePlanMeta(carePlan)
        }

        Text(
            text = "›",
            fontSize = 24.sp,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

@Composable
private fun ServiceSeparator() {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun CarePlanMeta(carePlan: CarePlanSummary) {
    val metaColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val statusColor = when (carePlan.status) {
        CarePlanStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondary
        CarePlanStatus.SYNC_PENDING -> SyncPendingColor
        CarePlanStatus.SHARED -> metaColor
    }
    val statusLabel = stringResource(carePlan.status.labelRes)

    Text(
        text = buildAnnotatedString {
            append("${carePlan.number} · ${carePlan.timeLabel} · ")
            withStyle(SpanStyle(color = statusColor)) { append(statusLabel) }
        },
        style = MaterialTheme.typography.bodySmall,
        color = metaColor,
    )
}

private val previewCarePlans = listOf(
    CarePlanSummary("#0003", listOf("Shelter", "Food"), "12 min ago", CarePlanStatus.IN_PROGRESS),
    CarePlanSummary("#0002", listOf("Shelter", "Food"), "Yesterday", CarePlanStatus.SHARED),
    CarePlanSummary("#0001", listOf("Shelter", "Food", "Medical"), "Yesterday", CarePlanStatus.SYNC_PENDING),
)

@Preview(showBackground = true, name = "1. Empty")
@Composable
private fun HomeScreenEmptyPreview() {
    ResourceBinderTheme {
        HomeScreen(
            greeting = "Good morning!",
            userName = "Marcus Guevara",
            teamName = "RAMS Outreach Team",
            recentCarePlans = emptyList(),
            onStartCarePlanClick = {},
            onMenuClick = {},
            onCarePlanClick = {},
            onAllCarePlansClick = {},
        )
    }
}

@Preview(showBackground = true, name = "2. With care plans")
@Composable
private fun HomeScreenWithCarePlansPreview() {
    ResourceBinderTheme {
        HomeScreen(
            greeting = "Good morning!",
            userName = "Marcus Guevara",
            teamName = "RAMS Outreach Team",
            recentCarePlans = previewCarePlans,
            onStartCarePlanClick = {},
            onMenuClick = {},
            onCarePlanClick = {},
            onAllCarePlansClick = {},
        )
    }
}
