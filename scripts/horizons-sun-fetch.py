#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the #398 checkpoint.

Every response is kept whole, exactly as returned, in a file whose first
lines record the request URL, the UTC time of the request and the
SHA-256 of the body. Nothing here is a build, test or runtime
dependency; this is run once, by hand, and its output becomes fixtures.

Quantities requested (Horizons observer-table codes):
  1  astrometric RA/Dec, ICRF, light-time corrected, no aberration
  2  apparent RA/Dec of date (airless)
  4  apparent azimuth/elevation, airless, topocentric
 13  target angular diameter (arcsec)
 20  observer range (AU) and range-rate
 29  constellation ID
 31  observer ecliptic longitude/latitude (of date)
"""
import datetime as dt
import hashlib
import sys
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://ssd.jpl.nasa.gov/api/horizons.api"
OUT = Path(__file__).parent / "horizons"
OUT.mkdir(exist_ok=True)

# name: (east longitude deg, latitude deg, altitude km)
OBSERVERS = {
    "oslo": (10.75, 59.91, 0.02),
    "quito": (281.5, -0.18, 2.85),
    "cape-town": (18.42, -33.93, 0.01),
    "alert": (297.66, 82.50, 0.03),
    "chatham": (183.5, -43.95, 0.02),
}

NAMED = [
    ("1900-01-01 00:00:00", "boundary-start"),
    ("1962-01-01 00:00:00", "eop-record-begins"),
    ("1972-01-01 00:00:00", "first-leap-second"),
    ("1992-10-13 00:00:00", "meeus-25a-civil-approx"),
    ("2000-01-01 12:00:00", "j2000"),
    ("2017-01-01 00:00:00", "last-leap-second"),
    ("2026-03-20 14:46:00", "march-equinox-2026"),
    ("2026-06-21 02:24:00", "june-solstice-2026"),
    ("2026-06-21 10:00:00", "oslo-sample-row"),
    ("2026-09-22 22:05:00", "september-equinox-2026"),
    ("2026-09-29 12:00:00", "today"),
    ("2026-12-21 20:50:00", "december-solstice-2026"),
    ("2100-12-31 23:59:59", "boundary-end"),
]


def query(params):
    q = {"format": "text", "COMMAND": "'10'", "MAKE_EPHEM": "'YES'",
         "EPHEM_TYPE": "'OBSERVER'", "CENTER": "'coord@399'",
         "COORD_TYPE": "'GEODETIC'", "QUANTITIES": "'1,2,4,13,20,29,31'",
         "EXTRA_PREC": "'YES'", "ANG_FORMAT": "'DEG'",
         "APPARENT": "'AIRLESS'", "CSV_FORMAT": "'YES'",
         "TIME_DIGITS": "'SECONDS'"}
    q.update(params)
    url = BASE + "?" + urllib.parse.urlencode(q, quote_via=urllib.parse.quote)
    when = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    with urllib.request.urlopen(url, timeout=300) as r:
        body = r.read()
    return url, when, body


def keep(name, url, when, body):
    digest = hashlib.sha256(body).hexdigest()
    text = body.decode("utf-8", "replace")
    ok = "$$SOE" in text and "$$EOE" in text
    header = (f"# JPL Horizons response, kept whole (#398)\n"
              f"# request-url: {url}\n# requested-utc: {when}\n"
              f"# body-sha256: {digest}\n# body-bytes: {len(body)}\n"
              f"# complete: {ok}\n")
    (OUT / f"{name}.txt").write_text(header + text, encoding="utf-8")
    rows = text.count("\n", text.find("$$SOE"), text.find("$$EOE")) if ok else -1
    print(f"{name}: {len(body)} bytes, rows={rows}, ok={ok}")
    return ok


def main():
    failures = 0
    for site, (lon, lat, alt) in OBSERVERS.items():
        coord = f"'{lon},{lat},{alt}'"
        # the named instants, one query per site, via TLIST
        tlist = " ".join(f"'{t}'" for t, _ in NAMED)
        url, when, body = query({"SITE_COORD": coord, "TLIST": tlist,
                                 "TLIST_TYPE": "'CAL'"})
        failures += not keep(f"named-{site}", url, when, body)
        # the matrix: every 30 days across the whole interval
        url, when, body = query({"SITE_COORD": coord,
                                 "START_TIME": "'1900-01-01 00:00'",
                                 "STOP_TIME": "'2100-12-31 00:00'",
                                 "STEP_SIZE": "'30 d'"})
        failures += not keep(f"matrix-30d-{site}", url, when, body)
    # one dense year at Oslo, daily, to see the within-year extremes
    url, when, body = query({"SITE_COORD": "'10.75,59.91,0.02'",
                             "START_TIME": "'2026-01-01 00:00'",
                             "STOP_TIME": "'2026-12-31 00:00'",
                             "STEP_SIZE": "'1 d'"})
    failures += not keep("dense-2026-oslo", url, when, body)
    # geocentric, for the Meeus 25.a case (geocentric apparent, TT)
    url, when, body = query({"CENTER": "'500@399'",
                             "TLIST": "'1992-10-13 00:00:00'",
                             "TLIST_TYPE": "'CAL'", "TIME_TYPE": "'TT'",
                             "QUANTITIES": "'1,2,13,20,31'"})
    failures += not keep("geocentric-meeus-25a-tt", url, when, body)
    (OUT / "NAMED-CASES.txt").write_text(
        "\n".join(f"{t}  {label}" for t, label in NAMED) + "\n")
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
