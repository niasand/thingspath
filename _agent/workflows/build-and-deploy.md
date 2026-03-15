---
description: Build the APK locally and copy it to the desktop
---

This workflow automates the local build and deployment process to ensure the desktop always has the latest APK.

1. Set the Java environment and build the APK.
// turbo
2. Build the project using Gradle.
```
JAVA_HOME="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" ./gradlew assembleDebug
```

3. Copy the output APK to the Desktop.
// turbo
4. Run the copy command.
```
cp app/build/outputs/apk/debug/thingspath-unsigned.apk ~/Desktop/thingspath-debug.apk
```
