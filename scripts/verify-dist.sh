#!/bin/sh
# Asserts the release archive's exact contents against the contract
# (docs/decisions/one-point-zero-contract.md): every expected file
# present, nothing undeclared - a stale or extra file fails. Then
# smoke-checks that the packaged application renders the reference
# chart headlessly through its own Class-Path, from a directory whose
# path contains a space.
set -eu
zip_file=$1
. "$(dirname "$0")/lib-versions.env"
name=JUranometria-$(cat "$(dirname "$0")/../VERSION")

work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
target="$work/with space"
mkdir -p "$target"
unzip -q "$zip_file" -d "$target"

expected="$work/expected"
cat > "$expected" <<LIST
$name
$name/JUranometria.jar
$name/LICENSE
$name/LICENSING.md
$name/README.txt
$name/juranometria
$name/juranometria.bat
$name/lib
$name/lib/flatlaf-$FLATLAF_VERSION.jar
$name/lib/flatlaf-extras-$FLATLAF_VERSION.jar
$name/lib/jsvg-$JSVG_VERSION.jar
$name/licenses
$name/licenses/LICENSE-Apache-2.0.txt
$name/licenses/LICENSE-BSD-3-Clause.txt
$name/licenses/LICENSE-CC-BY-SA-4.0.txt
$name/licenses/LICENSE-JSVG-MIT.txt
$name/licenses/LICENSE-Tabler-MIT.txt
$name/licenses/NOTICE-constellations.md
$name/licenses/NOTICE-openngc.md
$name/licenses/NOTICE-runtime-libraries.md
$name/licenses/NOTICE-star-identities.md
$name/licenses/NOTICE-tycho2.md
LIST
actual="$work/actual"
(cd "$target" && find "$name" | LC_ALL=C sort) > "$actual"
if ! diff -u "$expected" "$actual"; then
    echo "verify-dist: archive contents differ from the contract" >&2
    exit 1
fi

# Every relative markdown link in the archive's licensing map must
# resolve inside the archive - no dead source-tree links (PR #149).
links=$(grep -oE '\]\(([^)#]+)\)' "$target/$name/LICENSING.md" \
        | sed -E 's/^\]\(//; s/\)$//' | sort -u)
for link in $links; do
    case "$link" in http*|mailto*) continue ;; esac
    if [ ! -e "$target/$name/$link" ]; then
        echo "verify-dist: LICENSING.md links to a missing file: $link" >&2
        exit 1
    fi
done

grep -q "non-commercially only" "$target/$name/LICENSING.md" || {
    echo "verify-dist: LICENSING.md lacks the non-commercial statement" >&2
    exit 1
}
grep -q "NON-COMMERCIAL" "$target/$name/README.txt" || {
    echo "verify-dist: the non-commercial statement is missing" >&2
    exit 1
}

# The packaged smoke test: the application classes run from the
# shipped JAR (Class-Path manifest resolving lib/ beside it),
# rendering the reference chart headlessly - deterministic proof the
# archive is complete and launchable without a display.
chart="$work/smoke.png"
(cd "$target/$name" && java -Djava.awt.headless=true \
    -cp JUranometria.jar juranometria.app.ChartImageMain "$chart" \
    >/dev/null)
if [ ! -s "$chart" ]; then
    echo "verify-dist: packaged smoke render produced no chart" >&2
    exit 1
fi
size=$(wc -c < "$chart")
if [ "$size" -lt 10000 ]; then
    echo "verify-dist: packaged smoke render suspiciously small ($size bytes)" >&2
    exit 1
fi
echo "verify-dist: contents exact, non-commercial notice present,"
echo "verify-dist: packaged headless smoke render OK ($size bytes, path with space)"
