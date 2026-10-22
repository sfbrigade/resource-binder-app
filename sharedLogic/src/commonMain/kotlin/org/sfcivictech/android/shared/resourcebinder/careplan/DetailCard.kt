package org.sfcivictech.android.shared.resourcebinder.careplan

enum class CardKind { UNIVERSAL, NEED, ADDITIONAL }

data class DetailItem(
    val detailId: String,
    val label: String,
    val options: List<String>,
    // the chosen option, or null when nothing is chosen
    val value: String?,
)

// One card on the manual flow screen. needId and title are the need's id and label
// for NEED cards and null / "" for the other kinds, which the UI titles itself. A NEED
// card has no items when the other selected needs own all of its details.
data class DetailCard(
    val kind: CardKind,
    val needId: String?,
    val title: String,
    val items: List<DetailItem>,
    val chosenCount: Int,
)

// One chip in the Add details sheet. Locked chips are already on screen because of
// a selected need or the universal card; pinned chips were added from the sheet.
data class SheetDetail(
    val detailId: String,
    val label: String,
    val locked: Boolean,
    val pinned: Boolean,
)

data class SheetCategory(val label: String, val details: List<SheetDetail>)

// What "Find resources" hands over: only what is visible on screen.
data class ManualFlowResult(val needIds: List<String>, val values: Map<String, String>)
