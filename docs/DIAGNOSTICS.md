# Production diagnostics

Release builds collect battery intervals and Timber WARN/ERROR/ASSERT reports.
Debug builds keep ordinary Logcat logging and do not collect or upload diagnostics.

## Battery intervals

Reports contain the effective tracking mode (`charging`, `car`, `foreground`,
`background`, or `inactive`), elapsed duration, starting/ending battery
percentage, charging state, and charge-counter readings when the device supports
them. Mode and charging changes close the previous interval. A 15-minute sample
closes long intervals while the application process is running. No wake lock,
alarm, or additional location request is used.

These are **whole-device battery changes during a mode**, not an estimate of
energy consumed exclusively by this app. Charging intervals must be analyzed
separately from discharging intervals. Missing charge counters are omitted.
Process death ends sampling; intervals are not extrapolated across restarts.

## Failures

Existing production Timber warnings/errors, unsuccessful HTTP responses,
transport failures, and uncaught JVM exceptions enter the reporting path.
Handled failures that previously only produced UI errors now log safe operation
names. Expected coroutine cancellation and normal provider unavailability remain
excluded. The JVM exception handler does not run for native crashes or ANRs.

On Android 11 and newer, the next production startup also reads up to 16 entries
from [Android's process exit history](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,%20int,%20int)).
Native crashes and processes terminated for ANRs produce reason-only summaries
with the original exit timestamp. No process names, descriptions, traces, memory
measurements, package names, or process IDs are sent. Historical app version and
tracking mode are recorded as `unknown`. A local timestamp marker avoids
re-enqueueing old summaries; deterministic event IDs also deduplicate retries.
The marker follows best-effort enqueueing, so termination before queue persistence
can lose a summary. Android retains only limited recent history. Recovered ANRs,
force-stops, ordinary OS process kills, and native/ANR failures on older Android
versions are not collected by this path.

Callers must log fixed operation names and non-personal metadata. Throwable
messages/causes, request URLs, bodies, authorization tokens, coordinates, and
user-entered text must never be included. Reports retain exception class and
bounded code stack frames (up to 16 frames and 2048 ASCII characters). App-package
frames are preferred. If none are identifiable, the original frames are retained
within those limits; this fallback can include obfuscated app, library, and platform
frames. Keep the matching release mapping file to retrace obfuscated frames.
Basic redaction provides additional protection, not permission to log arbitrary
content. The crash handler synchronously saves one
small crash journal before invoking Android's existing handler; upload occurs
after a later startup.

## Delivery and storage

`POST /me/telemetry` accepts an authenticated JSON batch of at most 10 events.
The client also limits serialized batches to 15 KiB, below the server's 16 KiB
request limit. UUID event IDs make retries idempotent. The server derives the
account from the validated session; client-supplied ownership is never accepted.

Room stores up to 500 pending events for up to seven days. Each account keeps its
own pending reports across sign-out; only reports with matching account ownership
are selected for upload. Reports without known ownership stay local and expire
under the same retention and size limits. Sign-out clears cached friends and
locations while retaining the bounded diagnostics queue.
Delivery runs while the application process is alive and drains up to five
batches per wake, normally once per minute. It backs off to 15 minutes after
failure. The server also limits each account to five batches per minute per
server process, with a bounded limiter map; rejected batches receive HTTP 429. The dedicated client does not refresh
sessions, log itself, or recursively generate diagnostics. Invalid payloads are
discarded; offline, authentication, rate-limit, and server failures retain the
batch. Disk failure, queue limits, OS termination before asynchronous persistence,
and expiration can drop nonfatal reports.

The backend stores events separately in `telemetry_events`, with a unique
`(user_id, event_id)` index and a 30-day TTL based on server receipt time.
MongoDB TTL deletion is asynchronous. Deploy the backend endpoint before enabling
the corresponding release; this change does not deploy either application.

## Database upgrades

The exported Room version 4 schema is the baseline. Version 5 adds only the
diagnostics queue through `MIGRATION_4_5`; existing friends and user locations are
preserved. Destructive fallback has been removed. Future schema changes must
increment the version, preserve existing data through registered migrations,
commit exported schemas, and include preservation tests as required by
`AGENTS.md`. Earlier unexported schemas must be reconstructed from their actual
released definitions before adding migration paths; do not guess or reset data.
