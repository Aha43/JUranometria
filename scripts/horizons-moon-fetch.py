#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the Moon (#406, #407).

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
OUT = Path(__file__).parent.parent / "docs/studies/solar-system/horizons-moon"
OUT.mkdir(exist_ok=True)

# name: (east longitude deg, latitude deg, altitude km). The altitude
# is 0: the contract's observer stands on the WGS 84 ellipsoid at sea
# level (Place and Time carries no height), and the Moon's parallax is
# large enough that Quito's real 2.85 km would show as 1.6 arcseconds
# - which the first Moon fetch, made with the Sun's site heights,
# measured before these responses replaced it (issue #407).
OBSERVERS = {
    "oslo": (10.75, 59.91, 0.0),
    "quito": (281.5, -0.18, 0.0),
    "cape-town": (18.42, -33.93, 0.0),
    "alert": (297.66, 82.50, 0.0),
    "chatham": (183.5, -43.95, 0.0),
}

# The seasonal instants are never typed here: they are read from the
# cited fixture, so the requests, the table and the contract share one
# source (owner checkpoint on #399).
SEASONS = Path(__file__).parent.parent / "docs/studies/solar-system/seasons-2026.txt"


def seasons():
    events = {}
    for line in SEASONS.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        name, iso, _ = line.split("\t")
        events[name.strip()] = iso.strip().replace("T", " ").rstrip("Z")
    return events


_S = seasons()
NAMED = [
    ("1900-01-01 00:00:00", "boundary-start"),
    ("1962-01-01 00:00:00", "eop-record-begins"),
    ("1972-01-01 00:00:00", "first-leap-second"),
    ("1992-04-12 00:00:00", "meeus-47a-civil-approx"),
    ("2000-01-01 12:00:00", "j2000"),
    ("2017-01-01 00:00:00", "last-leap-second"),
    (_S["march-equinox"], "march-equinox-2026-imcce"),
    (_S["june-solstice"], "june-solstice-2026-imcce"),
    ("2026-06-21 10:00:00", "oslo-sample-row"),
    (_S["september-equinox"], "september-equinox-2026-imcce"),
    ("2026-09-29 12:00:00", "today"),
    (_S["december-solstice"], "december-solstice-2026-imcce"),
    ("2100-12-31 23:59:59", "boundary-end"),
]


def query(params):
    q = {"format": "text", "COMMAND": "'301'", "MAKE_EPHEM": "'YES'",
         "EPHEM_TYPE": "'OBSERVER'", "CENTER": "'coord@399'",
         "COORD_TYPE": "'GEODETIC'", "QUANTITIES": "'1,2,4,10,13,14,20,23,24,31,43'",
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
    header = (f"# JPL Horizons response, kept whole (#406)\n"
              f"# request-url: {url}\n# requested-utc: {when}\n"
              f"# body-sha256: {digest}\n# body-bytes: {len(body)}\n"
              f"# complete: {ok}\n")
    (OUT / f"{name}.txt").write_text(header + text, encoding="utf-8")
    rows = text.count("\n", text.find("$$SOE"), text.find("$$EOE")) if ok else -1
    print(f"{name}: {len(body)} bytes, rows={rows}, ok={ok}")
    return ok


def main():
    failures = 0
    only_named = "--only-named" in sys.argv
    for site, (lon, lat, alt) in OBSERVERS.items():
        coord = f"'{lon},{lat},{alt}'"
        # the named instants, one query per site, via TLIST
        tlist = " ".join(f"'{t}'" for t, _ in NAMED)
        url, when, body = query({"SITE_COORD": coord, "TLIST": tlist,
                                 "TLIST_TYPE": "'CAL'"})
        failures += not keep(f"named-{site}", url, when, body)
        if only_named:
            continue
        # the matrix: every 30 days across the whole interval
        url, when, body = query({"SITE_COORD": coord,
                                 "START_TIME": "'1900-01-01 00:00'",
                                 "STOP_TIME": "'2100-12-31 00:00'",
                                 "STEP_SIZE": "'7 d'"})
        failures += not keep(f"matrix-7d-{site}", url, when, body)
    if only_named:
        (OUT / "NAMED-CASES.txt").write_text(
            "\n".join(f"{t}  {label}" for t, label in NAMED) + "\n")
        print(f"failures={failures}")
        sys.exit(1 if failures else 0)
    # one dense year at Oslo, daily, to see the within-year extremes
    url, when, body = query({"SITE_COORD": "'10.75,59.91,0.0'",
                             "START_TIME": "'2026-01-01 00:00'",
                             "STOP_TIME": "'2026-12-31 00:00'",
                             "STEP_SIZE": "'1 d'"})
    failures += not keep("dense-2026-oslo", url, when, body)
    # the Sun on the same Oslo grid, so the bright-limb angle can be
    # held to Meeus 48.5 applied to Horizons' own Sun and Moon
    url, when, body = query({"COMMAND": "'10'", "SITE_COORD": "'10.75,59.91,0.0'",
                             "START_TIME": "'1900-01-01 00:00'",
                             "STOP_TIME": "'2100-12-31 00:00'",
                             "STEP_SIZE": "'7 d'", "QUANTITIES": "'1,2,4,20,31'"})
    failures += not keep("sun-matrix-7d-oslo", url, when, body)
    # geocentric, for the Meeus 47.a case (geocentric apparent, TT)
    url, when, body = query({"CENTER": "'500@399'",
                             "TLIST": "'1992-04-12 00:00:00'",
                             "TLIST_TYPE": "'CAL'", "TIME_TYPE": "'TT'",
                             "QUANTITIES": "'1,2,10,13,20,23,24,31,43'"})
    failures += not keep("geocentric-meeus-47a-tt", url, when, body)
    (OUT / "NAMED-CASES.txt").write_text(
        "\n".join(f"{t}  {label}" for t, label in NAMED) + "\n")
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
