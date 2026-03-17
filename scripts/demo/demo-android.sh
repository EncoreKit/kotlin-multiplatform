#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Setup deps
bash "$SCRIPT_DIR/setup-example.sh"

# Build debug APK
echo "==> ./gradlew :demo:assembleDebug"
cd "$SDK_ROOT" && ./gradlew :demo:assembleDebug

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Build complete. To run with native console logs:"
echo ""
echo "  1. Open the project root in Android Studio"
echo "  2. Select the 'demo' module"
echo "  3. Select an emulator or device"
echo "  4. Press Run (Shift+F10)"
echo ""
echo "  Native SDK logs appear in Logcat."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
