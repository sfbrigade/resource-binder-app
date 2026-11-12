package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.careplan.SheetCategory
import org.sfcivictech.android.shared.resourcebinder.careplan.SheetDetail
import org.sfcivictech.android.shared.resourcebinder.ui.common.CloseButton

const val SHEET_APPLY_TEST_TAG = "sheet_apply"
const val SHEET_CLOSE_TEST_TAG = "sheet_close"

fun sheetChipTestTag(detailId: String) = "sheet_chip_$detailId"

//lets the user pick extra details to show; reports only the ones added from the sheet
//(pinned), since locked chips are already on screen for another reason
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddDetailsSheet(
    categories: List<SheetCategory>,
    onDismiss: () -> Unit,
    onApply: (pinnedDetailIds: List<String>) -> Unit,
) {
    val allDetails = categories.flatMap { it.details }
    val initiallyPinned = remember { allDetails.filter { it.pinned }.map { it.detailId }.toSet() }
    //kept as a list so it can be saved; the picks survive rotation along with the sheet
    var draftIds by rememberSaveable { mutableStateOf(initiallyPinned.toList()) }
    val draft = draftIds.toSet()
    val canApply = draft != initiallyPinned

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CloseButton(
                    contentDescription = stringResource(R.string.add_details_close),
                    onClick = onDismiss,
                    modifier = Modifier.testTag(SHEET_CLOSE_TEST_TAG),
                )

                Text(
                    text = stringResource(R.string.add_details_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )

                ApplyButton(
                    enabled = canApply,
                    //sheet order, so the result doesn't depend on the order of the taps
                    onClick = { onApply(allDetails.map { it.detailId }.filter { it in draft }) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                categories.forEach { category ->
                    Column {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            category.details.forEach { detail ->
                                val selected = detail.locked || detail.detailId in draft
                                DetailChip(
                                    detail = detail,
                                    selected = selected,
                                    onToggle = {
                                        draftIds = if (detail.detailId in draft) draftIds - detail.detailId else draftIds + detail.detailId
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun DetailChip(detail: SheetDetail, selected: Boolean, onToggle: () -> Unit) {
    val container = when {
        //solid rather than faded, so the white label keeps enough contrast
        detail.locked -> MaterialTheme.colorScheme.onSurfaceVariant
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Text(
        text = detail.label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .testTag(sheetChipTestTag(detail.detailId))
            .clip(RoundedCornerShape(50))
            .background(container)
            //locked chips are already on screen for another reason, so they can't be turned off
            .toggleable(
                value = selected,
                enabled = !detail.locked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun ApplyButton(enabled: Boolean, onClick: () -> Unit) {
    val label = stringResource(R.string.add_details_apply)
    Box(
        modifier = Modifier
            .testTag(SHEET_APPLY_TEST_TAG)
            .minimumInteractiveComponentSize()
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable(enabled = enabled, onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "✓",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}
