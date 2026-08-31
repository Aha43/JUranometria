#!/bin/sh
# Compares two builds of the same native application image and
# reports the result honestly (issue #158): byte-identical is the
# expected outcome and is reported as such; genuine file differences
# are COUNTED AND REPORTED without failing, per the reproducibility
# contract in docs/decisions/one-point-zero-contract.md; and a real
# error from diff (status above 1 - unreadable path, missing
# directory) remains a failure.
#
# Usage: scripts/compare-app-images.sh DIR_A DIR_B [SUMMARY_FILE]
# Prints a one-line verdict; writes the same verdict plus
# representative differing entries to SUMMARY_FILE when given.
set -eu
a=$1
b=$2
summary=${3:-}
representatives=10

for dir in "$a" "$b"; do
    if [ ! -d "$dir" ]; then
        echo "compare-app-images: not a directory: $dir" >&2
        exit 1
    fi
done

# diff exits 0 identical, 1 different, >1 on a real error. Capture
# all three without the caller's set -e turning "different" - an
# outcome this script exists to report - into an abort.
set +e
report=$(diff -rq "$a" "$b" 2>&1)
status=$?
set -e

if [ "$status" -gt 1 ]; then
    echo "compare-app-images: diff failed (status $status):" >&2
    echo "$report" >&2
    exit 1
fi

if [ "$status" -eq 0 ]; then
    verdict="reproducible: the two builds are byte-identical"
    echo "$verdict"
    [ -n "$summary" ] && echo "$verdict" >> "$summary"
    exit 0
fi

count=$(printf '%s\n' "$report" | grep -c .)
verdict="reproducible: NO - $count differing entries (recorded; the release contract asserts contents, pinned inputs, and published SHA-256 rather than byte identity)"
echo "$verdict"
printf '%s\n' "$report" | head -n "$representatives" | sed 's/^/  /'
if [ -n "$summary" ]; then
    {
        echo "$verdict"
        echo '```'
        printf '%s\n' "$report" | head -n "$representatives"
        echo '```'
    } >> "$summary"
fi
exit 0
