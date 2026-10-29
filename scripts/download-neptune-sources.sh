#!/bin/sh
# Fetch the pinned raw inputs of the Neptune study (issue #654): NAIF's
# official nep098_part-1.bsp (Neptune, Triton, Nereid and Naiad about the
# Neptune barycentre, NEP098, with DE442's Neptune barycentre, Sun,
# Earth-Moon barycentre and Earth, 1800-2199), and the comment and merge
# files of the Neptune kernels read in choosing it. Approved by the owner
# on 2026-10-29 ("approve request 4", and requests 1 and 2 for the
# descriptive files) for measurement only. Each is held to the SHA-256
# pinned here, recorded on first retrieval (2026-10-29; the kernel first
# checked against NAIF's published MD5) and verified before anything reads
# it. Gitignored downloads, kept whole and unmodified; nothing at build,
# test or run time reaches the network, and nothing here is distributed.
# Whether any excerpt ships is the owner's ruling at the #654 checkpoint.
#
# RAW_DIR names the destination. Run it against the main checkout's
# ignored imports/raw/neptune-system, never only a worktree's: a removed
# worktree takes its imports with it (the Uranus road's lesson, #634).
#
# The study also reads the released packs' own sources, de440s.bsp and
# pck00011.tpc, pinned in download-solar-system-sources.sh and
# download-jovian-sources.sh.
set -e
RAW_DIR="${RAW_DIR:-$(dirname "$0")/../imports/raw/neptune-system}"
mkdir -p "$RAW_DIR"
NAIF_SPK="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites"
# NAIF's record (satellites/aa_checksums.txt) lists nep098_part-1.bsp at
# MD5 e83c4bf7d362822d35fd6159f4f3a319.
NEP098_MD5="e83c4bf7d362822d35fd6159f4f3a319"
NEP098_SHA256="5327f47abba438d993237ff676ab03b728778a965bb238f86de883c0ad7790b8"
NEP098_CMT_SHA256="3f89f88f0a247d4dc945501f512a4e6d5ed5c7288cd89f03822301befc7ade2f"
NEP098_MRG_SHA256="ecfa849333cfeebb9ea81fef80d66b4a250a1bb56d7e8cd24ceab43d3c01d55c"
sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}
md5of() {
  if command -v md5sum >/dev/null 2>&1; then md5sum "$1" | cut -d' ' -f1; else md5 -q "$1"; fi
}
fetch_pinned() {
  url="$1"; target="$2"; expected="$3"; expected_md5="$4"
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
  if [ -n "$expected_md5" ] && [ "$(md5of "$target.download")" != "$expected_md5" ]; then
    rm -f "$target.download"
    echo "refused $url: md5 is not NAIF's published $expected_md5" >&2
    exit 1
  fi
  mv "$target.download" "$target"
  echo "$(basename "$target") $actual"
}
fetch_pinned "$NAIF_SPK/nep098_part-1.bsp" "$RAW_DIR/nep098_part-1.bsp" "$NEP098_SHA256" "$NEP098_MD5"
fetch_pinned "$NAIF_SPK/nep098_part-1.cmt" "$RAW_DIR/nep098_part-1.cmt" "$NEP098_CMT_SHA256" ""
fetch_pinned "$NAIF_SPK/nep098_part-1.mrg" "$RAW_DIR/nep098_part-1.mrg" "$NEP098_MRG_SHA256" ""
# The descriptive files of the first two requests (the satellites summary and
# checksums, and the comment and merge files of nep097, nep105 and nep098
# parts 2 and 3) are recorded in docs/studies/neptune-system/SOURCES.md; they
# chose the kernel and are not re-fetched.
