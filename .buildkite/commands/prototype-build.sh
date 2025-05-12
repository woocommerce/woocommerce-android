#!/bin/bash -eu

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type build; then
  message="Skipping Build - no relevant files changed"
  echo "$message" | buildkite-agent annotate --style "info" --context "skip-build"
  echo "$message"
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

APP_TO_BUILD="${1?You need to specify the app to build, WooCommerce or WooCommerce-Wear}"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building ${APP_TO_BUILD}"
bundle exec fastlane build_and_upload_prototype_build app:"${APP_TO_BUILD}"
