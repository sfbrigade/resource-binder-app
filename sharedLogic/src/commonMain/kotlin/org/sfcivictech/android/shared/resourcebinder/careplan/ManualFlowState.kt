package org.sfcivictech.android.shared.resourcebinder.careplan

// The whole state of the manual flow. Immutable: every change returns a new state, and
// everything shown (cards, counts, flags) is derived from it, so each platform only
// has to hold the current value. Values chosen under a need stay in the state when
// that need is deselected but are hidden, uncounted and left out of result().
data class ManualFlowState(
    val catalog: Catalog,
    val selectedNeedIds: List<String>,
    // detail id -> chosen option
    val values: Map<String, String>,
    // details added from the Add details sheet, in catalog order
    val pinnedDetailIds: List<String>,
) {
    fun isSelected(needId: String): Boolean = needId in selectedNeedIds

    fun toggleNeed(needId: String): ManualFlowState {
        if (catalog.needOrNull(needId) == null) return this
        val updated = if (isSelected(needId)) selectedNeedIds - needId else selectedNeedIds + needId
        return copy(selectedNeedIds = updated)
    }

    // a null value clears the detail; an unknown detail or an option it doesn't offer is ignored
    fun setDetail(detailId: String, value: String?): ManualFlowState {
        val detail = catalog.detailOrNull(detailId) ?: return this
        if (value == null) return copy(values = values - detailId)
        if (value !in detail.options) return this
        return copy(values = values + (detailId to value))
    }

    fun setPinnedDetails(detailIds: List<String>): ManualFlowState =
        copy(pinnedDetailIds = catalog.details.map { it.id }.filter { it in detailIds })

    val cards: List<DetailCard>
        get() = buildCards()

    // ids of the details already on screen because of the universal card or a selected need
    val lockedDetailIds: List<String>
        get() = buildCards().filter { it.kind != CardKind.ADDITIONAL }.flatMap { card -> card.items.map { it.detailId } }

    val sheetCategories: List<SheetCategory>
        get() {
            val locked = lockedDetailIds
            return catalog.categories.map { category ->
                SheetCategory(
                    label = category,
                    details = catalog.details.filter { it.category == category }.map { detail ->
                        val isLocked = detail.id in locked
                        SheetDetail(
                            detailId = detail.id,
                            label = detail.label,
                            locked = isLocked,
                            pinned = !isLocked && detail.id in pinnedDetailIds,
                        )
                    },
                )
            }
        }

    val needCount: Int
        get() = selectedNeedIds.size

    // chosen values that are visible; hidden ones don't count
    val detailCount: Int
        get() = visibleValues().size

    val canContinue: Boolean
        get() = selectedNeedIds.isNotEmpty()

    val hasChanges: Boolean
        get() = selectedNeedIds.isNotEmpty() || pinnedDetailIds.isNotEmpty() || detailCount > 0

    fun result(): ManualFlowResult = ManualFlowResult(
        needIds = catalog.needs.map { it.id }.filter { isSelected(it) },
        values = visibleValues(),
    )

    private fun visibleValues(): Map<String, String> {
        val shown = buildCards().flatMap { card -> card.items.map { it.detailId } }.toSet()
        return values.filterKeys { it in shown }
    }

    // a detail shared by several selected needs goes under the first one in its priorities
    private fun ownerOf(detailId: String): String? =
        catalog.priorities[detailId]?.firstOrNull { isSelected(it) }

    private fun buildCards(): List<DetailCard> {
        val cards = mutableListOf<DetailCard>()
        cards += card(CardKind.UNIVERSAL, null, "", catalog.universalDetailIds)

        catalog.needs.filter { isSelected(it.id) }.forEach { need ->
            //every selected need gets a card, even an empty one when other selected needs own
            //all its details, so the selection visibly registers
            val owned = catalog.links[need.id].orEmpty().filter { ownerOf(it) == need.id }
            cards += card(CardKind.NEED, need.id, need.label, owned)
        }

        val shown = cards.flatMap { card -> card.items.map { it.detailId } }.toSet()
        val additional = catalog.details.map { it.id }.filter { it in pinnedDetailIds && it !in shown }
        if (additional.isNotEmpty()) cards += card(CardKind.ADDITIONAL, null, "", additional)
        return cards
    }

    private fun card(kind: CardKind, needId: String?, title: String, detailIds: List<String>): DetailCard {
        val items = detailIds.mapNotNull { id ->
            catalog.detailOrNull(id)?.let { DetailItem(it.id, it.label, it.options, values[it.id]) }
        }
        return DetailCard(kind, needId, title, items, items.count { it.value != null })
    }

    companion object {
        fun initial(catalog: Catalog): ManualFlowState =
            ManualFlowState(catalog, emptyList(), emptyMap(), emptyList())

        // rebuilds a saved state, dropping anything the catalog no longer has
        fun restore(
            catalog: Catalog,
            needIds: List<String>,
            values: Map<String, String>,
            pinnedDetailIds: List<String>,
        ): ManualFlowState = ManualFlowState(
            catalog = catalog,
            selectedNeedIds = needIds.filter { catalog.needOrNull(it) != null }.distinct(),
            values = values.filter { (id, value) -> catalog.detailOrNull(id)?.options?.contains(value) == true },
            pinnedDetailIds = catalog.details.map { it.id }.filter { it in pinnedDetailIds },
        )
    }
}
