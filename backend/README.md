# backend

Prototype for the search-resources feature: a semantic, free-text search over
San Francisco human-services data (shelters, food, financial assistance,
etc.), returned as ranked, geo-filtered results a mobile client can render
directly.

Data follows the [Open Referral Human Services Data Specification
(HSDS)](https://openreferral.org/) -- a standard schema for describing
services as related tables (organizations, services, locations, ...) -- so
the query engine isn't shelter-specific and works over any HSDS-compliant
dataset.

## Pipeline

| Script | Role |
|---|---|
| `export_openreferral_csv.py` | Converts a scraped JSON bundle (`data/sf_family_shelters_hsds_*.json`) into a multi-file HSDS CSV package under `data/hsds_csv_<timestamp>/`. |
| `query_services.py` | Loads an HSDS CSV package, embeds every service with `sentence-transformers`, and ranks/filters services against a free-text prompt and a lat/lon + radius. Callable as a library (`query()`) or as a CLI. |
| `scrape_shelters.py` | Interactive/CLI entry point: takes a free-text prompt (arg or stdin), runs it through `query_services.query()` against the newest local HSDS package, prints the results, and saves them as JSON under `data/`. |

`query_services.py` is the reusable core -- in a real backend, load the
dataset and embeddings once at startup and call `query()` per request rather
than going through the CLI.

## Setup

```bash
cd backend
python -m venv .venv
.venv\Scripts\activate      # Windows
pip install -r requirements.txt
```

The embedding model (`sentence-transformers/all-MiniLM-L6-v2`) downloads
once on first use and is cached under `~/.cache/huggingface`; every run
after that is fully offline (`HF_HUB_OFFLINE=1` / `TRANSFORMERS_OFFLINE=1`
are set automatically).

## Running a search

A sample HSDS package with prebuilt embeddings is checked in at
`data/hsds_csv_sample/`, so search works immediately without scraping or
exporting anything first:

```bash
python scrape_shelters.py "family shelters"
# or
python query_services.py "help paying rent this month" --lat 37.7749 --lon -122.4194
```

Both scripts operate on the newest `data/hsds_csv_*/` package found on disk.
To regenerate the CSV package and embedding index from a fresh scrape:

```bash
python export_openreferral_csv.py path/to/scraped_bundle.json
python query_services.py --build-index
```

## Data

`data/hsds_csv_sample/` is checked in as a fixture so the query engine works
out of the box. Everything else under `data/` (fresh scrapes, exported CSV
packages, saved query results) is generated at runtime and gitignored --
see `.gitignore`.
