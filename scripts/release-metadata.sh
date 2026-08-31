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
#   4  CHANGELOG.md has no dated section for this version, more
#      than one, or an empty one
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

# The heading must be the dated one the changelog format promises -
# "## [X.Y.Z] - YYYY-MM-DD" and nothing else. An undated heading is
# an unfinished release note, and matching it loosely would also
# accept "## [1.2.30]" when releasing 1.2.3.
escaped="$(printf '%s' "$version" | sed 's/\./\\./g')"
headings="$(grep -E "^## \[$escaped\] - [0-9]{4}-[0-9]{2}-[0-9]{2}\$" \
    "$tree/CHANGELOG.md" || true)"
found="$(printf '%s' "$headings" | grep -c . || true)"
if [ "$found" -gt 1 ]; then
    # Two sections for one version is an editing accident, and
    # concatenating them would publish notes nobody wrote
    # (automation review).
    echo "CHANGELOG.md has $found sections for $version:" >&2
    printf '%s\n' "$headings" >&2
    echo "a version has exactly one section" >&2
    exit 4
fi
heading="$headings"
if [ -z "$heading" ]; then
    if grep -qE "^## \[$escaped\]" "$tree/CHANGELOG.md"; then
        echo "CHANGELOG.md's section for $version is not dated" >&2
        echo "the heading must read '## [$version] - YYYY-MM-DD'" >&2
    else
        echo "CHANGELOG.md has no section '## [$version]'" >&2
        echo "add the section, dated, before tagging" >&2
    fi
    exit 4
fi

# Its entries: from that exact heading line to the next top-level
# heading, the heading itself excluded.
section="$(awk -v want="$heading" '
    $0 == want { collecting = 1; next }
    collecting && /^## / { exit }
    collecting { print }
' "$tree/CHANGELOG.md")"

# Blank lines only is the same as absent: a release must say what
# changed.
if [ -z "$(printf '%s' "$section" | tr -d ' \t\r\n')" ]; then
    echo "CHANGELOG.md has no entries under '$heading'" >&2
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
