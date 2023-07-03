#!/bin/bash -eu

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo '--- :test-analytics: Configuring Test Analytics'
export BUILDKITE_ANALYTICS_TOKEN=$BUILDKITE_ANALYTICS_TOKEN_INSTRUMENTED_TESTS
