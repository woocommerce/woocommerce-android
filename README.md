[![CircleCI](https://circleci.com/gh/woocommerce/woocommerce-ios.svg?style=svg)](https://circleci.com/gh/woocommerce/woocommerce-android)

# WooCommerce for Android

Jetpack-powered WooCommerce Android app codenamed Dervish. If you're just looking to install WooCommerce for Android, you can find
it on [Google Play][woo-app]. If you're a developer wanting to contribute, read on.

## Contents

* [Setup Instructions](#setup-instructions)
* [Build & Test](#build--test)
* [Project Overview](#project-overview)
  * [OAuth2 Authentication](#oauth2-authentication)
  * [Configuration Files](#configuration-files)
  * [Setting up Checkstyle](#setting-up-checkstyle)
  * [Using ktlint](#using-ktlint)
  * [Google Configuration](#google-configuration)
* [Security](#security)
* [Help & Support](#help--support)
* [License](#license)

## Setup Instructions

1. Make sure you've installed [Android Studio][studio].
1. Clone this repository in the folder of your preference, and then enter that folder:

    ```bash
    $ git clone https://github.com/woocommerce/woocommerce-android.git
    $ cd woocommerce-android
    ```

1. Generate the developer oauth2 tokens. These values get copied into the main `gradle.properties` file in the next step. See the [OAuth2 Authentication](#oauth2-authentication) section for details.
1. Generate the `gradle.properties` file for this app:

    ```bash
    $ cp ./gradle.properties-example ./gradle.properties
    ```

1. Generate the `gradle.properties` file for the [Login Library][login-lib] dependency:

    ```bash
    $ cp ./libs/login/gradle.properties-example ./libs/login/gradle.properties
    ```

1. Open and modify the newly created `gradle.properties` files. See the [Configuration Files](#configuration-files) section for a breakdown of the properties.
1. In Android Studio, open the project from the local repository. This will auto-generate `local.properties` with the SDK location.
1. Go to Tools → AVD Manager and create an emulated device.
1. Run.

## Build & Test

To build, install, and test the project from the command line:

```bash
$ ./gradlew assembleVanillaDebug                          # assemble the debug .apk
$ ./gradlew installVanillaDebug                           # install the debug apk if you have an
                                                          # emulator or a device connected
$ ./gradlew :WooCommerce:testVanillaDebugUnitTest         # assemble, install and run unit tests
$ ./gradlew :WooCommerce:connectedVanillaDebugAndroidTest # assemble, install and run Android tests
```

## Project Overview

### OAuth2 Authentication

The WooCommerce for Android app connects to stores via WordPress.com APIs so if a WooCommerce store is not hosted on WordPress.com, it will require the [Jetpack plugin][jetpack] to setup a common interface for communicating with a self-hosted store. In order to use these APIs, you will need a client ID and a client secret key. These details will be
used to authenticate your application and verify that the API calls being
made are valid. You can create an application or view details for your existing
applications with our [WordPress.com applications manager][wp-com-apps].

When creating your application, you should select "**Native client**" for the application type.
The "**Website URL**", "**Redirect URLs**", and "**Javascript Origins**" fields are required but not used for
the mobile apps. Just use "**[https://localhost](https://localhost)**".

Once you've created your application in the [applications manager][wp-com-apps], you'll
need to edit the `./gradle.properties` file and change the
`wp.oauth.app_id` and `wp.oauth.app_secret` fields. Then you can compile and
run the app on a device or an emulator and try to login with a WordPress.com
account. Note that authenticating to WordPress.com via Google is not supported
in development builds of the app, only in the official release.

Note that credentials created with our [WordPress.com applications manager][wp-com-apps]
allow login only and not signup. New accounts must be created using the [official app][wp-app]
or [on the web](https://wordpress.com/start). Login is restricted to the WordPress.com
account with which the credentials were created. In other words, if the credentials
were created with foo@email.com, you will only be able to login with foo@email.com.
Using another account like bar@email.com will cause the `Client cannot use "password" grant_type` error.

For security reasons, some account-related actions aren't supported for development
builds when using a WordPress.com account with 2-factor authentication enabled.

Read more about [OAuth2][oauth] and the [WordPress.com REST endpoint][wp-api].

### Configuration Files

#### Main `gradle.properties`

| Property                   | Description |
|:---------------------------|:------------|
|wc.oauth.app_id            | Required to build the app. See [OAuth2 Authentication](#oauth2-authentication)|
| wc.oauth.app_secret        | Required to build the app. See [OAuth2 Authentication](#oauth2-authentication) |
| wc.zendesk_app_id          | Used for Zendesk integration. Can be ignored.|
| wc.zendesk.domain          | Used for Zendesk integration. Can be ignored.|
| wc.zendesk.oauth_client_id | Used for Zendesk integration. Can be ignored.|
| wc.reset_db_on_downgrade   | Debug builds: If `true` will drop all tables and recreate the db if a database downgrade is detected. |
| wc.sentry.dsn              | Used for Sentry integration. Can be ignored.|

#### Login library `gradle.properties`

| Property                   | Description |
|:---------------------------|:------------|
| wp.debug.wpcom_login_email |Optional: used to autofill email during login on debug build only|
| wp.debug.wpcom_login_username|Optional: used to autofill username during login on debug build only|
|wp.debug.wpcom_login_password|Optional: used to autofill password during login on debug build only|
|wp.debug.wpcom_website_url|Optional: used to autofill store url during login on debug build only|

### Setting up Checkstyle

The woocommerce-android project uses [Checkstyle][checkstyle]. You can run checkstyle using `./gradlew checkstyle`. You can also view errors and warnings in realtime with the Checkstyle plugin.  When importing the project into Android Studio, Checkstyle should be set up automatically. If it is not, follow the steps below.

You can install the CheckStyle-IDEA plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > CheckStyle-IDEA`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Other Settings > Checkstyle`

From there, add and enable the configuration file for woocommerce-android, located at [config/checkstyle.xml](https://github.com/woocommerce/woocommerce-android/blob/develop/config/checkstyle.xml).

### Using ktlint

The woocommerce-android project uses [ktlint][ktlint] for Kotlin linting. You can run ktlint using `./gradlew ktlint`, and you can also run `./gradlew ktlintFormat` for auto-formatting. There is no IDEA plugin (like Checkstyle's) at this time.

### Google Configuration

Google Sign-In is only available for WordPress.com accounts through the [official app][woo-app].
Contributors can build and run the app without issue, but Google Sign-In will always fail.
Google Sign-In requires configuration files which contain client and server information
that can't be shared publicly. More documentation and guides can be found on the
[Google Identity Platform website][google-ident].

## Security

If you happen to find a security vulnerability, we would appreciate you letting us know at [https://hackerone.com/automattic](https://hackerone.com/automattic) and allowing us to respond before disclosing the issue publicly.

## Resources

* [Mobile Blog][blog-mobile]
* [WooCommerce Rest API Documentation][woo-rest-docs]
* [WooCommerce for Android Docs](https://docs.woocommerce.com/document/android/)
* [FluxC Library (Github)][wp-fluxc]
* [Login Library (Github)][login-lib]

## Help & Support

Usage docs can be found here: [docs.woocommerce.com][woo-docs]

General usage and development questions:

* [WooCommerce Slack Community][wc-slack]
* [WordPress.org Forums](https://wordpress.org/support/plugin/woocommerce)
* [The WooCommerce Help and Share Facebook group][woo-facebook]
* Say hello on our [Slack][wc-slack] channel: #mobile

## License

WooCommerce for Android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE). Note: code
in the `libs/` directory comes from external libraries, which might
be covered by a different license compatible with the GPLv2.

<!-- HTML Links -->
[checkstyle]: https://checkstyle.sourceforge.io/
[google-ident]: https://developers.google.com/identity/
[jetpack]: https://jetpack.com/
[ktlint]: https://github.com/shyiko/ktlint
[login-lib]: https://github.com/wordpress-mobile/WordPress-Login-Flow-Android
[oauth]: https://developer.wordpress.com/docs/oauth2/
[blog-mobile]: https://mobile.blog/
[studio]: https://developer.android.com/studio
[woo-app]: https://play.google.com/store/apps/details?id=com.woocommerce.android&referrer=utm_source%3Dgithub%26utm_medium%3Dwebsite
[woo-docs]: https://docs.woocommerce.com/
[woo-facebook]: https://www.facebook.com/groups/woohelp/
[woo-rest-docs]: https://woocommerce.github.io/woocommerce-rest-api-docs/#introduction
[wp-api]: https://developer.wordpress.com/docs/api/
[wp-app]: https://play.google.com/store/apps/details?id=org.wordpress.android
[wp-com-apps]: https://developer.wordpress.com/apps/
[wp-fluxc]: https://github.com/wordpress-mobile/WordPress-FluxC-Android
[wc-slack]: https://woocommerce.com/community-slack/


<p align="center">
    <br/><br/>
    Made with 💜 by <a href="https://woocommerce.com/">WooCommerce</a>.<br/>
    <a href="https://woocommerce.com/careers/">We're hiring</a>! Come work with us!
</p>
