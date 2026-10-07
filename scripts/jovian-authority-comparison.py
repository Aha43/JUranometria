#!/usr/bin/env python3
"""#472 authority comparison for Jupiter and the Galilean moons: Skyfield
on NAIF's jup365.bsp (which carries DE440's Jupiter barycentre, Sun and
Earth) against JPL Horizons, offline from the kept responses; plus the
visibility states (clear, transiting, behind, eclipsed) computed from the
same geometry and held to Horizons' visibility codes and to IMCCE's
predicted event minutes for the named 11 December 2026 case.

Nothing here is a build, test or runtime dependency. Run by hand from
the repository root with the study's virtual environment:

    JOVIAN_KERNEL=imports/raw/jovian-system/jup365.bsp \
    JOVIAN_HORIZONS=docs/studies/jovian-system/horizons \
    python3 scripts/jovian-authority-comparison.py > docs/studies/jovian-system/authority-comparison.md
"""
import datetime as dt
import math
import os
import sys
from pathlib import Path

import numpy as np
from skyfield.api import Loader, wgs84
from skyfield.constants import AU_KM

HERE = Path(__file__).parent
KERNEL = os.environ.get("JOVIAN_KERNEL", "imports/raw/jovian-system/jup365.bsp")
HORIZONS = Path(os.environ.get("JOVIAN_HORIZONS", "docs/studies/jovian-system/horizons"))
FILTER = os.environ.get("JOVIAN_FILTER", "")   # a substring every compared file name must contain
DEFLECTORS = (10,) if "--deflect" in sys.argv else ()   # Sun-only gravitational deflection, as an experiment
load = Loader(str(HERE.parent / "build" / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
kernel = load(str(Path(KERNEL).resolve()))
earth, sun, jupiter = kernel["earth"], kernel["sun"], kernel[599]
MOONS = {"io": 501, "europa": 502, "ganymede": 503, "callisto": 504}
# Radii as pck00011.tpc states them (km): the equatorial value of each
# triaxial shape, which is what Horizons' limb-to-limb codes use.
R_SUN = 695700.0
R_JUPITER_EQ = 71492.0
R_JUPITER_POLAR = 66854.0
R_MOON = {501: 1829.4, 502: 1562.6, 503: 2631.2, 504: 2410.3}
ARCSEC = 3600.0

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
        when = dt.datetime.strptime(stamp, "%Y-%b-%d %H:%M:%S.%f")
        res.append((when, f))
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


def time_of(w):
    civil = (w.year, w.month, w.day, w.hour, w.minute, w.second + w.microsecond / 1e6)
    return ts.utc(*civil) if w.year >= 1972 else ts.ut1(*civil)


def observer_for(name):
    if name == "geocentric":
        return earth
    lon, lat = OBSERVERS[name]
    return earth + wgs84.latlon(lat, lon if lon <= 180 else lon - 360, elevation_m=0.0)


def visibility(t, observer, moon_id):
    """The moon's state in the geometry Horizons' codes describe:
    limb-to-limb, equatorial radii, light-time considered. Returns
    (disc, shadow, depth_km, sep_arcsec) with disc in {"clear","transit",
    "occulted"} and shadow in {"lit","partial","total"}."""
    moon = kernel[moon_id]
    at = observer.at(t)
    j = at.observe(jupiter)
    m = at.observe(moon)
    J = j.position.km
    M = m.position.km
    jd = np.linalg.norm(J)
    md = np.linalg.norm(M)
    # limb-to-limb angular test, equatorial radii, as Horizons states
    angle = math.acos(max(-1.0, min(1.0, float(np.dot(J, M)) / (jd * md))))
    r_j = math.asin(R_JUPITER_EQ / jd)
    r_m = math.asin(R_MOON[moon_id] / md)
    overlaps = angle < r_j + r_m
    depth = md - jd
    disc = "clear" if not overlaps else ("transit" if depth < 0 else "occulted")
    if OBLATE:
        # the apparent figure: the moon's plane-of-sky offset in Jupiter's own
        # equatorial basis, against the projected ellipse (semi-axes R_eq and
        # b' = sqrt(R_p^2 cos^2 B + R_eq^2 sin^2 B)), limb-to-limb with the
        # moon's radius added along the offset direction
        pole = jupiter_pole(t)
        los = J / jd
        up = pole - np.dot(pole, los) * los          # pole projected on the sky
        up /= np.linalg.norm(up)
        east = np.cross(up, los)                      # completes the sky basis
        d = M - J
        px, py = float(np.dot(d, east)), float(np.dot(d, up))   # km in the plane of sky, at Jupiter's distance
        sub_lat = math.asin(max(-1.0, min(1.0, float(np.dot(pole, -los)))))
        b = math.sqrt(R_JUPITER_POLAR ** 2 * math.cos(sub_lat) ** 2 + R_JUPITER_EQ ** 2 * math.sin(sub_lat) ** 2)
        rho = math.hypot(px, py)
        if rho == 0.0:
            inside = True
        else:
            # the ellipse's radius along the offset direction, plus the moon's radius
            ux, uy = px / rho, py / rho
            r_edge = 1.0 / math.sqrt((ux / R_JUPITER_EQ) ** 2 + (uy / b) ** 2)
            inside = rho < r_edge + R_MOON[moon_id] * (jd / md)
        disc = "clear" if not inside else ("transit" if depth < 0 else "occulted")
    # shadow: at the moon's emission epoch, relative to the Sun-Jupiter axis
    lt = m.light_time  # days
    te = ts.tt_jd(t.tt - lt)
    S = sun.at(te).position.km
    P = jupiter.at(te).position.km
    Q = moon.at(te).position.km
    axis = P - S
    d_sj = np.linalg.norm(axis)
    u = axis / d_sj
    r = Q - P
    along = float(np.dot(r, u))
    perp = float(np.linalg.norm(r - along * u))
    shadow = "lit"
    if along > 0:
        umbra = R_JUPITER_EQ - along * (R_SUN - R_JUPITER_EQ) / d_sj
        if umbra > 0:
            if perp + R_MOON[moon_id] <= umbra:
                shadow = "total"
            elif perp - R_MOON[moon_id] < umbra:
                shadow = "partial"
    return disc, shadow, depth, math.degrees(angle) * ARCSEC


OBLATE = "--oblate" in sys.argv
POLE_RA0, POLE_RA1, POLE_DEC0, POLE_DEC1 = 268.056595, -0.006499, 64.495303, 0.002413  # pck00011.tpc, IAU 2015


def jupiter_pole(t):
    """Jupiter's north-pole direction (ICRF unit vector) at t, from the PCK."""
    T = (t.tt - 2451545.0) / 36525.0
    ra = math.radians(POLE_RA0 + POLE_RA1 * T)
    dec = math.radians(POLE_DEC0 + POLE_DEC1 * T)
    return np.array([math.cos(dec) * math.cos(ra), math.cos(dec) * math.sin(ra), math.sin(dec)])


def code_of(disc, shadow):
    if disc == "transit":
        return "t"
    if disc == "occulted":
        return {"lit": "O", "partial": "P", "total": "U"}[shadow]
    return {"lit": "*", "partial": "p", "total": "u"}[shadow]


def compare_jupiter(worst, counts):
    # columns: 0 date 3 RA 4 DEC 5 RAapp 6 DECapp 7 Az 8 El 9 Illu% 10 Ang-diam
    # 11 NP.ang 12 NP.dist 13 delta 14 deldot 15 S-T-O 16 NPole-RA 17 NPole-DC 18 phi
    for path in sorted(HORIZONS.glob("*-jupiter-*.txt")):
        if FILTER and FILTER not in path.name:
            continue
        site = path.stem.split("-jupiter-")[1]
        observer = observer_for(site)
        topocentric = site != "geocentric"
        for w, f in rows(path):
            t = time_of(w)
            astro = observer.at(t).observe(jupiter)
            ra, dec, dist = astro.radec()
            app = astro.apparent(deflectors=DEFLECTORS)
            ra_a, dec_a, _ = app.radec(epoch="date")
            e = era(w)
            counts[("jupiter", e)] = counts.get(("jupiter", e), 0) + 1
            tag = f"{site} {w:%Y-%m-%d %H:%M}"
            worst.note(("jupiter", e, "astrometric"), sep(ra._degrees, dec.degrees, float(f[3]), float(f[4])), tag)
            apparent_diff = sep(ra_a._degrees, dec_a.degrees, float(f[5]), float(f[6]))
            worst.note(("jupiter", e, "apparent"), apparent_diff, tag)
            # the same difference by the Sun's elongation, for the deflection question
            sun_dir = observer.at(t).observe(sun).position.km
            elong = math.degrees(math.acos(max(-1.0, min(1.0, float(np.dot(sun_dir, astro.position.km))
                                                           / (np.linalg.norm(sun_dir) * np.linalg.norm(astro.position.km))))))
            band = "< 1°" if elong < 1 else "1–5°" if elong < 5 else "5–20°" if elong < 20 else "> 20°"
            worst.note(("jupiter", "elongation " + band, "apparent"), apparent_diff, tag + f" (elongation {elong:.2f}°)")
            if topocentric:
                alt_, az, _ = app.altaz()
                worst.note(("jupiter", e, "horizontal"), sep(az.degrees, alt_.degrees, float(f[7]), float(f[8])), tag)
            worst.note(("jupiter", e, "distance km"), dist.km - float(f[13]) * AU_KM, tag)
            diam_asin = 2 * math.degrees(math.asin(R_JUPITER_EQ / dist.km)) * ARCSEC
            diam_atan = 2 * math.degrees(math.atan(R_JUPITER_EQ / dist.km)) * ARCSEC
            worst.note(("jupiter", e, "diameter asin"), diam_asin - float(f[10]), tag)
            worst.note(("jupiter", e, "diameter atan"), diam_atan - float(f[10]), tag)
            # phase angle at Jupiter from the geometry (geometric, light-time at Jupiter)
            J = astro.position.km
            te = ts.tt_jd(t.tt - astro.light_time)
            s_from_j = sun.at(te).position.km - jupiter.at(te).position.km
            cosi = float(np.dot(-J, s_from_j)) / (np.linalg.norm(J) * np.linalg.norm(s_from_j))
            i = math.degrees(math.acos(max(-1, min(1, cosi))))
            worst.note(("jupiter", e, "phase angle deg"), i - float(f[15]), tag)
            k = (1 + cosi) / 2 * 100
            worst.note(("jupiter", e, "illuminated %"), k - float(f[9]), tag)


def compare_moons(worst, counts, states):
    # columns: 0 date 3 RA 4 DEC 5 RAapp 6 DECapp 7 Az 8 El 9 X 10 Y 11 SatPANG
    # 12 ang-sep 13 vis 14 Ang-diam 15 delta 16 deldot 17 S-T-O
    for label, moon_id in MOONS.items():
        moon = kernel[moon_id]
        for path in sorted(HORIZONS.glob(f"*-{label}-*.txt")):
            if FILTER and FILTER not in path.name:
                continue
            site = path.stem.split(f"-{label}-")[1]
            observer = observer_for(site)
            topocentric = site != "geocentric"
            for w, f in rows(path):
                t = time_of(w)
                at = observer.at(t)
                astro = at.observe(moon)
                ra, dec, dist = astro.radec()
                app = astro.apparent(deflectors=DEFLECTORS)
                ra_a, dec_a, _ = app.radec(epoch="date")
                japp = at.observe(jupiter).apparent(deflectors=DEFLECTORS)
                jra_a, jdec_a, _ = japp.radec(epoch="date")
                jastro = at.observe(jupiter)
                jra, jdec, _ = jastro.radec()
                e = era(w)
                counts[(label, e)] = counts.get((label, e), 0) + 1
                tag = f"{site} {w:%Y-%m-%d %H:%M}"
                worst.note((label, e, "astrometric"), sep(ra._degrees, dec.degrees, float(f[3]), float(f[4])), tag)
                worst.note((label, e, "apparent"), sep(ra_a._degrees, dec_a.degrees, float(f[5]), float(f[6])), tag)
                if topocentric:
                    alt_, az, _ = app.altaz()
                    worst.note((label, e, "horizontal"), sep(az.degrees, alt_.degrees, float(f[7]), float(f[8])), tag)
                worst.note((label, e, "distance km"), dist.km - float(f[15]) * AU_KM, tag)
                # X/Y from apparent-of-date places (Horizons: "apparent differential coordinates")
                dra = (ra_a._degrees - jra_a._degrees + 540.0) % 360.0 - 180.0
                x_app = dra * math.cos(math.radians(jdec_a.degrees)) * ARCSEC
                y_app = (dec_a.degrees - jdec_a.degrees) * ARCSEC
                dra0 = (ra._degrees - jra._degrees + 540.0) % 360.0 - 180.0
                x_ast = dra0 * math.cos(math.radians(jdec.degrees)) * ARCSEC
                y_ast = (dec.degrees - jdec.degrees) * ARCSEC
                worst.note((label, e, "X apparent basis"), x_app - float(f[9]), tag)
                worst.note((label, e, "Y apparent basis"), y_app - float(f[10]), tag)
                worst.note((label, e, "X astrometric basis"), x_ast - float(f[9]), tag)
                worst.note((label, e, "Y astrometric basis"), y_ast - float(f[10]), tag)
                pa = math.degrees(math.atan2(x_app, y_app)) % 360.0
                dpa = (pa - float(f[11]) + 540.0) % 360.0 - 180.0
                worst.note((label, e, "position angle deg"), dpa, tag)
                disc, shadow, depth, sep_ours = visibility(t, observer, moon_id)
                worst.note((label, e, "separation arcsec"), sep_ours - float(f[12]), tag)
                ours = code_of(disc, shadow)
                theirs = f[13].strip("/").strip() or "*"
                key = (label, "agree" if ours == theirs else "disagree")
                states[key] = states.get(key, 0) + 1
                if ours != theirs:
                    states.setdefault((label, "cases"), []).append(
                            f"{tag}: ours {ours} ({disc}, {shadow}, depth {depth:+.0f} km,"
                            f" sep {sep_ours:.2f}\") vs Horizons {theirs} (sep {f[12]})")


def transitions(label, moon_id, site):
    """First and last minute of each Horizons visibility code, and ours,
    on the named evening's minute series."""
    path = HORIZONS / f"triple-transit-2026-12-11-minutes-{label}-{site}.txt"
    if not path.exists():
        return []
    observer = observer_for(site)
    res = []
    prev_h = prev_o = None
    for w, f in rows(path):
        theirs = f[13].strip("/").strip() or "*"
        disc, shadow, depth, _ = visibility(time_of(w), observer, moon_id)
        ours = code_of(disc, shadow)
        if theirs != prev_h:
            res.append(f"{w:%H:%M} Horizons -> {theirs}")
            prev_h = theirs
        if ours != prev_o:
            res.append(f"{w:%H:%M} ours     -> {ours}")
            prev_o = ours
    return res


def main():
    say("# #472 Jovian authority comparison, measured "
        + dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%MZ"))
    say()
    say(f"Skyfield on `{Path(KERNEL).name}` (JUP365 with DE440) against the kept JPL Horizons"
        f" responses under `{HORIZONS}` (Horizons: jup365 merged, DE441). Observers at sea level.")
    say()
    worst = Worst()
    counts = {}
    states = {}
    if "--december-only" not in sys.argv:
        compare_jupiter(worst, counts)
        compare_moons(worst, counts, states)
    say(f"Disc test: {'the oblate apparent figure with the PCK pole (--oblate)' if OBLATE else 'the equatorial sphere, as Horizons'}."
        f" Deflection: {'the Sun (--deflect)' if DEFLECTORS else 'none, as the atlas computes'}."
        + (f" Files: those containing '{FILTER}'." if FILTER else ""))
    eras = ["1900-1961", "1962-1971", "1972-2027-06", "after"]
    say("## Jupiter: worst absolute difference by era")
    say()
    say("| era | rows | astrometric | apparent | horizontal | distance | diameter (asin) | diameter (atan) | phase angle | illuminated |")
    say("|---|---|---|---|---|---|---|---|---|---|")
    for e in eras:
        g = lambda k: worst.get(("jupiter", e, k))[0]
        say(f"| {e} | {counts.get(('jupiter', e), 0)} | {g('astrometric'):.3f}″ | {g('apparent'):.3f}″ |"
            f" {g('horizontal'):.2f}″ | {g('distance km'):+.1f} km | {g('diameter asin'):+.4f}″ |"
            f" {g('diameter atan'):+.4f}″ | {g('phase angle deg') * 3600:+.2f}″ | {g('illuminated %'):+.4f} % |")
    say()
    say("Jupiter's apparent place by the Sun's elongation (worst difference, every era):")
    for band in ["< 1°", "1–5°", "5–20°", "> 20°"]:
        v, tag = worst.get(("jupiter", "elongation " + band, "apparent"))
        say(f"- elongation {band}: {v:.3f}″ at {tag}")
    for label in MOONS:
        say()
        say(f"## {label.capitalize()}: worst absolute difference by era")
        say()
        say("| era | rows | astrometric | apparent | horizontal | distance | X (apparent basis) | Y (apparent basis) | X (astrometric basis) | Y (astrometric basis) | position angle | separation |")
        say("|---|---|---|---|---|---|---|---|---|---|---|---|")
        for e in eras:
            g = lambda k: worst.get((label, e, k))[0]
            say(f"| {e} | {counts.get((label, e), 0)} | {g('astrometric'):.3f}″ | {g('apparent'):.3f}″ |"
                f" {g('horizontal'):.2f}″ | {g('distance km'):+.1f} km | {g('X apparent basis'):+.3f}″ |"
                f" {g('Y apparent basis'):+.3f}″ | {g('X astrometric basis'):+.3f}″ | {g('Y astrometric basis'):+.3f}″ |"
                f" {g('position angle deg'):+.4f}° | {g('separation arcsec'):+.3f}″ |")
    say()
    say("## Visibility states against Horizons' codes (limb-to-limb, equatorial radii)")
    say()
    say("| moon | rows agreeing | rows disagreeing |")
    say("|---|---|---|")
    for label in MOONS:
        say(f"| {label} | {states.get((label, 'agree'), 0)} | {states.get((label, 'disagree'), 0)} |")
    for label in MOONS:
        cases = states.get((label, "cases"), [])
        if cases:
            say()
            say(f"Disagreements, {label} ({len(cases)}):")
            for c in cases[:60]:
                say(f"- {c}")
            if len(cases) > 60:
                say(f"- … and {len(cases) - 60} more")
    say()
    say("## The named evening, 11 December 2026: every state transition, minute series")
    for site in ("geocentric", "oslo"):
        for label, moon_id in MOONS.items():
            tr = transitions(label, moon_id, site)
            if tr:
                say()
                say(f"**{label.capitalize()}, {site}:**")
                for line in tr:
                    say(f"- {line}")
    say()
    say("Where the worst rows are:")
    for key, (v, tag) in sorted(worst.v.items()):
        say(f"- {key[0]}, {key[1]}, {key[2]}: {v:.5f} at {tag}")


if __name__ == "__main__":
    main()
