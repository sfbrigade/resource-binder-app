package org.sfcivictech.android.shared.resourcebinder.careplan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CarePlanCatalogTest {

    private val catalog = CarePlanCatalog.default

    @Test
    fun defaultCatalogBuilds() {
        assertEquals(14, catalog.needs.size)
    }

    @Test
    fun shelterLinksTheSevenDetailsInDesignOrder() {
        assertEquals(
            listOf(
                "housing_status",
                "sleeping_location",
                "couples",
                "pets",
                "veteran_status",
                "gender",
                "has_id",
            ),
            catalog.links["shelter"],
        )
    }

    @Test
    fun mentalHealthLinksFour() {
        assertEquals(
            listOf("insurance", "current_crisis", "substance_use", "primary_care"),
            catalog.links["mental_health"],
        )
    }

    @Test
    fun universalIsLocationAgeLanguageAccessibility() {
        assertEquals(
            listOf("location_area", "age", "language", "accessibility"),
            catalog.universalDetailIds,
        )
    }

    @Test
    fun locationOptionsAreTheEightBayAreaCounties() {
        assertEquals(
            listOf(
                "San Francisco",
                "Alameda (Oakland)",
                "Contra Costa",
                "Marin",
                "San Mateo",
                "Santa Clara",
                "Solano",
                "Sonoma",
            ),
            catalog.detailOrNull("location_area")?.options,
        )
    }

    @Test
    fun needAndDetailNamespacesAreIndependent() {
        assertNotNull(catalog.needOrNull("substance_use"))
        assertNotNull(catalog.detailOrNull("substance_use"))
    }

    @Test
    fun everyNeedLinksAtLeastOneDetail() {
        catalog.needs.forEach { need ->
            assertTrue(catalog.links[need.id].orEmpty().isNotEmpty(), "${need.id} links nothing")
        }
    }

    @Test
    fun everyCategoryOfTheSheetExists() {
        assertEquals(
            listOf(
                "Demographics",
                "Location",
                "Housing",
                "Health",
                "Income & Benefits",
                "Documents / ID",
                "Family",
            ),
            catalog.categories,
        )
    }

    @Test
    fun sharedDetailsAreOwnedAsTheDesignNoteDescribes() {
        assertEquals(listOf("shelter", "transportation"), catalog.priorities["housing_status"])
        assertEquals(listOf("veterans", "shelter"), catalog.priorities["veteran_status"])
        assertEquals(listOf("benefits_income", "mental_health"), catalog.priorities["insurance"]?.take(2))
    }
}
