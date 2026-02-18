---
name: verify-on-device
description: Build, install, and visually verify the app on an Android emulator
allowed-tools: Bash, Read, Grep, Glob, mcp__mobile-mcp__*
user-invocable: true
---

# Verify on Device

Build, install, and visually verify the app on an Android emulator using mobile-mcp.

**Prerequisites:** Node.js v22+, Android SDK with platform-tools, a running Android emulator or connected device.

## Steps

1. **List available devices.** Use the `mobile_list_available_devices` MCP tool to discover running emulators or connected devices. If no device is found, instruct the user to start an emulator.

2. **Build the debug APK:**
   ```
   ./gradlew assembleWasabiDebug
   ```

3. **Install the APK** on the device using the `mobile_install_app` MCP tool:
   - Path: `WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk`

4. **Launch the app** using the `mobile_launch_app` MCP tool:
   - Package name: `com.woocommerce.android.dev`

5. **Take a screenshot** using `mobile_take_screenshot` to verify the app launched successfully.

6. **Navigate the app** as needed using:
   - `mobile_list_elements_on_screen` — discover UI elements and their coordinates
   - `mobile_click_on_screen_at_coordinates` — tap on elements
   - `mobile_swipe_on_screen` — scroll or swipe
   - `mobile_type_keys` — enter text into fields
   - `mobile_press_button` — press BACK, HOME, or other device buttons

7. **Take screenshots at each verification step** to document the state.

8. **Report results** with a summary of what was verified and any issues found.

## Tips

- Use `mobile_list_elements_on_screen` before tapping to find the exact coordinates of UI elements.
- After any navigation action, take a screenshot to confirm the expected screen appeared.
- If the app is not responding, try `mobile_terminate_app` followed by `mobile_launch_app` to restart it.
- For login flows, you may need the user to provide test credentials.
