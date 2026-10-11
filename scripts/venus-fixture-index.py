#!/usr/bin/env python3
"""Index the #500 Horizons responses: docs/studies/venus/horizons/README.md
for the committed fixtures and docs/studies/venus/horizons-kept.md for every
response kept, in Git or outside it, with its URL, request time and digest.
Run by hand from the repository root after a fetch; re-hashes every body."""
import hashlib
import os
from pathlib import Path

KEPT = Path(os.path.expanduser(os.environ.get("VENUS_HORIZONS", "~/juranometria-runs/venus-horizons")))
COMMITTED = Path("docs/studies/venus/horizons")


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


readme = ["# JPL Horizons responses for Venus (#500)", "",
          "Each file is kept whole, exactly as returned, headed by its request URL with every query parameter,"
          " the UTC of the request and the SHA-256 of the body. Fetched by `scripts/horizons-venus-fetch.py`"
          " (observers at sea level; Venus 299 with quantities 1,2,4,9,10,13,19,20,23,24,43; the Sun 10 with"
          " 1,2,4,20 on the same grids); indexed by `scripts/venus-fixture-index.py`. Study evidence only:"
          " nothing at build, test or run time depends on them beyond the tests reading them as fixtures, and"
          " the atlas never reaches Horizons.", "",
          "Committed here, as ruled on #500: the named instants and the four representative appearances for all"
          " five observers and geocentrically (Venus and Sun), the hourly window through the 2026 inferior"
          " conjunction at Oslo (Venus and Sun), the fortnightly 2026-27 apparition at Oslo (Venus and Sun) and"
          " the 7-day 1900-2100 matrix at Oslo (Venus). The other observers' long matrices, the geocentric"
          " matrices and the other dense windows are retained outside the repository in the durable study"
          " record, listed with their URLs and digests in `../horizons-kept.md` and measured in"
          " `../authority-comparison.md`.", "",
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
out = ["# Every Horizons response the #500 study kept", "",
       "The whole set fetched by `scripts/horizons-venus-fetch.py` (the main run, `--appearances` and"
       " `--apparition`) on 2026-10-11: the committed subset is in `horizons/`; the rest is retained outside"
       " Git in the durable study record (`~/juranometria-runs/venus-horizons/`), each file kept whole with"
       " this header. The URL asks the same question again; the digest is of the body as returned.", "",
       f"{len(kept)} responses, {total:,} body bytes.", "",
       "| file | in Git | bytes | rows | requested (UTC) | body sha256 | request URL |",
       "|---|---|---:|---:|---|---|---|"] + table
Path("docs/studies/venus/horizons-kept.md").write_text("\n".join(out) + "\n", encoding="utf-8")
print(f"{len(list(COMMITTED.glob('*.txt')))} committed, {len(kept)} kept")
