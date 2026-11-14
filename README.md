# ProfessorX Launcher - Red Phantom X (v1.0.0)

Kotlin + Jetpack Compose Android home launcher. Package: `com.professorx.launcher`. minSdk 26, targetSdk/compileSdk 34.

STATUS: source written, NOT yet compiled or run on a device. Build it, then report errors/crashes
back and they will be fixed before anything is marked verified (see CHECKLIST.md).

## Build (pick one)
1. GitHub Actions (phone friendly): create a GitHub repo, upload this folder's contents (keep
   `.github/workflows/build.yml`), push to `main`. Actions > "Build debug APK" > download the
   `professorx-debug-apk` artifact, unzip, install the APK.
2. AndroidIDE (on phone) or Android Studio (PC): open the folder, let it sync, run `assembleDebug`.
   Gradle 8.9+ and JDK 17 required. The wrapper jar is not included (see gradle/wrapper-README.txt).
3. Terminal with Gradle installed: `gradle assembleDebug`
   (output: app/build/outputs/apk/debug/app-debug.apk)

## Set as default launcher
Install the APK > press Home > choose "ProfessorX Launcher" > "Always".
(Or Settings > Apps > Default apps > Home app.)

## Permissions
- PACKAGE_USAGE_STATS: only for the Screen Time widget; the user grants it in system settings after an explanation dialog.
- No internet, location, storage or calendar permission is requested. Photos use the system Photo Picker
  and are copied into the app's private storage.

## Gestures
Swipe up on empty space = App Drawer. Swipe down = Drawer with search. Long-press empty space = Add menu.
Long-press an item = select (drag to move, dots to resize, Edit / Copy / Lock / Remove).
