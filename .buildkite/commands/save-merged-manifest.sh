#!/bin/bash

BUILD_VARIANT=$1

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 📦 Create Merged Manifest (Build Variant: ${BUILD_VARIANT})"
./gradlew assemble"${BUILD_VARIANT^}"
echo ""

echo "--- 💾 Save Merged Manifest (Module: WooCommerce, Build Variant: ${BUILD_VARIANT})"
save_android_merged_manifest "WooCommerce" ${BUILD_VARIANT}

echo "--- 💾 Save Merged Manifest (Module: WooCommerce-Wear, Build Variant: ${BUILD_VARIANT})"
save_android_merged_manifest "WooCommerce-Wear" ${BUILD_VARIANT}
