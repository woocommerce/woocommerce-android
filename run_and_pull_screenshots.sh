#!/bin/bash

# Run the screenshot test
./gradlew :WooCommerce:connectedWasabiDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woocommerce.android.e2e.tests.screenshot.WooPosScreenshotTest

# Create screenshots directory
mkdir -p screenshots

# Pull screenshots immediately
echo "Pulling screenshots..."
$HOME/Library/Android/sdk/platform-tools/adb shell "run-as com.woocommerce.android.dev cat app_screengrab/en-US/images/screenshots/1-pos-home-light.png" > screenshots/1-pos-home-light.png
$HOME/Library/Android/sdk/platform-tools/adb shell "run-as com.woocommerce.android.dev cat app_screengrab/en-US/images/screenshots/2-pos-totals-light.png" > screenshots/2-pos-totals-light.png
$HOME/Library/Android/sdk/platform-tools/adb shell "run-as com.woocommerce.android.dev cat app_screengrab/en-US/images/screenshots/3-pos-payment-success-light.png" > screenshots/3-pos-payment-success-light.png

echo "Screenshots saved to ./screenshots/"
ls -lh screenshots/
