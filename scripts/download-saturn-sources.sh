#!/bin/sh
# Fetch the pinned raw inputs of the Saturn study (issue #574): NAIF's
# official sat441.bsp (Saturn, its eight major moons, Phoebe and four
# small moons about the Saturn barycentre, 1749-2250), its comment and
# merge files. Each is held to the SHA-256 pinned here, recorded on first
# retrieval (2026-10-19) and verified before anything reads it. Gitignored downloads,
# kept whole and unmodified; nothing at build, test or run time reaches
# the network, and nothing here is distributed. Whether any excerpt ships
# is the owner's ruling at the #574 checkpoint.
set -e
RAW_DIR="$(dirname "$0")/../imports/raw/saturn-system"
mkdir -p "$RAW_DIR"
NAIF_SPK="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites"
SAT441_SHA256="d7e444a9ba7a52b8f448ff0747030789524d2f0c1212e4e2bd7f5fc3c96444d5"
SAT441_CMT_SHA256="a45d31f6039854593395b539a3fb61df8f83ab13fb621c3ce5d710ed35629882"
SAT441_MRG_SHA256="9e6c4d2d7d72437c73efffd631265909973cf44b1d1717a9f1ca79604fa5adb2"
sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}
fetch_pinned() {
  url="$1"; target="$2"; expected="$3"
  if [ -f "$target" ] && [ -n "$expected" ]; then
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
  if [ -n "$expected" ] && [ "$actual" != "$expected" ]; then
    rm -f "$target.download"
    echo "refused $url: sha256 $actual is not the pinned $expected" >&2
    exit 1
  fi
  mv "$target.download" "$target"
  echo "$(basename "$target") $actual"
}
fetch_pinned "$NAIF_SPK/sat441.bsp" "$RAW_DIR/sat441.bsp" "$SAT441_SHA256"
fetch_pinned "$NAIF_SPK/sat441.cmt" "$RAW_DIR/sat441.cmt" "$SAT441_CMT_SHA256"
fetch_pinned "$NAIF_SPK/sat441.mrg" "$RAW_DIR/sat441.mrg" "$SAT441_MRG_SHA256"
date -u +"retrieved %Y-%m-%dT%H:%M:%SZ" > "$RAW_DIR/RETRIEVED.txt"
