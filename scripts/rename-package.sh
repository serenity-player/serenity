#!/usr/bin/env bash
# Rebrand a fresh copy of the Serenity repository before its first commit.
#
# Usage:
#   scripts/rename-package.sh <package> <github-user> "<copyright holder>"
# Example:
#   scripts/rename-package.sh io.github.alice.serenity alice "Alice Example"
#
# Updates the Android namespace/application ID, Kotlin package declarations and
# source directories, GitHub repository links, and the MIT copyright notice.
set -euo pipefail

NEW_PKG="${1:-}"
GH_USER="${2:-}"
AUTHOR="${3:-}"
if [[ -z "$NEW_PKG" || -z "$GH_USER" || -z "$AUTHOR" ]]; then
  echo 'Usage: scripts/rename-package.sh <package> <github-user> "<copyright holder>"' >&2
  exit 1
fi
if ! [[ "$NEW_PKG" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "Invalid package '$NEW_PKG'. Use lowercase letters, digits and underscores, dot-separated." >&2
  exit 1
fi
if ! [[ "$GH_USER" =~ ^[A-Za-z0-9]([A-Za-z0-9-]*[A-Za-z0-9])?$ ]]; then
  echo "Invalid GitHub user '$GH_USER'." >&2
  exit 1
fi

cd "$(dirname "$0")/.."

# This repository's configured identity. Change these only when the canonical
# identity of this repository changes.
BASE_PKG='io.github.serenity_player.serenity'
BASE_PATH='io/github/serenity_player/serenity'
BASE_GH_USER='serenity-player'
NEW_PATH="${NEW_PKG//.//}"

# Move both source trees to match the requested Kotlin package.
if [[ "$NEW_PATH" != "$BASE_PATH" ]]; then
  for root in app/src/main/java app/src/test/java; do
    if [[ -d "$root/$BASE_PATH" ]]; then
      if [[ -e "$root/$NEW_PATH" ]]; then
        echo "Target source directory already exists: $root/$NEW_PATH" >&2
        exit 1
      fi
      mkdir -p "$(dirname "$root/$NEW_PATH")"
      mv "$root/$BASE_PATH" "$root/$NEW_PATH"
      rmdir -p "$(dirname "$root/$BASE_PATH")" 2>/dev/null || true
    fi
  done
fi

# Rewrite package names, repository owner references, and the copyright holder.
OLD_PKG_SED="${BASE_PKG//./\\.}"
while IFS= read -r -d '' file; do
  sed -i.bak \
    -e "s|$OLD_PKG_SED|$NEW_PKG|g" \
    -e "s|$BASE_GH_USER|$GH_USER|g" \
      "$file"
  rm -f "$file.bak"
done < <(grep -rlIZ --exclude-dir=.git --exclude-dir=build --exclude-dir=.gradle \
  --exclude=rename-package.sh \
  -e "$BASE_PKG" -e "$BASE_GH_USER" . || true)

# Keep the copyright holder independent from the GitHub account name.
awk -v author="$AUTHOR" '/^Copyright \(c\) [0-9][0-9][0-9][0-9] / { sub(/[^[:space:]]+$/, author) } { print }' LICENSE > LICENSE.tmp
mv LICENSE.tmp LICENSE

echo "Done. Package: $NEW_PKG  GitHub user: $GH_USER  copyright holder: $AUTHOR"
echo 'Next: ./gradlew testDebugUnitTest assembleDebug'
