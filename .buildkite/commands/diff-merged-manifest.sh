#!/bin/bash

set -euo pipefail

# Check if we can skip this job based on PR changes
if .buildkite/commands/should-skip-job.sh --job-type validation; then
  exit 0
fi

"$(dirname "${BASH_SOURCE[0]}")/restore-cache.sh"

BUILD_VARIANT=$1

echo "--- 💾 Diff Merged Manifest (Module: WooCommerce, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WooCommerce" ${BUILD_VARIANT}

echo "--- 💾 Diff Merged Manifest (Module: WooCommerce-Wear, Build Variant: ${BUILD_VARIANT})"
comment_with_manifest_diff "WooCommerce-Wear" ${BUILD_VARIANT}
