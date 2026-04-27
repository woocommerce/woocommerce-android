---
name: verify-on-device
description: Build, install, and visually verify the app on an Android emulator or device. Uses the Android CLI for agents (android) when available with a full mobile-mcp/adb fallback.
allowed-tools: Bash, Read, Grep, Glob, mcp__mobile-mcp__*
user-invocable: true
context: fork
---

# Verify on Device

Build, install, and visually verify the app on an Android emulator or physical device. The skill prefers the Android CLI for agents (`android`) when installed and falls back to mobile-mcp + adb otherwise.

**Prerequisites:** Node.js v22+, Android SDK with platform-tools, and an emulator or device to target (step 0 can provision one when the Android CLI is installed).

**Optional:** Google's Android CLI for agents (`android`). When present, the skill uses `android run` for combined install+launch, `android layout --diff` for low-token screen-transition polling, and `android docs` for platform-API lookups. If not installed, every step falls back to the mobile-mcp / adb path with no behavior change.

## Detect the Android CLI Once Per Session

Run this probe once at the start of a verification task and cache the result. Every CLI-based block in this skill is gated on `USE_ANDROID_CLI=1`.

The probe validates that `android` on PATH is Google's agent CLI (prints a semver like `0.7.15232955`) and not the deprecated Android SDK `android` tool from `tools/`, which shadows it whenever the legacy SDK tools dir is on PATH. The probe only detects — it does not mutate PATH or create symlinks, since an in-script `export PATH` would not survive subsequent shell invocations in this skill. If `android` is missing, shadowed, or broken, it sets `USE_ANDROID_CLI=0` and prints a hint pointing at the canonical install location (`~/.android/bin/android-cli`) when present. Exact PATH/symlink fix depends on the user's environment — the goal is to make `android --version` resolve to the agent CLI; the user applies the fix once in their shell rc so it persists.

The probe also writes `USE_ANDROID_CLI` to `/tmp/.verify_on_device.env`. Each subsequent `Bash` tool call starts a fresh shell, so a plain shell variable would be lost between invocations — every gated block in this skill begins by sourcing that file to restore the cached result.

```bash
_android_is_agent_cli() {
  android --version 2>&1 | grep -qE '^[0-9]+\.[0-9]+\.'
}

if command -v android >/dev/null 2>&1 && _android_is_agent_cli; then
  USE_ANDROID_CLI=1
  android --version   # log for reproducibility
else
  USE_ANDROID_CLI=0
  if [ -x "$HOME/.android/bin/android-cli" ]; then
    echo 'NOTE: Agent CLI binary is at ~/.android/bin/android-cli but `android` does not resolve to it (likely missing symlink or shadowed by the legacy Android SDK tool).'
    echo 'Fix: update your PATH/symlinks until `android --version` prints a semver (e.g. symlink android-cli to a name `android` on a directory earlier in PATH than the legacy Android SDK tools), then restart the session.'
  fi
fi

# Persist for subsequent Bash invocations (shell state does not survive
# across separate Bash tool calls). Subsequent gated blocks `source` this.
echo "USE_ANDROID_CLI=$USE_ANDROID_CLI" > /tmp/.verify_on_device.env
```

If `USE_ANDROID_CLI=0`, follow the fallback blocks (labelled "Fallback") throughout this skill.

## Verification Status of Agent-CLI Blocks

The `USE_ANDROID_CLI=0` fallback paths in this skill (mobile-mcp + adb) have been exercised end-to-end against this repo. The `USE_ANDROID_CLI=1` blocks were all re-run against agent CLI 0.7.15232955 on 2026-04-21 against this repo. Results:

| CLI block | Status |
|---|---|
| `android run` (step 7 install + launch) | Verified — installs and launches in one call. |
| `android layout --diff` (screen-transition polling) | Verified — captured the `ordersList` transition on a tab switch; diff JSON is a small fraction of a full layout dump. *Note: `--device=<device_id>` was added to the documented invocation post-verification (multi-device fix); the flag is documented by the CLI but the new combination has not been re-run end-to-end.* |
| `android screen capture --annotate` + `screen resolve` (Option B tap) | Verified — both short (`-a`/`-o`) and long (`--annotate`/`--output=…`) flag forms work. *Note: `--device=<device_id>` was added to the documented invocation post-verification (multi-device fix); the flag is documented by the CLI but the new combination has not been re-run end-to-end.* |
| `android docs search` / `docs fetch` | Verified — first invocation auto-downloads a knowledge-base zip (~one-time, a few seconds). |
| `android emulator list` (step 0 lifecycle) | Partial — `list` runs end-to-end. `create`/`start`/`stop` shape confirmed via `--help` only; no AVD was created during verification, so step 0 is flagged **Experimental** in its heading. |
| `android describe` | **Rejected.** Output is multi-line plain text (not JSON, not paths-to-JSON). Requires `ANDROID_HOME` set; produces listings only after a build. Replaced with `find` in step 7. |

If a CLI block fails in practice, **do not assume the docs are right**. Fall back to the `USE_ANDROID_CLI=0` path for that step, file the discrepancy as a skill issue, and fix it in the skill before the next run.

## Critical Rule: Default to Main App (Store Management)

Unless the task explicitly mentions **POS**, **Point of Sale**, or **WooPos**, always operate in the **main app** (store management) context — `MainActivity` with bottom navigation tabs. This applies to all workflows: creating orders, viewing products, collecting payments, etc. The main app is the default; POS is only used when specifically requested.

## Critical Rule: Always Restart the App

Do NOT attempt to recover from the current screen state when you start a task. Always force-stop the app and relaunch it to start from a known state (the dashboard or POS). This avoids wasted time navigating out of unknown screens.

```bash
adb -s <device_id> shell am force-stop com.woocommerce.android.dev
adb -s <device_id> shell am start -n com.woocommerce.android.dev/com.woocommerce.android.ui.main.MainActivity
```

For POS tasks (only when explicitly requested):
```bash
adb -s <device_id> shell am force-stop com.woocommerce.android.dev
adb -s <device_id> shell am start -n com.woocommerce.android.dev/com.woocommerce.android.ui.woopos.root.WooPosActivity
```

## Critical Rule: Never Estimate Tap Coordinates From Raw Screenshots

**NEVER estimate tap coordinates directly from a raw (un-annotated) screenshot.** Screenshots are scaled down from the actual device resolution (e.g., a 1080x2400 device produces a ~480x1065 screenshot). Coordinates derived from raw screenshots will be systematically wrong.

Use one of the two workflows below. Both translate a human-readable target (an accessibility label, a visual element) into exact device-pixel coordinates — neither relies on pixel-measuring a screenshot.

### Option A — Accessibility-tree workflow (default)

1. Call `mobile_list_elements_on_screen` to get elements with their **device-pixel coordinates**
2. Compute tap target as the **center** of the element's bounding rect: `tap_x = x + width/2`, `tap_y = y + height/2`
3. Call `mobile_click_on_screen_at_coordinates` with those computed coordinates
4. Call `mobile_take_screenshot` AFTER tapping to visually confirm the result

Only use `mobile_take_screenshot` for **visual verification** — never for deriving coordinates.

### Option B — Visual-label workflow (`USE_ANDROID_CLI=1`)

Useful when an element lacks an accessibility label or test tag, or when you already have an annotated screenshot in context. `android screen capture --annotate` overlays numeric labels (#1, #2, ...) on every interactive element; `android screen resolve` substitutes `#N` placeholders in a template string with the element's device-pixel `x y` coordinates.

Pass `--device=<device_id>` to every `android screen ...` call so the capture is taken from the same device the resolved tap will be piped to. Without it, the CLI may pick a different connected device than the `adb -s <device_id> shell` recipient and you get coordinates from one device applied to another.

```bash
# Capture an annotated screenshot — each interactive element gets a number.
android screen capture --device=<device_id> --annotate --output=/tmp/ui.png

# Idiomatic: let resolve produce a complete `input tap X Y` command and pipe
# it straight to `adb shell`. The CLI replaces `#5` with the resolved coords.
android screen resolve --device=<device_id> --screenshot=/tmp/ui.png --string="input tap #5" \
  | adb -s <device_id> shell

# Alternative: capture just the coordinates and feed mobile-mcp's tap tool.
COORDS=$(android screen resolve --device=<device_id> --screenshot=/tmp/ui.png --string="#5")
# $COORDS is now "<x> <y>"; call mobile_click_on_screen_at_coordinates with those.
```

Option A remains the default — accessibility-tree coordinates are stable and don't require visual inspection. Reach for Option B when Option A does not surface the element you need.

## Waiting for Screen Transitions

After every navigation action (tap, BACK press, app launch, swipe), the screen may be animating or loading data. ALWAYS follow one of the two stabilization protocols below.

### Preferred: Diff-Based Polling (`USE_ANDROID_CLI=1`)

`android layout --diff` returns only the elements that changed since the last snapshot, instead of re-reading the entire accessibility tree (50-200+ elements per call). This is the single biggest token-consumption win over repeated `mobile_list_elements_on_screen` calls — measure on your own flow to confirm the magnitude.

Always pass `--device=<device_id>` to keep these calls pinned to the same device chosen in step 1; without it, `android layout` may target a different connected device than the one the app was launched on.

```bash
# Baseline snapshot immediately after the action.
android layout --device=<device_id> --pretty --output=/tmp/layout_t0.json

# Poll diffs until the expected target appears (1 second between polls).
for i in 1 2 3 4 5; do
  android layout --device=<device_id> --diff --output=/tmp/layout_diff.json
  grep -q '"resource-id":"com.woocommerce.android.dev:id/ordersList"' /tmp/layout_diff.json && break
  sleep 1
done

# Safety net on the 5th failed diff — one full-layout read before giving up.
android layout --device=<device_id> --pretty --output=/tmp/layout_final.json
```

Replace the `grep` pattern with the `resource-id`, Compose test tag, or `content-description` of the screen you expect to land on (see the WooCommerce Navigation Reference).

### Fallback: Repeated Layout Reads (no `android` CLI)

1. Call `mobile_list_elements_on_screen` after the action.
2. If the expected target element is NOT present, call `mobile_list_elements_on_screen` again. Each tool round-trip takes ~1-2 seconds, which provides sufficient implicit delay.
3. Repeat up to 5 times.
4. If after 5 attempts the expected element is still missing, take a screenshot for diagnosis and report the issue.

### Guidance That Applies to Both Paths

**Loading indicators to watch for:**
- Skeleton/shimmer views (animated placeholder content) — the screen is loading data, keep waiting.
- `CircularProgressIndicator` or `ProgressBar` elements — an operation is in progress, keep waiting.
- Empty state views with text like "No orders yet" — the screen IS loaded, just empty. Do NOT keep waiting.

**When NOT to retry:** If the layout (full or diff) returns the same result 3 times in a row with no change, the screen is stable. The element you want is genuinely not present — consider scrolling or navigating differently.

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
| **Store Name Dialog** | Text "Name your store" (id: `nameYourStoreDialogFragment`). | Tap "Save" or dismiss. |
| **Create Test Order Dialog** | Text related to test order creation. | Tap "Dismiss" or "Create". |

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

**Shortcut:** If the app is already installed and logged in, skip to step 6 (Set Up API Mocks) to cover cases where a mock response is required.

### 0. Provision an Emulator (Optional, macOS/Linux only — Experimental)

Only run this step when no physical device is attached, no emulator is running, and the task requires a clean-slate device. Skip otherwise.

> **Status: experimental.** Of the three emulator subcommands used here, only `android emulator list` has been exercised end-to-end against this repo. `android emulator create`, `start`, and `stop` were shape-confirmed via `--help` only — no AVD was created during PR verification. Expect to iterate on this step the first time you actually use it; if it fails, fall back to booting the emulator manually via Android Studio or `emulator -avd <name>` and continue from step 1.

```bash
. /tmp/.verify_on_device.env  # restore USE_ANDROID_CLI from the probe step

# `android emulator` is disabled on Windows per Google's docs — detect Git
# Bash / MSYS / Cygwin and skip there. macOS and Linux both reach the `then`.
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) IS_WINDOWS=1 ;; *) IS_WINDOWS=0 ;; esac

if [ "$USE_ANDROID_CLI" = "1" ] && [ "$IS_WINDOWS" = "0" ]; then
  # List existing AVDs; reuse one if it already fits the task.
  android emulator list

  # Create a device from a profile. The CLI only accepts --profile and
  # --list-profiles — the profile name doubles as the device name.
  android emulator create --profile=medium_phone

  # Start the device using that profile name. `android emulator start`
  # returns once the command is issued — use `adb wait-for-device` plus a
  # boot-completed check before step 1, or `mobile_list_available_devices`
  # will race the boot sequence.
  android emulator start medium_phone
  adb wait-for-device
  until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
fi
```

At the end of the task, if this session created the AVD, stop it using the device serial (`adb devices` or `android emulator list` will print it):

```bash
. /tmp/.verify_on_device.env  # restore USE_ANDROID_CLI from the probe step

if [ "$USE_ANDROID_CLI" = "1" ]; then
  android emulator stop <device-serial-number>
fi
```

**Fallback (no `android` CLI):** continue to step 1 and assume a running device, as before.

### 1. Discover Devices

Call `mobile_list_available_devices`. If multiple devices are returned, ask the user which to use. If none are found, instruct the user to boot an emulator or connect a device.

### 2. Prepare the Device

Run these ADB commands to configure the device for reliable agent interaction:

```bash
# Disable animations (prevents flaky element detection during transitions)
adb -s <device_id> shell settings put global animator_duration_scale 0
adb -s <device_id> shell settings put global transition_animation_scale 0
adb -s <device_id> shell settings put global window_animation_scale 0
```

### 3. Disable LeakCanary

LeakCanary shows leak detection notifications and dialogs that interfere with agent verification. Disable it before building by setting the flag in `developer.properties` (a git-ignored local config file):

```bash
# Ensure developer.properties exists and has LeakCanary disabled
touch developer.properties
grep -q "enable_leak_canary" developer.properties && sed -i '' 's/enable_leak_canary=.*/enable_leak_canary=false/' developer.properties || echo "enable_leak_canary=false" >> developer.properties
```

### 4. Build the Debug APK

```
./gradlew assembleWasabiDebug
```

If the build fails with "SDK location not found", check that `local.properties` exists and contains the `sdk.dir` path. See the Error Recovery section below.

### 5. Install the APK

**Preferred path (`USE_ANDROID_CLI=1`):** skip this step. The single `android run` call in step 7 installs and launches in one shot — APK resolution and the install itself happen there.

**Fallback (no `android` CLI):** install via mobile-mcp.

Use `mobile_install_app` with:
- **path:** `WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk`

Optionally call `mobile_list_apps` first to check if the app is already installed.

### 6. Set Up API Mocks (Optional)

If the user requests verification of a specific scenario (error states, empty data, custom responses), set up ApiFaker mock endpoints **before** launching the app. See the "API Mocking with ApiFaker" section and `docs/api-faker-adb.md` for commands and workflow.

### 7. Restart and Launch the App

Always force-stop the app first, then launch fresh. This ensures a clean starting state regardless of what screen was previously active — the "Always Restart the App" rule above applies to both paths.

**Preferred path (`USE_ANDROID_CLI=1`, full flow):** locate the APK produced by step 4, then one `android run` call reinstalls and launches the exact Activity. `android run` has no "launch-only" mode — it always reinstalls. When the app is already installed and the build hasn't changed (shortcut flow from the top of "Steps"), use the **launch-only form** further below to skip the reinstall — the CLI does not offer a launch-only equivalent, so both no-CLI users and CLI shortcut users land on the same `am start` block.

```bash
# 1. Resolve the APK path. `android describe` was evaluated for this purpose but
#    its output is multi-line plain text (not JSON/paths-to-JSON) and the
#    underlying Gradle task requires ANDROID_HOME; a plain `find` is simpler and
#    works whether the CLI is installed or not.
APK=$(find WooCommerce/build -type f -name 'WooCommerce-wasabi-debug.apk' | head -n1)

# Persist for the POS variant below (each Bash tool call is a fresh shell, so
# a plain $APK does not survive between separate code blocks).
echo "APK=$APK" >> /tmp/.verify_on_device.env

# 2. Force-stop — android run does not guarantee a cold start.
adb -s <device_id> shell am force-stop com.woocommerce.android.dev

# 3. Combined install + launch.
android run --apks="$APK" \
  --activity=com.woocommerce.android.ui.main.MainActivity \
  --device=<device_id>
```

For POS tasks (only when explicitly requested) — sources `$APK` from the block above:
```bash
. /tmp/.verify_on_device.env  # restore $APK persisted by the main launch block

adb -s <device_id> shell am force-stop com.woocommerce.android.dev
android run --apks="$APK" \
  --activity=com.woocommerce.android.ui.woopos.root.WooPosActivity \
  --device=<device_id>
```

**Launch-only form (no reinstall):** force-stop + `am start`. Use this when (a) the agent CLI is not available, or (b) the agent CLI is available but the app is already installed and the build hasn't changed (the CLI shortcut path).

```bash
adb -s <device_id> shell am force-stop com.woocommerce.android.dev
adb -s <device_id> shell am start -n com.woocommerce.android.dev/com.woocommerce.android.ui.main.MainActivity
```

Do NOT use `mobile_launch_app` — it launches the default launcher intent which may not always resolve to `MainActivity`.

**For POS (only when explicitly requested):** Launch directly into POS with:
```bash
adb -s <device_id> shell am force-stop com.woocommerce.android.dev
adb -s <device_id> shell am start -n com.woocommerce.android.dev/com.woocommerce.android.ui.woopos.root.WooPosActivity
```

### 8. Handle Post-Launch Dialogs

Call `mobile_list_elements_on_screen` to check what appeared. If a dialog or overlay is blocking the main UI, dismiss it using the guidance in "Handling Unexpected Dialogs" above. Repeat until you reach the dashboard or the expected screen.

### 9. Start Screen Recording (Optional)

If the user requests a recording or demo video, start an ADB screen recording **before** navigating:

```bash
# Start recording in the background (max 180s, Android hard limit)
adb -s <device_id> shell "screenrecord --size 720x1280 /sdcard/_agent_rec.mp4" &
```

Use `--size 720x1280` to keep the file small. The recording runs in the background while you perform navigation steps. See the Screen Recording section below for full details.

### 10. Navigate and Verify

Navigate as needed per the user's request. After each navigation action:
1. Wait for the screen to stabilize (see "Waiting for Screen Transitions").
2. Verify you arrived at the expected screen using the Key Screen Identifiers table.
3. Take a screenshot with `mobile_save_screenshot` as evidence.

If the expected screen identifier is NOT present after retries:
1. Take a screenshot with `mobile_take_screenshot`.
2. Call `mobile_list_elements_on_screen` and identify which screen you are actually on.
3. Report to the user: "Navigation to [target] failed. Currently on [detected screen]."

### 11. Stop Recording and Save Evidence

**If recording:** stop the recording, pull the file, and clean up:

```bash
adb -s <device_id> shell "pkill -l SIGINT screenrecord"
sleep 2
adb -s <device_id> pull /sdcard/_agent_rec.mp4 ./verification_recording.mp4
adb -s <device_id> shell rm /sdcard/_agent_rec.mp4
```

**Always:** use `mobile_save_screenshot` at each verification step to save screenshots to disk.

### 12. Report Results

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

All resource IDs below use the debug package prefix `com.woocommerce.android.dev:id/`. Compose test tags (applied via `Modifier.testTag()`) also appear as resource IDs in the accessibility tree because `testTagsAsResourceId` is enabled in the app's theme.

The app has two distinct navigation domains with different architectures. **Only load the reference files you need for the task** — each file adds significant context cost.

### Always load first
- [Overview & Feature Tree](references/main-app-navigation.md) -- lightweight index of all screens, bottom tabs, global elements. Read this first to orient yourself, then load only the detailed references you need.

### Load on demand — match task keywords to the right reference

| If the task involves… | Load this reference |
|---|---|
| **Login**, authentication, store selection, credentials | [Login](references/main-app-login.md) |
| **Dashboard**, stats, analytics, onboarding, date ranges | [Dashboard](references/main-app-dashboard.md) |
| **Orders**, creating orders, **adding products to orders**, payment collection (cash/card/tap-to-pay), refunds, fulfillment, shipping labels, receipts | [Orders](references/main-app-orders.md) |
| **Product catalog** management — creating, editing, deleting, searching products in the Products tab | [Products](references/main-app-products.md) |
| **Settings**, payments hub, reviews, coupons, customers, Blaze, Google Ads | [More Menu](references/main-app-more.md) |
| **POS**, Point of Sale, WooPos, landscape checkout, cash register | [POS](references/pos-navigation.md) |

**Key distinction:** "Adding products to an order" is an **Orders** workflow (order creation screen), NOT a Products workflow. Only load the Products reference when the task is about the standalone product catalog (Products tab).

### Common Navigation Patterns

- **Go back:** Call `mobile_press_button` with button `BACK`. This is the most reliable way to navigate back.
- **Dismiss the soft keyboard:** Call `mobile_press_button` with `BACK`. This only dismisses the keyboard; it does NOT navigate back. If you need to navigate back AND the keyboard is visible, press BACK twice: once to dismiss keyboard, once to navigate.
- **Pull to refresh:** Use `mobile_swipe_on_screen` with direction `down` from the middle of the screen.
- **Scroll down a list:** Use `mobile_swipe_on_screen` with direction `up` (swipe up to scroll down).
- **Open a list item:** Find the item in `mobile_list_elements_on_screen` by its text or identifier, compute center coordinates, and tap.
- **Toolbar back arrow:** Look for elements in the toolbar area (`com.woocommerce.android.dev:id/toolbar`). If the back arrow is not exposed as a separate element, use `mobile_press_button` with `BACK` instead.

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

### Android Knowledge Base (`USE_ANDROID_CLI=1`)

When a platform-behavior question comes up mid-task (intent flags, `am start` semantics, `Activity` launch modes, permissions, animation scales, `settings put global` keys), prefer the Android Knowledge Base over a web search — the answers are authoritative, local, and don't cost browsing tokens.

```bash
# Search by free-form query, then fetch one of the kb:// URLs it returns.
android docs search "am start intent flags"
android docs fetch <kb-url-from-the-search-result>
```

If the CLI is not installed, fall back to the Android developer website as before.
