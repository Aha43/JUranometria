#!/bin/sh
# Downloads the pinned Milky Way candidate defined by
# docs/decisions/milky-way-layer.md into imports/raw/milky-way/
# (gitignored). The study verifies the SHA-256 again before reading
# anything.
#
# The bytes are NOT redistributed with this repository: whether they
# may be is precisely what the gate is waiting on
# (https://github.com/ofrohn/d3-celestial/issues/160).
#
# Nothing reaches its final name until its checksum matches, and an
# existing file that fails the checksum is replaced rather than
# trusted - a corrupt download must be repairable by running this
# script again (PR #199 review).
set -e

RAW_DIR="$(dirname "$0")/../imports/raw/milky-way"
# d3-celestial (Olaf Frohn, BSD-3-Clause), the same pinned commit the
# constellation and star-identity imports use.
D3C_COMMIT="7e720a3de062059d4c5400a379146a601d9010e0"
D3C_BASE="https://raw.githubusercontent.com/ofrohn/d3-celestial/$D3C_COMMIT/data"
MW_SHA256=aee221a7a0e879418e685de00c3e68fbdfac5667c0a8aab74929ef9cf4aab4fb

mkdir -p "$RAW_DIR"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

dest="$RAW_DIR/mw.json"

if [ -f "$dest" ]; then
  if [ "$(sha256 "$dest")" = "$MW_SHA256" ]; then
    echo "Already present and verified: mw.json"
    exit 0
  fi
  echo "Present but NOT the pinned bytes: mw.json - re-fetching."
fi

# Downloaded beside its destination, checked there, and only then
# given the name the study reads. A failed download or a corrupted
# transfer leaves the part file behind and the destination untouched.
part="$dest.part"
rm -f "$part"
echo "Downloading mw.json..."
curl -fsSL "$D3C_BASE/mw.json" -o "$part"

found=$(sha256 "$part")
if [ "$found" != "$MW_SHA256" ]; then
  echo "mw.json is not the pinned bytes:" >&2
  echo "  expected $MW_SHA256" >&2
  echo "  found    $found" >&2
  echo "Left at $part; nothing was installed." >&2
  exit 1
fi

mv "$part" "$dest"
echo "mw.json verified against its pinned checksum."
