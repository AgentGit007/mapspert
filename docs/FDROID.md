# F-Droid readiness

A checklist for the submission, with what is already done and what is not.

## Done in the repo

- **Licence**: GPL-3.0-or-later, `LICENSE` at the root.
- **No network**: the app declares no `INTERNET` permission, so being offline is a
  property of the build rather than a promise. **Verify after any dependency change**:
  open `app/build/outputs/logs/manifest-merger-debug-report.txt`, or run
  `aapt dump permissions app/build/outputs/apk/debug/app-debug.apk`; it should list none.
- **No proprietary dependencies**: AndroidX and Jetpack Compose only. No Google Play
  Services, no Firebase, no analytics.
- **Dependency metadata off**: `dependenciesInfo { includeInApk = false;
  includeInBundle = false }` in `app/build.gradle.kts`. F-Droid rejects the encrypted
  block the Android Gradle Plugin adds by default.
- **foojay-resolver removed** from `settings.gradle.kts`; F-Droid's scanner flags it
  because it downloads a JDK.
- **Bundled assets are free**: flags MIT, Unbounded and PT Mono SIL OFL 1.1, outlines from
  Natural Earth (public domain), country facts from Wikidata (CC0). Licence texts ship
  in `app/src/main/assets/licenses/` and are readable in the app under About.
- **Data is reproducible**: `tools/` holds the scripts, `data/` the inputs, and
  `data/README.md` the commands and the decisions made by hand.
- **Fastlane metadata**: `fastlane/metadata/android/en-US/`.

## To do before submitting

1. **Add `app/src/main/res/xml/locales_config.xml`** and reference it from the manifest
   once there is more than one language. Not needed yet.
2. **Release signing.** Create a keystore, keep it out of git (`*.jks`, `*.keystore`,
   `keystore.properties` in `.gitignore`), and record the fingerprint. F-Droid builds and
   signs from source with its own key, so a version you sideload now will not update from
   F-Droid later: uninstall first.
3. **Tag a release**: `git tag -a v0.1.0 -m "First release"` and push the tag. F-Droid
   builds from tags, not from the branch head.
4. **Set `versionCode` and `versionName`** deliberately in `app/build.gradle.kts`.
   `versionCode` must increase with every release.
5. **Write the metadata file** for `fdroiddata`, roughly:

   ```yaml
   Categories:
     - Science & Education
   License: GPL-3.0-or-later
   SourceCode: https://github.com/AgentGit007/mapspert
   IssueTracker: https://github.com/AgentGit007/mapspert/issues

   RepoType: git
   Repo: https://github.com/AgentGit007/mapspert.git

   Builds:
     - versionName: 0.1.0
       versionCode: 1
       commit: v0.1.0
       subdir: app
       gradle:
         - yes

   AutoUpdateMode: Version
   UpdateCheckMode: Tags
   ```

   Check this against F-Droid's current documentation before opening the merge request;
   the schema changes occasionally and I have not verified this against the current spec.
6. **Reproducible builds** are preferred but not required. If you want them, the usual
   blockers are timestamps and absolute paths in the APK.

## Release build settings worth adding

In `app/build.gradle.kts`, inside `buildTypes { release { ... } }`:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
```

The flags are looked up by name at runtime (`getIdentifier("flag_$key", ...)`), so resource
shrinking would strip them. `app/src/main/res/raw/keep.xml` already tells it not to:

```xml
<resources xmlns:tools="http://schemas.android.com/tools" tools:keep="@drawable/flag_*" />
```

Test a release build before releasing, specifically that flags still appear.
