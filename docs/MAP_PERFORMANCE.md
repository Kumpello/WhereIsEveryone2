# Map rendering and performance

Reviewed against Mapbox's Android documentation on 2026-10-08. The project uses Maps SDK and
Compose extension **11.31.1**, with the NDK 27 artifacts for 16 KB page-size support.

## Marker updates

Friend markers use two symbol layers sharing one GeoJSON source. This follows Mapbox's
[style-layer guidance](https://docs.mapbox.com/android/maps/guides/add-your-data/style-layers/)
and avoids allocating an Android view or annotation manager for every friend.

- Coordinates remain `Double` throughout animation. Position/bearing, size/speed, and opacity
  retain their independent one-second tweens; unchanged values do not start a tween.
- Animation snapshots use Compose's immutable, constant-time `SnapshotStateMap.toMap()`
  outside composition. Feature construction runs on
  `Dispatchers.Default`; unchanged friends reuse their data and cached color.
- Each feature has a stable GeoJSON ID. The source is initialized once per style, then uses
  [partial GeoJSON updates](https://docs.mapbox.com/android/maps/guides/migrate-to-v11/#introducing-partial-geojsonsource-apis)
  to add, update, or remove only affected features. Do not also assign `sourceState.data`:
  full replacements clear queued partial operations.
- Avatar bitmaps are created on a worker only for friends with locations. Uploaded images are
  tracked per style, restored after style loading, and removed when a marker disappears. The
  existing 50-entry bitmap cache stays bounded; uploads use small batches. The
  two SDF ring images are explicitly registered because an expression referencing an image ID
  does not register a `StyleImage`.
- Puck setup waits for style-loading events instead of polling a captured style. Transient
  animated source data is not serialized into the Activity's saved state.
- Camera bearing is sent to the ViewModel only while friend navigation is active. Starting
  navigation reads the current bearing immediately; the Mapbox compass works independently.

`FriendFeatureUpdatesTest` covers IDs, coordinate precision, independent add/update/remove
operations, immutable submitted features, and a 1,000-friend case in which one changed friend
creates one feature. Compose instrumented tests cover stationary markers, short bearing
rotation, coordinate precision, and independent opacity/position animation.

## Optional Vulkan build

In Android Studio, select **Production Release Vulkan (ADB)** and click **Run**. This
configuration builds the signed production release, installs it on one authorized ADB device,
and launches it with Vulkan enabled for that invocation.

OpenGL remains the default. Mapbox's
[Vulkan backend](https://docs.mapbox.com/android/maps/guides/#vulkan-rendering-backend-public-preview)
is a public preview with **ARM64-only support and no automatic OpenGL fallback**. Android 12+
is recommended; older GPU drivers can have problems. Map snapshots, custom layers, Android
Auto's `MapWidget`, and rain are currently unsupported by this backend.

Build a Vulkan APK with:

```sh
./gradlew :app:assembleDevelopmentDebug -PmapboxVulkan=true
```

Build, install, and launch it on one compatible ADB-connected device with:

```sh
./gradlew :app:runDevelopmentDebug -PmapboxVulkan=true
```

The production release configuration uses:

```sh
./gradlew :app:runProductionRelease -PmapboxVulkan=true
```

The flag applies Mapbox's dependency substitution across module configurations, replaces
`android-core-ndk27` with the matching `android-core-vulkan-ndk27` version, restricts the APK
to `arm64-v8a`, and raises that build's minimum SDK to 31. It is a build-time selection, not
a runtime renderer switch. Use a device with working Vulkan support; API level and ABI alone
do not prove that its GPU driver works with the preview.

For Android Studio's existing Run configurations, temporarily add `mapboxVulkan=true` to the
project's `gradle.properties`, sync, select a compatible device, and Run as usual. Remove that
line or set it to `false` and sync to return to the normal Android 9+ builds and supported ABIs.
Do not publish a Vulkan build until it has been tested on the intended device set.

## Measuring on a phone

No FPS or GPU improvement is assumed from the renderer choice. Compare both builds on the
same phone, camera position, pitch, friend count, and movement sequence. Include cold map
loading, zoom/rotation, many moving friends, and removing/re-adding markers.

Use Android Studio's CPU/memory profiler to inspect UI-thread work and allocations. Mapbox's
[performance statistics API](https://docs.mapbox.com/android/maps/guides/debugging-and-profiling/performance-stats/)
can report render durations and texture/vertex memory, and its
[tracing API](https://docs.mapbox.com/android/maps/guides/migrate-to-v11/#tracing-api)
can expose native and Android rendering work in a trace. Enable these probes only for a
profiling session because monitoring adds overhead. Compare a release build for representative
performance; a development-flavor release still uses the development backend.

If many friends move simultaneously, partial updates still have to process every moving
feature. Clustering, a lower animation update frequency, or reducing Standard-style 3D content
would change visible behavior and should be driven by device measurements.

## Credential configuration finding

`app/build.gradle.kts` embeds `MAPBOX_TOKEN` in the runtime `mapbox_access_token` resource,
and `settings.gradle` also uses it as the Maven download credential. The configured local
value is a secret token (`sk.`), so that secret is currently included in the APK.
Mapbox's [installation guide](https://docs.mapbox.com/android/maps/guides/install/#step-1-configure-your-public-token)
requires a **public** runtime token (`pk.`). Replace the runtime value with a public token from
the account, and keep any secret download credential separate from Android resources.
