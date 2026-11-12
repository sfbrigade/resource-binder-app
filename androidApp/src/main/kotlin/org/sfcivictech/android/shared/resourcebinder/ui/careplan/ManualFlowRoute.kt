package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.careplan.Catalog
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowResult
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowState

//keeps the flow's state across rotation and process death: just the ids and chosen values,
//rebuilt through restore() so anything the catalog no longer has is dropped
fun ManualFlowStateSaver(catalog: Catalog): Saver<ManualFlowState, Any> = listSaver(
    save = { state ->
        listOf(
            state.selectedNeedIds,
            //flattened to key, value, key, value so it stays a plain list of strings
            state.values.flatMap { (detailId, value) -> listOf(detailId, value) },
            state.pinnedDetailIds,
        )
    },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val lists = saved as List<List<String>>
        ManualFlowState.restore(
            catalog = catalog,
            needIds = lists[0],
            values = lists[1].chunked(2).associate { (detailId, value) -> detailId to value },
            pinnedDetailIds = lists[2],
        )
    },
)

//owns the flow's state and the sheet and dialog around the screen; the routing out of the
//flow is decided by the caller: onBack returns to the previous screen, onExit leaves the
//care plan entirely (X, Discard, and Save and exit, which does not save anything yet)
@Composable
fun ManualFlowRoute(
    catalog: Catalog,
    onBack: () -> Unit,
    onExit: () -> Unit,
    onFindResources: (ManualFlowResult) -> Unit,
) {
    var state by rememberSaveable(stateSaver = ManualFlowStateSaver(catalog)) {
        mutableStateOf(ManualFlowState.initial(catalog))
    }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var showDiscard by rememberSaveable { mutableStateOf(false) }

    //leaving with work in progress asks first; with nothing entered it just leaves
    fun leave(destination: () -> Unit) {
        if (state.hasChanges) showDiscard = true else destination()
    }

    //the sheet and the dialog are windows of their own and take system back first
    BackHandler(enabled = !showSheet && !showDiscard) { leave(onBack) }

    ManualFlowScreen(
        state = state,
        onToggleNeed = { state = state.toggleNeed(it) },
        onSetDetail = { detailId, value -> state = state.setDetail(detailId, value) },
        onAddDetailClick = { showSheet = true },
        onFindResources = { onFindResources(state.result()) },
        onBack = { leave(onBack) },
        onClose = { leave(onExit) },
    )

    if (showSheet) {
        AddDetailsSheet(
            categories = state.sheetCategories,
            onDismiss = { showSheet = false },
            onApply = { added ->
                //a detail that is both pinned and linked by a selected need is locked in the
                //sheet, so it isn't in the sheet's result; keep it pinned so it moves to
                //additional filters if that need is deselected
                val keptPinned = state.pinnedDetailIds.filter { it in state.lockedDetailIds }
                state = state.setPinnedDetails(keptPinned + added)
                showSheet = false
            },
        )
    }

    if (showDiscard) {
        DiscardDialog(
            onDiscard = {
                showDiscard = false
                onExit()
            },
            onKeepEditing = { showDiscard = false },
            //nothing stores care plans yet, so this behaves like discard
            onSaveAndExit = {
                showDiscard = false
                onExit()
            },
        )
    }
}

@Composable
private fun DiscardDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit, onSaveAndExit: () -> Unit) {
    Dialog(onDismissRequest = onKeepEditing) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(20.dp),
        ) {
            Text(
                text = stringResource(R.string.discard_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.discard_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogAction(
                    text = stringResource(R.string.discard_action_discard),
                    color = MaterialTheme.colorScheme.error,
                    onClick = onDiscard,
                )
                DialogAction(
                    text = stringResource(R.string.discard_action_keep),
                    color = MaterialTheme.colorScheme.onBackground,
                    onClick = onKeepEditing,
                )
                DialogAction(
                    text = stringResource(R.string.discard_action_save),
                    color = MaterialTheme.colorScheme.onBackground,
                    onClick = onSaveAndExit,
                )
            }
        }
    }
}

@Composable
private fun DialogAction(text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
