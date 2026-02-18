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

### 4. Launch the App

Use `mobile_launch_app` with:
- **packageName:** `com.woocommerce.android.dev`

### 5. Verify Launch and Navigate

After launching, call `mobile_list_elements_on_screen` to confirm the app has loaded. Then navigate as needed per the user's request.

### 6. Save Evidence

Use `mobile_save_screenshot` at each verification step to save screenshots to disk for documentation.

### 7. Report Results

Summarize what was verified, include saved screenshot paths, and flag any issues found.

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

### Common Navigation Patterns

- **Go back:** Call `mobile_press_button` with button `BACK`. This is the most reliable way to navigate back.
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
| **Element not found on screen** | The screen may still be loading. Wait briefly, then call `mobile_list_elements_on_screen` again. |
| **Tap lands on wrong element** | You likely used screenshot coordinates instead of element coordinates. Always use `mobile_list_elements_on_screen` and compute the center of the bounding rect. |
| **Login screen appears** | The app requires authentication. Ask the user for test credentials or to log in manually. |
| **App crashes on launch** | Run `adb logcat -d *:E` via Bash to check crash logs. Common cause: missing FluxC database migration. |
| **No devices found** | Run `adb devices` via Bash to check ADB connectivity. Ensure the emulator is booted or the physical device has USB debugging enabled. |
