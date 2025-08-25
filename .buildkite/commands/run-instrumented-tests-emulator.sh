#!/bin/bash -u

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 📋 Checking Android SDK"
echo "adb --version"
adb --version
echo "emulator -list-avds"
emulator -list-avds
echo ""

echo "--- 🤖 Launching Emulator(s)"
echo "emulator -avd pixel5api34 -no-snapshot -no-boot-anim -no-audio &"
emulator -avd pixel5api34 -no-snapshot -no-boot-anim -no-audio &
echo ""

echo "--- 🤖 Waiting for Emulator(s) to Start"
echo "adb wait-for-device"
adb wait-for-device
echo "while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do"
echo "  sleep 1"
echo "done"
while [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do
  sleep 1
done
echo ""

echo "--- 🤖 Unlock Emulator(s)"
echo "adb shell dumpsys window | grep mDreamingLockscreen"
adb shell dumpsys window | grep mDreamingLockscreen
echo "adb -s emulator-5554 shell input keyevent 82"
adb -s emulator-5554 shell input keyevent 82
echo ""

echo "--- 🤖 Checking Emulator(s)"
echo "adb devices"
adb devices
echo "adb shell dumpsys window | grep mDreamingLockscreen"
adb shell dumpsys window | grep mDreamingLockscreen
echo ""

echo "--- 🧪 Testing"
./gradlew :WooCommerce:connectedVanillaDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.woocommerce.android.e2e.tests.ui.ReviewsUITest
ui_test_exit_code=$?
echo ""

echo "--- 🤖 Checking Emulator(s)"
echo "adb devices"
adb devices
echo ""

echo "--- 🤖 Stopping Emulator(s)"
echo "adb -s emulator-5554 emu kill"
adb -s emulator-5554 emu kill
echo "adb devices"
while adb devices | grep -q emulator-5554; do sleep 1; done
echo ""

exit $ui_test_exit_code
