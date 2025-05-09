#!/bin/bash -eu

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --build; then
  message="Skipping Gradle Cache Build - no relevant files changed"
  echo "$message" | buildkite-agent annotate --style "info" --context "skip-gradle-cache"
  echo "$message"
  exit 0
fi

# This script is used to populate Gradle's build cache with task outputs that can be reused
# by the local machine.

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Building"
./gradlew assembleWasabiDebug
