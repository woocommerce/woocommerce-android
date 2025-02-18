#!/bin/bash

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 📦 Create Debug Manifest (Jalapeno)"
./gradlew assembleJalapenoDebug
echo ""

echo "--- 💾 Save Debug Manifest (Jalapeno)"
save_android_merged_manifest "WooCommerce"
