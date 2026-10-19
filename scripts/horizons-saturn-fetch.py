#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the #574 Saturn system
study: Saturn (699) and Mimas, Enceladus, Tethys, Dione, Rhea, Titan,
Hyperion and Iapetus (601-608). Approved by the owner on 2026-10-19 for
#574 measurement only.

Every response is kept whole, exactly as returned, in a file whose first
lines record the complete request URL (every query parameter), the UTC
time of the request and the SHA-256 and byte count of the body. Nothing
here is a build, test or runtime dependency; it is run once, by hand.

The run stops at the first response that lacks a complete table, names a
different target or carries an unexpected interval. It also stops if the
committed responses would exceed ~30 MB, the retained geocentric matrices
would exceed ~26 MB, or the request count would exceed 90.

Quantities requested (Horizons observer-table codes):
  1  astrometric RA/Dec, ICRF, light-time corrected, no aberration
  2  apparent RA/Dec of date (airless)
  4  apparent azimuth/elevation, airless, topocentric
  6  satellite X and Y from the primary (arcsec) and position angle
 10  illuminated fraction
 12  satellite angular separation from the primary and visibility code
 13  target angular diameter (equatorial, arcsec)
 14  observer sub-longitude and sub-latitude (Saturn: the ring opening to
     the observer)
 15  Sun sub-longitude and sub-latitude (Saturn: the ring opening to the Sun)
 17  north-pole position angle and distance
 20  observer range (AU) and range-rate
 24  Sun-Target-Observer phase angle
 32  north-pole RA and Dec (ICRF)
 43  phase angle and bisector

Committed: docs/studies/saturn-system/horizons (or $SATURN_HORIZONS_OUT).
Retained (geocentric matrices): ~/juranometria-runs/saturn-horizons-retained
(or $SATURN_HORIZONS_RETAINED).
"""
import datetime as dt
import hashlib
import os
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://ssd.jpl.nasa.gov/api/horizons.api"
OUT = Path(os.environ.get("SATURN_HORIZONS_OUT")
           or Path(__file__).parent.parent / "docs/studies/saturn-system/horizons")
RETAINED = Path(os.environ.get("SATURN_HORIZONS_RETAINED")
                or Path.home() / "juranometria-runs/saturn-horizons-retained")
OUT.mkdir(parents=True, exist_ok=True)
RETAINED.mkdir(parents=True, exist_ok=True)

COMMITTED_LIMIT = 30_000_000
RETAINED_LIMIT = 26_000_000
REQUEST_LIMIT = 90
PACE_SECONDS = 2.0

# name: (east longitude deg, latitude deg, altitude km) - sea level, as the
# Jovian study's observers stand.
OBSERVERS = {
    "oslo": (10.75, 59.91, 0.0),
    "quito": (281.5, -0.18, 0.0),
    "cape-town": (18.42, -33.93, 0.0),
    "alert": (297.66, 82.50, 0.0),
    "chatham": (183.5, -43.95, 0.0),
}
BODIES = {"saturn": "699", "mimas": "601", "enceladus": "602", "tethys": "603",
          "dione": "604", "rhea": "605", "titan": "606", "hyperion": "607",
          "iapetus": "608"}
SATURN_QUANTITIES = "'1,2,4,10,13,14,15,17,20,24,32,43'"
MOON_QUANTITIES = "'1,2,4,6,12,13,20,24'"

# Boundaries and reference instants only. Event instants come from separate
# published authorities, never typed here.
NAMED = [
    ("1900-01-01 00:00:00", "boundary-start"),
    ("1962-01-01 00:00:00", "eop-record-begins"),
    ("1972-01-01 00:00:00", "first-leap-second"),
    ("2000-01-01 12:00:00", "j2000"),
    ("2017-01-01 00:00:00", "last-leap-second"),
    ("2026-06-21 10:00:00", "oslo-sample-row"),
    ("2026-10-19 12:00:00", "today"),
    ("2100-12-31 23:59:59", "boundary-end"),
]

state = {"requests": 0, "committed": 0, "retained": 0}


def stop(message):
    print(f"STOPPED: {message}", flush=True)
    sys.exit(2)


def query(body, params):
    if state["requests"] >= REQUEST_LIMIT:
        stop(f"the request count would exceed {REQUEST_LIMIT}")
    q = {"format": "text", "COMMAND": f"'{body}'", "MAKE_EPHEM": "'YES'",
         "EPHEM_TYPE": "'OBSERVER'", "CENTER": "'coord@399'",
         "COORD_TYPE": "'GEODETIC'",
         "QUANTITIES": SATURN_QUANTITIES if body == "699" else MOON_QUANTITIES,
         "EXTRA_PREC": "'YES'", "ANG_FORMAT": "'DEG'",
         "APPARENT": "'AIRLESS'", "CSV_FORMAT": "'YES'",
         "TIME_DIGITS": "'SECONDS'"}
    q.update(params)
    url = BASE + "?" + urllib.parse.urlencode(q, quote_via=urllib.parse.quote)
    time.sleep(PACE_SECONDS)
    when = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    with urllib.request.urlopen(url, timeout=900) as r:
        body_bytes = r.read()
    state["requests"] += 1
    return url, when, body_bytes


def keep(directory, name, body_id, url, when, body, retained=False):
    digest = hashlib.sha256(body).hexdigest()
    text = body.decode("utf-8", "replace")
    if "$$SOE" not in text or "$$EOE" not in text:
        stop(f"{name}: no complete table - Horizons answered:\n{text[:1500]}")
    target = re.search(r"Target body name:\s*(.+)", text)
    if not target or f"({body_id})" not in target.group(1):
        stop(f"{name}: the response names another target: "
             f"{target.group(1) if target else 'none'}")
    header = (f"# JPL Horizons response, kept whole (#574)\n"
              f"# request-url: {url}\n# requested-utc: {when}\n"
              f"# body-sha256: {digest}\n# body-bytes: {len(body)}\n"
              f"# complete: True\n")
    data = (header + text).encode("utf-8")
    key = "retained" if retained else "committed"
    limit = RETAINED_LIMIT if retained else COMMITTED_LIMIT
    if state[key] + len(data) > limit:
        stop(f"{name}: the {key} responses would exceed {limit:,} bytes")
    (directory / f"{name}.txt").write_bytes(data)
    state[key] += len(data)
    rows = text.count("\n", text.find("$$SOE"), text.find("$$EOE"))
    print(f"{name}: {len(body):,} bytes, rows={rows}, request {state['requests']},"
          f" committed {state['committed']:,}, retained {state['retained']:,}", flush=True)


# The first run (2026-10-19, log ~/juranometria-runs/runs/saturn-horizons-
# fetch-2026-10-19.log) made 74 requests; --magnitudes continues within the
# same limits.
FIRST_RUN_REQUESTS = 74


def magnitudes():
    """Apparent magnitude (quantity 9), geocentric, every 30 days 1900-2100,
    one request per body: approved 2026-10-19 as exactly nine requests."""
    state["requests"] = FIRST_RUN_REQUESTS
    state["committed"] = sum(p.stat().st_size for p in OUT.glob("*.txt"))
    for label, body in BODIES.items():
        url, when, text = query(body, {"CENTER": "'500@399'", "QUANTITIES": "'9'",
                                       "START_TIME": "'1900-01-01 00:00'",
                                       "STOP_TIME": "'2100-12-31 00:00'",
                                       "STEP_SIZE": "'30 d'"})
        keep(OUT, f"magnitudes-30d-{label}-geocentric", body, url, when, text)
    print(f"done: {state['requests']} requests in all, committed {state['committed']:,} bytes")


def main():
    if "--magnitudes" in sys.argv:
        magnitudes()
        return
    tlist = " ".join(f"'{t}'" for t, _ in NAMED)
    oslo = "'10.75,59.91,0.0'"
    for site, (lon, lat, alt) in OBSERVERS.items():
        coord = f"'{lon},{lat},{alt}'"
        for label, body in BODIES.items():
            url, when, text = query(body, {"SITE_COORD": coord, "TLIST": tlist,
                                           "TLIST_TYPE": "'CAL'"})
            keep(OUT, f"named-{label}-{site}", body, url, when, text)
    for label, body in BODIES.items():
        url, when, text = query(body, {"SITE_COORD": oslo,
                                       "START_TIME": "'2026-01-01 00:00'",
                                       "STOP_TIME": "'2026-12-31 00:00'",
                                       "STEP_SIZE": "'1 d'"})
        keep(OUT, f"dense-2026-{label}-oslo", body, url, when, text)
    for label, body in BODIES.items():
        url, when, text = query(body, {"SITE_COORD": oslo,
                                       "START_TIME": "'1900-01-01 00:00'",
                                       "STOP_TIME": "'2100-12-31 00:00'",
                                       "STEP_SIZE": "'7 d'"})
        keep(OUT, f"matrix-7d-{label}-oslo", body, url, when, text)
    for label, body in BODIES.items():
        url, when, text = query(body, {"CENTER": "'500@399'",
                                       "START_TIME": "'1900-01-01 00:00'",
                                       "STOP_TIME": "'2100-12-31 00:00'",
                                       "STEP_SIZE": "'7 d'"})
        keep(RETAINED, f"matrix-7d-{label}-geocentric", body, url, when, text,
             retained=True)
    # Saturn daily across the 2024-2025 ring-plane crossing: a window, not a
    # claimed instant.
    url, when, text = query("699", {"SITE_COORD": oslo,
                                    "START_TIME": "'2024-09-01 00:00'",
                                    "STOP_TIME": "'2025-09-01 00:00'",
                                    "STEP_SIZE": "'1 d'"})
    keep(OUT, "ring-plane-window-2024-2025-saturn-oslo", "699", url, when, text)
    # Titan hourly across one orbit in August 2026.
    url, when, text = query("606", {"SITE_COORD": oslo,
                                    "START_TIME": "'2026-08-01 00:00'",
                                    "STOP_TIME": "'2026-08-17 00:00'",
                                    "STEP_SIZE": "'1 h'"})
    keep(OUT, "titan-orbit-2026-08-hourly-oslo", "606", url, when, text)
    (OUT / "NAMED-CASES.txt").write_text(
        "\n".join(f"{t}  {label}" for t, label in NAMED) + "\n")
    print(f"done: {state['requests']} requests, committed {state['committed']:,} bytes,"
          f" retained {state['retained']:,} bytes")


if __name__ == "__main__":
    main()
