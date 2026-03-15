---
name: build_apk
description: Build the Android APK and copy it to the desktop.
---

# Build APK Skill

This skill automates the process of building a debug APK for the ThingsPath project and copying it to the user's desktop for easy access.

## Prerequisites
- JDK 17 must be installed.
- Android SDK must be configured.

## Usage
Run the following steps to build and deploy:

1. **Build the APK**:
   ```bash
   JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ./gradlew assembleDebug
   ```

2. **Copy to Desktop**:
   ```bash
   cp app/build/outputs/apk/debug/app-debug.apk ~/Desktop/thingspath-debug.apk
   ```
   *Note: Check the output path as it might vary (e.g., `thingspath-unsigned.apk` or `app-debug.apk`).*
