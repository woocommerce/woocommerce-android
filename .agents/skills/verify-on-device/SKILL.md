---
name: verify-on-device
description: Build, install, and visually verify the app on an Android emulator or device
allowed-tools: Bash, Read, Grep, Glob, mcp__mobile-mcp__*
user-invocable: true
---

# Verify on Device

Build, install, and visually verify the app on an Android emulator or physical device using mobile-mcp.

**Prerequisites:** Node.js v22+, Android SDK with platform-tools, a running Android emulator or connected device.

## Critical Rule: Always Use the Accessibility Tree for Tapping

**NEVER estimate tap coordinates from screenshots.** Screenshots are scaled down from the actual device resolution (e.g., a 1080x2400 device produces a ~480x1065 screenshot). Coordinates derived from screenshots will be systematically wrong.

**ALWAYS follow this workflow:**
1. Call `mobile_list_elements_on_screen` to get elements with their **device-pixel coordinates**
2. Compute tap target as the **center** of the element's bounding rect: `tap_x = x + width/2`, `tap_y = y + height/2`
3. Call `mobile_click_on_screen_at_coordinates` with those computed coordinates
4. Call `mobile_take_screenshot` AFTER tapping to visually confirm the result

Only use `mobile_take_screenshot` for **visual verification** — never for deriving coordinates.

## Waiting for Screen Transitions

After every navigation action (tap, BACK press, app launch, swipe), the screen may be animating or loading data. ALWAYS follow this stabilization protocol:

1. Call `mobile_list_elements_on_screen` immediately after the action.
2. If the expected target element is NOT present, call `mobile_list_elements_on_screen` again. Each tool round-trip takes ~1-2 seconds, which provides sufficient implicit delay.
3. Repeat up to 5 times.
4. If after 5 attempts the expected element is still missing, take a screenshot for diagnosis and report the issue.

**Loading indicators to watch for:**
- Skeleton/shimmer views (animated placeholder content) — the screen is loading data, keep waiting.
- `CircularProgressIndicator` or `ProgressBar` elements — an operation is in progress, keep waiting.
- Empty state views with text like "No orders yet" — the screen IS loaded, just empty. Do NOT keep waiting.

**When NOT to retry:** If `mobile_list_elements_on_screen` returns the same result 3 times in a row with no change, the screen is stable. The element you want is genuinely not present — consider scrolling or navigating differently.

### Timing Guidelines

| Action | Expected Wait | Max Attempts |
|--------|--------------|--------------|
| App launch to dashboard | 3-8 seconds | 5 |
| Tab navigation (bottom bar) | <1 second | 3 |
| Opening a detail screen | 1-3 seconds | 4 |
| Network data load (pull to refresh) | 2-10 seconds | 8 |
| Dialog appearance after button tap | <1 second | 3 |
| Keyboard appearing after field tap | <1 second | 2 |

## Text Input Workflow

Typing text into a field requires a specific sequence:

1. **Find the input field** using `mobile_list_elements_on_screen`. Look for elements with type `EditText`, `TextField`, or hint text like "Search".
2. **Tap the field** using `mobile_click_on_screen_at_coordinates` at its center to give it focus. The soft keyboard will appear.
3. **Confirm focus** — call `mobile_list_elements_on_screen` to verify the field is focused.
4. **Type the text** using `mobile_type_keys`. Set `submit: false` unless you want to press Enter after typing.
5. **Dismiss the keyboard** if needed: call `mobile_press_button` with `BACK`. On Android, the first BACK press while the keyboard is visible dismisses the keyboard only — it does NOT navigate back. A second BACK press would navigate back.

**Common pitfall:** Calling `mobile_type_keys` without first tapping the input field types into whatever element last had focus (or nothing).

**Search fields:** The orders and products lists use a toolbar search icon. Tap the magnifying glass icon first, wait for the search field to expand, then type into the expanded field.

## Handling Unexpected Dialogs

WooCommerce may show dialogs automatically on launch or during navigation. Detect and dismiss these before proceeding.

After launching the app or navigating to a new screen, call `mobile_list_elements_on_screen` and check for:

| Dialog Type | How to Detect | How to Dismiss |
|-------------|---------------|----------------|
| **Privacy Banner** | Elements with text "Privacy Settings" or "Save" button on a bottom sheet. This is NOT cancellable — tapping outside won't work. | Tap the "Save" button. |
| **What's New / Feature Announcement** | Element with identifier containing `closeFeatureAnnouncementButton` or text "Close". | Tap the close button. |
| **App Rating Dialog** | AlertDialog with text containing "rate" or "enjoy". | Tap "No Thanks" or "Remind Me Later". |
| **Android Permission Dialog** | Elements from `com.android.permissioncontroller`, or text containing "Allow" / "Don't allow". | Tap "Allow" for testing purposes. |
| **Snackbar** | Element with identifier containing `snackbar_text` near the bottom of the screen. | Do NOT dismiss — auto-dismisses after a few seconds. May temporarily cover bottom nav tabs; if a bottom tab tap fails, wait 3-4 seconds and retry. |

**General dialog dismissal strategy:** Look for a dismiss/close/cancel button and tap it. If none visible, try `mobile_press_button` with `BACK`. If BACK doesn't work (non-cancellable dialogs), look for any actionable button ("OK", "Save", "Got it") and tap it. After dismissing, call `mobile_list_elements_on_screen` to confirm the dialog is gone.

## Finding Elements That Require Scrolling

When `mobile_list_elements_on_screen` does not return the element you expect, it may be off-screen:

1. Call `mobile_list_elements_on_screen` and check for the target element.
2. If not found, call `mobile_swipe_on_screen` with direction `up` (swipe up = scroll down) from the center of the screen.
3. Call `mobile_list_elements_on_screen` again.
4. Repeat up to 10 times. If the same elements keep appearing (no new content), you have reached the bottom of the list.
5. If still not found, try scrolling back up (direction `down`) or try an alternative navigation path.

**Tip:** To scroll within a specific scrollable container (not the full screen), use the container's center coordinates as the swipe starting point.

## Working with Element Lists

`mobile_list_elements_on_screen` can return 50-200+ elements. To find what you need:

- **By resource identifier (most reliable):** Match the `identifier` field (e.g., `com.woocommerce.android.dev:id/ordersList`). Resource IDs are stable across app versions.
- **By display text:** Match the element's `text` or `label` field. Useful for finding specific list items (e.g., order "#1234").
- **By position:** Elements are returned in document order (top to bottom, left to right). Toolbar/status bar elements appear first, list items in visual order.

**Compose vs View elements:** View-based screens have stable `com.woocommerce.android.dev:id/*` identifiers. Compose-based screens (Dashboard cards, Settings, newer screens) may lack resource IDs — rely on `contentDescription` or display text instead.

## Fresh Install vs. Upgrade

- **Fresh install** (app not previously installed, or after `mobile_uninstall_app`): Always shows the login screen. The agent cannot proceed past login without user credentials.
- **Upgrade/reinstall** (`mobile_install_app` when already installed): Session is preserved. App goes directly to the dashboard.
- **After clearing data** (`adb shell pm clear com.woocommerce.android.dev`): Same as fresh install — session destroyed, login required.

Plan your verification flow accordingly. If the user wants to test post-login features, ensure the app is already logged in.

## Steps

### 1. Discover Devices

Call `mobile_list_available_devices`. If multiple devices are returned, ask the user which to use. If none are found, instruct the user to boot an emulator or connect a device.

### 2. Build the Debug APK

```
./gradlew assembleWasabiDebug
```

If the build fails with "SDK location not found", check that `local.properties` exists and contains the `sdk.dir` path. See the Error Recovery section below.

### 3. Install the APK

Use `mobile_install_app` with:
- **path:** `WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk`

Optionally call `mobile_list_apps` first to check if the app is already installed.

### 4. Set Up API Mocks (Optional)

If the user requests verification of a specific scenario (error states, empty data, custom responses), set up ApiFaker mock endpoints **before** launching the app. See the "API Mocking with ApiFaker" section and `docs/api-faker-adb.md` for commands and workflow.

### 5. Launch the App

Use `mobile_launch_app` with:
- **packageName:** `com.woocommerce.android.dev`

### 6. Handle Post-Launch Dialogs

Call `mobile_list_elements_on_screen` to check what appeared. If a dialog or overlay is blocking the main UI, dismiss it using the guidance in "Handling Unexpected Dialogs" above. Repeat until you reach the dashboard or the expected screen.

### 7. Start Screen Recording (Optional)

If the user requests a recording or demo video, start an ADB screen recording **before** navigating:

```bash
# Start recording in the background (max 180s, Android hard limit)
adb -s <device_id> shell "screenrecord --size 720x1280 /sdcard/_agent_rec.mp4" &
```

Use `--size 720x1280` to keep the file small. The recording runs in the background while you perform navigation steps. See the Screen Recording section below for full details.

### 8. Navigate and Verify

Navigate as needed per the user's request. After each navigation action:
1. Wait for the screen to stabilize (see "Waiting for Screen Transitions").
2. Verify you arrived at the expected screen using the Key Screen Identifiers table.
3. Take a screenshot with `mobile_save_screenshot` as evidence.

If the expected screen identifier is NOT present after retries:
1. Take a screenshot with `mobile_take_screenshot`.
2. Call `mobile_list_elements_on_screen` and identify which screen you are actually on.
3. Report to the user: "Navigation to [target] failed. Currently on [detected screen]."

### 9. Stop Recording and Save Evidence

**If recording:** stop the recording, pull the file, and clean up:

```bash
adb -s <device_id> shell "pkill -l SIGINT screenrecord"
sleep 2
adb -s <device_id> pull /sdcard/_agent_rec.mp4 ./verification_recording.mp4
adb -s <device_id> shell rm /sdcard/_agent_rec.mp4
```

**Always:** use `mobile_save_screenshot` at each verification step to save screenshots to disk.

### 10. Report Results

Summarize what was verified, include saved screenshot and recording paths, and flag any issues found.

## Available MCP Tools Reference

### Device Management
| Tool | Purpose |
|------|---------|
| `mobile_list_available_devices` | Discover emulators and physical devices |
| `mobile_get_screen_size` | Get device resolution in pixels — useful to understand coordinate space |
| `mobile_get_orientation` | Check if device is in portrait or landscape |
| `mobile_set_orientation` | Switch between portrait and landscape (e.g., for tablet testing) |

### App Lifecycle
| Tool | Purpose |
|------|---------|
| `mobile_list_apps` | List installed apps — verify the app is installed before launching |
| `mobile_install_app` | Install an APK (`.apk` file) on the device |
| `mobile_uninstall_app` | Remove the app for clean install testing |
| `mobile_launch_app` | Start the app by package name |
| `mobile_terminate_app` | Force-stop the app (useful for restart/recovery) |

### Screen Interaction
| Tool | Purpose |
|------|---------|
| `mobile_list_elements_on_screen` | **Primary interaction tool.** Returns all UI elements with device-pixel coordinates. ALWAYS call this before tapping. |
| `mobile_click_on_screen_at_coordinates` | Tap at exact device-pixel coordinates |
| `mobile_double_tap_on_screen` | Double-tap (e.g., zoom in on images) |
| `mobile_long_press_on_screen_at_coordinates` | Long-press for context menus or bulk actions |
| `mobile_swipe_on_screen` | Scroll content or swipe between pages |
| `mobile_type_keys` | Type text into the currently focused input field |
| `mobile_press_button` | Press hardware/system buttons: BACK, HOME, ENTER, VOLUME_UP, VOLUME_DOWN |
| `mobile_open_url` | Open a URL in the device browser (useful for deep link testing) |

### Screenshots
| Tool | Purpose |
|------|---------|
| `mobile_take_screenshot` | Capture screen for visual verification (do NOT use for coordinate extraction) |
| `mobile_save_screenshot` | Save a screenshot to a file path for documentation |

## Screen Recording via ADB

Use `adb shell screenrecord` to capture video of navigation flows. This is useful for demo recordings, PR evidence, or reproducing bugs.

### Start Recording

Run in the background so the agent can continue navigating while recording:

```bash
adb -s <device_id> shell "screenrecord --size 720x1280 /sdcard/_agent_rec.mp4" &
```

| Option | Default | Notes |
|--------|---------|-------|
| `--size WxH` | Device native | Use `720x1280` to reduce file size |
| `--time-limit N` | 180 | Maximum seconds (Android hard limit is 180) |

### Stop Recording

**CRITICAL:** Always stop with `SIGINT`. Using `SIGKILL` or just killing the process leaves the MP4 unfinalized and unplayable.

```bash
adb -s <device_id> shell "pkill -l SIGINT screenrecord"
sleep 2
adb -s <device_id> pull /sdcard/_agent_rec.mp4 ./recording.mp4
adb -s <device_id> shell rm /sdcard/_agent_rec.mp4
```

### Flows Longer Than 3 Minutes

Android limits recordings to 180 seconds. For longer flows, chain recordings:

```bash
# Record in segments
adb -s <device_id> shell "screenrecord --time-limit 180 /sdcard/_agent_rec_1.mp4"
adb -s <device_id> pull /sdcard/_agent_rec_1.mp4 ./segment_1.mp4
adb -s <device_id> shell rm /sdcard/_agent_rec_1.mp4
# Start next segment...
```

Note: with `--time-limit`, the command blocks until done, so navigation must happen from a parallel process or between segments.

### Recording Failure Modes

| Symptom | Cause | Fix |
|---------|-------|-----|
| MP4 unplayable | Stopped with SIGKILL or device disconnected | Always use `pkill -l SIGINT screenrecord` |
| Recording stops after 3 min | Hit 180s Android limit | Use chained recordings |
| Black screen in video | DRM-protected content on screen | OS-level restriction, cannot be avoided |
| `screenrecord: not found` | Android < 4.4 | Not supported on very old devices |

## API Mocking with ApiFaker

ApiFaker intercepts API calls at the OkHttp layer and returns fake responses from a local database. Control it via ADB broadcast commands to test specific scenarios (error states, empty lists, custom data) during device verification. ApiFaker is available only in debug builds.

When the user requests verification of a specific scenario, follow this workflow:
1. Clear any existing endpoints
2. Add mock endpoint(s) for the scenario
3. Enable ApiFaker
4. Launch or navigate the app — mocked endpoints return fake responses
5. Verify the UI shows the expected behavior
6. Disable ApiFaker and clear endpoints when done

For all ADB commands, extras, API types, examples, and debugging tips, read `docs/api-faker-adb.md`.

**Key tips:**
- All am broadcast commands must include -p com.woocommerce.android.dev — without it, Android 8.0+ silently drops the broadcast.
- All actions log results to logcat under the `WCApiFaker` tag — use `adb logcat -s WCApiFaker -d` to check feedback.

## WooCommerce Navigation Reference

### Bottom Navigation Bar

The bottom nav bar has these tabs (identifiers are stable across screen sizes):

| Tab | Element Identifier | Label |
|-----|-------------------|-------|
| My Store | `com.woocommerce.android.dev:id/dashboard` | "My store" |
| Orders | `com.woocommerce.android.dev:id/orders` | "Orders" |
| Products | `com.woocommerce.android.dev:id/products` | "Products" |
| Menu | `com.woocommerce.android.dev:id/moreMenu` | "Menu" |

To navigate between tabs, find the target tab element in `mobile_list_elements_on_screen` by its `identifier` field, compute the center of its bounding rect, and tap.

**Determining the active tab:** The currently selected tab typically has a `selected: true` property in the accessibility tree. If indeterminate, take a screenshot and visually check which tab is highlighted.

### Common Navigation Patterns

- **Go back:** Call `mobile_press_button` with button `BACK`. This is the most reliable way to navigate back.
- **Dismiss the soft keyboard:** Call `mobile_press_button` with `BACK`. This only dismisses the keyboard; it does NOT navigate back. If you need to navigate back AND the keyboard is visible, press BACK twice: once to dismiss keyboard, once to navigate.
- **Pull to refresh:** Use `mobile_swipe_on_screen` with direction `down` from the middle of the screen.
- **Scroll down a list:** Use `mobile_swipe_on_screen` with direction `up` (swipe up to scroll down).
- **Open a list item:** Find the item in `mobile_list_elements_on_screen` by its text or identifier, compute center coordinates, and tap.
- **Toolbar back arrow:** Look for elements in the toolbar area (`com.woocommerce.android.dev:id/toolbar`). If the back arrow is not exposed as a separate element, use `mobile_press_button` with `BACK` instead.

### Key Screen Identifiers

| Screen | How to identify |
|--------|----------------|
| Dashboard | Element with identifier `com.woocommerce.android.dev:id/collapsing_toolbar` and label "My store" |
| Orders list | Element with identifier `com.woocommerce.android.dev:id/ordersList` |
| Order detail | Element with identifier `com.woocommerce.android.dev:id/orderDetail_container` |
| Products list | Element with identifier `com.woocommerce.android.dev:id/productsRecycler` or toolbar text "Products" |
| Menu | Elements with identifiers like `com.woocommerce.android.dev:id/moreMenu_settings` or toolbar text "Settings" |

## Error Recovery

| Problem | Solution |
|---------|----------|
| **Build fails: "SDK location not found"** | Ensure `local.properties` exists at the repo root with `sdk.dir=/path/to/Android/sdk`. Copy from the main repo if working in a worktree. |
| **Build fails: missing `secrets.properties`** | Copy from `~/.configure/woocommerce-android/secrets/` or use `defaults.properties` as a template. |
| **App not responding / blank screen** | Call `mobile_terminate_app` then `mobile_launch_app` to restart. |
| **Element not found on screen** | The screen may still be loading — follow the "Waiting for Screen Transitions" protocol. If stable, try scrolling (see "Finding Elements That Require Scrolling"). |
| **Tap lands on wrong element** | You likely used screenshot coordinates instead of element coordinates. Always use `mobile_list_elements_on_screen` and compute the center of the bounding rect. |
| **Login screen appears** | The app requires authentication. The login screen shows elements with text like "Log in" or "Enter your store address". The agent CANNOT complete login autonomously without credentials. Stop and ask the user to provide test credentials or log in manually on the emulator. |
| **App crashes on launch** | Run `adb logcat -d *:E` via Bash to check crash logs. Common cause: missing FluxC database migration. |
| **No devices found** | Run `adb devices` via Bash to check ADB connectivity. Ensure the emulator is booted or the physical device has USB debugging enabled. |
| **Recording MP4 is unplayable** | Stopped with SIGKILL instead of SIGINT. Always use `pkill -l SIGINT screenrecord` and wait 2s before pulling. |
| **Recording cuts off at 3 min** | Android's hard 180s limit. Use chained recordings for longer flows. |

### Diagnostic ADB Commands (via Bash)

When mobile-mcp tools are not giving enough information:

| Command | Purpose |
|---------|---------|
| `adb -s <device> shell dumpsys activity top \| head -20` | Identify the current foreground Activity/Fragment |
| `adb -s <device> shell dumpsys window \| grep mCurrentFocus` | Get the current window/dialog in focus |
| `adb -s <device> logcat -d *:E \| tail -30` | Check recent error logs |
| `adb -s <device> shell am force-stop com.woocommerce.android.dev` | Force kill the app |
| `adb -s <device> shell pm clear com.woocommerce.android.dev` | Clear app data (full reset — will require re-login) |
