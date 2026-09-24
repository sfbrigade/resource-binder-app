"""
Answer a free-text prompt about SF shelter/human services using the local
Open Referral HSDS data package under data/hsds_csv_*/.

This never contacts sfserviceguide.org or any other network endpoint. It
only reads the HSDS CSV package already exported by export_openreferral_csv.py
and searches it with the semantic query engine in query_services.py. The
one network dependency anywhere in that path -- downloading the
sentence-transformers model weights -- is one-time and already cached
locally under ~/.cache/huggingface, so normal runs are fully offline.

Prompts for what the user is looking for (e.g. "family shelters",
"adult shelters for men"), or takes it from argv, searches the newest
local HSDS package, and prints the matching services.

Output: matching services printed to the console, plus the same result
saved as JSON under data/.
"""

import json
import logging
import os
import sys
from datetime import datetime
from pathlib import Path

# Must be set before sentence-transformers (imported by query_services) loads,
# so it uses the local model cache only and never checks huggingface.co for
# updates -- otherwise every run makes network calls even with a warm cache.
os.environ.setdefault("HF_HUB_OFFLINE", "1")
os.environ.setdefault("TRANSFORMERS_OFFLINE", "1")

import query_services

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
log = logging.getLogger(__name__)

OUTPUT_DIR = Path(__file__).parent / "data"

# Default search origin: downtown San Francisco. query_services.query() ranks
# and filters by distance from this point; a generous radius keeps every SF
# neighborhood in range so a plain text prompt "just works" without also
# asking the user for a location.
DEFAULT_LAT = 37.7749
DEFAULT_LON = -122.4194
DEFAULT_RADIUS_MILES = 50.0
DEFAULT_LIMIT = 20


def print_answer(result: dict) -> None:
    """Print a human-readable answer to the user's search prompt."""
    if result.get("interpreted_query"):
        print(f"\n(Interpreting \"{result['query']}\" as \"{result['interpreted_query']}\")")
    print(f"\n=== Results for: \"{result['query']}\" ({result['count']} found) ===\n")
    if not result["results"]:
        print("No matching services found in the local Open Referral dataset.")
        return

    for svc in result["results"]:
        print(f"- {svc['service_name'] or svc['organization'] or '(unnamed service)'}")
        if svc["organization"] and svc["organization"] != svc["service_name"]:
            print(f"    Organization: {svc['organization']}")
        for loc in svc["locations"]:
            for addr in loc["addresses"]:
                bits = [addr.get("address_1", ""), addr.get("city", "")]
                addr_str = ", ".join(b for b in bits if b)
                if addr_str:
                    print(f"    Address: {addr_str} ({loc['distance_miles']} mi away)")
        if svc["phones"]:
            print(f"    Phone: {svc['phones'][0]}")
        if svc["eligibility"]:
            print(f"    Eligibility: {svc['eligibility'][:200]}")
        if svc["description"]:
            print(f"    {svc['description'][:200]}")
        print()


def save_result(result: dict) -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    out_path = OUTPUT_DIR / f"sf_shelters_query_{ts}.json"
    out_path.write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding="utf-8")
    log.info("Query result saved: %s", out_path)


def main():
    search_prompt = " ".join(sys.argv[1:]).strip()
    if not search_prompt:
        search_prompt = input(
            "What are you searching for? (e.g. 'family shelters', 'adult shelters for men'): "
        ).strip()

    if not search_prompt:
        log.error("No search prompt provided.")
        sys.exit(1)

    try:
        package_dir = query_services.find_latest_package()
    except FileNotFoundError as e:
        log.error(str(e))
        sys.exit(1)

    log.info("Searching local Open Referral dataset: %s", package_dir)
    dataset = query_services.load_dataset(package_dir)
    embeddings = query_services.build_or_load_service_embeddings(dataset, package_dir)

    result = query_services.query(
        search_prompt, DEFAULT_LAT, DEFAULT_LON,
        radius_miles=DEFAULT_RADIUS_MILES, limit=DEFAULT_LIMIT,
        package_dir=package_dir, dataset=dataset, service_embeddings=embeddings,
    )

    save_result(result)
    print_answer(result)


if __name__ == "__main__":
    main()
