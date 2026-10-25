#!/bin/sh
# Fetch the pinned raw inputs of the Mars study (issue #601): NAIF's
# official mar099s.bsp (Mars, Phobos and Deimos about the Mars barycentre,
# with DE440's Mars barycentre, Sun, Earth-Moon barycentre and Earth,
# 1995-2050), its comment and merge files, and the satellites directory's
# summary and checksum records. Approved by the owner on 2026-10-25 for
# measurement only. Each is held to the SHA-256 pinned here, recorded on
# first retrieval (2026-10-25) and verified before anything reads it.
# Gitignored downloads, kept whole and unmodified; nothing at build, test
# or run time reaches the network, and nothing here is distributed.
# Whether any excerpt ships is the owner's ruling at the #601 checkpoint.
#
# The study also reads the released packs' own sources, de440s.bsp and
# pck00011.tpc, pinned in download-solar-system-sources.sh and
# download-jovian-sources.sh.
set -e
RAW_DIR="$(dirname "$0")/../imports/raw/mars-system"
mkdir -p "$RAW_DIR"
NAIF_SPK="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites"
# NAIF's record lists mar099s.bsp at MD5 fd7302dfbaa0c63ce85b1e98923ee6a1.
MAR099S_SHA256="997dc93ba640e476da7a494d2237dcdeb145e528db37be8ccee588c615e4e1ff"
MAR099S_CMT_SHA256="d39cf30469cf969992587ca8c92fef739033a6aa1c3d02f89323ed0b76ec16ea"
MAR099S_MRG_SHA256="cf68f8d0bb349dc85db6da141f62af2ad426b22847f919ded1668ddf6d18f591"
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
fetch_pinned "$NAIF_SPK/mar099s.bsp" "$RAW_DIR/mar099s.bsp" "$MAR099S_SHA256"
fetch_pinned "$NAIF_SPK/mar099s.cmt" "$RAW_DIR/mar099s.cmt" "$MAR099S_CMT_SHA256"
fetch_pinned "$NAIF_SPK/mar099s.mrg" "$RAW_DIR/mar099s.mrg" "$MAR099S_MRG_SHA256"
# The summary and checksum records are rewritten by NAIF as the directory
# changes (both were dated 2026-10-24 when retrieved); they are kept as
# retrieved and are not re-fetched against a pin.
date -u +"retrieved %Y-%m-%dT%H:%M:%SZ" > "$RAW_DIR/RETRIEVED.txt"
