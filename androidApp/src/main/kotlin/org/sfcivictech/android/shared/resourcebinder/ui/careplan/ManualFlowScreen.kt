package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.careplan.CarePlanCatalog
import org.sfcivictech.android.shared.resourcebinder.careplan.CardKind
import org.sfcivictech.android.shared.resourcebinder.careplan.DetailCard
import org.sfcivictech.android.shared.resourcebinder.careplan.DetailItem
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowState
import org.sfcivictech.android.shared.resourcebinder.ui.common.BackButton
import org.sfcivictech.android.shared.resourcebinder.ui.common.CloseButton
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

const val FIND_RESOURCES_TEST_TAG = "find_resources"
const val MANUAL_BACK_TEST_TAG = "manual_back"
const val MANUAL_CLOSE_TEST_TAG = "manual_close"

fun needChipTestTag(needId: String) = "need_chip_$needId"

fun detailPillTestTag(detailId: String) = "detail_pill_$detailId"

fun detailOptionTestTag(detailId: String, option: String) = "detail_option_${detailId}_$option"

//renders the state it is given and reports taps; all the rules live in ManualFlowState
@Composable
fun ManualFlowScreen(
    state: ManualFlowState,
    onToggleNeed: (needId: String) -> Unit,
    onSetDetail: (detailId: String, value: String?) -> Unit,
    onAddDetailClick: () -> Unit,
    onFindResources: () -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        ManualFlowHeader(onBack = onBack, onClose = onClose)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.manual_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            NeedChips(state = state, onToggleNeed = onToggleNeed)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.manual_details_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.cards.forEach { card -> DetailCardView(card = card, onSetDetail = onSetDetail) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onAddDetailClick)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                //decorative, so screen readers don't read the plus aloud
                Text(
                    text = "+",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.clearAndSetSemantics {},
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.manual_action_add_detail),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        ManualFlowBottomBar(state = state, onFindResources = onFindResources)
    }
}

@Composable
private fun ManualFlowHeader(onBack: () -> Unit, onClose: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onClick = onBack, modifier = Modifier.testTag(MANUAL_BACK_TEST_TAG))

            Spacer(modifier = Modifier.weight(1f))

            CloseButton(
                contentDescription = stringResource(R.string.manual_action_close),
                onClick = onClose,
                modifier = Modifier.testTag(MANUAL_CLOSE_TEST_TAG),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.manual_step_counter),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.manual_step_title),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NeedChips(state: ManualFlowState, onToggleNeed: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.catalog.needs.forEach { need ->
            val selected = state.isSelected(need.id)
            Text(
                text = need.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .testTag(needChipTestTag(need.id))
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .toggleable(
                        value = selected,
                        role = Role.Checkbox,
                        onValueChange = { onToggleNeed(need.id) },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailCardView(card: DetailCard, onSetDetail: (String, String?) -> Unit) {
    val title = when (card.kind) {
        CardKind.UNIVERSAL -> stringResource(R.string.manual_card_universal)
        CardKind.NEED -> card.title
        CardKind.ADDITIONAL -> stringResource(R.string.manual_card_additional)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            //the count appears once something is chosen, and never on additional filters
            if (card.kind != CardKind.ADDITIONAL && card.chosenCount > 0) {
                Text(
                    text = stringResource(R.string.manual_card_count, card.chosenCount, card.items.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (card.items.isEmpty()) {
            //the other selected needs show all of this need's details
            Text(
                text = stringResource(R.string.manual_card_covered),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                card.items.forEach { item -> DetailPill(item = item, onSetDetail = onSetDetail) }
            }
        }
    }
}

@Composable
private fun DetailPill(item: DetailItem, onSetDetail: (String, String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val chosen = item.value != null
    //a chosen value alone ("Yes", "Receiving") means nothing to a screen reader
    val description = item.value?.let { stringResource(R.string.manual_pill_description, item.label, it) }

    Box {
        Row(
            modifier = Modifier
                .testTag(detailPillTestTag(item.detailId))
                .clip(shape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(role = Role.Button) { expanded = true }
                .semantics { description?.let { contentDescription = it } }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = item.value ?: item.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (chosen) FontWeight.Medium else FontWeight.Normal,
                color = if (chosen) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "⌄",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            item.options.forEach { option ->
                val isChosen = option == item.value
                DropdownMenuItem(
                    text = { Text(option) },
                    //a fixed-width slot keeps the labels aligned whether or not there is a check
                    leadingIcon = {
                        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                            if (isChosen) Text("✓", style = MaterialTheme.typography.labelMedium)
                        }
                    },
                    //tapping the checked option clears it
                    onClick = {
                        onSetDetail(item.detailId, if (isChosen) null else option)
                        expanded = false
                    },
                    modifier = Modifier.testTag(detailOptionTestTag(item.detailId, option)),
                )
            }
        }
    }
}

@Composable
private fun ManualFlowBottomBar(state: ManualFlowState, onFindResources: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val summary = if (!state.canContinue) {
            stringResource(R.string.manual_helper_select_need)
        } else {
            val needs = pluralStringResource(R.plurals.manual_footer_needs, state.needCount, state.needCount)
            val details = if (state.detailCount == 0) {
                stringResource(R.string.manual_footer_no_details)
            } else {
                pluralStringResource(R.plurals.manual_footer_details, state.detailCount, state.detailCount)
            }
            "$needs · $details"
        }

        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onFindResources,
            enabled = state.canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag(FIND_RESOURCES_TEST_TAG),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        ) {
            Text(text = stringResource(R.string.manual_action_find))
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun ManualFlowScreenPreview() {
    ResourceBinderTheme {
        val state = ManualFlowState.initial(CarePlanCatalog.default)
            .toggleNeed("shelter")
            .toggleNeed("mental_health")
            .setDetail("location_area", "San Francisco")
            .setDetail("housing_status", "Unsheltered")
        ManualFlowScreen(
            state = state,
            onToggleNeed = {},
            onSetDetail = { _, _ -> },
            onAddDetailClick = {},
            onFindResources = {},
            onBack = {},
            onClose = {},
        )
    }
}
