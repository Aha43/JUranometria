#!/usr/bin/env python3
"""#500 authority comparison for Venus: Skyfield on DE440 (de440s.bsp)
against the kept JPL Horizons responses (DE441), offline. Measures the
astrometric and apparent place, the airless horizontal place, distance,
angular diameter, elongation, phase angle, illuminated fraction and the
bright-limb position angle (Meeus 48.5, checked against the same angle
computed from Horizons' own apparent Sun and Venus); then DE440 against
DE442s for Venus and Earth; then the two radius conventions.

Nothing here is a build, test or runtime dependency. Run by hand from
the repository root with the study's virtual environment:

    VENUS_HORIZONS=~/juranometria-runs/venus-horizons \
    python3 scripts/venus-authority-comparison.py > docs/studies/venus/authority-comparison.md
"""
import datetime as dt
import math
import os
import sys
from pathlib import Path

import numpy as np
from skyfield.api import Loader, wgs84
from skyfield.constants import AU_KM, C_AUDAY

HERE = Path(__file__).parent
KERNEL = os.environ.get("VENUS_KERNEL", "imports/raw/solar-system/de440s.bsp")
NEWER = os.environ.get("VENUS_NEWER", "imports/raw/venus/de442s.bsp")
HORIZONS = Path(os.path.expanduser(os.environ.get("VENUS_HORIZONS", "~/juranometria-runs/venus-horizons")))
load = Loader(str(HERE.parent / "build" / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
kernel = load(str(Path(KERNEL).resolve()))
earth, sun, venus = kernel["earth"], kernel["sun"], kernel["venus"]
R_SOLID = 6051.8      # km, pck00011 BODY299_RADII; Magellan cartographic mean, +-1.0
R_CLOUD = 6051.8 + 74.0  # km, mean cloud-top altitude 74 +-1 km (VIRTIS, Ignatiev et al. 2009)
R_OPTICAL = 6131.0    # km, optical radius at 4500 A from the 2012 transit, +-13 (Reale et al. 2015)
ARCSEC = 3600.0
C_KMS = C_AUDAY * AU_KM / 86400.0
ERAS = ["1900-1961", "1962-1971", "1972-2027-06", "after"]
OBSERVERS = {"oslo": (10.75, 59.91), "quito": (281.5, -0.18),
             "cape-town": (18.42, -33.93), "alert": (297.66, 82.50),
             "chatham": (183.5, -43.95)}
out = []


def say(s=""):
    out.append(s)
    print(s, flush=True)


def sep(ra1, dec1, ra2, dec2):
    a1, d1, a2, d2 = map(math.radians, (ra1, dec1, ra2, dec2))
    c = math.sin(d1) * math.sin(d2) + math.cos(d1) * math.cos(d2) * math.cos(a1 - a2)
    return math.degrees(math.acos(max(-1.0, min(1.0, c)))) * ARCSEC


def rows(path):
    text = path.read_text(encoding="utf-8")
    body = text[text.index("$$SOE") + 5:text.index("$$EOE")].strip()
    res = []
    for line in body.splitlines():
        f = [x.strip() for x in line.split(",")]
        stamp = f[0] if "." in f[0] else f[0] + ".000"
        res.append((dt.datetime.strptime(stamp, "%Y-%b-%d %H:%M:%S.%f"), f))
    return res


def era(w):
    if w.year < 1962:
        return "1900-1961"
    if w.year < 1972:
        return "1962-1971"
    if w < dt.datetime(2027, 6, 28):
        return "1972-2027-06"
    return "after"


class Worst:
    def __init__(self):
        self.v = {}

    def note(self, k, val, tag):
        if k not in self.v or abs(val) > abs(self.v[k][0]):
            self.v[k] = (val, tag)

    def get(self, k):
        return self.v.get(k, (0.0, "-"))


ATLAS = {}


def julian(w):
    return (w - dt.datetime(1970, 1, 1)).total_seconds() / 86400.0 + 2440587.5


def atlas_read(instants):
    """The atlas's own TT for each civil instant, from its TimeScales
    (juranometria.tool.TimeScaleTableMain, built by `make classes`)."""
    import subprocess
    todo = sorted({w for w in instants if w not in ATLAS})
    if not todo:
        return
    text = "\n".join(f"{w:%Y-%m-%dT%H:%M:%S}.{w.microsecond:06d}Z" for w in todo) + "\n"
    res = subprocess.run(["java", "-cp", "build/classes:lib/*", "juranometria.tool.TimeScaleTableMain"],
                         input=text, capture_output=True, text=True, check=True)
    for w, line in zip(todo, res.stdout.splitlines()):
        _, jd, confidence = line.split()
        ATLAS[w] = (float(jd), confidence)


def time_of(w, atlas=False):
    """Skyfield's reading of the civil instant (UT1 before 1972, UTC
    from it, its own ΔT), or the atlas's: TT from its TimeScales and
    UT1 = the civil instant, as the atlas's observer takes it."""
    if atlas:
        jd = ATLAS[w][0]
        t = ts.tt_jd(jd)
        t.delta_t = (jd - julian(w)) * 86400.0
        return t
    civil = (w.year, w.month, w.day, w.hour, w.minute, w.second + w.microsecond / 1e6)
    return ts.utc(*civil) if w.year >= 1972 else ts.ut1(*civil)


def observer_for(site):
    if site == "geocentric":
        return earth
    lon, lat = OBSERVERS[site]
    return earth + wgs84.latlon(lat, lon if lon <= 180 else lon - 360, elevation_m=0.0)


def bright_limb_pa(ra_s, dec_s, ra_b, dec_b):
    """Meeus 48.5: position angle of the bright limb's midpoint, from north
    through east, from the Sun (s) and body (b) in one frame, degrees."""
    a0, d0, a, d = map(math.radians, (ra_s, dec_s, ra_b, dec_b))
    y = math.cos(d0) * math.sin(a0 - a)
    x = math.sin(d0) * math.cos(d) - math.cos(d0) * math.sin(d) * math.cos(a0 - a)
    return math.degrees(math.atan2(y, x)) % 360.0


def wrap(d):
    return (d + 540.0) % 360.0 - 180.0


def geometry(t, observer, deflect):
    """Everything the contract would state, for one instant and observer."""
    at = observer.at(t)
    astro = at.observe(venus)
    ra, dec, dist = astro.radec()
    app = astro.apparent(deflectors=(10,) if deflect else ())
    ra_a, dec_a, _ = app.radec(epoch="date")
    s_astro = at.observe(sun)
    s_app = s_astro.apparent(deflectors=())
    sra_a, sdec_a, _ = s_app.radec(epoch="date")
    sra, sdec, _ = s_astro.radec()
    # Phase angle at Venus: from Venus (at emission) to the Sun (light-time
    # from the Sun to Venus) and from Venus to the observer.
    te = ts.tt_jd(t.tt - astro.light_time)
    v_at = venus.at(te)
    to_sun = v_at.observe(sun).position.km
    to_obs = -astro.position.km
    cosi = np.dot(to_sun, to_obs) / (np.linalg.norm(to_sun) * np.linalg.norm(to_obs))
    i = math.degrees(math.acos(max(-1.0, min(1.0, cosi))))
    # Horizons' phi: the same, with the Sun's direction as Venus sees it
    # aberrated by Venus's own (barycentric) velocity, first order.
    n = to_sun / np.linalg.norm(to_sun)
    beta = v_at.velocity.km_per_s / C_KMS
    sun_seen = n + beta - n * np.dot(n, beta)
    cosphi = np.dot(sun_seen, to_obs) / (np.linalg.norm(sun_seen) * np.linalg.norm(to_obs))
    phi = math.degrees(math.acos(max(-1.0, min(1.0, cosphi))))
    # The Moon's ruled S-T-O: that Sun direction, and the down-leg aberrated
    # by the observer's velocity, reversed to point from Venus to the observer.
    to_obs_app = -app.position.km
    cossto = np.dot(sun_seen, to_obs_app) / (np.linalg.norm(sun_seen) * np.linalg.norm(to_obs_app))
    sto = math.degrees(math.acos(max(-1.0, min(1.0, cossto))))
    g = dict(phi=phi, k_phi=(1 + cosphi) / 2, sto=sto, k_sto=(1 + cossto) / 2, ra=ra._degrees, dec=dec.degrees, ra_a=ra_a._degrees, dec_a=dec_a.degrees,
             km=dist.km, i=i, k=(1 + cosi) / 2,
             elong=sep(sra_a._degrees, sdec_a.degrees, ra_a._degrees, dec_a.degrees) / ARCSEC,
             chi_date=bright_limb_pa(sra_a._degrees, sdec_a.degrees, ra_a._degrees, dec_a.degrees),
             chi_j2000=bright_limb_pa(sra._degrees, sdec.degrees, ra._degrees, dec.degrees),
             diam=2 * math.degrees(math.asin(R_SOLID / dist.km)) * ARCSEC)
    if observer is not earth:
        alt, az, _ = app.altaz()
        g.update(alt=alt.degrees, az=az.degrees)
    return g


def compare(deflect, atlas=False):
    worst, counts = Worst(), {}
    elong_bands = Worst()
    for path in sorted(HORIZONS.glob("*.txt")):
        name = path.stem
        if name.endswith("-10"):
            continue
        site = "geocentric" if "geocentric" in name else next(s for s in OBSERVERS if s in name)
        observer = observer_for(site)
        for w, f in rows(path):
            t = time_of(w, atlas)
            g = geometry(t, observer, deflect)
            e = era(w)
            counts[e] = counts.get(e, 0) + 1
            tag = f"{site} {w:%Y-%m-%d %H:%M} ({name})"
            # Venus columns (1,2,4,9,10,13,19,20,23,24,43): 3 RA 4 DEC 5 RAapp
            # 6 DECapp 7 Az 8 El 9 APmag 10 S-brt 11 Illu% 12 Ang-diam 13 r
            # 14 rdot 15 delta 16 deldot 17 S-O-T 18 /r 19 S-T-O 20 phi 21 PABlon 22 PABlat
            worst.note((e, "astrometric"), sep(g["ra"], g["dec"], float(f[3]), float(f[4])), tag)
            d_app = sep(g["ra_a"], g["dec_a"], float(f[5]), float(f[6]))
            worst.note((e, "apparent"), d_app, tag)
            sot = float(f[17])
            band = "≤ 1°" if sot <= 1 else "1–2°" if sot < 2 else "2–5°" if sot < 5 else "> 5°"
            elong_bands.note((band, "every era"), d_app, tag)
            if sot > 1:
                worst.note((e, "apparent beyond 1°"), d_app, tag)
                worst.note((e, "elongation beyond 1°"), (g["elong"] - sot) * ARCSEC, tag)
                worst.note((e, "S-T-O beyond 1°"), (g["sto"] - float(f[19])) * ARCSEC, tag)
            if w.year >= 1962:
                elong_bands.note((band, "from 1962"), d_app, tag)
            if "alt" in g:
                worst.note((e, "horizontal"), sep(g["az"], g["alt"], float(f[7]), float(f[8])), tag)
                gra, gdec, _ = earth.at(t).observe(venus).radec()
                worst.note(("parallax",), sep(gra._degrees, gdec.degrees, g["ra"], g["dec"]), tag)
            worst.note((e, "distance km"), g["km"] - float(f[15]) * AU_KM, tag)
            worst.note((e, "diameter"), g["diam"] - float(f[12]), tag)
            worst.note((e, "elongation"), (g["elong"] - sot) * ARCSEC, tag)
            worst.note((e, "phase S-T-O"), (g["i"] - float(f[19])) * ARCSEC, tag)
            worst.note((e, "phase phi"), (g["i"] - float(f[20])) * ARCSEC, tag)
            worst.note((e, "illuminated"), g["k"] * 100 - float(f[11]), tag)
            worst.note((e, "phi as Horizons"), (g["phi"] - float(f[20])) * ARCSEC, tag)
            worst.note((e, "S-T-O as the Moon"), (g["sto"] - float(f[19])) * ARCSEC, tag)
            worst.note((e, "illuminated from the Moon's S-T-O"), g["k_sto"] * 100 - float(f[11]), tag)
            worst.note((e, "illuminated as Horizons"), g["k_phi"] * 100 - float(f[11]), tag)
            worst.note((e, "illuminated from S-T-O"), (1 + math.cos(math.radians(float(f[19])))) / 2 * 100 - float(f[11]), tag)
    return worst, counts, elong_bands


def chi_check():
    """χ from Skyfield against χ from Horizons' own apparent Sun and Venus,
    wherever both were fetched on one grid."""
    w_chi, n = Worst(), 0
    frame = Worst()
    flags = {"ra": [0, 0], "eclon": [0, 0]}
    from skyfield.framelib import ecliptic_frame
    for path in sorted(HORIZONS.glob("*-299.txt")):
        name = path.stem[:-4]
        sun_path = HORIZONS / f"{name}-10.txt"
        if not sun_path.exists():
            continue
        site = "geocentric" if "geocentric" in name else next(s for s in OBSERVERS if s in name)
        observer = observer_for(site)
        sun_rows = dict(rows(sun_path))
        for w, f in rows(path):
            if w not in sun_rows:
                continue
            fs = sun_rows[w]
            oracle = bright_limb_pa(float(fs[5]), float(fs[6]), float(f[5]), float(f[6]))
            oracle_j2000 = bright_limb_pa(float(fs[3]), float(fs[4]), float(f[3]), float(f[4]))
            g = geometry(time_of(w, True), observer, True)
            tag = f"{site} {w:%Y-%m-%d %H:%M} illu={f[11]}% elong={f[17]}°"
            k = float(f[11])
            cond = "clear of k < 2 % and k ≥ 98 %" if 2.0 <= k < 98.0 else "k < 2 % or k ≥ 98 %"
            w_chi.note(("date", era(w), cond), wrap(g["chi_date"] - oracle) * ARCSEC, tag)
            w_chi.note(("j2000", era(w), cond), wrap(g["chi_j2000"] - oracle_j2000) * ARCSEC, tag)
            frame.note((era(w),), wrap(oracle_j2000 - oracle), tag)
            trailing = "T" in f[18]
            by_ra = wrap(float(f[5]) - float(fs[5])) > 0
            flags["ra"][0 if by_ra == trailing else 1] += 1
            at = observer.at(time_of(w))
            lv = at.observe(venus).apparent().frame_latlon(ecliptic_frame)[1].degrees
            ls = at.observe(sun).apparent().frame_latlon(ecliptic_frame)[1].degrees
            flags["eclon"][0 if (wrap(lv - ls) > 0) == trailing else 1] += 1
            n += 1
    return w_chi, frame, n, flags


def de442_check():
    """DE440 against DE442s: Venus and Earth barycentric, and the derived
    geocentric astrometric place, distance and phase angle, daily 1900-2100."""
    newer = load(str(Path(NEWER).resolve()))
    a = (kernel["venus"], kernel["earth"], kernel["sun"])
    b = (newer["venus"], newer["earth"], newer["sun"])
    days = np.arange(ts.utc(1900, 1, 1).tt, ts.utc(2100, 12, 31).tt, 1.0)
    t = ts.tt_jd(days)
    res = {}
    for label, (v, e, s) in (("440", a), ("442", b)):
        res[label] = dict(v=v.at(t).position.km, e=e.at(t).position.km, s=s.at(t).position.km)
    dv = np.linalg.norm(res["440"]["v"] - res["442"]["v"], axis=0)
    de = np.linalg.norm(res["440"]["e"] - res["442"]["e"], axis=0)
    ds = np.linalg.norm(res["440"]["s"] - res["442"]["s"], axis=0)
    g440 = res["440"]["v"] - res["440"]["e"]
    g442 = res["442"]["v"] - res["442"]["e"]
    n440 = np.linalg.norm(g440, axis=0)
    n442 = np.linalg.norm(g442, axis=0)
    cos = np.sum(g440 * g442, axis=0) / (n440 * n442)
    ang = np.degrees(np.arccos(np.clip(cos, -1, 1))) * ARCSEC
    years = 2000 + (days - 2451545.0) / 365.25
    return dict(dv=dv, de=de, ds=ds, ang=ang, dist=n440 - n442, years=years)


def events_check():
    """The AstroPixels (Espenak) named rows against the instants DE440
    gives: conjunction in geocentric apparent ecliptic longitude of date,
    greatest elongation as the maximum of the geocentric Sun-Venus angle."""
    from skyfield.searchlib import find_discrete, find_maxima
    from skyfield.framelib import ecliptic_frame
    res = []
    path = Path("docs/studies/venus/venus-events.txt")
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#"):
            continue
        name, iso, row = line.split("\t")
        w = dt.datetime.strptime(iso, "%Y-%m-%dT%H:%MZ")
        t0 = ts.utc(w.year, w.month, w.day - 3, w.hour)
        t1 = ts.utc(w.year, w.month, w.day + 3, w.hour)
        e = earth.at
        if "conjunction" in name:
            def east(t):
                a = e(t)
                lv = a.observe(venus).apparent().frame_latlon(ecliptic_frame)[1].degrees
                ls = a.observe(sun).apparent().frame_latlon(ecliptic_frame)[1].degrees
                return ((lv - ls + 540) % 360 - 180) > 0
            east.step_days = 0.5
            times, _ = find_discrete(t0, t1, east)
            def east_ra(t):
                a = e(t)
                rv = a.observe(venus).apparent().radec("date")[0]._degrees
                rs = a.observe(sun).apparent().radec("date")[0]._degrees
                return ((rv - rs + 540) % 360 - 180) > 0
            east_ra.step_days = 0.5
            ra_times, _ = find_discrete(ts.utc(w.year, w.month, w.day - 5, w.hour),
                                        ts.utc(w.year, w.month, w.day + 5, w.hour), east_ra)
            when, value = times[0], ("ra", ra_times[0].utc_datetime().replace(tzinfo=None))
        else:
            def elong(t):
                a = e(t)
                return a.observe(venus).apparent().separation_from(a.observe(sun).apparent()).degrees
            elong.step_days = 0.5
            times, values = find_maxima(t0, t1, elong)
            when, value = times[0], values[0]
            listed = elong(ts.utc(w.year, w.month, w.day, w.hour, w.minute))
            value = (value, (value - listed) * ARCSEC)
        ours = when.utc_datetime().replace(tzinfo=None)
        res.append((name, row, w, ours, value))
    return res


def main():
    deflect_only = "--deflect" in sys.argv
    say("# #500 Venus authority comparison, measured "
        + dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%MZ"))
    say()
    say(f"Skyfield on `{Path(KERNEL).name}` (DE440) against the kept JPL Horizons responses"
        f" (Horizons: DE441, Venus radius 6051.8 km). Observers at sea level. The proposed contract reads"
        f" each civil instant with the atlas's own `TimeScales` (TT from the leap-second record, Espenak-Meeus"
        f" ΔT outside it) and UT1 = the civil instant; the two Skyfield passes read it with Skyfield's"
        f" (UT1 before 1972, UTC from it, its own ΔT), which isolates the time-scale model's cost.")
    atlas_read([w for path in HORIZONS.glob("*.txt") for w, _ in rows(path)])
    passes = [(True, True)] if deflect_only else [(True, True), (False, False), (True, False)]
    for deflect, atlas in passes:
        worst, counts, bands = compare(deflect, atlas)
        say()
        say(f"## {'The proposed contract' if atlas else 'Skyfield'} against Horizons:"
            f" {'the atlas' if atlas else 'Skyfield'}'s time scale,"
            f" deflection by the Sun {'applied' if deflect else 'not applied'}")
        say()
        say("| era | rows | astrometric | apparent | apparent, elongation > 1° | horizontal | distance | diameter | elongation | S-T-O, both aberrations (the Moon's) | its k vs Illu% | phi, the Sun's aberration only | its k vs Illu% | neither aberration vs S-T-O | vs phi | its k vs Illu% |")
        say("|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|")
        for e in ERAS:
            g = lambda k: worst.get((e, k))[0]
            say(f"| {e} | {counts.get(e, 0)} | {g('astrometric'):.3f}″ | {g('apparent'):.3f}″ | {g('apparent beyond 1°'):.3f}″ |"
                f" {g('horizontal'):.2f}″ | {g('distance km'):+.1f} km | {g('diameter'):+.4f}″ |"
                f" {g('elongation'):+.2f}″ | {g('S-T-O as the Moon'):+.2f}″ | {g('illuminated from the Moon' + chr(39) + 's S-T-O'):+.5f} % |"
                f" {g('phi as Horizons'):+.2f}″ | {g('illuminated as Horizons'):+.5f} % |"
                f" {g('phase S-T-O'):+.2f}″ | {g('phase phi'):+.2f}″ | {g('illuminated'):+.4f} % |")
        say()
        v, tag = worst.get(("parallax",))
        say(f"Parallax applied (topocentric minus geocentric astrometric place, every observer row): up to {v:.2f}″ at {tag}.")
        say()
        say("Beyond 1° from the Sun, by era: elongation " + ", ".join(
            f"{e} {worst.get((e, 'elongation beyond 1°'))[0]:+.2f}″" for e in ERAS) + "; S-T-O " + ", ".join(
            f"{e} {worst.get((e, 'S-T-O beyond 1°'))[0]:+.2f}″" for e in ERAS) + ".")
        say()
        say("Apparent place by Venus's elongation from the Sun (worst, every era):")
        for band in ["≤ 1°", "1–2°", "2–5°", "> 5°"]:
            v, tag = bands.get((band, "every era"))
            v2, tag2 = bands.get((band, "from 1962"))
            say(f"- elongation {band}: {v:.3f}″ at {tag}; from 1962 {v2:.3f}″ at {tag2}")
        say()
        say("Where the worst rows are:")
        for key, (v, tag) in sorted(worst.v.items()):
            if len(key) == 2:
                say(f"- {key[0]}, {key[1]}: {v:+.5f} at {tag}")
    if deflect_only:
        Path("build").mkdir(exist_ok=True)
        return
    say()
    w_chi, frame, n, flags = chi_check()
    say("## Bright-limb position angle χ (Meeus 48.5)")
    say()
    say(f"The proposed contract's χ (the atlas's time scale, Venus deflected by the Sun) against χ from Horizons'"
        f" own Sun and Venus, every row where both were fetched ({n} rows), worst by era:")
    say()
    say("| era | of date, clear of k < 2 % and k ≥ 98 % | of date, near those | J2000, clear | J2000, near |")
    say("|---|---|---|---|---|")
    for e in ERAS:
        cells = []
        for frame_name in ("date", "j2000"):
            for cond in ("clear of k < 2 % and k ≥ 98 %", "k < 2 % or k ≥ 98 %"):
                v, _ = w_chi.get((frame_name, e, cond))
                cells.append(f"{v:+.2f}″")
        say(f"| {e} | " + " | ".join(cells) + " |")
    say()
    say("Where the worst rows are:")
    for key, (v, tag) in sorted(w_chi.v.items()):
        say(f"- {key[0]}, {key[1]}, {key[2]}: {v:+.2f}″ at {tag}")
    say()
    say("Horizons' /T (trailing: east of the Sun, evening) and /L (leading: west, morning) flag, against two"
        " definitions of east, same rows:")
    say(f"- apparent right ascension of date, Venus minus Sun: agrees on {flags['ra'][0]}, disagrees on {flags['ra'][1]}")
    say(f"- apparent ecliptic longitude of date, Venus minus Sun: agrees on {flags['eclon'][0]}, disagrees on {flags['eclon'][1]}")
    say()
    say("The frame matters: χ in ICRF/J2000 minus χ of date, Horizons' own positions, largest by era:")
    for e in ERAS:
        v, tag = frame.get((e,))
        say(f"- {e}: {v:+.3f}° at {tag}")
    say()
    d = de442_check()
    say("## DE440 against DE442s (daily, 1900-01-01 to 2100-12-31, TT)")
    say()
    say("| quantity | worst | where |")
    say("|---|---|---|")
    for label, arr, unit in (("Venus barycentric position", d["dv"], "km"),
                             ("Earth barycentric position", d["de"], "km"),
                             ("Sun barycentric position", d["ds"], "km"),
                             ("geocentric direction of Venus", d["ang"], "″"),
                             ("geocentric distance of Venus", d["dist"], "km")):
        j = int(np.argmax(np.abs(arr)))
        say(f"| {label} | {arr[j]:+.4f} {unit} | {d['years'][j]:.2f} |")
    for lo, hi in ((1900, 1962), (1962, 2027.49), (2027.49, 2101)):
        m = (d["years"] >= lo) & (d["years"] < hi)
        say(f"| geocentric direction, {lo:g}–{hi:g} | {np.max(d['ang'][m]):.4f} ″ | |")
    say()
    say("## The named events: Espenak (AstroPixels) against DE440")
    say()
    say("Conjunction: Venus and the Sun at one apparent ecliptic longitude of date; greatest elongation: the"
        " maximum of the geocentric apparent Sun-Venus angle. Espenak states ±30 minutes for both.")
    say()
    say("| case | published row (UT, to the hour) | DE440 instant (UT) | difference | DE440 greatest elongation, or the conjunction in RA | its excess over the listed hour's |")
    say("|---|---|---|---|---|---|")
    for name, row, w, ours, value in events_check():
        diff = (ours - w).total_seconds() / 60
        say(f"| {name} | {row} | {ours:%Y-%m-%d %H:%M:%S} | {diff:+.0f} min |"
            + (f" in right ascension {value[1]:%Y-%m-%d %H:%M} | |" if value[0] == "ra"
               else f" {value[0]:.3f}° | {value[1]:.2f}″ |"))
    say()
    say("## What a second of time costs")
    say()
    rate, rate_tag, dd = 0.0, "-", 0.0
    for path in sorted(HORIZONS.glob("dense-*-299.txt")) + [HORIZONS / "matrix-7d-geocentric-299.txt"]:
        r = rows(path)
        for (w0, f0), (w1, f1) in zip(r, r[1:]):
            dtsec = (w1 - w0).total_seconds()
            v = sep(float(f0[3]), float(f0[4]), float(f1[3]), float(f1[4])) / dtsec
            if v > rate:
                rate, rate_tag = v, f"{w0:%Y-%m-%d %H:%M} ({path.stem})"
            dd = max(dd, abs(float(f0[16])))
    say(f"- Venus's fastest astrometric motion on the kept grids: {rate:.4f}″ per second of time"
        f" ({rate * 60:.2f}″ a minute) at {rate_tag}; its fastest range rate {dd:.2f} km/s.")
    say("- The Earth turns 15.04″ per second of UT1: a time error moves the horizontal place by up to that.")
    say("- So one second of civil-time error costs Venus's equatorial place at most"
        f" {rate:.3f}″ and its distance at most {dd:.1f} km, and its horizontal place up to 15″.")
    for w in (dt.datetime(2027, 6, 28), dt.datetime(2050, 1, 1), dt.datetime(2100, 12, 31, 23, 59, 59)):
        atlas_read([w])
        atlas_dt = (ATLAS[w][0] - julian(w)) * 86400.0
        sky_dt = float(time_of(w).delta_t)
        say(f"- ΔT at {w:%Y-%m-%d %H:%M:%S}: the atlas's {atlas_dt:.1f} s ({ATLAS[w][1]}), Skyfield's {sky_dt:.1f} s;"
            f" apart {atlas_dt - sky_dt:+.1f} s.")
    say("- Horizons' own ΔT is not printed; the Skyfield passes' 411″ horizontal says it and Skyfield's part by"
        " about 27 s by 2100, and the proposed contract's after-era astrometric and distance differences above"
        " are the atlas's ΔT parting from Horizons' at the rates above - the cost of the accepted model, measured.")
    say()
    say("## The radius: solid body against cloud top")
    say()
    near = far = None
    for path in sorted(HORIZONS.glob("matrix-7d-geocentric-299.txt")):
        r = rows(path)
        near = min(r, key=lambda x: float(x[1][15]))
        far = max(r, key=lambda x: float(x[1][15]))
    say("| case | distance | solid 6051.8 km | cloud top +74 km | optical +79 km (4500 Å) | cloud − solid | Horizons Ang-diam |")
    say("|---|---|---|---|---|---|---|")
    for label, (w, f) in (("nearest (geocentric, 7-day)", near), ("farthest (geocentric, 7-day)", far)):
        km = float(f[15]) * AU_KM
        dsolid = 2 * math.degrees(math.asin(R_SOLID / km)) * ARCSEC
        dcloud = 2 * math.degrees(math.asin(R_CLOUD / km)) * ARCSEC
        dopt = 2 * math.degrees(math.asin(R_OPTICAL / km)) * ARCSEC
        say(f"| {label}, {w:%Y-%m-%d} | {km:,.0f} km | {dsolid:.3f}″ | {dcloud:.3f}″ | {dopt:.3f}″ |"
            f" {dcloud - dsolid:+.3f}″ ({(R_CLOUD / R_SOLID - 1) * 100:.2f} %) | {float(f[12]):.3f}″ |")
    Path("build").mkdir(exist_ok=True)


if __name__ == "__main__":
    main()
