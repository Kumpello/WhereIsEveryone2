# WhereIsEveryone: Google Play beta setup

The workflow publishes `com.kumpello.whereiseveryone` using the production server,
release signing, and Mapbox Vulkan. The Google Play destination is **internal testing**.
Premium is not deployed. Vulkan currently requires ARM64 and this project sets Android 12+
for Vulkan builds; test maps on your friends' actual devices because there is no OpenGL fallback.

## 1. Create the Play app

1. Open https://play.google.com/console/ and register/verify your developer account if needed.
2. Choose **Create app**, name it **WhereIsEveryone**, select its language, choose **App**,
   choose the intended pricing, and complete the declarations.
3. Open **Test and release → Testing → Internal testing**. This supports up to 100 testers.
   You can start an internal test before completing all store setup.
4. The first bundle fixes the package name: `com.kumpello.whereiseveryone`.

## 2. Configure Mapbox and signing

The existing ignored `keystore.properties` points to an existing release key. Back up that
key and its passwords securely. Reuse the correct registered upload key if the app already
exists in Play. Do not create a replacement key arbitrarily.

Create/copy a **public** Mapbox token starting with `pk.` from https://console.mapbox.com/.
Add `MAPBOX_PUBLIC_TOKEN=pk.…` to the local ignored `gradle.properties`. The separate
`MAPBOX_TOKEN` credential is used only for Maven downloads; never embed an `sk.` token
in the app. If previous APKs containing that download token have been shared, rotate it
in Mapbox and update local/CI download credentials.

For GitHub, open https://github.com/Kumpello/WhereIsEveryone2/settings/secrets/actions
and create these repository secrets (do not paste private values into chat):

| Secret | Value |
| --- | --- |
| `MAPBOX_TOKEN` | Mapbox Maven download credential |
| `MAPBOX_PUBLIC_TOKEN` | Public runtime map token beginning with `pk.` |
| `KEYSTORE_BASE64` | Base64 of the existing release/upload keystore |
| `KEYSTORE_PASSWORD` | Existing keystore password |
| `KEY_ALIAS` | Existing signing key alias |
| `KEY_PASSWORD` | Existing signing key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Full JSON credentials from step 5 |

On Linux, base64 can be copied directly to a clipboard tool, if installed:

```sh
base64 -w 0 /path/to/existing-upload-key.jks | wl-copy
```

Otherwise generate it in a private temporary file and paste it into GitHub's secret field.
Base64 is not encryption. Remove any temporary copy afterwards. Do not commit it.

## 3. Build the first bundle

After setting the public Mapbox token, run from the Android repository:

```sh
./gradlew :app:bundleProductionRelease -PmapboxVulkan=true -PversionCode=2 --max-workers=2
```

If Play already contains version code 2 or greater, choose a new unused higher code.
The result is `app/build/outputs/bundle/productionRelease/app-production-release.aab`.
Install and test an equivalent production Vulkan APK on a real compatible device before release:

```sh
./gradlew :app:runProductionRelease -PmapboxVulkan=true -PversionCode=2
```

Alternatively, after the workflow changes reach master and signing/Mapbox secrets are set,
open GitHub **Actions → Android CI/CD → Run workflow**, choose `master`, leave `publish`
unchecked, and download the `production-vulkan-…` artifact. This uses the CI version sequence.
AAB files are uploaded to Play; they cannot be installed directly like APK files.

## 4. First manual Play release and friends

1. On Internal testing, create a release and configure **Play App Signing** when prompted.
   Let Google manage the app signing key unless you have a reason to supply your own.
   CI will continue signing bundles with your upload key.
2. Upload the signed AAB manually; the publishing action requires this initial upload.
3. Add release notes, resolve the Console's listed requirements, and roll out to internal testing.
   If Console says the app is still a draft, finish this initial rollout before enabling CI publishing.
4. Under **Testers**, create/select an email list containing your friends' Google-account addresses.
5. Save and copy the opt-in link. Share it with your friends; each must opt in with the listed
   account before installing. First-time links may take several hours to become available.
6. Friends already using a sideloaded release may need to uninstall it if its signing certificate
   differs from Google's app signing key. This can remove local data. Debug/development apps
   have different package names and do not become the Play app automatically.

## 5. Give CI access to Google Play

1. Create/select a Google Cloud project at https://console.cloud.google.com/.
2. Enable **Google Play Android Developer API** (Android Publisher API).
3. Create a service account for CI. It does not need broad Cloud project Owner/Editor roles.
4. In Play Console **Users and permissions → Invite new users**, enter the service account email.
   Restrict app access to WhereIsEveryone. Grant app visibility and **Release apps to testing tracks**.
   Do not grant production release or financial permissions for this workflow.
5. Create a JSON key for that service account, store its full contents as GitHub secret
   `PLAY_SERVICE_ACCOUNT_JSON`, then remove the downloaded copy from shared locations.
   Protect and rotate this credential as needed.

## 6. Enable automatic releases

In GitHub **Settings → Secrets and variables → Actions → Variables**, set:

| Variable | Value |
| --- | --- |
| `PLAY_PUBLISH_ENABLED` | `true`, only after the first Play rollout and secret setup |
| `PLAY_VERSION_CODE_BASE` | Optional; defaults to `1000` |

The version code is base + GitHub workflow run number. Choose a base so the next generated
code exceeds every previously uploaded code. Never decrease it or reset the sequence when
renaming/replacing the workflow. Codes must remain at most 2,100,000,000.
A rerun uses the same code; if a prior upload succeeded, launch a **new** workflow run instead.
Rollbacks also require a new build with a higher code.

After a master push, tests and lint must pass before deployment. The release job builds only
production Vulkan, retains the bundle and mapping as a GitHub artifact, serializes Play uploads,
and releases to `internal` with status `completed`. Play processing may delay tester delivery.
PRs do not publish. Manual runs publish only when `publish` is checked and the enable variable
is true. Set the enable variable to `false` to stop future automatic publishing.

Inside the serialized release job, a final guard skips uploading if master has
advanced or a newer master push/manual run exists, including a newer run for the
same commit. Stale runs can still finish their build and retain the artifact.
A newer manual run with `publish` unchecked also supersedes older pending
publishers; start a fresh manual run with `publish` checked when ready to release.
If the guard cannot verify run history, publication fails closed. External/manual
Play uploads still require choosing a sufficiently high version-code base.

For fork PRs, secret-dependent Mapbox dependency downloads require a separate trusted validation
strategy; never expose download/signing credentials by switching to pull_request_target.

## 7. Before expanding beyond internal testing

The app declares background location and location/data-sync foreground services. Prepare
accurate permissions declarations, an in-app location disclosure before permission requests,
a hosted privacy policy linked inside the app, account-deletion support, a Data safety form,
and reviewer access if requested. This setup work is not a completed Play policy audit.
Internal-only apps are exempt from the Data safety section, but that is not a general exemption
from user-data obligations. Fill out only statements verified against the actual app/backend/SDKs.

For personal accounts created after November 13, 2023, production access requires a **closed**
test with at least 12 testers opted in continuously for at least 14 days, followed by an application
for production access. Internal testing does not count toward that requirement. If public launch
is your next goal, plan a closed testing track after this initial friends beta.

## References

- Testing setup: https://support.google.com/googleplay/android-developer/answer/9845334
- Personal-account testing: https://support.google.com/googleplay/android-developer/answer/14151465
- API access: https://developers.google.com/android-publisher/getting_started
- Publishing action and first manual upload: https://github.com/r0adkll/upload-google-play
- Mapbox public token: https://docs.mapbox.com/android/maps/guides/install/
- Vulkan preview limitations: https://docs.mapbox.com/android/maps/guides/
- Background location: https://support.google.com/googleplay/android-developer/answer/9799150
