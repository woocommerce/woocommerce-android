#!/usr/bin/env bash

set -euo pipefail

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "+++ :sentry: Upload with the token present"
./gradlew :WooCommerce:uploadSentryProguardMappingsVanillaRelease
./gradlew :WooCommerce-Wear:sentryUploadSourceBundleWasabiRelease

echo "--- :sentry: Guard fires when the token is missing"
set +e
output=$(SENTRY_AUTH_TOKEN= ./gradlew :WooCommerce-Wear:sentryUploadSourceBundleWasabiRelease --rerun-tasks 2>&1)
status=$?
set -e

printf '%s\n' "$output" | tail -30

if [[ "$status" -eq 0 ]]; then
  echo "FAIL: expected a non-zero exit without SENTRY_AUTH_TOKEN, got success."
  exit 1
fi

if ! printf '%s' "$output" | grep -q 'SENTRY_AUTH_TOKEN is not set'; then
  echo "FAIL: build failed without SENTRY_AUTH_TOKEN, but not through the guard."
  exit 1
fi

echo "Guard fired as expected."
