#!/usr/bin/env python3
"""#574: event times from sat441 against IMCCE's predicted phenomena of
the Saturnian satellites, 2006-2013 (W. Thuillot, from TASS 1.6) - an
authority independent of JPL's integration, which Horizons is not.

For every eclipse (EC.D, EC.R), occultation (OC.D, OC.R) and transit
(TR.I, TR.E) IMCCE lists, the matching crossing is found in sat441 by
Skyfield near IMCCE's instant: the satellite's centre on Saturn's limb
(occultations and transits) or on the edge of Saturn's umbra (eclipses),
"each event computed for the satellite centre", as IMCCE's README states.
Two figures of Saturn are tried: the equatorial sphere and the oblate
figure (the PCK pole and radii). Shadow transits (SH.I, SH.E) are not
compared. Geocentric observer; IMCCE's times are TT.

The IMCCE files are pinned and kept whole outside Git; the report states
only differences, and the rows it quotes - one per satellite and kind,
the worst on the better figure - are written beside it with their line
numbers, attribution and the source files' digests.

Run by hand from the repository root with the study's virtual environment:

    ~/juranometria-runs/venv/bin/python scripts/saturn-imcce-comparison.py \\
        > docs/studies/saturn-system/imcce-comparison.md
"""
import datetime as dt
import hashlib
import re
import sys
from pathlib import Path

import numpy as np
from skyfield.api import Loader

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "imports/raw"
STUDY = ROOT / "docs/studies/saturn-system"
IMCCE = RAW / "saturn-system"
PINS = {
    RAW / "saturn-system/sat441.bsp": "d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5",
    RAW / "jovian-system/pck00011.tpc": "3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1",
    IMCCE / "imcce-phensaturne-README.txt": "9fba712de344e3dd76b205d089d903fc464a9331fb7c4aa6c11073589630c637",
    IMCCE / "imcce-phensaturne-phenE-S1.dat": "fa462f76bce97e89441da5ea58b992ad5c2e6d1d0db62ba3cf56905cf8937145",
    IMCCE / "imcce-phensaturne-phenE-S2.dat": "7fc95bdfc246f5cee825397604e39c01d2b1d8a13d244ef458b61cfef765626c",
    IMCCE / "imcce-phensaturne-phenE-S3.dat": "ffe39d0bbace5edb145409c0e1df4213689a839e02a0ca19a1e15261d0f1ce7b",
    IMCCE / "imcce-phensaturne-phenE-S4.dat": "0a8431191a27be84acd8d85a7e58046edfe57a89a75faa5750ae543c05c1c302",
    IMCCE / "imcce-phensaturne-phenE-S5.dat": "30a04db8a9a75b5a8ccf851b0003559ee89f8891c95cf5c0c9327e6243cb7661",
    IMCCE / "imcce-phensaturne-phenE-S6.dat": "bb1fafd2855dcc343c3a0bcc6b5211c3a0887eb3c7e1155df7c91e86178d04c8",
    IMCCE / "imcce-phensaturne-phenE-S7.dat": "0961de801d13c8c33b467906311134c47b3ec21cb9ee01a81d2a448d598aaf82",
    IMCCE / "imcce-phensaturne-phenE-S8.dat": "59e88532bfb959c65cbc70695f61fff3c247912a715dd5994426af0d192e81b2",
}
MOONS = {1: ("mimas", 601), 2: ("enceladus", 602), 3: ("tethys", 603), 4: ("dione", 604),
         5: ("rhea", 605), 6: ("titan", 606), 7: ("hyperion", 607), 8: ("iapetus", 608)}
KINDS = {"EC": "eclipse", "OC": "occultation", "TR": "transit"}
INTO = {"D", "I"}               # disappearance and ingress go inside; R and E come out
WINDOW_MIN = 90                 # the search, minutes either side of IMCCE's instant
STEP_MIN = 1


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def stop(message):
    sys.exit(f"saturn-imcce-comparison: {message} - stopping")


for path, pin in PINS.items():
    if sha256(path) != pin:
        stop(f"{path} does not match its pin")


def pck_constants(path):
    values, data, pending = {}, False, None
    for line in path.read_text(encoding="ascii", errors="replace").splitlines():
        s = line.strip()
        if s in ("\\begindata", "\\begintext"):
            data = s == "\\begindata"
            continue
        if not data:
            continue
        if pending is not None:
            pending[1].append(s)
            if ")" in s:
                values[pending[0]] = " ".join(pending[1])
                pending = None
            continue
        m = re.match(r"(BODY\w+)\s*=\s*(.*)", s)
        if m:
            if m.group(2).startswith("(") and ")" not in m.group(2):
                pending = (m.group(1), [m.group(2)])
            else:
                values[m.group(1)] = m.group(2)
    return {k: [float(x.replace("D", "E")) for x in v.strip("() ").split()] for k, v in values.items()}


PCK = pck_constants(RAW / "jovian-system/pck00011.tpc")
R_SUN = PCK["BODY10_RADII"][0]
R_EQ, _, R_POLAR = PCK["BODY699_RADII"]
POLE_RA, POLE_DEC = PCK["BODY699_POLE_RA"], PCK["BODY699_POLE_DEC"]

load = Loader(str(ROOT / "build" / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
sat = load(str(RAW / "saturn-system/sat441.bsp"))
earth, sun, saturn = sat["earth"], sat["sun"], sat[699]


def unit(v):
    return v / np.linalg.norm(v, axis=0)


def pole_icrf(tdb):
    T = (tdb - 2451545.0) / 36525.0
    ra = np.radians(POLE_RA[0] + POLE_RA[1] * T)
    dec = np.radians(POLE_DEC[0] + POLE_DEC[1] * T)
    return np.array([np.cos(dec) * np.cos(ra), np.cos(dec) * np.sin(ra), np.sin(dec)])


def ellipse_radius(px, py, a, b):
    """The radius, along (px, py), of the ellipse with semi-axes a (x) and b (y)."""
    rho = np.hypot(px, py)
    with np.errstate(invalid="ignore", divide="ignore"):
        return 1.0 / np.sqrt((px / rho / a) ** 2 + (py / rho / b) ** 2)


def disc_function(t, moon_id, oblate):
    """Positive outside Saturn's apparent figure, negative inside (km, in
    the plane of the sky at Saturn's distance), for the satellite's
    centre; and the moon's depth (km, positive when farther than Saturn)."""
    at = earth.at(t)
    s = at.observe(saturn).position.km
    m = at.observe(sat[moon_id]).position.km
    ds, dm = np.linalg.norm(s, axis=0), np.linalg.norm(m, axis=0)
    los = unit(s)
    off = m * (ds / dm) - s
    off = off - np.sum(off * los, axis=0) * los
    rho = np.linalg.norm(off, axis=0)
    if not oblate:
        return rho - R_EQ, dm - ds
    pole = pole_icrf(t.tdb)
    up = unit(pole - np.sum(pole * los, axis=0) * los)
    east = np.cross(up.T, los.T).T
    px, py = np.sum(off * east, axis=0), np.sum(off * up, axis=0)
    sub = np.arcsin(np.clip(np.sum(pole * -los, axis=0), -1, 1))
    b = np.sqrt(R_POLAR ** 2 * np.cos(sub) ** 2 + R_EQ ** 2 * np.sin(sub) ** 2)
    return rho - ellipse_radius(px, py, R_EQ, b), dm - ds


def shadow_function(t, moon_id, oblate):
    """Positive outside Saturn's umbra, negative inside (km, across the
    shadow axis), for the satellite's centre at its emission epoch."""
    at = earth.at(t)
    lt = at.observe(sat[moon_id]).light_time
    te = ts.tdb_jd(t.tdb - lt)
    S, P, Q = sun.at(te).position.km, saturn.at(te).position.km, sat[moon_id].at(te).position.km
    axis = P - S
    d = np.linalg.norm(axis, axis=0)
    u = axis / d
    r = Q - P
    along = np.sum(r * u, axis=0)
    across = r - along * u
    shrink = 1.0 - along * (R_SUN - R_EQ) / (R_EQ * d)
    if not oblate:
        return np.where(along > 0, np.linalg.norm(across, axis=0) - R_EQ * shrink, np.inf)
    pole = pole_icrf(te.tdb)
    up = unit(pole - np.sum(pole * u, axis=0) * u)
    east = np.cross(up.T, u.T).T
    px, py = np.sum(across * east, axis=0), np.sum(across * up, axis=0)
    sub = np.arcsin(np.clip(np.sum(pole * u, axis=0), -1, 1))
    b = np.sqrt(R_POLAR ** 2 * np.cos(sub) ** 2 + R_EQ ** 2 * np.sin(sub) ** 2)
    edge = ellipse_radius(px, py, R_EQ * shrink, b * shrink)
    return np.where(along > 0, np.hypot(px, py) - edge, np.inf)


SIXTY = []


def events():
    """Every IMCCE event: (satellite, kind, direction, TT instant, file, line number)."""
    rows = []
    for n in MOONS:
        path = IMCCE / f"imcce-phensaturne-phenE-S{n}.dat"
        for k, line in enumerate(path.read_text(encoding="latin-1").splitlines(), 1):
            m = re.match(r"\s*(\d)\s+([A-Z]{2})\.([A-Z])\s+(\d{4})\s+(\d+)\s+(\d+)\s+(\d+)\.\s*(\d+)\.\s*(\d+)\.", line)
            if not m:
                continue
            sat_n, kind, way = int(m.group(1)), m.group(2), m.group(3)
            if sat_n != n:
                stop(f"{path.name} line {k} names satellite {sat_n}")
            y, mo, d, hh, mi, ss = map(int, m.groups()[3:])
            if ss == 60:
                SIXTY.append((path.name, k))
            rows.append((n, kind, way, dt.datetime(y, mo, d, hh, mi) + dt.timedelta(seconds=ss), path.name, k, line))
    return rows


def crossing_times(rows, moon_id, kind, way, oblate):
    """Our crossing nearest IMCCE's instant, each row: seconds from IMCCE,
    or nan where none lies in the window or it lies on the wrong side."""
    offsets = np.arange(-WINDOW_MIN, WINDOW_MIN + STEP_MIN, STEP_MIN) / 1440.0
    base = np.array([ts.tt(w.year, w.month, w.day, w.hour, w.minute, w.second).tt for *_, w, _, _, _ in rows])
    grid = (base[:, None] + offsets[None, :]).ravel()
    t = ts.tt_jd(grid)
    if kind == "EC":
        f = shadow_function(t, moon_id, oblate)
        depth = np.ones_like(f)
    else:
        f, depth = disc_function(t, moon_id, oblate)
    f = f.reshape(len(rows), -1)
    depth = depth.reshape(len(rows), -1)
    res = np.full(len(rows), np.nan)
    rate = np.full(len(rows), np.nan)
    into = way in INTO
    for i in range(len(rows)):
        fi = f[i]
        best = None
        for j in range(len(fi) - 1):
            a, b = fi[j], fi[j + 1]
            if not (np.isfinite(a) and np.isfinite(b)):
                continue
            if (into and a > 0 >= b) or (not into and a <= 0 < b):
                if kind == "TR" and depth[i, j] > 0 or kind == "OC" and depth[i, j] < 0:
                    continue
                x = offsets[j] + (offsets[j + 1] - offsets[j]) * a / (a - b)
                if best is None or abs(x) < abs(best):
                    best, slope = x, abs(b - a) / (STEP_MIN * 60.0)
        if best is not None:
            res[i] = best * 86400.0
            rate[i] = slope
    return res, rate


def main():
    print("# #574 Saturnian satellite events: sat441 against IMCCE (TASS 1.6)")
    print()
    print("IMCCE's predictions of the phenomena of the Saturnian satellites, 2006-2013, by"
          " W. Thuillot from TASS 1.6 (Vienne & Duriez 1995), Institut de mécanique céleste et de"
          " calcul des éphémérides, Paris Observatory: retrieved 2026-10-19, kept whole outside"
          " Git (digests in `SOURCES.md`). Against them: Skyfield on the pinned `sat441.bsp`,"
          " geocentric, each crossing of the satellite's centre over Saturn's limb (occultations,"
          " transits) or the edge of Saturn's umbra (eclipses), found within"
          f" {WINDOW_MIN} minutes of IMCCE's TT instant on a {STEP_MIN}-minute grid and interpolated."
          " Two figures of Saturn: the equatorial sphere and the oblate figure (PCK pole, radii"
          f" {R_EQ:g} and {R_POLAR:g} km). Differences are ours minus IMCCE's, in seconds."
          " Shadow transits are not compared. IMCCE rounds to the second.")
    rows = events()
    quoted = []
    summary = {}
    along = {}
    for n, (label, moon_id) in MOONS.items():
        for kind in KINDS:
            for oblate in (False, True):
                diffs, rates, picks = [], [], []
                for way in sorted({r[2] for r in rows if r[0] == n and r[1] == kind}):
                    group = [r for r in rows if r[0] == n and r[1] == kind and r[2] == way]
                    d, r = crossing_times(group, moon_id, kind, way, oblate)
                    diffs.append(d)
                    rates.append(r)
                    picks.extend(group)
                d = np.concatenate(diffs) if diffs else np.array([])
                r = np.concatenate(rates) if rates else np.array([])
                summary[(label, kind, oblate)] = (d, picks)
                along[(label, kind, oblate)] = d * r
    print()
    print(f"{len(rows)} events read; {len(SIXTY)} give 60 in the seconds field, a carried rounding,"
          " read as the next minute.")
    print()
    print("## Differences by satellite, kind and figure")
    print()
    print("*km* converts each time difference to a distance at Saturn: the difference times the"
          " rate at which our crossing function changes there (km/s, across the limb or the umbra's"
          " edge). It compares a fast and a slow, grazing event on one scale.")
    print()
    print("| satellite | kind | figure | events | matched | median s | 95% abs s | max abs s | median abs km | 95% abs km | max abs km |")
    print("|---|---|---|---:|---:|---:|---:|---:|---:|---:|---:|")
    for n, (label, _) in MOONS.items():
        for kind, word in KINDS.items():
            for oblate in (False, True):
                d, picks = summary[(label, kind, oblate)]
                ok = d[np.isfinite(d)]
                fig = "oblate" if oblate else "sphere"
                a = np.abs(along[(label, kind, oblate)])
                a = a[np.isfinite(a)]
                if ok.size:
                    print(f"| {label} | {word} | {fig} | {d.size} | {ok.size} | {np.median(ok):+.1f} |"
                          f" {np.percentile(np.abs(ok), 95):.1f} | {np.max(np.abs(ok)):.1f} |"
                          f" {np.median(a):.0f} | {np.percentile(a, 95):.0f} | {np.max(a):.0f} |")
                else:
                    print(f"| {label} | {word} | {fig} | {d.size} | 0 | - | - | - | - | - | - |")
    # the better figure, by the 95th percentile over all matched events
    p = {}
    for oblate in (False, True):
        allv = np.concatenate([summary[(l, k, oblate)][0] for _, (l, _) in MOONS.items() for k in KINDS])
        ok = allv[np.isfinite(allv)]
        p[oblate] = (np.percentile(np.abs(ok), 95), ok.size, allv.size)
    better = min(p, key=lambda o: p[o][0])
    print()
    for oblate, (q, k, total) in p.items():
        print(f"- {'oblate figure' if oblate else 'equatorial sphere'}: {k} of {total} events matched,"
              f" 95% within {q:.1f} s")
    print(f"- the better fit: the {'oblate figure' if better else 'equatorial sphere'}")
    print()
    print("## Unmatched events on the better figure")
    print()
    any_unmatched = False
    for n, (label, _) in MOONS.items():
        for kind, word in KINDS.items():
            d, picks = summary[(label, kind, better)]
            miss = [picks[i] for i in range(len(d)) if not np.isfinite(d[i])]
            if miss:
                any_unmatched = True
                print(f"- {label}, {word}: {len(miss)} (first {miss[0][3]:%Y-%m-%d %H:%M:%S} TT,"
                      f" {miss[0][4]} line {miss[0][5]})")
    if not any_unmatched:
        print("- none")
    for n, (label, _) in MOONS.items():
        for kind in KINDS:
            d, picks = summary[(label, kind, better)]
            if np.isfinite(d).any():
                i = int(np.nanargmax(np.abs(d)))
                quoted.append((picks[i], d[i]))
    print()
    print("## The worst event of each satellite and kind, on the better figure")
    print()
    print("Quoted in `imcce-quoted-rows.txt`, verbatim, with their file and line.")
    print()
    for (n, kind, way, w, name, k, line), d in quoted:
        print(f"- {MOONS[n][0]} {kind}.{way} {w:%Y-%m-%d %H:%M:%S} TT ({name} line {k}): ours {d:+.1f} s")
    out = STUDY / "imcce-quoted-rows.txt"
    with out.open("w", encoding="utf-8") as f:
        f.write("# IMCCE, Phenomena of the Saturnian satellites 2006-2013 (TT): quoted rows for the #574 comparison\n")
        f.write("# source: https://ftp.imcce.fr/pub/ephem/satel/phensaturne/ (retrieved 2026-10-19; whole files kept outside Git)\n")
        f.write("# attribution: Institut de mecanique celeste et de calcul des ephemerides (IMCCE), Paris Observatory;"
                " predictions by W. Thuillot from TASS 1.6 (Vienne & Duriez 1995); each event for the satellite centre\n")
        f.write("# terms: https://www.imcce.fr/mentions-legales - \"The data contained in the Web and FTP servers of the IMCCE"
                " can be used as an example to illustrate documents if the source is clearly identified.\" Quoted, not redistributed.\n")
        f.write("# layout (README): sat phen year m d hh mm ss X Y sd d - X toward East, Y toward North in planet radii;"
                " sd the semi-duration in seconds; d the distance from the planet's edge in arcsec\n")
        for name in sorted({q[0][4] for q in quoted}):
            f.write(f"# {name} sha256 {PINS[IMCCE / name]}\n")
        for (n, kind, way, w, name, k, line), d in quoted:
            f.write(f"{name}:{k}\t{line}\n")


if __name__ == "__main__":
    main()
