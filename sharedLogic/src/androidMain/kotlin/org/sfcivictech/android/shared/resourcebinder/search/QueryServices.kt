package org.sfcivictech.android.shared.resourcebinder.search

/*
 * Kotlin counterpart of backend/query_services.py.
 *
 * Query an Open Referral / HSDS CSV data package with a free-text prompt,
 * ranked by semantic similarity and filtered/ordered by distance from the
 * caller's current location.
 *
 * Not shelter-specific: HSDS is a general human-services schema, so the same
 * "services" table covers shelter, housing, financial assistance, food,
 * and anything else in the data package -- this searches across all of it.
 *
 * Meant to back the mobile app: a user types something like "help paying rent
 * this month" or "food pantry for my family" and shares their GPS position,
 * and gets back the closest matching services -- within a configurable radius,
 * nearest first -- with organization, locations, addresses, phone numbers, and
 * categories already joined together.
 *
 * MATCHING ENGINE: real local sentence embeddings (all-MiniLM-L6-v2,
 * exported to ONNX and INT8-quantized), so "kids" matches "children",
 * "domestic violence" matches "IPV", etc. via actual semantic similarity,
 * not a synonym list. Fully offline at runtime: no API key, no network
 * call, ever -- the one-time cost was the export step, already done.
 * query()/buildOrLoadServiceEmbeddings() take an EmbeddingProvider; this
 * file doesn't hardcode which implementation, so it runs against
 * embedding.OnnxEmbeddingModel (on-device, NNAPI/CPU, Android assets).
 * Android-only for now: there is no iOS embedding backend yet.
 *
 * Before embedding, the prompt is also checked word-by-word against the
 * dataset's own vocabulary (service/org names, descriptions, taxonomy terms)
 * plus a small list of common query terms, and near-miss typos are corrected
 * (e.g. "shelders" -> "shelters") -- see correctSpelling().
 *
 * Input:  the newest data/hsds_csv_* directory (or one passed explicitly)
 * Output: a JSON-serializable QueryResult -- see query()
 */

import java.io.File
import java.io.FileNotFoundException
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

val DATA_DIR = File("data")

const val EARTH_RADIUS_MILES = 3958.8
const val DEFAULT_RADIUS_MILES = 30.0

// How many of the top semantic matches to geo-filter/rank, before cutting
// down to `limit`. Keeps the geo pass cheap even at 100x today's data size.
const val DEFAULT_CANDIDATE_POOL = 200

// Below this cosine similarity a match is treated as unrelated to the prompt,
// regardless of distance -- otherwise a nearby-but-irrelevant service could
// outrank a highly relevant one just for being closer.
const val DEFAULT_MIN_SIMILARITY = 0.25

// Ratio (Ratcliff/Obershelp, mirroring Python's difflib.SequenceMatcher) a
// mistyped word must clear against the nearest vocabulary word to be
// auto-corrected. High enough to catch a single typo ("shelders" ->
// "shelters") without rewriting unrelated words.
const val SPELLING_CORRECTION_CUTOFF = 0.8

// Words shorter than this are never auto-corrected, even if unrecognized --
// see correctSpelling().
const val MIN_SPELLING_CORRECTION_LENGTH = 4

// Common query vocabulary that may not appear verbatim in every dataset
// (e.g. a dataset with no current "veteran" services shouldn't make
// "veterans" uncorrectable), on top of whatever the dataset itself contains.
val COMMON_QUERY_TERMS: Set<String> = setOf(
    "shelter", "shelters", "housing", "home", "homeless", "homelessness",
    "family", "families", "adult", "adults", "child", "children", "kid",
    "kids", "youth", "teen", "teens", "men", "man", "women", "woman",
    "senior", "seniors", "elderly", "veteran", "veterans", "transgender",
    "lgbtq", "food", "pantry", "meal", "meals", "rent", "financial",
    "assistance", "domestic", "violence", "transitional", "emergency",
    "pregnant", "single", "parent", "low", "income", "eligibility",
    "application", "service", "services", "shower", "showers", "clinic",
    "medical", "mental", "health", "substance", "recovery", "job", "jobs",
    "employment", "legal", "immigration", "disability", "disabled",
)

private val WORD_RE = Regex("[A-Za-z]+")

// --------------------------------------------------------------------------
// Dataset loading
// --------------------------------------------------------------------------

data class Dataset(
    val organizations: List<Map<String, String>>,
    val services: List<Map<String, String>>,
    val locations: List<Map<String, String>>,
    val addresses: List<Map<String, String>>,
    val phones: List<Map<String, String>>,
    val serviceAtLocations: List<Map<String, String>>,
    val serviceTaxonomy: List<Map<String, String>>,
    val taxonomyTerms: List<Map<String, String>>,
)

fun findLatestPackage(dataDir: File = DATA_DIR): File {
    val candidates = (dataDir.listFiles { f -> f.isDirectory && f.name.startsWith("hsds_csv_") } ?: emptyArray())
        .sortedBy { it.name }
    if (candidates.isEmpty()) {
        throw FileNotFoundException(
            "No hsds_csv_* package found in $dataDir. Run a scraper (e.g. " +
                "scrape_shelters.py) and export_openreferral_csv.py first.",
        )
    }
    return candidates.last()
}

private fun readCsv(file: File): List<Map<String, String>> {
    file.bufferedReader(Charsets.UTF_8).use { reader ->
        val format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
        CSVParser(reader, format).use { parser ->
            return parser.records.map { it.toMap() }
        }
    }
}

fun loadDataset(packageDir: File? = null): Dataset {
    val dir = packageDir ?: findLatestPackage()
    fun table(name: String) = readCsv(File(dir, "$name.csv"))
    return Dataset(
        organizations = table("organizations"),
        services = table("services"),
        locations = table("locations"),
        addresses = table("addresses"),
        phones = table("phones"),
        serviceAtLocations = table("service_at_locations"),
        serviceTaxonomy = table("service_taxonomy"),
        taxonomyTerms = table("taxonomy_terms"),
    )
}

private fun indexBy(rows: List<Map<String, String>>, key: String): Map<String, List<Map<String, String>>> {
    val index = LinkedHashMap<String, MutableList<Map<String, String>>>()
    for (row in rows) {
        index.getOrPut(row.getValue(key)) { mutableListOf() }.add(row)
    }
    return index
}

private fun taxonomyByService(dataset: Dataset): Map<String, List<String>> {
    val termsById = dataset.taxonomyTerms.associateBy { it.getValue("id") }
    val result = LinkedHashMap<String, MutableList<String>>()
    for (link in dataset.serviceTaxonomy) {
        val term = termsById[link["taxonomy_term_id"]] ?: continue
        result.getOrPut(link.getValue("service_id")) { mutableListOf() }.add(term.getValue("name"))
    }
    return result
}

/**
 * All words (3+ letters) appearing anywhere in the dataset's text fields,
 * lowercased, plus COMMON_QUERY_TERMS -- used to spell-correct prompts
 * against terms that are actually meaningful for this data.
 */
fun datasetVocabulary(dataset: Dataset): Set<String> {
    val textFields = mutableListOf<String>()
    for (s in dataset.services) {
        textFields.add(s["name"] ?: "")
        textFields.add(s["description"] ?: "")
        textFields.add(s["eligibility_description"] ?: "")
    }
    for (o in dataset.organizations) textFields.add(o["name"] ?: "")
    for (t in dataset.taxonomyTerms) textFields.add(t["name"] ?: "")

    val vocabulary = COMMON_QUERY_TERMS.toMutableSet()
    for (text in textFields) {
        for (match in WORD_RE.findAll(text.lowercase())) {
            if (match.value.length >= 3) vocabulary.add(match.value)
        }
    }
    return vocabulary
}

// --------------------------------------------------------------------------
// Spelling correction (Ratcliff/Obershelp ratio, mirroring difflib exactly
// enough for short single-word typo correction; not optimized for long
// strings -- fine here since it only ever compares individual words)
// --------------------------------------------------------------------------

private fun matchingCharacters(a: String, b: String): Int {
    if (a.isEmpty() || b.isEmpty()) return 0
    var bestLen = 0
    var bestI = 0
    var bestJ = 0
    for (i in a.indices) {
        for (j in b.indices) {
            var len = 0
            while (i + len < a.length && j + len < b.length && a[i + len] == b[j + len]) len++
            if (len > bestLen) {
                bestLen = len
                bestI = i
                bestJ = j
            }
        }
    }
    if (bestLen == 0) return 0
    val left = matchingCharacters(a.substring(0, bestI), b.substring(0, bestJ))
    val right = matchingCharacters(a.substring(bestI + bestLen), b.substring(bestJ + bestLen))
    return left + bestLen + right
}

private fun sequenceMatcherRatio(a: String, b: String): Double {
    val total = a.length + b.length
    if (total == 0) return 1.0
    return 2.0 * matchingCharacters(a, b) / total
}

private fun closestMatch(word: String, vocabulary: Set<String>, cutoff: Double): String? =
    vocabulary
        .asSequence()
        .map { it to sequenceMatcherRatio(word, it) }
        .filter { it.second >= cutoff }
        .maxByOrNull { it.second }
        ?.first

/**
 * Best-effort typo correction against `vocabulary` before embedding, e.g.
 * "shelders for kids" -> "shelters for kids". A word is only replaced when
 * it isn't already a known word and a close match clears
 * SPELLING_CORRECTION_CUTOFF -- ambiguous or unmatched words (names,
 * numbers, unrelated typos) are left alone rather than guessed at.
 */
fun correctSpelling(prompt: String, vocabulary: Set<String>): String {
    val words = prompt.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return words.joinToString(" ") { word ->
        val core = word.filter { it.isLetter() }.lowercase()
        // Short words (the, me, is, ...) are skipped even if unrecognized: a
        // single-character difference is proportionally huge for them, so
        // the ratio is too unreliable to trust (e.g. "me" -> "men").
        if (core.length < MIN_SPELLING_CORRECTION_LENGTH || core in vocabulary) {
            word
        } else {
            closestMatch(core, vocabulary, SPELLING_CORRECTION_CUTOFF) ?: word
        }
    }
}

fun serviceText(service: Map<String, String>, orgName: String, categories: List<String>): String {
    val parts = listOf(
        service["name"] ?: "",
        orgName,
        categories.joinToString(", "),
        service["description"] ?: "",
        service["eligibility_description"] ?: "",
    )
    return parts.filter { it.isNotEmpty() }.joinToString(" . ")
}

// --------------------------------------------------------------------------
// Embedding cache
// --------------------------------------------------------------------------

@Serializable
private data class EmbeddingCache(val serviceIds: List<String>, val vectors: List<List<Float>>)

private fun embeddingCachePath(packageDir: File): File = File(packageDir, "service_embeddings.json")

/**
 * Return one L2-normalized embedding per dataset.services[i] (same order),
 * caching to disk next to the CSV package so a 100x-sized dataset only needs
 * to be encoded once per package. Cache format is a plain JSON file (not
 * numpy .npz -- there is no numpy on the JVM), incompatible with any cache
 * written by the Python original.
 */
fun buildOrLoadServiceEmbeddings(
    dataset: Dataset,
    packageDir: File,
    embeddingProvider: EmbeddingProvider,
    rebuild: Boolean = false,
): List<FloatArray> {
    val serviceIds = dataset.services.map { it.getValue("id") }
    val cachePath = embeddingCachePath(packageDir)

    if (!rebuild && cachePath.exists()) {
        val cached = Json.decodeFromString<EmbeddingCache>(cachePath.readText(Charsets.UTF_8))
        if (cached.serviceIds == serviceIds) {
            return cached.vectors.map { it.toFloatArray() }
        }
    }

    val orgsById = dataset.organizations.associateBy { it.getValue("id") }
    val taxByService = taxonomyByService(dataset)
    val texts = dataset.services.map { s ->
        serviceText(
            s,
            orgsById[s["organization_id"]]?.get("name") ?: "",
            taxByService[s.getValue("id")] ?: emptyList(),
        )
    }

    val vectors = embeddingProvider.encode(texts)
    cachePath.writeText(
        Json.encodeToString(EmbeddingCache(serviceIds, vectors.map { it.toList() })),
        Charsets.UTF_8,
    )
    return vectors
}

// --------------------------------------------------------------------------
// Geo
// --------------------------------------------------------------------------

fun haversineMiles(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val rlat1 = Math.toRadians(lat1)
    val rlon1 = Math.toRadians(lon1)
    val rlat2 = Math.toRadians(lat2)
    val rlon2 = Math.toRadians(lon2)
    val dlat = rlat2 - rlat1
    val dlon = rlon2 - rlon1
    val a = sin(dlat / 2).pow(2) + cos(rlat1) * cos(rlat2) * sin(dlon / 2).pow(2)
    return 2 * EARTH_RADIUS_MILES * asin(sqrt(a))
}

// --------------------------------------------------------------------------
// Result shapes -- @SerialName pins every JSON key to match the Python
// dict output exactly, so this stays a drop-in replacement for anything
// consuming the JSON (e.g. a future mobile API contract).
// --------------------------------------------------------------------------

@Serializable
data class AddressCard(
    @SerialName("address_1") val address1: String = "",
    @SerialName("address_2") val address2: String = "",
    val city: String = "",
    @SerialName("state_province") val stateProvince: String = "",
    @SerialName("postal_code") val postalCode: String = "",
)

@Serializable
data class LocationCard(
    val name: String = "",
    val latitude: String = "",
    val longitude: String = "",
    @SerialName("distance_miles") val distanceMiles: Double,
    val addresses: List<AddressCard>,
    val phones: List<String>,
)

@Serializable
data class ServiceCard(
    @SerialName("service_id") val serviceId: String,
    @SerialName("service_name") val serviceName: String,
    val organization: String,
    val description: String,
    val eligibility: String,
    val fees: String,
    @SerialName("application_process") val applicationProcess: String,
    val categories: List<String>,
    val phones: List<String>,
    val locations: List<LocationCard>,
    val url: String,
    @SerialName("distance_miles") val distanceMiles: Double,
    @SerialName("relevance_score") val relevanceScore: Double,
)

@Serializable
data class Origin(val lat: Double, val lon: Double)

@Serializable
data class QueryResult(
    val query: String,
    @SerialName("interpreted_query") val interpretedQuery: String? = null,
    val origin: Origin,
    @SerialName("radius_miles") val radiusMiles: Double,
    val count: Int,
    val results: List<ServiceCard>,
)

// Python's round() uses banker's rounding; these use ordinary half-up
// rounding, which only differs from the original at exact .xx5 boundaries --
// immaterial for display-only distance/similarity figures.
private fun round2(x: Double) = Math.round(x * 100.0) / 100.0
private fun round4(x: Double) = Math.round(x * 10000.0) / 10000.0

// --------------------------------------------------------------------------
// query()
// --------------------------------------------------------------------------

/**
 * Semantically search an HSDS CSV package for services matching a
 * free-text prompt, restricted to a radius around the caller's current
 * location and ordered nearest-first.
 *
 * @param prompt free-text description of what the user needs, e.g. "emergency
 *   shelter for a family with young kids", "help paying rent this month", or
 *   "food pantry near me" -- any category present in the HSDS package, not
 *   just shelter.
 * @param userLat @param userLon the caller's current GPS coordinates.
 * @param embeddingProvider the encode() backend -- embedding.OnnxEmbeddingModel
 *   on-device.
 * @param radiusMiles only services with a location within this distance are
 *   returned.
 * @param limit max number of results to return.
 * @param candidatePool how many of the top semantic matches to consider for
 *   geo-filtering before applying `limit`. Keeps the geo pass cheap
 *   regardless of total dataset size.
 * @param minSimilarity cosine-similarity floor (0-1) a service must clear to
 *   be considered a match at all, applied before distance sorting so an
 *   irrelevant-but-nearby service can't outrank a relevant one.
 * @param packageDir explicit hsds_csv_* directory; defaults to the newest one
 *   under data/.
 * @param dataset a pre-loaded dataset from loadDataset(), to avoid re-reading
 *   the CSVs on every call.
 * @param serviceEmbeddings a pre-loaded array from
 *   buildOrLoadServiceEmbeddings(), to avoid recomputing/reloading it on
 *   every call. Load `dataset` and `serviceEmbeddings` once per app session
 *   and pass both into every query() call.
 *
 * Returns a QueryResult where interpretedQuery is the spelling-corrected
 * prompt actually embedded (null if no correction was made), and results are
 * ordered by distance ascending (nearest first) among the top semantic
 * matches within radiusMiles.
 */
fun query(
    prompt: String,
    userLat: Double,
    userLon: Double,
    embeddingProvider: EmbeddingProvider,
    radiusMiles: Double = DEFAULT_RADIUS_MILES,
    limit: Int = 5,
    candidatePool: Int = DEFAULT_CANDIDATE_POOL,
    minSimilarity: Double = DEFAULT_MIN_SIMILARITY,
    packageDir: File? = null,
    dataset: Dataset? = null,
    serviceEmbeddings: List<FloatArray>? = null,
): QueryResult {
    val resolvedPackageDir = packageDir ?: findLatestPackage()
    val resolvedDataset = dataset ?: loadDataset(resolvedPackageDir)
    val embeddings = serviceEmbeddings
        ?: buildOrLoadServiceEmbeddings(resolvedDataset, resolvedPackageDir, embeddingProvider)

    val orgsById = resolvedDataset.organizations.associateBy { it.getValue("id") }
    val locationsById = resolvedDataset.locations.associateBy { it.getValue("id") }
    val addressesByLocation = indexBy(resolvedDataset.addresses, "location_id")
    val phonesByService = indexBy(resolvedDataset.phones, "service_id")
    val phonesByLocation = indexBy(resolvedDataset.phones, "location_id")
    val salByService = indexBy(resolvedDataset.serviceAtLocations, "service_id")
    val taxByService = taxonomyByService(resolvedDataset)

    val vocabulary = datasetVocabulary(resolvedDataset)
    val interpretedPrompt = correctSpelling(prompt, vocabulary)

    val promptVector = embeddingProvider.encode(listOf(interpretedPrompt))[0]
    val similarities = DoubleArray(embeddings.size) { i ->
        val vec = embeddings[i]
        var dot = 0.0
        for (j in vec.indices) dot += vec[j] * promptVector[j]
        dot
    }
    // Cosine similarity: both sides are L2-normalized, so a dot product is enough.
    val rankedIndices = similarities.indices.sortedByDescending { similarities[it] }.take(candidatePool)

    data class Candidate(
        val distanceMiles: Double,
        val similarity: Double,
        val service: Map<String, String>,
        val nearbyLocations: List<Pair<Double, Map<String, String>>>,
    )

    val candidates = mutableListOf<Candidate>()
    for (idx in rankedIndices) {
        // rankedIndices is sorted descending, so nothing after this clears the bar.
        if (similarities[idx] < minSimilarity) break
        val service = resolvedDataset.services[idx]
        val nearby = mutableListOf<Pair<Double, Map<String, String>>>()
        for (link in salByService[service.getValue("id")] ?: emptyList()) {
            val loc = locationsById[link["location_id"]] ?: continue
            val lat = loc["latitude"]?.toDoubleOrNull()
            val lon = loc["longitude"]?.toDoubleOrNull()
            if (lat == null || lon == null) continue
            val distance = haversineMiles(userLat, userLon, lat, lon)
            if (distance <= radiusMiles) nearby.add(distance to loc)
        }
        if (nearby.isEmpty()) continue
        nearby.sortBy { it.first }
        candidates.add(Candidate(nearby.first().first, similarities[idx], service, nearby))
    }

    candidates.sortBy { it.distanceMiles } // nearest first

    val results = candidates.take(limit).map { c ->
        val org = orgsById[c.service["organization_id"]] ?: emptyMap()
        val locations = c.nearbyLocations.map { (dist, loc) ->
            LocationCard(
                name = loc["name"] ?: "",
                latitude = loc["latitude"] ?: "",
                longitude = loc["longitude"] ?: "",
                distanceMiles = round2(dist),
                addresses = (addressesByLocation[loc["id"]] ?: emptyList()).map { addr ->
                    AddressCard(
                        address1 = addr["address_1"] ?: "",
                        address2 = addr["address_2"] ?: "",
                        city = addr["city"] ?: "",
                        stateProvince = addr["state_province"] ?: "",
                        postalCode = addr["postal_code"] ?: "",
                    )
                },
                phones = (phonesByLocation[loc["id"]] ?: emptyList()).map { it["number"] ?: "" },
            )
        }
        ServiceCard(
            serviceId = c.service.getValue("id"),
            serviceName = c.service["name"] ?: "",
            organization = org["name"] ?: "",
            description = c.service["description"] ?: "",
            eligibility = c.service["eligibility_description"] ?: "",
            fees = c.service["fees_description"] ?: "",
            applicationProcess = c.service["application_process"] ?: "",
            categories = taxByService[c.service.getValue("id")] ?: emptyList(),
            phones = (phonesByService[c.service.getValue("id")] ?: emptyList()).map { it["number"] ?: "" },
            locations = locations,
            url = c.service["url"]?.takeIf { it.isNotEmpty() } ?: (org["url"] ?: ""),
            distanceMiles = round2(c.distanceMiles),
            relevanceScore = round4(c.similarity),
        )
    }

    return QueryResult(
        query = prompt,
        interpretedQuery = interpretedPrompt.takeIf { it != prompt },
        origin = Origin(userLat, userLon),
        radiusMiles = radiusMiles,
        count = results.size,
        results = results,
    )
}
