# woocommerce-android

[![CircleCI](https://circleci.com/gh/woocommerce/woocommerce-ios.svg?style=svg)](https://circleci.com/gh/woocommerce/woocommerce-android)

Jetpack-powered WooCommerce Android app codenamed Dervish.

## Build Instructions ##

(Make sure you've installed [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Android Studio](https://developer.android.com/studio/index.html).)

You first need to generate the `gradle.properties` file:

    $ cp ./gradle.properties-example ./gradle.properties

Next, you'll have to get a WordPress.com OAuth2 ID and secret, for use in the `gradle.properties` file. Please read the
[OAuth2 Authentication](#oauth2-authentication) section.

**In Android Studio, don't accept any offers to upgrade the gradle plugin - this project does not support Gradle plugin
3 yet.**

## Running Tests ##

    $ ./gradlew testVanillaDebug                     # Unit tests
    $ ./gradlew connectedVanillaDebugAndroidTest     # UI tests

## Setting up Checkstyle ##

The woocommerce-android project uses [Checkstyle](http://checkstyle.sourceforge.net/). You can run checkstyle using `./gradlew checkstyle`.  You can also view errors and warnings in realtime with the Checkstyle plugin.  When importing the project into Android Studio, Checkstyle should be set up automatically.  If it is not, follow the steps below.

You can install the CheckStyle-IDEA plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > CheckStyle-IDEA`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Other Settings > Checkstyle`

From there, add and enable the configuration file for woocommerce-android, located at [config/checkstyle.xml](https://github.com/woocommerce/woocommerce-android/blob/develop/config/checkstyle.xml).

## Using ktlint ##

The woocommerce-android project uses [ktlint](https://github.com/shyiko/ktlint) for Kotlin linting. You can run ktlint using `./gradlew ktlint`, and you can also run `./gradlew ktlintFormat` for auto-formatting. There is no IDEA plugin (like Checkstyle's) at this time.

## OAuth2 Authentication ##

You will need a client ID and a client secret key. These details will be
used to authenticate your application and verify that the API calls being
made are valid. You can create an application or view details for your existing
applications with our [WordPress.com applications manager](https://developer.wordpress.com/apps/).

When creating your application, you should select "Native client" for the application type.
The "Website URL", "Redirect URLs", and "Javascript Origins" fields are required but not used for
the mobile apps. Just use "https://localhost".

Once you've created your application in the [applications manager](https://developer.wordpress.com/apps/), you'll
need to edit the `./gradle.properties` file and change the
`wp.oauth.app_id` and `wp.oauth.app_secret` fields. Then you can compile and
run the app on a device or an emulator and try to login with a WordPress.com
account.

Read more about [OAuth2](https://developer.wordpress.com/docs/oauth2/) and the [WordPress.com REST endpoint](https://developer.wordpress.com/docs/api/).

## Google Configuration ##

Google Sign-In is only available for WordPress.com accounts through the official app.
Contributors can build and run the app without issue, but Google Sign-In will always fail.
Google Sign-In requires configuration files which contain client and server information
that can't be shared publicly. More documentation and guides can be found on the
[Google Identity Platform website](https://developers.google.com/identity/).

## Need help? ##

Usage docs can be found here: [docs.woocommerce.com](https://docs.woocommerce.com/)

General usage and development questions:

* [WooCommerce Slack Community](https://woocommerce.com/community-slack/)
* [WordPress.org Forums](https://wordpress.org/support/plugin/woocommerce)
* The WooCommerce Help and Share Facebook group

## License ##

woocommerce-android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE). Note: code
in the `libs/` directory comes from external libraries, which might
be covered by a different license compatible with the GPLv2.
