# Localization

During development, adding a string in the [`values/strings.xml`](../WooCommerce/src/main/res/values/strings.xml) resource and using it in the code or layout file should be enough.

**Important:** `plurals` are not supported at the moment. Use `StringUtils::getQuantityString` method.

```xml
<!-- strings.xml -->
<string name="orderdetail_shipping_details">Shipping details</string>
```

```kotlin
// In code
val label = context.getString(R.string.orderdetail_shipping_details)
```

```xml
<!-- layout.xml -->
<TextView
    ...
    android:text="@string/orderdetail_shipping_details"
    ...
    />
```

We also have string resources outside of `strings.xml` such as `key_strings`. These strings are not user-facing and should be used as static strings such as preference keys.

To help ease the translation process we ask that you mark alias string resources - as well as other strings where appropriate - as not translatable. For example `<string name="foo" translatable="false">@string/bar</string>`

You shouldn't need to touch the `strings.xml` for the other languages. During the release process, the `values/strings.xml` file is uploaded to [GlotPress](https://translate.wordpress.com/projects/woocommerce/woocommerce-android/) for translation. Before the release build is finalized, all the translations are grabbed from GlotPress and saved back to their appropriate `values-[lang_code]/strings.xml` file.

## Use Meaningful Names

Meaningful names help give more context to translators. Whenever possible, the first part of the `name` should succinctly describe where the string is used.

```xml
<!-- Do -->
<string name="orderdetail_shipping_details">Shipping details</string>
```

```xml
<!-- Avoid -->
<string name="shipping_details">Shipping details</string>
```

If the string is for a [`contentDescription`](https://developer.android.com/reference/android/view/View.html#attr_android:contentDescription), consider adding `_content_description` to the end.

```xml
<string name="product_image_content_description">Product image</string>
```

## Use Placeholders Instead of Concatenation

Concatenating strings to include dynamic values splits them into separate translatable items. The completed (joined) sentence may end up not being grammatically correct, especially for RTL languages.

```xml
<!-- Don't -->
<string name="continue_terms_of_service_text_first_part">By continuing, you agree to our</string>
<string name="continue_terms_of_service_text_second_part"> Terms of Service.</string>
```

```kotlin
// Don't
val label = context.getString(string.continue_terms_of_service_text_first_part) +
        " $title " + context.getString(string.continue_terms_of_service_text_second_part) + " $productName"
```

Use placeholders instead. They give more context and enables translators to move them where they make sense.

```xml
<!-- Do -->
<string name="continue_terms_of_service_text">By continuing, you agree to our %1$sTerms of Service%2$s.</string>
```

```kotlin
// Do
val label = String.format(
        context.getString(string.continue_terms_of_service_text),
        title, productName
)
```

Also consider adding information about what the placeholders are in the `name`.

## Pluralization

GlotPress currently does not support pluralization using [Quantity strings](https://developer.android.com/guide/topics/resources/string-resource.html#Plurals). So, right now, you have to support plurals manually by creating separate strings.

```xml
<string name="product_downloadable_files_value_multiple">%1$d files</string>
    <string name="product_downloadable_files_value_single">1 file</string>
```

```kotlin
val message = if (downloadableFileCount == 1) {
    context.getString(string.product_downloadable_files_value_single)
} else {
    String.format(
            context.getString(string.product_downloadable_files_value_multiple),
            downloadableFileCount
    )
}
```
