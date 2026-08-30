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

# Star identities (Sprint 13): the same pinned commit supplies the
# HIP-keyed star names and designations.
mkdir -p "$RAW_DIR/../star-identities"
if [ -f "$RAW_DIR/../star-identities/starnames.json" ]; then
  echo "Already exists: starnames.json"
else
  echo "Downloading starnames.json..."
  curl -fsSL "$D3C_BASE/starnames.json" -o "$RAW_DIR/../star-identities/starnames.json"
fi

# The upstream licence text ships verbatim inside the generated pack.
dest="$RAW_DIR/LICENSE"
if [ -f "$dest" ]; then
  echo "Already exists: LICENSE"
else
  echo "Downloading LICENSE..."
  curl -fsSL "https://raw.githubusercontent.com/ofrohn/d3-celestial/$D3C_COMMIT/LICENSE" -o "$dest"
fi

echo "Done. Files in $RAW_DIR"
