# Woo AI Smoke Architecture

This document explains the Android AI Assistant headless smoke harness after the primary no-device path moved into
`:libs:ai-assistant:feature`.

The short version:

- The primary harness is a Robolectric unit test in `:libs:ai-assistant:feature`.
- It does not launch UI and does not require a device.
- It uses explicit smoke-store credentials from `~/.woo-ai-smoke/store.env`.
- It runs the real `JetpackAiChatService` and real `WooCommerceToolRegistry`.
- It writes review artifacts under `build/outputs`; tests do not edit source files.
- The checked-in baseline changes only when a developer intentionally copies an approved generated baseline into
  `src/debug/resources`.

## Module Ownership

```mermaid
flowchart TD
    WooCommerce[":WooCommerce"] --> Feature[":libs:ai-assistant:feature"]
    Feature --> Core[":libs:ai-assistant:core"]

    Feature --> FeatureDebug["feature/src/debug\nrunner, scenarios, live JWT provider, artifacts"]
    Feature --> FeatureTestDebug["feature/src/testDebug\nRobolectric live tests and test Hilt graph"]
    Core --> CoreFixtures["core testFixtures\nheadless contracts, baseline comparator, hard checks"]
    WooCommerce --> OptionalAdapter["WooCommerce/src/androidTest\noptional installed-app adapter"]
```

`:WooCommerce` is not the primary no-device harness anymore. It keeps only optional installed-app/device coverage through
`WooAiSmokeAndroidTest`.

## Test Entrypoints

```mermaid
flowchart LR
    Check["WooAiSmokeLiveRobolectricTest\ncheck mode"] --> RunLive["WooAiSmokeDebugBridge.runLive"]
    Approve["WooAiSmokeLiveRobolectricApprovalTest\napprove mode"] --> RunLive
    Device["WooAiSmokeAndroidTest\noptional device adapter"] --> RunDevice["WooAiSmokeDebugBridge.run"]
```

Primary commands:

```bash
WOO_AI_SMOKE_RUN_LIVE=true WOO_AI_SMOKE_MODE=check \
  ./gradlew :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricTest"

WOO_AI_SMOKE_RUN_LIVE=true WOO_AI_SMOKE_MODE=approve \
  ./gradlew :libs:ai-assistant:feature:testDebugUnitTest \
    --tests "*.WooAiSmokeLiveRobolectricApprovalTest"
```

The optional device-backed adapter remains useful when someone wants to validate an installed authenticated debug app and
its selected-site state, but it is not the primary evidence path.

## Live Robolectric Flow

```mermaid
sequenceDiagram
    participant Test as Live Robolectric test
    participant Env as WooAiSmokeLiveEnvRule
    participant Hilt as Feature test Hilt graph
    participant Bridge as WooAiSmokeDebugBridge.runLive
    participant JWT as WooAiSmokeDirectJwtTokenProvider
    participant Bootstrap as WooAiSmokeCredentialBootstrap
    participant Tools as WooCommerceToolRegistry
    participant Runner as WooAiSmokeRunner
    participant Writer as WooAiSmokeRunWriter

    Test->>Env: parse WOO_* credentials
    Env-->>Test: skip when live opt-in is absent
    Test->>Hilt: inject feature test graph
    Test->>Bridge: runLive(application, credentials)
    Bridge->>JWT: mint smoke-only Jetpack AI JWT
    Bridge->>Bootstrap: persist SiteModel, app password, SelectedSite
    Bootstrap->>Tools: execute safe read-only preflight tools
    Tools-->>Bootstrap: ToolResult.Success for each preflight tool
    Bootstrap-->>Bridge: WooAiSmokePreflightReport
    Bridge->>Writer: write preflight.json under build/outputs
    Bridge->>Runner: run live scenario suite
    Runner->>Tools: model-requested tool calls
    Runner->>Writer: write run.json, turns.jsonl, summary.md, comparison
```

The env rule runs before Hilt injection. Without `WOO_AI_SMOKE_RUN_LIVE=true`, the live tests skip by JUnit assumption and
do not build the live graph.

## Feature Test Hilt Graph

The feature module cannot import app modules. The live Robolectric graph therefore has feature-owned test modules:

- `WooAiSmokeFeatureRobolectricModule`
  - includes the FluxC/Woo database and network modules needed by the tool stack;
  - includes `WooAiSmokeRobolectricNetworkModule`, which replaces Volley delivery with a direct executor for Robolectric.
- `WooAiSmokeFeatureAppBindingsModule`
  - provides app-level bindings that the feature graph needs in tests:
    - unqualified `Context`;
    - `CoroutineDispatcher`;
    - `UserAgent`;
    - blank `AppSecrets`;
    - smoke-only `ApplicationPasswordsConfiguration`;
    - no-op `AssistantTelemetryTracker`.

This keeps dependency direction intact:

```text
:WooCommerce -> :libs:ai-assistant:feature -> :libs:ai-assistant:core
```

There is no dependency from `:libs:ai-assistant:feature` back to `:WooCommerce`.

## Bootstrap And Preflight

Bootstrap prepares Android data-layer state before the model scenarios run:

1. persist a `SiteModel` for the smoke site;
2. store application-password credentials for that site;
3. set `SelectedSite`;
4. verify the real `WooCommerceToolRegistry`;
5. execute safe read-only preflight tools:
   - `products_list`;
   - `orders_list`;
   - `orders_list` with pending-order arguments, reported as `orders_list_pending`;
   - `analytics_orders`.

The important behavioral check is inside `executePreflight`: every preflight tool must return `ToolResult.Success`. If one
does not, bootstrap fails and the live scenarios do not run.

`safeToolResults` is part of `WooAiSmokePreflightReport`. It records the preflight tool names and result kinds in
`preflight.json` so the generated artifact shows which real read-only tools were executed before the scenario run.

```json
{
  "safeToolResults": [
    { "toolName": "products_list", "resultKind": "SUCCESS" },
    { "toolName": "orders_list", "resultKind": "SUCCESS" },
    { "toolName": "orders_list_pending", "resultKind": "SUCCESS" },
    { "toolName": "analytics_orders", "resultKind": "SUCCESS" }
  ]
}
```

This field is report data. The enforcement is the preflight execution itself; the field is not used to decide later control
flow.

## Artifacts And Baseline

```mermaid
flowchart TD
    Run["Live smoke run"] --> BuildArtifacts["build/outputs/woo-ai-smoke/live/latest\nand live/runs/<timestamp-id>"]
    BuildArtifacts --> Preflight["preflight.json\nbootstrap/report metadata"]
    BuildArtifacts --> RunJson["run.json\nscenario results"]
    BuildArtifacts --> Turns["turns.jsonl\nredacted turn/tool trace"]
    BuildArtifacts --> Summary["summary.md\nhuman-readable result"]
    BuildArtifacts --> Approved["approved-live-baseline.json\napproval mode only"]

    SourceBaseline["src/debug/resources/woo-ai-smoke/live-baseline.json\nchecked in"] --> CheckMode["check mode comparison"]
    BuildArtifacts --> CheckMode
    Approved -. manual copy after review .-> SourceBaseline
```

Generated artifacts live under:

```text
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/latest
libs/ai-assistant/feature/build/outputs/woo-ai-smoke/live/runs/<yyyyMMdd-HHmmss>-<shortRunId>
```

These generated files are not source code and are not committed.

The checked-in live baseline is:

```text
libs/ai-assistant/feature/src/debug/resources/woo-ai-smoke/live-baseline.json
```

Approval mode writes `approved-live-baseline.json` into `build/outputs`. A developer updates the checked-in baseline only
by manually copying that generated file into `src/debug/resources` after review.

## Check Mode Versus Approval Mode

```mermaid
flowchart LR
    Scenarios["live-scenarios.json"] --> Runner
    Runner["WooAiSmokeRunner"] --> Results["current live results"]
    Baseline["live-baseline.json"] --> Comparator["baseline comparator"]
    Results --> Comparator
    Comparator --> Check["check mode\nfails on missing/stale baseline or regression"]
    Results --> Approval["approval mode\nwrites approved-live-baseline.json"]
```

Check mode is the normal regression gate. Approval mode is only for intentionally refreshing the expected live baseline.

## What The Harness Proves

The primary live path proves the Android assistant can run without UI or device while using:

- explicit live smoke credentials;
- the debug-only direct Jetpack AI JWT provider;
- real `JetpackAiChatService`;
- real `WooCommerceToolRegistry`;
- real FluxC/Woo stores and application-password state;
- canonical merchant scenarios and hard checks;
- redacted run artifacts for review.

It does not prove full app launch behavior. That remains the role of the optional `:WooCommerce` device-backed adapter.
