# Android AppFunctions (proof of concept)

[AppFunctions](https://developer.android.com/ai/appfunctions) lets an Android app expose functions that an on-device
agent, such as Gemini, can discover and call. The app describes each function in KDoc, a KSP processor turns the
declarations into metadata that Android indexes, and the agent calls the function in the app's process, in the
background, with the app's own login.

This proof of concept (WOOMOB-4318) exposes six store functions that reuse the in-app Assistant's tools. It uses
`androidx.appfunctions` `1.0.0-rc01` and only works in debug builds. Gemini can't call it yet: Gemini access to
third-party AppFunctions is a private preview that Google controls. Today you call the functions from `adb` or from
Google's Testing Agent.

## Status

| Android version | What we saw |
|---|---|
| Android 17 (API 37.0) | All six functions are indexed and work from `adb` and from Google's Testing Agent, including a minified, non-debuggable build. |
| Android 16 QPR2 (API 36.1) | All six functions are indexed. There is no `list-app-functions --package` filter on this version. |
| Android 16.0 (API 36.0) | Nothing is indexed with the documented setup, and `cmd app_function` doesn't exist. The app runs normally. |
| Android 15 and older | The app runs normally. The Assistant access setting is hidden. |

## How it works

1. KSP generates `StoreAppFunctionService` from the abstract `BaseStoreAppFunctionService`, plus the metadata XML that
   Android indexes. The generated service is declared in the feature module's manifest and protected by
   `android.permission.BIND_APP_FUNCTION_SERVICE`, so only the system can bind to it.
2. Each function in `BaseStoreAppFunctionService` switches to the IO dispatcher and calls `StoreAppFunctions`.
3. `StoreAppFunctions` checks access, captures the selected store, builds a `ToolCall` for one of the Assistant's
   existing tools, runs it through `ToolRegistry`, and maps the JSON result to a small `@AppFunctionSerializable`
   class. Before returning, it checks that the store didn't change.
4. Changes go through the app's own UI. `requestStockChange` doesn't write anything: it returns a `PendingIntent` to
   `AppFunctionConfirmationActivity`, which shows the Assistant's confirmation preview and card. The write runs only
   when the merchant taps Confirm, and the request is marked as used before it runs, so it can't run twice.
5. `openOrderInApp` returns a `PendingIntent` to `AppFunctionOpenOrderActivity`. It checks access and the store again,
   then opens the order through `MainActivity`. It never switches stores.

All functions are declared with `isEnabled = false`. In debug builds, the host's `WooAppFunctionsAccess` enables them
when the user is logged in, a store is selected and the Assistant access switch is on, and disables them otherwise.
Release builds never call the system's enable API, and every call checks `AppFunctionsAccessPolicy` again, which
rejects release builds.

### Where the code lives

| Area | Files |
|---|---|
| Functions and result types | `libs/ai-assistant/feature/.../aiassistant/appfunctions/BaseStoreAppFunctionService.kt`, `StoreAppFunctionModels.kt` |
| Tool calls, mapping and store checks | `StoreAppFunctions.kt`, `AppFunctionsStoreAccess.kt` |
| Enabled state | `StoreAppFunctionsAvailability.kt` (feature), `WooCommerce/.../ui/aiassistant/appfunctions/WooAppFunctionsAccess.kt` (host) |
| Stock confirmation | `StoreAppFunctionIntents.kt`, `AppFunctionConfirmationActivity.kt`, `AppFunctionConfirmationViewModel.kt`, `AppFunctionDialogs.kt` |
| Order hand-off | `AppFunctionOpenOrderActivity.kt`, host `WooAppFunctionsHostNavigation.kt` and `MainActivity` |
| Host contracts | `AppFunctionsHost.kt` (`AppFunctionsAccessPolicy`, `AppFunctionsHostNavigation`), bound in host `AppFunctionsHostModule.kt` |
| Manifest and agent instructions | `libs/ai-assistant/feature/src/main/AndroidManifest.xml`, `res/xml/ai_assistant_app_functions_metadata.xml` |
| Gradle | `androidx-appfunctions` in `gradle/libs.versions.toml`; `implementation` and `ksp` in `libs/ai-assistant/feature/build.gradle` |
| Merchant switch | Settings > Experimental features ("Assistant access"), `BetaFeaturesFragment` |

The library needs compileSdk 37 and AGP 9.1 or newer. Its KSP processor runs only in `:libs:ai-assistant:feature`.

## The functions

Function IDs are `com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#<name>`. In code, use
the generated `StoreAppFunctionService.FUNCTION_ID_*` constants.

| Function | Inputs | Result |
|---|---|---|
| `getSalesSummary` | `period`: `today`, `yesterday`, `last_7_days` or `this_month` | Store name, dates (store time zone), currency, net sales, order count, average order value, items sold |
| `findOrders` | Optional `searchText`, `status`, `limit` (10 by default, 50 at most) | Matching orders, newest first, and the store name |
| `getOrderDetails` | `orderId` | Status, dates, totals, the customer's name, up to 10 items, and the store name |
| `findProducts` | Optional `searchText`, exact `sku`, `stockStatus`, `limit` | Matching products with price and stock, and the store name |
| `requestStockChange` | `productId`, optional `variationId`, `newQuantity` (0 or more) | A `PendingIntent` to the confirmation screen. Nothing changes until the merchant confirms. |
| `openOrderInApp` | `orderId` | A `PendingIntent` that opens the order in the app |

## Rules

- **Store:** every call uses the store selected in the app. There is no store parameter. A call captures the store's
  local site ID and URL and checks them again before it returns. The Assistant's data sources read the selected store
  themselves, so this check detects a store switch but doesn't pin the store for the whole call.
- **Privacy:** order results include the customer's name only. Email, phone, addresses, payment method, notes and
  customer ID are left out. Errors never include tokens, raw payloads or transport diagnostics.
- **Merchant switch:** "Assistant access" in Settings > Experimental features. It shows in debug builds on Android 16
  and later, is on by default, applies to whichever store is open, and survives logout. Turning it off disables every
  function and refuses confirmations that were issued earlier.
- **Errors:**

| Code | Exception | When |
|---|---|---|
| 1001 | `AppFunctionInvalidArgumentException` | A bad argument, such as an unknown period or a negative quantity, or a variable product without `variationId` (the message lists its variations) |
| 1002 | Raised by the system | The function is disabled: logged out, no store, switch off, or a release build |
| 1500 | `AppFunctionElementNotFoundException` | The order or product doesn't exist in the selected store |
| 3000 | `AppFunctionAppUnknownException` | Any other tool failure, with a short message |
| 3501 | `AppFunctionNotSupportedException` | Access isn't allowed right now, or the store changed during the call |

## How to test

### Set up an emulator

`cmd app_function` needs an Android 17 image. Android 16.0 images don't have it.

```bash
sdkmanager "system-images;android-37.0;google_apis;arm64-v8a"
avdmanager create avd -n Pixel_8a_API_37_AppFunctions -k "system-images;android-37.0;google_apis;arm64-v8a" -d pixel_8a
emulator -avd Pixel_8a_API_37_AppFunctions -no-snapshot-save &
```

Then install the debug build and log in with the auto-login helper (it needs a local profile, see
[agent-auto-login.md](../../../.agents/skills/verify-on-device/references/agent-auto-login.md)):

```bash
SERIAL=emulator-5554
./gradlew :WooCommerce:assembleWasabiDebug
adb -s "$SERIAL" install -r WooCommerce/build/outputs/apk/wasabi/debug/WooCommerce-wasabi-debug.apk
tools/agent-login/agent-login.sh --flavor dev --serial "$SERIAL"
```

Check that Assistant access is on in Settings > Experimental features.

### Call the functions from adb

List the functions and check whether one is enabled:

```bash
adb -s "$SERIAL" shell "cmd app_function list-app-functions --package com.woocommerce.android.dev"
adb -s "$SERIAL" shell "cmd app_function is-enabled --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#findOrders'"
```

Run the read functions (replace the order ID with one from your test store):

```bash
adb -s "$SERIAL" shell "cmd app_function execute-app-function --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#getSalesSummary' --parameters '{\"period\":\"today\"}'"
adb -s "$SERIAL" shell "cmd app_function execute-app-function --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#findOrders' --parameters '{\"limit\":1}'"
adb -s "$SERIAL" shell "cmd app_function execute-app-function --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#getOrderDetails' --parameters '{\"orderId\":2692}'"
adb -s "$SERIAL" shell "cmd app_function execute-app-function --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#findProducts' --parameters '{\"stockStatus\":\"instock\",\"limit\":1}'"
```

The two hand-off functions return a `PendingIntent`. `--pending-intent-path property/androidAppfunctionsReturnValue`
makes the shell open it, the way an agent would after the user approves. The first command opens the stock
confirmation (tap Cancel unless you want to change the test store's stock), and the second opens the order:

```bash
adb -s "$SERIAL" shell "cmd app_function execute-app-function --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#requestStockChange' --parameters '{\"productId\":74,\"newQuantity\":3}' --pending-intent-path property/androidAppfunctionsReturnValue"
adb -s "$SERIAL" shell "cmd app_function execute-app-function --package com.woocommerce.android.dev --function 'com.woocommerce.android.aiassistant.appfunctions.BaseStoreAppFunctionService#openOrderInApp' --parameters '{\"orderId\":2692}' --pending-intent-path property/androidAppfunctionsReturnValue"
```

To check the access rules, turn Assistant access off or log out, then run any command again: it fails with code 1002.

### Use Google's Testing Agent

Google's [Testing Agent](https://github.com/android/appfunctions/tree/main/agent) is an app that lists and runs any
app's functions, the way an agent would. We tested it at commit `286a61a1`.

1. Clone `https://github.com/android/appfunctions` outside this repository.
2. From its `agent/` folder, with `ANDROID_HOME` pointing at your Android SDK, run
   `./run_privileged.sh --build -s "$SERIAL"`. The standard flavor needs no API key. The script installs the agent and
   gives it shell permissions on the emulator.
3. In the agent's Debug tab, pick "Woo (Dev)", expand a function, fill in its parameters and tap Run.

What you should see:

- The read functions show their structured results. The agent shows function names and parameters but not our KDoc
  descriptions.
- For `requestStockChange`, the agent shows the call with a "You should only open the app from trusted source" warning
  and Open and Dismiss buttons. Open shows our confirmation screen over the agent, with the store, the product and the
  current and new quantities.
- If Assistant access is turned off before tapping Open, our screen says the request is no longer available and
  nothing was changed.
- `openOrderInApp` opens the order screen in the app.

Uninstall the agent when you're done (`adb -s "$SERIAL" uninstall com.example.appfunctions.agent`).

### Unit tests and release builds

The tests live next to the code: `StoreAppFunctionsTest`, `AppFunctionsStoreAccessTest`,
`AppFunctionConfirmationViewModelTest` and `AppFunctionOpenOrderViewModelTest` in the feature module, and
`WooAppFunctionsAccessTest` in the host app. Run them with `./gradlew :libs:ai-assistant:feature:testDebugUnitTest`
and `./gradlew :WooCommerce:testWasabiDebugUnitTest`.

The feature module's test task sets `kotlinx.coroutines.stacktrace.recovery=false`. Without it, a test hangs when a
function throws an `AppFunctionException`.

The library ships its own R8 rules, and we didn't need any of ours. A release build keeps the service and the
generated classes. To check runtime behavior under full R8, build the debug type with `minifyEnabled true` and
`debuggable false`, temporarily make `WooAppFunctionsAccess` allow access (release builds are rejected by design), and
install it over a logged-in debug build. Don't commit that change.

## How to add a function

1. Add a `suspend` function to `BaseStoreAppFunctionService` with `@AppFunctionDeclaration(isDescribedByKDoc = true,
   isEnabled = false)`. Its KDoc is what the agent reads: put the key information first, describe each `@param` and
   the `@return`, and don't write it as a prompt. Declare optional parameters as nullable with `= null`.
2. Return a small `@AppFunctionSerializable` data class with a KDoc comment on each property. Use `String` or `Int`
   with `@AppFunctionStringValueConstraint` or `@AppFunctionIntValueConstraint` instead of enums.
3. Implement it in `StoreAppFunctions` with the existing `ToolRegistry`: check access first, check the store again
   before returning, and map failures to the exceptions in the error table.
4. Add the generated `FUNCTION_ID_*` constant to `StoreAppFunctionsAvailability`, so the function follows the
   enabled-state rules.
5. For anything that changes store data, return a `PendingIntent` to a confirmation screen instead of writing.
6. Add tests for each real branch, then check the description with `list-app-functions`.

Things that bite:

- Give every optional parameter a default, such as `= null`. The compiler marks a parameter as optional only when it
  has a default value; without one, agents must send something, and Gemini sends an empty string, which fails the
  `@AppFunctionStringValueConstraint` check. The default value itself is ignored: a missing argument arrives as `null`,
  `0`, `false` or an empty list, so resolve real defaults in the function body.
- Throwing an `Error` (for example `TODO()`) crashes the app. Throw an `AppFunctionException` subclass.
- Renaming the function, `BaseStoreAppFunctionService` or its package changes the function ID.

## Known limitations

- Android 16.0 doesn't index the functions. A legacy index option (`appfunctions:generateV1Xml`) exists, but even with
  it the functions can't be searched, so the POC doesn't use it.
- The store check detects a store switch during a call, but the Assistant's data sources aren't pinned to the captured
  store.
- Stock previews read the product cache first, so the "before" quantity can be stale.
- Order results don't list gift cards or fees, so a total can look lower than the items plus tax.
- Variation IDs are only discoverable through the error that `requestStockChange` returns for a variable product.
- There are no analytics events yet, and Google's Testing Agent is the only agent we could test with.
