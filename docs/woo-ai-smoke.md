# Woo AI Smoke

`woo-ai-smoke` runs the Android AI Assistant headless regression harness. The default path is a
Robolectric/JVM test in `:libs:ai-assistant:feature`; it does not require a connected Android device,
emulator, installed app, login state, selected site, live network, or live Jetpack AI credentials.

## Default No-Device Command

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeRobolectricTest"
```

Expected result: `BUILD SUCCESSFUL` with all five smoke scenarios passing. This command exercises the
real assistant loop, smoke scenario resources, hard-check evaluator, approved-baseline comparison, and
artifact writer using a fake static site id and deterministic no-device chat/tool fixtures.

Review artifacts are written to:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/latest
```

Expected check-mode artifacts:

```text
baseline-comparison.json
run.json
summary.md
turns.jsonl
```

## No-Device Baseline Approval

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeRobolectricApprovalTest"
```

Approval mode uses the same no-device harness and stable output directory. It writes
`approved-baseline.json` only when every scenario passes hard checks.

After reviewer approval, update the checked-in baseline with:

```bash
cp \
  libs/ai-assistant/feature/build/outputs/woo-ai-smoke/latest/approved-baseline.json \
  libs/ai-assistant/feature/src/debug/resources/woo-ai-smoke/baseline.json
```

## Full No-Device Smoke Support Suite

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmoke*"
```

Expected result: `BUILD SUCCESSFUL`. This includes the primary Robolectric check and approval tests
plus parser, config, mapper, baseline approval, run writer, and summary renderer tests.

## Optional Device-Backed Live Adapter

Use this only when you explicitly want to reuse an installed authenticated Wasabi debug app and the
app's selected-site state. This path can exercise real `JetpackAiChatService` and live tool wiring,
but it is not the default harness and it requires a connected device or emulator.

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

The optional adapter writes artifacts under `Application.filesDir/woo-ai-smoke/latest`, which maps to
`adb run-as com.woocommerce.android.dev tar -C files/woo-ai-smoke/latest -cf - .`.
