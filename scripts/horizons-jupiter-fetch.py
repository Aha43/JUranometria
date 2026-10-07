#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the #472 Jovian contract
study: Jupiter (599) and Io, Europa, Ganymede and Callisto (501-504).
Every response is kept whole, exactly as returned, in a file whose first
lines record the request URL, the UTC time of the request and the
SHA-256 of the body. Nothing here is a build, test or runtime
dependency; this is run once, by hand, and its output becomes fixtures
once the owner has ruled which subset is committed.

Quantities requested (Horizons observer-table codes):
  1  astrometric RA/Dec, ICRF, light-time corrected, no aberration
  2  apparent RA/Dec of date (airless)
  4  apparent azimuth/elevation, airless, topocentric
  6  satellite X and Y from the primary (arcsec) and position angle
 10  illuminated fraction
 12  satellite angular separation from the primary and visibility code
 13  target angular diameter (equatorial, arcsec)
 17  north-pole position angle and distance
 20  observer range (AU) and range-rate
 24  Sun-Target-Observer phase angle
 32  north-pole RA and Dec (ICRF)
 43  phase angle and bisector

Output directory: $JOVIAN_HORIZONS_OUT, else docs/studies/jovian-system/horizons.
"""
import datetime as dt
import hashlib
import os
import sys
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://ssd.jpl.nasa.gov/api/horizons.api"
OUT = Path(os.environ.get("JOVIAN_HORIZONS_OUT")
           or Path(__file__).parent.parent / "docs/studies/jovian-system/horizons")
OUT.mkdir(parents=True, exist_ok=True)

# name: (east longitude deg, latitude deg, altitude km) - sea level, as
# the contract's observer stands (Place and Time carries no height).
OBSERVERS = {
    "oslo": (10.75, 59.91, 0.0),
    "quito": (281.5, -0.18, 0.0),
    "cape-town": (18.42, -33.93, 0.0),
    "alert": (297.66, 82.50, 0.0),
    "chatham": (183.5, -43.95, 0.0),
}
BODIES = {"jupiter": "599", "io": "501", "europa": "502",
          "ganymede": "503", "callisto": "504"}
JUPITER_QUANTITIES = "'1,2,4,10,13,17,20,24,32,43'"
MOON_QUANTITIES = "'1,2,4,6,12,13,20,24'"

NAMED = [
    ("1900-01-01 00:00:00", "boundary-start"),
    ("1962-01-01 00:00:00", "eop-record-begins"),
    ("1972-01-01 00:00:00", "first-leap-second"),
    ("2000-01-01 12:00:00", "j2000"),
    ("2017-01-01 00:00:00", "last-leap-second"),
    ("2026-01-10 00:00:00", "near-opposition-2026"),
    ("2026-06-21 10:00:00", "oslo-sample-row"),
    ("2026-10-07 12:00:00", "today"),
    ("2026-12-11 22:30:00", "triple-transit-before-io-enters"),
    ("2026-12-11 22:45:00", "triple-transit-inside"),
    ("2026-12-11 22:55:00", "triple-transit-callisto-centre-left-disc-overlapping"),
    ("2026-12-11 23:00:00", "triple-transit-after-callisto-leaves"),
    ("2100-12-31 23:59:59", "boundary-end"),
]
# Published configurations kept as additional candidates (Sky &
# Telescope, Joe Rao, 2024-12-05; Astronomy.com, 2025-12-01), geocentric.
PUBLISHED = [
    ("2024-12-20 04:49:00", "st-isosceles-triangle-east"),
    ("2024-12-28 23:34:00", "st-triple-lineup-west"),
    ("2025-01-06 07:01:00", "st-triple-lineup-east"),
    ("2025-02-26 05:39:00", "st-compact-triangle-west"),
    ("2025-03-23 02:27:00", "st-close-alignment-east"),
    ("2025-12-02 05:00:00", "astronomy-ganymede-shadow-transit"),
]


def query(body, params):
    q = {"format": "text", "COMMAND": f"'{body}'", "MAKE_EPHEM": "'YES'",
         "EPHEM_TYPE": "'OBSERVER'", "CENTER": "'coord@399'",
         "COORD_TYPE": "'GEODETIC'",
         "QUANTITIES": JUPITER_QUANTITIES if body == "599" else MOON_QUANTITIES,
         "EXTRA_PREC": "'YES'", "ANG_FORMAT": "'DEG'",
         "APPARENT": "'AIRLESS'", "CSV_FORMAT": "'YES'",
         "TIME_DIGITS": "'SECONDS'"}
    q.update(params)
    url = BASE + "?" + urllib.parse.urlencode(q, quote_via=urllib.parse.quote)
    when = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    with urllib.request.urlopen(url, timeout=600) as r:
        body_bytes = r.read()
    return url, when, body_bytes


def keep(name, url, when, body):
    digest = hashlib.sha256(body).hexdigest()
    text = body.decode("utf-8", "replace")
    ok = "$$SOE" in text and "$$EOE" in text
    header = (f"# JPL Horizons response, kept whole (#472)\n"
              f"# request-url: {url}\n# requested-utc: {when}\n"
              f"# body-sha256: {digest}\n# body-bytes: {len(body)}\n"
              f"# complete: {ok}\n")
    (OUT / f"{name}.txt").write_text(header + text, encoding="utf-8")
    rows = text.count("\n", text.find("$$SOE"), text.find("$$EOE")) if ok else -1
    print(f"{name}: {len(body)} bytes, rows={rows}, ok={ok}", flush=True)
    return ok


def main():
    failures = 0
    only_named = "--only-named" in sys.argv
    tlist = " ".join(f"'{t}'" for t, _ in NAMED)
    for site, (lon, lat, alt) in OBSERVERS.items():
        coord = f"'{lon},{lat},{alt}'"
        for label, body in BODIES.items():
            url, when, text = query(body, {"SITE_COORD": coord, "TLIST": tlist,
                                           "TLIST_TYPE": "'CAL'"})
            failures += not keep(f"named-{label}-{site}", url, when, text)
    if not only_named:
        # The matrices: every 7 days across the whole interval, Oslo
        # topocentric and geocentric, every body.
        for label, body in BODIES.items():
            url, when, text = query(body, {"SITE_COORD": "'10.75,59.91,0.0'",
                                           "START_TIME": "'1900-01-01 00:00'",
                                           "STOP_TIME": "'2100-12-31 00:00'",
                                           "STEP_SIZE": "'7 d'"})
            failures += not keep(f"matrix-7d-{label}-oslo", url, when, text)
            url, when, text = query(body, {"CENTER": "'500@399'",
                                           "START_TIME": "'1900-01-01 00:00'",
                                           "STOP_TIME": "'2100-12-31 00:00'",
                                           "STEP_SIZE": "'7 d'"})
            failures += not keep(f"matrix-7d-{label}-geocentric", url, when, text)
            # one dense year at Oslo, daily
            url, when, text = query(body, {"SITE_COORD": "'10.75,59.91,0.0'",
                                           "START_TIME": "'2026-01-01 00:00'",
                                           "STOP_TIME": "'2026-12-31 00:00'",
                                           "STEP_SIZE": "'1 d'"})
            failures += not keep(f"dense-2026-{label}-oslo", url, when, text)
            # December 2026 hourly, every body, Oslo: a month of events
            url, when, text = query(body, {"SITE_COORD": "'10.75,59.91,0.0'",
                                           "START_TIME": "'2026-12-01 00:00'",
                                           "STOP_TIME": "'2026-12-31 23:00'",
                                           "STEP_SIZE": "'1 h'"})
            failures += not keep(f"december-2026-hourly-{label}-oslo", url, when, text)
            # the named fixture evening, minute by minute, Oslo and geocentric
            for center, cname in (("'coord@399'", "oslo"), ("'500@399'", "geocentric")):
                params = {"CENTER": center, "START_TIME": "'2026-12-11 18:00'",
                          "STOP_TIME": "'2026-12-12 01:00'", "STEP_SIZE": "'1 m'"}
                if cname == "oslo":
                    params["SITE_COORD"] = "'10.75,59.91,0.0'"
                url, when, text = query(body, params)
                failures += not keep(f"triple-transit-2026-12-11-minutes-{label}-{cname}",
                                     url, when, text)
        # published configurations, geocentric, every body
        plist = " ".join(f"'{t}'" for t, _ in PUBLISHED)
        for label, body in BODIES.items():
            url, when, text = query(body, {"CENTER": "'500@399'", "TLIST": plist,
                                           "TLIST_TYPE": "'CAL'"})
            failures += not keep(f"published-{label}-geocentric", url, when, text)
    (OUT / "NAMED-CASES.txt").write_text(
        "\n".join(f"{t}  {label}" for t, label in NAMED) + "\n"
        + "\n".join(f"{t}  {label}  (published, geocentric)" for t, label in PUBLISHED)
        + "\n")
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
