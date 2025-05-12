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
  exit 1
fi

if [ "$2" = "validation" ]; then
  # Check if changes are limited to documentation, tooling, non-code files, and localization files
  PATTERNS=("${COMMON_PATTERNS[@]}" "**/strings.xml")
  pr_changed_files --all-match "${PATTERNS[@]}"
elif [ "$2" = "build" ] || [ "$2" = "lint" ]; then
  # Check if changes are limited to documentation, tooling, and non-code files (NOT localization files)
  PATTERNS=("${COMMON_PATTERNS[@]}")
  pr_changed_files --all-match "${PATTERNS[@]}"
else
  echo "Error: Job type must be either 'validation', 'build', or 'lint'"
  buildkite-agent step cancel
  exit 1
fi
