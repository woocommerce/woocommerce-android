#!/usr/bin/env bash
set -euo pipefail

test "$BUILDKITE_SOURCE" = api
test "$BUILDKITE_PULL_REQUEST" = false
test "$(git rev-parse HEAD)" = "$BUILDKITE_COMMIT"
git merge-base --is-ancestor "$BUILDKITE_COMMIT" "refs/remotes/origin/$BUILDKITE_BRANCH"
buildkite-agent --version
echo "Checkout verified for $BUILDKITE_BRANCH at $BUILDKITE_COMMIT"

case "$1" in
  release)
    source .buildkite/shared-pipeline-vars
    : "${RELEASE_VERSION:?release version required}"
    buildkite-agent pipeline upload --dry-run .buildkite/release-builds.yml >/dev/null
    echo "Woo release pipeline dry run passed"
    ;;
  cancel)
    probe_dir=$(mktemp -d)
    cleanup() {
      rmdir "$probe_dir"
      test ! -d "$probe_dir"
      echo "Android v4 cancellation cleanup completed"
      exit 0
    }
    trap cleanup TERM INT
    echo "Android v4 probe ready for cancellation"
    while true; do sleep 1; done
    ;;
esac
