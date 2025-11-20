# Generating Play Store Screenshots

This guide explains how to generate promotional screenshots for the Google Play Store listing. Screenshots are generated for both phone and tablet devices.

## Prerequisites

Before generating screenshots, ensure you have the following tools installed:

### Install Ruby and Bundler

```sh
# Install Ruby (if not already installed)
brew install ruby

# Install Bundler
gem install bundler
```

### Install Fastlane and Dependencies

```sh
# Install project dependencies including Fastlane and screenshot generation gems
bundle install --with screenshots
```

### Install ImageMagick and drawText

```sh
# Install ImageMagick and drawText for promo screenshot generation
brew install imagemagick automattic/build-tools/drawText
```

ImageMagick is required to apply masks, composite device frames, and add text during promo screenshot generation.

### Install Proxima Nova Font

The Proxima Nova font is required for screenshot text. Check Font Book app first - it's included in macOS Sonoma and later. Only install manually if not already available.

### Configure Device for Screenshots

To remove unwanted icons from the status bar and keep the time set to "12:30," enable demo mode and turn on the "Show demo mode" switch.

When generating screenshots, unwanted notifications from other apps may appear. To prevent this:

1. Go to Settings → Notifications → App notifications
2. Select All apps from the dropdown menu
3. Tap the Show system option from the menu at the top
4. Disable notifications for:
   - Android System Intelligence
   - Digital Wellbeing
   - Google Play Store
   - Google Play Protect Service
5. Tap on Android System and disable all switches within its details

## Generating Screenshots

### Setup

1. Connect the appropriate device or start an emulator:
   - **Phone**: Pixel 9 XL device or emulator
   - **Tablet (POS)**: Pixel Tablet device or emulator (API 35)
   - Emulators can be started from Android Studio UI (Device Manager) or command line

2. Add Android SDK platform-tools to your PATH (if not already added):
   ```sh
   export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
   ```

3. Ensure only one device is connected:
   ```sh
   adb devices
   ```
   You should see only one device listed. If multiple devices are connected, disconnect the others to avoid conflicts

### Generate Raw Screenshots

Run the fastlane command to capture raw screenshots:

**Phone:**
```sh
bundle exec fastlane take_screenshots
```

**Tablet (POS):**
```sh
bundle exec fastlane take_pos_screenshots
```

Optional: Specify locales (otherwise generates for all):
```sh
bundle exec fastlane take_screenshots locales:en-US,fr-FR
bundle exec fastlane take_pos_screenshots locales:en-US,fr-FR
```

This will:
- Build debug APKs
- Install the app on the connected device
- Run UI tests that capture screenshots for both light and dark themes
- Save raw screenshots to `fastlane/screenshots/raw/{locale}/images/phoneScreenshots/`

**Note:** This command can take hours to run for all locales. Verify it works for one locale first, then let it run in the background.

If you modify the screenshot test or change screenshot order, update the filenames in `fastlane/screenshots.json`.

### Generate Promo Screenshots

After raw screenshots are captured, download the translated strings:

```sh
bundle exec fastlane download_promo_strings
```

Then generate the final promotional screenshots with device frames and text:

**Phone:**
```sh
bundle exec fastlane create_promo_screenshots
```

**Tablet (POS):**
```sh
bundle exec fastlane create_pos_promo_screenshots
```

This will:
- Apply device frames to the raw screenshots
- Add promotional text from `fastlane/metadata/android/{locale}/promo_screenshot_*.txt` or `promo_screenshot_pos_*.txt`
- Save final screenshots to `fastlane/screenshots/promo_screenshots/{locale}/`

### Output Location

Final promo screenshots will be in:
```
fastlane/screenshots/promo_screenshots/{locale}/
├── Phone-01.png or Pixel Tablet-01.png
├── Phone-02.png or Pixel Tablet-02.png
└── ...
```

## Configuration

### Phone Screenshot Configuration

Configuration file: `fastlane/screenshots.json`

This file defines:
- Device frame sizes and offsets
- Screenshot positioning
- Text placement
- Background images

Promotional text: `fastlane/metadata/android/{locale}/promo_screenshot_*.txt`

### Tablet Screenshot Configuration

Configuration file: `fastlane/screenshots_pos.json`

This file defines:
- Device frame: `fastlane/playstoreres/assets/pixel-tablet.png`
- Rounded corner mask: `fastlane/playstoreres/assets/pixel-tablet-mask.png`
- Background images: `fastlane/playstoreres/assets/background-tablet-*.png`
- Screenshot size, offset, and frame dimensions

Promotional text: `fastlane/metadata/android/{locale}/promo_screenshot_pos_*.txt`
