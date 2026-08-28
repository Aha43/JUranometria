#!/bin/sh
# Downloads the pinned raw catalogue inputs defined by
# docs/decisions/catalogue-sources.md into imports/raw/ (gitignored).
# Idempotent; delete a file to re-fetch it. The import tool verifies
# SHA-256 checksums of these files before transforming anything.
set -e

RAW_DIR="$(dirname "$0")/../imports/raw"
TYCHO_BASE="https://cdsarc.cds.unistra.fr/ftp/I/259"
OPENNGC_TAG="v20260501"
OPENNGC_BASE="https://raw.githubusercontent.com/mattiaverga/OpenNGC/$OPENNGC_TAG"

mkdir -p "$RAW_DIR"

fetch() {
  dest="$RAW_DIR/$2"
  if [ -f "$dest" ]; then
    echo "Already exists: $2"
  else
    echo "Downloading $2..."
    curl -fsSL "$1" -o "$dest"
  fi
}

i=0
while [ "$i" -le 19 ]; do
  n=$(printf '%02d' "$i")
  fetch "$TYCHO_BASE/tyc2.dat.$n.gz" "tyc2.dat.$n.gz"
  i=$((i + 1))
done
fetch "$TYCHO_BASE/suppl_1.dat.gz" "suppl_1.dat.gz"
fetch "$TYCHO_BASE/ReadMe" "tycho2-ReadMe"
fetch "$OPENNGC_BASE/database_files/NGC.csv" "openngc-NGC.csv"
fetch "$OPENNGC_BASE/database_files/addendum.csv" "openngc-addendum.csv"
fetch "$OPENNGC_BASE/LICENSES/CC-BY-SA-4.0.txt" "openngc-CC-BY-SA-4.0.txt"

echo "Done: $RAW_DIR"
