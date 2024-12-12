#!/bin/bash

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🛠 Download Mobile App Dependencies [Assemble Mobile App]"
./gradlew :WooCommerce:assembleJalapenoDebug
echo ""

echo "--- 💾 Save Cache"
save_gradle_dependency_cache
