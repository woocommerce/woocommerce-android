#!/bin/bash -u

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :rubygems: Setting up Gems"
install_gems

echo "--- :closed_lock_with_key: Installing Secrets"
bundle exec fastlane run configure_apply

echo "--- 🧹 Linting"
./gradlew :WooCommerce:lintJalapenoDebug
app_lint_exit_code=$?

./gradlew :WooCommerce-Wear:lintJalapenoDebug
wear_lint_exit_code=$?

lint_exit_code=0
if [ $app_lint_exit_code -ne 0 ] || [ $wear_lint_exit_code -ne 0 ]; then
  lint_exit_code=1
fi

upload_sarif_to_github 'WooCommerce/build/reports/lint-results-jalapenoDebug.sarif'

exit $lint_exit_code
