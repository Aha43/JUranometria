#!/bin/sh
# Fetch the pinned raw inputs of the Saturn study (issue #574): NAIF's
# official sat441.bsp (Saturn, its eight major moons, Phoebe and four
# small moons about the Saturn barycentre, 1749-2250), its comment and
# merge files; and, for comparison only (approved 2026-10-19, not the
# Saturn authority), NAIF's DE442s planetary ephemeris (1849-2150), its
# merge file and the planets directory's checksum record; and IMCCE's
# predictions of the Saturnian satellites' phenomena 2006-2013 (TASS 1.6,
# W. Thuillot), kept whole and never committed or bundled - only quoted
# rows are committed, with attribution (approved 2026-10-19). Each is held to the SHA-256 pinned here, recorded on first
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
NAIF_PLANETS="https://naif.jpl.nasa.gov/pub/naif/generic_kernels/spk/planets"
DE442S_SHA256="54d97562a5b094d298b1b8eafa5a2e17e3e010ce85e1a366d07f003ad159323c"
DE442S_MRG_SHA256="7823b2fff1f737a252993459470602f259656f4a922ebf3ab5490d16c4474c7a"
# NAIF's record lists de442s.bsp at MD5 cc49327e06088124c0e39d8dde9f0b58.
PLANETS_CHECKSUMS_SHA256="3c618bed7d51a48ee059269097eeeab1f69f3b2641ec2aeb96132757418c845b"
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
fetch_pinned "$NAIF_PLANETS/de442s.bsp" "$RAW_DIR/de442s.bsp" "$DE442S_SHA256"
fetch_pinned "$NAIF_PLANETS/de442s.mrg" "$RAW_DIR/de442s.mrg" "$DE442S_MRG_SHA256"
fetch_pinned "$NAIF_PLANETS/aa_checksums.txt" "$RAW_DIR/planets-aa_checksums.txt" "$PLANETS_CHECKSUMS_SHA256"
IMCCE_PHENSATURNE="https://ftp.imcce.fr/pub/ephem/satel/phensaturne"
fetch_pinned "$IMCCE_PHENSATURNE/README.txt" "$RAW_DIR/imcce-phensaturne-README.txt" "9fba712de344e3dd76b205d089d903fc464a9331fb7c4aa6c11073589630c637"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S1.dat" "$RAW_DIR/imcce-phensaturne-phenE-S1.dat" "fa462f76bce97e89441da5ea58b992ad5c2e6d1d0db62ba3cf56905cf8937145"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S2.dat" "$RAW_DIR/imcce-phensaturne-phenE-S2.dat" "7fc95bdfc246f5cee825397604e39c01d2b1d8a13d244ef458b61cfef765626c"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S3.dat" "$RAW_DIR/imcce-phensaturne-phenE-S3.dat" "ffe39d0bbace5edb145409c0e1df4213689a839e02a0ca19a1e15261d0f1ce7b"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S4.dat" "$RAW_DIR/imcce-phensaturne-phenE-S4.dat" "0a8431191a27be84acd8d85a7e58046edfe57a89a75faa5750ae543c05c1c302"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S5.dat" "$RAW_DIR/imcce-phensaturne-phenE-S5.dat" "30a04db8a9a75b5a8ccf851b0003559ee89f8891c95cf5c0c9327e6243cb7661"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S6.dat" "$RAW_DIR/imcce-phensaturne-phenE-S6.dat" "bb1fafd2855dcc343c3a0bcc6b5211c3a0887eb3c7e1155df7c91e86178d04c8"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S7.dat" "$RAW_DIR/imcce-phensaturne-phenE-S7.dat" "0961de801d13c8c33b467906311134c47b3ec21cb9ee01a81d2a448d598aaf82"
fetch_pinned "$IMCCE_PHENSATURNE/phenE-S8.dat" "$RAW_DIR/imcce-phensaturne-phenE-S8.dat" "59e88532bfb959c65cbc70695f61fff3c247912a715dd5994426af0d192e81b2"
date -u +"retrieved %Y-%m-%dT%H:%M:%SZ" > "$RAW_DIR/RETRIEVED.txt"
