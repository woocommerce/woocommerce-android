---
name: woo-ai-smoke
description: Run the Android AI Assistant headless smoke regression harness without launching UI.
---

# Woo AI Smoke

## Default Live Command

If `~/.woo-ai-smoke/store.env` does not exist, create it with these keys and stop so the developer
can fill it in outside the repo:

```text
WOO_SITE_URL=
WOO_SITE_ID=
WOO_USERNAME=
WOO_APP_PASSWORD=
```

Never print the file contents, expanded env, username, app password, JWTs, Basic auth headers,
cookies, or raw credential config.

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

Artifacts are written to:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest
```

## Live Baseline Approval

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

After reviewer inspection:

```bash
cp \
  libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest/approved-live-baseline.json \
  libs/ai-assistant/feature/src/debug/resources/woo-ai-smoke/live-baseline.json
```

## Support/Unit Coverage

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeDeterministicSupport*"
```

Deterministic support tests validate harness wiring only. They are not accepted primary smoke
evidence and must not be used to approve the live baseline.

## Optional Device-Backed Live Adapter

Use only for explicit device-backed verification:

```bash
./gradlew :WooCommerce:connectedWasabiDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.woocommerce.android.aiassistant.headless.WooAiSmokeAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.clearPackageData=false \
  -Pandroid.testInstrumentationRunnerArguments.wooAiSmoke=true \
  -Pandroid.testInstrumentationRunnerArguments.wooAiSmokeBaselineMode=check \
  -Pandroid.testInstrumentationRunnerArguments.wooAiSmokeWriteMode=decline
```
