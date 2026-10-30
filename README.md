# Curriculum Android project

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
The app's OpenAI API key remains in WebView localStorage (as in the original HTML app), not Android's secure keystore. Do not distribute a build containing your own key; add it only on your personal phone. Some external services may not permit requests from a WebView origin. The generated project has not been compiled or tested on a real device in this environment.

## Alternative: build without Android Studio using GitHub Actions
Push the folder contents (including the hidden `.github` directory) into a private GitHub repository on the `main` branch. Open the repository's **Actions > Build Android APK > Run workflow** (or let its push trigger run). Download the `Curriculum-APK` workflow artifact when the build completes, unzip it, and install `app-debug.apk` on your phone. This relies on GitHub's online build environment; it is not performed here.
