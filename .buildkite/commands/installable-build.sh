#!/bin/bash -eu

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- Restoring Gradle Cache"
restore_cache "$GRADLE_DEPENDENCY_CACHE_KEY"

echo "--- :hammer_and_wrench: Building"
bundle exec fastlane build_and_upload_installable_build
