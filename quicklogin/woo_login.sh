#!/bin/bash -e

# PASS WORDPRESS_EMAIL, WORDPRESS_SITE and WORDPRESS_PASSWORD as env variable to the script
# Optionally pass path to your adb: ADB_PATH
# Example: ADB_PATH=~/Library/Android/sdk/platform-tools/adb WORDPRESS_EMAIL=my_wp_email@gmail.com  WORDPRESS_PASSWORD=my_pasword_1_# WORDPRESS_SITE=my_wordpress_site ./quicklogin/woo_login.sh

if [[ "$ADB_PATH" ]]; then
  adbPath="$ADB_PATH"
else
  adbPath='~/Library/Android/sdk/platform-tools/adb'
fi

packageName="com.woocommerce.android.dev"

eval $adbPath shell pm clear $packageName

./gradlew installWasabiDebug
./gradlew :quicklogin:installDebug \
-PquickLoginWpEmail="$WORDPRESS_EMAIL" \
-PquickLoginWpPassword="$WORDPRESS_PASSWORD" \
-PquickLoginWpSite="$WORDPRESS_SITE"

eval $adbPath shell am instrument -w -r -e debug false -e class 'com.woocommerce.android.quicklogin.QuickLoginWordpress' $packageName.quicklogin/androidx.test.runner.AndroidJUnitRunner
eval $adbPath shell am start -n $packageName/com.woocommerce.android.ui.main.MainActivity
