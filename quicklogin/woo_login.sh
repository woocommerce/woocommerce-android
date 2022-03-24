#!/bin/bash -e

# PASS WORDPRESS_EMAIL and WORDPRESS_PASSWORD as env variable to the script
# Optionally pass path to your adb: ADB_PATH
# Example: ADB_PATH=~/Library/Android/sdk/platform-tools/adb WORDPRESS_EMAIL=my_wp_email@gmail.com  WORDPRESS_PASSWORD=my_pasword_1_# ./quicklogin/woo_login.sh

if [[ "$ADB_PATH" ]]; then
  adbPath="$ADB_PATH"
else
  adbPath='~/Library/Android/sdk/platform-tools/adb'
fi

eval $adbPath shell pm clear com.woocommerce.android.prealpha

./gradlew installJalapenoDebug
./gradlew :quicklogin:installDebug \
-PquickLoginWpEmail="$WORDPRESS_EMAIL" \
-PquickLoginWpPassword="$WORDPRESS_PASSWORD"

eval $adbPath shell am instrument -w -r -e debug false -e class 'com.woocommerce.android.quicklogin.QuickLoginWordpress' com.woocommerce.android.prealpha.quicklogin/androidx.test.runner.AndroidJUnitRunner
eval $adbPath shell am start -n com.woocommerce.android.prealpha/com.woocommerce.android.ui.main.MainActivity
