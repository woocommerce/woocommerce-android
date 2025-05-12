#!/bin/bash -eu

# Usage: should-skip-job.sh --job-type [validation|build|lint]
# --job-type validation: Skip when changes are limited to documentation, tooling, non-code files, and localization files
# --job-type build: Skip when changes are limited to documentation, tooling, and non-code files
# --job-type lint: Skip when changes are limited to documentation, tooling, and non-code files

COMMON_PATTERNS=(
  "*.md"
  "*.pot"
  "*.txt"
  ".gitignore"
  "config/**"
  "docs/**"
  "fastlane/**"
  "Gemfile"
  "Gemfile.lock"
  "gradle/**"
  "version.properties"
)

# Check if arguments are valid
if [ -z "${1:-}" ] || [ "$1" != "--job-type" ] || [ -z "${2:-}" ]; then
  echo "Error: Must specify --job-type [validation|build|lint]"
  buildkite-agent step cancel
  exit 15
fi

case "$2" in
  "validation")
    # We should skip if changes are limited to documentation, tooling, non-code files, and localization files
    PATTERNS=("${COMMON_PATTERNS[@]}" "**/strings.xml")
    pr_changed_files --all-match "${PATTERNS[@]}"
    ;;
  "build"|"lint")
    # We should if changes are limited to documentation, tooling, and non-code files
    # We'll let the job run (won't skip) if PR includes changes in localization files though
    PATTERNS=("${COMMON_PATTERNS[@]}")
    pr_changed_files --all-match "${PATTERNS[@]}"
    ;;
  *)
    echo "Error: Job type must be either 'validation', 'build', or 'lint'"
    buildkite-agent step cancel
    exit 1
    ;;
esac
