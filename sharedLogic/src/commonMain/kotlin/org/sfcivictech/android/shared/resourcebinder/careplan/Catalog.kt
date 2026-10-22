package org.sfcivictech.android.shared.resourcebinder.careplan

data class Need(val id: String, val label: String)

data class Detail(
    val id: String,
    val label: String,
    // heading the detail sits under in the Add details sheet
    val category: String,
    val options: List<String>,
)

// Everything the manual flow knows about needs and details, as plain data.
// Adding a need, detail or category means editing these tables, not the UI.
// Need ids and detail ids are separate namespaces, so the same id can name both.
class Catalog(
    val needs: List<Need>,
    val details: List<Detail>,
    // need id -> its detail ids, in the order they appear in that need's card
    val links: Map<String, List<String>>,
    // detail id -> need ids, owner first; a detail shared by several selected needs
    // appears under the first of these that is selected
    val priorities: Map<String, List<String>>,
    // details shown in "Useful in most cases" whatever is selected
    val universalDetailIds: List<String>,
) {
    private val needsById = needs.associateBy { it.id }
    private val detailsById = details.associateBy { it.id }

    // sheet headings, in the order the details first mention them
    val categories: List<String> = details.map { it.category }.distinct()

    fun needOrNull(id: String): Need? = needsById[id]

    fun detailOrNull(id: String): Detail? = detailsById[id]

    init {
        require(needsById.size == needs.size) { "duplicate need id in ${needs.map { it.id }}" }
        require(detailsById.size == details.size) { "duplicate detail id in ${details.map { it.id }}" }
        details.forEach { detail ->
            require(detail.category.isNotBlank()) { "detail ${detail.id} has a blank category" }
            require(detail.options.isNotEmpty()) { "detail ${detail.id} has no options" }
        }

        links.forEach { (needId, detailIds) ->
            require(needId in needsById) { "links names unknown need $needId" }
            require(detailIds.distinct().size == detailIds.size) { "need $needId links a detail twice" }
            detailIds.forEach { detailId ->
                require(detailId in detailsById) { "need $needId links unknown detail $detailId" }
                require(priorities[detailId]?.contains(needId) == true) {
                    "detail $detailId is linked by $needId but its priorities do not list $needId"
                }
            }
        }

        priorities.forEach { (detailId, needIds) ->
            require(detailId in detailsById) { "priorities names unknown detail $detailId" }
            require(needIds.distinct().size == needIds.size) { "priorities for $detailId list a need twice" }
            needIds.forEach { needId ->
                require(needId in needsById) { "priorities for $detailId name unknown need $needId" }
                require(links[needId]?.contains(detailId) == true) {
                    "priorities for $detailId list $needId, which does not link it"
                }
            }
        }

        require(universalDetailIds.distinct().size == universalDetailIds.size) { "universal list names a detail twice" }
        val linkedDetailIds = links.values.flatten().toSet()
        universalDetailIds.forEach { detailId ->
            require(detailId in detailsById) { "universal list names unknown detail $detailId" }
            require(detailId !in linkedDetailIds) { "detail $detailId is both universal and linked by a need" }
        }
    }
}
