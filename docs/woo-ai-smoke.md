# Woo AI Smoke

`woo-ai-smoke` runs the Android AI Assistant headless regression harness without launching UI.
The accepted default path is a live no-device Robolectric test in `:libs:ai-assistant:feature`: it uses
iOS-style smoke credentials from `~/.woo-ai-smoke/store.env`, mints a smoke-only Jetpack AI JWT,
bootstraps Android `SelectedSite` plus application-password state through feature-owned test Hilt modules,
and runs the real `JetpackAiChatService` with the real `WooCommerceToolRegistry`.

## Credential File

Create `~/.woo-ai-smoke/store.env` outside the repo:

```text
WOO_SITE_URL=
WOO_SITE_ID=
WOO_USERNAME=
WOO_APP_PASSWORD=
```

Do not echo this file, paste its values into chat, commit it, or include expanded environment output
in logs or PR text. CI should provide the same keys as masked environment variables.

## Default Live No-Device Command

```bash
while IFS='=' read -r key value; do
  case "$key" in
    WOO_SITE_URL|WOO_SITE_ID|WOO_USERNAME|WOO_APP_PASSWORD) export "$key=$value" ;;
  esac
done < "$HOME/.woo-ai-smoke/store.env"
WOO_AI_SMOKE_RUN_LIVE=true WOO_AI_SMOKE_MODE=check \
  ./gradlew :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricTest"
```

Review artifacts are written to:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/runs/<yyyyMMdd-HHmmss>-<shortRunId>
```

Expected artifacts:

```text
baseline-comparison.json
preflight.json
run.json
summary.md
turns.jsonl
```

Check mode fails with `Live baseline approval required` when `live-baseline.json` is absent or stale.

## Live Baseline Approval

Run approval only when intentionally refreshing the accepted live baseline:

```bash
while IFS='=' read -r key value; do
  case "$key" in
    WOO_SITE_URL|WOO_SITE_ID|WOO_USERNAME|WOO_APP_PASSWORD) export "$key=$value" ;;
  esac
done < "$HOME/.woo-ai-smoke/store.env"
WOO_AI_SMOKE_RUN_LIVE=true WOO_AI_SMOKE_MODE=approve \
  ./gradlew :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricApprovalTest"
```

After reviewer inspection, update the checked-in live baseline manually:

```bash
cp \
  libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest/approved-live-baseline.json \
  libs/ai-assistant/feature/src/debug/resources/woo-ai-smoke/live-baseline.json
```

The accepted live baseline must come from `JetpackAiChatService`,
`WooAiSmokeDirectJwtTokenProvider`, and `WooCommerceToolRegistry`.

## Support/Unit Coverage

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeDeterministicSupport*"
```

These deterministic tests validate harness wiring, scenario parsing, hard checks, baseline
comparison, and artifact writing with fake no-device chat/tool fixtures. They are not accepted
primary smoke evidence and must not be used to approve `live-baseline.json`.

## Optional Device-Backed Live Adapter

Use this only when you explicitly want to reuse an installed authenticated Wasabi debug app and the
app's selected-site state. It requires a connected device or emulator and is not the primary success
path.

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
