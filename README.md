# Curriculum Android project

## Reproducible build and tests

Requires JDK 17, Android SDK Platform 35 and Build Tools 35.0.0. The included
Gradle 8.9 wrapper downloads Gradle automatically. Set `ANDROID_HOME` to your SDK
directory, then run (on Windows use `gradlew.bat`):

```sh
./gradlew assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
```

The second command needs a running emulator or USB-debugging Android device.
Tests cover startup, navigation, the native backup bridge, program creation and
saved-data persistence after reloading. GitHub Actions runs these checks on an
Android 15 emulator and uploads the APK only after they pass. Test reports are
uploaded separately. Paid AI calls and document picker flows need manual testing.

Minimum Android version: Android 8.0 (API 26). The APK is debug-signed for personal
testing. GitHub runners generate independent debug keys: a later build may require
uninstalling the old app. Export a backup before uninstalling. JSON backups include
your API key, so keep them private. Use a persistent signing keystore for releases.

This is the existing Curriculum web app packaged in an Android WebView with a stable local HTTPS origin (`https://curriculum.local/`, intercepted from bundled assets). No server/hosting is required. Internet is used for OpenAI API calls and optional external links. File attachments use the Android file picker. JSON exports use Android's Save As picker.

## Build an APK
1. Install Android Studio (a current stable version).
2. Open this folder as a project. Allow Android Studio to install Android SDK Platform 35 and sync Gradle.
3. Choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. Find `app/build/outputs/apk/debug/app-debug.apk`. Copy that APK to your Android phone, open it, and allow installation from that source if prompted.

For long-term distribution, build and sign a release APK with your own keystore. Debug APK is suitable for personal testing.

## Before migrating
In the existing HTML app, Settings > Export JSON. On the Android app, Settings > Import JSON. The old browser storage does **not** automatically transfer into the Android app.

## Limitations and security
The app's OpenAI API key remains in WebView localStorage (as in the original HTML app), not Android's secure keystore. Add it only on your personal phone. Some external services may not permit requests from a WebView origin. Automated emulator tests do not replace testing paid AI calls and backup/import document pickers on your own device.

## Alternative: build without Android Studio using GitHub Actions
Push the folder contents (including the hidden `.github` directory) into a private GitHub repository on the `main` branch. Open the repository's **Actions > Build Android APK > Run workflow** (or let its push trigger run). Download the `Curriculum-APK` workflow artifact when the build completes, unzip it, and install `app-debug.apk` on your phone. This relies on GitHub's online build environment; it is not performed here.
