# Localization

During development, adding a string in the [`values/strings.xml`](../WooCommerce/src/main/res/values/strings.xml) resource and using it in the code or layout file should be enough.

**Important:** keep using the manual `_single`/`_multiple` plural convention (see [Pluralization](#pluralization)) and the `StringUtils::getQuantityString` method. Real CLDR/ICU `<plurals>` are a deferred cross-platform follow-up.

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

You shouldn't need to touch the `strings.xml` for the other languages. **GlotPress is retired.** Translations are produced by the self-contained AI translation engine in [`fastlane/ai_translation`](../fastlane/ai_translation/README.md):

- A **PR-time CI check** translates only the keys your PR adds/changes (delta vs `fastlane/ai_translation/translation-manifest.json`) for every supported locale, and a bot commits the resulting `values-[lang_code]/strings.xml` + manifest back to your PR branch so the change is reviewable inline and `trunk` stays fully translated.
- A **code-freeze reconciliation sweep** re-checks every key × every locale (the safety net) and translates per-release Play Store notes.
- AI ships by default; human review is sampled and **non-blocking**. Hard, blocking gates run on every translation: placeholder parity, XML well-formedness, key parity, and plural-pair output integrity.

You normally don't run anything by hand. To translate locally: `bundle exec fastlane ai_translate mode:prtime` (needs `ANTHROPIC_API_KEY`), or a no-spend dry run: `ruby fastlane/ai_translation/bin/woo-ai-translate --offline --locales pl,cs ...`.

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

The project keeps a **manual** `_single`/`_multiple` plural convention rather than Android [`<plurals>`](https://developer.android.com/guide/topics/resources/string-resource.html#Plurals). The AI engine translates each variant independently and never collapses a pair. Migrating to real CLDR/ICU plurals (Android `<plurals>` + the Kotlin call-site migration) is an **explicitly deferred, separately-tracked cross-platform follow-up**.

> **Known v1 limitation:** the 2-form `_single`/`_multiple` convention is linguistically incomplete for the new Slavic locales (`pl`, `cs`, `uk`, `bg`) and already imperfect for `ru`. This is accepted until the CLDR plural migration lands.

Support plurals manually by creating separate strings:

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

## Open questions

- **Store metadata locale support can differ from Android resources.** `SUPPORTED_LOCALES` keeps separate Android resource qualifiers and Google Play metadata codes. For example, Norwegian uses Android `nb` resources, but Google Play metadata uses `no-NO`.
- **WPCOM import cron retirement.** `wpcom/bin/i18n/import-github-originals.php` imported the frozen `strings.xml` into GlotPress. With GlotPress gone it must be retired — an Apps Infra handoff, outside this repo.
- **Screenshots stay English** for all locales (existing 16 included). Localized screenshots (assets + overlay text) are a separate content-ops follow-up; the AI pipeline never touches `promo_screenshot_*.txt`.
