#!/bin/bash -e

# PASS WORDPRESS_EMAIL, WORDPRESS_SITE and WORDPRESS_PASSWORD as env variable to the script
# Optionally pass path to your adb: ADB_PATH
# Example: ADB_PATH=~/Library/Android/sdk/platform-tools/adb WORDPRESS_EMAIL=my_wp_email@gmail.com  WORDPRESS_PASSWORD=my_pasword_1_# WORDPRESS_SITE=my_wordpress_site ./baselineprofile/woo_generate_baseline_profile.sh

if [[ "$ADB_PATH" ]]; then
  adbPath="$ADB_PATH"
else
  adbPath='~/Library/Android/sdk/platform-tools/adb'
fi

packageName="com.woocommerce.android"

eval $adbPath uninstall $packageName
eval $adbPath root

eval $adbPath shell rm -r /storage/emulated/0/Android/media/com.woocommerce.android.baselineprofile

./gradlew :baselineprofile:connectedCheck -P android.testInstrumentationRunnerArguments.class=com.woocommerce.android.baselineprofile.BaselineProfileGenerator#loggedInStartup \
-PquickLoginWpEmail="$WORDPRESS_EMAIL" \
-PquickLoginWpPassword="$WORDPRESS_PASSWORD" \
-PquickLoginWpSite="$WORDPRESS_SITE"

baselineFileName="BaselineProfileGenerator_loggedInStartup-baseline-prof.txt"

eval $adbPath pull /storage/emulated/0/Android/media/com.woocommerce.android.baselineprofile/$baselineFileName

mv $baselineFileName WooCommerce/src/main/baseline-prof.txt
