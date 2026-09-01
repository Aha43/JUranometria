#!/bin/sh
# Is an existing release for this tag another delivery of the same
# push, or a genuine conflict? (issue #195)
#
#   scripts/release-duplicate.sh <version> <staging> <published>
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
# WHAT CAN AND CANNOT BE COMPARED, and why it is not the obvious
# thing. The four native application images are NOT byte-reproducible
# across runners - jpackage does not promise it, and the two v1.3.0
# runs proved it: same tag, same commit, four different checksums
# (macos-arm64 26624cff... against the published 205508c9...).
# Requiring the whole set to match would therefore condemn every
# benign duplicate as a conflict, which is the bug this fixes.
#
# The portable archive is different, and deliberately so: `make dist`
# stamps one fixed modification time, sorts the entries, and zips
# with -X, so it IS reproducible from the same source on any machine
# - and the same two runs produced the identical 473fcbb1... for it.
# That makes it the identity check. A published release whose
# portable archive hashes to what this run built was built from this
# run's source; one that does not, was not.
#
# This is the narrower honest claim the 1.0 contract already makes:
# byte-identity reported where it holds rather than promised
# universally. What was compared is printed, and so is what was not.
set -eu

version="${1:-}"
staging="${2:-}"
published="${3:-}"
if [ -z "$version" ] || [ -z "$staging" ] || [ -z "$published" ]; then
    echo "usage: $0 <version> <staging> <published>" >&2
    exit 64
fi

archives="macos-arm64 macos-x64 windows-x64 linux-x64 portable"

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
# at both ends: a line for "...-portable.zip" must never answer for
# "...-portable.zip.sig", and a manifest naming a file twice is a
# malformed manifest rather than a match.
stated_in() {
    manifest="$1"
    wanted="$2"
    found="$(awk -v want="$wanted" \
        '$2 == want { print $1; n++ } END { if (n != 1) exit 1 }' \
        "$manifest" 2>/dev/null || true)"
    echo "$found"
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

# --- this run's own side, which must be sound before anything is
# --- compared against it
mine="$staging/SHA256SUMS.txt"
if [ ! -s "$mine" ]; then
    echo "release-duplicate: this run staged no $mine" >&2
    exit 9
fi
portable_name="$(name_for portable)"
mine_portable="$(stated_in "$mine" "$portable_name")"
if [ -z "$mine_portable" ]; then
    echo "release-duplicate: this run's checksums do not name" \
         "$portable_name exactly once" >&2
    exit 9
fi

# --- the published side, read back rather than assumed
assets="$published/assets.txt"
if [ ! -f "$assets" ]; then
    echo "release-duplicate: no published asset list at $assets" >&2
    exit 9
fi

# Exactly the six the contract publishes - no more, no fewer. An
# extra asset means someone attached something this run did not
# build, and a missing one means the published release is incomplete.
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
if [ ! -s "$theirs" ]; then
    conflict "its SHA256SUMS.txt could not be read back."
fi

# The published manifest must account for the same five archives.
# Comparing the names before the checksums means a manifest for a
# different version is reported as what it is.
for cell in $archives; do
    if [ -z "$(stated_in "$theirs" "$(name_for "$cell")")" ]; then
        conflict "its SHA256SUMS.txt does not name" \
            "$(name_for "$cell") exactly once."
    fi
done

# The identity check, on the one archive that is reproducible by
# construction. Hashed here from the bytes GitHub served, so a
# published manifest that disagrees with its own published archive
# is caught too - the release is checked, not merely quoted.
their_file="$published/$portable_name"
if [ ! -s "$their_file" ]; then
    conflict "its $portable_name could not be downloaded."
fi
their_portable="$(sha256_of "$their_file")"
their_stated="$(stated_in "$theirs" "$portable_name")"
if [ "$their_portable" != "$their_stated" ]; then
    conflict "$(printf 'the published %s does not match the published
SHA256SUMS.txt: the file hashes to %s, the manifest says %s.' \
        "$portable_name" "$their_portable" "$their_stated")"
fi
if [ "$their_portable" != "$mine_portable" ]; then
    conflict "$(printf 'it was built from different source. The
reproducible %s hashes to %s there and %s here.' \
        "$portable_name" "$their_portable" "$mine_portable")"
fi

# Benign. Say exactly what that rests on, including its limit.
echo "This tag is already released, by another delivery of the same"
echo "push, and that release is this run's own work:"
echo
echo "  the published release holds exactly the six contract assets"
echo "  its SHA256SUMS.txt names all five archives"
echo "  the reproducible portable archive matches this run exactly"
echo "    $portable_name"
echo "    $their_portable"
echo
echo "The four native application images are NOT compared: jpackage"
echo "does not build them byte-identically across runners, so a"
echo "difference there would prove nothing. The portable archive is"
echo "reproducible by construction and carries the identity claim."
echo
echo "Nothing is uploaded, replaced or deleted. There is nothing to"
echo "do, and that is not a failure."
exit 0
