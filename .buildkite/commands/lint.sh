#!/bin/bash -u

echo "--- 🧹 Linting"
cp gradle.properties-example gradle.properties
./gradlew lintJalapenoDebug
lint_exit_code=$?

upload_sarif_to_github 'WooCommerce/build/reports/lint-results-jalapenoDebug.sarif' 'woocommerce' 'woocommerce-android'

exit $lint_exit_code
