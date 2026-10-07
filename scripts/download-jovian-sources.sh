#!/bin/sh
# Fetch the pinned raw inputs of the Jovian study (issue #472): NAIF's
# official jup365.bsp (the JUP365 Galilean-satellite ephemeris merged
# with DE440's Jupiter barycentre, Sun and Earth), its comment and
# merge files, and the IAU 2015 planetary-constants kernel pck00011.tpc.
# Each is held to the SHA-256 pinned here, recorded on first retrieval
# (2026-10-07) and verified by the study before anything is cut from
# them. Gitignored downloads; nothing at build, test or run time
# reaches the network, and nothing here is distributed: the atlas
# ships a modified, renamed excerpt under NAIF's rules, if the owner
# rules so on #472.
set -e
RAW_DIR="$(dirname "$0")/../imports/raw/jovian-system"
mkdir -p "$RAW_DIR"
NAIF_SPK="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/satellites"
NAIF_PCK="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/pck"
JUP365_SHA256="dbf016c01ba4d022154838000cf3f06962cf958ddc503a366f7fe8f81495c5cb"
JUP365_CMT_SHA256="3c851a4b5d1223e155320e7f63558ebcc81908c8f68fb1c7b43985a69663e08a"
JUP365_MRG_SHA256="e954b0291ac1ef2ad45bea296975ed63ef0db5a42c72afc5a770104c116096b1"
PCK_SHA256="3dff7b1dbeceaa01f25467767d3fa25816051c85d162d1edf04acb310ee28bb1"
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
    echo "here and in the study, and re-measure." >&2
    exit 1
  fi
  mv "$target.download" "$target"
}
fetch_pinned "$NAIF_SPK/jup365.bsp" "$RAW_DIR/jup365.bsp" "$JUP365_SHA256"
fetch_pinned "$NAIF_SPK/jup365.cmt" "$RAW_DIR/jup365.cmt" "$JUP365_CMT_SHA256"
fetch_pinned "$NAIF_SPK/jup365.mrg" "$RAW_DIR/jup365.mrg" "$JUP365_MRG_SHA256"
fetch_pinned "$NAIF_PCK/pck00011.tpc" "$RAW_DIR/pck00011.tpc" "$PCK_SHA256"
echo "Done. Files in $RAW_DIR"
