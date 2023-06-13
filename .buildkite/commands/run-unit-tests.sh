#!/bin/bash -eu

echo "--- 🧪 Testing"
set +e
cp gradle.properties-example gradle.properties
./gradlew testJalapenoDebug lib:cardreader:testDebug lib:iap:testDebug
TESTS_EXIT_STATUS=$?
set -e

if [[ "$TESTS_EXIT_STATUS" -ne 0 ]]; then
  # Keep the (otherwise collapsed) current "Testing" section open in Buildkite logs on error. See https://buildkite.com/docs/pipelines/managing-log-output#collapsing-output
  echo "^^^ +++"
  echo "Unit Tests failed!"
fi

# Pattern to match the paths
path_pattern="*/build/test-results/*/*.xml"

# Find the XML files matching the pattern
results_files=($(find . -path "$path_pattern" -type f -name "*.xml"))

if [[ $BUILDKITE_BRANCH == add-annotate-test-failures ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$results_files" --slack "jos-testing-notif"
else
    annotate_test_failures "$results_files"
fi

echo "--- ⚒️ Generating and uploading code coverage"
./gradlew jacocoTestReport
.buildkite/commands/upload-code-coverage.sh

exit $TESTS_EXIT_STATUS
