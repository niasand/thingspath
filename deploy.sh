#!/bin/bash
set -e
echo "Building and installing APK..."
./gradlew installDebug


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
