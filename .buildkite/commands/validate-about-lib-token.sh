#!/usr/bin/env bash

set -euo pipefail

export GIT_AUTHOR_NAME=validation GIT_AUTHOR_EMAIL=validation@example.com
export GIT_COMMITTER_NAME=validation GIT_COMMITTER_EMAIL=validation@example.com

echo '--- :ruby: Setup Ruby Tools'
install_gems

start_sha=$(git rev-parse HEAD)

echo '--- :white_check_mark: localize_libs with the CI token'
if [[ -z "${AUTOMATTIC_GITHUB_TOKEN_READ_ONLY:-}" ]]; then
  echo '^^^ +++ AUTOMATTIC_GITHUB_TOKEN_READ_ONLY is not set on this agent'
  exit 1
fi
positive_log=$(mktemp)
bundle exec fastlane localize_libs 2>&1 | tee "$positive_log"
grep -q 'Logged in as:' "$positive_log"
grep -q 'Strings.xml file for About Library downloaded to' "$positive_log"
git reset --hard "$start_sha"

echo '--- :x: localize_libs with a bogus token must fail on auth'
negative_log=$(mktemp)
if AUTOMATTIC_GITHUB_TOKEN_READ_ONLY=bogus bundle exec fastlane localize_libs >"$negative_log" 2>&1; then
  cat "$negative_log"
  echo '^^^ +++ localize_libs succeeded with a bogus token — the token is not what authenticates'
  exit 1
fi
cat "$negative_log"
if ! grep -q 'Bad credentials' "$negative_log"; then
  echo '^^^ +++ localize_libs failed, but not on authentication'
  exit 1
fi
git reset --hard "$start_sha"

echo '+++ :tada: Token authenticates and About Library strings download'
