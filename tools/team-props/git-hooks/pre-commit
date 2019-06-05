#!/bin/bash
# Stops accidental commits to master and develop. Pulled from: https://gist.github.com/stefansundin/9059706
# Install:
# cd path/to/git/repo
# curl -fL -o .git/hooks/pre-commit https://gist.githubusercontent.com/stefansundin/9059706/raw/pre-commit
# chmod +x .git/hooks/pre-commit

BRANCH=`git rev-parse --abbrev-ref HEAD`

if [[ "$BRANCH" == "master" || "$BRANCH" == "develop" ]]; then
  echo "You are on branch $BRANCH. Are you sure you want to commit to this branch?"
  echo "If so, commit with -n to bypass this pre-commit hook."
  exit 1
fi

# This block allows for chaining pre-commit hooks if this hook is a global hook (via core.hooksPath) and there also exists a repo-specific pre-commit hook
if [[ -f ".git/hooks/pre-commit" ]]; then
  type realpath >/dev/null 2>&1 || { echo >&2 "NOTE: the realpath binary is required to chain to the repo-specific pre-commit hook. Ignoring."; exit 0; }
  if [[ "$(realpath "${BASH_SOURCE[0]}")" != "$(realpath ".git/hooks/pre-commit")" ]]; then
    .git/hooks/pre-commit
    exit $?
  fi
fi

exit 0
