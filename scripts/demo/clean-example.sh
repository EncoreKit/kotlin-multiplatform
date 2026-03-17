#!/bin/bash
# scripts/demo/clean-example.sh
# Removes build artifacts from demo apps.
# Usage: ./scripts/demo/clean-example.sh [--nuke]

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${BLUE}Cleaning demo build artifacts...${NC}"

./gradlew clean

# iOS artifacts
rm -rf iosApp/Pods iosApp/*.xcworkspace iosApp/Podfile.lock
rm -rf shared/build

if [ "$1" = "--nuke" ]; then
    echo -e "${BLUE}Nuke mode: removing all caches...${NC}"
    rm -rf .gradle build
    rm -rf ~/Library/Caches/org.swift.swiftpm
    rm -rf iosApp/xcshareddata/swiftpm
fi

echo -e "${GREEN}Clean complete.${NC}"
