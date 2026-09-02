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

# The application mark this platform installs (issue #202). A
# MISSING icon is a failure, not a silent fallback: the whole point
# of the mark is that the application does not arrive wearing Java's
# default cup, and an image that quietly shipped without one would
# pass every other check here.
case "$(uname -s)" in
    Darwin) icon="$root/packaging/icon/JUranometria.icns" ;;
    Linux)  icon="$root/packaging/icon/JUranometria-512.png" ;;
    *)      icon="$root/packaging/icon/JUranometria.ico" ;;
esac
[ -f "$icon" ] || {
    echo "build-app-image: no application icon at $icon" >&2
    echo "run 'make icons' to write it from the chosen geometry" >&2
    exit 1
}
# Present is not enough. jpackage copies a container verbatim, so
# eight bytes of rubbish named JUranometria.icns builds a complete
# image with a broken icon - measured, not imagined. This asks the
# only question that catches that: is what we ship still the mark
# that was reviewed?
"$root/scripts/verify-icons.sh" "$jar_dir/JUranometria.jar"
icon_arg="--icon $icon"

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
    --add-launcher juranometria-acceptance="$root/packaging/acceptance-launcher.properties" \
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

# The exact source commit this image was packaged from (#195
# review): the release contract's source-commit identity.
# An identical portable archive proves the application code and its
# bundled libraries match; it says nothing about the packaging
# scripts or the runtime jlink trimmed, which can both change while
# that archive stays byte-identical. The commit moves when any of
# them does, so it - not a hash of one artifact - is what ties a
# published release to a tree. Unknown when built outside a
# checkout, which fails the release comparison closed.
#
# On a TAG PUSH - the only event that publishes - GITHUB_SHA is the
# tag's own commit, the same value release-provenance.sh checks
# against the annotated tag and origin/main. On a pull request it is
# the ephemeral merge commit instead, so an image built by PR CI
# names a commit that is on no branch. That is correct for what it
# is: the tree that was actually packaged.
commit="${GITHUB_SHA:-$(git -C "$root" rev-parse HEAD 2>/dev/null \
    || echo unknown)}"

# Record the pinned runtime and inputs beside the image.
{
    echo "JUranometria $version (app-image; jpackage version label $appversion)"
    echo "source: $commit"
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

# Mechanical licensing inventory (issue #150): the packaged JAR
# inside the image must carry the complete About inventory - the
# licensing summary, every data notice and licence text, and the
# icon licence - and the trimmed runtime must carry a legal/
# directory for every module it contains.
appjar=""
for candidate in "$image/Contents/app/JUranometria.jar" \
        "$image/app/JUranometria.jar" \
        "$image/lib/app/JUranometria.jar"; do
    [ -f "$candidate" ] && appjar=$candidate && break
done
[ -n "$appjar" ] || {
    echo "build-app-image: packaged JAR not found in image" >&2
    exit 1
}
jarlist=$(unzip -l "$appjar")
for resource in \
        resources/about/licensing-summary.txt \
        resources/catalog/bright-sky/NOTICE-tycho2.md \
        resources/catalog/bright-sky/NOTICE-openngc.md \
        resources/catalog/bright-sky/LICENSE-CC-BY-SA-4.0.txt \
        resources/geo/constellations/NOTICE-constellations.md \
        resources/geo/constellations/LICENSE-BSD-3-Clause.txt \
        resources/catalog/star-identities/NOTICE-star-identities.md \
        resources/catalog/star-identities/LICENSE-BSD-3-Clause.txt \
        resources/icons/LICENSE; do
    echo "$jarlist" | grep -q " $resource\$" || {
        echo "build-app-image: packaged licensing inventory missing" \
             "$resource" >&2
        exit 1
    }
done
for module in java.base java.datatransfer java.xml java.prefs \
        java.desktop java.logging; do
    [ -d "$legal/$module" ] || {
        echo "build-app-image: runtime legal notices missing for" \
             "$module" >&2
        exit 1
    }
done
echo "packaged licensing inventory: complete (9 resources, 6 module"
echo "legal directories)"

# The packaged headless smoke path through the NATIVE image: the
# additional juranometria-smoke launcher renders the reference chart
# with the bundled runtime alone.
case "$(uname -s)" in
    Darwin) smoke="$image/Contents/MacOS/juranometria-smoke" ;;
    *) smoke="$image/bin/juranometria-smoke"
       [ -e "$smoke" ] || smoke="$image/juranometria-smoke.exe" ;;
esac
# The mark is IN the image, and on the platforms that copy it
# verbatim it is the mark this repository committed - so a
# substituted or truncated icon cannot pass unnoticed. Windows
# embeds the ICO into the launcher, so there the committed container
# is verified by its own tests and what is asserted here is that the
# launcher exists; the boundary is stated rather than papered over.
case "$(uname -s)" in
    Darwin)
        installed="$image/Contents/Resources/JUranometria.icns"
        [ -f "$installed" ] || {
            echo "build-app-image: the image carries no icon at" \
                 "$installed" >&2
            exit 1
        }
        cmp -s "$installed" "$icon" || {
            echo "build-app-image: the image's icon is not the one" \
                 "this repository committed" >&2
            exit 1
        }
        echo "icon: $(basename "$icon") installed and identical"
        ;;
    Linux)
        installed=$(find "$image" -name 'JUranometria.png' | head -1)
        [ -n "$installed" ] || {
            echo "build-app-image: the image carries no icon" >&2
            find "$image" -maxdepth 3 -name '*.png' >&2 || true
            exit 1
        }
        # jpackage may re-encode for the desktop entry, so this
        # asserts a real PNG is there rather than identical bytes.
        head -c 8 "$installed" | od -An -tx1 | grep -q "89 50 4e 47" || {
            echo "build-app-image: the image's icon is not a PNG" >&2
            exit 1
        }
        echo "icon: installed at ${installed#"$image"/}"
        ;;
    *)
        [ -f "$launcher" ] || {
            echo "build-app-image: no launcher to carry the icon" >&2
            exit 1
        }
        echo "icon: embedded in $(basename "$launcher") by jpackage"
        ;;
esac

out="$dest/native-smoke.png"
env PATH=/nonexistent "$smoke" "$out" >/dev/null 2>&1 \
    || "$smoke" "$out" >/dev/null
size=$(wc -c < "$out")
[ "$size" -gt 10000 ] || {
    echo "build-app-image: native smoke render failed ($size bytes)" >&2
    exit 1
}

# The packaged acceptance surface (issue #150): About content and a
# genuine preference change-and-reload, through the bundled runtime
# alone, no system Java.
case "$(uname -s)" in
    Darwin) acceptance="$image/Contents/MacOS/juranometria-acceptance" ;;
    *) acceptance="$image/bin/juranometria-acceptance"
       [ -e "$acceptance" ] || acceptance="$image/juranometria-acceptance.exe" ;;
esac
accept_out=$(env PATH=/nonexistent "$acceptance" "$version" 2>&1) \
    || accept_out=$("$acceptance" "$version" 2>&1) || {
    echo "build-app-image: packaged acceptance failed:" >&2
    echo "$accept_out" >&2
    exit 1
}
echo "$accept_out" | grep -q "version binding OK" || {
    echo "build-app-image: the packaged application did not confirm" >&2
    echo "version $version:" >&2
    echo "$accept_out" >&2
    exit 1
}
echo "$accept_out" | grep -q "PACKAGED ACCEPTANCE OK" || {
    echo "build-app-image: packaged acceptance did not conclude:" >&2
    echo "$accept_out" >&2
    exit 1
}
echo "$accept_out" | sed 's/^/  /'

echo "app-image: $image"
echo "unpacked size: $(du -sh "$image" | cut -f1)"
echo "native headless smoke render: $size bytes"
