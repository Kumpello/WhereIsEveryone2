# Android Studio run configurations

The shared configurations in this directory appear in Android Studio's Run/Debug
selector after the project is opened or refreshed. They use project-relative paths
and do not store device IDs or signing credentials.

## App variants

| Flavor | Debug | Release |
| --- | --- | --- |
| Development | `Run Development Debug` | `Run Development Release` |
| Production | `Run Production Debug` | `Run Production Release` |
| Production Premium | `Run Production Premium Debug` | `Run Production Premium Release` |

These six Gradle profiles invoke the corresponding `:app:run<Variant>` task. Each
task builds that exact variant, installs its APK with `adb install -r`, and launches
its launcher activity using an explicit component made from the variant's
application ID and namespace (`am start -n`). The profiles do not
depend on Android Studio's currently selected build variant.

Explicit component launching avoids implicit intent resolution for the
launcher-only activity filter, including non-debuggable release builds. The task
requires Activity Manager to report `Status: ok` and fails if launching is rejected,
even when `adb` exits with code zero. See the
[ADB activity manager documentation](https://developer.android.com/tools/adb#am).

Connect one authorized device or emulator before running a profile. If several are
connected, add `-PandroidRunSerial=SERIAL` to that profile's Gradle arguments, or
set `ANDROID_SERIAL` in its environment variables. The launch task checks the
selection before installing and refuses an ambiguous target. Installation preserves
app data and does not automatically uninstall an app if the signing key differs.

Release profiles use the project's existing release signing configuration. Configure
the signing values through `keystore.properties` or the supported environment
variables; no signing values belong in these XML files.

## IDE debugging

`App (selected build variant)` is a native Android App configuration with normal IDE
device selection and debugger support. Select the desired variant in **Build >
Select Build Variant**, then use this configuration's Run or Debug action. Release
builds retain their existing non-debuggable behavior.

Android Studio's native app configurations use the project's active build variant,
as described in the [Android Studio run documentation](https://developer.android.com/studio/run#changing-variant).
The Gradle launch profiles provide explicit variant selection; they do not attach
the Android app debugger.

## Builds and checks

The remaining profiles cover all public variant combinations supported by this
project:

- **Build APKs:** six `assemble<Variant>` profiles.
- **Build Bundles:** six `bundle<Variant>` profiles.
- **Lint:** six `lint<Variant>` profiles.
- **Tests:** three `test<DebugVariant>UnitTest` profiles and three
  `connected<DebugVariant>AndroidTest` profiles. The project currently generates
  test tasks for debug variants only. Device tests require a connected target.
- **Validation:** `Validate All Debug Variants` runs `test`, all three per-variant
  debug lint tasks, and `assembleDebug` with `--continue` and two Gradle workers.

There are 32 configurations in total. Private Gradle implementation tasks are not
individual run profiles.
