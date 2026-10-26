# Car mode

Enable or disable car mode in Settings. The map also suggests enabling it after a
fresh, measured speed exceeds 30 km/h. Accepting the dialog saves the setting;
dismissing it suppresses further suggestions for one hour in the current session.
The preference survives app restarts and is cleared with the session at logout.

Car mode shows a speed card, follows GPS course with a 60-degree camera pitch, and
positions the vehicle below the map center to leave room for the road ahead. Road
labels stay visible; POI/transit/pedestrian labels and 3D objects are hidden. Panning
and rotating still interrupt camera following; Center and Zoom restore the driving
camera. Leaving car mode restores the normal map style, camera, and location provider.

Mapbox's standard `DefaultLocationProvider` handles puck and camera bearing.
Car mode selects `PuckBearing.COURSE`, which uses the acquired GPS movement vector;
normal mode selects `PuckBearing.HEADING`, which uses device orientation. The
standard provider receives fixes from `LocationServiceProxy` through Mapbox's
supported device-provider factory. It does not start a second GPS request.
The former car-specific map provider has been removed. Road snapping changes only
the displayed coordinates; bearing, speed, timestamps, and uploaded fixes remain
the original measurements.

The user and friends in car mode have a minimal car icon inside their map marker.
The marker keeps its existing color, accuracy/direction ring, and click behavior.
Leaving car mode restores the user's plain dot or the friend's name. Car icons
share the existing bounded bitmap cache; friend images change only when their mode
changes or the map style reloads, without restarting marker movement animations.

Friends' direction rings use the sender's GPS course, including in car mode; the
phone compass never supplies a remote marker's bearing. Android's built-in
`Location.bearing`/`hasBearing()` supply the course and Mapbox's native symbol
`iconRotate` renders it relative to the map. A valid northbound course (`0°`)
survives uploads, MongoDB, friend responses, and the local cache. An unavailable
course remains null and hides the direction notch instead of pointing north.
This uses the existing location stream, marker tween, and partial feature updates.

Mapbox Map Matching uses recent GPS traces to position the vehicle on a road. Only
confident, recent matches within the fix's uncertainty are displayed. Missing,
stale, or failed matches fall back to GPS. Measurements and fix timestamps sent to
our backend remain the provider's originals. The active navigation target is also
matched when that friend reports car mode; other friends keep their reported positions.

When tracking a driver, the navigation card uses their live friend record and shows
their speed and `your speed − their speed`. Outside car mode, the estimate is a
drive to their latest position using Mapbox Directions with the driving/traffic
profile. When both users are in car mode, it predicts an interception instead:

- The driver continues along the current road at their reported speed. The age of
  their fix is included before predicting where they will be.
- A bearing-constrained Directions request supplies the road's actual geometry.
  Prediction follows bends, road-name changes, and identifiable continuations;
  it stops before an uncertain turn, exit, or end of road. It never treats the end
  of that geometry as a stop by the driver. Lookahead is capped at 15 minutes and
  30 km of road.
- Batched Matrix road distances determine whether you can reach each predicted
  point before the driver, using your measured speed. A second bounded batch
  verifies and refines the first crossing. On the same road this gives
  `gap / (your speed − driver speed)` when following and
  `gap / (your speed + driver speed)` when approaching.
- The card shows the estimated interception time and road distance. If no
  interception is found in the road lookahead, it instead shows your arrival time
  at a projected point and how long after the driver passes you would arrive.
  A stopped driver is a stationary road destination; a stopped follower can wait
  for a driver whose predicted road passes their position.

These are estimates assuming the current road and speeds, not a known future
destination or turn-by-turn guidance. Missing measurements remain unknown; zero
means a measured stop. Stale locations, unavailable road data, and material speed
or heading changes suppress the old prediction. Predictions count down from their
request time using ordinary location updates, without adding a UI timer. Changing
mode or navigation target also rejects queued results from the previous request.

The external API contracts are documented by Mapbox's
[Directions API](https://docs.mapbox.com/api/navigation/directions/) and
[Matrix API](https://docs.mapbox.com/api/navigation/matrix/).

## Refresh and resource use

| State | Requested GPS interval | Upload pacing |
| --- | --- | --- |
| Charging/full battery | 0 ms, fastest supported, no batching | At most one per second |
| Car mode, foreground or background | 1 second, no batching | At most one per second |
| Normal foreground | 5 seconds, no batching | At most one per 5 seconds |
| Background | Existing 10-minute request | At most one per 5 seconds when fixes arrive |

Friends polling uses a one-second interval while connected to power (charging or
full battery) and the existing 15-second interval otherwise. Plugging in or
unplugging immediately restarts polling with the new interval; retries use the
same interval. Each delay follows completion of the previous fetch, so slow
requests do not overlap. Power observation runs with the friends subscription
independently of the location service and stops with polling.

`LocationForegroundService` owns one state and resolves acquisition priority as
Charging → Car Mode → Foreground → Background. Activity visibility, charging,
the saved car preference, forced foreground, and sharing all enter the same
evaluation path. A changed request restarts acquisition; a sharing change only
starts or stops the uploader when the location request is unchanged. On unplugging,
the service returns to Car Mode when enabled, otherwise current Activity visibility
and forced foreground determine Foreground or Background. A charging change never
starts a stopped service or enables sharing.

With sharing paused, the service supplies local fixes to the visible map in every
mode, but uploads nothing to our backend. Local acquisition stops when the Activity
enters background. Car Mode continues its one-second acquisition in background
when sharing is enabled. Mapbox receives fixes from the same service and
keeps its standard animation and compass/course handling.

There is one conflated upload queue and one uploader. Location consumers receive
the service's latest fix without artificial delays; `StateFlow` conflation keeps
slow consumers from building a queue of obsolete fixes. Friend mapping and
proximity calculations run on the computation dispatcher. Road
requests run only while the map is visible and car mode or driver tracking needs
them: matching is paced by a two-second loop, and routes by a 15-second minimum
interval. Traces are bounded to 12 fixes. Freshness checks cause UI updates only
when readings become fresh or stale. The SDK animates the puck and camera; no
additional frame loop or GPS subscription is created for the driving puck.

Interception geometry and arithmetic run on the computation dispatcher. Each
15-second estimate cycle uses at most one forward-road request and two small,
single-origin distance batches (at most 11 coordinates), with an eight-second
total prediction budget. A stationary driver needs only one route request.
Missing or invalid measurements issue no prediction requests. Leaving the map
cancels all this work; prediction creates no additional GPS subscription.

The navigation API client is separate from the application API client and uses
the existing `mapbox_access_token` resource. Road matching and road estimates need
network access and a Mapbox token that can access those APIs. See the existing
[credential configuration finding](MAP_PERFORMANCE.md#credential-configuration-finding)
when configuring the runtime token.

## Backend contract and rollout

`PUT /me/location` includes `car_mode: Boolean`. Eligible accepted-friend locations
from `GET /me/friends` include the same field. `speed` is meters per second and may
be null or omitted in uploads when the provider has no measurement; friend
responses include null for unavailable speed. MongoDB stores one nullable speed
value without a separate presence flag, preserving measured zero. `bearing` is
GPS course in degrees and may be null when
unavailable; `0` means north. Mode and measurements belong to the original fix and
retain the existing atomic timestamp guard, including retries. Pending and paused
friendships retain their existing location privacy rules.

Deploy the Android and backend changes together. Room schema version 4 uses the
project's existing cache-reset upgrade policy; preferences are stored separately.
No MongoDB index or database wipe is needed for the added location fields.

Automated validation covers consent, setting persistence, speed freshness, live
driver tracking, closing/approaching/equal speeds, arrival lateness, road bends and
exit boundaries, fix age, prediction pacing/timeouts, request cancellation, road
matching failures, charging priority,
wire/cache serialization, shared location delivery, and MongoDB timestamp guards. Before release, verify
road-following and camera transitions on a phone, including unplugging while the
Activity is stopped, pausing sharing, changing drivers, GPS loss, and network loss.
Device frame timing and road snapping cannot be validated by JVM tests alone.
