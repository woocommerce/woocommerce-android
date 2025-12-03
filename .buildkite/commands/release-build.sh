#!/bin/bash -eu

APP_TO_BUILD="${1?You need to specify the app to build, WooCommerce or WooCommerce-Wear}"

echo "--- :closed_lock_with_key: Decrypting Secrets"
git-conceal-unlock

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :hammer_and_wrench: Building ${APP_TO_BUILD}"
bundle exec fastlane build_and_upload_google_play app:"${APP_TO_BUILD}"
