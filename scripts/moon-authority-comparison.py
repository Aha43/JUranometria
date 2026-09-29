#!/usr/bin/env python3
"""#406 authority comparison for the Moon: Skyfield on de440s vs JPL Horizons,
plus Meeus 47.a/48.a, plus the bright-limb position angle checked from the
oracle's own Sun and Moon positions. Offline from the kept responses."""
import datetime as dt
import math
from pathlib import Path

import numpy as np
from skyfield.api import Loader, wgs84
from skyfield.constants import AU_KM

HERE = Path(__file__).parent
load = Loader(str(HERE / "skyfield-cache"), verbose=False)
ts = load.timescale(builtin=True)
kernel = load("/private/tmp/j398-study/downloads/de440s.bsp")
earth, sun, moon = kernel["earth"], kernel["sun"], kernel["moon"]
R_MOON_KM = 1737.4
ARCSEC = 3600.0

OBSERVERS = {"oslo": (10.75, 59.91, 0.02), "quito": (281.5, -0.18, 2.85),
             "cape-town": (18.42, -33.93, 0.01), "alert": (297.66, 82.50, 0.03),
             "chatham": (183.5, -43.95, 0.02)}
out = []


def say(s=""):
    out.append(s); print(s)


def sep(ra1, dec1, ra2, dec2):
    a1, d1, a2, d2 = map(math.radians, (ra1, dec1, ra2, dec2))
    c = math.sin(d1)*math.sin(d2) + math.cos(d1)*math.cos(d2)*math.cos(a1-a2)
    return math.degrees(math.acos(max(-1.0, min(1.0, c)))) * ARCSEC


def rows(path, cols):
    text = path.read_text(encoding="utf-8")
    body = text[text.index("$$SOE")+5:text.index("$$EOE")].strip()
    res = []
    for line in body.splitlines():
        f = [x.strip() for x in line.split(",")]
        stamp = f[0] if "." in f[0] else f[0] + ".000"
        when = dt.datetime.strptime(stamp, "%Y-%b-%d %H:%M:%S.%f")
        res.append((when, f))
    return res


def bright_limb_pa(ra_s, dec_s, ra_m, dec_m):
    """Meeus 48.5: position angle of the Moon's bright limb, from north
    through east, from apparent Sun (s) and Moon (m) positions, degrees."""
    a0, d0, a, d = map(math.radians, (ra_s, dec_s, ra_m, dec_m))
    y = math.cos(d0) * math.sin(a0 - a)
    x = math.sin(d0)*math.cos(d) - math.cos(d0)*math.sin(d)*math.cos(a0 - a)
    return math.degrees(math.atan2(y, x)) % 360.0


class Worst:
    def __init__(self): self.v = {}
    def note(self, k, val, tag):
        if k not in self.v or abs(val) > abs(self.v[k][0]): self.v[k] = (val, tag)
    def get(self, k): return self.v.get(k, (0.0, "-"))


def era(w):
    return "1900-1961" if w.year < 1962 else "1962-1971" if w.year < 1972 else "1972-2027-06" if w < dt.datetime(2027, 6, 28) else "after"


def main():
    say("# #406 Moon authority comparison, measured " + dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%d %H:%MZ"))
    say()
    worst = Worst(); counts = {}; libr = Worst(); parallax = Worst()
    # Moon columns (QUANTITIES 1,2,4,10,13,14,20,23,24,31,43):
    # 3 RA 4 DEC 5 RAapp 6 DECapp 7 Az 8 El 9 Illu% 10 diam 11 subLon 12 subLat 13 delta 14 deldot 15 SOT 16 /r 17 STO 18 EcLon 19 EcLat 20 phi 21 PABlon 22 PABlat
    for name in sorted(p.stem for p in (HERE/"horizons").glob("*.txt") if p.stem.startswith(("named-", "matrix-7d-", "dense-"))):
        site = next(s for s in OBSERVERS if name.endswith(s))
        lon, lat, alt = OBSERVERS[site]
        topos = wgs84.latlon(lat, lon if lon <= 180 else lon-360, elevation_m=alt*1000)
        observer = earth + topos
        for w, f in rows(HERE/"horizons"/f"{name}.txt", None):
            civil = (w.year, w.month, w.day, w.hour, w.minute, w.second + w.microsecond/1e6)
            t = ts.utc(*civil) if w.year >= 1972 else ts.ut1(*civil)
            astro = observer.at(t).observe(moon)
            ra, dec, dist = astro.radec()
            app = astro.apparent(deflectors=())
            ra_a, dec_a, _ = app.radec(epoch="date")
            alt_, az, _ = app.altaz()
            diam = 2*math.degrees(math.asin(R_MOON_KM/dist.km))*ARCSEC
            sun_app = observer.at(t).observe(sun).apparent(deflectors=())
            phase = app.separation_from(sun_app)  # not the phase angle; elongation
            elongation = phase.degrees
            # phase angle at the Moon (Sun-Moon-observer): from vectors
            m_pos = astro.position.km; s_from_m = (observer.at(t).observe(sun).position.km - m_pos)
            cosi = np.dot(-m_pos, s_from_m) / (np.linalg.norm(m_pos)*np.linalg.norm(s_from_m))
            i = math.degrees(math.acos(max(-1, min(1, cosi))))
            k = (1 + cosi)/2*100
            e = era(w); counts[e] = counts.get(e, 0) + 1
            tag = f"{site} {w:%Y-%m-%d %H:%M}"
            worst.note((e, "astrometric"), sep(ra._degrees, dec.degrees, float(f[3]), float(f[4])), tag)
            worst.note((e, "apparent"), sep(ra_a._degrees, dec_a.degrees, float(f[5]), float(f[6])), tag)
            worst.note((e, "horizontal"), sep(az.degrees, alt_.degrees, float(f[7]), float(f[8])), tag)
            worst.note((e, "distance km"), dist.km - float(f[13])*AU_KM, tag)
            worst.note((e, "diameter arcsec"), diam - float(f[10]), tag)
            worst.note((e, "illuminated %"), k - float(f[9]), tag)
            worst.note((e, "phase angle deg"), i - float(f[17]), tag)
            worst.note((e, "elongation deg"), elongation - float(f[15]), tag)
            libr.note(("lon", e), float(f[11]), tag); libr.note(("lat", e), float(f[12]), tag)
            # topocentric vs geocentric: the parallax actually applied
            geo = earth.at(t).observe(moon); gra, gdec, _ = geo.radec()
            parallax.note((e,), sep(gra._degrees, gdec.degrees, ra._degrees, dec.degrees), tag)
    say("## Skyfield (DE440) vs Horizons (DE441), worst absolute difference by era")
    say(); say("| era | rows | astrometric | apparent | horizontal | distance | diameter | illuminated | phase angle | elongation |")
    say("|---|---|---|---|---|---|---|---|---|---|")
    for e in ["1900-1961", "1962-1971", "1972-2027-06", "after"]:
        g = lambda k: worst.get((e, k))[0]
        say(f"| {e} | {counts.get(e,0)} | {g('astrometric'):.3f}″ | {g('apparent'):.3f}″ | {g('horizontal'):.2f}″ | {g('distance km'):+.4f} km | {g('diameter arcsec'):+.4f}″ | {g('illuminated %'):+.4f} % | {g('phase angle deg')*3600:+.2f}″ | {g('elongation deg')*3600:+.2f}″ |")
    say(); say("Where the worst rows are:")
    for (e, k), (v, tag) in sorted(worst.v.items()): say(f"- {e}, {k}: {v:.5f} at {tag}")
    say(); say("Libration (Horizons sub-observer point) extremes by era, and the parallax applied (topocentric − geocentric):")
    for e in ["1900-1961", "1962-1971", "1972-2027-06", "after"]:
        say(f"- {e}: sub-lon max |{libr.get(('lon', e))[0]:.2f}°| at {libr.get(('lon', e))[1]}; sub-lat max |{libr.get(('lat', e))[0]:.2f}°| at {libr.get(('lat', e))[1]}; parallax max {parallax.get((e,))[0]/3600:.3f}° at {parallax.get((e,))[1]}")
    say()
    # Bright-limb PA: Meeus 48.5 from Skyfield inputs vs from Horizons' own Sun+Moon apparent positions (Oslo, weekly)
    moon_rows = {w: f for w, f in rows(HERE/"horizons/matrix-7d-oslo.txt", None)}
    sun_rows = {w: f for w, f in rows(HERE/"horizons/sun-matrix-7d-oslo.txt", None)}  # cols: 3 RA 4 DEC 5 RAapp 6 DECapp 7 Az 8 El 9 delta 10 deldot 11 EcLon 12 EcLat
    topos = wgs84.latlon(59.91, 10.75, elevation_m=20); observer = earth + topos
    w_chi = Worst(); n = 0
    for w in moon_rows:
        if w not in sun_rows: continue
        fm, fs = moon_rows[w], sun_rows[w]
        chi_oracle = bright_limb_pa(float(fs[5]), float(fs[6]), float(fm[5]), float(fm[6]))
        civil = (w.year, w.month, w.day, w.hour, w.minute, w.second)
        t = ts.utc(*civil) if w.year >= 1972 else ts.ut1(*civil)
        ma = observer.at(t).observe(moon).apparent(deflectors=()); sa = observer.at(t).observe(sun).apparent(deflectors=())
        ra_m, dec_m, _ = ma.radec(epoch="date"); ra_s, dec_s, _ = sa.radec(epoch="date")
        chi = bright_limb_pa(ra_s._degrees, dec_s.degrees, ra_m._degrees, dec_m.degrees)
        d = (chi - chi_oracle + 540) % 360 - 180
        w_chi.note(("chi",), d*3600, f"{w:%Y-%m-%d %H:%M} illu={fm[9]}"); n += 1
    say(f"Bright-limb position angle χ (Meeus 48.5) from Skyfield apparent positions vs from Horizons' own apparent Sun and Moon, Oslo weekly, {n} rows: worst {w_chi.get(('chi',))[0]:.2f}″ at {w_chi.get(('chi',))[1]}")
    say()
    # Meeus 47.a / 48.a: 1992 April 12.0 TD, geocentric
    t = ts.tt(1992, 4, 12, 0, 0, 0)
    g = earth.at(t).observe(moon); ga = g.apparent(deflectors=())
    lat_e, lon_e, dist_e = ga.ecliptic_latlon(epoch="date")
    ra_a, dec_a, _ = ga.radec(epoch="date")
    sa = earth.at(t).observe(sun).apparent(deflectors=()); rs, ds, _ = sa.radec(epoch="date")
    m_pos = g.position.km; s_from_m = earth.at(t).observe(sun).position.km - m_pos
    cosi = np.dot(-m_pos, s_from_m)/(np.linalg.norm(m_pos)*np.linalg.norm(s_from_m)); i = math.degrees(math.acos(cosi)); k = (1+cosi)/2
    chi = bright_limb_pa(rs._degrees, ds.degrees, ra_a._degrees, dec_a.degrees)
    say("## Meeus 47.a and 48.a (1992 April 12.0 TD, geocentric; published, non-JPL)")
    say(); say("| quantity | Meeus | Skyfield/DE440 | difference |"); say("|---|---|---|---|")
    say(f"| apparent λ | 133.162655° | {lon_e.degrees:.6f}° | {(lon_e.degrees-133.162655)*ARCSEC:+.1f}″ |")
    say(f"| apparent β | −3.229126° | {lat_e.degrees:.6f}° | {(lat_e.degrees+3.229126)*ARCSEC:+.1f}″ |")
    say(f"| distance Δ | 368409.7 km | {dist_e.km:.1f} km | {dist_e.km-368409.7:+.1f} km |")
    say(f"| apparent α | 134.688470° | {ra_a._degrees:.6f}° | {(ra_a._degrees-134.688470)*ARCSEC:+.1f}″ |")
    say(f"| apparent δ | +13.768368° | {dec_a.degrees:.6f}° | {(dec_a.degrees-13.768368)*ARCSEC:+.1f}″ |")
    say(f"| phase angle i (48.a) | 69.0756° | {i:.4f}° | {(i-69.0756)*ARCSEC:+.1f}″ |")
    say(f"| illuminated fraction k (48.a) | 0.6786 | {k:.4f} | {k-0.6786:+.4f} |")
    say(f"| bright-limb PA χ (48.a) | 285.0° | {chi:.2f}° | {chi-285.0:+.2f}° |")
    hz = rows(HERE/"horizons/geocentric-meeus-47a-tt.txt", None)[0][1]  # 1,2,10,13,20,23,24,31,43: 3 RA 4 DEC 5 RAapp 6 DECapp 7 Illu 8 diam 9 delta 10 deldot 11 SOT 12 /r 13 STO 14 EcLon 15 EcLat 16 phi ...
    say(f"| Horizons at the same TT instant: apparent α, δ; k; i | {float(hz[5]):.6f}°, {float(hz[6]):.6f}°; {float(hz[7]):.4f}%; {float(hz[13]):.4f}° | vs Skyfield {sep(ra_a._degrees, dec_a.degrees, float(hz[5]), float(hz[6])):.2f}″; {k*100-float(hz[7]):+.4f}%; {(i-float(hz[13]))*ARCSEC:+.1f}″ | |")
    say()
    # 2026: perigee/apogee from the daily series, and the size range
    d = [(w, float(f[13])*AU_KM, float(f[10]), float(f[9])) for w, f in rows(HERE/"horizons/dense-2026-oslo.txt", None)]
    near = min(d, key=lambda r: r[1]); far = max(d, key=lambda r: r[1])
    say(f"2026 daily series at Oslo (Horizons): nearest {near[0]:%Y-%m-%d} at {near[1]:.0f} km, diameter {near[2]:.1f}″; farthest {far[0]:%Y-%m-%d} at {far[1]:.0f} km, diameter {far[2]:.1f}″ (topocentric; daily sampling).")
    (HERE/"comparison-moon.md").write_text("\n".join(out)+"\n", encoding="utf-8")


if __name__ == "__main__":
    main()
