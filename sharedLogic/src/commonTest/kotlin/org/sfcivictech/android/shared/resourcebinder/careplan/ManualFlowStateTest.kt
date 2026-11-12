package org.sfcivictech.android.shared.resourcebinder.careplan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ManualFlowStateTest {

    private val catalog = CarePlanCatalog.default

    private fun initial() = ManualFlowState.initial(catalog)

    private fun ManualFlowState.select(vararg needIds: String) =
        needIds.fold(this) { state, id -> state.toggleNeed(id) }

    private fun ManualFlowState.needCard(needId: String) = cards.first { it.needId == needId }

    private fun DetailCard.detailIds() = items.map { it.detailId }

    private fun ManualFlowState.allShownDetailIds() = cards.flatMap { it.detailIds() }

    @Test
    fun initialStateShowsOnlyTheUniversalCardAndCannotContinue() {
        val state = initial()

        assertEquals(1, state.cards.size)
        assertEquals(CardKind.UNIVERSAL, state.cards.single().kind)
        assertEquals(catalog.universalDetailIds, state.cards.single().detailIds())
        assertFalse(state.canContinue)
        assertEquals(0, state.needCount)
        assertEquals(0, state.detailCount)
        assertFalse(state.hasChanges)
    }

    @Test
    fun shelterCardListsTheSevenDetailsInOrder() {
        val state = initial().select("shelter")

        assertEquals(2, state.cards.size)
        assertEquals(CardKind.NEED, state.needCard("shelter").kind)
        assertEquals("Shelter", state.needCard("shelter").title)
        assertEquals(catalog.links["shelter"], state.needCard("shelter").detailIds())
        assertTrue(state.canContinue)
        assertEquals(1, state.needCount)
    }

    @Test
    fun mentalHealthCardIsFourDetails() {
        val state = initial().select("mental_health")

        assertEquals(
            listOf("insurance", "current_crisis", "substance_use", "primary_care"),
            state.needCard("mental_health").detailIds(),
        )
    }

    @Test
    fun noteExample() {
        val state = initial().select("shelter", "veterans", "benefits_income")

        assertTrue("housing_status" in state.needCard("shelter").detailIds())
        assertTrue("veteran_status" in state.needCard("veterans").detailIds())
        assertFalse("veteran_status" in state.needCard("shelter").detailIds())
        assertTrue("insurance" in state.needCard("benefits_income").detailIds())
        assertTrue("language" in state.cards.first { it.kind == CardKind.UNIVERSAL }.detailIds())
        val shown = state.allShownDetailIds()
        assertEquals(shown.distinct(), shown, "a detail is shown twice")
    }

    @Test
    fun needWhoseDetailsAreAllOwnedElsewhereStillGetsAnEmptyCard() {
        val tiny = Catalog(
            needs = listOf(Need("a", "A"), Need("b", "B")),
            details = listOf(
                Detail("u", "U", "C", listOf("o")),
                Detail("x", "X", "C", listOf("o")),
            ),
            links = mapOf("a" to listOf("x"), "b" to listOf("x")),
            priorities = mapOf("x" to listOf("b", "a")),
            universalDetailIds = listOf("u"),
        )

        val state = ManualFlowState.initial(tiny).toggleNeed("a").toggleNeed("b")

        val needCards = state.cards.filter { it.kind == CardKind.NEED }
        assertEquals(listOf("a", "b"), needCards.map { it.needId })
        assertTrue(needCards.first { it.needId == "a" }.items.isEmpty())
        assertEquals(0, needCards.first { it.needId == "a" }.chosenCount)
        assertEquals(listOf("x"), needCards.first { it.needId == "b" }.detailIds())
    }

    @Test
    fun everySelectedNeedGetsACardEvenWhenOtherNeedsOwnAllItsDetails() {
        val foodCovered = initial().select("food_supplies", "benefits_income", "transportation")
        assertTrue(foodCovered.needCard("food_supplies").items.isEmpty())

        val mentalHealthCovered = initial().select(
            "mental_health", "benefits_income", "crisis_support", "substance_use", "medical_dental",
        )
        assertTrue(mentalHealthCovered.needCard("mental_health").items.isEmpty())
    }

    @Test
    fun cardsFollowCatalogOrderNotSelectionOrder() {
        val state = initial().select("mental_health", "shelter")

        assertEquals(
            listOf("shelter", "mental_health"),
            state.cards.filter { it.kind == CardKind.NEED }.map { it.needId },
        )
    }

    @Test
    fun toggleNeedIgnoresUnknownId() {
        val state = initial()

        assertEquals(state, state.toggleNeed("ghost"))
    }

    @Test
    fun setDetailCountsAndShowsChosenCount() {
        val state = initial().setDetail("location_area", "San Francisco")

        val universal = state.cards.single()
        assertEquals(1, universal.chosenCount)
        assertEquals("San Francisco", universal.items.first { it.detailId == "location_area" }.value)
        assertEquals(1, state.detailCount)
    }

    @Test
    fun setDetailNullClears() {
        val state = initial().setDetail("age", "18-25").setDetail("age", null)

        assertNull(state.cards.single().items.first { it.detailId == "age" }.value)
        assertEquals(0, state.detailCount)
    }

    @Test
    fun setDetailRejectsOptionNotInList() {
        val state = initial()

        assertEquals(state, state.setDetail("age", "not an age"))
    }

    @Test
    fun setDetailIgnoresUnknownDetail() {
        val state = initial()

        assertEquals(state, state.setDetail("ghost", "x"))
    }

    @Test
    fun togglingANeedOnThenOffIsNotAChange() {
        val state = initial().select("shelter", "shelter")

        assertFalse(state.hasChanges)
    }

    @Test
    fun chosenValueMakesHasChanges() {
        assertTrue(initial().setDetail("language", "Spanish").hasChanges)
    }

    @Test
    fun deselectedNeedValuesAreHiddenUncountedAndOutOfResultButRestoredOnReselect() {
        val chosen = initial().select("shelter").setDetail("housing_status", "Unsheltered")
        assertEquals(1, chosen.detailCount)
        assertEquals(mapOf("housing_status" to "Unsheltered"), chosen.result().values)

        val deselected = chosen.toggleNeed("shelter")
        assertEquals(0, deselected.detailCount)
        assertTrue(deselected.result().values.isEmpty())
        assertFalse(deselected.hasChanges)

        val reselected = deselected.toggleNeed("shelter")
        assertEquals(
            "Unsheltered",
            reselected.needCard("shelter").items.first { it.detailId == "housing_status" }.value,
        )
        assertEquals(1, reselected.detailCount)
    }

    @Test
    fun pinnedUnlinkedDetailGoesToAdditionalFilters() {
        val state = initial().setPinnedDetails(listOf("documents_needed"))

        assertEquals(listOf(CardKind.UNIVERSAL, CardKind.ADDITIONAL), state.cards.map { it.kind })
        assertEquals(listOf("documents_needed"), state.cards.last().detailIds())
        assertTrue(state.hasChanges)
    }

    @Test
    fun pinnedDetailLinkedBySelectedNeedShowsOnceInNeedCard() {
        val state = initial().select("documents_id").setPinnedDetails(listOf("documents_needed"))

        assertTrue(state.cards.none { it.kind == CardKind.ADDITIONAL })
        assertTrue("documents_needed" in state.needCard("documents_id").detailIds())
        assertEquals(1, state.allShownDetailIds().count { it == "documents_needed" })
    }

    @Test
    fun pinnedDetailMovesToAdditionalWhenItsNeedIsDeselected() {
        val state = initial()
            .select("documents_id")
            .setPinnedDetails(listOf("documents_needed"))
            .toggleNeed("documents_id")

        assertEquals(listOf("documents_needed"), state.cards.single { it.kind == CardKind.ADDITIONAL }.detailIds())
    }

    @Test
    fun sheetMarksUniversalAndSelectedNeedDetailsLockedAndPinnedOnesPinned() {
        val state = initial().select("shelter").setPinnedDetails(listOf("documents_needed"))
        val sheet = state.sheetCategories.flatMap { it.details }.associateBy { it.detailId }

        assertTrue(sheet.getValue("age").locked)
        assertTrue(sheet.getValue("housing_status").locked)
        assertTrue(sheet.getValue("documents_needed").pinned)
        assertFalse(sheet.getValue("documents_needed").locked)
        assertFalse(sheet.getValue("snap").locked)
        assertFalse(sheet.getValue("snap").pinned)
    }

    @Test
    fun sheetCategoriesFollowCatalogCategoryOrder() {
        val categories = initial().sheetCategories

        assertEquals(catalog.categories, categories.map { it.label })
        assertEquals(catalog.details.size, categories.sumOf { it.details.size })
    }

    @Test
    fun restoreDropsUnknownIdsAndInvalidOptions() {
        val state = ManualFlowState.restore(
            catalog = catalog,
            needIds = listOf("shelter", "ghost_need"),
            values = mapOf("age" to "18-25", "ghost_detail" to "x", "language" to "Klingon"),
            pinnedDetailIds = listOf("documents_needed", "ghost_detail"),
        )

        assertEquals(listOf("shelter"), state.selectedNeedIds)
        assertEquals(mapOf("age" to "18-25"), state.values)
        assertEquals(listOf("documents_needed"), state.pinnedDetailIds)
    }

    @Test
    fun resultListsSelectedNeedsInCatalogOrderAndVisibleValues() {
        val result = initial()
            .select("mental_health", "shelter")
            .setDetail("age", "18-25")
            .setDetail("housing_status", "Unsheltered")
            .result()

        assertEquals(listOf("shelter", "mental_health"), result.needIds)
        assertEquals(mapOf("age" to "18-25", "housing_status" to "Unsheltered"), result.values)
    }
}
