#!/usr/bin/env sh

# Exit if any command fails
set -eu

TARGET_BRANCH_DEPENDENCIES_FILE="target_branch_dependencies.txt"
CURRENT_TARGET_BRANCH_DEPENDENCIES_FILE="current_branch_dependencies.txt"
DIFF_DEPENDENCIES_FOLDER="./build/reports/diff"
DIFF_DEPENDENCIES_FILE="$DIFF_DEPENDENCIES_FOLDER/diff_dependencies.txt"
CONFIGURATION="vanillaReleaseRuntimeClasspath"

if [ -n $CIRCLE_PULL_REQUEST ]; then
  curl -L "https://github.com/stedolan/jq/releases/download/jq-1.5/jq-linux64" -o jq
  chmod +x jq
  prNumber=$(echo "$CIRCLE_PULL_REQUEST" | sed "s/^.*\/\([0-9]*$\)/\1/")
  githubUrl="https://api.github.com/repos/$CIRCLE_PROJECT_USERNAME/$CIRCLE_PROJECT_REPONAME/pulls/$prNumber"
  githubResponse="$(curl "$githubUrl" -H "Authorization: token $GITHUB_API_TOKEN")"
  targetBranch=$(echo "$githubResponse" | tr '\r\n' ' ' | ./jq '.base.ref' | tr -d '"')

  mkdir -p $DIFF_DEPENDENCIES_FOLDER

  git checkout "$targetBranch"
  ./gradlew :WooCommerce:dependencies --configuration $CONFIGURATION >$TARGET_BRANCH_DEPENDENCIES_FILE

  git checkout "$CIRCLE_BRANCH"
  ./gradlew :WooCommerce:dependencies --configuration $CONFIGURATION >$CURRENT_TARGET_BRANCH_DEPENDENCIES_FILE

  ./tools/dependency-tree-diff/dependency-tree-diff.jar $TARGET_BRANCH_DEPENDENCIES_FILE $CURRENT_TARGET_BRANCH_DEPENDENCIES_FILE >$DIFF_DEPENDENCIES_FILE

  if [ -s $DIFF_DEPENDENCIES_FILE ]; then
    echo "There are changes in dependencies of the project"
    cat "$DIFF_DEPENDENCIES_FILE"
  else
    echo "There are no changes in dependencies of the project"
    rm "$DIFF_DEPENDENCIES_FILE"
  fi
  ./gradlew dependencyTreeDiffCommentToGitHub -DGITHUB_PULLREQUESTID="${CIRCLE_PULL_REQUEST##*/}" -DGITHUB_OAUTH2TOKEN="$GITHUB_API_TOKEN" --info
else
  echo "$CIRCLE_PULL_REQUEST is missing in env"
fi
