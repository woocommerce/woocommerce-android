---
name: woo-ai-smoke
description: Run the Android AI Assistant headless smoke regression harness without launching UI.
---

# Woo AI Smoke

## Default Live Command

The live suite mirrors the iOS `/woo-ai-smoke` scenario list from
`woocommerce/woocommerce-ios#17016`: 25 scripted scenarios. The Android Robolectric test produces
the trace artifacts and enforces the deterministic baseline gate.

If `~/.woo-ai-smoke/store.env` does not exist, create it with these keys and stop so the developer
can fill it in outside the repo:

```text
WOO_SITE_URL=
WOO_WPCOM_USERNAME=
WOO_WPCOM_PASSWORD=
```

The target store must be Jetpack-connected and connected to the same WordPress.com account used by
`WOO_WPCOM_USERNAME`.

`WOO_WPCOM_PASSWORD` may be a WordPress.com Application Password when the account requires 2FA. The
smoke harness does not implement an interactive 2FA challenge.

Live chat routes through the WPCOM wrapper endpoint
`/wpcom/v2/woo-mobile-ai/chat/completions` with a WordPress.com OAuth bearer. Store tools still
target `WOO_SITE_URL` through the WPCOM REST / Jetpack-connected path.

Never print the file contents, expanded env, WPCOM username, WPCOM password/Application Passwords,
WPCOM bearer tokens, cookies, or raw credential config.

```bash
while IFS='=' read -r key value; do
  case "$key" in
    WOO_SITE_URL|WOO_WPCOM_USERNAME|WOO_WPCOM_PASSWORD) export "$key=$value" ;;
  esac
done < "$HOME/.woo-ai-smoke/store.env"
./gradlew -PwooAiSmokeRunLive=true :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricTest"
```

Optional focused/debug controls:

```bash
WOO_AI_SMOKE_SCENARIO_ID=orders_with_email WOO_AI_SMOKE_SAMPLES=3 \
  ./gradlew -PwooAiSmokeRunLive=true :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricTest"
```

`WOO_AI_SMOKE_SCENARIO_ID` supports a comma-separated list for the check test entrypoint.
Approval uses the separate `WooAiSmokeLiveRobolectricApprovalTest` entrypoint, must run the full
suite, and rejects scenario filters. `WOO_AI_SMOKE_SAMPLES` supports `1..3`. In check runs,
primary scenario status and JUnit failure use sample 1. Baseline comparison also uses sample 1
unless the checked-in baseline contains an approved `sampleExpectation` or `knownFailure`.
`sampleExpectation` checks compare the sampled classification and requested sample count; approved
`knownFailure` checks compare every failing sample's failed hard-check set against
`knownFailure.expectedFailedHardChecks`. Approved `FLAKY` is a sampled-run tolerance for acceptable
scenario-specific variability: sampled `FLAKY` remains non-blocking only while global guards still
pass, sampled `PASS` asks for a baseline refresh, and single-sample `FAIL` is blocking because one
sample cannot prove flakiness.

Artifacts are written to:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest
```

After the run, always read `run.json`, `turns.jsonl`, and `baseline-comparison.json` from that
directory and include a scenario recap plus an iOS-style `Rubric` table in the final response. The
recap must show every scenario, the run result, sampled classification when present, and the
comparison against the checked-in baseline. Do not paste raw `turns.jsonl`, credentials, WPCOM
bearer tokens, cookies, or expanded environment values.

`KNOWN_FAILURE` in the baseline column is an accepted, explicitly documented live failure; include
it in the recap instead of converting it to PASS. `KNOWN_FAILURE_FIXED` is non-blocking but means the
baseline exception should be removed after review. Any `REGRESSION`, `NEW`, or `MISSING` status
still needs triage.

Every scenario also has global guards for no `FAILED` outcome, no turn errors, and non-blank
assistant text. Empty/error outputs should never be treated as passing just because negative checks
passed.

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
  def sampled($scenario):
    if $scenario.sampleSummary == null then "n/a"
    else "\($scenario.sampleSummary.classification) (PASS=\($scenario.sampleSummary.passCount) FAIL=\($scenario.sampleSummary.failCount))"
    end;
  ($comparison[0].scenarioStatuses
    | map({ key: .scenarioId, value: { status: .status, message: .message } })
    | from_entries) as $baseline
  | "| Scenario | Category | Result | Sampled | Baseline | Outcome | Tools |",
    "| --- | --- | --- | --- | --- | --- | --- |",
    (.scenarios[] |
      ($baseline[.scenarioId] // { status: "MISSING", message: "No baseline comparison." }) as $b
      | "| \(.scenarioId) | \(.category) | \(.status) | \(sampled(.)) | \($b.status): \($b.message) | \(outcomes(.)) | \(tool_summary(.)) |"
    )
' "$RUN_DIR/run.json"
```

If the Gradle command fails before artifacts are written, say that no scenario recap is available and
include the failure reason instead.

## Live Baseline Approval

```bash
while IFS='=' read -r key value; do
  case "$key" in
    WOO_SITE_URL|WOO_WPCOM_USERNAME|WOO_WPCOM_PASSWORD) export "$key=$value" ;;
  esac
done < "$HOME/.woo-ai-smoke/store.env"
WOO_AI_SMOKE_SAMPLES=3 \
  ./gradlew -PwooAiSmokeRunLive=true :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricApprovalTest"
```

Approval mode accepts `WOO_AI_SMOKE_SAMPLES=1..3` and still rejects scenario filters. A sampled
approval writes a `sampleExpectation` for all-pass and mixed pass/fail scenarios: all-pass samples
approve `PASS`, mixed pass/fail samples approve `FLAKY`, and all-fail samples are rejected unless
an existing `knownFailure` is being preserved because every failing sample has the same expected
failed hard-check set. Preserved known-failure approvals do not write `sampleExpectation`. Approved
`FLAKY` is separate from `knownFailure`; it does not accept failed outcomes, turn errors, or blank
assistant responses.

If live auth fails with a 2FA-required message, tell the operator to use a WordPress.com
Application Password as `WOO_WPCOM_PASSWORD`. If site resolution fails, verify the target store is
connected to the same WordPress.com account and is Jetpack-connected.

After reviewer inspection:

```bash
cp \
  libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest/approved-live-baseline.json \
  libs/ai-assistant/feature/src/testDebug/resources/woo-ai-smoke/live-baseline.json
```

After an approval run, print the same scenario recap table from
`libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest`. Also state whether
`approved-live-baseline.json` was produced. Approval can preserve an existing `knownFailure` entry
only when every failing sample still has the same expected failed hard-check set, but new failures
must not be added by hand without a reason and expected failed hard checks. If a scenario is
intentionally flaky, approve it with sampled approval so the checked-in baseline records the `FLAKY`
sample expectation instead of hiding it as a known failure.

## Support/Unit Coverage

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeDeterministicSupportTest"
```

Deterministic support tests validate harness wiring only. They are not accepted primary smoke
evidence and must not be used to approve the live baseline. They do not use a deterministic
baseline; fake-chat/fake-tool failures fail directly.

## Rubric

Do not make Gradle, CI, or the Kotlin baseline comparison depend on a model judge. After artifacts
exist, the final response must include a separate `Rubric` section based only on redacted artifacts:
`run.json`, `turns.jsonl`, and `baseline-comparison.json`.

The report must clearly separate:

- `Deterministic gate`: scenario status, failed hard checks, and baseline comparison. This is the
  merge-blocking result.
- `Rubric`: iOS-style 0/1/2 scoring from traces and scenario intent. These scores are reviewer
  guidance and are not the Kotlin/JUnit gate.

Score each scenario, or each turn when a scenario has materially different turn outcomes, using:

- `2`: correct / well-grounded / appropriate / recovered or no recovery needed.
- `1`: partially correct or minor issue that reviewers should inspect.
- `0`: incorrect, unsupported, wrong tool/safety behavior, or failed recovery.

Use these dimensions:

- `Correct`: answers the merchant's request and follows scenario-specific requirements.
- `Grounded`: user-facing claims are supported by tool traces or explicit tool failures.
- `Tools`: tool choices, safety behavior, and write-confirmation handling fit the scenario.
- `Recovery`: handles errors, declines, empty results, and clarification needs appropriately.

The `Rubric` table must include deterministic status in the same row so reviewers get one cohesive
report:

```markdown
| Scenario | Gate | Sampled | Baseline | Correct | Grounded | Tools | Recovery | Notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| orders_with_email | PASS | n/a | PASS | 2 | 2 | 2 | 2 | Email appears only when supported by the orders tool result. |
```

`Gate` is the primary scenario status from `run.json`. `Sampled` is `PASS`, `FAIL`, `FLAKY`, or
`n/a` from `sampleSummary` when present. `Baseline` is the scenario status from
`baseline-comparison.json`. `Notes` should be short and should call out failed hard checks,
baseline regressions, sampled flakiness, tool mismatch, unsupported claims, or recovery concerns.

For the `spanish` scenario, Android keeps the same hard-check floor as iOS: turn 1 contains
`pedido|pedidos`, and turn 2 contains `ayer`. The rubric must flag user-facing English or
mixed-language replies as a `Correct` issue because the scenario expects Spanish throughout. Do not
add deterministic negative English substring checks for this; full-language review belongs in the
rubric.
