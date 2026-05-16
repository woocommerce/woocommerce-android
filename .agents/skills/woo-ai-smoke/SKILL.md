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

After the run, always read `run.json` and `baseline-comparison.json` from that directory and include a
scenario recap in the final response. The recap must show every scenario, the run result, and the
comparison against the checked-in baseline. Do not paste raw `turns.jsonl`, credentials, JWTs, Basic
auth headers, cookies, or expanded environment values.

Use this helper when the artifact files exist:

```bash
RUN_DIR="libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest"
jq -r --slurpfile comparison "$RUN_DIR/baseline-comparison.json" '
  def tool_summary($scenario):
    [
      $scenario.result.turns[]
      | .toolCalls[]
      | "\(.name)(\(.resultKind))"
    ] | if length == 0 then "none" else join(", ") end;
  def outcomes($scenario):
    [$scenario.result.turns[].outcome] | unique | join("/");
  ($comparison[0].scenarioStatuses
    | map({ key: .scenarioId, value: { status: .status, message: .message } })
    | from_entries) as $baseline
  | "| Scenario | Category | Result | Baseline | Outcome | Tools |",
    "| --- | --- | --- | --- | --- | --- |",
    (.scenarios[] |
      ($baseline[.scenarioId] // { status: "MISSING", message: "No baseline comparison." }) as $b
      | "| \(.scenarioId) | \(.category) | \(.status) | \($b.status): \($b.message) | \(outcomes(.)) | \(tool_summary(.)) |"
    )
' "$RUN_DIR/run.json"
```

If the Gradle command fails before artifacts are written, say that no scenario recap is available and
include the failure reason instead.

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

After an approval run, print the same scenario recap table from
`libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest`. Also state whether
`approved-live-baseline.json` was produced.

## Support/Unit Coverage

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeDeterministicSupport*"
```

Deterministic support tests validate harness wiring only. They are not accepted primary smoke
evidence and must not be used to approve the live baseline.
