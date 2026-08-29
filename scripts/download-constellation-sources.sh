#!/bin/sh
# Downloads the pinned raw constellation-geography inputs defined by
# docs/decisions/constellation-geography.md into
# imports/raw/constellations/ (gitignored). Idempotent; delete a file
# to re-fetch it. The import tool verifies SHA-256 checksums of these
# files before transforming anything.
set -e

RAW_DIR="$(dirname "$0")/../imports/raw/constellations"
# d3-celestial (Olaf Frohn, BSD-3-Clause), pinned commit.
D3C_COMMIT="7e720a3de062059d4c5400a379146a601d9010e0"
D3C_BASE="https://raw.githubusercontent.com/ofrohn/d3-celestial/$D3C_COMMIT/data"

mkdir -p "$RAW_DIR"

fetch() {
  dest="$RAW_DIR/$1"
  if [ -f "$dest" ]; then
    echo "Already exists: $1"
  else
    echo "Downloading $1..."
    curl -fsSL "$D3C_BASE/$1" -o "$dest"
  fi
}

fetch constellations.json
fetch constellations.lines.json
fetch constellations.bounds.json

echo "Done. Files in $RAW_DIR"
