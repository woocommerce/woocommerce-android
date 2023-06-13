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

results_files=()
while IFS= read -r -d '' file; do
  results_files+=("$file")
done < <(find . -path "$path_pattern" -type f -name "*.xml" -print0)

printf "results files value: %s\n" "${results_files[@]}"

for file in "${results_files[@]}"; do
  if [[ $BUILDKITE_BRANCH == add-annotate-test-failures ]] || [[ $BUILDKITE_BRANCH == release/* ]]; then
    annotate_test_failures "$file" --slack "jos-testing-notif"
  else
    annotate_test_failures "$file"
  fi
done

echo "--- ⚒️ Generating and uploading code coverage"
./gradlew jacocoTestReport
.buildkite/commands/upload-code-coverage.sh

exit $TESTS_EXIT_STATUS
