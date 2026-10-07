#!/usr/bin/env python3
"""Reference states across JUP365's 1997 segment split (#473).

JUP365 keeps two SPK Type 2 segments per body, meeting at JD 2450464.5
TDB (1997-01-16; measured in #472). An independent reader (jplephem)
evaluates the *unmodified* source kernel at epochs on both sides of
the split and at the split itself, and prints every covering segment's
state - at the split, both - so the Java reader's choice of segment and
its arithmetic are both held: the reader's state must equal the row of
the segment NAIF's precedence selects, to rounding, and the two
segments' rows at the split measure the fit's own discontinuity.

Usage: spk-boundary-reference.py jup365.bsp > spk-boundary-reference.txt
Pairs and the window are those of SpkBoundaryFixtureMain, which cuts
the committed fixture kernel over the same window.
"""
import hashlib
import sys
from pathlib import Path

from jplephem.spk import SPK

SPLIT_JD = 2450464.5          # the split, measured in #472
HALF_WINDOW_DAYS = 20.0       # the fixture's window about the split
PAIRS = [(5, 501), (5, 599), (0, 5)]

kernel = Path(sys.argv[1])
digest = hashlib.sha256(kernel.read_bytes()).hexdigest()
spk = SPK.open(str(kernel))

print("# Reference states across the JUP365 segment split (issue #473)")
print(f"# generator: scripts/spk-boundary-reference.py with jplephem "
      f"{__import__('jplephem').__version__} (MIT), an independent reader,"
      " on the unmodified source kernel")
print(f"# kernel: {kernel.name} sha256 {digest}")
print(f"# split: JD {SPLIT_JD:.1f} TDB; window: {HALF_WINDOW_DAYS:.0f} days each side")
print("# columns: segment(center->target) index jd_tdb x_km y_km z_km vx_km_s vy_km_s vz_km_s")
print("# index counts the pair's segments in file order from 0; at the split both segments' rows appear")
print("# jd_tdb is a Julian date on the TDB scale; the reader converts to seconds past J2000")

lo = SPLIT_JD - HALF_WINDOW_DAYS
hi = SPLIT_JD + HALF_WINDOW_DAYS
for center, target in PAIRS:
    of = [s for s in spk.segments if (s.center, s.target) == (center, target)]
    for index, s in enumerate(of):
        init, intlen, rsize, n = s.daf.map_array(s.end_i - 3, s.end_i)
        print(f"# {center}->{target} index {index}: {s.start_jd:.6f} to {s.end_jd:.6f},"
              f" records of {intlen:.0f} s, {int(rsize)} words")
    epochs = {lo, hi, SPLIT_JD, SPLIT_JD - 1e-6, SPLIT_JD + 1e-6}
    for s in of:
        init, intlen, rsize, n = s.daf.map_array(s.end_i - 3, s.end_i)
        start_jd = 2451545.0 + init / 86400.0
        step = intlen / 86400.0
        # record boundaries of this segment's own grid inside the window
        k0 = int((lo - start_jd) // step)
        k1 = int((hi - start_jd) // step) + 1
        for k in range(max(k0, 0), min(k1, int(n)) + 1):
            b = start_jd + k * step
            for e in (b - 1e-6, b, b + 1e-6, b + step / 2):
                if lo <= e <= hi:
                    epochs.add(e)
    for jd in sorted(round(e, 9) for e in epochs):
        for index, s in enumerate(of):
            if not (s.start_jd <= jd <= s.end_jd):
                continue
            p, v = s.compute_and_differentiate(jd)
            v = v / 86400.0  # jplephem differentiates per day
            print(f"{center}->{target} {index} {jd:.9f} "
                  + " ".join(f"{x:.9f}" for x in p)
                  + " " + " ".join(f"{x:.12f}" for x in v))
spk.close()
