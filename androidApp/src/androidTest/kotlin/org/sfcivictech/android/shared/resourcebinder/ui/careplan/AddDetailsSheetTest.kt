package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.careplan.CarePlanCatalog
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowState
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class AddDetailsSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val initial = ManualFlowState.initial(CarePlanCatalog.default)

    private fun setSheet(
        state: ManualFlowState = initial,
        onDismiss: () -> Unit = {},
        onApply: (List<String>) -> Unit = {},
    ) {
        composeTestRule.setContent {
            ResourceBinderTheme {
                AddDetailsSheet(
                    categories = state.sheetCategories,
                    onDismiss = onDismiss,
                    onApply = onApply,
                )
            }
        }
    }

    private fun tapChip(detailId: String) {
        composeTestRule.onNodeWithTag(sheetChipTestTag(detailId)).performScrollTo().performClick()
    }

    @Test
    fun showsEveryCategoryHeading() {
        setSheet()

        CarePlanCatalog.default.categories.forEach { category ->
            composeTestRule.onNodeWithText(category).assertExists()
        }
    }

    @Test
    fun lockedChipIgnoresTaps() {
        setSheet()

        //age is on the universal card, so it is locked
        tapChip("age")

        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun applyDisabledUntilSelectionChanges() {
        setSheet()
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsNotEnabled()

        tapChip("snap")

        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsEnabled()
    }

    @Test
    fun applyReturnsOnlyPinnedIds() {
        var applied: List<String>? = null
        setSheet(
            state = initial.setPinnedDetails(listOf("documents_needed")),
            onApply = { applied = it },
        )

        tapChip("snap")
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).performClick()

        //age and the other locked chips are not in the result; order follows the sheet
        assertEquals(listOf("snap", "documents_needed"), applied)
    }

    @Test
    fun tappingPinnedChipRemovesItAndEnablesApply() {
        var applied: List<String>? = null
        setSheet(
            state = initial.setPinnedDetails(listOf("documents_needed")),
            onApply = { applied = it },
        )
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsNotEnabled()

        tapChip("documents_needed")
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsEnabled().performClick()

        assertEquals(emptyList<String>(), applied)
    }

    @Test
    fun closeButtonCallsOnDismiss() {
        var count = 0
        setSheet(onDismiss = { count++ })

        composeTestRule.onNodeWithTag(SHEET_CLOSE_TEST_TAG).performClick()

        assertEquals(1, count)
    }

    @Test
    fun systemBackCallsOnDismiss() {
        var count = 0
        setSheet(onDismiss = { count++ })

        Espresso.pressBack()

        assertEquals(1, count)
    }

    @Test
    fun draftSurvivesRecreation() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            ResourceBinderTheme {
                AddDetailsSheet(categories = initial.sheetCategories, onDismiss = {}, onApply = {})
            }
        }
        tapChip("snap")
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsEnabled()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertIsEnabled()
    }
}
