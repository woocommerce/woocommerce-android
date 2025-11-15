#!/bin/bash -eu

APP_TO_BUILD="${1?You need to specify the app to build, WooCommerce or WooCommerce-Wear}"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Decrypting Secrets"
git-crypt-unlock

echo "--- :hammer_and_wrench: Building ${APP_TO_BUILD}"
bundle exec fastlane build_and_upload_google_play app:"${APP_TO_BUILD}"
