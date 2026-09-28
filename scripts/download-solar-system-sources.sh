#!/bin/sh
# Fetch the pinned raw inputs of the Solar System pack (issue #399):
# JPL's official de440s.bsp from NAIF and the IERS leap-second file.
# Both are held to a SHA-256 pinned here, the same digests
# juranometria.tool.SolarSystemPackMain verifies before it builds.
#
# The leap-second file changes upstream about twice a year, when a new
# Bulletin C extends its validity. That is deliberate: a pin that no
# longer matches fails loudly here, and updating it - then rebuilding
# the pack with `make import-solar-system` - is how the exact time
# interval moves forward, through the pack's provenance and nowhere
# else. Nothing at build, test or run time reaches the network.
set -e

RAW_DIR="$(dirname "$0")/../imports/raw/solar-system"
mkdir -p "$RAW_DIR"

DE440S_URL="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets/de440s.bsp"
DE440S_SHA256="c1c7feeab882263fc493a9d5a5b2ddd71b54826cdf65d8d17a76126b260a49f2"
LEAP_URL="https://hpiers.obspm.fr/iers/bul/bulc/Leap_Second.dat"
LEAP_SHA256="6cb6f5d4b819f2e568e25db4b0b26d89dedf031fdffb18bc94d40f4e94e268d7"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

fetch_pinned() {
  url="$1"; target="$2"; expected="$3"
  if [ -f "$target" ]; then
    if [ "$(sha256 "$target")" = "$expected" ]; then
      echo "present and pinned: $target"
      return 0
    fi
    echo "removing $target: it does not match its pin"
    rm -f "$target"
  fi
  echo "fetching $url"
  curl -fsSL "$url" -o "$target.download"
  actual="$(sha256 "$target.download")"
  if [ "$actual" != "$expected" ]; then
    rm -f "$target.download"
    echo "ERROR: $url failed its pinned SHA-256 (expected $expected, got $actual)." >&2
    echo "If the upstream file was legitimately updated, review it, update the pin" >&2
    echo "here and in SolarSystemPackMain, and rebuild the pack." >&2
    exit 1
  fi
  mv "$target.download" "$target"
}

fetch_pinned "$DE440S_URL" "$RAW_DIR/de440s.bsp" "$DE440S_SHA256"
fetch_pinned "$LEAP_URL" "$RAW_DIR/Leap_Second.dat" "$LEAP_SHA256"

echo "Done. Files in $RAW_DIR"
