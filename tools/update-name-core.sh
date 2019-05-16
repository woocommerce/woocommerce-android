#!/bin/sh

# This script defines some shared functions that are used by the update-name* set.

function check_version_code() {
    echo "Done."
}

function edit_build_file_commit() {
    echo "Editing $BUILD_FILE - name: $2 code: $1"
    perl -pi -e "s/versionCode.*$/versionCode $1/" $BUILD_FILE
    perl -pi -e "s/versionName.*$/versionName \"$2\"/" $BUILD_FILE
    echo "Adding to git"
    git add $BUILD_FILE >> $LOGFILE 2>> $LOGFILE
    git commit -m "$2 / $1 version bump" >> $LOGFILE 2>> $LOGFILE
}

function switch_branch_pull() {
    echo "Switching and pulling: $1"
    git checkout $1 >> $LOGFILE 2>> $LOGFILE
    git pull origin $1 >> $LOGFILE 2>> $LOGFILE
}