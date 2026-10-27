#!/bin/sh
# Fetch the pinned raw inputs of the Uranus study (issue #633): NAIF's
# official ura184_part-3.bsp (Uranus and Miranda, Ariel, Umbriel, Titania
# and Oberon about the Uranus barycentre, URA182.21, with DE442's Uranus
# barycentre, Sun, Earth-Moon barycentre and Earth, and nine irregular
# moons, 1600-2399), and its comment and merge files. Approved by the owner
# on 2026-10-27 ("approve requests 3 and 4", and requests 1 and 2 for the
# descriptive files) for measurement only. Each is held to the SHA-256
# pinned here, recorded on first retrieval (2026-10-27) and verified before
# anything reads it. Gitignored downloads, kept whole and unmodified;
# nothing at build, test or run time reaches the network, and nothing here
# is distributed. Whether any excerpt ships is the owner's ruling at the
# #633 checkpoint.
#
# The study also reads the released packs' own sources, de440s.bsp and
# pck00011.tpc, pinned in download-solar-system-sources.sh and
# download-jovian-sources.sh.
set -e
RAW_DIR="$(dirname "$0")/../imports/raw/uranus-system"
mkdir -p "$RAW_DIR"
NAIF_SPK="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites"
# NAIF's record lists ura184_part-3.bsp at MD5 cf24471fb9037543cdbe168bc0fd4c2e.
URA184_SHA256="273cd4ccc470d1098562cd4e94fa48fc2b4c6b1c2896308e473c949b87937c53"
URA184_CMT_SHA256="c9f4f0ae15b759ea6f5dcc652922629d23b4baa7d10615e0db2766a167d6323f"
URA184_MRG_SHA256="19e86c20aad79697930152c1e7345c9af0e8f834c4bd111a85c6f19396b322f5"
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
fetch_pinned "$NAIF_SPK/ura184_part-3.bsp" "$RAW_DIR/ura184_part-3.bsp" "$URA184_SHA256"
fetch_pinned "$NAIF_SPK/ura184_part-3.cmt" "$RAW_DIR/ura184_part-3.cmt" "$URA184_CMT_SHA256"
fetch_pinned "$NAIF_SPK/ura184_part-3.mrg" "$RAW_DIR/ura184_part-3.mrg" "$URA184_MRG_SHA256"
# The descriptive files of the first request (the satellites summary and
# checksums, and the other families' comment and merge files) are recorded
# in imports/raw/uranus-system/RETRIEVED.txt and docs/studies/uranus-system/
# SOURCES.md; they chose the kernel and are not re-fetched.
