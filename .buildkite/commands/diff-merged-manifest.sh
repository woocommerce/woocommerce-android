#!/bin/bash

"$(dirname "${BASH_SOURCE[0]}")/restore-merged-manifest.sh" "WooCommerce" "jalapenoDebug"

"$(dirname "${BASH_SOURCE[0]}")/restore-merged-manifest.sh" "WooCommerce-Wear" "jalapenoDebug"

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 📦 Create Merged Manifest"
./gradlew assembleJalapenoDebug
echo ""

echo "--- 💾 Diff Merged Manifest for WooCommerce"
comment_with_manifest_diff "WooCommerce" "jalapenoDebug"

echo "--- 💾 Diff Merged Manifest for WooCommerce-Wear"
comment_with_manifest_diff "WooCommerce-Wear" "jalapenoDebug"
