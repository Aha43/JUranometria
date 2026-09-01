#!/bin/sh
# Is an existing release for this tag another delivery of the same
# push, or a genuine conflict? (issue #195)
#
#   scripts/release-duplicate.sh <version> <staging> <published> \
#                                <commit>
#
# GitHub delivered the v1.3.0 tag push twice, one second apart. The
# concurrency guard serialised the two runs, the first published, and
# the second refused - correctly, under the #88 rule that a release
# people may already have downloaded is never silently replaced. But
# the refusal left a red X on a release that was entirely correct,
# and red that means nothing is expensive on the day it means
# something.
#
# So the refusal is split in two. This script reads the published
# release back and decides which case it is:
#
#   0  BENIGN - the published release is this same delivery. The
#      caller publishes nothing; it is already there.
#   8  CONFLICT - a different release holds this tag. The caller
#      fails, loudly, exactly as before.
#   9  this run's own staging is not what it should be (a bug here,
#      not a conflict there)
#  64  usage
#
# WHAT IS COMPARED, and why it is not the obvious thing. The four
# native application images are NOT byte-reproducible across runners
# - jpackage does not promise it, and the two v1.3.0 runs proved it:
# same tag, same commit, four different checksums (macos-arm64
# 26624cff... against the published 205508c9...). Requiring the whole
# set to match byte for byte would condemn every benign duplicate as
# a conflict, which is the bug this fixes.
#
# Three checks stand in its place, and together they cover what a
# byte comparison would have:
#
#  1. EVERY published archive is downloaded and hashed here, and must
#     match the published SHA256SUMS.txt. A truncated or substituted
#     archive keeps its name and its manifest line; it cannot keep
#     its bytes (#195 review).
#  2. Every application image must state THIS COMMIT in its own
#     build-info.txt. That is the immutable source-commit identity:
#     it moves when the packaging scripts or the bundled runtime move,
#     neither of which an identical portable archive would notice
#     (#195 review).
#  3. The portable archive must hash to exactly what this run built.
#     `make dist` stamps one fixed modification time, sorts the
#     entries and zips with -X, so it IS reproducible from the same
#     source on any machine - the two v1.3.0 runs produced the
#     identical 473fcbb1... for it, and the #195 rehearsal built that
#     same archive again on another branch on another day.
#
# This run's own staged bytes are hashed too, rather than its
# manifest being taken at its word, so exit 9 means the local set is
# genuinely broken rather than merely inconsistent on paper.
#
# What is compared is printed, and so is what is not.
set -eu

version="${1:-}"
staging="${2:-}"
published="${3:-}"
commit="${4:-}"
if [ -z "$version" ] || [ -z "$staging" ] || [ -z "$published" ] \
        || [ -z "$commit" ]; then
    echo "usage: $0 <version> <staging> <published> <commit>" >&2
    exit 64
fi

natives="macos-arm64 macos-x64 windows-x64 linux-x64"
archives="$natives portable"

name_for() {
    echo "JUranometria-$version-$1.zip"
}

sha256_of() {
    if command -v sha256sum > /dev/null 2>&1; then
        sha256sum "$1" | cut -d' ' -f1
    else
        shasum -a 256 "$1" | cut -d' ' -f1
    fi
}

# The checksum a SHA256SUMS.txt states for one exact name. Anchored
# on the whole field: a line for "...-portable.zip" must never answer
# for "...-portable.zip.sig", and a manifest naming a file twice is a
# malformed manifest rather than a match.
stated_in() {
    awk -v want="$2" \
        '$2 == want { print $1; n++ } END { if (n != 1) exit 1 }' \
        "$1" 2>/dev/null || true
}

# The commit an application image records for itself. Listing the
# entries first is deliberate, exactly as release-artifacts.sh does
# it: reading every match at once would let a decoy build-info.txt
# anywhere in the archive answer on behalf of the real one.
source_commit_of() {
    entries=$(unzip -Z1 "$1" 2>/dev/null \
        | grep -E '(^|/)build-info\.txt$' || true)
    if [ "$(printf '%s' "$entries" | grep -c . || true)" != "1" ]; then
        echo "multiple-or-missing-build-info"
        return 0
    fi
    unzip -p "$1" "$entries" 2>/dev/null \
        | awk '$1 == "source:" { print $2; found = 1; exit }
               END { if (!found) print "no-source-line"; }'
}

conflict() {
    echo "::error::a DIFFERENT release already exists for" \
         "JUranometria $version." >&2
    echo "$1" >&2
    echo "Nothing was uploaded, replaced or deleted. Inspect that" >&2
    echo "release, then either delete it deliberately and re-run," >&2
    echo "or release a new version. This run's verified artifacts" >&2
    echo "remain attached to it as workflow artifacts." >&2
    exit 8
}

broken() {
    echo "release-duplicate: this run's own staging is wrong:" >&2
    echo "  $1" >&2
    echo "Nothing was compared against the published release." >&2
    exit 9
}

# --- this run's own side, hashed rather than believed. Deciding
# --- whether a published release matches a manifest whose own files
# --- have moved would be answering the wrong question.
mine="$staging/SHA256SUMS.txt"
[ -s "$mine" ] || broken "no checksums at $mine"
for cell in $archives; do
    file="$staging/$(name_for "$cell")"
    [ -s "$file" ] || broken "staged no $(name_for "$cell")"
    stated=$(stated_in "$mine" "$(name_for "$cell")")
    [ -n "$stated" ] \
        || broken "its checksums do not name $(name_for "$cell") once"
    actual=$(sha256_of "$file")
    if [ "$actual" != "$stated" ]; then
        broken "$(printf '%s hashes to %s, its own manifest says %s' \
            "$(name_for "$cell")" "$actual" "$stated")"
    fi
done
mine_portable=$(stated_in "$mine" "$(name_for portable)")

# --- the published side, read back rather than assumed
assets="$published/assets.txt"
[ -f "$assets" ] || broken "no published asset list at $assets"

# Exactly the six the contract publishes - no more, no fewer. An
# extra asset means someone attached something this run did not
# build; a missing one means the published release is incomplete.
# Both are for a person to look at.
{
    for cell in $archives; do name_for "$cell"; done
    echo "SHA256SUMS.txt"
} | LC_ALL=C sort > "$published/expected-assets.txt"
LC_ALL=C sort "$assets" > "$published/actual-assets.txt"
if ! cmp -s "$published/expected-assets.txt" \
            "$published/actual-assets.txt"; then
    conflict "$(printf 'its assets are not the six this run built:\n%s' \
        "$(diff "$published/expected-assets.txt" \
                "$published/actual-assets.txt" || true)")"
fi

theirs="$published/SHA256SUMS.txt"
[ -s "$theirs" ] || conflict "its SHA256SUMS.txt could not be read back."

# 1. Every published archive, hashed from the bytes GitHub served,
#    so truncation and substitution are caught rather than assumed
#    away by a matching name.
for cell in $archives; do
    file="$published/$(name_for "$cell")"
    if [ ! -s "$file" ]; then
        conflict "its $(name_for "$cell") could not be downloaded."
    fi
    stated=$(stated_in "$theirs" "$(name_for "$cell")")
    if [ -z "$stated" ]; then
        conflict "its SHA256SUMS.txt does not name $(name_for "$cell")
exactly once."
    fi
    actual=$(sha256_of "$file")
    if [ "$actual" != "$stated" ]; then
        conflict "$(printf 'the published %s does not match the published
SHA256SUMS.txt: the file hashes to %s, the manifest says %s.' \
            "$(name_for "$cell")" "$actual" "$stated")"
    fi
done

# 2. The source-commit identity, stated by each image about itself.
for cell in $natives; do
    stated=$(source_commit_of "$published/$(name_for "$cell")")
    if [ "$stated" != "$commit" ]; then
        conflict "$(printf 'its %s was built from a different source
commit: build-info.txt says %s, this run is publishing %s.' \
            "$(name_for "$cell")" "$stated" "$commit")"
    fi
done

# 3. The archive that is reproducible by construction must be this
#    run's own bytes.
their_portable=$(sha256_of "$published/$(name_for portable)")
if [ "$their_portable" != "$mine_portable" ]; then
    conflict "$(printf 'it was built from different source. The
reproducible %s hashes to %s there and %s here.' \
        "$(name_for portable)" "$their_portable" "$mine_portable")"
fi

# Benign. Say exactly what that rests on, including its limit.
echo "This tag is already released, by another delivery of the same"
echo "push, and that release is this run's own work:"
echo
echo "  it holds exactly the six contract assets"
echo "  all five archives were downloaded and hash to what its own"
echo "    SHA256SUMS.txt states"
echo "  all four application images record this exact source commit"
echo "    source: $commit"
echo "  the reproducible portable archive matches this run exactly"
echo "    $their_portable"
echo
echo "The application images are NOT compared byte for byte: jpackage"
echo "does not build them identically across runners, so a difference"
echo "there would prove nothing. Their recorded source commit and its"
echo "own published checksums carry the claim instead."
echo
echo "Nothing is uploaded, replaced or deleted. There is nothing to"
echo "do, and that is not a failure."
exit 0
