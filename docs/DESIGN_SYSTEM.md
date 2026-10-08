# WhereIsEveryone UI design system

The app uses its existing Material 3 theme and feature ViewModels. Green remains the
primary action color, teal supports secondary surfaces, and dark mode retains the
lime accent. Components use semantic color roles rather than raw red, green, or gray.
No dependency or navigation route is added by the UI revision.

## Foundations

| Foundation | Definition | Use |
| --- | --- | --- |
| Spacing | `AppSpacing`: 4, 8, 12, 16, 24, 32, 48 dp | 16 dp between controls; 24 dp inside forms and dialogs |
| Touch targets | `AppSize.touchTarget`: 48 dp | Icon buttons, selectable and toggleable rows |
| Buttons | `AppSize.button`: minimum 52 dp | Let height grow when labels wrap or text scales |
| Icons | 24 dp; avatars 40 dp | Keep glyph size separate from touch target size |
| Content width | Forms 440 dp; lists/settings 640 dp; dialogs 560 dp | Center content and preserve readable line lengths on wider screens |
| Shapes | 4, 8, 12, 20, 28 dp | Medium for fields/buttons, large for cards/toolbars, extra large for dialogs |
| Elevation | Floating controls 4 dp; dialogs 6 dp | Regular cards use surface color and borders instead of heavy shadows |
| Type | Material roles, default sans serif, explicit line heights | Headlines for screen/dialog titles, title medium for rows/sections, body for supporting text, label large for actions |

Definitions live in `core/src/main/java/com/kumpello/whereiseveryone/core/ui/theme/`.
`WhereIsEveryoneTheme` installs the typography and shapes as well as the full color
scheme, including surface containers, inverse colors, and fixed accent roles used by
Material components. Fixed roles stay the same in light and dark themes.

Use `surfaceContainerLow` for grouped content, `surfaceContainerHigh` for dialogs and
floating controls, and their matching `onSurface`/`onSurfaceVariant` text roles. Use
`primaryContainer` with `onPrimaryContainer` for small accents such as request counts
and avatars. Error actions use `error`; warning panels use `errorContainer` with
`onErrorContainer`. Disabled controls retain Material's disabled styling.

The light primary green and tertiary green are slightly darker for readable text.
Dark outlines are brighter so input boundaries are visible, and dark error actions
use a darker foreground. `ColorContrastTest` guards a 4.5:1 minimum for the text pairs
used by the UI and 3:1 for control outlines. These thresholds follow Android's
[color guidance](https://developer.android.com/design/ui/mobile/guides/styles/color).

## Shared components and behavior

- `Button.Primary`: the main action, with native ripple, a subtle press animation,
  and optional progress feedback. Loading disables repeated clicks.
- `Button.Secondary`: an outlined alternate action, optionally with an icon or error
  styling for a destructive action. Button labels wrap rather than being clipped.
- `TextField.Regular` and `TextField.Password`: integrated Material labels, consistent
  shapes, single-line defaults, and keyboard actions. Status editing explicitly allows
  multiple lines. Password controls announce whether they show or hide the password.
- `RememberPasswordToggle`: the full row toggles a single checkbox accessibility node.
- `ScreenHeader`: a visible return-to-map action and a semantic heading.
- `AppDialog`: shared shape, width, surface, margins, and scrollable content. Actions
  remain reachable when the keyboard or large text reduces available space.
- `AuthLayout`: a centered, width-limited, scrolling form. The existing wordmark gets
  a light monochrome treatment on dark backgrounds so it remains visible.
- Map toolbars group related controls and offer native hover/long-press tooltips.
  Navigation keeps its swipe-to-dismiss gesture and also exposes a close button.

Prefer Material interaction components and one semantic click/toggle target per
control. Decorative icons have no content description; icon-only actions have a
localized description. Minimum target sizing and parent-row selection follow the
[Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults).
Tooltip behavior follows the [Compose tooltip guidance](https://developer.android.com/develop/ui/compose/components/tooltip).

## Audit and changes

| Finding | Revision |
| --- | --- |
| Shape definitions existed but were not installed in the theme | Install the complete shape scale in MaterialTheme |
| Typography overrode one positional display style with serif text | Define every Material typography role explicitly |
| Default Material surface/inverse roles could introduce off-palette colors | Specify those roles for both themes |
| Button labels reached 26 sp, with fixed heights and a 30% shrink animation | Use semantic action variants, minimum heights, wrapping labels, and a 2% press animation |
| Field labels were separate text nodes; usernames could accept multiple lines | Use integrated labels, single-line fields, and Next/Done keyboard actions |
| Remember-password text was outside the toggle hit area | Make the whole labeled row toggleable |
| Friend rows used large red/green icons and competing fill-max-size children | Use themed 24 dp icons in 48 dp targets and a weighted username column |
| Request tabs blinked and click selection did not move the pager | Use stable counts and one PagerState for both clicks and swipes; filter by the actual page |
| Friend lists had no empty-state guidance | Add a distinct explanation for each category |
| Friends/Settings lacked visible back navigation | Add shared headers using the existing navigation stack |
| The friend action panel had a fixed 200 dp height | Let content size itself and scroll the panel when it exceeds 45% of available screen height |
| Settings was a non-scrolling column of equally prominent buttons | Group settings, use switches for on/off states, and scroll the content |
| Dialog widths, shapes, margins, and action sizes varied | Reuse one scrollable dialog surface and flexible actions |
| The precise-location dialog placed its action row outside its content column | Keep warnings, options, and actions in one coherent layout |
| Status editing was anchored at a fixed map offset and failures had no visible feedback | Use a dismissible dialog and show the existing error action as a toast |
| QR generation ran during composition | Generate it in a cancellable Compose effect on Default and show progress |
| The original wordmark's dark lettering disappeared in dark mode | Apply a light tint at display time without changing the source asset |
| Map avatar text was always white even on light marker colors | Choose a black or white foreground by luminance while keeping the marker colors and full names |

The map, friend requests, sharing controls, NFC/QR flow, authentication validation,
saved credentials, location settings, and backend contracts retain their existing
ViewModel events. New UI behavior is limited to clearer navigation, keyboard actions,
scrolling, tab synchronization, tooltips, empty states, and existing error feedback.

## Verification

Run `./gradlew test lint assembleDebug --continue --max-workers=2` after UI changes.
The unit suite includes palette contrast checks. `FriendsListContentTest` covers
category selection through clicks and swipes and the three empty states; these are
instrumented tests and need a device or emulator to execute.

For a visual review, use the light/dark Compose previews and check a narrow phone,
landscape, a wide screen, 200% text size, keyboard visibility, long usernames/statuses,
and all dialogs. Build/lint checks do not replace this device review.
