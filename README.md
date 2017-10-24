# woocommerce-android

[![Build Status](https://travis-ci.com/woocommerce/woocommerce-android.svg?token=umh3yUbJENBttxqUrtQX&branch=develop)](https://travis-ci.com/woocommerce/woocommerce-android)

Jetpack-powered WooCommerce Android app codenamed Dervish.

## Build Instructions ##

You first need to generate the `gradle.properties` file:

    $ cp ./gradle.properties-example ./gradle.properties

Next, you'll have to get a WordPress.com OAuth2 ID and secret, for use in the `gradle.properties` file. Please read the
[OAuth2 Authentication](#oauth2-authentication) section.

## OAuth2 Authentication ##

You will need a client ID and a client secret key. These details will be
used to authenticate your application and verify that the API calls being
made are valid. You can create an application or view details for your existing
applications with our [WordPress.com applications manager](https://developer.wordpress.com/apps/).

When creating your application, you should select "Native client" for the
application type. The applications manager currently requires a "redirect URL",
but this isn't used for mobile apps. Just use "https://localhost".

Once you've created your application in the [applications manager](https://developer.wordpress.com/apps/), you'll
need to edit the `./gradle.properties` file and change the
`wp.oauth.app_id` and `wp.oauth.app_secret` fields. Then you can compile and
run the app on a device or an emulator and try to login with a WordPress.com
account.

Read more about [OAuth2](https://developer.wordpress.com/docs/oauth2/) and the [WordPress.com REST endpoint](https://developer.wordpress.com/docs/api/).

## How we work ##

TODO

## Need help to build? ##

TODO

## License ##

woocommerce-android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE). Note: code
in the `libs/` directory comes from external libraries, which might
be covered by a different license compatible with the GPLv2.
