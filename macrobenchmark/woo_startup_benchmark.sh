#!/bin/bash -e

# PASS WORDPRESS_EMAIL, WORDPRESS_SITE and WORDPRESS_PASSWORD as env variable to the script
# Optionally pass path to your adb: ADB_PATH
# Example: ADB_PATH=~/Library/Android/sdk/platform-tools/adb WORDPRESS_EMAIL=my_wp_email@gmail.com  WORDPRESS_PASSWORD=my_pasword_1_# WORDPRESS_SITE=my_wordpress_site ./macrobenchmark/woo_startup_benchmark.sh

if [[ "$ADB_PATH" ]]; then
  adbPath="$ADB_PATH"
else
  adbPath='~/Library/Android/sdk/platform-tools/adb'
fi

packageName="com.woocommerce.android"

eval $adbPath uninstall $packageName

./gradlew :macrobenchmark:connectedCheck -P android.testInstrumentationRunnerArguments.class=com.woocommerce.android.benchmark.LoggedInStartupBenchmark \
-PquickLoginWpEmail="$WORDPRESS_EMAIL" \
-PquickLoginWpPassword="$WORDPRESS_PASSWORD" \
-PquickLoginWpSite="$WORDPRESS_SITE"
