#!/bin/sh
# The committed application-icon containers are exactly what the
# chosen geometry produces (issue #202).
#
#   scripts/verify-icons.sh [classes-dir]
#
# Checking that an icon file EXISTS catches a deletion and nothing
# else. It was measured: replacing JUranometria.icns with eight bytes
# of rubbish built a complete, passing application image, because
# jpackage copies the container verbatim and every other check
# compared that copy against the same rubbish. A substituted design,
# a truncated file and a missing size all looked identical to a
# correct one.
#
# So the containers are regenerated here from
# juranometria.app.ApplicationMark - the geometry the gate chose -
# and compared byte for byte with what is committed. That is one
# question with one answer: is what we ship still the mark that was
# reviewed?
#
#   0  every container matches the geometry
#   5  a container is missing, altered, or no longer what the
#      geometry draws
set -eu

root="$(dirname "$0")/.."
classes="${1:-$root/build/classes}"
icons="$root/packaging/icon"

if [ ! -d "$classes" ]; then
    echo "verify-icons: no compiled classes at $classes" >&2
    echo "run 'make classes' first" >&2
    exit 5
fi

fresh="$(mktemp -d)"
trap 'rm -rf "$fresh"' EXIT

java -cp "$classes" juranometria.tool.ApplicationIconMain "$fresh" \
    > /dev/null || {
    echo "verify-icons: the generator would not run" >&2
    exit 5
}

status=0
for produced in "$fresh"/*; do
    name="$(basename "$produced")"
    committed="$icons/$name"
    if [ ! -f "$committed" ]; then
        echo "verify-icons: $name is missing from packaging/icon" >&2
        status=5
        continue
    fi
    if ! cmp -s "$produced" "$committed"; then
        echo "verify-icons: $name is not what the geometry draws" >&2
        echo "  committed: $(wc -c < "$committed") bytes" >&2
        echo "  geometry:  $(wc -c < "$produced") bytes" >&2
        status=5
    fi
done

# Nothing may ride along uninspected either: a stray container in
# packaging/icon is one nobody regenerates and nobody reviews.
for committed in "$icons"/JUranometria*; do
    name="$(basename "$committed")"
    if [ ! -f "$fresh/$name" ]; then
        echo "verify-icons: $name is committed but the generator" \
             "does not produce it" >&2
        status=5
    fi
done

if [ "$status" -eq 0 ]; then
    echo "icons: $(ls "$fresh" | wc -l | tr -d ' ') containers, each" \
         "identical to what the chosen geometry draws"
fi
exit "$status"
