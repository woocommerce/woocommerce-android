# libs/ai-assistant

Assistant module family for WooCommerce Android. Use this file for coding-agent rules; use [README.md](README.md)
for what the modules are.

## Quick Commands

These task paths are real Gradle tasks under `:libs:ai-assistant:core` or `:libs:ai-assistant:feature`. Feature tasks
need Android SDK/local.properties; core tests are plain JVM.

- Core JVM tests: `./gradlew :libs:ai-assistant:core:test`
- Core focused test: `./gradlew :libs:ai-assistant:core:test --tests "*.AgenticLoopImplTest"`
- Feature unit/Robolectric tests: `./gradlew :libs:ai-assistant:feature:testDebugUnitTest`
- Feature focused test: `./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.AssistantViewModelTest"`
- No-network smoke support: `./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooAiSmokeDeterministicSupportTest"`
- Chat/SSE smoke slice: `./gradlew :libs:ai-assistant:feature:testDebugUnitTest --tests "*.WooMobileAiChatServiceHeadlessHarnessTest"`

Live smoke and approval flows are heavier; use [docs/woo-ai-smoke.md](docs/woo-ai-smoke.md) instead of memory.

## Orientation

Production dependency direction is `:WooCommerce -> :libs:ai-assistant:feature -> :libs:ai-assistant:core`.
`:WooCommerce` is the host integration surface; new assistant behavior belongs in this module family unless the host
app truly owns it.

`:core` is a pure Kotlin/JVM contract and loop layer: chat events, tool contracts, safety decisions,
history budgeting, retry policy, and the agentic loop. Its `testFixtures` contain the shared
headless harness used by both core and feature tests.

`:feature` is the Android library: Hilt wiring, Compose UI, `AssistantViewModel`, runtime adapter,
WPCOM wrapper chat service, FluxC-backed tools, confirmation previews, cards, telemetry, and smoke
test wiring.

For current files, prefer discovery over inventories: `rg --files libs/ai-assistant` and
`rg -n "ToolDescriptor|AssistantToolHandler|AssistantConfig|WooAiSmoke" libs/ai-assistant`.

## Working Rules

- Keep assistant contracts that do not need Android in `:core`; keep Android, Hilt, OkHttp, FluxC, Compose,
  resources, and EventHorizon usage in `:feature`.
- Do not move feature logic into `:WooCommerce`; the README says the app owns only entry point,
  navigation, DI bootstrap, feature flags, and runtime environment.
- `AssistantViewModel` is a plain AndroidX `ViewModel` with `StateFlow`; it is not a `ScopedViewModel` and does
  not use main-app `triggerEvent()` patterns.
- `AssistantRoute` obtains the Hilt ViewModel; lower composables should receive state/callbacks.
- Use injected `@AiAssistantJson` for assistant serialization unless a test-local JSON is building isolated fixtures.
- Tool handlers implement `AssistantToolHandler`: descriptor name/schema/safety level plus
  `execute(ToolCall): ToolResult`.
- Tool handlers should validate unknown/unsupported args, return `ToolResult.ValidationError` for
  caller/model mistakes, return diagnostic `TransportError` for store/network failures, and rethrow
  coroutine cancellation.
- Write-capable tools must stay `ToolSafetyLevel.UNSAFE` and have focused tests for descriptor
  safety, validation, success payloads, and deterministic vs unknown failure diagnostics.
- When adding or renaming a tool, update Dagger multibinding, catalog scope selection, tool activity
  labeling if UI-visible, prompt/scenario expectations if behavior changes, and focused tests.
- Keep system prompt construction in `AssistantSystemPromptProvider`; runtime replaces stale system
  prompts before calling core.
- Keep WPCOM wrapper chat traffic behind `WooMobileAiChatService`/OpenAI-compatible SSE plumbing so auth, endpoint
  config, parser behavior, and error mapping stay independently testable.
- Smoke artifacts are redacted before disk. Do not bypass `WooAiSmokeRedactor` or add new artifact
  fields containing site URLs, usernames, passwords, tokens, or raw merchant data.

## Testing

- Core loop/contracts/headless changes: run `:libs:ai-assistant:core:test`; add focused tests near
  `AgenticLoopImplTest`, `ToolCallAssemblerTest`, `SafetyOrchestratorImplTest`, or `core/headless/*Test`.
- Feature runtime/UI/tool changes: run `:libs:ai-assistant:feature:testDebugUnitTest` or a focused `--tests` filter
  first, then broaden before handoff when shared behavior changed.
- Chat transport changes: cover `ChatStreamParserTest`, `WooMobileAiChatServiceTest`, and the
  `WooMobileAiChatServiceHeadlessHarnessTest` slice.
- Tool/data-source changes: cover schema, argument validation, FluxC success mapping, partial fetch
  behavior, failure diagnostics, and card/confirmation output when applicable.
- Prompt, model, tool catalog, live scenarios, or real tool behavior changes: run relevant unit tests plus the
  deterministic smoke support test; use Level 3 live smoke when the live model or real store can observe the change.
- Baseline approval is not a unit-test update. Inspect `approved-live-baseline.json` under
  `build/outputs` before manually copying it to the checked-in `live-baseline.json`.

## Boundaries

Always:
- Preserve dependency direction and rerun boundary tests when imports, dependencies, telemetry, or module placement
  change.
- Add tests with behavior changes; this module already has focused coverage for loop contracts, runtime mapping,
  prompts, tool schemas, cards, safety previews, telemetry, and smoke wiring.
- Use `libs/ai-assistant/core/src/testFixtures` harnesses instead of inventing a second headless
  loop runner.

Ask First:
- Changing `AssistantConfig` model id, prompt version, tool-catalog version, completion stack, or
  feature name.
- Editing the system prompt, tool names, tool schemas, safety levels, live scenarios, or
  `live-baseline.json`.
- Changing WPCOM wrapper endpoint/base URL/auth behavior, smoke credential flow, redaction policy,
  or generated EventHorizon assistant event contracts.
- Adding dependencies to `:core`, moving code across `:core`/`:feature`/`:WooCommerce`, or creating another
  ai-assistant module.

Never:
- Commit `~/.woo-ai-smoke/store.env`, `wc.oauth.*` values, tokens, expanded smoke env output, or generated smoke
  artifacts under `feature/build/outputs/woo-ai-smoke`.
- Add Android, Compose, Hilt, OkHttp, FluxC, EventHorizon, or `com.woocommerce.android.analytics` imports/deps to
  `:core`.
- Import `com.woocommerce.android.analytics` in `:feature`; local telemetry uses assistant
  telemetry classes and generated EventHorizon events.

## Further Reading

- [README.md](README.md) - module split and ownership.
- [docs/woo-ai-smoke.md](docs/woo-ai-smoke.md) - smoke levels, credentials, live/approval commands.
- [docs/woo-ai-smoke-architecture.md](docs/woo-ai-smoke-architecture.md) - harness architecture.
- `core/src/test/kotlin/.../architecture/CoreModuleBoundaryTest.kt` - enforced core import boundary.
- `feature/src/test/kotlin/.../architecture/FeatureModuleBoundaryTest.kt` - enforced feature telemetry boundary.
- `core/build.gradle` and `feature/build.gradle` - source sets, dependencies, and testFixtures.
