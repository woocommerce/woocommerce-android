#!/bin/bash -eu

echo "--- 🧪 Testing"
set +e
cp gradle.properties-example gradle.properties
./gradlew testJalapenoDebugUnitTest lib:cardreader:testDebugUnitTest lib:iap:testDebugUnitTest
TESTS_EXIT_STATUS=$?
set -e

if [[ "$TESTS_EXIT_STATUS" -ne 0 ]]; then
  # Keep the (otherwise collapsed) current "Testing" section open in Buildkite logs on error. See https://buildkite.com/docs/pipelines/managing-log-output#collapsing-output
  echo "^^^ +++"
  echo "Unit Tests failed!"
fi


echo "--- 🚦 Report Tests Status"
results_file="WooCommerce/build/test-results/merged-test-results.xml"
# Merge JUnit results into a single file (for performance reasons with reporting)
# See https://github.com/woocommerce/woocommerce-android/pull/12064
merge_junit_reports -d WooCommerce/build/test-results/testJalapenoDebugUnitTest -o $results_file

if [[ $BUILDKITE_BRANCH == trunk ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$results_file" --slack "build-and-ship"
else
    annotate_test_failures "$results_file"
fi

echo "--- ⚒️ Generating and uploading code coverage"
./gradlew jacocoTestReport
.buildkite/commands/upload-code-coverage.sh

echo "--- 🧪 Copying test logs for test collector"
mkdir WooCommerce/build/buildkite-test-analytics && cp WooCommerce/build/test-results/*.xml WooCommerce/build/buildkite-test-analytics

exit $TESTS_EXIT_STATUS
