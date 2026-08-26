#!/bin/bash -eu

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type validation; then
  mkdir -p WooCommerce/build/buildkite-test-analytics && touch WooCommerce/build/buildkite-test-analytics/empty.xml
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "+++ 🧪 Testing"
set +e
./gradlew testJalapenoDebugUnitTest testDebugUnitTest jacocoTestReport
TESTS_EXIT_STATUS=$?
set -e

if [[ "$TESTS_EXIT_STATUS" -eq 0 ]]; then
  echo -e "\n--- ⚒️ Uploading code coverage"
  .buildkite/commands/upload-code-coverage.sh
fi

echo -e "\n--- 🚦 Report Tests Status"
results_file="WooCommerce/build/test-results/merged-test-results.xml"
# Merge JUnit results into a single file (for performance reasons with reporting)
# See https://github.com/woocommerce/woocommerce-android/pull/12064
merge_junit_reports -d WooCommerce/build/test-results/testJalapenoDebugUnitTest -o "$results_file"

if [[ $BUILDKITE_BRANCH == trunk ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$results_file" --slack "build-and-ship"
else
    annotate_test_failures "$results_file"
fi

echo "--- 🧪 Copying test logs for test collector"
mkdir WooCommerce/build/buildkite-test-analytics && cp "$results_file" WooCommerce/build/buildkite-test-analytics

exit $TESTS_EXIT_STATUS
