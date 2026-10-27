# Points of interest

Long press an empty place on the map to create a point with a name and optional
description. Each user can save up to five points. A flag control opens the point
manager, shows the number of occupied slots, and toggles the separate points layer.
Your own points are listed there for easy selection, editing, and deletion.

The map renders flags in the creator's marker color, with the point name above
each flag. Tap a flag to see its name, creator username, creation date/time in the
device's locale and time zone, and description. Owners can edit the name and
description or confirm deletion to free a slot. Failed saves retain the draft;
failed deletions retain the point so the operation can be retried.

Navigate uses the existing in-app compass and geographic distance to the saved
coordinates. It shares the existing navigation card and cancellation control with
friend navigation. Starting point navigation replaces the friend target and vice
versa. Point navigation adds no driving/interception request or GPS subscription.

Saved points are visible to their creator and accepted friends independently of
live location sharing. Pausing sharing leaves points visible. Pending users do not
receive points, and a former friend's points disappear on the next friends refresh.
The layer toggle affects only display and does not change sharing or delete data.

## Data and rollout

Points are persisted by the Go backend. Own points load when the map becomes
visible and can be refreshed in the manager; successful mutations update the map
immediately. Friends' points arrive through the existing paginated friends API and
use its existing one-second powered / 15-second unplugged polling intervals.
There is no additional points polling loop. Points are not stored in the Room
friend cache, so a fresh server response is needed after restarting the app.

The backend enforces the five-point cap atomically and permits changes only by the
authenticated creator. Names allow up to 64 Unicode characters and descriptions up
to 1000. IDs, creator usernames, and creation timestamps are server metadata.
Editing preserves the original creation date.

Deploy the backend's `/me/points` CRUD routes and extended `/me/friends` response
before releasing the Android client. No Room schema change or database reset is
required. Backend API documentation and production-protection checks are recorded
in the backend repository's `docs/points-of-interest.md`. Live Cloudflare/Hetzner
checks and production deployment have not been performed by this implementation.

Validation covers creation, the five-point cap, editing/deletion ownership,
failure/retry states, refresh/mutation ordering, navigation coordinates and camera
bearing, paused-sharing independence, pending privacy, Unicode limits, and server
metadata. Compose dialogs have themed previews independent of live services.
