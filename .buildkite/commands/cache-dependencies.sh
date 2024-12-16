#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🛠 Download Mobile App Dependencies [Assemble Mobile App]"
./gradlew :WooCommerce:assembleJalapenoDebug

echo "--- 🛠 Download Wear App Dependencies [Assemble Wear App]"
./gradlew :WooCommerce-Wear:assembleJalapenoDebug

echo "--- 🧹 Download Lint Dependencies [Lint Mobile App]"
./gradlew :WooCommerce:lintJalapenoDebug

echo "--- 🧹 Download Detekt Dependencies [Run Detekt]"
./gradlew detektAll

echo "--- 🧪 Download Unit Test Dependencies [Assemble Unit Tests]"
./gradlew assembleJalapenoDebugUnitTest lib:cardreader:assembleDebugUnitTest lib:iap:assembleDebugUnitTest

echo "--- 🧪 Download Android Test Dependencies [Assemble Android Tests]"
./gradlew assembleJalapenoDebugAndroidTest

save_gradle_dependency_cache
