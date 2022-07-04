# Feature Flags

In WooCommerce Android, we use [feature flags](https://martinfowler.com/articles/feature-toggles.html) to allow us to merge in-progress features into `trunk`, while still allowing us to safely deliver builds for testing and production. It's mostly useful for features that require multiple PRs to ship, and may take some time to complete.

We currently do this through [`FeatureFlag`](https://github.com/woocommerce/woocommerce-android/blob/trunk/WooCommerce/src/main/kotlin/com/woocommerce/android/util/FeatureFlag.kt).

### FeatureFlag

The `FeatureFlag` enum contains a case for any in-progress features that are currently feature flagged. It contains a single method `isEnabled`. This determines whether the feature should currently be enabled. This is typically determined based on the current `BuildConfig`. Here are a couple of examples:

```kotlin
enum class FeatureFlag {
    SHIPPING_LABELS_M4,
    DB_DOWNGRADE,
    ORDER_CREATION,
    CARD_READER;

    /// Returns a boolean indicating if the feature is enabled
    fun isEnabled(context: Context? = null): Boolean {
        return when (this) {
            SHIPPING_LABELS_M4 -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            DB_DOWNGRADE -> {
                PackageUtils.isDebugBuild() || context != null && PackageUtils.isBetaBuild(context)
            }
            ORDER_CREATION -> PackageUtils.isDebugBuild() || PackageUtils.isTesting()
            CARD_READER -> CardPresentEligibleFeatureChecker.isCardPresentEligible
        }
    }
}
```

Here, we have three features which will be active in the following circumstances:

* `SHIPPING_LABELS_M4` will only be enabled in debug builds and during tests until the feature is released.
* `DB_DOWNGRADE` will only be enabled for debug / beta builds
* `CARD_READER` will be enabled if the store is currently eligible for payments i.e when WooCommerce Payments plugin is available.

### Putting it all together

The final step is to check the current status of a feature flag to selectively enable the feature within the app. For example, this might be displaying a button in the UI if a feature is enabled, or perhaps switching to a different code path in a service:

```kotlin
if (!FeatureFlag.SHIPPING_LABELS_M4.isEnabled()) {
    binding.expandIcon.isVisible = false
} else {
    binding.expandIcon.isVisible = true
}
```

Once a feature is ready for release, you can remove the feature flag and the old code path.
