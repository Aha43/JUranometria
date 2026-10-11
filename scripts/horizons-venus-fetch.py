#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the Venus contract study (#500).

Every response is kept whole, exactly as returned, in a file whose first
lines record the request URL, the UTC time of the request and the SHA-256
of the body. Nothing here is a build, test or runtime dependency; it is run
once, by hand. The bulk is written outside the repository
($VENUS_HORIZONS_OUT, else ~/juranometria-runs/venus-horizons); only the
subset the owner rules becomes committed fixtures.

Quantities requested (Horizons observer-table codes):
  1  astrometric RA/Dec, ICRF, light-time corrected, no aberration
  2  apparent RA/Dec of date (airless)
  4  apparent azimuth/elevation, airless, topocentric
  9  visual magnitude and surface brightness
 10  illuminated fraction
 13  target angular diameter (arcsec)
 19  heliocentric range and range-rate
 20  observer range (AU) and range-rate
 23  Sun-Observer-Target elongation and leading/trailing flag
 24  Sun-Target-Observer phase angle
 43  phase angle and its bisector
The Sun (10) is fetched on the same grids for elongation and bright-limb
geometry checked independently.
"""
import datetime as dt
import hashlib
import os
import sys
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://ssd.jpl.nasa.gov/api/horizons.api"
OUT = Path(os.environ.get("VENUS_HORIZONS_OUT")
           or Path.home() / "juranometria-runs/venus-horizons")
OUT.mkdir(parents=True, exist_ok=True)
ROOT = Path(__file__).parent.parent

# The established observers, at sea level as the contract's observer stands.
OBSERVERS = {
    "oslo": (10.75, 59.91, 0.0),
    "quito": (281.5, -0.18, 0.0),
    "cape-town": (18.42, -33.93, 0.0),
    "alert": (297.66, 82.50, 0.0),
    "chatham": (183.5, -43.95, 0.0),
}
VENUS_Q = "'1,2,4,9,10,13,19,20,23,24,43'"
SUN_Q = "'1,2,4,20'"


def events():
    out = []
    for line in (ROOT / "docs/studies/venus/venus-events.txt").read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.startswith("#"):
            name, iso, _ = line.split("\t")
            out.append((iso.replace("T", " ").replace("Z", ":00"), name))
    return out


NAMED = [
    ("1900-01-01 00:00:00", "boundary-start"),
    ("1962-01-01 00:00:00", "eop-record-begins"),
    ("1972-01-01 00:00:00", "first-leap-second"),
    ("2000-01-01 12:00:00", "j2000"),
    ("2017-01-01 00:00:00", "last-leap-second"),
    ("2027-06-28 00:00:00", "exact-interval-end"),
    ("2100-12-31 23:59:59", "boundary-end"),
] + events()


# Representative appearances of the 2026-27 apparition, each the weekly
# Oslo row (matrix-7d-oslo, Horizons' own Illu%) nearest a quarter or three
# quarters lit on either side of the 2026 inferior conjunction: the named
# set has half-lit, near-full and near-conjunction cases but no crescent
# or gibbous one (#500). Fetched on their own by --appearances.
APPEARANCES = [
    ("2026-06-15 00:00:00", "gibbous-evening"),
    ("2026-09-21 00:00:00", "crescent-evening"),
    ("2026-11-30 00:00:00", "crescent-morning"),
    ("2027-03-08 00:00:00", "gibbous-morning"),
]


def query(body, params):
    q = {"format": "text", "COMMAND": f"'{body}'", "MAKE_EPHEM": "'YES'",
         "EPHEM_TYPE": "'OBSERVER'", "CENTER": "'coord@399'",
         "COORD_TYPE": "'GEODETIC'", "QUANTITIES": VENUS_Q if body == "299" else SUN_Q,
         "EXTRA_PREC": "'YES'", "ANG_FORMAT": "'DEG'", "APPARENT": "'AIRLESS'",
         "CSV_FORMAT": "'YES'", "TIME_DIGITS": "'SECONDS'"}
    q.update(params)
    url = BASE + "?" + urllib.parse.urlencode(q, quote_via=urllib.parse.quote)
    when = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    with urllib.request.urlopen(url, timeout=600) as r:
        return url, when, r.read()


def keep(name, url, when, body):
    digest = hashlib.sha256(body).hexdigest()
    text = body.decode("utf-8", "replace")
    ok = "$$SOE" in text and "$$EOE" in text
    header = (f"# JPL Horizons response, kept whole (#500)\n# request-url: {url}\n"
              f"# requested-utc: {when}\n# body-sha256: {digest}\n"
              f"# body-bytes: {len(body)}\n# complete: {ok}\n")
    (OUT / f"{name}.txt").write_text(header + text, encoding="utf-8")
    rows = text.count("\n", text.find("$$SOE"), text.find("$$EOE")) - 1 if ok else -1
    print(f"{name}: {len(body)} bytes, rows={rows}, ok={ok}", flush=True)
    return ok


def window(iso, before_days, after_days):
    t = dt.datetime.fromisoformat(iso)
    return ((t - dt.timedelta(days=before_days)).strftime("%Y-%m-%d %H:%M"),
            (t + dt.timedelta(days=after_days)).strftime("%Y-%m-%d %H:%M"))


def appearances():
    failures = 0
    tlist = " ".join(f"'{t}'" for t, _ in APPEARANCES)
    for site, (lon, lat, alt) in OBSERVERS.items():
        for body in ("299", "10"):
            url, when, b = query(body, {"SITE_COORD": f"'{lon},{lat},{alt}'", "TLIST": tlist,
                                        "TLIST_TYPE": "'CAL'"})
            failures += not keep(f"appearances-{site}-{body}", url, when, b)
    for body in ("299", "10"):
        url, when, b = query(body, {"CENTER": "'500@399'", "TLIST": tlist, "TLIST_TYPE": "'CAL'"})
        failures += not keep(f"appearances-geocentric-{body}", url, when, b)
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


def apparition():
    """Every 14 days through the 2026 evening apparition and into the morning
    one at Oslo, Venus and Sun: the range the table proposal shows (#500)."""
    failures = 0
    for body in ("299", "10"):
        url, when, b = query(body, {"SITE_COORD": "'10.75,59.91,0.0'", "START_TIME": "'2026-06-01 21:00'",
                                    "STOP_TIME": "'2027-01-31 21:00'", "STEP_SIZE": "'14 d'"})
        failures += not keep(f"apparition-2026-oslo-{body}", url, when, b)
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


def main():
    if "--appearances" in sys.argv:
        appearances()
    if "--apparition" in sys.argv:
        apparition()
    failures = 0
    tlist = " ".join(f"'{t}'" for t, _ in NAMED)
    for site, (lon, lat, alt) in OBSERVERS.items():
        coord = f"'{lon},{lat},{alt}'"
        for body in ("299", "10"):
            url, when, b = query(body, {"SITE_COORD": coord, "TLIST": tlist, "TLIST_TYPE": "'CAL'"})
            failures += not keep(f"named-{site}-{body}", url, when, b)
        step = "'7 d'" if site == "oslo" else "'30 d'"
        url, when, b = query("299", {"SITE_COORD": coord, "START_TIME": "'1900-01-01 00:00'",
                                     "STOP_TIME": "'2100-12-31 00:00'", "STEP_SIZE": step})
        failures += not keep(f"matrix-{step.strip(chr(39)).replace(' ', '')}-{site}", url, when, b)
    # geocentric: named instants (UT and TT) and the 7-day matrix, Venus and Sun
    for body in ("299", "10"):
        url, when, b = query(body, {"CENTER": "'500@399'", "TLIST": tlist, "TLIST_TYPE": "'CAL'"})
        failures += not keep(f"named-geocentric-{body}", url, when, b)
        url, when, b = query(body, {"CENTER": "'500@399'", "START_TIME": "'1900-01-01 00:00'",
                                    "STOP_TIME": "'2100-12-31 00:00'", "STEP_SIZE": "'7 d'"})
        failures += not keep(f"matrix-7d-geocentric-{body}", url, when, b)
    # dense windows at Oslo around every named event: hourly within 3 days of a
    # conjunction (Venus changes fastest there), 6-hourly within 10 days of an
    # elongation; Venus and Sun on the same grid.
    for iso, name in events():
        conj = "conjunction" in name
        start, stop = window(iso.replace(" ", "T"), 3 if conj else 10, 3 if conj else 10)
        for body in ("299", "10"):
            url, when, b = query(body, {"SITE_COORD": "'10.75,59.91,0.0'", "START_TIME": f"'{start}'",
                                        "STOP_TIME": f"'{stop}'", "STEP_SIZE": "'1 h'" if conj else "'6 h'"})
            failures += not keep(f"dense-{name}-oslo-{body}", url, when, b)
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
