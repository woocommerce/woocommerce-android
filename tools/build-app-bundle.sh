#!/bin/sh

# Check for bundletool
command -v bundletool > /dev/null || { echo "bundletool is required to build the APKs. Install it with 'brew install bundletool'" >&2; exit 1; }

# Exit if any command fails
set -eu

# Load the Gradle helper functions
source "./tools/gradle-functions.sh"

function extract_universal_apk {
  app_bundle="$1"
  apk_output="$2"
  tmp_dir=$(mktemp -d)

  echo "Extracting universal APK..." | tee -a $LOGFILE
  bundletool build-apks --bundle="$app_bundle" \
                        --output="$tmp_dir/universal.apks" \
                        --mode=universal \
                        --ks="$(get_gradle_property gradle.properties storeFile)" \
                        --ks-pass="pass:$(get_gradle_property gradle.properties storePassword)" \
                        --ks-key-alias="$(get_gradle_property gradle.properties keyAlias)" \
                        --key-pass="pass:$(get_gradle_property gradle.properties keyPassword)" >> $LOGFILE 2>&1
  
  unzip "$tmp_dir/universal.apks" -d "$tmp_dir"  >> $LOGFILE 2>&1
  cp "$tmp_dir/universal.apk" "$apk_output" | tee -a $LOGFILE
}

OUTPUT_DIR="artifacts"
LOGFILE="$OUTPUT_DIR/build.log"

mkdir -p "$OUTPUT_DIR" && > "$LOGFILE"

# Print the logs on failure
function cleanup {
  cat "$LOGFILE"
}
trap cleanup ERR

FLAVOR="Vanilla"
AAB_PATH="WooCommerce/build/outputs/bundle/"$FLAVOR"Release/WooCommerce.aab"

VERSION_NAME=$(gradle_version_name "WooCommerce/build.gradle")
VERSION_CODE=$(gradle_version_code "WooCommerce/build.gradle")
AAB_NAME="wcandroid-$VERSION_NAME.aab"
APK_NAME="wcandroid-$VERSION_NAME-universal.apk"

echo "Cleaning $VERSION_NAME / $VERSION_CODE ..." | tee -a $LOGFILE
./gradlew clean >> $LOGFILE 2>&1
echo "Linting $VERSION_NAME / $VERSION_CODE ..." | tee -a $LOGFILE
./gradlew lint"$FLAVOR"Release >> $LOGFILE 2>&1
echo "Building $VERSION_NAME / $VERSION_CODE ..." | tee -a $LOGFILE
./gradlew bundle"$FLAVOR"Release >> $LOGFILE 2>&1
cp -v "$AAB_PATH" "$OUTPUT_DIR/$AAB_NAME" | tee -a $LOGFILE
echo "Bundle ready: $AAB_NAME" | tee -a $LOGFILE
extract_universal_apk "$OUTPUT_DIR/$AAB_NAME" "$OUTPUT_DIR/$APK_NAME"
