# Зургийн хавтас (Photo Folders)

Android app: pick photos from the phone's gallery, put them into a named folder, and browse only those photos.

- Photos are picked with the system Photo Picker (no storage permission needed).
- Picked photos are **copied** into the app's private storage, so they stay even if the originals are deleted from the gallery.
- Folder list → folder grid → fullscreen viewer (swipe, pinch / double-tap zoom).
- Rename / delete folders, add / delete photos (originals in the gallery are never touched).

Kotlin + Jetpack Compose, Material 3, Coil. minSdk 26 (Android 8.0).

## Build

Requires JDK 17 and the Android SDK.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.12'
.\gradlew.bat assembleRelease
```

APK: `app/build/outputs/apk/release/app-release.apk` (signed with the debug key, installable directly on a phone).
