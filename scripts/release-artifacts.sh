#!/bin/sh
# The release artifact gate (issue #88): exactly the five archives
# the 1.0 contract publishes, each carrying the version it claims,
# with their checksums written last.
#
#   scripts/release-artifacts.sh <version> <directory>
#
# Nothing is published unless this passes, so a missing cell, a
# stray file, or an archive built from the wrong tree stops the
# release instead of shipping in it.
#
#   0  the five artifacts are present, correctly named, and agree
#   6  an artifact is missing, unexpected, or empty
#   7  an artifact's contents do not carry this version
set -eu

version="${1:-}"
directory="${2:-}"
if [ -z "$version" ] || [ -z "$directory" ]; then
    echo "usage: $0 <version> <directory>" >&2
    exit 64
fi
cd "$directory"

expected="JUranometria-$version-macos-arm64.zip
JUranometria-$version-macos-x64.zip
JUranometria-$version-windows-x64.zip
JUranometria-$version-linux-x64.zip
JUranometria-$version-portable.zip"

for artifact in $expected; do
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
    case "$expected" in
        *"$present"*) ;;
        *)
            echo "unexpected file in the release set: $present" >&2
            echo "only the contract's five archives are published" >&2
            exit 6
            ;;
    esac
done

# Each archive must carry the version it is named for - an image
# through its build-info.txt, the portable archive through the
# directory the reader unpacks.
for image in macos-arm64 macos-x64 windows-x64 linux-x64; do
    artifact="JUranometria-$version-$image.zip"
    if ! unzip -p "$artifact" '*build-info.txt' 2>/dev/null \
            | grep -q "JUranometria $version"; then
        echo "$artifact does not carry version $version in its" \
             "build-info.txt" >&2
        exit 7
    fi
done
if ! unzip -l "JUranometria-$version-portable.zip" \
        | grep -q "JUranometria-$version/"; then
    echo "JUranometria-$version-portable.zip does not unpack to" \
         "JUranometria-$version/" >&2
    exit 7
fi

# Written last, over the exact set just verified.
rm -f SHA256SUMS.txt
if command -v sha256sum > /dev/null 2>&1; then
    sha256sum $expected > SHA256SUMS.txt
else
    shasum -a 256 $expected > SHA256SUMS.txt
fi

echo "five artifacts verified for $version, checksums written:"
cat SHA256SUMS.txt
