# Auto Clicker (Android)

Kotlin + XML Android app using Gradle Kotlin DSL and GitHub Actions.

## Features
- Tap at a configured screen coordinate.
- Start immediately, after a delay, or at the next local `HH:mm` time.
- Configure interval in milliseconds and a finite click count.
- Stop button and automatic stop after the requested count.
- Floating coordinate marker (requires “Display over other apps” permission).
- GitHub Actions builds debug and release APK artifacts.

## Build on GitHub
1. Create a GitHub repository and upload/commit all project files.
2. Open **Actions** and enable workflows if GitHub asks.
3. Run **Build Android APKs** manually from **Run workflow**, or push to `main`/`master`.
4. Open the completed workflow run and download `autoclicker-apks` under **Artifacts**.

The workflow uses Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, JDK 17, and Android SDK 35.

## Important notes
- `assembleRelease` creates an **unsigned release APK**. It is useful as a build artifact but is not ready for Play Store distribution or installation over an existing signed app until you configure release signing.
- The debug APK is signed with Android's debug key.
- Enable the service manually in Android Settings → Accessibility → Auto Clicker.
- Grant “Display over other apps” before using the coordinate marker.
- The picker saves the overlay marker's approximate screen position to preferences. You may enter X/Y directly for exact coordinates. Overlay/window coordinate origins can vary by device/navigation mode.
- Android may delay gestures under load. An interval is a target delay, not a hard real-time guarantee.
- Test only in apps where automation is permitted. The service dispatches screen gestures and does not inspect other apps' content.
- For production release signing, use GitHub Actions secrets and configure `signingConfig`; never commit keystore files or passwords.

## Gradle Wrapper note
The repository includes `gradle-wrapper.properties`; GitHub Actions uses Gradle 8.9 installed by the workflow and therefore builds without a wrapper JAR. The included `gradlew` scripts are convenience launchers for systems that already have Gradle installed, not a self-contained wrapper. To add the official wrapper JAR, run `gradle wrapper --gradle-version 8.9` in this project on a machine with Gradle installed, then commit the generated wrapper files.

- The header displays “Powered by Mehrdad R”.
- Site time synchronization reads the HTTP `Date` response header, not the visible clock on the webpage. Some sites block HEAD requests or do not provide a usable header; use another HTTPS URL or the phone's clock. The offset is approximate and depends on network latency and the site's server clock.


## Floating marker and permissions
- With the draw-over-other-apps permission granted, the marker can be dragged over other apps.
- Dragging stores its target coordinate; tapping the marker attempts one accessibility tap at its center and leaves the marker visible.
- On launch, the app attempts to synchronize time using the HTTP `Date` header from `https://tsetmc.ir`. This may fail if the site does not return a valid header or blocks the request.
- Android does not allow apps to silently enable Accessibility or overlay permissions. The user must explicitly enable these in Android Settings; the app can only open the relevant settings page.
- Special permissions cannot all be granted at installation; Android requires user confirmation when needed.
