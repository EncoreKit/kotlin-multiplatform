#!/bin/bash
# scripts/release/publish-release.sh
# Interactive release script with semantic versioning for KMP SDK.
# Modeled on encore-android/scripts/release/publish-release.sh.

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

GRADLE_PROPS="gradle.properties"

echo -e "${BLUE}Encore KMP SDK Release Manager${NC}"
echo ""

# =============================================================================
# Step 1: Validate repository state
# =============================================================================
echo -e "${BLUE}Step 1: Checking repository state...${NC}"

CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
    echo -e "${RED}Error: Must be on main branch (currently on $CURRENT_BRANCH)${NC}"
    exit 1
fi

if ! git diff-index --quiet HEAD --; then
    echo -e "${RED}Error: You have uncommitted changes${NC}"
    echo "   Commit or stash your changes before releasing"
    exit 1
fi

git fetch origin main --tags --force
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u})
if [ "$LOCAL" != "$REMOTE" ]; then
    echo -e "${RED}Error: Local main is out of sync with remote${NC}"
    echo "   Run: git pull origin main"
    exit 1
fi

echo -e "${GREEN}Repository is clean and up to date${NC}"
echo ""

# =============================================================================
# Step 2: Read current version from gradle.properties
# =============================================================================
echo -e "${BLUE}Step 2: Detecting current version...${NC}"

CURRENT_VERSION_NUMBER=$(grep -E '^VERSION_NAME=' "$GRADLE_PROPS" | cut -d'=' -f2)
if [ -z "$CURRENT_VERSION_NUMBER" ]; then
    echo -e "${RED}Error: VERSION_NAME not found in $GRADLE_PROPS${NC}"
    exit 1
fi

CURRENT_VERSION="v${CURRENT_VERSION_NUMBER}"
echo -e "   Current version: ${GREEN}$CURRENT_VERSION${NC}"

CURRENT_MAJOR=$(echo "$CURRENT_VERSION_NUMBER" | cut -d'.' -f1)
CURRENT_MINOR=$(echo "$CURRENT_VERSION_NUMBER" | cut -d'.' -f2)
CURRENT_PATCH=$(echo "$CURRENT_VERSION_NUMBER" | cut -d'.' -f3)

echo ""

# =============================================================================
# Step 3: Prompt for new version
# =============================================================================
echo -e "${BLUE}Step 3: Enter the new version number${NC}"
echo ""
echo -e "   Current version: ${GREEN}$CURRENT_VERSION${NC}"
echo -e "   Shortcuts:       ${YELLOW}patch${NC} -> v${CURRENT_MAJOR}.${CURRENT_MINOR}.$((CURRENT_PATCH + 1))"
echo -e "                    ${YELLOW}minor${NC} -> v${CURRENT_MAJOR}.$((CURRENT_MINOR + 1)).0"
echo -e "                    ${YELLOW}major${NC} -> v$((CURRENT_MAJOR + 1)).0.0"
echo ""

read -p "Enter version (e.g. 0.2.0) or shortcut (patch/minor/major): " VERSION_INPUT

case $VERSION_INPUT in
    patch)
        NEW_MAJOR=$CURRENT_MAJOR
        NEW_MINOR=$CURRENT_MINOR
        NEW_PATCH=$((CURRENT_PATCH + 1))
        ;;
    minor)
        NEW_MAJOR=$CURRENT_MAJOR
        NEW_MINOR=$((CURRENT_MINOR + 1))
        NEW_PATCH=0
        ;;
    major)
        NEW_MAJOR=$((CURRENT_MAJOR + 1))
        NEW_MINOR=0
        NEW_PATCH=0
        ;;
    *)
        VERSION_INPUT="${VERSION_INPUT#v}"
        if ! echo "$VERSION_INPUT" | grep -qE '^[0-9]+\.[0-9]+\.[0-9]+$'; then
            echo -e "${RED}Invalid format. Expected X.Y.Z (e.g. 0.2.0)${NC}"
            exit 1
        fi
        NEW_MAJOR=$(echo "$VERSION_INPUT" | cut -d'.' -f1)
        NEW_MINOR=$(echo "$VERSION_INPUT" | cut -d'.' -f2)
        NEW_PATCH=$(echo "$VERSION_INPUT" | cut -d'.' -f3)
        ;;
esac

NEW_VERSION="v${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"
NEW_VERSION_NUMBER="${NEW_MAJOR}.${NEW_MINOR}.${NEW_PATCH}"

# =============================================================================
# Step 4: Validate new version > current version
# =============================================================================
echo ""
echo -e "${BLUE}Step 4: Validating version...${NC}"

CURRENT_WEIGHT=$(( CURRENT_MAJOR * 1000000 + CURRENT_MINOR * 1000 + CURRENT_PATCH ))
NEW_WEIGHT=$(( NEW_MAJOR * 1000000 + NEW_MINOR * 1000 + NEW_PATCH ))

# Allow same version if tag doesn't exist yet (first publish)
TAG_EXISTS=$(git tag -l "$NEW_VERSION")
if [ "$NEW_WEIGHT" -lt "$CURRENT_WEIGHT" ]; then
    echo -e "${RED}New version $NEW_VERSION must not be less than current $CURRENT_VERSION${NC}"
    exit 1
elif [ "$NEW_WEIGHT" -eq "$CURRENT_WEIGHT" ] && [ -n "$TAG_EXISTS" ]; then
    echo -e "${RED}Version $NEW_VERSION is already tagged. Bump to a new version.${NC}"
    exit 1
fi

echo -e "${GREEN}Next version: $NEW_VERSION${NC}"
echo ""

# =============================================================================
# Step 5: Show changes since last release
# =============================================================================
echo -e "${BLUE}Step 5: Changes since $CURRENT_VERSION:${NC}"
echo ""

if git tag -l "$CURRENT_VERSION" | grep -q .; then
    git log --oneline "$CURRENT_VERSION"..HEAD | head -20
else
    git log --oneline | head -20
fi

echo ""

# =============================================================================
# Step 6: Confirm release details
# =============================================================================
echo -e "${YELLOW}Step 6: Confirm release details:${NC}"
echo ""
echo "   Current version:   $CURRENT_VERSION"
echo "   New version:       $NEW_VERSION"
echo "   Maven artifact:    com.encorekit:encore-kmp:$NEW_VERSION_NUMBER"
echo ""
read -p "Proceed with release? (yes/no): " CONFIRM

if [ "$CONFIRM" != "yes" ]; then
    echo -e "${RED}Release cancelled${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}Proceeding with release $NEW_VERSION${NC}"
echo ""

# =============================================================================
# Step 7: Run tests
# =============================================================================
echo -e "${BLUE}Step 7: Running tests...${NC}"
./gradlew :encore-kmp:allTests

echo -e "${GREEN}All tests passed${NC}"
echo ""

# =============================================================================
# Step 8: Update VERSION_NAME in gradle.properties
# =============================================================================
echo -e "${BLUE}Step 8: Updating VERSION_NAME to $NEW_VERSION_NUMBER...${NC}"

if [ "$NEW_VERSION_NUMBER" != "$CURRENT_VERSION_NUMBER" ]; then
    sed -i '' "s/^VERSION_NAME=.*/VERSION_NAME=$NEW_VERSION_NUMBER/" "$GRADLE_PROPS"

    if ! grep -q "^VERSION_NAME=$NEW_VERSION_NUMBER$" "$GRADLE_PROPS"; then
        echo -e "${RED}Error: Failed to update VERSION_NAME in $GRADLE_PROPS${NC}"
        exit 1
    fi

    echo -e "${GREEN}VERSION_NAME updated to $NEW_VERSION_NUMBER${NC}"
else
    echo -e "${GREEN}VERSION_NAME already at $NEW_VERSION_NUMBER — no change needed${NC}"
fi
echo ""

# =============================================================================
# Step 9: Commit version bump (if changed), push to origin, tag
# =============================================================================
echo -e "${BLUE}Step 9: Committing version bump and tagging...${NC}"

if [ "$NEW_VERSION_NUMBER" != "$CURRENT_VERSION_NUMBER" ]; then
    git add "$GRADLE_PROPS"
    git commit -m "Bump version to $NEW_VERSION_NUMBER"
    git push origin main
else
    echo -e "${GREEN}No version bump commit needed${NC}"
fi

echo -e "${BLUE}Creating tag $NEW_VERSION...${NC}"
git tag -a "$NEW_VERSION" -m "Release $NEW_VERSION"
git push origin "$NEW_VERSION"

echo -e "${GREEN}Version bump committed and tagged${NC}"
echo ""

# =============================================================================
# Step 10: Build Maven Central bundle for manual upload
# =============================================================================
echo -e "${BLUE}Step 10: Building Maven Central bundle...${NC}"

# Clean stale artifacts from Maven Local before publishing
rm -rf "$HOME/.m2/repository/com/encorekit"

./gradlew :encore-kmp:publishToMavenLocal

MAVEN_LOCAL="$HOME/.m2/repository"
ARTIFACT_BASE="$MAVEN_LOCAL/com/encorekit"
BUNDLE_DIR=".build/maven-central-bundle"
BUNDLE_ZIP=".build/encore-kmp-${NEW_VERSION_NUMBER}-bundle.zip"

rm -rf "$BUNDLE_DIR" "$BUNDLE_ZIP"
mkdir -p "$BUNDLE_DIR"

# KMP generates multiple publications (shared, shared-iosarm64, etc.)
# Copy all artifacts under com/encorekit/ matching the version
find "$ARTIFACT_BASE" -path "*/$NEW_VERSION_NUMBER/*" -type f | while read -r file; do
    relative="${file#$ARTIFACT_BASE/}"
    target_dir="$BUNDLE_DIR/com/encorekit/$(dirname "$relative")"
    mkdir -p "$target_dir"
    cp "$file" "$target_dir/"
done

echo -e "${BLUE}   Generating checksums...${NC}"
find "$BUNDLE_DIR" -type f | while read -r file; do
    case "$file" in *.md5|*.sha1|*.asc) continue ;; esac
    md5 -q "$file" > "$file.md5"
    shasum -a 1 "$file" | cut -d' ' -f1 > "$file.sha1"
done

(cd "$BUNDLE_DIR" && zip -r "../../$BUNDLE_ZIP" .)

BUNDLE_PATH="$(pwd)/$BUNDLE_ZIP"
echo -e "${GREEN}Bundle created: $BUNDLE_PATH${NC}"
echo ""
echo -e "${YELLOW}Upload to Maven Central:${NC}"
echo "   1. Go to https://central.sonatype.com"
echo "   2. Click 'Publish Component'"
echo "   3. Deployment name: encore-kmp-$NEW_VERSION_NUMBER"
echo "   4. Upload file: $BUNDLE_PATH"
echo "   5. Click 'Publish'"
echo ""
read -p "Press Enter after uploading to Maven Central to continue..."
echo ""

# =============================================================================
# Step 11: Trigger framework SDK version bumps (future)
# =============================================================================
echo -e "${BLUE}Step 11: Post-release...${NC}"
echo -e "${YELLOW}Note: KMP SDK does not yet trigger downstream bumps.${NC}"
echo ""

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}Release $NEW_VERSION published successfully!${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo "   1. Test Gradle dependency resolution:"
echo "      implementation(\"com.encorekit:encore-kmp:$NEW_VERSION_NUMBER\")"
echo "   2. Verify Maven Central: https://central.sonatype.com/artifact/com.encorekit/encore-kmp/$NEW_VERSION_NUMBER"
echo ""
