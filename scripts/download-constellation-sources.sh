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
# HIP-keyed star names and designations, verified against a pinned
# SHA-256 and downloaded atomically like the dependency bootstrap.
STARNAMES_SHA256=19c84bc885f8a97c3b8e1f6a380084c575a9758dedfe35256e911a823ec3a695
STAR_DIR="$RAW_DIR/../star-identities"
mkdir -p "$STAR_DIR"
sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}
target="$STAR_DIR/starnames.json"
if [ -f "$target" ] && [ "$(sha256 "$target")" = "$STARNAMES_SHA256" ]; then
  echo "Already exists: starnames.json"
else
  if [ -f "$target" ]; then
    echo "Checksum mismatch for existing starnames.json (partial or corrupt); re-downloading..."
    rm -f "$target"
  fi
  echo "Downloading starnames.json..."
  curl -fsSL "$D3C_BASE/starnames.json" -o "$target.download"
  actual="$(sha256 "$target.download")"
  if [ "$actual" != "$STARNAMES_SHA256" ]; then
    rm -f "$target.download"
    echo "ERROR: starnames.json failed its pinned SHA-256 (expected $STARNAMES_SHA256, got $actual)." >&2
    exit 1
  fi
  mv "$target.download" "$target"
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
