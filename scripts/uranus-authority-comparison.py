#!/usr/bin/env python3
"""#633 authority comparison for Uranus and its five major moons: Skyfield
on the pinned kernels against the kept JPL Horizons responses, offline -
the Mars study's comparison (#601) for Uranus.
Three Uranus models are compared with Horizons (which answers Uranus and
its moons from its own satellite solution and the Earth from DE441):
  barycentre      - DE440's Uranus barycentre alone (0 -> 7), 1900-2100: the
                    planet without any satellite kernel;
  centre          - DE440's barycentre plus ura184_part-3's 7 -> 799;
  centre on DE442 - ura184_part-3's own barycentre (DE442) plus its 7 -> 799,
                    the kernel as NAIF assembled it.
The moons are DE440's barycentre plus ura184_part-3's 7 -> 70x. The pole is
the PCK's IAU 2015 model. DE442s against DE440 measures what the newest
compact planetary ephemeris would move.
Every constant is read from a pinned file, never typed: radii and pole from
pck00011.tpc, the exactness boundary of the civil time scale from the
application's own Leap_Second.dat. Every input is verified against its
SHA-256 before it is read, and every Horizons response against the body
digest its header records.
Nothing here is a build, test or runtime dependency. Run by hand from the
repository root with the study's virtual environment:
    ~/juranometria-runs/venv/bin/python scripts/uranus-authority-comparison.py \\
        > docs/studies/uranus-system/authority-comparison.md
URANUS_RETAINED names the directory of the retained geocentric matrices
(default ~/juranometria-runs/uranus-horizons).
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
STUDY = ROOT / "docs/studies/uranus-system"
HORIZONS = STUDY / "horizons"
RETAINED = Path(os.environ.get("URANUS_RETAINED",
                               str(Path.home() / "juranometria-runs/uranus-horizons")))
DE442S = Path.home() / "juranometria-runs/venus-raw/de442s.bsp"

PINS = {
    RAW / "uranus-system/ura184_part-3.bsp": "273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53",
    RAW / "solar-system/de440s.bsp": "c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2",
    RAW / "jovian-system/pck00011.tpc": "3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1",
    DE442S: "54d97562a5b094d298b1b8eafa5a2e17e3e010ce85e1a366d07f003ad159323c",
}
LEAP = ROOT / "src/resources/solar-system/Leap_Second.dat"
ARCSEC = 3600.0
MOONS = {"miranda": 705, "ariel": 701, "umbriel": 702, "titania": 703, "oberon": 704}
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
    sys.exit(f"uranus-authority-comparison: {message} - stopping")


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
R_EQ, _, R_POLAR = PCK["BODY799_RADII"]
POLE_RA, POLE_DEC = PCK["BODY799_POLE_RA"], PCK["BODY799_POLE_DEC"]
# Uranus's own pole carries no periodic terms in pck00011 (the BODY7 angles
# are its satellites'); read them if present, as zeros if not.
ANGLES = np.array(PCK["BODY7_NUT_PREC_ANGLES"]).reshape(-1, 2)
NUT_RA = PCK.get("BODY799_NUT_PREC_RA", [0.0] * len(ANGLES))
NUT_DEC = PCK.get("BODY799_NUT_PREC_DEC", [0.0] * len(ANGLES))
R_MOON = {i: PCK[f"BODY{i}_RADII"] for i in MOONS.values()}

EXPIRES = dt.datetime.strptime(
    re.search(r"File expires on\s+(\d{1,2}\s+[A-Za-z]+\s+\d{4})", LEAP.read_text()).group(1),
    "%d %B %Y")
ERAS = ["1900-1961", "1962-1971", f"1972-{EXPIRES:%Y-%m}", "after"]

load = Loader(str(ROOT / "build" / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
ura = load(str(RAW / "uranus-system/ura184_part-3.bsp"))
de440 = load(str(RAW / "solar-system/de440s.bsp"))
de442 = load(str(DE442S))
earth, sun = de440["earth"], de440["sun"]
BARY = de440[7]
centre_offset = Stack([s for s in ura.segments if s.center == 7 and s.target == 799])
CENTRE = BARY + centre_offset
BARY_442 = Stack([s for s in ura.segments if s.center == 0 and s.target == 7])
CENTRE_442 = BARY_442 + Stack([s for s in ura.segments if s.center == 7 and s.target == 799])
MOON_BODY = {i: BARY + Stack([s for s in ura.segments if s.center == 7 and s.target == i])
             for i in MOONS.values()}
_segs = [s for s in ura.segments if s.target == 799]
SPAN = (min(s.spk_segment.start_jd for s in _segs), max(s.spk_segment.end_jd for s in _segs))


def pole_icrf(tdb_jd, periodic=True):
    """Uranus's north pole (ICRF unit vectors), the PCK's IAU 2015 model."""
    T = (tdb_jd - 2451545.0) / 36525.0
    ra = POLE_RA[0] + POLE_RA[1] * T + POLE_RA[2] * T * T
    dec = POLE_DEC[0] + POLE_DEC[1] * T + POLE_DEC[2] * T * T
    if periodic:
        for k, c in enumerate(NUT_RA):
            if c:
                th = np.radians(ANGLES[k, 0] + ANGLES[k, 1] * T)
                ra = ra + c * np.sin(th)
        for k, c in enumerate(NUT_DEC):
            if c:
                th = np.radians(ANGLES[k, 0] + ANGLES[k, 1] * T)
                dec = dec + c * np.cos(th)
    ra, dec = np.radians(ra), np.radians(dec)
    return np.array([np.cos(dec) * np.cos(ra), np.cos(dec) * np.sin(ra), np.sin(dec)]), ra, dec


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


def _unused_pole_icrf(tdb_jd):
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



def inside_span(t):
    return (t.tdb > SPAN[0] + 1.0) & (t.tdb < SPAN[1] - 1.0)


def compare_uranus(worst, counts, extremes):
    for path in horizons_files("*-uranus-*.txt"):
        site = path.stem.split("-uranus-")[1].split("-hourly")[0]
        names, rows = table(path)
        whens = [w for w, _ in rows]
        col = lambda p: np.array([float(f[column(names, p)]) for _, f in rows])
        tags = [f"{path.stem} {w:%Y-%m-%d %H:%M}" for w in whens]
        t = times(whens)
        observer = observer_for(site)
        at = observer.at(t)
        span = inside_span(t)
        for model, body in (("barycentre", BARY), ("centre", CENTRE),
                            ("centre on DE442", CENTRE_442)):
            if model != "barycentre" and not span.any():
                continue
            ix_model = np.arange(len(whens)) if model == "barycentre" else np.nonzero(span)[0]
            if model != "barycentre":
                tt = ts.tt_jd(t.tt[ix_model])
                at_m = observer.at(tt)
            else:
                tt, at_m = t, at
            astro = at_m.observe(body)
            app0 = astro.apparent(deflectors=())
            app1 = astro.apparent(deflectors=(10,))
            ra, dec, dist = astro.radec()
            ra0, dec0, _ = app0.radec(epoch="date")
            ra1, dec1, _ = app1.radec(epoch="date")
            sub = lambda a: a[ix_model]
            S = astro.position.km
            d_km = dist.km
            te = ts.tdb_jd(tt.tdb - astro.light_time)
            sun_from = sun.at(te).position.km - body.at(te).position.km
            cos_i = np.sum(-S * sun_from, axis=0) / (np.linalg.norm(S, axis=0) * np.linalg.norm(sun_from, axis=0))
            phase = np.degrees(np.arccos(np.clip(cos_i, -1, 1)))
            body_at = body.at(te).position.km
            lt_sun = np.linalg.norm(sun_from, axis=0) / AU_KM / C_AUDAY
            for _ in range(3):
                sun_seen = sun.at(ts.tdb_jd(te.tdb - lt_sun)).position.km - body_at
                lt_sun = np.linalg.norm(sun_seen, axis=0) / AU_KM / C_AUDAY
            beta = body.at(te).velocity.km_per_s / (C_AUDAY * AU_KM / 86400.0)
            u_s = unit(sun_seen)
            sun_ab = u_s + beta - np.sum(u_s * beta, axis=0) * u_s
            cos_ab = np.sum(-S * unit(sun_ab), axis=0) / np.linalg.norm(S, axis=0)
            phase_ab = np.degrees(np.arccos(np.clip(cos_ab, -1, 1)))
            q = {
                "phase angle, Sun light time and aberration″": (phase_ab - sub(col("S-T-O"))) * ARCSEC,
                "phase angle, Sun light time and aberration, vs phi″": (phase_ab - sub(col("phi"))) * ARCSEC,
                "phase angle vs phi″": (phase - sub(col("phi"))) * ARCSEC,
                "illuminated, Sun light time and aberration %": (1 + cos_ab) / 2 * 100 - sub(col("Illu%")),
                "astrometric″": radec_sep(ra._degrees, dec.degrees, sub(col("R.A.___")), sub(col("DEC____"))),
                "apparent, no deflection″": radec_sep(ra0._degrees, dec0.degrees, sub(col("R.A.__(a-app)")), sub(col("DEC___(a-app)"))),
                "apparent, Sun deflection″": radec_sep(ra1._degrees, dec1.degrees, sub(col("R.A.__(a-app)")), sub(col("DEC___(a-app)"))),
                "distance km": d_km - sub(col("delta")) * AU_KM,
                "diameter″": 2 * np.degrees(np.arcsin(R_EQ / d_km)) * ARCSEC - sub(col("Ang-diam")),
                "illuminated %": (1 + cos_i) / 2 * 100 - sub(col("Illu%")),
                "phase angle″": (phase - sub(col("S-T-O"))) * ARCSEC,
            }
            if model == "barycentre":
                for periodic in (True, False):
                    pole, pra, pdec = pole_icrf(te.tdb, periodic)
                    east, north = sky_basis(of_date(tt, app1.position.km))
                    p_date = of_date(tt, pole)
                    pa = np.degrees(np.arctan2(np.sum(p_date * east, axis=0), np.sum(p_date * north, axis=0))) % 360
                    tag = "with periodic terms" if periodic else "secular only"
                    q[f"pole RA, {tag}″"] = wrap(np.degrees(pra) - col("N.Pole-RA")) * ARCSEC
                    q[f"pole Dec, {tag}″"] = (np.degrees(pdec) - col("N.Pole-DC")) * ARCSEC
                    q[f"pole angle, {tag}″"] = wrap(pa - col("NP.ang")) * ARCSEC
                    if periodic:
                        b = np.arcsin(np.clip(np.sum(pole * -unit(S), axis=0), -1, 1))
                        q["sub-observer latitude, centric vs Horizons″"] = (np.degrees(b) - col("ObsSub-LAT")) * ARCSEC
                        detic = np.arctan(np.tan(b) * (R_EQ / R_POLAR) ** 2)
                        q["sub-observer latitude, detic vs Horizons″"] = (np.degrees(detic) - col("ObsSub-LAT")) * ARCSEC
            sun_obs = at_m.observe(sun).position.km
            elong = angle_arcsec(S, sun_obs) / ARCSEC
            far = elong > 1.0
            dev1 = q["apparent, Sun deflection″"]
            q["apparent, Sun deflection, beyond 1° from the Sun″"] = np.where(far, dev1, 0.0)
            if site != "geocentric":
                alt, az, _ = app1.altaz()
                q["horizontal″"] = radec_sep(az.degrees, alt.degrees, sub(col("Azimuth")), sub(col("Elevation")))
            sub_whens = [whens[i] for i in ix_model]
            sub_tags = [tags[i] for i in ix_model]
            for e, ix in by_era(sub_whens).items():
                counts[(model, e)] = counts.get((model, e), 0) + len(ix)
                for k, v in q.items():
                    worst.note((model, e, k), v[ix], [sub_tags[i] for i in ix])


def states(t, at, moon_id, planet_astro, moon_astro):
    """Disc and shadow state per row on Horizons' definition: limb to limb
    on Uranus's equatorial sphere and the moon's largest radius, light time
    considered; the shadow at the moon's emission epoch, Uranus's
    equatorial-sphere umbra."""
    J = planet_astro.position.km
    M = moon_astro.position.km
    jd = np.linalg.norm(J, axis=0)
    md = np.linalg.norm(M, axis=0)
    r_m = R_MOON[moon_id][0]
    angle = np.radians(angle_arcsec(J, M) / ARCSEC)
    overlaps = angle < np.arcsin(R_EQ / jd) + np.arcsin(r_m / md)
    te = ts.tdb_jd(t.tdb - moon_astro.light_time)
    S = sun.at(te).position.km
    P = CENTRE.at(te).position.km
    Q = MOON_BODY[moon_id].at(te).position.km
    axis = P - S
    d_sp = np.linalg.norm(axis, axis=0)
    u = axis / d_sp
    r = Q - P
    along = np.sum(r * u, axis=0)
    perp = np.linalg.norm(r - along * u, axis=0)
    umbra = R_EQ - along * (R_SUN - R_EQ) / d_sp
    total = (along > 0) & (umbra > 0) & (perp + r_m <= umbra)
    partial = (along > 0) & (umbra > 0) & ~total & (perp - r_m < umbra)
    disc = np.where(~overlaps, "clear", np.where(md - jd < 0, "transit", "occulted"))
    shadow = np.where(total, "total", np.where(partial, "partial", "lit"))
    return [code_of(a, b) for a, b in zip(disc, shadow)], np.degrees(angle) * ARCSEC


def code_of(disc, shadow):
    if disc == "transit":
        return "t"
    if disc == "occulted":
        return {"lit": "O", "partial": "P", "total": "U"}[shadow]
    return {"lit": "*", "partial": "p", "total": "u"}[shadow]



def compare_moons(worst, counts, agreement, cases):
    for label, moon_id in MOONS.items():
        moon = MOON_BODY[moon_id]
        for path in horizons_files(f"*-{label}-*.txt"):
            site = path.stem.split(f"-{label}-")[1].split("-10m")[0]
            names, rows = table(path)
            keep = [i for i, (w, _) in enumerate(rows)]
            whens = [w for w, _ in rows]
            t = times(whens)
            ix = np.nonzero(inside_span(t))[0]
            if ix.size == 0:
                continue
            rows = [rows[i] for i in ix]
            whens = [whens[i] for i in ix]
            col = lambda p: np.array([float(f[column(names, p)]) for _, f in rows])
            tags = [f"{path.stem} {w:%Y-%m-%d %H:%M}" for w in whens]
            t = ts.tt_jd(t.tt[ix])
            observer = observer_for(site)
            at = observer.at(t)
            astro = at.observe(moon)
            app = astro.apparent(deflectors=(10,))
            m_astro = at.observe(CENTRE)
            m_app = m_astro.apparent(deflectors=(10,))
            ra, dec, dist = astro.radec()
            ra_a, dec_a, _ = app.radec(epoch="date")
            mra_a, mdec_a, _ = m_app.radec(epoch="date")
            x_app = wrap(ra_a._degrees - mra_a._degrees) * np.cos(np.radians(mdec_a.degrees)) * ARCSEC
            y_app = (dec_a.degrees - mdec_a.degrees) * ARCSEC
            pa = np.degrees(np.arctan2(x_app, y_app)) % 360
            ours, sep = states(t, at, moon_id, m_astro, astro)
            radii = R_MOON[moon_id]
            q = {
                "astrometric″": radec_sep(ra._degrees, dec.degrees, col("R.A.___"), col("DEC____")),
                "apparent″": radec_sep(ra_a._degrees, dec_a.degrees, col("R.A.__(a-app)"), col("DEC___(a-app)")),
                "distance km": dist.km - col("delta") * AU_KM,
                "X apparent″": x_app - col("X_(sat-prim)"),
                "Y apparent″": y_app - col("Y_(sat-prim)"),
                "position angle°": wrap(pa - col("SatPANG")),
                "separation″": sep - col("ang-sep"),
                "diameter, largest radius″": 2 * np.degrees(np.arcsin(radii[0] / dist.km)) * ARCSEC - col("Ang-diam"),
            }
            elong = angle_arcsec(m_astro.position.km, at.observe(sun).position.km) / ARCSEC
            q["apparent, beyond 1° from the Sun″"] = np.where(elong > 1.0, q["apparent″"], 0.0)
            for k in ("X apparent″", "Y apparent″", "separation″", "position angle°"):
                q[f"{k[:-1]}, beyond 1° from the Sun{k[-1]}"] = np.where(elong > 1.0, q[k], 0.0)
            near = angle_arcsec(m_astro.position.km, astro.position.km) < 5.0
            q["position angle, within 5″ of Uranus°"] = np.where(near, q["position angle°"], 0.0)
            q["position angle, beyond 5″ of Uranus°"] = np.where(~near, q["position angle°"], 0.0)
            if site != "geocentric":
                alt, az, _ = app.altaz()
                q["horizontal″"] = radec_sep(az.degrees, alt.degrees, col("Azimuth"), col("Elevation"))
            for e, jx in by_era(whens).items():
                counts[(label, e)] = counts.get((label, e), 0) + len(jx)
                for k, v in q.items():
                    worst.note((label, e, k), v[jx], [tags[i] for i in jx])
            theirs = [f[column(names, "vis.")].strip("/").strip() or "*" for _, f in rows]
            for i, (o, h) in enumerate(zip(ours, theirs)):
                key = (label, "agree" if o == h else "disagree")
                agreement[key] = agreement.get(key, 0) + 1
                agreement[(label, "code " + h)] = agreement.get((label, "code " + h), 0) + 1
                if o != h:
                    cases.setdefault(label, []).append(
                        f"{tags[i]}: ours {o}, Horizons {h} (separation {sep[i]:.3f}″, Horizons {col('ang-sep')[i]:.3f}″)")


def ephemeris_separation():
    """DE440 against DE442s and against ura184's own (DE442) barycentre for
    the Uranus barycentre, weekly over 1900-2100, and what leaving
    ura184's centre offset out does to the direction."""
    t = ts.tdb_jd(np.arange(ts.utc(1900, 1, 1).tdb, ts.utc(2100, 12, 31).tdb, 7.0))
    out = {}
    b440, b442 = de440[7].at(t).position.km, de442[7].at(t).position.km
    out["barycentre km"] = np.linalg.norm(b442 - b440, axis=0)
    out["earth km"] = np.linalg.norm(de442["earth"].at(t).position.km - de440["earth"].at(t).position.km, axis=0)
    g440 = de440["earth"].at(t).observe(de440[7])
    g442 = de442["earth"].at(t).observe(de442[7])
    out["geocentric direction″"] = angle_arcsec(g440.position.km, g442.position.km)
    out["geocentric distance km"] = g442.distance().km - g440.distance().km
    out["ura184 barycentre (DE442) vs DE440 km"] = np.linalg.norm(BARY_442.at(t).position.km - b440, axis=0)
    out["ura184 barycentre (DE442) vs de442s km"] = np.linalg.norm(BARY_442.at(t).position.km - b442, axis=0)
    td = ts.tdb_jd(np.arange(ts.utc(1900, 1, 1).tdb, ts.utc(2100, 12, 31).tdb, 0.25))
    c = earth.at(td).observe(CENTRE).position.km
    b = earth.at(td).observe(BARY).position.km
    out["centre vs barycentre, geocentric direction, 1900-2100 every 6 h″"] = angle_arcsec(c, b)
    return out


def crossings(whens, values):
    """The instants a series changes sign, linearly interpolated."""
    res = []
    for i in range(1, len(values)):
        a, b = values[i - 1], values[i]
        if a == 0 or a * b < 0:
            f = a / (a - b)
            res.append(whens[i - 1] + (whens[i] - whens[i - 1]) * f)
    return res



def named_events():
    """Each almanac row's instant against the models': the geocentric
    apparent ecliptic longitude of Uranus less the Sun's, of date, reaching
    180 degrees (opposition) or 0 (conjunction), found to the second by
    bisection inside a ten-day bracket about the almanac's hour."""
    events = STUDY / "uranus-events.txt"
    rows = []
    for line in events.read_text(encoding="utf-8").splitlines():
        if line.startswith("#") or not line.strip():
            continue
        y, mon, d, h, rest = line.split(" ", 4)
        kind = "opposition" if "Opposition" in rest else "conjunction" if "Conjunction" in rest else None
        if kind is None:
            continue
        when = dt.datetime.strptime(f"{y} {mon} {d} {h}", "%Y %b %d %H")
        rows.append((kind, when))
    out = []
    for kind, when in rows:
        target = 180.0 if kind == "opposition" else 0.0
        found = {}
        for model, body in (("barycentre", BARY), ("centre on DE442", CENTRE_442)):
            def f(jd):
                t = ts.tt_jd(jd)
                e = earth.at(t)
                lu = e.observe(body).apparent().ecliptic_latlon(epoch="date")[1].degrees
                ls = e.observe(sun).apparent().ecliptic_latlon(epoch="date")[1].degrees
                return wrap(lu - ls - target)
            c = ts.utc(when.year, when.month, when.day, when.hour).tt
            lo, hi = c - 5.0, c + 5.0
            flo = f(lo)
            for _ in range(60):
                mid = (lo + hi) / 2
                fm = f(mid)
                if (fm < 0) == (flo < 0):
                    lo, flo = mid, fm
                else:
                    hi = mid
            t = ts.tt_jd((lo + hi) / 2)
            found[model] = t.utc_datetime().replace(tzinfo=None)
        out.append((kind, when, found))
    return out


def main():
    say("# #633 Uranus authority comparison")
    say()
    say("Skyfield on the pinned kernels against the kept JPL Horizons responses (Horizons: its"
        " own satellite solution for Uranus and the moons, DE441 for the Earth). The Earth and the Sun are"
        " DE440's (`de440s.bsp`). Observers at sea level on the WGS84 ellipsoid. Civil time: UT1"
        f" before 1972, UTC from 1972; the record's expiry, {EXPIRES:%Y-%m-%d}, read from"
        " `Leap_Second.dat`, bounds the exact era.")
    say()
    say("Three Uranus models: **barycentre**, DE440's Uranus barycentre alone (0 -> 7);"
        " **centre**, the barycentre plus `ura184_part-3`'s 7 -> 799; **centre on DE442**,"
        " `ura184_part-3`'s own barycentre (DE442) plus its 7 -> 799. Every row 1900-2100 lies"
        f" inside the kernel's span (JD {SPAN[0]:.1f} to {SPAN[1]:.1f}).")
    say()
    say(f"Constants read from `pck00011.tpc`: Uranus's radii {R_EQ:g}, {R_EQ:g}, {R_POLAR:g} km;"
        f" pole RA {POLE_RA[0]} {POLE_RA[1]:+} T, Dec {POLE_DEC[0]} {POLE_DEC[1]:+} T, with"
        f" {sum(1 for c in NUT_RA if c)} periodic terms in RA and {sum(1 for c in NUT_DEC if c)} in"
        f" Dec; the moons' radii "
        + ", ".join(f"{k.capitalize()} {R_MOON[v]}" for k, v in MOONS.items())
        + f" km; the Sun's radius {R_SUN:g} km.")
    worst, counts = Worst(), {}
    compare_uranus(worst, counts, {})
    for model in ("barycentre", "centre", "centre on DE442"):
        qs = sorted({k[2] for k in worst.v if k[0] == model})
        say()
        say(f"## Uranus, {model} model: worst absolute difference from Horizons, by era")
        say()
        say("| quantity | " + " | ".join(f"{e} ({counts.get((model, e), 0)} rows)" for e in ERAS) + " | worst row |")
        say("|---|" + "---:|" * len(ERAS) + "---|")
        for k in qs:
            vals = [worst.get((model, e, k)) for e in ERAS]
            big = max(vals, key=lambda v: abs(v[0]))
            say(f"| {k} | " + " | ".join(f"{v[0]:+.4f}" if counts.get((model, e), 0) else "-"
                                         for v, e in zip(vals, ERAS)) + f" | {big[1]} |")
    agreement, cases = {}, {}
    compare_moons(worst, counts, agreement, cases)
    for label in MOONS:
        qs = sorted({k[2] for k in worst.v if k[0] == label})
        say()
        say(f"## {label.capitalize()}: worst absolute difference from Horizons, by era")
        say()
        say("| quantity | " + " | ".join(f"{e} ({counts.get((label, e), 0)} rows)" for e in ERAS) + " | worst row |")
        say("|---|" + "---:|" * len(ERAS) + "---|")
        for k in qs:
            vals = [worst.get((label, e, k)) for e in ERAS]
            big = max(vals, key=lambda v: abs(v[0]))
            say(f"| {k} | " + " | ".join(f"{v[0]:+.4f}" if counts.get((label, e), 0) else "-"
                                         for v, e in zip(vals, ERAS)) + f" | {big[1]} |")
    say()
    say("## Visibility states against Horizons' codes")
    say()
    say("Ours on Horizons' definition: limb to limb on Uranus's equatorial sphere and each moon's"
        " largest PCK radius, light time considered; the shadow at the moon's emission epoch, in"
        " Uranus's equatorial-sphere umbra. Horizons' codes: `t` transiting, `O` occulted, `p`/`u`"
        " partial/total umbral eclipse, `P`/`U` occulted while partially/totally eclipsed, `*` clear.")
    say()
    say("| moon | agreeing | disagreeing | Horizons' codes |")
    say("|---|---:|---:|---|")
    for label in MOONS:
        codes = ", ".join(f"`{k[1][5:]}` {v}" for k, v in sorted(agreement.items())
                          if k[0] == label and k[1].startswith("code "))
        say(f"| {label} | {agreement.get((label, 'agree'), 0):,} | {agreement.get((label, 'disagree'), 0):,} | {codes} |")
    for label in MOONS:
        if cases.get(label):
            say()
            say(f"Disagreements, {label} ({len(cases[label])}):")
            for c in cases[label][:40]:
                say(f"- {c}")
            if len(cases[label]) > 40:
                say(f"- ... and {len(cases[label]) - 40} more")
    say()
    say("## The planetary ephemeris, and the centre offset")
    say()
    sep = ephemeris_separation()
    say("| quantity | worst |")
    say("|---|---:|")
    for k, v in sep.items():
        say(f"| {k} | {np.max(np.abs(v)):.6g} |")
    say()
    say("## The named events against Espenak's almanacs")
    say()
    say("The almanac gives each event to the hour (GMT). The models' instants are the geocentric"
        " apparent ecliptic longitude difference of date reaching 180° or 0°, to the second.")
    say()
    say("| event | almanac (UT) | barycentre model | minus almanac | centre on DE442 | minus almanac |")
    say("|---|---|---|---:|---|---:|")
    for kind, when, found in named_events():
        b, c = found["barycentre"], found["centre on DE442"]
        say(f"| {kind} {when:%Y} | {when:%Y-%m-%d %H:%M} | {b:%Y-%m-%d %H:%M:%S} |"
            f" {(b - when).total_seconds() / 60:+.1f} min | {c:%Y-%m-%d %H:%M:%S} |"
            f" {(c - when).total_seconds() / 60:+.1f} min |")


if __name__ == "__main__":
    main()
