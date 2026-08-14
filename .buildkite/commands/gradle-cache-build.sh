#!/bin/bash -eu

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type build; then
  exit 0
fi

# This script is used to populate Gradle's build cache with task outputs that can be reused
# by the local machine.

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

echo "--- :hammer_and_wrench: Building"
./gradlew assembleWasabiDebug
