# Woo AI Smoke

`woo-ai-smoke` is the Android AI Assistant headless regression harness. It runs the assistant's
real agentic loop end to end without launching the app or a device. The same loop harness,
`WooAssistantHeadless`, is reused across several test levels — each one swaps in a different chat
service, tool registry, and runtime.

This page tells you which level to run, when to run it, what it proves (and what it does not), and
where to look when it fails. For the why behind the layout, see
[Woo AI Smoke Architecture](woo-ai-smoke-architecture.md).

## Which test should I run?

| You changed... | Run this first | Accepted as merge-blocking live regression evidence? |
| --- | --- | --- |
| Anything in `:libs:ai-assistant:core` headless types (harness, parser, comparator, hard-check evaluator) | [Level 1 — core unit tests](#level-1--core-unit-tests) | No |
| Smoke harness wiring: scenario mapper, redactor, summary renderer, run writer, Hilt test graph | [Level 2 — feature support tests](#level-2--feature-support-tests-no-network) | No |
| `WooMobileAiChatService`, SSE plumbing, WPCOM bearer auth, stream parser | [Level 2 — chat-service slice](#chat-service-slice) | No |
| Anything the live model or the real Woo store sees: tool implementations, tool catalog, system prompt, model id, scenarios | [Level 3 — live no-device smoke](#level-3--live-no-device-smoke-check-mode) | **Yes** |
| Intentionally accepting a new live baseline | [Level 4 — live approval](#level-4--live-baseline-approval) | Yes, after manual review |

Rule of thumb: a green Level 2 run is necessary but not sufficient. Only a green Level 3 run
against the checked-in `live-baseline.json` is accepted live regression evidence.

## Shared mental model: `WooAssistantHeadless`

`WooAssistantHeadless` (in `:libs:ai-assistant:core` `testFixtures`) is the loop harness. Given a
scenario, an initial history, and a `SessionContext`, it runs the real `AgenticLoopImpl` and returns
a `HeadlessRunResult` — assistant text, tool-call traces, confirmation requests/results, errors.

Every level that drives a scenario through the agentic loop uses the same harness. Each level
differs only in *what gets plugged into it*:

| Level | ChatService | ToolRegistry | Runtime | Uses `WooAssistantHeadless`? |
| --- | --- | --- | --- | --- |
| 1. `WooAssistantHeadlessTest` | scripted `ScriptedHeadlessChatService` | recording fake | plain JVM | Direct |
| 1. `Headless{Baseline,HardCheck}*Test` | n/a | n/a | plain JVM | No — tests supporting contracts only |
| 2. `WooMobileAiChatServiceHeadlessHarnessTest` | real `WooMobileAiChatService` against `MockWebServer` | `NoOpToolRegistry` | plain JVM | Direct |
| 2. `WooAiSmokeDeterministicSupportTest` | fake `WooAiSmokeDeterministicSupportChatService` | fake `WooAiSmokeDeterministicSupportToolRegistry` | Robolectric | Indirect, via `WooAiSmokeRunner.run()` |
| 3. `WooAiSmokeLiveRobolectricTest` | real `WooMobileAiChatService` against the WPCOM wrapper | real `WooCommerceToolRegistry` | Robolectric | Indirect, via `WooAiSmokeRunner.run()` |
| 4. `WooAiSmokeLiveRobolectricApprovalTest` | same as Level 3 | same as Level 3 | Robolectric, approval mode | Indirect, via `WooAiSmokeRunner.run()` |

The parser/comparator/hard-check evaluator tests in `:core` do **not** instantiate the harness —
they pin the contracts that the harness produces (run result) and consumes (baseline JSON). They
keep the harness honest from the side.

## Level 1 — Core unit tests

Module: `:libs:ai-assistant:core`. Plain JVM, no Android, no Hilt, no Robolectric.

```bash
./gradlew :libs:ai-assistant:core:test
```

What it proves: the loop-harness contract is well-defined. Scripted assistant turns produce the
expected text/tool/safety traces. Baseline JSON parses and round-trips. The comparator catches
regressions, missing scenarios, and new scenarios. The hard-check evaluator matches traces
correctly.

What it does not prove: that any live chat, real tool, or scenario fixture still works. This level
never touches `WooMobileAiChatService` or `WooCommerceToolRegistry`.

Where to look on failure: each `Headless*Test` class under
`core/src/test/kotlin/.../core/headless/` names exactly what it pins. Read the assertion failure,
then re-read the contract type — `WooAssistantHeadless` itself in `core/src/testFixtures`, or the
parsers/comparators/evaluator in `core/src/testFixtures`.

## Level 2 — Feature support tests (no network)

Module: `:libs:ai-assistant:feature`. Robolectric runner, but no live network and no credentials.
Fakes for both the chat service and the tool registry.

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest \
  --tests "*.WooAiSmokeDeterministicSupportTest"
```

What it proves: the whole `WooAiSmokeRunner` glue works. Scenario JSON parses into the harness, the
harness runs the agentic loop end to end, hard checks fire, and the run writer produces the
deterministic artifacts (`run.json`, `turns.jsonl`, `summary.md`). The same `WooAiSmokeRunner` is
what Level 3 invokes, so a broken run writer, scenario mapper, redactor, or Hilt test wiring
surfaces here — fast, offline, and reproducible.

There is no deterministic baseline. Because fake chat and fake tools have no model or store drift,
Level 2 fails directly when a scenario or hard check breaks. Baselines are reserved for live runs.

What it does not prove: anything about the real model, the real chat service, the real tool
registry, or the real store. Both chat and tool registry are
`WooAiSmokeDeterministicSupport*` fakes. **This level is support evidence only and must not be
used to approve `live-baseline.json`.**

Artifacts land under `libs/ai-assistant/feature/build/outputs/woo-ai-smoke/latest` (stable, no per-run
directory).

How to read a failure:
- A failed scenario or hard check → the failure message names the failing scenario id. The specific
  failed hard checks are in `summary.md`; walk back from there to `deterministic-scenarios.json`
  (under `feature/src/test/resources/woo-ai-smoke`) and the scripted response in
  `WooAiSmokeDeterministicSupportFixtures.kt`.
- A missing artifact → the runner threw before the writer reached it; the JUnit failure message
  names the phase.

### Chat-service slice

```bash
./gradlew :libs:ai-assistant:feature:testDebugUnitTest \
  --tests "*.WooMobileAiChatServiceHeadlessHarnessTest"
```

This single test instantiates `WooAssistantHeadless` directly with the real `WooMobileAiChatService`
talking to a `MockWebServer`. It pins the SSE transport, the `Authorization: Bearer …` header, and
the stream parser without needing credentials or a live store. Use it when you touched
`:feature` chat plumbing and want fast feedback before paying the cost of a live run.

## Level 3 — Live no-device smoke (check mode)

Module: `:libs:ai-assistant:feature`, source set `src/testDebug`. Robolectric host;
real `WooMobileAiChatService` against real woo-mobile-ai WPCOM wrapper; real `WooCommerceToolRegistry` against the
smoke store named in your credentials file.

```bash
while IFS='=' read -r key value; do
  case "$key" in
    WOO_SITE_URL|WOO_WPCOM_USERNAME|WOO_WPCOM_PASSWORD) export "$key=$value" ;;
  esac
done < "$HOME/.woo-ai-smoke/store.env"
./gradlew -PwooAiSmokeRunLive=true :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricTest"
```

Without `-PwooAiSmokeRunLive=true`, `WooAiSmokeLiveEnvRule` skips the test by JUnit assumption —
the Hilt graph is never built and no live network calls happen. The opt-in is a Gradle property on
the current command, not a sticky shell environment variable. If the property is present but
credentials are missing or malformed, the test fails loudly rather than skipping.

What it proves: the Android AI Assistant runtime can, without a device or UI, authenticate to
WordPress.com through `AccountStore`, resolve the configured site's WPCOM site id from the
authenticated account's `/me/sites` response, bootstrap `SelectedSite` as a WPCOM REST
Jetpack-connected site under Robolectric, drive the real WPCOM wrapper chat service, exercise the
real Woo tool registry against the configured store, and match the canonical merchant scenarios in
`live-scenarios.json` (plus their hard checks) against the checked-in `live-baseline.json`. This is
the only level whose green status is accepted regression evidence for merge.

Focused and sampled controls:
- `WOO_AI_SMOKE_SCENARIO_ID=orders_with_email` runs a focused subset in the check test entrypoint. Multiple ids
  can be comma-separated. Filtered check runs compare only the selected scenarios against the
  checked-in baseline so they are useful for debugging; the approval test entrypoint rejects filters
  because a baseline refresh must cover the full suite.
- `WOO_AI_SMOKE_SAMPLES=3` repeats each selected scenario. Valid values are `1..3`. In check mode,
  primary scenario status and JUnit failure use sample 1. Baseline comparison also uses sample 1
  unless the checked-in baseline contains an approved `sampleExpectation` or `knownFailure`.
  `sampleExpectation` checks compare the sampled classification and requested sample count; approved
  `knownFailure` checks compare every failing sample's failed hard-check set against
  `knownFailure.expectedFailedHardChecks`. Approved `FLAKY` is a sampled-run tolerance for
  acceptable scenario-specific variability: sampled `FLAKY` remains non-blocking only while global
  guards still pass, sampled `PASS` asks for a baseline refresh, and single-sample `FAIL` is
  blocking because one sample cannot prove flakiness. Approval mode accepts sampled runs and can
  intentionally approve `PASS` or `FLAKY` sample classifications.
- Every scenario now gets global hard guards in addition to its JSON hard checks: no `FAILED`
  outcome, no turn errors, and non-blank assistant text. This prevents empty/error responses from
  passing scenarios that mostly contain negative checks.
- The operator skill adds an iOS-style rubric report after artifacts exist. Those scores read the
  already-redacted traces and stay separate from the deterministic baseline gate.

What it does not prove: anything UI-side. The chat fragment, screen state, scroll behavior, and
Compose rendering are not exercised. It also does not stand in for any device-backed coverage of
the host app surface — `:WooCommerce` no longer owns a device-backed smoke adapter; the live
headless path lives entirely in `:libs:ai-assistant:feature`.

Artifacts:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/runs/<yyyyMMdd-HHmmss>-<shortRunId>
```

| File | What to inspect |
| --- | --- |
| `preflight.json` | Did WordPress.com auth, WPCOM site-id resolution, `SelectedSite`, WPCOM REST / Jetpack-connected routing intent, and the read-only preflight tools bootstrap? `safeToolResults` lists every preflight call with its result kind. |
| `run.json` | Per-scenario status and hard-check outcomes. Sampled check runs include `sampleResults` and `sampleSummary` while keeping primary fields based on sample 1. |
| `baseline-comparison.json` | Per-scenario diff against `live-baseline.json`. `PASS`, `KNOWN_FAILURE`, and `KNOWN_FAILURE_FIXED` are non-blocking; `REGRESSION`, `NEW`, and `MISSING` need triage. |
| `turns.jsonl` | Redacted turn-by-turn trace of chat and tool calls. Sampled check runs include `sampleIndex`; one-sample runs keep the current shape. |
| `summary.md` | Human-readable summary of the run, including selected scenario filters and sampled classification. |

How to read common failures:

| Failure message | Most likely cause | First place to look |
| --- | --- | --- |
| `Live baseline approval required: missing woo-ai-smoke/live-baseline.json` | Checked-in baseline was deleted or renamed. | `feature/src/testDebug/resources/woo-ai-smoke/live-baseline.json`. Do not work around with approval mode; find out why the baseline went missing. |
| `Woo AI smoke baseline check failed: ...` | A blocking diff (`REGRESSION`, `NEW`, or `MISSING`). Hard-check failures surface here as `REGRESSION`. | `baseline-comparison.json` first, then `run.json` or `summary.md` for failed hard checks. |
| `PHASE_TIMEOUT: <phase>` (one of `wpcom_auth`, `wpcom_site_resolution`, `selected_site_and_tool_preflight`, `live_scenarios`) | The named phase exceeded its timeout (`wpcom_auth`: 1min, `wpcom_site_resolution`: 1min, `selected_site_and_tool_preflight`: 3min, `live_scenarios`: 5min multiplied by `WOO_AI_SMOKE_SAMPLES`). | `preflight.json` first — that tells you whether bootstrap finished. |
| `WPCOM_AUTH_REQUIRES_2FA: ...` | The WordPress.com account requires two-factor authentication for password auth. | Use a WordPress.com Application Password as `WOO_WPCOM_PASSWORD`; the smoke harness does not implement an interactive 2FA challenge. |
| `WPCOM_OAUTH_TOKEN_MISSING: ...` | AccountStore reported authentication success but no WPCOM access token was available. | Check `wc.oauth.app_id`, `wc.oauth.app_secret`, and the WPCOM credentials. |
| `WPCOM_SITE_NOT_FOUND: ...` | The configured `WOO_SITE_URL` was not found in the authenticated account's `/me/sites` response. | Confirm the site is connected to the same WordPress.com account used by the smoke run. |
| `WPCOM_SITE_INVALID_ORIGIN: ...` | The matched site is not a WPCOM REST site. | Use a WordPress.com-connected site exposed through the WPCOM REST path. |
| `WPCOM_SITE_NOT_JETPACK_CONNECTED: ...` | The matched site is not Jetpack installed and connected. | Connect Jetpack for the target store before running live smoke. |
| `PREFLIGHT_FAILED: <tool> returned <kind>` | A read-only preflight tool returned anything other than `Success`. Scenarios never ran. | The named tool, plus credentials/site setup. |

Reminder: `safeToolResults` in `preflight.json` is a **report, not enforcement**. The enforcement
is `require(result is ToolResult.Success)` inside bootstrap — any non-success throws before the
scenarios start. By the time `safeToolResults` is written to disk, every entry will be `SUCCESS`.
The field exists for traceability.

## Level 4 — Live baseline approval

Run approval only when you are intentionally refreshing the accepted live baseline.

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

This is the only path that produces a new live baseline candidate. It runs the same live plumbing
as Level 3 and writes `approved-live-baseline.json` under `build/outputs`. It does **not** touch
the checked-in baseline — tests never write into `src/`.

After the run, inspect `build/outputs/woo-ai-smoke/live/latest/approved-live-baseline.json`. If the
new shape is what you intend to accept, copy it manually:

```bash
cp \
  libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest/approved-live-baseline.json \
  libs/ai-assistant/feature/src/testDebug/resources/woo-ai-smoke/live-baseline.json
```

The accepted live baseline must come from `WooMobileAiChatService` using a WordPress.com OAuth
bearer, a WPCOM REST Jetpack-connected selected site, and `WooCommerceToolRegistry`. There is no
Level 2 approval baseline. If a scenario cannot reliably pass yet, keep the scenario in the baseline with a
`knownFailure` block containing a reason and the exact hard checks expected to fail.
Do not add undocumented failures.

Sampled approval writes `sampleExpectation` into `approved-live-baseline.json` for all-pass and
mixed pass/fail scenarios: all-pass samples record `PASS`, mixed pass/fail samples record `FLAKY`,
and all-fail samples are rejected unless an existing `knownFailure` is deliberately preserved
because every failing sample has the same expected failed hard-check set. Preserved known-failure
approvals do not write `sampleExpectation`. Flaky approval is separate from `knownFailure`; use it
when the live behavior is acceptable but not stable across repeated samples. It does not accept
failed outcomes, turn errors, or blank assistant responses.

Check and approval are wired to **two separate test classes** so a normal Level 3 run cannot
accidentally produce a candidate baseline.

## Skill-wrapper judge mode

The Android Robolectric live test remains the deterministic trace producer and baseline gate.
Gradle and CI do not call an LLM judge. After artifacts exist, the `woo-ai-smoke` operator skill
adds an iOS-style rubric report from the redacted `run.json`, `turns.jsonl`, and
`baseline-comparison.json`. Rubric scores are reviewer guidance and stay separate from deterministic
status.

For the `spanish` scenario, Android keeps the same deterministic marker floor as iOS: turn 1 must
contain `pedido|pedidos`, and turn 2 must contain `ayer`. Full "Spanish throughout" enforcement is
handled by rubric review: mixed English/Spanish user-facing replies should be flagged as a
correctness issue in the rubric notes, not encoded as brittle deterministic substring bans.

## Credentials

Live runs (Levels 3 and 4) require an env file outside the repo at `~/.woo-ai-smoke/store.env`:

```text
WOO_SITE_URL=
WOO_WPCOM_USERNAME=
WOO_WPCOM_PASSWORD=
```

`WOO_WPCOM_PASSWORD` is the password used for WordPress.com AccountStore authentication. If the
account requires 2FA, use a WordPress.com Application Password for this value; the smoke harness
does not implement an interactive 2FA challenge.

The target store must be Jetpack-connected and connected to the same WordPress.com account named by
`WOO_WPCOM_USERNAME`. Local builds also need valid `wc.oauth.app_id` and `wc.oauth.app_secret` in
the usual secrets file so AccountStore password auth can request a WPCOM OAuth token.

Chat traffic goes through `WooMobileAiChatService` to
`/wpcom/v2/woo-mobile-ai/chat/completions` with that WPCOM OAuth bearer. Store tools still target
the configured `WOO_SITE_URL`; the resolver-selected site is persisted as `ORIGIN_WPCOM_REST` with
Jetpack installed/connected state, and preflight records `toolTransportIntent =
WPCOM_REST_JETPACK_TUNNEL`.

Never echo this file, paste its values into chat, commit it, or include expanded environment
output in logs or PR text. CI provides the same keys as masked environment variables. The harness
redacts site URL, username, and WPCOM password from artifacts before they reach disk; do not bypass
that redaction.

## Skill recap

The `woo-ai-smoke` skill builds a scenario recap table from `run.json` and
`baseline-comparison.json` in `build/outputs/woo-ai-smoke/live/latest`. The recap is the fastest
way to see every scenario, its hard-check status, its baseline status, and the tools the loop
actually called. If the Gradle command fails before artifacts are written, the skill says so
instead of pretending to summarize.
