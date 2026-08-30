#!/bin/sh
# Builds the self-contained JUranometria application image for the
# CURRENT operating system and architecture (issue #150): jpackage
# --type app-image over the built JAR and its pinned libraries, with
# an explicit jlink module list measured by jdeps
# (java.base,java.desktop,java.logging,java.prefs), the launcher
# carrying --enable-native-access, and the runtime's legal/ notices
# kept. Never cross-packages: build on the target platform.
#
# Usage: scripts/build-app-image.sh [dest-dir]   (default build/app-image)
set -eu
here=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
root=$(dirname "$here")
dest=${1:-"$root/build/app-image"}
version=$(cat "$root/VERSION")

# jpackage on macOS refuses an app-version whose first number is 0;
# prototypes before 1.0.0 carry a placeholder there, recorded in the
# build info. The 1.0 release itself is unaffected.
appversion=$version
case "$version" in 0*) appversion=1.0.0 ;; esac

jar_dir="$root/build/app"
[ -f "$jar_dir/JUranometria.jar" ] || {
    echo "build-app-image: run 'make app' first" >&2; exit 1; }

modules=java.base,java.desktop,java.logging,java.prefs

rm -rf "$dest"
mkdir -p "$dest"

icon_arg=""
case "$(uname -s)" in
    Darwin) [ -f "$root/packaging/icon/JUranometria.icns" ] \
        && icon_arg="--icon $root/packaging/icon/JUranometria.icns" ;;
    Linux)  [ -f "$root/packaging/icon/JUranometria-512.png" ] \
        && icon_arg="--icon $root/packaging/icon/JUranometria-512.png" ;;
    *)      [ -f "$root/packaging/icon/JUranometria.ico" ] \
        && icon_arg="--icon $root/packaging/icon/JUranometria.ico" ;;
esac

# shellcheck disable=SC2086
jpackage \
    --type app-image \
    --name JUranometria \
    --app-version "$appversion" \
    --vendor "JUranometria Contributors" \
    --input "$jar_dir" \
    --main-jar JUranometria.jar \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    --add-modules "$modules" \
    --add-launcher juranometria-smoke="$root/packaging/smoke-launcher.properties" \
    $icon_arg \
    --dest "$dest"

# Locate the image root and its bundled runtime release file.
case "$(uname -s)" in
    Darwin)
        image="$dest/JUranometria.app"
        release="$image/Contents/runtime/Contents/Home/release"
        legal="$image/Contents/runtime/Contents/Home/legal"
        launcher="$image/Contents/MacOS/JUranometria"
        ;;
    *)
        image="$dest/JUranometria"
        # Layouts differ: Linux keeps the runtime under lib/runtime,
        # Windows under runtime/. Probe rather than assume.
        release=""
        for candidate in "$image/runtime/release" \
                "$image/lib/runtime/release" \
                "$image/runtime/lib/release"; do
            [ -f "$candidate" ] && release=$candidate && break
        done
        [ -n "$release" ] || {
            echo "build-app-image: no runtime release file under $image" >&2
            find "$image" -maxdepth 3 -name release >&2 || true
            exit 1
        }
        legal=$(dirname "$release")/legal
        [ -d "$legal" ] || legal=$(dirname "$release")/lib/legal
        launcher="$image/bin/JUranometria"
        [ -f "$launcher" ] || launcher="$image/JUranometria.exe"
        ;;
esac

# Record the pinned runtime and inputs beside the image.
{
    echo "JUranometria $version (app-image; jpackage version label $appversion)"
    echo "built: from checked source, modules: $modules"
    echo "packager: $(jpackage --version) ($(uname -s) $(uname -m))"
    echo "runtime release:"
    sed -e 's/^/  /' "$release"
} > "$dest/build-info.txt"
cp "$root/packaging/README-app-image.txt" "$dest/README.txt"

# Assertions: the runtime is the trimmed module set and its legal
# notices travel with the image.
# jlink resolves the transitive closure: desktop pulls datatransfer
# and xml. Six modules total is the measured, asserted inventory.
grep -q 'MODULES="java.base java.datatransfer java.xml java.prefs java.desktop java.logging"' \
    "$release" || {
    echo "build-app-image: unexpected module set in $release" >&2
    grep MODULES "$release" >&2 || true
    exit 1
}
[ -d "$legal/java.base" ] || {
    echo "build-app-image: runtime legal notices missing at $legal" >&2
    exit 1
}
[ -x "$launcher" ] || {
    echo "build-app-image: launcher missing at $launcher" >&2
    exit 1
}

# The packaged headless smoke path through the NATIVE image: the
# additional juranometria-smoke launcher renders the reference chart
# with the bundled runtime alone.
case "$(uname -s)" in
    Darwin) smoke="$image/Contents/MacOS/juranometria-smoke" ;;
    *) smoke="$image/bin/juranometria-smoke"
       [ -e "$smoke" ] || smoke="$image/juranometria-smoke.exe" ;;
esac
out="$dest/native-smoke.png"
env PATH=/nonexistent "$smoke" "$out" >/dev/null 2>&1 \
    || "$smoke" "$out" >/dev/null
size=$(wc -c < "$out")
[ "$size" -gt 10000 ] || {
    echo "build-app-image: native smoke render failed ($size bytes)" >&2
    exit 1
}

echo "app-image: $image"
echo "unpacked size: $(du -sh "$image" | cut -f1)"
echo "native headless smoke render: $size bytes"
