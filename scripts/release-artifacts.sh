#!/bin/sh
# The release artifact gate (issue #88): exactly the five archives
# the 1.0 contract publishes, each carrying the version it claims,
# with their checksums written last.
#
#   scripts/release-artifacts.sh <version> <directory> <commit>
#
# Nothing is published unless this passes, so a missing cell, a
# stray file, or an archive built from the wrong tree stops the
# release instead of shipping in it.
#
# Every comparison here is LITERAL and EXACT (automation review):
# a check that accepts "1.2.30" for 1.2.3, or a stray file because
# its name happens to be a substring of a real one, is not a check.
#
# Each image must also record THAT SOURCE COMMIT in its own
# build-info.txt (#195 review). The duplicate-delivery guard compares
# published releases by that commit, so it is part of the release
# contract - and the argument is REQUIRED rather than optional,
# because a check that can be skipped by omitting an argument is a
# check that will eventually be skipped (#195 follow-up review).
#
#   0  the five artifacts are present, correctly named, and agree
#   6  an artifact is missing, unexpected, or empty
#   7  an artifact's contents do not carry this version or commit
set -eu

version="${1:-}"
directory="${2:-}"
commit="${3:-}"
if [ -z "$version" ] || [ -z "$directory" ] || [ -z "$commit" ]; then
    echo "usage: $0 <version> <directory> <commit>" >&2
    exit 64
fi
cd "$directory"

cells="macos-arm64 macos-x64 windows-x64 linux-x64 portable"

expected_name() {
    echo "JUranometria-$version-$1.zip"
}

# Exact membership, never substring: is $1 one of the five names?
is_expected() {
    for cell in $cells; do
        if [ "$1" = "$(expected_name "$cell")" ]; then
            return 0
        fi
    done
    return 1
}

for cell in $cells; do
    artifact="$(expected_name "$cell")"
    if [ ! -f "$artifact" ]; then
        echo "missing release artifact: $artifact" >&2
        exit 6
    fi
    if [ ! -s "$artifact" ]; then
        echo "empty release artifact: $artifact" >&2
        exit 6
    fi
done

# Nothing may ride along uninspected: the published set is exactly
# the contract's five, plus the checksums this script writes.
for present in *.zip; do
    if ! is_expected "$present"; then
        echo "unexpected file in the release set: $present" >&2
        echo "only the contract's five archives are published" >&2
        exit 6
    fi
done

# Each image must carry the version it is named for, in ITS OWN
# build-info.txt. Listing the entries first is deliberate: reading
# every match at once would let a decoy build-info.txt anywhere in
# the archive satisfy the check on behalf of the real one.
for cell in macos-arm64 macos-x64 windows-x64 linux-x64; do
    artifact="$(expected_name "$cell")"
    entries="$(unzip -Z1 "$artifact" 2>/dev/null \
        | grep -E '(^|/)build-info\.txt$' || true)"
    count="$(printf '%s' "$entries" | grep -c . || true)"
    if [ "$count" != "1" ]; then
        echo "$artifact carries $count build-info.txt files;" \
             "exactly one identifies an image" >&2
        printf '%s\n' "$entries" >&2
        exit 7
    fi
    # The first line reads "JUranometria <version> (app-image; ...)";
    # take field 2 and compare it literally.
    stated="$(unzip -p "$artifact" "$entries" | head -1 | awk '{print $2}')"
    if [ "$stated" != "$version" ]; then
        echo "$artifact states version '$stated', not '$version'" >&2
        exit 7
    fi
    # Read from the same single entry, for the same reason.
    built="$(unzip -p "$artifact" "$entries" \
        | awk '$1 == "source:" { print $2; found = 1; exit }
               END { if (!found) print "absent"; }')"
    if [ "$built" != "$commit" ]; then
        echo "$artifact was built from source '$built'," \
             "not '$commit'" >&2
        exit 7
    fi
done

# The portable archive is identified by the directory the reader
# unpacks: an exact top-level entry, not a name that merely starts
# the same way.
portable="$(expected_name portable)"
if ! unzip -Z1 "$portable" 2>/dev/null \
        | grep -qxF "JUranometria-$version/"; then
    echo "$portable does not unpack to JUranometria-$version/" >&2
    unzip -Z1 "$portable" 2>/dev/null | head -3 >&2
    exit 7
fi

# Written last, over the exact set just verified.
rm -f SHA256SUMS.txt
set --
for cell in $cells; do
    set -- "$@" "$(expected_name "$cell")"
done
if command -v sha256sum > /dev/null 2>&1; then
    sha256sum "$@" > SHA256SUMS.txt
else
    shasum -a 256 "$@" > SHA256SUMS.txt
fi

echo "five artifacts verified for $version, checksums written:"
cat SHA256SUMS.txt
