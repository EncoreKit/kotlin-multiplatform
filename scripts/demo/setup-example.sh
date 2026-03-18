#!/bin/bash
# scripts/demo/setup-example.sh
# Installs dependencies for the iOS and Android demo apps.
# After running, open in Xcode / Android Studio to build and run.

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${BLUE}Setting up Encore KMP demo apps...${NC}"

# iOS: CocoaPods setup
if [ -d "iosApp" ]; then
    echo -e "${BLUE}Installing CocoaPods for iOS demo...${NC}"
    ./gradlew :encore-kmp:podInstall
    (cd iosApp && pod install)
    echo -e "${GREEN}iOS: Open iosApp/EncoreKMPDemo.xcworkspace in Xcode${NC}"
fi

# Android: Gradle sync happens automatically when opening in Android Studio
echo -e "${GREEN}Android: Open the project root in Android Studio and run the 'demo' module${NC}"

echo -e "${GREEN}Setup complete.${NC}"
