#!/bin/bash -u

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --validation; then
  message="Skipping Lint - no relevant files changed"
  echo "$message" | buildkite-agent annotate --style "info" --context "skip-lint"
  echo "$message"
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

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
