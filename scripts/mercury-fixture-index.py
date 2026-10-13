#!/usr/bin/env python3
"""Index the #528 Horizons responses: docs/studies/mercury/horizons/README.md
for the committed fixtures and docs/studies/mercury/horizons-kept.md for every
response kept, in Git or outside it, with its URL, request time and digest.
Run by hand from the repository root after a fetch; re-hashes every body."""
import hashlib
import os
from pathlib import Path

KEPT = Path(os.path.expanduser(os.environ.get("MERCURY_HORIZONS", "~/juranometria-runs/mercury-horizons")))
COMMITTED = Path("docs/studies/mercury/horizons")


def meta(path):
    text = path.read_text(encoding="utf-8")
    lines = text.splitlines(keepends=True)
    head = {}
    for line in lines[:6]:
        key, _, value = line[2:].strip().partition(": ")
        head[key] = value
    body = "".join(lines[6:]).encode("utf-8")
    if hashlib.sha256(body).hexdigest() != head["body-sha256"]:
        raise SystemExit(f"{path}: body does not match its recorded digest")
    rows = text.count("\n", text.find("$$SOE"), text.find("$$EOE")) - 1
    return head, rows


readme = ["# JPL Horizons responses for Mercury (#528)", "",
          "Each file is kept whole, exactly as returned, headed by its request URL with every query parameter,"
          " the UTC of the request and the SHA-256 of the body. Fetched by `scripts/horizons-mercury-fetch.py`"
          " (observers at sea level; Mercury 199 with quantities 1,2,4,9,10,13,19,20,23,24,43; the Sun 10 with"
          " 1,2,4,20 on the same grids); indexed by `scripts/mercury-fixture-index.py`. Study evidence only:"
          " nothing at build, test or run time depends on them beyond the tests reading them as fixtures, and"
          " the atlas never reaches Horizons.", "",
          "Committed here (#528): three lists of named instants - the interval's boundaries and the record's"
          " with Espenak's conjunctions (`named-*`, 25), the greatest elongations with the two lunar occultations"
          " (`elongations-*`, 20) and the 28 transits of 1907-2098 (`transits-*`) - and the four representative"
          " appearances of the 2026 evening and morning apparitions, each for all five observers and geocentrically"
          " (Mercury and Sun); the hourly window through the 2026-03-07 inferior conjunction and the six-hourly"
          " window around the 2026-04-03 greatest western elongation at Oslo (Mercury and Sun); the 2026 apparitions"
          " every 3 days from 25 January to 25 April at Oslo (Mercury and Sun); and the 7-day 1900-2100 matrix at"
          " Oslo (Mercury). Three lists where Venus had one: Horizons' gateway refuses a request beyond about"
          " 1 900 characters, so no list carries more than 32 instants. The geocentric 7-day matrix is retained"
          " outside the repository in the durable study record, listed with its URL and digest in"
          " `../horizons-kept.md` and measured in `../authority-comparison.md`.", "",
          "| file | bytes | rows | requested (UTC) | body sha256 |", "|---|---:|---:|---|---|"]
for p in sorted(COMMITTED.glob("*.txt")):
    head, rows = meta(p)
    readme.append(f"| `{p.name}` | {head['body-bytes']} | {rows} | {head['requested-utc']} | `{head['body-sha256']}` |")
(COMMITTED / "README.md").write_text("\n".join(readme) + "\n", encoding="utf-8")

kept = sorted(KEPT.glob("*.txt"))
total = 0
table = []
for p in kept:
    head, rows = meta(p)
    total += int(head["body-bytes"])
    table.append(f"| `{p.name}` | {'yes' if (COMMITTED / p.name).exists() else 'no'} | {head['body-bytes']} | {rows} |"
                 f" {head['requested-utc']} | `{head['body-sha256']}` | <{head['request-url']}> |")
out = ["# Every Horizons response the #528 study kept", "",
       "The whole set fetched by `scripts/horizons-mercury-fetch.py` (the main run, `--appearances` and"
       " `--apparition`) on 2026-10-13: the committed subset is in `horizons/`; the rest is retained outside"
       " Git in the durable study record (`~/juranometria-runs/mercury-horizons/`), each file kept whole with"
       " this header. The URL asks the same question again; the digest is of the body as returned.", "",
       f"{len(kept)} responses, {total:,} body bytes.", "",
       "| file | in Git | bytes | rows | requested (UTC) | body sha256 | request URL |",
       "|---|---|---:|---:|---|---|---|"] + table
Path("docs/studies/mercury/horizons-kept.md").write_text("\n".join(out) + "\n", encoding="utf-8")
print(f"{len(list(COMMITTED.glob('*.txt')))} committed, {len(kept)} kept")
