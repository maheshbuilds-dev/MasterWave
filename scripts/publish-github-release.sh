#!/usr/bin/env bash
set -euo pipefail

# ==============================================================================
# MasterWave GitHub Release & Direct In-App Update Broadcaster
# ==============================================================================

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_DIR"

# 1. Extract version info from build.gradle.kts
VERSION_CODE=$(grep 'versionCode =' app/build.gradle.kts | head -n1 | tr -dc '0-9')
VERSION_NAME=$(grep 'versionName =' app/build.gradle.kts | head -n1 | cut -d '"' -f2)
TAG="v${VERSION_NAME}"

echo "=================================================="
echo "MasterWave Release Publisher: $TAG (Code: $VERSION_CODE)"
echo "=================================================="

# 2. Compile release APK
echo "[1/5] Compiling signed release APK..."
./gradlew assembleRelease --no-daemon

APK_PATH="app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK_PATH" ]]; then
    echo "ERROR: Release APK not found at $APK_PATH"
    exit 1
fi

APK_SIZE_BYTES=$(wc -c < "$APK_PATH" | tr -d ' ')
APK_SIZE_MB=$(python3 -c "print(f'{$APK_SIZE_BYTES / (1024*1024):.1f} MB')")
echo "Built release APK: $APK_SIZE_MB ($APK_SIZE_BYTES bytes)"

# 3. Update update.json in the repository
echo "[2/5] Updating update.json..."
PUBLISHED_AT=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

cat <<EOF > update.json
{
  "versionCode": $VERSION_CODE,
  "versionName": "$VERSION_NAME",
  "releaseNotes": "MasterWave $TAG release with latest performance, audio, and visual enhancements.",
  "apkUrl": "https://github.com/mahesh2008here-star/MasterWave/releases/download/$TAG/app-release.apk",
  "apkSize": "$APK_SIZE_MB",
  "publishedAt": "$PUBLISHED_AT",
  "forceUpdate": false
}
EOF

# 4. Commit and push tag if not existing
echo "[3/5] Verifying git tag..."
if git rev-parse "$TAG" >/dev/null 2>&1; then
    echo "Tag $TAG already exists locally."
else
    git add update.json
    git commit -m "chore(release): bump update.json to $TAG (code $VERSION_CODE)" || true
    git tag -a "$TAG" -m "MasterWave $TAG release"
    git push origin main
    git push origin "$TAG"
fi

# 5. Create or upload to GitHub Release
echo "[4/5] Publishing GitHub Release via gh CLI..."
if gh release view "$TAG" >/dev/null 2>&1; then
    echo "Updating assets on existing release $TAG..."
    gh release upload "$TAG" "$APK_PATH#app-release.apk" --clobber
else
    echo "Creating new GitHub Release $TAG..."
    gh release create "$TAG" "$APK_PATH#app-release.apk" \
        --title "MasterWave $TAG — Lossless Music Player" \
        --notes "### What's New in $TAG (Code: $VERSION_CODE)
- MasterWave $TAG direct release
- Code: $VERSION_CODE
- Instant in-app update delivered directly from GitHub"
fi

echo "=================================================="
echo "SUCCESS: MasterWave $TAG is live on GitHub Releases!"
echo "All installed apps will now detect and download this update directly."
echo "=================================================="
