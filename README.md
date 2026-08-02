# Minim — a minimal, text-first Android launcher

Kotlin + Jetpack Compose launcher in the spirit of Niagara Launcher: an
alphabetical, text-first app list as the primary surface, with everything
else layered on as opt-in extras rather than an always-on icon grid.

## How to open this

1. Open the `minim/` folder in Android Studio (Ladybug or newer).
2. Let it sync — Android Studio will offer to generate the Gradle wrapper
   jar automatically if it's missing (this project ships the wrapper
   *properties* file but not the binary jar, since I can't produce a binary
   in this environment). If it doesn't offer, run `gradle wrapper` once
   with any local Gradle install, or File → Sync Project with Gradle Files.
3. Run on a device/emulator, then set it as your default home app when
   prompted (or Settings → Apps → Default apps → Home app).

compileSdk/targetSdk are 36 (Android 16). minSdk is 26.

## What's implemented

**Core list & speed**
- Text-first alphabetical app list backed by a Room cache — PackageManager
  is queried once on first run, then only patched via install/uninstall/
  update broadcasts (`PackageChangeReceiver`), never polled.
- In-memory prefix search index (`AppSearchIndex`), rebuilt only when the
  app list actually changes (not on every keystroke) — search stays
  instant even with 500+ apps installed.
- Fast-scroll alphabet index bar with drag-to-jump.
- On-device smart suggestions: SQL frequency+recency query over a 21-day
  window, no ML runtime, no network.
- Recently-installed section (last 3 days, configurable window in
  `SettingsRepository`).
- Real icon rendering with an LRU-cached, off-main-thread loader — icons
  stay fully opt-in and never load unless enabled in Settings.

**Feel & aesthetics**
- Three complete, switchable design languages (Settings → Design) — not
  color swaps, each changes color scheme, corner shapes, typography, and
  panel treatment together:
  - **Nothing OS** (default): black/white + one signature red, sharp
    near-rectangular corners, monospace dot-matrix-style type with wide
    letter-spacing on labels, bordered "etched panel" surfaces.
  - **Android 16 / Material 3 Expressive**: bold color (with optional
    Material You wallpaper-driven dynamic color), large continuous
    corners, tonal surfaces.
  - **iOS 26 Liquid Glass**: translucent frosted panels with a real
    backdrop blur of your actual live wallpaper via
    `Window.setBackgroundBlurRadius` (API 31+) — not a fake screenshot
    blur — plus a soft specular highlight on each panel's top edge.
    Degrades gracefully to plain translucency (no blur) below API 31.
- Live clock/date header, ticking once a minute via a suspend loop (no
  BroadcastReceiver, no per-second recomposition).
- Dark-first true-black OLED theme, light variant, Material You dynamic
  color (Android 12+), and a curated accent-color picker for people who
  don't want wallpaper-driven color.
- Spring-based swipe animation and short haptic ticks on tap/long-press/
  swipe-reveal, gated behind a single "Haptic feedback" setting.
- Dot badges instead of numeric counts (cheaper to render, calmer to look
  at) — badge *state* itself needs the notification listener noted below.

**Useful, native-feeling features**
- Swipe-to-reveal quick actions per app (call / message / just-open),
  assigned through a real picker sheet and resolved back to an `Intent` in
  `MainActivity.runQuickAction`.
- Long-press context sheet: favorite, hide, set quick action, add to a
  space, uninstall — all wired to real repository calls, not stubs.
- Quick-settings shortcut row: torch is toggled directly via `CameraManager`
  (the one radio Android lets a third-party app flip without extra
  permission); Wi-Fi, Bluetooth, and DND open the real system panel in one
  tap, since Android has not allowed apps to flip those radios directly
  since Android 10 — see `SystemToggleController` for why faking it would
  be dishonest UX.
- "Spaces" — named, ordered app groupings (e.g. "Work", "Morning") stored
  in Room (`SpaceEntity`/`SpaceMemberEntity`, `SpacesRepository`). The data
  layer and long-press "Add to a space" hook are done; the actual space
  picker/management screen is the one piece left (see below).
- Settings export to JSON (`SettingsRepository.exportToJson`) so a
  person's setup isn't trapped on one device — shared via the standard
  Android share sheet.

**Stability**
- `CrashGuard`: a launcher crashing is uniquely bad — the user loses their
  home screen, not just one app. It installs a default uncaught-exception
  handler that logs the crash and relaunches `MainActivity` in a fresh task
  rather than silently falling back to a different installed launcher.
- Room's `fallbackToDestructiveMigration()` — a launcher should never
  crash-loop on a schema mismatch; it should just rebuild its cache.
- Single Activity, `launchMode="singleTask"`, `stateNotNeeded="true"` — a
  launcher is torn down/recreated far more than a typical app, so all real
  state lives in the ViewModel/Flow layer, not Activity fields.
- No foreground service, no polling loop anywhere in the app.

**Contextual Profiles**
- Spaces (app groupings) can now carry their own look — a Space's
  `designLanguage`/`accentName`/`themeMode` override the global Settings
  values whenever that Space is active, so activating a profile restyles
  the whole launcher, not just filters the app list.
- Manual activation: long-press the clock to open the profile switcher.
- Optional auto-activation by time window (e.g. "Work" from 9-17) —
  checked only when the launcher is opened (`MainViewModel.onResume`),
  never a background alarm or job.
- Full management screen (`SpacesActivity`): create/rename/delete spaces,
  set per-space design/accent, configure the auto-activate window, manage
  member apps.
- `ProfileSwitcherSheet` and `SpacePickerSheet` (add-app-to-space, with
  inline "create new space") round out the flow end to end.

**Gesture Engine**
- Seven recognized gestures — double-tap, swipe up, swipe down, pinch-in,
  two-finger tap, and both edge-swipes — each independently assignable to:
  nothing, lock screen, open Settings, expand notifications (best effort),
  toggle torch, open a specific app, or activate a specific profile.
- `GestureAction` is a small encodable sealed class stored per-gesture in
  `GestureRepository` (its own DataStore, separate from `SettingsRepository`
  since this is a self-contained feature).
- Edge-swipe and two-finger-tap aren't built into Compose, so
  `GestureDetectors.kt` hand-rolls both on top of `awaitPointerEventScope` —
  documented there as intentionally simple pattern-matchers, not a
  general-purpose multi-touch recognizer.
- `GesturesActivity` is the assignment UI: tap any gesture, pick a category,
  and (for "open an app" / "activate a profile") pick from a live list.
- The old fixed "double-tap to lock" / "swipe down for notifications"
  booleans in Settings are gone, fully superseded by this system.

**Widget hosting** — now real, not a placeholder box.
- `WidgetHostManager` wraps `AppWidgetHost`/`AppWidgetManager`; the "+ Add
  widget" button in the widget space launches the system's
  `ACTION_APPWIDGET_PICK` picker (the only path available to a non-system
  launcher, since `BIND_APPWIDGET` itself is a protected permission),
  handles widgets that need a configuration screen, and persists the chosen
  `appWidgetId` in Settings.
- `startListening()`/`stopListening()` are tied to `onStart()`/`onStop()`,
  so the host only receives live updates while the launcher is visible.

**Notification badges — real, not always-empty.**
- `MinimNotificationListenerService` exposes currently-notifying packages
  as a `StateFlow`. Never auto-bound — the person grants it explicitly via
  Settings → "Notification badge access", which deep-links to the system
  screen. A separate in-app toggle controls whether badges render even once
  granted.

**Calendar peek — the glanceable info line, on-device only.**
- `CalendarPeek` queries `CalendarContract` directly, no network. Settings
  → "Show next calendar event" requests `READ_CALENDAR` only when turned
  on, and reverts itself if the permission is denied.
- Weather was deliberately left out — every real source needs either an API
  key + `INTERNET` permission (breaking the zero-network story) or bundled
  stale data. If you add it anyway, swap the data source in `CalendarPeek`
  and expect to add the permission honestly.

**Icon shapes** — circle / squircle / rounded-square, a global toggle in
Settings independent of design language, wired through `HomeUiState` into
every `AppRow`.

**Biometric lock for hidden apps** — `HiddenAppsActivity` (a
`FragmentActivity`, since `BiometricPrompt` requires one) gates access
behind the device's existing lock method via `BiometricGate`, using no
launcher-specific PIN. Settings → "Lock hidden apps" toggles it; a new
Settings → "Hidden apps" row is the only way to see and unhide them (there
wasn't one before this pass — hiding was previously a one-way trip).

**Gesture engine — consolidated into one state machine.**
- The five independent `pointerInput` modifiers from the previous pass
  (each racing the others for the same touch events) are replaced by a
  single `detectAllGestures` function in `GestureDetectors.kt`: one
  `awaitEachGesture` loop that classifies an entire touch sequence exactly
  once at its end (tap/double-tap, vertical swipe, edge swipe, pinch,
  two-finger tap), instead of five separate detectors each guessing
  independently. This is the fix for the flakiness called out in the
  previous round.

## What's still stubbed

1. **Swipe-down-to-expand-notifications** — genuinely not reliably
   possible without an Accessibility Service on modern Android (the old
   `StatusBarManager` reflection trick is blocked on recent API levels).
   `promptNotificationShade()` is an intentional no-op rather than a hack
   that breaks on the next OEM update. Assignable as a gesture action
   regardless — it just won't do anything until this is solved.
2. **Glass mode on the Settings/Spaces/Gestures/HiddenApps screens**
   doesn't get the live wallpaper/blur treatment (`WindowChromeController`
   is only wired into `MainActivity`) — their colors still adapt, but the
   backdrop blur is home-screen-only for now.
3. **Auto-activated profiles only check on resume** — if the launcher is
   never reopened during a Space's time window, that Space simply never
   auto-activates that day. This is a deliberate tradeoff (no background
   alarm/job) rather than a bug, but worth knowing.
4. **Accessibility pass** — not yet done. The custom gesture-based chrome
   (double-tap, swipe, pinch on the root Box) has no TalkBack equivalent by
   nature — this is a real limitation, not an oversight, and worth deciding
   how to handle deliberately (e.g. exposing the same actions as visible
   buttons for accessibility users). `AppRow`'s tap/long-press also uses
   raw pointer input rather than `Modifier.clickable`, so it doesn't
   automatically expose click semantics to TalkBack — needs explicit
   `Modifier.semantics { onClick {...}; onLongClick {...} }` added.
5. **Widget configuration edge cases** — the pick → (optional) configure →
   save flow in `MainActivity` handles the common case, but hasn't been
   run against real widgets on a device. Some providers' configuration
   activities may behave unexpectedly with this flow; worth verifying with
   a few different widgets (e.g. a simple clock vs. something like a
   calendar agenda widget) before relying on it.
6. **Calendar peek refresh cadence** — recomputes every 15 minutes while
   the launcher is in the foreground via a `LaunchedEffect` loop, not a
   background job. If nothing changes for 15 minutes after an event
   starts/ends, the line can be briefly stale; acceptable for a glanceable
   line, but worth knowing.

## Architecture notes

- No DI framework — `MinimApplication` hand-wires five cheap singletons.
  Revisit if the dependency graph grows past what one class can wire
  cleanly.
- MVVM/MVI-ish: one `MainViewModel` exposing a single
  `StateFlow<HomeUiState>` combined from Room + DataStore flows. UI never
  touches PackageManager, DataStore, or CameraManager directly — that's
  what `SystemToggleController` and the repositories are for.
- `AppSearchIndex` memoizes against the *previous app list instance*, not
  a content hash — cheap because Room's Flow only emits a new list
  instance when the underlying data actually changed.

## MainActivity is deliberately thin

`MainActivity` used to own gesture dispatch, widget lifecycle, theme
resolution, and every bottom sheet's state directly — it had become the
kind of god object that's hard to review, test, or hand off. It's now a
wiring layer over collaborators that don't know an Activity exists:

- **`AppearanceResolver`** (`util/`) — pure function merging an active
  Contextual Profile's style overrides with global Settings. No Context, no
  Compose — genuinely unit-testable.
- **`GestureDispatcher`** (`util/`) — turns a `GestureAction` into an
  effect via injected callbacks. The "which action does what" logic is
  decoupled from Activity specifics.
- **`WidgetPickerController`** (`util/`) — owns the entire widget
  allocate → pick → configure → persist flow and its `ActivityResultLauncher`
  ceremony. `MainActivity` just calls `launchPicker()`.
- **`rememberMainScreenSettings` / `rememberGestureBindings`**
  (`ui/state/MainScreenSettings.kt`) — bundle ~19 individual
  `collectAsState()` reads into two data classes, so call sites read
  `screenSettings.showClock` instead of scrolling past a wall of `by`
  declarations.
- **`rememberCalendarPeekText`** (`ui/state/CalendarPeekState.kt`) — the
  15-minute foreground refresh loop, extracted so it's not inline in
  `setContent`.
- **`HomeOverlays`** (`ui/overlays/`) — all four contextual bottom sheets
  plus the settings entry button, as one `BoxScope` composable instead of
  ~150 lines inline.
- **`DesignLanguage.fromRaw()`/`.toRaw()`** and
  **`MinimThemeMode.fromRaw()`/`.toRaw()`** — the string↔enum mapping that
  was previously copy-pasted with slight variations across `MainActivity`,
  `SettingsActivity`, `SpacesActivity`, `GesturesActivity`, and
  `HiddenAppsActivity` now lives once, on the enums themselves.

What's left in `MainActivity` is genuinely Activity-shaped: `onCreate`/
`onStart`/`onStop` lifecycle, wiring the collaborators together once, and
the handful of methods (`launchApp`, `lockScreen`, `requestUninstall`) that
need `Context`/`PackageManager`/`Intent` directly. Nothing here has been
run through an actual compiler yet — see the balance/cross-reference checks
in this session's history for what *was* verified without one.

