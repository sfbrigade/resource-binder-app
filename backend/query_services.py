"""
Step 3: Query an Open Referral / HSDS CSV data package with a free-text
prompt, ranked by semantic similarity and filtered/ordered by distance from
the caller's current location.

Not shelter-specific: HSDS is a general human-services schema, so the same
"services" table covers shelter, housing, financial assistance, food,
and anything else in the data package -- this searches across all of it.

Meant to back a mobile app: a user types something like "help paying rent
this month" or "food pantry for my family" and shares their GPS position,
and gets back the closest matching services -- within a configurable radius,
nearest first -- with organization, locations, addresses, phone numbers, and
categories already joined together.

Matching uses local sentence embeddings (sentence-transformers), not literal
keyword overlap, so "kids" matches "children", "domestic violence" matches
"IPV", etc. The model runs on-device/on-server and needs no API key; the
only network dependency is the one-time download of model weights the first
time it's used (cached under ~/.cache after that -- fully offline from then
on).

Before embedding, the prompt is also checked word-by-word against the
dataset's own vocabulary (service/org names, descriptions, taxonomy terms)
plus a small list of common query terms, and near-miss typos are corrected
(e.g. "shelders" -> "shelters") -- see _correct_spelling().

Input:  newest data/hsds_csv_*/ package (or a directory passed explicitly)
Output: a JSON-serializable dict -- see query()
"""

import csv
import difflib
import json
import math
import re
from pathlib import Path

import numpy as np
from sentence_transformers import SentenceTransformer

DATA_DIR = Path(__file__).parent / "data"
EMBEDDING_MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
EARTH_RADIUS_MILES = 3958.8
DEFAULT_RADIUS_MILES = 30.0
# How many of the top semantic matches to geo-filter/rank, before cutting
# down to `limit`. Keeps the geo pass cheap even at 100x today's data size.
DEFAULT_CANDIDATE_POOL = 200
# Below this cosine similarity a match is treated as unrelated to the prompt,
# regardless of distance -- otherwise a nearby-but-irrelevant service could
# outrank a highly relevant one just for being closer.
DEFAULT_MIN_SIMILARITY = 0.25
# difflib.SequenceMatcher ratio a mistyped word must clear against the
# nearest vocabulary word to be auto-corrected. High enough to catch a
# single typo ("shelders" -> "shelters") without rewriting unrelated words.
SPELLING_CORRECTION_CUTOFF = 0.8
# Words shorter than this are never auto-corrected, even if unrecognized --
# see correct_spelling().
MIN_SPELLING_CORRECTION_LENGTH = 4

# Common query vocabulary that may not appear verbatim in every dataset
# (e.g. a dataset with no current "veteran" services shouldn't make
# "veterans" uncorrectable), on top of whatever the dataset itself contains.
_COMMON_QUERY_TERMS = {
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
}

_WORD_RE = re.compile(r"[A-Za-z]+")

_model: SentenceTransformer | None = None


def _get_model() -> SentenceTransformer:
    global _model
    if _model is None:
        _model = SentenceTransformer(EMBEDDING_MODEL_NAME)
    return _model


def find_latest_package(data_dir: Path = DATA_DIR) -> Path:
    candidates = sorted(p for p in data_dir.glob("hsds_csv_*") if p.is_dir())
    if not candidates:
        raise FileNotFoundError(
            f"No hsds_csv_* package found in {data_dir}. Run a scraper (e.g. "
            "scrape_shelters.py) and export_openreferral_csv.py first."
        )
    return candidates[-1]


def _read_csv(path: Path) -> list[dict]:
    with path.open(newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def load_dataset(package_dir: Path | None = None) -> dict[str, list[dict]]:
    """Load every table of an HSDS CSV package into memory, keyed by table name."""
    package_dir = package_dir or find_latest_package()
    tables = [
        "organizations", "services", "locations", "addresses", "phones",
        "service_at_locations", "service_taxonomy", "taxonomy_terms",
    ]
    return {name: _read_csv(package_dir / f"{name}.csv") for name in tables}


def _index_by(rows: list[dict], key: str) -> dict[str, list[dict]]:
    index: dict[str, list[dict]] = {}
    for row in rows:
        index.setdefault(row[key], []).append(row)
    return index


def _taxonomy_by_service(dataset: dict) -> dict[str, list[str]]:
    terms_by_id = {t["id"]: t for t in dataset["taxonomy_terms"]}
    taxonomy_by_service: dict[str, list[str]] = {}
    for link in dataset["service_taxonomy"]:
        term = terms_by_id.get(link["taxonomy_term_id"])
        if term:
            taxonomy_by_service.setdefault(link["service_id"], []).append(term["name"])
    return taxonomy_by_service


def _dataset_vocabulary(dataset: dict) -> set[str]:
    """All words (3+ letters) appearing anywhere in the dataset's text
    fields, lowercased, plus _COMMON_QUERY_TERMS -- used to spell-correct
    prompts against terms that are actually meaningful for this data."""
    text_fields: list[str] = []
    for s in dataset["services"]:
        text_fields.append(s.get("name", ""))
        text_fields.append(s.get("description", ""))
        text_fields.append(s.get("eligibility_description", ""))
    for o in dataset["organizations"]:
        text_fields.append(o.get("name", ""))
    for t in dataset["taxonomy_terms"]:
        text_fields.append(t.get("name", ""))

    vocabulary = set(_COMMON_QUERY_TERMS)
    for text in text_fields:
        for word in _WORD_RE.findall(text.lower()):
            if len(word) >= 3:
                vocabulary.add(word)
    return vocabulary


def correct_spelling(prompt: str, vocabulary: set[str]) -> str:
    """
    Best-effort typo correction against `vocabulary` before embedding, e.g.
    "shelders for kids" -> "shelters for kids". A word is only replaced when
    it isn't already a known word and a close match clears
    SPELLING_CORRECTION_CUTOFF -- ambiguous or unmatched words (names,
    numbers, unrelated typos) are left alone rather than guessed at.
    """
    corrected_words = []
    for word in prompt.split():
        core = "".join(ch for ch in word if ch.isalpha()).lower()
        # Short words (the, me, is, ...) are skipped even if unrecognized:
        # a single-character difference is proportionally huge for them, so
        # difflib's ratio is too unreliable to trust (e.g. "me" -> "men").
        if len(core) < MIN_SPELLING_CORRECTION_LENGTH or core in vocabulary:
            corrected_words.append(word)
            continue
        match = difflib.get_close_matches(core, vocabulary, n=1, cutoff=SPELLING_CORRECTION_CUTOFF)
        corrected_words.append(match[0] if match else word)
    return " ".join(corrected_words)


def _service_text(service: dict, org_name: str, categories: list[str]) -> str:
    parts = [
        service.get("name", ""),
        org_name,
        ", ".join(categories),
        service.get("description", ""),
        service.get("eligibility_description", ""),
    ]
    return " . ".join(p for p in parts if p)


def _embedding_cache_path(package_dir: Path) -> Path:
    return package_dir / "service_embeddings.npz"


def build_or_load_service_embeddings(dataset: dict, package_dir: Path,
                                      rebuild: bool = False) -> np.ndarray:
    """
    Return an (N, dim) float32 array of L2-normalized embeddings, one row per
    dataset["services"][i], caching to disk next to the CSV package so a
    100x-sized dataset only needs to be encoded once per package.
    """
    service_ids = np.array([s["id"] for s in dataset["services"]])
    cache_path = _embedding_cache_path(package_dir)

    if cache_path.exists() and not rebuild:
        cached = np.load(cache_path, allow_pickle=False)
        if np.array_equal(cached["service_ids"], service_ids):
            return cached["vectors"]

    orgs_by_id = {o["id"]: o for o in dataset["organizations"]}
    taxonomy_by_service = _taxonomy_by_service(dataset)
    texts = [
        _service_text(s, orgs_by_id.get(s["organization_id"], {}).get("name", ""),
                       taxonomy_by_service.get(s["id"], []))
        for s in dataset["services"]
    ]

    vectors = _get_model().encode(
        texts, normalize_embeddings=True, show_progress_bar=len(texts) > 200,
    ).astype("float32")
    np.savez(cache_path, vectors=vectors, service_ids=service_ids)
    return vectors


def _haversine_miles(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    lat1, lon1, lat2, lon2 = map(math.radians, (lat1, lon1, lat2, lon2))
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    return 2 * EARTH_RADIUS_MILES * math.asin(math.sqrt(a))


def query(prompt: str, user_lat: float, user_lon: float,
          radius_miles: float = DEFAULT_RADIUS_MILES, limit: int = 5,
          candidate_pool: int = DEFAULT_CANDIDATE_POOL,
          min_similarity: float = DEFAULT_MIN_SIMILARITY,
          package_dir: Path | None = None, dataset: dict | None = None,
          service_embeddings: np.ndarray | None = None) -> dict:
    """
    Semantically search an HSDS CSV package for services matching a
    free-text prompt, restricted to a radius around the caller's current
    location and ordered nearest-first.

    Args:
        prompt: free-text description of what the user needs, e.g.
            "emergency shelter for a family with young kids", "help paying
            rent this month", or "food pantry near me" -- any category
            present in the HSDS package, not just shelter.
        user_lat, user_lon: the caller's current GPS coordinates.
        radius_miles: only services with a location within this distance
            are returned.
        limit: max number of results to return.
        candidate_pool: how many of the top semantic matches to consider for
            geo-filtering before applying `limit`. Keeps the geo pass cheap
            regardless of total dataset size.
        min_similarity: cosine-similarity floor (0-1) a service must clear to
            be considered a match at all, applied before distance sorting so
            an irrelevant-but-nearby service can't outrank a relevant one.
        package_dir: explicit hsds_csv_* directory; defaults to the newest
            one under data/.
        dataset: a pre-loaded dataset from load_dataset(), to avoid
            re-reading the CSVs on every call.
        service_embeddings: a pre-loaded array from
            build_or_load_service_embeddings(), to avoid recomputing/
            reloading it on every call. In a mobile API backend, load
            `dataset` and `service_embeddings` once at startup and pass both
            into every query() call.

    Returns:
        {"query", "interpreted_query", "origin", "radius_miles", "count",
         "results": [ {...service card with distance_miles...}, ... ]}
        where "interpreted_query" is the spelling-corrected prompt actually
        embedded (None if no correction was made), and results are ordered
        by distance ascending (nearest first) among the top semantic
        matches within radius_miles.
    """
    package_dir = package_dir or find_latest_package()
    dataset = dataset or load_dataset(package_dir)
    if service_embeddings is None:
        service_embeddings = build_or_load_service_embeddings(dataset, package_dir)

    orgs_by_id = {o["id"]: o for o in dataset["organizations"]}
    locations_by_id = {loc["id"]: loc for loc in dataset["locations"]}
    addresses_by_location = _index_by(dataset["addresses"], "location_id")
    phones_by_service = _index_by(dataset["phones"], "service_id")
    phones_by_location = _index_by(dataset["phones"], "location_id")
    sal_by_service = _index_by(dataset["service_at_locations"], "service_id")
    taxonomy_by_service = _taxonomy_by_service(dataset)

    interpreted_prompt = correct_spelling(prompt, _dataset_vocabulary(dataset))

    prompt_vector = _get_model().encode([interpreted_prompt], normalize_embeddings=True)[0]
    similarities = service_embeddings @ prompt_vector  # cosine, both L2-normalized
    ranked_indices = np.argsort(-similarities)[:candidate_pool]

    candidates = []
    for idx in ranked_indices:
        if similarities[idx] < min_similarity:
            break  # ranked_indices is sorted descending, so nothing after this clears the bar
        service = dataset["services"][idx]
        nearby_locations = []
        for link in sal_by_service.get(service["id"], []):
            loc = locations_by_id.get(link["location_id"])
            if not loc or not loc.get("latitude") or not loc.get("longitude"):
                continue
            distance = _haversine_miles(user_lat, user_lon,
                                         float(loc["latitude"]), float(loc["longitude"]))
            if distance <= radius_miles:
                nearby_locations.append((distance, loc))
        if not nearby_locations:
            continue
        nearby_locations.sort(key=lambda item: item[0])
        candidates.append((nearby_locations[0][0], float(similarities[idx]), service,
                            nearby_locations))

    candidates.sort(key=lambda item: item[0])  # nearest first

    results = []
    for distance_miles, similarity, service, nearby_locations in candidates[:limit]:
        org = orgs_by_id.get(service["organization_id"], {})
        locations = [
            {
                "name": loc.get("name", ""),
                "latitude": loc.get("latitude", ""),
                "longitude": loc.get("longitude", ""),
                "distance_miles": round(dist, 2),
                "addresses": [
                    {k: address.get(k, "") for k in
                     ("address_1", "address_2", "city", "state_province", "postal_code")}
                    for address in addresses_by_location.get(loc["id"], [])
                ],
                "phones": [p.get("number", "") for p in phones_by_location.get(loc["id"], [])],
            }
            for dist, loc in nearby_locations
        ]

        results.append({
            "service_id": service["id"],
            "service_name": service.get("name", ""),
            "organization": org.get("name", ""),
            "description": service.get("description", ""),
            "eligibility": service.get("eligibility_description", ""),
            "fees": service.get("fees_description", ""),
            "application_process": service.get("application_process", ""),
            "categories": taxonomy_by_service.get(service["id"], []),
            "phones": [p.get("number", "") for p in phones_by_service.get(service["id"], [])],
            "locations": locations,
            "url": service.get("url") or org.get("url", ""),
            "distance_miles": round(distance_miles, 2),
            "relevance_score": round(similarity, 4),
        })

    return {
        "query": prompt,
        "interpreted_query": interpreted_prompt if interpreted_prompt != prompt else None,
        "origin": {"lat": user_lat, "lon": user_lon},
        "radius_miles": radius_miles,
        "count": len(results),
        "results": results,
    }


def main():
    import argparse

    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("prompt", nargs="?",
                         default="help finding food, housing, or financial assistance")
    parser.add_argument("--lat", type=float, default=37.7749, help="caller latitude")
    parser.add_argument("--lon", type=float, default=-122.4194, help="caller longitude")
    parser.add_argument("--radius", type=float, default=DEFAULT_RADIUS_MILES,
                         help="search radius in miles")
    parser.add_argument("--limit", type=int, default=5)
    parser.add_argument("--min-similarity", type=float, default=DEFAULT_MIN_SIMILARITY,
                         help="cosine-similarity floor for a match, 0-1")
    parser.add_argument("--build-index", action="store_true",
                         help="(re)build the embedding cache and exit")
    args = parser.parse_args()

    package_dir = find_latest_package()
    dataset = load_dataset(package_dir)

    if args.build_index:
        build_or_load_service_embeddings(dataset, package_dir, rebuild=True)
        print(f"Embedding index built for {package_dir}")
        return

    result = query(args.prompt, args.lat, args.lon, radius_miles=args.radius,
                    limit=args.limit, min_similarity=args.min_similarity,
                    package_dir=package_dir, dataset=dataset)
    print(json.dumps(result, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
