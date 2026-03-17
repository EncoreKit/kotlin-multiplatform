#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SDK_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

# Setup deps
bash "$SCRIPT_DIR/setup-example.sh"

# Build shared framework for iOS
echo "==> ./gradlew :shared:podInstall"
cd "$SDK_ROOT" && ./gradlew :shared:podInstall

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Build complete. To run with native console logs:"
echo ""
echo "  1. Open iosApp/EncoreKMPDemo.xcworkspace in Xcode"
echo "  2. Select a simulator or device"
echo "  3. Press Cmd+R to run"
echo ""
echo "  Native SDK logs appear in the Xcode console."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
