#!/bin/sh

if [ x"$2" == x ]; then
  echo "This script updates the version name and code"
  echo "Usage:   $0 version-code beta-version"
  echo "Example: $0 31 1.1"
  exit 1
fi

source ./tools/update-name-core.sh

BETA_VERSION_CODE=$1
BETA_VERSION=$2

if [ x"$3" == x ]; then
    BETA_BRANCH=release/`echo $BETA_VERSION | cut -d- -f1`
else
    BETA_BRANCH=$3
fi

BUILD_FILE=WooCommerce/build.gradle
LOGFILE=/tmp/update-release-names.log
echo > $LOGFILE

check_version_code

switch_branch_pull $BETA_BRANCH
edit_build_file_commit $BETA_VERSION_CODE $BETA_VERSION
