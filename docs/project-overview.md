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
| wc.reset_db_on_downgrade   | Debug/Beta builds: If `true` will drop all tables and recreate the db if a database downgrade is detected. |
| wc.sentry.dsn              | Used for Sentry integration. Can be ignored.|

### Setting up Checkstyle

The woocommerce-android project uses [Checkstyle][checkstyle]. You can run checkstyle using `./gradlew checkstyle`.

Optionally, you can also view errors and warnings in realtime with the Checkstyle plugin. Follow the steps below if needed.

You can install the CheckStyle-IDEA plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > CheckStyle-IDEA`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Other Settings > Checkstyle`

From there, add and enable the configuration file for woocommerce-android, located at [config/checkstyle.xml](https://github.com/woocommerce/woocommerce-android/blob/develop/config/checkstyle.xml).

### Using detekt

The woocommerce-android project uses [detekt][detekt] for Kotlin linting and code style check.

You can run detekt using `./gradlew detekt`.

You can also view errors and warnings in realtime with the Detekt plugin.

You can install the detekt plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > detekt`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Tools > Detekt`

From there, add and enable the custom configuration file, located at [config/detekt/detekt.yml](https://github.com/wordpress-mobile/WordPress-Android/blob/develop/config/detekt/detekt.yml).

If you want to use the **AutoCorrect** feature of the plugin, make sure that the option `Enable formatting (ktlint) rules` is enabled in the above settings, then you will be able to reformat any file according to detekt's rules using the refactor menu `AutoCorrect by Detekt Rules`

### Google Configuration

Google Sign-In is only available for WordPress.com accounts through the [official app][woo-app].
Contributors can build and run the app without issue, but Google Sign-In will always fail.
Google Sign-In requires configuration files which contain client and server information
that can't be shared publicly. More documentation and guides can be found on the
[Google Identity Platform website][google-ident].



[wp-com-apps]: https://developer.wordpress.com/apps/
[wp-app]: https://apps.wordpress.com/mobile/
[wp-api]: https://developer.wordpress.org/rest-api/
[oauth]: https://oauth.net
[google-ident]: https://cloud.google.com/identity-platform/docs/
[detekt]: https://detekt.github.io/detekt/
[jetpack]: https://wordpress.org/plugins/jetpack/
[checkstyle]: https://checkstyle.org
