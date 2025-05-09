#!/bin/bash -eu

# Usage: should-skip-job.sh [--validation|--build]
# --validation: Skip when changes are limited to documentation, tooling, non-code files, and localization files
# --build: Skip when changes are limited to documentation, tooling, and non-code files

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

if [ "$1" = "--validation" ]; then
  # Check if changes are limited to documentation, tooling, non-code files, and localization files
  PATTERNS=("${COMMON_PATTERNS[@]}" "**/strings.xml")
  pr_changed_files --all-match "${PATTERNS[@]}"
elif [ "$1" = "--build" ]; then
  # Check if changes are limited to documentation, tooling, and non-code files (NOT localization files)
  PATTERNS=("${COMMON_PATTERNS[@]}")
  pr_changed_files --all-match "${PATTERNS[@]}"
else
  echo "Error: Must specify either --validation or --build"
  exit 1
fi
