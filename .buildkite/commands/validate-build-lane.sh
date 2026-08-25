#!/usr/bin/env bash

set -euo pipefail

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- :hammer_and_wrench: Running build_and_upload_beta (stop before Play Store upload)"
LOG="$(mktemp)"
bundle exec fastlane build_and_upload_beta app:WooCommerce skip_confirm:true | tee "${LOG}"

if ! grep -F 'VALIDATION: bundle built' "${LOG}"; then
  echo "VALIDATION: lane did not print the bundle marker; refusing to treat this as a pass"
  exit 1
fi
