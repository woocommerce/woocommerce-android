#!/bin/bash

set -euo pipefail

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --validation; then
  message="Skipping Diff Merged Manifest - no relevant files changed"
  echo "$message" | buildkite-agent annotate --style "info" --context "skip-diff-merged-manifest"
  echo "$message"
  exit 0
fi

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
