package org.sfcivictech.android.shared.resourcebinder.ui.careplan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.careplan.CarePlanCatalog
import org.sfcivictech.android.shared.resourcebinder.careplan.ManualFlowState
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class ManualFlowScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun setScreen(
        onFindResources: () -> Unit = {},
        onAddDetailClick: () -> Unit = {},
        onBack: () -> Unit = {},
        onClose: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ResourceBinderTheme {
                var state by remember { mutableStateOf(ManualFlowState.initial(CarePlanCatalog.default)) }
                ManualFlowScreen(
                    state = state,
                    onToggleNeed = { state = state.toggleNeed(it) },
                    onSetDetail = { detailId, value -> state = state.setDetail(detailId, value) },
                    onAddDetailClick = onAddDetailClick,
                    onFindResources = onFindResources,
                    onBack = onBack,
                    onClose = onClose,
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

    @Test
    fun startsWithHelperTextAndDisabledFindResources() {
        setScreen()

        composeTestRule.onNodeWithText(string(R.string.manual_helper_select_need)).assertExists()
        composeTestRule.onNodeWithTag(FIND_RESOURCES_TEST_TAG).assertIsNotEnabled()
    }

    @Test
    fun selectingNeedAddsItsCardAndEnablesFindResources() {
        setScreen()

        selectNeed("shelter")

        composeTestRule.onNodeWithTag(detailPillTestTag("housing_status")).assertExists()
        composeTestRule.onNodeWithTag(FIND_RESOURCES_TEST_TAG).assertIsEnabled()
        composeTestRule.onNodeWithText(string(R.string.manual_helper_select_need)).assertDoesNotExist()
    }

    @Test
    fun shelterCardShowsSevenPillsAlways() {
        setScreen()

        selectNeed("shelter")

        listOf(
            "housing_status", "sleeping_location", "couples", "pets", "veteran_status", "gender", "has_id",
        ).forEach { composeTestRule.onNodeWithTag(detailPillTestTag(it)).assertExists() }
    }

    @Test
    fun chosenValueReplacesPillLabelAndShowsCount() {
        setScreen()

        chooseOption("location_area", "San Francisco")

        composeTestRule.onNodeWithTag(detailPillTestTag("location_area")).assertTextContains("San Francisco")
        composeTestRule.onNodeWithText("1 of 4").assertExists()
    }

    @Test
    fun tappingCheckedOptionClearsIt() {
        setScreen()
        chooseOption("location_area", "San Francisco")

        composeTestRule.onNodeWithTag(detailPillTestTag("location_area")).performScrollTo().performClick()
        composeTestRule.onNodeWithTag(detailOptionTestTag("location_area", "San Francisco")).performClick()

        composeTestRule.onNodeWithTag(detailPillTestTag("location_area")).assertTextContains("Location / Area")
        composeTestRule.onNodeWithText("1 of 4").assertDoesNotExist()
    }

    @Test
    fun zeroChosenHidesCount() {
        setScreen()

        selectNeed("shelter")

        composeTestRule.onNodeWithText("0 of 4").assertDoesNotExist()
        composeTestRule.onNodeWithText("0 of 7").assertDoesNotExist()
    }

    @Test
    fun footerWording() {
        setScreen()

        selectNeed("shelter")
        composeTestRule.onNodeWithText("1 need · No details added").assertExists()

        selectNeed("mental_health")
        composeTestRule.onNodeWithText("2 needs selected · No details added").assertExists()

        chooseOption("location_area", "San Francisco")
        composeTestRule.onNodeWithText("2 needs selected · 1 detail added").assertExists()

        chooseOption("age", "18-25")
        composeTestRule.onNodeWithText("2 needs selected · 2 details added").assertExists()
    }

    @Test
    fun findResourcesInvokesCallbackWhenEnabled() {
        var count = 0
        setScreen(onFindResources = { count++ })
        selectNeed("shelter")

        composeTestRule.onNodeWithTag(FIND_RESOURCES_TEST_TAG).performClick()

        assertEquals(1, count)
    }

    @Test
    fun backAndCloseInvokeCallbacks() {
        var backCount = 0
        var closeCount = 0
        setScreen(onBack = { backCount++ }, onClose = { closeCount++ })

        composeTestRule.onNodeWithTag(MANUAL_BACK_TEST_TAG).performClick()
        composeTestRule.onNodeWithTag(MANUAL_CLOSE_TEST_TAG).performClick()

        assertEquals(1, backCount)
        assertEquals(1, closeCount)
    }

    @Test
    fun addAnotherDetailInvokesCallback() {
        var count = 0
        setScreen(onAddDetailClick = { count++ })

        composeTestRule.onNodeWithText(string(R.string.manual_action_add_detail)).performScrollTo().performClick()

        assertEquals(1, count)
    }

    @Test
    fun needWhoseDetailsAreAllOwnedElsewhereSaysCoveredAbove() {
        setScreen()

        selectNeed("food_supplies")
        selectNeed("benefits_income")
        selectNeed("transportation")

        composeTestRule.onNodeWithText(string(R.string.manual_card_covered)).assertExists()
    }

    @Test
    fun pillWithAValueAnnouncesLabelAndValue() {
        setScreen()

        chooseOption("location_area", "San Francisco")

        composeTestRule.onNodeWithTag(detailPillTestTag("location_area"))
            .assertContentDescriptionEquals("Location / Area: San Francisco")
    }
}
