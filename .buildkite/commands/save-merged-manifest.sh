#!/bin/bash

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 📦 Create Merged Manifest"
./gradlew assembleJalapenoDebug
echo ""

echo "--- 💾 Save Merged Manifest for WooCommerce"
save_android_merged_manifest "WooCommerce" "jalapenoDebug"

echo "--- 💾 Save Merged Manifest for WooCommerce-Wear"
save_android_merged_manifest "WooCommerce-Wear" "jalapenoDebug"
