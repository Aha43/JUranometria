#!/usr/bin/env python3
"""Kernel-level reference states for the Java SPK reader (#399).

An independent implementation (jplephem) evaluates the committed subset
kernel at chosen TDB Julian dates and prints the raw segment states -
position km and velocity km/s - for the four segments. The Java
reader is held to these at machine precision, because both read the
same Chebyshev coefficients: any difference is a reading mistake.

Epochs: both ends of the coverage, both sides of several record
boundaries (32-day records from the file's own INIT), and a spread of
ordinary dates. Output goes to stdout; the caller commits it with the
header this script prints.
"""
import hashlib
import sys
from pathlib import Path

from jplephem.spk import SPK
from jplephem.daf import DAF

kernel = Path(sys.argv[1])
digest = hashlib.sha256(kernel.read_bytes()).hexdigest()
spk = SPK.open(str(kernel))

print("# Reference states for the JUranometria SPK reader (issue #399)")
print(f"# generator: scripts/spk-reference.py with jplephem "
      f"{__import__('jplephem').__version__} (MIT), an independent reader")
print(f"# kernel: {kernel.name} sha256 {digest}")
print("# columns: segment(center->target) jd_tdb x_km y_km z_km vx_km_s vy_km_s vz_km_s")
print("# jd_tdb is a Julian date on the TDB scale; the reader converts to seconds past J2000")

segments = {(s.center, s.target): s for s in spk.segments}
epochs = []
for s in spk.segments:
    # the file's own record grid
    init, intlen, rsize, n = s.daf.map_array(s.end_i - 3, s.end_i)
    start_jd = 2451545.0 + init / 86400.0
    step_days = intlen / 86400.0
    for k in (0, 1, 2, 100, 1000, n // 2, n - 2, n - 1):
        b = start_jd + k * step_days
        epochs += [b, b + 1e-6, b - 1e-6 if k else b, b + step_days / 2]
    epochs += [s.start_jd, s.end_jd - 1e-6]
    break  # same grid for all four
epochs += [2415020.5, 2429000.0, 2440587.5, 2451545.0, 2451545.25,
           2460000.0, 2461000.123456789, 2470000.0, 2488069.5]
epochs = sorted(set(round(e, 9) for e in epochs))

for (center, target), s in sorted(segments.items()):
    for jd in epochs:
        if not (s.start_jd <= jd <= s.end_jd):
            continue
        p, v = s.compute_and_differentiate(jd)
        v = v / 86400.0  # jplephem differentiates per day
        print(f"{center}->{target} {jd:.9f} "
              + " ".join(f"{x:.9f}" for x in p)
              + " " + " ".join(f"{x:.12f}" for x in v))
spk.close()
