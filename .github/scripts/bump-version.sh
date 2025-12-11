#!/bin/bash
set -e

# Get the current version from build.sbt
CURRENT_VERSION=$(grep 'version := ' build.sbt | sed 's/version := "\(.*\)"/\1/' | tr -d '[:space:]')
echo "Current version: $CURRENT_VERSION"

# Remove -SNAPSHOT suffix if present
BASE_VERSION=$(echo $CURRENT_VERSION | sed 's/-SNAPSHOT//')
echo "Base version: $BASE_VERSION"

# Parse version components
IFS='.' read -r -a VERSION_PARTS <<< "$BASE_VERSION"
MAJOR="${VERSION_PARTS[0]}"
MINOR="${VERSION_PARTS[1]}"
PATCH="${VERSION_PARTS[2]}"

echo "Parsed version: Major=$MAJOR, Minor=$MINOR, Patch=$PATCH"

# Get the last tag (if exists)
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
if [ -z "$LAST_TAG" ]; then
  echo "No previous tag found, using initial version"
  LAST_TAG="v0.0.0"
fi
echo "Last tag: $LAST_TAG"

# Get commits since last tag
COMMITS=$(git log ${LAST_TAG}..HEAD --pretty=format:"%s")
echo "Analyzing commits since $LAST_TAG..."

# Determine version bump type
BUMP_TYPE="none"

# Check for breaking changes (major bump)
if echo "$COMMITS" | grep -qE "^[a-z]+(\(.+\))?!:|BREAKING CHANGE:"; then
  BUMP_TYPE="major"
  echo "Found breaking change - major version bump"
# Check for features (minor bump)
elif echo "$COMMITS" | grep -qE "^feat(\(.+\))?:"; then
  BUMP_TYPE="minor"
  echo "Found feature - minor version bump"
# Check for fixes (patch bump)
elif echo "$COMMITS" | grep -qE "^fix(\(.+\))?:"; then
  BUMP_TYPE="patch"
  echo "Found fix - patch version bump"
else
  BUMP_TYPE="patch"
  echo "No conventional commit found - defaulting to patch version bump"
fi

# Calculate new version
if [ "$BUMP_TYPE" == "major" ]; then
  MAJOR=$((MAJOR + 1))
  MINOR=0
  PATCH=0
elif [ "$BUMP_TYPE" == "minor" ]; then
  MINOR=$((MINOR + 1))
  PATCH=0
elif [ "$BUMP_TYPE" == "patch" ]; then
  PATCH=$((PATCH + 1))
fi

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
echo "New version: $NEW_VERSION"

# Update build.sbt
sed -i.bak "s/version := \".*\"/version := \"$NEW_VERSION\"/" build.sbt
rm build.sbt.bak 2>/dev/null || true

echo "Updated build.sbt to version $NEW_VERSION"

# Output for GitHub Actions
echo "new_version=$NEW_VERSION" >> $GITHUB_OUTPUT
echo "bump_type=$BUMP_TYPE" >> $GITHUB_OUTPUT
