#!/bin/bash

set -euo pipefail

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

BUILD_VARIANT=$1

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 💾 Diff Merged Manifest (Module: WooCommerce, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WooCommerce" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: WooCommerce-Wear, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WooCommerce-Wear" ${BUILD_VARIANT}
