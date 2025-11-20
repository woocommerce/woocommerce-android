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

### Install ImageMagick

```sh
# Install ImageMagick for promo screenshot generation
brew install imagemagick
```

ImageMagick is required to apply masks, composite device frames, and add text during promo screenshot generation.

## Phone Screenshots

### Setup

1. Connect a Pixel 9 device or start a Pixel 9 emulator
   - Emulators can be started from Android Studio UI (Device Manager) or command line
2. Add Android SDK platform-tools to your PATH (if not already added):
   ```sh
   export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
   ```
3. Ensure only the phone is connected:
   ```sh
   adb devices
   ```
   You should see only one device listed. If multiple devices are connected, disconnect the others to avoid conflicts

### Generate Raw Screenshots

Run the fastlane command to capture raw screenshots:

```sh
bundle exec fastlane take_screenshots
```

This will:
- Build debug APKs
- Install the app on the connected device
- Run UI tests that capture screenshots for both light and dark themes
- Save raw screenshots to `fastlane/screenshots/raw/{locale}/images/phoneScreenshots/`

### Generate Promo Screenshots

After raw screenshots are captured, generate the final promotional screenshots with device frames and text:

```sh
bundle exec fastlane create_promo_screenshots
```

This will:
- Apply device frames to the raw screenshots
- Add promotional text from `fastlane/metadata/android/{locale}/promo_screenshot_*.txt`
- Save final screenshots to `fastlane/screenshots/promo_screenshots/{locale}/`

### Output Location

Final phone promo screenshots will be in:
```
fastlane/screenshots/promo_screenshots/{locale}/
├── Phone-01.png
├── Phone-02.png
├── Phone-03.png
└── ...
```

## Tablet Screenshots (POS)

### Setup

1. Connect a Pixel Tablet device or start a Pixel Tablet emulator (API 35)
   - Emulators can be started from Android Studio UI (Device Manager) or command line:
   ```sh
   # Example: Launch Pixel Tablet emulator from command line
   emulator -avd Pixel_Tablet_API_35
   ```

2. Add Android SDK platform-tools to your PATH (if not already added):
   ```sh
   export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
   ```

3. Ensure only the tablet is connected:
   ```sh
   adb devices
   ```
   You should see only one device listed. If multiple devices are connected, disconnect the others to avoid conflicts

### Generate Raw Screenshots

Run the fastlane command to capture raw POS screenshots:

```sh
bundle exec fastlane take_pos_screenshots
```

This will:
- Build debug APKs
- Install the app on the connected tablet
- Run POS UI tests that capture screenshots for both light and dark themes
- Save raw screenshots to `fastlane/screenshots/raw/{locale}/images/phoneScreenshots/`
  - Files will be named like `1-pos-home-light.png`, `2-pos-totals-dark.png`, etc.

### Generate Promo Screenshots

After raw screenshots are captured, generate the final promotional screenshots:

```sh
bundle exec fastlane create_pos_promo_screenshots
```

This will:
- Apply Pixel Tablet device frames to the raw screenshots
- Add promotional text from `fastlane/metadata/android/{locale}/promo_screenshot_pos_*.txt`
- Apply rounded corner masks (configured in `fastlane/screenshots_pos.json`)
- Save final screenshots to `fastlane/screenshots/promo_screenshots/{locale}/`

### Output Location

Final tablet promo screenshots will be in:
```
fastlane/screenshots/promo_screenshots/{locale}/
├── Pixel Tablet-01.png
├── Pixel Tablet-02.png
├── Pixel Tablet-03.png
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
