#!/bin/sh
# Release metadata for an annotated vX.Y.Z tag (issue #88): the
# agreement check that must pass BEFORE anything is built or
# published, and the release notes assembled from the changelog
# section the tag names.
#
# Nothing here talks to GitHub. It reads the tagged tree and writes a
# notes file, so the same logic is exercised by the test suite, by a
# rehearsal, and by the real release - one code path, three callers.
#
#   scripts/release-metadata.sh check  <tag> [tree]
#   scripts/release-metadata.sh notes  <tag> [tree] [out]
#
# Exit status is the vocabulary the workflow reads:
#   0  agreement holds
#   2  the tag is not a vX.Y.Z version
#   3  the tag and VERSION disagree
#   4  CHANGELOG.md has no section for this version, or it is empty
#   5  the tree is not a JUranometria checkout
set -eu

usage() {
    echo "usage: $0 check|notes <vX.Y.Z> [tree] [out]" >&2
    exit 64
}

command="${1:-}"
tag="${2:-}"
tree="${3:-.}"
out="${4:--}"
[ -n "$command" ] && [ -n "$tag" ] || usage

if [ ! -f "$tree/VERSION" ] || [ ! -f "$tree/CHANGELOG.md" ]; then
    echo "not a JUranometria checkout: $tree" >&2
    exit 5
fi

# The tag is the only input a human types on release day, so it is
# checked strictly: no v1.0, no v1.0.0-rc1, no stray whitespace.
case "$tag" in
    v[0-9]*.[0-9]*.[0-9]*) ;;
    *)
        echo "malformed release tag: '$tag' (expected vX.Y.Z)" >&2
        exit 2
        ;;
esac
version="${tag#v}"
case "$version" in
    *[!0-9.]* | *..* | .* | *.)
        echo "malformed release tag: '$tag' (expected vX.Y.Z)" >&2
        exit 2
        ;;
esac
if [ "$(echo "$version" | tr -cd '.' | wc -c | tr -d ' ')" != "2" ]; then
    echo "malformed release tag: '$tag' (expected vX.Y.Z)" >&2
    exit 2
fi

declared="$(tr -d ' \t\r\n' < "$tree/VERSION")"
if [ "$declared" != "$version" ]; then
    echo "tag $tag does not match VERSION $declared" >&2
    echo "the release commit must carry the version it is tagged with" >&2
    exit 3
fi

# The changelog section for exactly this version: from its heading to
# the next top-level heading, heading line excluded.
section="$(awk -v want="## [$version]" '
    index($0, want) == 1 { collecting = 1; next }
    collecting && /^## / { exit }
    collecting { print }
' "$tree/CHANGELOG.md")"

# Blank lines only is the same as absent: a release must say what
# changed.
if [ -z "$(printf '%s' "$section" | tr -d ' \t\r\n')" ]; then
    echo "CHANGELOG.md has no entries under '## [$version]'" >&2
    echo "add the section, dated, before tagging" >&2
    exit 4
fi

if [ "$command" = "check" ]; then
    echo "$tag agrees with VERSION and CHANGELOG.md"
    exit 0
fi
[ "$command" = "notes" ] || usage

notes="$(
    cat <<HEADER
Download the file for your machine, unpack it anywhere, and open the
atlas. The first four include their own Java runtime — install nothing.

| Your machine | File | Launch |
|---|---|---|
| Mac, Apple silicon | \`JUranometria-$version-macos-arm64.zip\` | open \`JUranometria.app\` |
| Mac, Intel | \`JUranometria-$version-macos-x64.zip\` | open \`JUranometria.app\` |
| Windows 11 (x86-64) | \`JUranometria-$version-windows-x64.zip\` | \`JUranometria\\JUranometria.exe\` |
| Linux (x86-64) | \`JUranometria-$version-linux-x64.zip\` | \`JUranometria/bin/JUranometria\` |
| Bring your own Java 21+ | \`JUranometria-$version-portable.zip\` | \`./juranometria\`, \`juranometria.bat\`, or \`java -jar JUranometria.jar\` |

Verify your download against \`SHA256SUMS.txt\`. Paths containing
spaces are fine.

**These builds are unsigned.** On macOS, Gatekeeper may block the
first launch: right-click the app and choose Open, or approve it
under System Settings > Privacy & Security. On Windows, SmartScreen
may show "Windows protected your PC": choose More info, then Run
anyway.

**Everything works offline, permanently.** The application makes no
network requests of any kind — no telemetry, no update check, no
remote lookup.

## What changed in $version
HEADER
    printf '%s\n' "$section"
    cat <<'FOOTER'
## Licensing

The code is MIT. The bundled star catalogue is derived from the
Tycho-2 Catalogue under **CC BY-NC 3.0 IGO**, so **the packaged
application may be used and redistributed non-commercially only**
for as long as that data is included. Deep-sky data from OpenNGC
(CC BY-SA 4.0); constellation geography and star identities from
d3-celestial (BSD-3-Clause); Tabler icons (MIT). The four
self-contained downloads bundle an Eclipse Temurin 21 runtime
(GPLv2 with the Classpath Exception) with its complete legal
notices inside. Every archive carries the full licensing map, and
Help > About states the same offline.
FOOTER
)"

if [ "$out" = "-" ]; then
    printf '%s\n' "$notes"
else
    printf '%s\n' "$notes" > "$out"
    echo "release notes for $tag written to $out"
fi
