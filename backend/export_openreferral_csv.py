"""
Step 2: Export the scraped HSDS JSON bundle into the Open Referral
Human Services Data Specification (HSDS) multi-file CSV format.

Open Referral / HSDS (openreferral.org) is an open data standard that
describes services as a set of related tables (organizations, services,
locations, ...) joined by uuid foreign keys, so any HSDS-compliant tool
can import the data without custom parsing. The canonical distribution
is one CSV file per table -- this script produces that layout from the
single nested JSON bundle that scrape_shelters.py writes.

Input:  newest data/sf_family_shelters_hsds_*.json (or a path passed as argv[1])
Output: data/hsds_csv_<timestamp>/ containing one CSV per HSDS table
"""

import csv
import json
import sys
from datetime import datetime
from pathlib import Path

DATA_DIR = Path(__file__).parent / "data"


def find_latest_bundle() -> Path:
    candidates = sorted(DATA_DIR.glob("sf_family_shelters_hsds_*.json"))
    if not candidates:
        sys.exit("No HSDS JSON bundle found in data/. Run scrape_shelters.py first.")
    return candidates[-1]


def write_csv(path: Path, fieldnames: list[str], rows: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in rows:
            writer.writerow({k: row.get(k, "") for k in fieldnames})


def split_taxonomy(service_attributes: list[dict]) -> tuple[list[dict], list[dict]]:
    """HSDS keeps taxonomy terms and their assignment to services in two
    separate tables; the scraper bundles both together as service_attributes."""
    terms_seen: dict[str, dict] = {}
    service_taxonomy = []
    for attr in service_attributes:
        term = attr["taxonomy_term"]
        terms_seen[term["id"]] = {
            "id": term["id"],
            "name": term["name"],
            "taxonomy": term.get("taxonomy", ""),
            "parent_id": "",
        }
        service_taxonomy.append({
            "id": attr["id"],
            "service_id": attr["service_id"],
            "taxonomy_term_id": term["id"],
        })
    return list(terms_seen.values()), service_taxonomy


def main():
    src = Path(sys.argv[1]) if len(sys.argv) > 1 else find_latest_bundle()
    bundle = json.loads(src.read_text(encoding="utf-8"))

    out_dir = DATA_DIR / f"hsds_csv_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
    out_dir.mkdir(parents=True, exist_ok=True)

    write_csv(
        out_dir / "organizations.csv",
        ["id", "name", "alternate_name", "description", "email", "url",
         "tax_status", "year_incorporated", "legal_status"],
        [{**o, "url": o.get("website", "")} for o in bundle["organizations"]],
    )

    write_csv(
        out_dir / "services.csv",
        ["id", "organization_id", "name", "alternate_name", "description",
         "url", "email", "status", "interpretation_services",
         "application_process", "fees_description", "wait_time",
         "accreditations", "eligibility_description", "minimum_age",
         "maximum_age", "required_documents", "notes"],
        bundle["services"],
    )

    write_csv(
        out_dir / "locations.csv",
        ["id", "organization_id", "name", "alternate_name", "description",
         "latitude", "longitude", "transportation"],
        bundle["locations"],
    )

    write_csv(
        out_dir / "addresses.csv",
        ["id", "location_id", "attention", "address_1", "address_2", "city",
         "region", "state_province", "postal_code", "country", "address_type"],
        bundle["addresses"],
    )

    write_csv(
        out_dir / "phones.csv",
        ["id", "location_id", "service_id", "organization_id", "number",
         "extension", "type", "description"],
        bundle["phones"],
    )

    write_csv(
        out_dir / "service_at_locations.csv",
        ["id", "service_id", "location_id", "description"],
        bundle["service_at_locations"],
    )

    taxonomy_terms, service_taxonomy = split_taxonomy(bundle["service_attributes"])
    write_csv(out_dir / "taxonomy_terms.csv",
              ["id", "name", "taxonomy", "parent_id"], taxonomy_terms)
    write_csv(out_dir / "service_taxonomy.csv",
              ["id", "service_id", "taxonomy_term_id"], service_taxonomy)

    print(f"HSDS CSV package written to {out_dir}")
    for csv_file in sorted(out_dir.glob("*.csv")):
        print(f"  {csv_file.name}")


if __name__ == "__main__":
    main()
