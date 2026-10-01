#!/usr/bin/env python3
"""#398 authority comparison: Skyfield on the DE440 subset vs JPL Horizons,
the subset vs the full kernel, Meeus 25.a, and the delta-T models vs IERS.

Runs offline from the kept downloads. Writes comparison.md.
"""
import datetime as dt
import math
import re
from pathlib import Path

import numpy as np
from skyfield.api import Loader, wgs84
from skyfield.constants import AU_KM
from skyfield.api import load_constellation_map

HERE = Path(__file__).parent
load = Loader(str(HERE / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
SUBSET = HERE / "subsets/juranometria-de440-sun-earth-1900-2100.bsp"
FULL = HERE / "downloads/de440s.bsp"
R_SUN_KM = 695_700.0
ARCSEC = 3600.0

OBSERVERS = {
    "oslo": (10.75, 59.91, 0.02),
    "quito": (281.5, -0.18, 2.85),
    "cape-town": (18.42, -33.93, 0.01),
    "alert": (297.66, 82.50, 0.03),
    "chatham": (183.5, -43.95, 0.02),
}

out = []


def say(s=""):
    out.append(s)
    print(s)


def sep_arcsec(ra1, dec1, ra2, dec2):
    """Angular separation in arcseconds, degrees in."""
    a1, d1, a2, d2 = map(math.radians, (ra1, dec1, ra2, dec2))
    c = (math.sin(d1) * math.sin(d2)
         + math.cos(d1) * math.cos(d2) * math.cos(a1 - a2))
    return math.degrees(math.acos(max(-1.0, min(1.0, c)))) * ARCSEC


def parse_horizons(path):
    text = path.read_text(encoding="utf-8")
    body = text[text.index("$$SOE") + 5:text.index("$$EOE")]
    rows = []
    for line in body.strip().splitlines():
        f = [x.strip() for x in line.split(",")]
        stamp = f[0] if "." in f[0] else f[0] + ".000"
        when = dt.datetime.strptime(stamp, "%Y-%b-%d %H:%M:%S.%f")
        rows.append(dict(
            when=when, ra=float(f[3]), dec=float(f[4]),
            ra_app=float(f[5]), dec_app=float(f[6]),
            az=float(f[7]), el=float(f[8]), diam=float(f[9]),
            delta=float(f[10]), cnst=f[12], eclon=float(f[13]),
            eclat=float(f[14])))
    return rows


def era(when):
    if when.year < 1962:
        return "1900-1961"
    if when.year < 1972:
        return "1962-1971"
    if when < dt.datetime(2026, 9, 26):
        return "1972-2026"
    return "2026-2100"


class Worst:
    def __init__(self):
        self.v = {}

    def note(self, key, value, tag):
        if key not in self.v or abs(value) > abs(self.v[key][0]):
            self.v[key] = (value, tag)

    def get(self, key):
        return self.v.get(key, (0.0, "-"))


def compare(kernel_path, label):
    kernel = load(str(kernel_path))
    earth, sun = kernel["earth"], kernel["sun"]
    cmap = load_constellation_map()
    worst = Worst()
    counts = {}
    cnst_mismatch = []
    ut1_cost = Worst()
    files = sorted((HERE / "horizons").glob("*.txt"))
    for path in files:
        name = path.stem
        if name.startswith("geocentric"):
            continue
        site = next((s for s in OBSERVERS if name.endswith(s)), None)
        if site is None:
            continue
        lon, lat, alt_km = OBSERVERS[site]
        topos = wgs84.latlon(lat, lon if lon <= 180 else lon - 360,
                             elevation_m=alt_km * 1000)
        observer = earth + topos
        for row in parse_horizons(path):
            w = row["when"]
            # Before 1972 the civil instant is UT (UT1); Horizons reads
            # it so, and so must the contract. From 1972 it is UTC.
            civil = (w.year, w.month, w.day, w.hour, w.minute,
                     w.second + w.microsecond / 1e6)
            t = ts.utc(*civil) if w.year >= 1972 else ts.ut1(*civil)
            astro = observer.at(t).observe(sun)
            ra, dec, dist = astro.radec()
            app = astro.apparent(deflectors=())  # the Sun deflects its own centre by nothing
            ra_a, dec_a, _ = app.radec(epoch="date")
            alt, az, _ = app.altaz()
            lat_e, lon_e, _ = app.ecliptic_latlon(epoch="date")
            diam = 2 * math.degrees(math.asin(R_SUN_KM / dist.km)) * ARCSEC
            e = era(w)
            counts[e] = counts.get(e, 0) + 1
            tag = f"{site} {w:%Y-%m-%d %H:%M}"
            worst.note((e, "astrometric RA/Dec"),
                       sep_arcsec(ra._degrees, dec.degrees, row["ra"], row["dec"]), tag)
            worst.note((e, "apparent RA/Dec of date"),
                       sep_arcsec(ra_a._degrees, dec_a.degrees, row["ra_app"], row["dec_app"]), tag)
            # az/alt: compare as a separation on the local sphere
            worst.note((e, "azimuth/altitude"),
                       sep_arcsec(az.degrees, alt.degrees, row["az"], row["el"]), tag)
            worst.note((e, "distance (km)"),
                       dist.km - row["delta"] * AU_KM, tag)
            worst.note((e, "angular diameter (arcsec)"),
                       diam - row["diam"], tag)
            dl = (lon_e.degrees - row["eclon"] + 180) % 360 - 180
            worst.note((e, "ecliptic longitude of date (arcsec)"), dl * ARCSEC, tag)
            # constellation, Skyfield's map vs Horizons
            c = cmap(astro)
            if c != row["cnst"]:
                cnst_mismatch.append((tag, c, row["cnst"]))
            # the cost of UT1 = UTC on azimuth/altitude: same civil
            # instant read as UT1
            t_ut1 = ts.ut1(*civil)
            alt2, az2, _ = observer.at(t_ut1).observe(sun).apparent(deflectors=()).altaz()
            ut1_cost.note((e, "UT1=UTC cost on az/alt"),
                          sep_arcsec(az.degrees, alt.degrees, az2.degrees, alt2.degrees), tag)
    say(f"### {label}: Skyfield vs Horizons, worst absolute difference by era")
    say()
    say("| era | rows | astrometric RA/Dec | apparent of date | az/alt | distance | ang. diameter | ecl. lon of date |")
    say("|---|---|---|---|---|---|---|---|")
    for e in ["1900-1961", "1962-1971", "1972-2026", "2026-2100"]:
        g = lambda k: worst.get((e, k))
        say(f"| {e} | {counts.get(e, 0)} | {g('astrometric RA/Dec')[0]:.3f}″ | "
            f"{g('apparent RA/Dec of date')[0]:.3f}″ | {g('azimuth/altitude')[0]:.2f}″ | "
            f"{g('distance (km)')[0]:+.3f} km | {g('angular diameter (arcsec)')[0]:+.4f}″ | "
            f"{g('ecliptic longitude of date (arcsec)')[0]:+.3f}″ |")
    say()
    say("Where the worst rows are:")
    for (e, k), (v, tag) in sorted(worst.v.items()):
        say(f"- {e}, {k}: {v:.4f} at {tag}")
    say()
    say("Cost of reading the civil instant as UT1 (UT1 = UTC), on az/alt, worst by era:")
    for e in ["1900-1961", "1962-1971", "1972-2026", "2026-2100"]:
        v, tag = ut1_cost.get((e, "UT1=UTC cost on az/alt"))
        say(f"- {e}: {v:.2f}″ at {tag}")
    say()
    say(f"Constellation (Skyfield's IAU boundary map vs Horizons): "
        f"{len(cnst_mismatch)} disagreements of {sum(counts.values())} rows.")
    for tag, c, h in cnst_mismatch[:20]:
        say(f"- {tag}: Skyfield {c}, Horizons {h}")
    say()
    kernel.close()


def subset_equals_full():
    a, b = load(str(SUBSET)), load(str(FULL))
    times = ts.utc(1900, 1, range(1, 73415, 7))  # weekly, 1900–2100
    pa = (a["earth"].at(times).observe(a["sun"]))
    pb = (b["earth"].at(times).observe(b["sun"]))
    d = np.abs(pa.position.au - pb.position.au).max()
    say("### The subset against the full kernel")
    say()
    say(f"Geocentric astrometric Sun, weekly 1900–2100 ({len(times)} instants): "
        f"max |Δposition| = {d:.3e} AU ({d * AU_KM * 1000:.3f} mm). "
        + ("Identical coefficients." if d == 0 else "NOT identical."))
    say()
    a.close(); b.close()


def meeus_25a():
    """Meeus, Astronomical Algorithms, example 25.a: 1992 Oct 13.0 TD,
    geocentric apparent: RA 13h13m31.4s (198.38083°), Dec −7°47′06″
    (−7.78507°), apparent longitude 199.90895°, R = 0.99766 AU, stated
    accuracy 0.01°."""
    k = load(str(SUBSET))
    t = ts.tt(1992, 10, 13, 0, 0, 0)
    app = k["earth"].at(t).observe(k["sun"]).apparent(deflectors=())
    ra, dec, dist = app.radec(epoch="date")
    _, lon, _ = app.ecliptic_latlon(epoch="date")
    say("### Meeus 25.a (published, non-JPL, stated accuracy 0.01°)")
    say()
    say("| quantity | Meeus | Skyfield/DE440 | difference |")
    say("|---|---|---|---|")
    say(f"| apparent RA of date | 198.38083° | {ra._degrees:.5f}° | {(ra._degrees - 198.38083) * ARCSEC:+.1f}″ |")
    say(f"| apparent Dec of date | −7.78507° | {dec.degrees:.5f}° | {(dec.degrees + 7.78507) * ARCSEC:+.1f}″ |")
    say(f"| apparent longitude | 199.90895° | {lon.degrees:.5f}° | {(lon.degrees - 199.90895) * ARCSEC:+.1f}″ |")
    say(f"| distance R | 0.99766 AU | {dist.au:.5f} AU | {(dist.au - 0.99766) * AU_KM:+.0f} km |")
    # Horizons geocentric at the same TT instant
    text = (HERE / "horizons/geocentric-meeus-25a-tt.txt").read_text()
    body = text[text.index("$$SOE") + 5:text.index("$$EOE")].strip().splitlines()[0]
    f = [x.strip() for x in body.split(",")]
    r = dict(ra_app=float(f[5]), dec_app=float(f[6]))
    say(f"| Horizons (TT), apparent RA/Dec | {r['ra_app']:.5f}°, {r['dec_app']:.5f}° | — | "
        f"vs Skyfield {sep_arcsec(ra._degrees, dec.degrees, r['ra_app'], r['dec_app']):.2f}″ |")
    say()
    k.close()


def delta_t_study():
    """IERS EOP C01: UT1−TAI since 1846 → ΔT = 32.184 − (UT1−TAI)."""
    rec = []
    for line in (HERE / "downloads/eopc01.iau2000.1846-now").read_text().splitlines():
        if line.startswith("#") or not line.strip():
            continue
        f = line.split()
        try:
            mjd = float(f[0]); ut1_tai = float(f[3])
        except (ValueError, IndexError):
            continue
        if ut1_tai > 99.0:  # the series' "no value" sentinel before 1962
            continue
        year = 1858.0 + (mjd + 0.5 - 0.0) / 365.25 - (1858 + 321.5 / 365.25 - 1858.879452)  # rough
        year = 1858.8794 + mjd / 365.25
        if 1900 <= year <= 2027:
            rec.append((year, 32.184 - ut1_tai))
    rec.sort()
    # Before 1962 the IERS series carries no UT1-TAI (its 99.99 sentinel),
    # so the reference there is the published Stephenson–Morrison–
    # Hohenkerk Table S15 (2016, addendum 2020), as Skyfield bundles it:
    # columns year, ΔT (s), ... — a smoothed spline through the
    # observational record, not a polynomial fit.
    import os, skyfield
    s15 = np.load(os.path.join(os.path.dirname(skyfield.__file__), "data",
                               "delta_t.npz"))["Table-S15.2020.txt"]
    # six fields by 58 intervals: start year, end year, three spline
    # coefficients, and ΔT at the start year
    for col in range(s15.shape[1]):
        y, d = float(s15[0, col]), float(s15[5, col])
        if 1900 <= y < 1962:
            rec.append((y, d))
    rec.sort()

    def espenak_meeus(y):
        if y < 1920:
            t = y - 1900
            return -2.79 + 1.494119 * t - 0.0598939 * t**2 + 0.0061966 * t**3 - 0.000197 * t**4
        if y < 1941:
            t = y - 1920
            return 21.20 + 0.84493 * t - 0.076100 * t**2 + 0.0020936 * t**3
        if y < 1961:
            t = y - 1950
            return 29.07 + 0.407 * t - t**2 / 233 + t**3 / 2547
        if y < 1986:
            t = y - 1975
            return 45.45 + 1.067 * t - t**2 / 260 - t**3 / 718
        if y < 2005:
            t = y - 2000
            return (63.86 + 0.3345 * t - 0.060374 * t**2 + 0.0017275 * t**3
                    + 0.000651814 * t**4 + 0.00002373599 * t**5)
        if y < 2050:
            t = y - 2000
            return 62.92 + 0.32217 * t + 0.005589 * t**2
        return -20 + 32 * ((y - 1820) / 100)**2 - 0.5628 * (2150 - y)

    say("### ΔT: the Espenak–Meeus polynomials against the record (Table S15 to 1961, IERS EOP C01 from 1962)")
    say()
    say("| years | reference points | E&M worst |ΔT model − reference| | Skyfield built-in worst |")
    say("|---|---|---|---|")
    ranges = [(1900, 1920), (1920, 1941), (1941, 1961), (1961, 1972), (1972, 1986),
              (1986, 2005), (2005, 2017), (2017, 2026.75)]
    for a, b in ranges:
        pts = [(y, d) for y, d in rec if a <= y < b]
        if not pts:
            continue
        em = max(abs(espenak_meeus(y) - d) for y, d in pts)
        sky = 0.0
        for y, d in pts[::10]:
            yy = int(y); frac = y - yy
            t = ts.utc(yy, 1, 1 + int(frac * 365))
            sky = max(sky, abs(float(t.delta_t) - d))
        say(f"| {a}–{b:g} | {len(pts)} | {em:.2f} s | {sky:.2f} s |")
    say()
    latest = rec[-1]
    say(f"Latest IERS point: {latest[0]:.3f} → ΔT = {latest[1]:.3f} s. "
        f"E&M 2005–2050 at that date: {espenak_meeus(latest[0]):.2f} s "
        f"({espenak_meeus(latest[0]) - latest[1]:+.2f} s). "
        f"The Sun moves 0.041″ per second of time, so that is "
        f"{(espenak_meeus(latest[0]) - latest[1]) * 0.041:+.2f}″ of RA.")
    lsd = (HERE / "downloads/Leap_Second.dat").read_text()
    exp = re.search(r"File expires on\s+(.+)", lsd)
    say(f"IERS Leap_Second.dat: {exp.group(0).strip() if exp else '(no expiry line)'}; "
        f"last entry: {lsd.strip().splitlines()[-1].strip()}")
    say(f"E&M at 2050: {espenak_meeus(2050):.1f} s; at 2100: {espenak_meeus(2100):.1f} s "
        f"(2050–2150 formula; the 2005–2050 formula gives {62.92 + 0.32217*50 + 0.005589*2500:.1f} s at 2050).")
    say()


def main():
    say("# #398 authority comparison, measured " + dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%MZ"))
    say()
    say(f"Skyfield {__import__('skyfield').__version__}, jplephem {__import__('jplephem').__version__}, "
        f"kernel subset `{SUBSET.name}` cut from `de440s.bsp`; Horizons responses in `horizons/`.")
    say()
    subset_equals_full()
    compare(SUBSET, "DE440 subset")
    meeus_25a()
    delta_t_study()
    (HERE / "comparison.md").write_text("\n".join(out) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
