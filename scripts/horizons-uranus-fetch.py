#!/usr/bin/env python3
"""Fetch the JPL Horizons oracle responses for the #633 Uranus system study:
Uranus (799) and its five major moons, Miranda (705), Ariel (701), Umbriel
(702), Titania (703) and Oberon (704). Approved by the owner on 2026-10-27
("approve requests 3 and 4") for #633 measurement only, as Request 4: at
most 60 requests, at most 15 MB committed, at most 30 MB of geocentric
matrices retained outside Git.

Every response is kept whole, exactly as returned, in a file whose first
lines record the complete request URL (every query parameter), the UTC
time of the request and the SHA-256 and byte count of the body. Nothing
here is a build, test or runtime dependency; it is run once, by hand.

The run stops at the first response that lacks a complete table or names a
different target, and before any request or byte would pass the approved
bounds.

The opposition instants are read from Espenak's almanacs retained outside
Git (~/juranometria-runs/venus-raw, ~/juranometria-runs/mars-raw), each
verified against its digest; none is typed here.

Quantities requested: as the Mars study's (horizons-mars-fetch.py).

Committed: docs/studies/uranus-system/horizons (or $URANUS_HORIZONS_OUT).
Retained (geocentric matrices): ~/juranometria-runs/uranus-horizons
(or $URANUS_HORIZONS_RETAINED).
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
OUT = Path(os.environ.get("URANUS_HORIZONS_OUT")
           or Path(__file__).parent.parent / "docs/studies/uranus-system/horizons")
RETAINED = Path(os.environ.get("URANUS_HORIZONS_RETAINED")
                or Path.home() / "juranometria-runs/uranus-horizons")
OUT.mkdir(parents=True, exist_ok=True)
RETAINED.mkdir(parents=True, exist_ok=True)

COMMITTED_LIMIT = 15_000_000
RETAINED_LIMIT = 30_000_000
REQUEST_LIMIT = 60
PACE_SECONDS = 2.0

OBSERVERS = {
    "oslo": (10.75, 59.91, 0.0),
    "quito": (281.5, -0.18, 0.0),
    "cape-town": (18.42, -33.93, 0.0),
    "alert": (297.66, 82.50, 0.0),
    "chatham": (183.5, -43.95, 0.0),
}
BODIES = {"uranus": "799", "miranda": "705", "ariel": "701", "umbriel": "702",
          "titania": "703", "oberon": "704"}
MOONS = [(k, v) for k, v in BODIES.items() if v != "799"]
PLANET_QUANTITIES = "'1,2,4,10,13,14,15,17,20,24,32,43'"
MOON_QUANTITIES = "'1,2,4,6,12,13,20,24'"

NAMED = [
    ("1900-01-01 00:00:00", "boundary-start"),
    ("1962-01-01 00:00:00", "eop-record-begins"),
    ("1972-01-01 00:00:00", "first-leap-second"),
    ("2000-01-01 12:00:00", "j2000"),
    ("2017-01-01 00:00:00", "last-leap-second"),
    ("2026-10-27 12:00:00", "today"),
    ("2100-12-31 23:59:59", "boundary-end"),
]

# Almanac files and their digests (venus-events.txt and mars-raw/RETRIEVED.txt).
ALMANACS = {
    2003: (Path.home() / "juranometria-runs/mars-raw/almanac2003gmt.html",
           "50811f3be0f29b6de42cd4f3ad2bdbc3c296b9ced7c3807d8e08cff8c5fd4cc3"),
    2012: (Path.home() / "juranometria-runs/venus-raw/almanac2012gmt.html",
           "251a85e49504212df9391af58daec07b72f4fee86c77fe22b443e44c1018b307"),
    2018: (Path.home() / "juranometria-runs/mars-raw/almanac2018gmt.html",
           "8f0d441a2b224cef1af9ce0544274d3f97d1d758a1a3525ccab50a95f2b830be"),
    2026: (Path.home() / "juranometria-runs/venus-raw/almanac2026gmt.html",
           "4236c51d23297c270a42a3cb7a17c82881c1e5020de06b890594a1f40d80ce17"),
    2027: (Path.home() / "juranometria-runs/mars-raw/almanac2027gmt.html",
           "af77389f672434ed752163a351533ce68ba31b51a030663795e76ed8388f9540"),
    2035: (Path.home() / "juranometria-runs/mars-raw/almanac2035gmt.html",
           "0af51c5ed77b28875654ebd91f97c9ee086857ebca3eaabb847545fb3f397fdd"),
    2100: (Path.home() / "juranometria-runs/venus-raw/almanac2100gmt.html",
           "1178bd6d049593e5a3f40a94e6dbdf4210637587b09a25c3972b4d79b81aecd6"),
}
MONTHS = "Jan Feb Mar Apr May Jun Jul Aug Sep Oct Nov Dec".split()


def uranus_events(year):
    """(kind, datetime) for each 'Uranus at Opposition' and 'Uranus in
    Conjunction with Sun' row of the year's almanac, to the hour (GMT)."""
    path, digest = ALMANACS[year]
    raw = path.read_bytes()
    if hashlib.sha256(raw).hexdigest() != digest:
        stop(f"{path}: does not match its digest")
    text = re.sub(r"<[^>]+>", "", raw.decode("utf-8", "replace"))
    month, found = None, []
    for line in text.splitlines():
        m = re.match(r"^(%s) " % "|".join(MONTHS), line)
        if m:
            month = MONTHS.index(m.group(1)) + 1
        e = re.search(r"(\d\d)\s+(\d\d)\s+Uranus (at Opposition|in Conjunction with Sun)", line)
        if e and month:
            kind = "opposition" if "Opposition" in e.group(3) else "conjunction"
            found.append((kind, dt.datetime(year, month, int(e.group(1)), int(e.group(2)))))
    return found


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
         "QUANTITIES": PLANET_QUANTITIES if body == "799" else MOON_QUANTITIES,
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
    header = (f"# JPL Horizons response, kept whole (#633)\n"
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


def window(body, label, centre, half, step, site="'10.75,59.91,0.0'"):
    start = (centre - half).strftime("%Y-%m-%d %H:%M")
    end = (centre + half).strftime("%Y-%m-%d %H:%M")
    url, when, text = query(body, {"SITE_COORD": site, "START_TIME": f"'{start}'",
                                   "STOP_TIME": f"'{end}'", "STEP_SIZE": f"'{step}'"})
    keep(OUT, label, body, url, when, text)


def main():
    events = {y: uranus_events(y) for y in ALMANACS}
    for y, ev in events.items():
        print(f"almanac {y}: {[(k, t.isoformat()) for k, t in ev]}", flush=True)
    tlist = " ".join(f"'{t}'" for t, _ in NAMED)
    oslo = "'10.75,59.91,0.0'"
    # 30: every body from every observer at the named instants.
    for site, (lon, lat, alt) in OBSERVERS.items():
        coord = f"'{lon},{lat},{alt}'"
        for label, body in BODIES.items():
            url, when, text = query(body, {"SITE_COORD": coord, "TLIST": tlist,
                                           "TLIST_TYPE": "'CAL'"})
            keep(OUT, f"named-{label}-{site}", body, url, when, text)
    # 2: Uranus weekly over 1900-2100, Oslo (committed) and geocentric (retained).
    url, when, text = query("799", {"SITE_COORD": oslo,
                                    "START_TIME": "'1900-01-01 00:00'",
                                    "STOP_TIME": "'2100-12-31 00:00'",
                                    "STEP_SIZE": "'7 d'"})
    keep(OUT, "matrix-7d-uranus-oslo", "799", url, when, text)
    url, when, text = query("799", {"CENTER": "'500@399'",
                                    "START_TIME": "'1900-01-01 00:00'",
                                    "STOP_TIME": "'2100-12-31 00:00'",
                                    "STEP_SIZE": "'7 d'"})
    keep(RETAINED, "matrix-7d-uranus-geocentric", "799", url, when, text, retained=True)
    # 10: each moon weekly, Oslo over 2000-2100 (committed), geocentric over
    # 1900-2100 (retained), so the moons' interval can be ruled on measurement.
    for label, body in MOONS:
        url, when, text = query(body, {"SITE_COORD": oslo,
                                       "START_TIME": "'2000-01-01 00:00'",
                                       "STOP_TIME": "'2100-12-31 00:00'",
                                       "STEP_SIZE": "'7 d'"})
        keep(OUT, f"matrix-7d-{label}-oslo", body, url, when, text)
        url, when, text = query(body, {"CENTER": "'500@399'",
                                       "START_TIME": "'1900-01-01 00:00'",
                                       "STOP_TIME": "'2100-12-31 00:00'",
                                       "STEP_SIZE": "'7 d'"})
        keep(RETAINED, f"matrix-7d-{label}-geocentric", body, url, when, text, retained=True)
    # 6: Uranus hourly over three days about each almanac opposition, Oslo.
    for y, ev in events.items():
        for kind, t in ev:
            if kind == "opposition":
                window("799", f"opposition-{y}-uranus-oslo-hourly", t, dt.timedelta(days=3), "1 h")
    # 5: each moon every 10 minutes over two days about the 2026 opposition, geocentric.
    for kind, t in events[2026]:
        if kind != "opposition":
            continue
        for label, body in MOONS:
            start = (t - dt.timedelta(days=1)).strftime("%Y-%m-%d %H:%M")
            end = (t + dt.timedelta(days=1)).strftime("%Y-%m-%d %H:%M")
            url, when, text = query(body, {"CENTER": "'500@399'",
                                           "START_TIME": f"'{start}'",
                                           "STOP_TIME": f"'{end}'",
                                           "STEP_SIZE": "'10 m'"})
            keep(OUT, f"opposition-2026-{label}-geocentric-10m", body, url, when, text)
    # 6: magnitudes every 30 days over 1900-2100, geocentric.
    for label, body in BODIES.items():
        url, when, text = query(body, {"CENTER": "'500@399'", "QUANTITIES": "'9'",
                                       "START_TIME": "'1900-01-01 00:00'",
                                       "STOP_TIME": "'2100-12-31 00:00'",
                                       "STEP_SIZE": "'30 d'"})
        keep(OUT, f"magnitudes-30d-{label}-geocentric", body, url, when, text)
    print(f"done: {state['requests']} requests, committed {state['committed']:,} bytes,"
          f" retained {state['retained']:,} bytes", flush=True)


if __name__ == "__main__":
    main()
