#!/bin/bash -eu

# PASS WORDPRESS_EMAIL and WORDPRESS_PASSWORD as env variable to the script
# Example: WORDPRESS_EMAIL=my_wp_email@gmail.com  WORDPRESS_PASSWORD=my_pasword_1_# /quicklogin/woo_login.sh

~/Library/Android/sdk/platform-tools/adb shell pm clear com.woocommerce.android.prealpha

./gradlew installJalapenoDebug
./gradlew :quicklogin:installDebug \
-PquickLoginWpEmail="$WORDPRESS_EMAIL" \
-PquickLoginWpPassword="$WORDPRESS_PASSWORD"

~/Library/Android/sdk/platform-tools/adb shell am instrument -w -r -e debug false -e class 'com.woocommerce.android.quicklogin.QuickLoginWordpress' com.woocommerce.android.prealpha.quicklogin/androidx.test.runner.AndroidJUnitRunner
~/Library/Android/sdk/platform-tools/adb shell am start -n com.woocommerce.android.prealpha/com.woocommerce.android.ui.main.MainActivity
