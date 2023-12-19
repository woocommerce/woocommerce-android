# Coding Style

Our code style guidelines are based on the [Android Code Style Guidelines for Contributors](https://developer.android.com/kotlin/style-guide). We only changed a few rules:

* Line length is 120 characters with the exception of test names, which can be as long as necessary.
* FIXME must not be committed in the repository use TODO instead. FIXME can be used in your own local repository only.

We use [editorconfig](https://editorconfig.org/) to keep our setups in sync. Unfortunately, not every settings is supported, therefore if you want to use autoformatting feature of Android Studio it's better to adjust it's settings:
* Preferences -> Kotlin -> Imports -> Remove all from "Packages to use import with *"

On top of the Android linter rules (best run for this project using `./gradlew lintVanillaRelease`), we use [detekt](https://detekt.github.io/detekt/).

## Detekt

You can run detekt via a gradle command:

```
$ ./gradlew detektAll
```

It generates an HTML report in `WooCommerce/build/reports/detekt/detekt.html`.

You can also view errors and warnings in realtime with the Detekt plugin.

You can install the detekt plugin in Android Studio here:

`Android Studio > Preferences... > Plugins > detekt`

Once installed, you can configure the plugin here:

`Android Studio > Preferences... > Tools > Detekt`

From there, add and enable the custom configuration file, located at [config/detekt/detekt.yml](https://github.com/woocommerce/woocommerce-android/blob/trunk/config/detekt/detekt.yml).
