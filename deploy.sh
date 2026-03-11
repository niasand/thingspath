#!/bin/bash
set -e
echo "Building and installing APK..."
./gradlew installDebug

# Add build record to release.md
echo "Updating release.md..."
timestamp=$(date "+%Y-%m-%d %H:%M:%S")

# Ensure release.md exists
if [ ! -f "release.md" ]; then
    echo "# Release Log" > release.md
fi

# Append build record
echo "" >> release.md
echo "## Build Record: $timestamp" >> release.md
echo "- Status: Success" >> release.md

if git rev-parse --is-inside-work-tree > /dev/null 2>&1; then
    commit_hash=$(git rev-parse --short HEAD)
    echo "- Base Commit: $commit_hash" >> release.md
fi

# Git commit if message provided
if [ -n "$1" ]; then
    echo "Committing changes..."
    git add .
    git commit -m "$1"
    
    # Get the new commit hash
    new_hash=$(git rev-parse --short HEAD)
    echo "Committed as: $new_hash"
else
    echo "No commit message provided. Skipping git commit."
    echo "Usage: ./deploy.sh \"Commit message\""
fi

echo "Launching app..."
adb shell am start -n com.thingspath/com.thingspath.ui.MainActivity
