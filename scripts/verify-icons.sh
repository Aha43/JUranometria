#!/bin/sh
# The committed application-icon containers are exactly what the
# chosen geometry produces (issue #202).
#
#   scripts/verify-icons.sh [classpath]
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
icons="$root/packaging/icon"

# The application JAR by preference, because that is what the image
# is built from and therefore what ships: verifying against it asks
# whether the containers match the geometry a reader will actually
# run. The classes directory is the fallback for a working tree that
# has not been packaged yet. (The image workflow consumes a
# prebuilt JAR and never compiles, which is how this was found.)
if [ -n "${1:-}" ]; then
    classpath="$1"
elif [ -f "$root/build/app/JUranometria.jar" ]; then
    classpath="$root/build/app/JUranometria.jar"
elif [ -d "$root/build/classes" ]; then
    classpath="$root/build/classes"
else
    echo "verify-icons: nothing to regenerate the mark from -" \
         "no build/app/JUranometria.jar and no build/classes" >&2
    echo "run 'make jar' or 'make classes' first" >&2
    exit 5
fi

fresh="$(mktemp -d)"
trap 'rm -rf "$fresh"' EXIT

java -cp "$classpath" juranometria.tool.ApplicationIconMain "$fresh" \
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
