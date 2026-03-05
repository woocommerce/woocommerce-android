#!/usr/bin/env bash

set -eu

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type build; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

APP_TO_BUILD="${1:?Usage: prototype-build.sh <WooCommerce|WooCommerce-Wear>}"
echo "Selected app: ${APP_TO_BUILD}"

echo "--- :hammer_and_wrench: Building and uploading to Firebase App Distribution"
bundle exec fastlane build_and_upload_prototype_build app:"${APP_TO_BUILD}"
