

<h1 align="center"><img src="docs/images/logo-woocommerce.svg" width="300"><br>for Android</h1>

<p align="center">A Jetpack-powered mobile app for WooCommerce.</p>

<p align="center">
    <a href="https://circleci.com/gh/woocommerce/woocommerce-android">
        <img src="https://circleci.com/gh/woocommerce/woocommerce-android.svg?style=shield" alt="CircleCI">
    </a>
    <a href="https://github.com/woocommerce/woocommerce-android/releases">
        <img alt="Release" src="https://img.shields.io/github/v/tag/woocommerce/woocommerce-android?label=release&sort=semver">
    </a>
    <a href="https://github.com/woocommerce/woocommerce-android/blob/trunk/LICENSE.md">
        <img alt="License" src="https://img.shields.io/github/license/woocommerce/woocommerce-android">
    </a>
</p>

<p align="center">
    <a href="#setup-instructions">Setup Instructions</a> •
    <a href="#build--test">Build & Test</a> •
    <a href="#-documentation">Documentation</a> •
    <a href="#-contributing">Contributing</a> •
    <a href="#-automation">Automation</a> •
    <a href="#-security">Security</a> •
    <a href="#-need-help">Need Help?</a> •
    <a href="#-resources">Resources</a> •
    <a href="#-license">License</a>
</p>

## 🎉 Setup Instructions

1. Make sure you've installed [Android Studio](https://developer.android.com/studio).
1. Clone this repository in the folder of your preference, and then enter that folder:

    ```bash
    $ git clone https://github.com/woocommerce/woocommerce-android.git
    $ cd woocommerce-android
    ```

1. Generate the developer oauth2 tokens. These values get copied into the main `gradle.properties` file in the next step. See the [OAuth2 Authentication](docs/project-overview.md#oauth2-authentication) section for details.
1. Generate the `gradle.properties` file for this app:

    ```bash
    $ cp ./gradle.properties-example ./gradle.properties
    ```

1. Open and modify the newly created `gradle.properties` files. See the [Configuration Files](docs/project-overview.md#configuration-files) section for a breakdown of the properties.
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

## 📚 Documentation

- Project Overview
    - [OAuth2 Authentication](docs/project-overview.md#oauth2-authentication)
    - [Configuration Files](docs/project-overview.md#configuration-files)
    - [Setting up Checkstyle](docs/project-overview.md#setting-up-checkstyle)
    - [Using Detekt](docs/project-overview.md#using-detekt)
    - [Google Configuration](docs/project-overview.md#google-configuration)
- Development Practices
    - [Coding Style Practices](docs/coding-style.md)
    - [Pull Request Guidelines](docs/pull-request-guidelines.md)
    - [Material Theme Designs](docs/material-theme-designs.md)
    - [Using Android Resources](docs/using-android-resources.md)
    - [Localization](docs/localization.md)
    - [Themes & Styling Practices](docs/theming-styling-best-practices.md)
    - [Subtree'd Library Projects](docs/subtreed-library-projects.md)
- Data
    - [Tracking Events](docs/tracking-events.md)
- Accessibility
    - [Accessibility Guidelines](docs/accessibility-guidelines.md)
    - [Right to Left Layout Guidelines](docs/right-to-left-layout-guidelines.md)
- Quality & Testing
    - [Beta Testing](https://woocommercehalo.wordpress.com/setup/join-android-beta/)
    - [Issue Triage](docs/issue-triage.md)
- Features
    - [Feature Flags](docs/feature-flags.md)

## 👏 Contributing

Read our [Contributing Guide](CONTRIBUTING.md) to learn about reporting issues, contributing code, and more ways to contribute.

## 🔐 Security

If you happen to find a security vulnerability, we would appreciate you letting us know at https://hackerone.com/automattic and allowing us to respond before disclosing the issue publicly.

## 🦮 Need Help?

You can find the WooCommerce usage docs here: [docs.woocommerce.com](https://docs.woocommerce.com/)

General usage and development questions:

* [WooCommerce Slack Community](https://woocommerce.com/community-slack/)
* [WordPress.org Forums](https://wordpress.org/support/plugin/woocommerce)
* [The WooCommerce Help and Share Facebook group](https://www.facebook.com/groups/woohelp/)

## 🔗 Resources

- [Mobile blog](https://mobile.blog)
- [WooCommerce API Documentation (currently v3)](https://woocommerce.github.io/woocommerce-rest-api-docs/#introduction)

## 📜 License

WooCommerce for Android is an Open Source project covered by the [GNU General Public License version 2](https://github.com/woocommerce/woocommerce-android/blob/trunk/LICENSE.md).


<p align="center">
    <br/><br/>
    Made with 💜 by <a href="https://woocommerce.com/">WooCommerce</a>.<br/>
    <a href="https://woocommerce.com/careers/">We're hiring</a>! Come work with us!
</p>
