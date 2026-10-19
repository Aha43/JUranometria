#!/usr/bin/env python3
"""#574 authority comparison for Saturn and its eight major moons:
Skyfield on NAIF's sat441.bsp against the kept JPL Horizons responses,
offline; the planetary ephemeris separated from sat441's own Saturn-centre
and moon solution by a second, independent planetary ephemeris (DE442s)
against the released DE440 (de440s); the ring geometry from the PCK pole
and the retained PDS ring table; and the visibility states against
Horizons' codes.

Every constant is read from a pinned file, never typed: radii and poles
from pck00011.tpc, ring boundaries from the retained PDS table
(docs/studies/saturn-system/rings/pds-rings-table.tsv, via its digest-
verified page), the exactness boundary of the civil time scale from the
application's own Leap_Second.dat. Every input is verified against its
SHA-256 before it is read, and every Horizons response against the body
digest its header records.

Nothing here is a build, test or runtime dependency. Run by hand from the
repository root with the study's virtual environment:

    ~/juranometria-runs/venv/bin/python scripts/saturn-authority-comparison.py \\
        > build/saturn-authority-comparison/report.md

SATURN_RETAINED names the directory of the retained geocentric matrices
(default ~/juranometria-runs/saturn-horizons-retained).
"""
import datetime as dt
import hashlib
import math
import os
import re
import sys
from pathlib import Path

import numpy as np
from skyfield.api import Loader, wgs84
from skyfield.constants import AU_KM, C_AUDAY
from skyfield.jpllib import Stack

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "imports/raw"
STUDY = ROOT / "docs/studies/saturn-system"
HORIZONS = STUDY / "horizons"
RETAINED = Path(os.environ.get("SATURN_RETAINED",
                               str(Path.home() / "juranometria-runs/saturn-horizons-retained")))

PINS = {
    RAW / "saturn-system/sat441.bsp": "d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5",
    RAW / "saturn-system/de442s.bsp": "54d97562a5b094d298b1b8eafa5a2e17e3e010ce85e1a366d07f003ad159323c",
    RAW / "solar-system/de440s.bsp": "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2",
    RAW / "jovian-system/pck00011.tpc": "3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1",
    STUDY / "rings/saturn_tables.html": "2dea8fcda149c94324353ccfb47e3df27f5dfef00ba8d62ab738a917378fc64c",
}
LEAP = ROOT / "src/resources/solar-system/Leap_Second.dat"
ARCSEC = 3600.0
MOONS = {"mimas": 601, "enceladus": 602, "tethys": 603, "dione": 604,
         "rhea": 605, "titan": 606, "hyperion": 607, "iapetus": 608}
OBSERVERS = {"oslo": (10.75, 59.91), "quito": (281.5, -0.18),
             "cape-town": (18.42, -33.93), "alert": (297.66, 82.50),
             "chatham": (183.5, -43.95)}


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            h.update(block)
    return h.hexdigest()


def stop(message):
    sys.exit(f"saturn-authority-comparison: {message} - stopping")


for path, pin in PINS.items():
    if sha256(path) != pin:
        stop(f"{path} does not match its pin")


def pck_constants(path):
    """Every BODYnnn_X assignment inside the kernel's \\begindata blocks."""
    values, data, pending = {}, False, None
    for line in path.read_text(encoding="ascii", errors="replace").splitlines():
        s = line.strip()
        if s == "\\begindata":
            data = True
            continue
        if s == "\\begintext":
            data = False
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
    return {k: [float(x.replace("D", "E")) for x in v.strip("() ").split()]
            for k, v in values.items()}


PCK = pck_constants(RAW / "jovian-system/pck00011.tpc")
R_SUN = PCK["BODY10_RADII"][0]
R_EQ, _, R_POLAR = PCK["BODY699_RADII"]
POLE_RA, POLE_DEC = PCK["BODY699_POLE_RA"], PCK["BODY699_POLE_DEC"]
if any(k.startswith("BODY699_NUT_PREC") for k in PCK):
    stop("the PCK gives Saturn's pole nutation terms this study does not model")
R_MOON = {i: PCK[f"BODY{i}_RADII"] for i in MOONS.values()}


def ring_table():
    path = STUDY / "rings/pds-rings-table.tsv"
    lines = [l for l in path.read_text(encoding="utf-8").splitlines() if not l.startswith("#")]
    if PINS[STUDY / "rings/saturn_tables.html"] not in path.read_text(encoding="utf-8").splitlines()[0]:
        stop(f"{path.name} was not extracted from the pinned page")
    head = lines[0].split("\t")
    rows = {}
    for line in lines[1:]:
        cells = dict(zip(head, line.split("\t")))
        rows[cells["Feature"]] = cells
    return rows


RINGS = ring_table()


def km(feature, edge):
    return float(RINGS[feature][f"{edge} Boundary (km)"].replace(",", ""))


EXPIRES = dt.datetime.strptime(
    re.search(r"File expires on\s+(\d{1,2}\s+[A-Za-z]+\s+\d{4})", LEAP.read_text()).group(1),
    "%d %B %Y")
ERAS = ["1900-1961", "1962-1971", f"1972-{EXPIRES:%Y-%m}", "after"]

load = Loader(str(ROOT / "build" / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
sat = load(str(RAW / "saturn-system/sat441.bsp"))
de440 = load(str(RAW / "solar-system/de440s.bsp"))
de442 = load(str(RAW / "saturn-system/de442s.bsp"))
earth, sun, saturn = sat["earth"], sat["sun"], sat[699]
centre_offset = Stack([s for s in sat.segments if s.center == 6 and s.target == 699])


def say(s=""):
    print(s, flush=True)


# --- the Horizons responses ------------------------------------------------

def horizons_files(pattern):
    files = sorted(HORIZONS.glob(pattern)) + sorted(RETAINED.glob(pattern))
    if not pattern.startswith("magnitudes-"):
        files = [p for p in files if not p.name.startswith("magnitudes-")]
    for path in files:
        data = path.read_bytes()
        head = data.split(b"\n", 6)
        recorded = dict(re.match(rb"# ([\w-]+): (.*)", l).groups() for l in head[1:6])
        if hashlib.sha256(head[6]).hexdigest().encode() != recorded[b"body-sha256"]:
            stop(f"{path.name}: its body does not match the digest its header records")
    return files


def table(path):
    """(header names, rows of (datetime, fields)) of one kept response."""
    text = path.read_text(encoding="utf-8")
    soe = text.index("$$SOE")
    header = text[:soe].strip().splitlines()[-2]
    names = [n.strip() for n in header.split(",")]
    body = text[soe + 5:text.index("$$EOE")].strip()
    rows = []
    for line in body.splitlines():
        f = [x.strip() for x in line.split(",")]
        stamp = f[0] if "." in f[0] else f[0] + ".000"
        rows.append((dt.datetime.strptime(stamp, "%Y-%b-%d %H:%M:%S.%f"), f))
    return names, rows


def column(names, prefix):
    for i, n in enumerate(names):
        if n.startswith(prefix):
            return i
    stop(f"no column {prefix!r}")


def era(w):
    if w.year < 1962:
        return ERAS[0]
    if w.year < 1972:
        return ERAS[1]
    return ERAS[2] if w < EXPIRES else ERAS[3]


def times(whens):
    """Skyfield's reading of each civil instant: UT1 before 1972 (its own
    delta T), UTC from 1972, as the atlas reads them."""
    jd = []
    for w in whens:
        civil = (w.year, w.month, w.day, w.hour, w.minute, w.second + w.microsecond / 1e6)
        t = ts.utc(*civil) if w.year >= 1972 else ts.ut1(*civil)
        jd.append(t.tt)
    return ts.tt_jd(np.array(jd))


def observer_for(site):
    if site == "geocentric":
        return earth
    lon, lat = OBSERVERS[site]
    return earth + wgs84.latlon(lat, lon if lon <= 180 else lon - 360, elevation_m=0.0)


def unit(v):
    return v / np.linalg.norm(v, axis=0)


def angle_arcsec(a, b):
    """Angle between two arrays of 3-vectors (3, N), arcseconds."""
    c = np.sum(unit(a) * unit(b), axis=0)
    s = np.linalg.norm(np.cross(unit(a).T, unit(b).T).T, axis=0)
    return np.degrees(np.arctan2(s, c)) * ARCSEC


def radec_sep(ra1, dec1, ra2, dec2):
    a1, d1, a2, d2 = map(np.radians, (ra1, dec1, ra2, dec2))
    v1 = np.array([np.cos(d1) * np.cos(a1), np.cos(d1) * np.sin(a1), np.sin(d1)])
    v2 = np.array([np.cos(d2) * np.cos(a2), np.cos(d2) * np.sin(a2), np.sin(d2)])
    return angle_arcsec(v1, v2)


def wrap(deg):
    return (deg + 540.0) % 360.0 - 180.0


def pole_icrf(tdb_jd):
    """Saturn's north pole (ICRF unit vectors), the PCK's IAU model."""
    T = (tdb_jd - 2451545.0) / 36525.0
    ra = np.radians(POLE_RA[0] + POLE_RA[1] * T + POLE_RA[2] * T * T)
    dec = np.radians(POLE_DEC[0] + POLE_DEC[1] * T + POLE_DEC[2] * T * T)
    return np.array([np.cos(dec) * np.cos(ra), np.cos(dec) * np.sin(ra), np.sin(dec)]), ra, dec


def detic(lat_c):
    """Planetodetic latitude of a planetocentric one, on the PCK spheroid."""
    return np.arctan(np.tan(lat_c) * (R_EQ / R_POLAR) ** 2)


def sky_basis(direction):
    """East and north unit vectors on the sky at each direction (3, N)."""
    d = unit(direction)
    z = np.array([0.0, 0.0, 1.0])[:, None] * np.ones(d.shape[1])
    east = unit(np.cross(z.T, d.T).T)
    north = np.cross(d.T, east.T).T
    return east, north


def of_date(t, v):
    """An ICRF vector array in the true equator and equinox of date."""
    return np.einsum("ijn,jn->in", t.M, v)


class Worst:
    def __init__(self):
        self.v = {}

    def note(self, key, values, tags):
        values = np.asarray(values, dtype=float)
        if values.size == 0:
            return
        i = int(np.nanargmax(np.abs(values)))
        if key not in self.v or abs(values[i]) > abs(self.v[key][0]):
            self.v[key] = (float(values[i]), tags[i])

    def get(self, key):
        return self.v.get(key, (0.0, "-"))


def by_era(whens):
    groups = {}
    for i, w in enumerate(whens):
        groups.setdefault(era(w), []).append(i)
    return {e: np.array(ix) for e, ix in groups.items()}


# --- Saturn ------------------------------------------------------------------

def saturn_geometry(t, observer):
    at = observer.at(t)
    astro = at.observe(saturn)
    app = astro.apparent(deflectors=())
    S = astro.position.km
    lt = astro.light_time
    te = ts.tdb_jd(t.tdb - lt)
    pole, pra, pdec = pole_icrf(te.tdb)
    los = unit(S)
    saturn_at = saturn.at(te).position.km
    sun_from_saturn = sun.at(te).position.km - saturn_at
    # the Sun as Saturn sees it: light time from the Sun to Saturn, iterated
    lt_sun = np.linalg.norm(sun_from_saturn, axis=0) / AU_KM / C_AUDAY
    for _ in range(3):
        sun_seen = sun.at(ts.tdb_jd(te.tdb - lt_sun)).position.km - saturn_at
        lt_sun = np.linalg.norm(sun_seen, axis=0) / AU_KM / C_AUDAY
    # and aberrated by Saturn's own barycentric motion (first order)
    v = saturn.at(te).velocity.km_per_s
    c_km_s = C_AUDAY * AU_KM / 86400.0
    u = unit(sun_seen)
    beta = v / c_km_s
    sun_aberrated = u + beta - np.sum(u * beta, axis=0) * u
    return at, astro, app, te, pole, pra, pdec, los, sun_from_saturn, sun_seen, sun_aberrated


def compare_saturn(worst, counts, rings_out):
    for path in horizons_files("*-saturn-*.txt"):
        site = path.stem.split("-saturn-")[1]
        names, rows = table(path)
        whens = [w for w, _ in rows]
        col = lambda p: np.array([float(f[column(names, p)]) for _, f in rows])
        tags = [f"{path.stem} {w:%Y-%m-%d %H:%M}" for w in whens]
        t = times(whens)
        observer = observer_for(site)
        at, astro, app, te, pole, pra, pdec, los, s_sun, s_seen, s_ab = saturn_geometry(t, observer)
        ra, dec, dist = astro.radec()
        ra_a, dec_a, _ = app.radec(epoch="date")
        S = astro.position.km
        d_km = dist.km
        # the geometry the report's quantities read
        b_obs = np.arcsin(np.clip(np.sum(pole * -los, axis=0), -1, 1))
        app_los = unit(app.position.km)
        b_obs_app = np.arcsin(np.clip(np.sum(pole * -app_los, axis=0), -1, 1))
        b_sun = np.arcsin(np.clip(np.sum(pole * unit(s_sun), axis=0), -1, 1))
        b_seen = np.arcsin(np.clip(np.sum(pole * unit(s_seen), axis=0), -1, 1))
        cos_seen = np.sum(-S * s_seen, axis=0) / (np.linalg.norm(S, axis=0) * np.linalg.norm(s_seen, axis=0))
        phase_seen = np.degrees(np.arccos(np.clip(cos_seen, -1, 1)))
        b_ab = np.arcsin(np.clip(np.sum(pole * unit(s_ab), axis=0), -1, 1))
        cos_ab = np.sum(-S * unit(s_ab), axis=0) / np.linalg.norm(S, axis=0)
        phase_ab = np.degrees(np.arccos(np.clip(cos_ab, -1, 1)))
        east, north = sky_basis(of_date(t, app.position.km))
        p_date = of_date(t, pole)
        pa = np.degrees(np.arctan2(np.sum(p_date * east, axis=0), np.sum(p_date * north, axis=0))) % 360
        cos_i = np.sum(-S * s_sun, axis=0) / (np.linalg.norm(S, axis=0) * np.linalg.norm(s_sun, axis=0))
        phase = np.degrees(np.arccos(np.clip(cos_i, -1, 1)))
        quantities = {
            "astrometric″": radec_sep(ra._degrees, dec.degrees, col("R.A.___"), col("DEC____")),
            "apparent″": radec_sep(ra_a._degrees, dec_a.degrees, col("R.A.__(a-app)"), col("DEC___(a-app)")),
            "distance km": d_km - col("delta") * AU_KM,
            "diameter″": 2 * np.degrees(np.arcsin(R_EQ / d_km)) * ARCSEC - col("Ang-diam"),
            "illuminated %": (1 + cos_i) / 2 * 100 - col("Illu%"),
            "phase angle″": (phase - col("S-T-O")) * ARCSEC,
            "phase angle vs phi″": (phase - col("phi")) * ARCSEC,
            "phase, Sun light time″": (phase_seen - col("S-T-O")) * ARCSEC,
            "phase, Sun light time, vs phi″": (phase_seen - col("phi")) * ARCSEC,
            "illuminated, Sun light time %": (1 + cos_seen) / 2 * 100 - col("Illu%"),
            "phase, Sun aberrated, vs phi″": (phase_ab - col("phi")) * ARCSEC,
            "phase, Sun aberrated″": (phase_ab - col("S-T-O")) * ARCSEC,
            "B centric″": (np.degrees(b_obs) - col("ObsSub-LAT")) * ARCSEC,
            "B detic″": (np.degrees(detic(b_obs)) - col("ObsSub-LAT")) * ARCSEC,
            "B detic, apparent line″": (np.degrees(detic(b_obs_app)) - col("ObsSub-LAT")) * ARCSEC,
            "B′ centric″": (np.degrees(b_sun) - col("SunSub-LAT")) * ARCSEC,
            "B′ detic″": (np.degrees(detic(b_sun)) - col("SunSub-LAT")) * ARCSEC,
            "B′ detic, Sun light time″": (np.degrees(detic(b_seen)) - col("SunSub-LAT")) * ARCSEC,
            "B′ detic, Sun aberrated″": (np.degrees(detic(b_ab)) - col("SunSub-LAT")) * ARCSEC,
            "pole angle″": wrap(pa - col("NP.ang")) * ARCSEC,
            "pole RA″": wrap(np.degrees(pra) - col("N.Pole-RA")) * ARCSEC,
            "pole Dec″": (np.degrees(pdec) - col("N.Pole-DC")) * ARCSEC,
        }
        if site != "geocentric":
            alt, az, _ = app.altaz()
            quantities["horizontal″"] = radec_sep(az.degrees, alt.degrees, col("Azimuth"), col("Elevation"))
        for e, ix in by_era(whens).items():
            counts[("saturn", e)] = counts.get(("saturn", e), 0) + len(ix)
            for q, v in quantities.items():
                worst.note(("saturn", e, q), v[ix], [tags[i] for i in ix])
        rings_out.append((path.stem, whens, col("ObsSub-LAT"), np.degrees(detic(b_obs)),
                          col("SunSub-LAT"), np.degrees(detic(b_ab)), np.degrees(b_obs),
                          col("Ang-diam"), d_km, col("deldot"), col("S-T-O")))


# --- the moons ---------------------------------------------------------------

def code_of(disc, shadow):
    if disc == "transit":
        return "t"
    if disc == "occulted":
        return {"lit": "O", "partial": "P", "total": "U"}[shadow]
    return {"lit": "*", "partial": "p", "total": "u"}[shadow]


def states(t, at, moon_id, sat_astro, moon_astro, oblate):
    """Disc and shadow state per row: limb-to-limb on the equatorial sphere
    (Horizons' definition), or on the apparent oblate figure; the shadow
    at the moon's emission epoch, Saturn's equatorial-sphere umbra."""
    J = sat_astro.position.km
    M = moon_astro.position.km
    jd = np.linalg.norm(J, axis=0)
    md = np.linalg.norm(M, axis=0)
    r_m = R_MOON[moon_id][0]
    angle = np.radians(angle_arcsec(J, M) / ARCSEC)
    depth = md - jd
    if not oblate:
        overlaps = angle < np.arcsin(R_EQ / jd) + np.arcsin(r_m / md)
    else:
        te = ts.tdb_jd(t.tdb - sat_astro.light_time)
        pole, _, _ = pole_icrf(te.tdb)
        los = unit(J)
        up = unit(pole - np.sum(pole * los, axis=0) * los)
        east = np.cross(up.T, los.T).T
        d = M * (jd / md) - J          # the moon's offset on the sky, at Saturn's distance
        px, py = np.sum(d * east, axis=0), np.sum(d * up, axis=0)
        sub = np.arcsin(np.clip(np.sum(pole * -los, axis=0), -1, 1))
        b = np.sqrt(R_POLAR ** 2 * np.cos(sub) ** 2 + R_EQ ** 2 * np.sin(sub) ** 2)
        rho = np.hypot(px, py)
        with np.errstate(invalid="ignore", divide="ignore"):
            edge = 1.0 / np.sqrt((px / rho / R_EQ) ** 2 + (py / rho / b) ** 2)
        overlaps = (rho == 0) | (rho < edge + r_m * (jd / md))
    te = ts.tdb_jd(t.tdb - moon_astro.light_time)
    S = sun.at(te).position.km
    P = saturn.at(te).position.km
    Q = sat[moon_id].at(te).position.km
    axis = P - S
    d_sp = np.linalg.norm(axis, axis=0)
    u = axis / d_sp
    r = Q - P
    along = np.sum(r * u, axis=0)
    perp = np.linalg.norm(r - along * u, axis=0)
    umbra = R_EQ - along * (R_SUN - R_EQ) / d_sp
    total = (along > 0) & (umbra > 0) & (perp + r_m <= umbra)
    partial = (along > 0) & (umbra > 0) & ~total & (perp - r_m < umbra)
    disc = np.where(~overlaps, "clear", np.where(depth < 0, "transit", "occulted"))
    shadow = np.where(total, "total", np.where(partial, "partial", "lit"))
    return [code_of(a, b) for a, b in zip(disc, shadow)], np.degrees(angle) * ARCSEC


def ring_crossing(observer_pos, moon_pos, pole, saturn_pos, inner, outer):
    """Where the sight line from the observer through the moon meets Saturn's
    ring plane: the fraction s of the way to the moon (0 < s < 1, the rings
    lie in front of the moon; s > 1, behind it) and whether that point lies
    between the inner and outer radius (km)."""
    n = pole
    o = observer_pos - saturn_pos
    m = moon_pos - saturn_pos
    ho, hm = np.sum(o * n, axis=0), np.sum(m * n, axis=0)
    with np.errstate(invalid="ignore", divide="ignore"):
        s = ho / (ho - hm)
    p = o + s * (m - o)
    radius = np.linalg.norm(p - np.sum(p * n, axis=0) * n, axis=0)
    return s, (radius >= inner) & (radius <= outer)


def compare_moons(worst, counts, agreement, cases, relevance):
    inner, outer = km("C Ring", "Inner"), km("A Ring", "Outer")
    for label, moon_id in MOONS.items():
        moon = sat[moon_id]
        for path in horizons_files(f"*-{label}-*.txt"):
            site = path.stem.split(f"-{label}-")[1]
            names, rows = table(path)
            whens = [w for w, _ in rows]
            col = lambda p: np.array([float(f[column(names, p)]) for _, f in rows])
            tags = [f"{path.stem} {w:%Y-%m-%d %H:%M}" for w in whens]
            t = times(whens)
            observer = observer_for(site)
            at = observer.at(t)
            astro = at.observe(moon)
            app = astro.apparent(deflectors=())
            s_astro = at.observe(saturn)
            s_app = s_astro.apparent(deflectors=())
            ra, dec, dist = astro.radec()
            ra_a, dec_a, _ = app.radec(epoch="date")
            sra, sdec, _ = s_astro.radec()
            sra_a, sdec_a, _ = s_app.radec(epoch="date")
            x_app = wrap(ra_a._degrees - sra_a._degrees) * np.cos(np.radians(sdec_a.degrees)) * ARCSEC
            y_app = (dec_a.degrees - sdec_a.degrees) * ARCSEC
            x_ast = wrap(ra._degrees - sra._degrees) * np.cos(np.radians(sdec.degrees)) * ARCSEC
            y_ast = (dec.degrees - sdec.degrees) * ARCSEC
            pa = np.degrees(np.arctan2(x_app, y_app)) % 360
            ours, sep = states(t, at, moon_id, s_astro, astro, oblate=False)
            figure, _ = states(t, at, moon_id, s_astro, astro, oblate=True)
            radii = R_MOON[moon_id]
            mean_r = sum(radii) / 3
            quantities = {
                "astrometric″": radec_sep(ra._degrees, dec.degrees, col("R.A.___"), col("DEC____")),
                "apparent″": radec_sep(ra_a._degrees, dec_a.degrees, col("R.A.__(a-app)"), col("DEC___(a-app)")),
                "distance km": dist.km - col("delta") * AU_KM,
                "X apparent″": x_app - col("X_(sat-prim)"),
                "Y apparent″": y_app - col("Y_(sat-prim)"),
                "X astrometric″": x_ast - col("X_(sat-prim)"),
                "Y astrometric″": y_ast - col("Y_(sat-prim)"),
                "position angle°": wrap(pa - col("SatPANG")),
                "separation″": sep - col("ang-sep"),
                "diameter, largest radius″": 2 * np.degrees(np.arcsin(radii[0] / dist.km)) * ARCSEC - col("Ang-diam"),
                "diameter, mean radius″": 2 * np.degrees(np.arcsin(mean_r / dist.km)) * ARCSEC - col("Ang-diam"),
            }
            if site != "geocentric":
                alt, az, _ = app.altaz()
                quantities["horizontal″"] = radec_sep(az.degrees, alt.degrees, col("Azimuth"), col("Elevation"))
            for e, ix in by_era(whens).items():
                counts[(label, e)] = counts.get((label, e), 0) + len(ix)
                for q, v in quantities.items():
                    worst.note((label, e, q), v[ix], [tags[i] for i in ix])
            theirs = [f[column(names, "vis.")].strip("/").strip() or "*" for _, f in rows]
            for i, (o, h, g) in enumerate(zip(ours, theirs, figure)):
                key = (label, "agree" if o == h else "disagree")
                agreement[key] = agreement.get(key, 0) + 1
                if o != h:
                    cases.setdefault(label, []).append(
                        f"{tags[i]}: ours {o}, Horizons {h} (separation {sep[i]:.2f}″, Horizons {col('ang-sep')[i]:.2f}″)")
                if g != o:
                    agreement[(label, "figure differs")] = agreement.get((label, "figure differs"), 0) + 1
                for code in (h,):
                    agreement[(label, "code " + code)] = agreement.get((label, "code " + code), 0) + 1
            # what the rings do to the moon, as seen: behind them, or before them
            te = ts.tdb_jd(t.tdb - s_astro.light_time)
            pole, _, _ = pole_icrf(te.tdb)
            o_pos = at.position.km
            sat_pos = o_pos + s_astro.position.km
            moon_pos = o_pos + astro.position.km
            frac, on_rings = ring_crossing(o_pos, moon_pos, pole, sat_pos, inner, outer)
            behind = on_rings & (frac > 0) & (frac < 1)
            in_front = on_rings & (frac > 1)
            if path.stem.startswith("matrix-7d") and site == "oslo":
                relevance[label] = {
                    "rows": len(rows),
                    "separation max″": float(np.max(col("ang-sep"))),
                    "separation min″": float(np.min(col("ang-sep"))),
                    "diameter max″": float(np.max(col("Ang-diam"))),
                    "diameter min″": float(np.min(col("Ang-diam"))),
                    "behind the rings": int(np.sum(behind)),
                    "before the rings": int(np.sum(in_front)),
                    "codes": {c: theirs.count(c) for c in sorted(set(theirs))},
                }


# --- the planetary ephemeris, separated from sat441 --------------------------

def ephemeris_separation():
    """DE440 (de440s) against DE442s over the study interval, weekly; and
    what sat441's own Saturn-centre offset (6 -> 699) contributes."""
    start = ts.utc(1900, 1, 1).tdb
    end = ts.utc(2100, 12, 31).tdb
    t = ts.tdb_jd(np.arange(start, end, 7.0))
    out = {}
    b440 = de440[6].at(t).position.km
    b442 = de442[6].at(t).position.km
    e440 = de440["earth"].at(t).position.km
    e442 = de442["earth"].at(t).position.km
    out["barycentre km"] = np.linalg.norm(b442 - b440, axis=0)
    out["earth km"] = np.linalg.norm(e442 - e440, axis=0)
    g440 = de440["earth"].at(t).observe(de440[6] + centre_offset)
    g442 = de442["earth"].at(t).observe(de442[6] + centre_offset)
    out["direction″"] = angle_arcsec(g440.position.km, g442.position.km)
    out["distance km"] = g442.distance().km - g440.distance().km
    # sat441's own barycentre and Earth against de440s's
    out["sat441 barycentre vs de440s km"] = np.linalg.norm(sat[6].at(t).position.km - b440, axis=0)
    out["sat441 earth vs de440s km"] = np.linalg.norm(earth.at(t).position.km - e440, axis=0)
    # the centre offset: its size and what leaving it out would do
    offset = centre_offset.at(t).position.km
    out["centre offset km"] = np.linalg.norm(offset, axis=0)
    centre = earth.at(t).observe(saturn)
    bary = earth.at(t).observe(sat[6])
    out["centre vs barycentre direction″"] = angle_arcsec(centre.position.km, bary.position.km)
    years = np.array([2000.0 + (x - 2451545.0) / 365.25 for x in t.tdb])
    return years, out


# --- the report --------------------------------------------------------------

def crossings(whens, values):
    """The instants a series changes sign, linearly interpolated."""
    res = []
    for i in range(1, len(values)):
        a, b = values[i - 1], values[i]
        if a == 0 or a * b < 0:
            f = a / (a - b)
            res.append(whens[i - 1] + (whens[i] - whens[i - 1]) * f)
    return res


def main():
    say("# #574 Saturn authority comparison")
    say()
    say("Skyfield on `sat441.bsp` (Saturn, 699, and Mimas-Iapetus, 601-608, about the Saturn"
        " system barycentre; its own barycentre and Earth are DE440's) against the kept JPL Horizons"
        " responses (Horizons: `sat441l` for every Saturn-system body, DE441 for the Earth)."
        " Observers at sea level on the WGS84 ellipsoid. Civil time: UT1 before 1972, UTC from"
        f" 1972; the record's expiry, {EXPIRES:%Y-%m-%d}, read from `Leap_Second.dat`, bounds the"
        " exact era. No gravitational deflection, as the atlas computes.")
    say()
    say(f"Constants read from `pck00011.tpc`: Saturn's radii {R_EQ:g}, {R_EQ:g}, {R_POLAR:g} km;"
        f" pole RA {POLE_RA[0]} {POLE_RA[1]:+} T, Dec {POLE_DEC[0]} {POLE_DEC[1]:+} T (T in Julian"
        f" centuries TDB from J2000, at the light-time-corrected epoch); the Sun's radius {R_SUN:g} km;"
        " each moon's three radii. Ring radii from the retained PDS table.")
    worst, counts, rings_out = Worst(), {}, []
    compare_saturn(worst, counts, rings_out)
    say()
    say("## Saturn: worst absolute difference by era")
    say()
    qs = ["astrometric″", "apparent″", "horizontal″", "distance km", "diameter″", "illuminated %",
          "phase angle″", "phase angle vs phi″", "phase, Sun light time″",
          "phase, Sun light time, vs phi″", "illuminated, Sun light time %", "phase, Sun aberrated″", "phase, Sun aberrated, vs phi″", "pole RA″", "pole Dec″", "pole angle″"]
    say("| era | rows | " + " | ".join(qs) + " |")
    say("|---|---:|" + "---:|" * len(qs))
    for e in ERAS:
        say(f"| {e} | {counts.get(('saturn', e), 0)} | " + " | ".join(
            f"{worst.get(('saturn', e, q))[0]:+.4f}" for q in qs) + " |")
    say()
    say("## Saturn: the ring opening, three readings against Horizons' sub-observer and sub-solar latitudes")
    say()
    qs = ["B centric″", "B detic″", "B detic, apparent line″", "B′ centric″", "B′ detic″",
          "B′ detic, Sun light time″", "B′ detic, Sun aberrated″"]
    say("| era | " + " | ".join(qs) + " |")
    say("|---|" + "---:|" * len(qs))
    for e in ERAS:
        say(f"| {e} | " + " | ".join(f"{worst.get(('saturn', e, q))[0]:+.3f}" for q in qs) + " |")
    say()
    say("*centric* is the planetocentric latitude of the observer (or the Sun) over the PCK pole:"
        " the ring plane's true opening angle. *detic* is the same direction's planetodetic latitude"
        " on the PCK spheroid. *apparent line* uses the aberrated line of sight. *Sun light time*"
        " places the Sun where Saturn sees it, its light time to Saturn iterated; *Sun aberrated* adds the aberration of Saturn's own barycentric motion. The Sun's"
        " ring-plane crossings below use the aberrated reading.")

    # the ring geometry the matrices show
    say()
    say("## The rings: openings, crossings and apparent ellipses")
    for stem, whens, h_b, o_b, h_bs, o_bs, b_c, diam, d_km, deldot, sto in rings_out:
        if not (stem.startswith("matrix-7d") or stem.startswith("ring-plane") or stem.startswith("dense")):
            continue
        say()
        say(f"**{stem}** ({len(whens)} rows, {whens[0]:%Y-%m-%d} to {whens[-1]:%Y-%m-%d}):")
        i = int(np.argmax(np.abs(b_c)))
        say(f"- widest opening to the observer: B = {b_c[i]:+.3f}° planetocentric at {whens[i]:%Y-%m-%d}"
            f" (Horizons' ObsSub-LAT {h_b[i]:+.3f}°)")
        for name, ours, theirs in (("Earth", o_b, h_b), ("Sun", o_bs, h_bs)):
            ho = crossings(whens, theirs)
            oo = crossings(whens, ours)
            say(f"- ring-plane crossings of the {name}: Horizons {len(ho)}, ours {len(oo)}")
            for a, b in zip(ho, oo):
                say(f"  - Horizons {a:%Y-%m-%d %H:%M}, ours {b:%Y-%m-%d %H:%M}"
                    f" ({(b - a).total_seconds() / 3600:+.2f} h)")
        if stem.startswith("dense") or stem.startswith("matrix"):
            # nearest approach (range rate through zero, falling to rising)
            near = [whens[k] for k in range(1, len(deldot)) if deldot[k - 1] < 0 <= deldot[k]]
            say(f"- nearest approaches (Horizons' range rate through zero): {len(near)}"
                + (f", first {near[0]:%Y-%m-%d}, last {near[-1]:%Y-%m-%d}" if near else ""))
        a_outer, c_inner = km("A Ring", "Outer"), km("C Ring", "Inner")
        k_max, k_min = int(np.argmin(d_km)), int(np.argmax(d_km))
        for label, k in (("nearest", k_max), ("farthest", k_min)):
            major = 2 * math.degrees(math.asin(a_outer / d_km[k])) * ARCSEC
            say(f"- {label} row {whens[k]:%Y-%m-%d}: A Ring outer edge {major:.2f}″ by"
                f" {major * abs(math.sin(math.radians(b_c[k]))):.2f}″; C Ring inner edge"
                f" {2 * math.degrees(math.asin(c_inner / d_km[k])) * ARCSEC:.2f}″ across;"
                f" the globe {diam[k]:.2f}″ (Horizons)")

    # the moons
    agreement, cases, relevance = {}, {}, {}
    compare_moons(worst, counts, agreement, cases, relevance)
    qs = ["astrometric″", "apparent″", "horizontal″", "distance km", "X apparent″", "Y apparent″",
          "X astrometric″", "Y astrometric″", "position angle°", "separation″",
          "diameter, largest radius″", "diameter, mean radius″"]
    for label in MOONS:
        say()
        say(f"## {label.capitalize()}: worst absolute difference by era")
        say()
        say("| era | rows | " + " | ".join(qs) + " |")
        say("|---|---:|" + "---:|" * len(qs))
        for e in ERAS:
            say(f"| {e} | {counts.get((label, e), 0)} | " + " | ".join(
                f"{worst.get((label, e, q))[0]:+.4f}" for q in qs) + " |")
    say()
    say("## Visibility states against Horizons' codes")
    say()
    say("Ours on Horizons' definition: limb to limb on Saturn's equatorial sphere, each moon's"
        " largest PCK radius, light time considered; the shadow at the moon's emission epoch,"
        " Saturn's equatorial-sphere umbra. *Figure differs* counts rows where the apparent"
        " oblate figure (the PCK pole and polar radius) gives a different state. Horizons' codes"
        " take no account of the rings.")
    say()
    say("| moon | agreeing | disagreeing | figure differs | Horizons' codes |")
    say("|---|---:|---:|---:|---|")
    for label in MOONS:
        codes = ", ".join(f"{k[1][5:]} {v}" for k, v in sorted(agreement.items())
                          if k[0] == label and k[1].startswith("code "))
        say(f"| {label} | {agreement.get((label, 'agree'), 0)} | {agreement.get((label, 'disagree'), 0)} |"
            f" {agreement.get((label, 'figure differs'), 0)} | {codes} |")
    for label in MOONS:
        if cases.get(label):
            say()
            say(f"Disagreements, {label} ({len(cases[label])}):")
            for c in cases[label][:40]:
                say(f"- {c}")
            if len(cases[label]) > 40:
                say(f"- ... and {len(cases[label]) - 40} more")
    say()
    say("## Observational relevance, from the weekly Oslo matrix 1900-2100")
    say()
    say(f"*Behind the rings* counts rows where the sight line to a moon meets the ring plane"
        f" between the observer and the moon, between {km('C Ring', 'Inner'):,.0f} and"
        f" {km('A Ring', 'Outer'):,.0f} km (the C Ring's inner and the A Ring's outer boundary);"
        " *before the rings*, rows where the line meets them beyond the moon, so that the moon is"
        " seen against them. The rings are taken as a plane with no thickness and no gaps. This is"
        " geometry only: no Horizons code or published authority checks it.")
    say()
    say("| moon | rows | separation from Saturn″ (min-max) | diameter″ (min-max) | behind the rings | before the rings | Horizons' codes |")
    say("|---|---:|---|---|---:|---:|---|")
    for label, r in relevance.items():
        say(f"| {label} | {r['rows']} | {r['separation min″']:.1f}-{r['separation max″']:.1f} |"
            f" {r['diameter min″']:.4f}-{r['diameter max″']:.4f} | {r['behind the rings']} |"
            f" {r['before the rings']} | " + ", ".join(f"{k} {v}" for k, v in r["codes"].items()) + " |")

    # brightness, from the magnitude run
    say()
    say("## Brightness, from Horizons' magnitudes (geocentric, every 30 days, 1900-2100)")
    say()
    say("Horizons' `APmag`, read as given: airless, no extinction; Saturn's includes the rings only"
        " under the conditions its notes state.")
    say()
    say("| body | rows | brightest | median | faintest | n.a. |")
    say("|---|---:|---:|---:|---:|---:|")
    for label in ["saturn"] + list(MOONS):
        path = horizons_files(f"magnitudes-30d-{label}-geocentric.txt")[0]
        names, rows = table(path)
        i = column(names, "APmag")
        raw = [f[i] for _, f in rows]
        values = np.array([float(v) for v in raw if v != "n.a."])
        say(f"| {label} | {len(rows)} | {values.min():.2f} | {np.median(values):.2f} |"
            f" {values.max():.2f} | {len(raw) - values.size} |")

    # the planetary ephemeris, separated
    years, sep = ephemeris_separation()
    say()
    say("## The planetary ephemeris, separated from sat441")
    say()
    say("Weekly over 1900-2100 (TDB): DE442s (`de442s.bsp`) against the released DE440"
        " (`de440s.bsp`), each carrying Saturn's centre by sat441's own 6 -> 699; then sat441's"
        " barycentre and Earth against de440s's; then the size of the centre offset itself and"
        " the direction it moves Saturn by, seen from the Earth.")
    say()
    say("| quantity | 1900-1949 | 1950-1999 | 2000-2049 | 2050-2100 |")
    say("|---|---:|---:|---:|---:|")
    bands = [(1900, 1950), (1950, 2000), (2000, 2050), (2050, 2101)]
    for q, v in sep.items():
        cells = []
        for a, b in bands:
            m = (years >= a) & (years < b)
            cells.append(f"{np.max(np.abs(v[m])):.4g}")
        say(f"| {q} | " + " | ".join(cells) + " |")

    # the hourly Titan series: its greatest elongations
    titan = HORIZONS / "titan-orbit-2026-08-hourly-oslo.txt"
    names, rows = table(titan)
    whens = [w for w, _ in rows]
    hsep = np.array([float(f[column(names, "ang-sep")]) for _, f in rows])
    t = times(whens)
    at = observer_for("oslo").at(t)
    _, osep = states(t, at, 606, at.observe(saturn), at.observe(sat[606]), oblate=False)
    say()
    say("## Titan, hourly at Oslo, 2026-08-01 to 2026-08-17: greatest elongations")
    say()
    for k in range(1, len(hsep) - 1):
        if hsep[k] >= hsep[k - 1] and hsep[k] > hsep[k + 1]:
            j = k
            say(f"- Horizons {whens[k]:%Y-%m-%d %H:%M} {hsep[k]:.2f}″; ours {osep[j]:.2f}″"
                f" ({osep[j] - hsep[k]:+.3f}″)")
    say()
    say("## Where the worst rows are")
    say()
    for key, (v, tag) in sorted(worst.v.items()):
        say(f"- {key[0]}, {key[1]}, {key[2]}: {v:+.5f} at {tag}")


if __name__ == "__main__":
    main()
