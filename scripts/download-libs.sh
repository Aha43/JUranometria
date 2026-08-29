#!/bin/sh
# Downloads the pinned runtime and test dependencies into lib/ and
# lib/test/ - the POSIX twin of download-libs.ps1. Idempotent: files
# that already exist are skipped. Versions come from lib-versions.env,
# the single authority shared with the PowerShell script and Makefile.
set -e

SCRIPT_DIR="$(dirname "$0")"
. "$SCRIPT_DIR/lib-versions.env"
ROOT_DIR="$SCRIPT_DIR/.."
LIB_DIR="$ROOT_DIR/lib"
TEST_LIB_DIR="$ROOT_DIR/lib/test"
MAVEN="https://repo1.maven.org/maven2"

mkdir -p "$LIB_DIR" "$TEST_LIB_DIR"

fetch() {
  # $1 group path, $2 file name, $3 target directory
  if [ -f "$3/$2" ]; then
    echo "Already exists: $2"
  else
    echo "Downloading $2..."
    curl -fsSL "$MAVEN/$1/$2" -o "$3/$2"
  fi
}

fetch "com/formdev/flatlaf/$FLATLAF_VERSION" \
      "flatlaf-$FLATLAF_VERSION.jar" "$LIB_DIR"
fetch "com/formdev/flatlaf-extras/$FLATLAF_VERSION" \
      "flatlaf-extras-$FLATLAF_VERSION.jar" "$LIB_DIR"
fetch "com/github/weisj/jsvg/$JSVG_VERSION" \
      "jsvg-$JSVG_VERSION.jar" "$LIB_DIR"
fetch "org/junit/platform/junit-platform-console-standalone/$JUNIT_VERSION" \
      "junit-platform-console-standalone-$JUNIT_VERSION.jar" "$TEST_LIB_DIR"

echo "Done."
echo "  Runtime libs : $LIB_DIR"
echo "  Test libs    : $TEST_LIB_DIR"
