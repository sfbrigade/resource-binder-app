package org.sfcivictech.android.shared.resourcebinder.careplan

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CatalogTest {

    private val needOne = Need("n1", "One")
    private val needTwo = Need("n2", "Two")
    private val universal = Detail("d_universal", "Universal", "A", listOf("x"))
    private val shared = Detail("d_shared", "Shared", "A", listOf("x"))
    private val onlyOne = Detail("d_one", "Only one", "B", listOf("x"))
    private val onlyTwo = Detail("d_two", "Only two", "B", listOf("x"))

    // a small valid catalog; each test overrides the one table it breaks
    private fun catalog(
        needs: List<Need> = listOf(needOne, needTwo),
        details: List<Detail> = listOf(universal, shared, onlyOne, onlyTwo),
        links: Map<String, List<String>> = mapOf(
            "n1" to listOf("d_shared", "d_one"),
            "n2" to listOf("d_shared", "d_two"),
        ),
        priorities: Map<String, List<String>> = mapOf(
            "d_shared" to listOf("n1", "n2"),
            "d_one" to listOf("n1"),
            "d_two" to listOf("n2"),
        ),
        universalDetailIds: List<String> = listOf("d_universal"),
    ) = Catalog(needs, details, links, priorities, universalDetailIds)

    @Test
    fun validCatalogBuilds() {
        val catalog = catalog()

        assertEquals(listOf("A", "B"), catalog.categories)
        assertEquals(needOne, catalog.needOrNull("n1"))
        assertEquals(shared, catalog.detailOrNull("d_shared"))
        assertNull(catalog.needOrNull("missing"))
        assertNull(catalog.detailOrNull("missing"))
    }

    @Test
    fun rejectsDuplicateNeedId() {
        assertFailsWith<IllegalArgumentException> {
            catalog(needs = listOf(needOne, needOne.copy(label = "Again"), needTwo))
        }
    }

    @Test
    fun rejectsDuplicateDetailId() {
        assertFailsWith<IllegalArgumentException> {
            catalog(details = listOf(universal, shared, shared.copy(label = "Again"), onlyOne, onlyTwo))
        }
    }

    @Test
    fun rejectsBlankCategory() {
        assertFailsWith<IllegalArgumentException> {
            catalog(details = listOf(universal, shared, onlyOne, onlyTwo.copy(category = "  ")))
        }
    }

    @Test
    fun rejectsEmptyOptions() {
        assertFailsWith<IllegalArgumentException> {
            catalog(details = listOf(universal, shared, onlyOne, onlyTwo.copy(options = emptyList())))
        }
    }

    @Test
    fun rejectsLinksKeyThatIsAnUnknownNeed() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                links = mapOf(
                    "n1" to listOf("d_shared", "d_one"),
                    "n2" to listOf("d_shared", "d_two"),
                    "ghost" to listOf("d_shared"),
                ),
            )
        }
    }

    @Test
    fun rejectsLinkedDetailThatIsUnknown() {
        assertFailsWith<IllegalArgumentException> {
            catalog(links = mapOf("n1" to listOf("d_shared", "d_one", "ghost"), "n2" to listOf("d_shared", "d_two")))
        }
    }

    @Test
    fun rejectsSameDetailTwiceInOneNeedsLinks() {
        assertFailsWith<IllegalArgumentException> {
            catalog(links = mapOf("n1" to listOf("d_shared", "d_one", "d_one"), "n2" to listOf("d_shared", "d_two")))
        }
    }

    @Test
    fun rejectsPrioritiesKeyThatIsAnUnknownDetail() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                priorities = mapOf(
                    "d_shared" to listOf("n1", "n2"),
                    "d_one" to listOf("n1"),
                    "d_two" to listOf("n2"),
                    "ghost" to listOf("n1"),
                ),
            )
        }
    }

    @Test
    fun rejectsPrioritiesValueThatIsAnUnknownNeed() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                priorities = mapOf(
                    "d_shared" to listOf("n1", "n2", "ghost"),
                    "d_one" to listOf("n1"),
                    "d_two" to listOf("n2"),
                ),
            )
        }
    }

    @Test
    fun rejectsDuplicateNeedInAPrioritiesList() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                priorities = mapOf(
                    "d_shared" to listOf("n1", "n2", "n1"),
                    "d_one" to listOf("n1"),
                    "d_two" to listOf("n2"),
                ),
            )
        }
    }

    @Test
    fun rejectsUniversalDetailThatIsUnknown() {
        assertFailsWith<IllegalArgumentException> {
            catalog(universalDetailIds = listOf("d_universal", "ghost"))
        }
    }

    @Test
    fun rejectsDuplicateInTheUniversalList() {
        assertFailsWith<IllegalArgumentException> {
            catalog(universalDetailIds = listOf("d_universal", "d_universal"))
        }
    }

    @Test
    fun rejectsDetailThatIsBothUniversalAndLinked() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                links = mapOf("n1" to listOf("d_shared", "d_one", "d_universal"), "n2" to listOf("d_shared", "d_two")),
                priorities = mapOf(
                    "d_shared" to listOf("n1", "n2"),
                    "d_one" to listOf("n1"),
                    "d_two" to listOf("n2"),
                    "d_universal" to listOf("n1"),
                ),
            )
        }
    }

    @Test
    fun rejectsLinkedDetailWithNoPrioritiesEntry() {
        assertFailsWith<IllegalArgumentException> {
            catalog(priorities = mapOf("d_shared" to listOf("n1", "n2"), "d_one" to listOf("n1")))
        }
    }

    @Test
    fun rejectsPrioritiesEntryMissingANeedThatLinksTheDetail() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                priorities = mapOf(
                    "d_shared" to listOf("n1"),
                    "d_one" to listOf("n1"),
                    "d_two" to listOf("n2"),
                ),
            )
        }
    }

    @Test
    fun rejectsPrioritiesListingANeedThatDoesNotLinkTheDetail() {
        assertFailsWith<IllegalArgumentException> {
            catalog(
                priorities = mapOf(
                    "d_shared" to listOf("n1", "n2"),
                    "d_one" to listOf("n1", "n2"),
                    "d_two" to listOf("n2"),
                ),
            )
        }
    }
}
