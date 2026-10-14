#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the Mercury contract study (#528).

Every response is kept whole, exactly as returned, in a file whose first
lines record the request URL, the UTC time of the request and the SHA-256
of the body. Nothing here is a build, test or runtime dependency; it is run
once, by hand. The bulk is written outside the repository
($MERCURY_HORIZONS_OUT, else ~/juranometria-runs/mercury-horizons); only the
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
import time
import urllib.parse
import urllib.request
from pathlib import Path

BASE = "https://ssd.jpl.nasa.gov/api/horizons.api"
OUT = Path(os.environ.get("MERCURY_HORIZONS_OUT")
           or Path.home() / "juranometria-runs/mercury-horizons")
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
MERCURY_Q = "'1,2,4,9,10,13,19,20,23,24,43'"
SUN_Q = "'1,2,4,20'"


def events():
    out = []
    for line in (ROOT / "docs/studies/mercury/mercury-events.txt").read_text(encoding="utf-8").splitlines():
        if line.strip() and not line.startswith("#"):
            name, iso, _ = line.split("\t")
            out.append((iso.replace("T", " ").replace("Z", ":00") if iso.count(":") == 1 else iso.replace("T", " ").replace("Z", ""), name))
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
    # filled from the 7-day Oslo matrix once fetched (Horizons' own Illu%):
    # the weekly rows nearest a quarter and three quarters lit on each
    # side of the 2026-03-07 inferior conjunction, named crescent/gibbous
    # evening/morning as for Venus. Set by --appearances=ISO:name,...
]


def query(body, params):
    q = {"format": "text", "COMMAND": f"'{body}'", "MAKE_EPHEM": "'YES'",
         "EPHEM_TYPE": "'OBSERVER'", "CENTER": "'coord@399'",
         "COORD_TYPE": "'GEODETIC'", "QUANTITIES": MERCURY_Q if body == "199" else SUN_Q,
         "EXTRA_PREC": "'YES'", "ANG_FORMAT": "'DEG'", "APPARENT": "'AIRLESS'",
         "CSV_FORMAT": "'YES'", "TIME_DIGITS": "'SECONDS'"}
    q.update(params)
    url = BASE + "?" + urllib.parse.urlencode(q, quote_via=urllib.parse.quote)
    when = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    for attempt in range(4):
        try:
            with urllib.request.urlopen(url, timeout=600) as r:
                return url, when, r.read()
        except urllib.error.HTTPError as e:
            if e.code in (502, 503, 504) and attempt < 3:
                print(f"  {e.code} from Horizons, retrying in {30 * (attempt + 1)} s", flush=True)
                time.sleep(30 * (attempt + 1))
                continue
            raise


def keep(name, url, when, body):
    digest = hashlib.sha256(body).hexdigest()
    text = body.decode("utf-8", "replace")
    ok = "$$SOE" in text and "$$EOE" in text
    header = (f"# JPL Horizons response, kept whole (#528)\n# request-url: {url}\n"
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
    for arg in sys.argv:
        if arg.startswith("--appearances="):
            for item in arg.split("=", 1)[1].split(","):
                iso, name = item.split(":", 1)
                APPEARANCES.append((iso.replace("T", " ") + ":00", name))
    if not APPEARANCES:
        raise SystemExit("--appearances=ISO:name,... names the chosen rows")
    failures = 0
    tlist = " ".join(f"'{t}'" for t, _ in APPEARANCES)
    for site, (lon, lat, alt) in OBSERVERS.items():
        for body in ("199", "10"):
            url, when, b = query(body, {"SITE_COORD": f"'{lon},{lat},{alt}'", "TLIST": tlist,
                                        "TLIST_TYPE": "'CAL'"})
            failures += not keep(f"appearances-{site}-{body}", url, when, b)
    for body in ("199", "10"):
        url, when, b = query(body, {"CENTER": "'500@399'", "TLIST": tlist, "TLIST_TYPE": "'CAL'"})
        failures += not keep(f"appearances-geocentric-{body}", url, when, b)
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


def apparition():
    """Every 3 days through the 2026 evening apparition (greatest elongation
    east 2026-02-19) and into the morning one (greatest elongation west
    2026-04-03) at Oslo, Mercury and Sun: the range the table proposal
    shows. Mercury's apparitions last weeks, not months, so the step is
    3 days where Venus's was 14."""
    failures = 0
    for body in ("199", "10"):
        url, when, b = query(body, {"SITE_COORD": "'10.75,59.91,0.0'", "START_TIME": "'2026-01-25 17:00'",
                                    "STOP_TIME": "'2026-04-25 17:00'", "STEP_SIZE": "'3 d'"})
        failures += not keep(f"apparition-2026-oslo-{body}", url, when, b)
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


def main():
    """Fetched in parts, each on its own permission (#528): --committed is
    the ruled fixture set (named instants at the five observers and
    geocentrically, Mercury and Sun; the 7-day Oslo matrix; the 2026
    inferior conjunction hourly and the 2026 greatest western elongation
    six-hourly at Oslo); --geocentric-matrix the 7-day geocentric Mercury
    matrix; --geocentric-sun-matrix the Sun's; --matrix-30d the other four
    observers' 30-day matrices; --dense-all every other named window."""
    if "--appearances" in " ".join(sys.argv):
        appearances()
    if "--apparition" in sys.argv:
        apparition()
    failures = 0
    # Horizons' gateway refuses a GET beyond about 1 900 characters (a
    # 45-instant TLIST answered 502 where 32 answered 200), so the named
    # instants go in three lists of at most 32: the interval's boundaries
    # with the conjunctions ("named"), the greatest elongations with the
    # lunar occultations ("elongations"), and the transits.
    groups = {
        "named": [(t, n) for t, n in NAMED if "elongation" not in n and "occultation" not in n and "transit" not in n],
        "elongations": [(t, n) for t, n in NAMED if "elongation" in n or "occultation" in n],
        "transits": [(t, n) for t, n in NAMED if "transit" in n],
    }
    for kind, items in groups.items():
        if len(items) > 32:
            raise SystemExit(f"{kind}: {len(items)} instants, more than one request carries")
    tlists = {kind: " ".join(f"'{t}'" for t, _ in items) for kind, items in groups.items()}
    if "--committed" in sys.argv:
        for site, (lon, lat, alt) in OBSERVERS.items():
            coord = f"'{lon},{lat},{alt}'"
            for body in ("199", "10"):
                for kind, tl in tlists.items():
                    url, when, b = query(body, {"SITE_COORD": coord, "TLIST": tl, "TLIST_TYPE": "'CAL'"})
                    failures += not keep(f"{kind}-{site}-{body}", url, when, b)
        url, when, b = query("199", {"SITE_COORD": "'10.75,59.91,0.0'", "START_TIME": "'1900-01-01 00:00'",
                                     "STOP_TIME": "'2100-12-31 00:00'", "STEP_SIZE": "'7 d'"})
        failures += not keep("matrix-7d-oslo", url, when, b)
        for body in ("199", "10"):
            for kind, tl in tlists.items():
                url, when, b = query(body, {"CENTER": "'500@399'", "TLIST": tl, "TLIST_TYPE": "'CAL'"})
                failures += not keep(f"{kind}-geocentric-{body}", url, when, b)
        for iso, name in events():
            if name not in ("2026-inferior-conjunction-1", "2026-greatest-elongation-west-1"):
                continue
            conj = "conjunction" in name
            start, stop = window(iso.replace(" ", "T"), 3 if conj else 10, 3 if conj else 10)
            for body in ("199", "10"):
                url, when, b = query(body, {"SITE_COORD": "'10.75,59.91,0.0'", "START_TIME": f"'{start}'",
                                            "STOP_TIME": f"'{stop}'", "STEP_SIZE": "'1 h'" if conj else "'6 h'"})
                failures += not keep(f"dense-{name}-oslo-{body}", url, when, b)
    if "--geocentric-matrix" in sys.argv:
        url, when, b = query("199", {"CENTER": "'500@399'", "START_TIME": "'1900-01-01 00:00'",
                                     "STOP_TIME": "'2100-12-31 00:00'", "STEP_SIZE": "'7 d'"})
        failures += not keep("matrix-7d-geocentric-199", url, when, b)
    if "--geocentric-sun-matrix" in sys.argv:
        url, when, b = query("10", {"CENTER": "'500@399'", "START_TIME": "'1900-01-01 00:00'",
                                    "STOP_TIME": "'2100-12-31 00:00'", "STEP_SIZE": "'7 d'"})
        failures += not keep("matrix-7d-geocentric-10", url, when, b)
    if "--matrix-30d" in sys.argv:
        for site, (lon, lat, alt) in OBSERVERS.items():
            if site == "oslo":
                continue
            url, when, b = query("199", {"SITE_COORD": f"'{lon},{lat},{alt}'", "START_TIME": "'1900-01-01 00:00'",
                                         "STOP_TIME": "'2100-12-31 00:00'", "STEP_SIZE": "'30 d'"})
            failures += not keep(f"matrix-30d-{site}", url, when, b)
    if "--dense-all" in sys.argv:
        for iso, name in events():
            if name in ("2026-inferior-conjunction-1", "2026-greatest-elongation-west-1") or "occultation" in name or "transit" in name:
                continue
            conj = "conjunction" in name
            start, stop = window(iso.replace(" ", "T"), 3 if conj else 10, 3 if conj else 10)
            for body in ("199", "10"):
                url, when, b = query(body, {"SITE_COORD": "'10.75,59.91,0.0'", "START_TIME": f"'{start}'",
                                            "STOP_TIME": f"'{stop}'", "STEP_SIZE": "'1 h'" if conj else "'6 h'"})
                failures += not keep(f"dense-{name}-oslo-{body}", url, when, b)
    print(f"failures={failures}")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
