#!/bin/bash

BUILD_VARIANT=$1

"$(dirname "${BASH_SOURCE[0]}")/restore-merged-manifest.sh" "WooCommerce" ${BUILD_VARIANT}

"$(dirname "${BASH_SOURCE[0]}")/restore-merged-manifest.sh" "WooCommerce-Wear" ${BUILD_VARIANT}

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

set -euo pipefail

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 📦 Create Merged Manifest (Build Variant: ${BUILD_VARIANT})"
./gradlew assemble"${BUILD_VARIANT^}"
echo ""

echo "--- 💾 Diff Merged Manifest (Module: WooCommerce, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WooCommerce" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: WooCommerce-Wear, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WooCommerce-Wear" ${BUILD_VARIANT}
