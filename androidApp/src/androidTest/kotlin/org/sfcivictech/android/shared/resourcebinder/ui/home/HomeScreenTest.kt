package org.sfcivictech.android.shared.resourcebinder.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.sfcivictech.android.shared.resourcebinder.R
import org.sfcivictech.android.shared.resourcebinder.ui.theme.ResourceBinderTheme

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val carePlans = listOf(
        CarePlanSummary("#0002", listOf("Shelter", "Food"), "Yesterday", CarePlanStatus.SHARED),
        CarePlanSummary("#0001", listOf("Shelter", "Medical"), "Yesterday", CarePlanStatus.SYNC_PENDING),
    )

    private fun string(resId: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)

    private fun setHome(
        carePlans: List<CarePlanSummary>,
        onStart: () -> Unit = {},
        onMenu: () -> Unit = {},
        onCarePlan: (CarePlanSummary) -> Unit = {},
        onAll: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ResourceBinderTheme {
                HomeScreen(
                    greeting = "Good morning!",
                    userName = "Marcus Guevara",
                    teamName = "RAMS Outreach Team",
                    recentCarePlans = carePlans,
                    onStartCarePlanClick = onStart,
                    onMenuClick = onMenu,
                    onCarePlanClick = onCarePlan,
                    onAllCarePlansClick = onAll,
                )
            }
        }
    }

    @Test
    fun showsUserAndGreeting() {
        setHome(emptyList())

        composeTestRule.onNodeWithText("Marcus Guevara").assertExists()
        composeTestRule.onNodeWithText("RAMS Outreach Team").assertExists()
        composeTestRule.onNodeWithText("Good morning!").assertExists()
        composeTestRule.onNodeWithText(string(R.string.home_title)).assertExists()
    }

    @Test
    fun emptyStateShowsTheFirstCarePlanMessage() {
        setHome(emptyList())

        composeTestRule.onNodeWithText(string(R.string.home_recent_empty)).assertExists()
        composeTestRule.onNodeWithText(string(R.string.home_action_all_care_plans)).assertDoesNotExist()
    }

    @Test
    fun populatedStateShowsCarePlansAndHidesTheEmptyMessage() {
        setHome(carePlans)

        composeTestRule.onNodeWithText(string(R.string.home_recent_empty)).assertDoesNotExist()
        composeTestRule.onNodeWithTag(carePlanRowTestTag("#0001")).assertExists()
        composeTestRule.onNodeWithText(string(R.string.home_action_all_care_plans)).assertExists()
    }

    @Test
    fun startButtonInvokesTheCallback() {
        var started = false
        setHome(emptyList(), onStart = { started = true })

        composeTestRule.onNodeWithText(string(R.string.home_action_start)).performClick()

        assertTrue(started)
    }

    @Test
    fun menuButtonHasAnAccessibleNameAndInvokesTheCallback() {
        var opened = false
        setHome(emptyList(), onMenu = { opened = true })

        composeTestRule.onNodeWithContentDescription(string(R.string.home_action_menu)).performClick()

        assertTrue(opened)
    }

    @Test
    fun clickingACarePlanPassesItToTheCallback() {
        var clicked: CarePlanSummary? = null
        setHome(carePlans, onCarePlan = { clicked = it })

        composeTestRule.onNodeWithTag(carePlanRowTestTag("#0001")).performClick()

        assertEquals("#0001", clicked?.number)
    }

    @Test
    fun allCarePlansInvokesTheCallback() {
        var opened = false
        setHome(carePlans, onAll = { opened = true })

        composeTestRule.onNodeWithText(string(R.string.home_action_all_care_plans)).performClick()

        assertTrue(opened)
    }

    @Test
    fun greetingChangesAtTheMorningAfternoonAndEveningBoundaries() {
        assertEquals(R.string.home_greeting_evening, greetingForHour(4))
        assertEquals(R.string.home_greeting_morning, greetingForHour(5))
        assertEquals(R.string.home_greeting_morning, greetingForHour(11))
        assertEquals(R.string.home_greeting_afternoon, greetingForHour(12))
        assertEquals(R.string.home_greeting_afternoon, greetingForHour(16))
        assertEquals(R.string.home_greeting_evening, greetingForHour(17))
        assertEquals(R.string.home_greeting_evening, greetingForHour(0))
    }
}
