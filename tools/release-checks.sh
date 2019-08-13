#!/bin/sh

RESDIR=WooCommerce/src/main/res/
BUILDFILE=WooCommerce/build.gradle

function pOk() {
  echo "[$(tput setaf 2)OK$(tput sgr0)]"
}

function pFail() {
  echo "[$(tput setaf 1)KO$(tput sgr0)]"
}

function checkENStrings() {
  if [[ -n $(git status --porcelain|grep "M res") ]]; then
    /bin/echo -n "Unstagged changes detected in $RESDIR - can't continue..."
    pFail
    exit 3
  fi
  # save local changes
  git stash | grep "No local changes to save" > /dev/null
  needpop=$?

  rm -f $RESDIR/values-??/strings.xml $RESDIR/values-??-r??/strings.xml
  /bin/echo -n "Check for missing strings (slow)..."
  ./gradlew buildVanillaRelease > /dev/null 2>&1 && pOk || (pFail; ./gradlew buildVanillaRelease)
  ./gradlew clean > /dev/null 2>&1
  git checkout -- $RESDIR/

  # restore local changes
  if [ $needpop -eq 1 ]; then
    git stash pop > /dev/null
  fi
}

function printVersion() {
  gradle_version=$(grep -E 'versionName' $BUILDFILE | sed s/versionName// | grep -Eo "[a-zA-Z0-9.-]+" )
  echo "$BUILDFILE version $gradle_version"
}

checkENStrings
printVersion