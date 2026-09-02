#!/bin/sh
# Downloads the bundled Tabler outline icons at a pinned release into
# src/resources/icons/. Idempotent; delete an icon file to re-fetch it.
# Record any icon added here in src/resources/icons/ICONS.md.
set -e

TABLER_VERSION="v3.46.0"
ICONS_DIR="$(dirname "$0")/../src/resources/icons"
BASE_URL="https://raw.githubusercontent.com/tabler/tabler-icons/$TABLER_VERSION/icons/outline"

ICONS="zoom-in zoom-out zoom-reset minus plus list-details x door-exit"

mkdir -p "$ICONS_DIR"
# The MIT license requires bundling the copyright and permission notice
# with the copied icons; it ships as a classpath resource beside them.
if [ -f "$ICONS_DIR/LICENSE" ]; then
  echo "Already exists: LICENSE"
else
  echo "Downloading LICENSE ($TABLER_VERSION)..."
  curl -fsSL "https://raw.githubusercontent.com/tabler/tabler-icons/$TABLER_VERSION/LICENSE" \
    -o "$ICONS_DIR/LICENSE"
fi
for icon in $ICONS; do
  dest="$ICONS_DIR/$icon.svg"
  if [ -f "$dest" ]; then
    echo "Already exists: $icon.svg"
  else
    echo "Downloading $icon.svg ($TABLER_VERSION)..."
    curl -fsSL "$BASE_URL/$icon.svg" -o "$dest"
  fi
done
echo "Done: $ICONS_DIR"
