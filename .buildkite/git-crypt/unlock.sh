#!/bin/bash

if [ -z "$GIT_CRYPT_ENCRYPTION_KEY" ]; then
  echo "GIT_CRYPT_ENCRYPTION_KEY is not set"
  exit 1
fi

set -euo pipefail

echo "Checking for git-crypt..."
if command -v git-crypt >/dev/null 2>&1; then
  echo " - Using system git-crypt"
  gitcrypt_path="git-crypt"
elif [ "$(uname -s)" == "Linux" ] && [ "$(uname -m)" == "x86_64" ]; then
  echo " - Using pre-compiled x86_64 git-crypt"
  gitcrypt_path=".buildkite/git-crypt/git-crypt.linux-x86_64"
else
  echo "Unable to find git-crypt binary (architecture: $(uname -s) $(uname -m))"
  exit 1
fi

echo "🔓 Decrypting repository..."
"${gitcrypt_path}" unlock <(echo "${GIT_CRYPT_ENCRYPTION_KEY}" | base64 -d)
echo "✅ git-crypt unlocked"
