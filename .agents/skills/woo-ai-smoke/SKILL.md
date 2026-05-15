---
name: woo-ai-smoke
description: Run the Android AI Assistant headless smoke regression harness without launching UI.
---

# Woo AI Smoke

## Default Command

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeRobolectricTest"
```

This is the default smoke harness. It does not require a connected device, emulator, installed app, login state,
selected site, live network, or live Jetpack AI credentials.

Review artifacts are written to:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/latest
```

## No-Device Baseline Approval

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeRobolectricApprovalTest"
```

After reviewer approval, update the checked-in baseline with:

```bash
cp \
  libs/ai-assistant/feature/build/outputs/woo-ai-smoke/latest/approved-baseline.json \
  libs/ai-assistant/feature/src/debug/resources/woo-ai-smoke/baseline.json
```

## Optional Device-Backed Live Adapter

Use this only when explicit live/device verification is needed.

### Preconditions

- A Wasabi debug build is installed.
- The app is already logged into the smoke store and has a selected site.
- The command passes `clearPackageData=false` so existing app auth and `SelectedSite` remain available.
- Smoke config only reads `wooAiSmoke*` arguments. Normal runner args and e2e secret args are ignored by smoke config.
- The command does not accept `wooAiSmoke` token, password, credential, or secret arguments.
- Unsafe write confirmations are declined by the smoke harness.
- The smoke run does not launch an Activity or Compose screen.

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
