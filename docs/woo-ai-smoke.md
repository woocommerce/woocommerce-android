# Woo AI Smoke

`woo-ai-smoke` runs the Android AI Assistant headless regression harness through a no-UI instrumentation test.

## Preconditions

- A Wasabi debug build is installed.
- The app is already logged into the smoke store and has a selected site.
- The command passes `clearPackageData=false` so existing app auth and `SelectedSite` remain available.
- Smoke config only reads `wooAiSmoke*` arguments. Normal runner args and e2e secret args are ignored by smoke config.
- The command does not accept `wooAiSmoke` token, password, credential, or secret arguments.
- Unsafe write confirmations are declined by the smoke harness.
- The smoke run does not launch an Activity or Compose screen.

## Check Mode

```bash
./gradlew :WooCommerce:connectedWasabiDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.woocommerce.android.aiassistant.headless.WooAiSmokeAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.clearPackageData=false \
  -Pandroid.testInstrumentationRunnerArguments.wooAiSmoke=true \
  -Pandroid.testInstrumentationRunnerArguments.wooAiSmokeBaselineMode=check \
  -Pandroid.testInstrumentationRunnerArguments.wooAiSmokeWriteMode=decline
mkdir -p WooCommerce/build/outputs/woo-ai-smoke/latest
adb exec-out run-as com.woocommerce.android.dev \
  tar -C files/woo-ai-smoke/latest -cf - . \
  | tar -C WooCommerce/build/outputs/woo-ai-smoke/latest -xf -
```

Artifacts are written under `Application.filesDir/woo-ai-smoke/latest`, which maps to
`adb run-as com.woocommerce.android.dev tar -C files/woo-ai-smoke/latest -cf - .`.

## Baseline Approval

Use the same command with:

```bash
-Pandroid.testInstrumentationRunnerArguments.wooAiSmokeBaselineMode=approve
```

Approval mode still declines unsafe writes. It only writes `approved-baseline.json` when every scenario passes hard
checks. Copy `WooCommerce/build/outputs/woo-ai-smoke/latest/approved-baseline.json` over
`libs/ai-assistant/feature/src/debug/resources/woo-ai-smoke/baseline.json` only after reviewer approval.
