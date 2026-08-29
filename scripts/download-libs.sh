#!/bin/sh
# Downloads the pinned runtime and test dependencies into lib/ and
# lib/test/. Idempotent: files whose pinned SHA-256 already verifies
# are skipped. Versions and checksums come from lib-versions.env, the
# single authority shared with the Makefile.
set -e

SCRIPT_DIR="$(dirname "$0")"
. "$SCRIPT_DIR/lib-versions.env"
ROOT_DIR="$SCRIPT_DIR/.."
LIB_DIR="$ROOT_DIR/lib"
TEST_LIB_DIR="$ROOT_DIR/lib/test"
MAVEN="https://repo1.maven.org/maven2"

mkdir -p "$LIB_DIR" "$TEST_LIB_DIR"

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | cut -d' ' -f1
  else
    shasum -a 256 "$1" | cut -d' ' -f1
  fi
}

fetch() {
  # $1 group path, $2 file name, $3 target directory, $4 expected SHA-256
  target="$3/$2"
  if [ -f "$target" ]; then
    if [ "$(sha256 "$target")" = "$4" ]; then
      echo "Already exists: $2"
      return
    fi
    echo "Checksum mismatch for existing $2 (partial or corrupt); re-downloading..."
    rm -f "$target"
  fi
  echo "Downloading $2..."
  # Download to a temporary name and move into place only after the
  # checksum verifies, so an interrupted transfer can never pose as a
  # cached dependency.
  curl -fsSL "$MAVEN/$1/$2" -o "$target.download"
  actual="$(sha256 "$target.download")"
  if [ "$actual" != "$4" ]; then
    rm -f "$target.download"
    echo "ERROR: $2 failed its pinned SHA-256 (expected $4, got $actual)." >&2
    echo "Check the network and scripts/lib-versions.env, then rerun this script." >&2
    exit 1
  fi
  mv "$target.download" "$target"
}

fetch "com/formdev/flatlaf/$FLATLAF_VERSION" \
      "flatlaf-$FLATLAF_VERSION.jar" "$LIB_DIR" "$FLATLAF_SHA256"
fetch "com/formdev/flatlaf-extras/$FLATLAF_VERSION" \
      "flatlaf-extras-$FLATLAF_VERSION.jar" "$LIB_DIR" "$FLATLAF_EXTRAS_SHA256"
fetch "com/github/weisj/jsvg/$JSVG_VERSION" \
      "jsvg-$JSVG_VERSION.jar" "$LIB_DIR" "$JSVG_SHA256"
fetch "org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION" \
      "junit-platform-console-standalone-$JUNIT_VERSION.jar" "$TEST_LIB_DIR" "$JUNIT_SHA256"

echo "Done."
echo "  Runtime libs : $LIB_DIR"
echo "  Test libs    : $TEST_LIB_DIR"
