package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.careplan.CarePlanCatalog
import org.sfcivictech.android.shared.resourcebinder.careplan.Catalog
import org.sfcivictech.android.shared.resourcebinder.careplan.Detail
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowResult
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowState
import org.sfcivictech.android.shared.resourcebinder.careplan.Need
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class ManualFlowRouteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var backCount = 0
    private var exitCount = 0
    private var found: ManualFlowResult? = null

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun setRoute() {
        composeTestRule.setContent {
            ResourceBinderTheme {
                ManualFlowRoute(
                    catalog = CarePlanCatalog.default,
                    onBack = { backCount++ },
                    onExit = { exitCount++ },
                    onFindResources = { found = it },
                )
            }
        }
    }

    private fun selectNeed(needId: String) {
        composeTestRule.onNodeWithTag(needChipTestTag(needId)).performScrollTo().performClick()
    }

    private fun chooseOption(detailId: String, option: String) {
        composeTestRule.onNodeWithTag(detailPillTestTag(detailId)).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(detailOptionTestTag(detailId, option)).performClick()
    }

    private fun openSheet() {
        composeTestRule.onNodeWithText(string(R.string.manual_action_add_detail)).performScrollTo().performClick()
    }

    private fun assertDiscardDialogShown() {
        composeTestRule.onNodeWithText(string(R.string.discard_title)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.discard_body)).assertExists()
    }

    @Test
    fun closeWithoutChangesExitsHome() {
        setRoute()

        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        assertEquals(1, exitCount)
        assertEquals(0, backCount)
    }

    @Test
    fun backArrowWithoutChangesGoesBack() {
        setRoute()

        composeTestRule.onNodeWithTag(MANUAL_BACK_TEST_TAG).performClick()

        assertEquals(1, backCount)
        assertEquals(0, exitCount)
    }

    @Test
    fun systemBackWithoutChangesGoesBack() {
        setRoute()

        Espresso.pressBack()

        assertEquals(1, backCount)
        assertEquals(0, exitCount)
    }

    @Test
    fun closeWithChangesShowsDialogWithBodyText() {
        setRoute()
        selectNeed("shelter")

        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        assertDiscardDialogShown()
        assertEquals(0, exitCount)
    }

    @Test
    fun backWithChangesShowsDialog() {
        setRoute()
        selectNeed("shelter")

        composeTestRule.onNodeWithTag(MANUAL_BACK_TEST_TAG).performClick()

        assertDiscardDialogShown()
        assertEquals(0, backCount)
        assertEquals(0, exitCount)
    }

    @Test
    fun keepEditingDismissesDialogAndKeepsState() {
        setRoute()
        selectNeed("shelter")
        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.discard_action_keep)).performClick()

        composeTestRule.onNodeWithText(string(R.string.discard_title)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(detailPillTestTag("housing_status")).assertExists()
        assertEquals(0, exitCount)
    }

    @Test
    fun discardExits() {
        setRoute()
        selectNeed("shelter")
        composeTestRule.onNodeWithTag(MANUAL_BACK_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.discard_action_discard)).performClick()

        assertEquals(1, exitCount)
        assertEquals(0, backCount)
    }

    @Test
    fun saveAndExitExits() {
        setRoute()
        selectNeed("shelter")
        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.discard_action_save)).performClick()

        assertEquals(1, exitCount)
        assertEquals(0, backCount)
    }

    @Test
    fun toggleOnThenOffThenCloseExitsWithoutDialog() {
        setRoute()
        selectNeed("shelter")
        selectNeed("shelter")

        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        composeTestRule.onNodeWithText(string(R.string.discard_title)).assertDoesNotExist()
        assertEquals(1, exitCount)
    }

    @Test
    fun systemBackWithSheetOpenClosesOnlySheet() {
        setRoute()
        openSheet()
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertExists()

        Espresso.pressBack()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithText(string(R.string.discard_title)).assertDoesNotExist()
        assertEquals(0, backCount)
        assertEquals(0, exitCount)
    }

    @Test
    fun applyingSheetPinsDetailIntoAdditionalFilters() {
        setRoute()
        openSheet()

        composeTestRule.onNodeWithTag(sheetChipTestTag("documents_needed")).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(SHEET_APPLY_TEST_TAG).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(string(R.string.manual_card_additional)).assertExists()
        composeTestRule.onNodeWithTag(detailPillTestTag("documents_needed")).assertExists()
    }

    @Test
    fun findResourcesPassesSelectedNeedsAndValues() {
        setRoute()
        selectNeed("shelter")
        chooseOption("housing_status", "Unsheltered")

        composeTestRule.onNodeWithTag(FIND_RESOURCES_TEST_TAG).performClick()

        assertEquals(listOf("shelter"), found?.needIds)
        assertEquals(mapOf("housing_status" to "Unsheltered"), found?.values)
    }

    @Test
    fun stateSurvivesRecreation() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            ResourceBinderTheme {
                ManualFlowRoute(
                    catalog = CarePlanCatalog.default,
                    onBack = {},
                    onExit = {},
                    onFindResources = {},
                )
            }
        }
        selectNeed("shelter")
        chooseOption("housing_status", "Unsheltered")

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithTag(detailPillTestTag("housing_status")).assertTextContains("Unsheltered")
    }

    @Test
    fun saverDropsIdsMissingFromCatalog() {
        val saved = with(ManualFlowStateSaver(CarePlanCatalog.default)) {
            SaverScope { true }.save(
                ManualFlowState.initial(CarePlanCatalog.default)
                    .toggleNeed("shelter")
                    .setDetail("age", "18-25"),
            )
        }
        assertNotNull(saved)

        //a catalog that no longer has "shelter" or "age"
        val smaller = Catalog(
            needs = listOf(Need("n1", "One")),
            details = listOf(Detail("d1", "D1", "C", listOf("x"))),
            links = mapOf("n1" to listOf("d1")),
            priorities = mapOf("d1" to listOf("n1")),
            universalDetailIds = emptyList(),
        )
        val restored = ManualFlowStateSaver(smaller).restore(saved!!)

        assertTrue(restored!!.selectedNeedIds.isEmpty())
        assertTrue(restored.values.isEmpty())
    }
}
