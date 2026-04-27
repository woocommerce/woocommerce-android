# `:libs:ai-assistant`

Assistant feature modules for WooCommerce Android. All assistant implementation lives here;
`:WooCommerce` owns only host integration (entry point, navigation, DI bootstrap).

## Module layout

```
:WooCommerce  ──depends on──►  :libs:ai-assistant:feature  ──depends on──►  :libs:ai-assistant:core
```

### `:libs:ai-assistant:feature`

Android library. The implementation home for the assistant:

- Chat UI and screen-level state (Compose)
- ViewModel and agentic loop orchestration
- `ChatService` implementation (`JetpackAiChatService` over OkHttp SSE)
- JWT token provider (WP.com Jetpack AI)
- Tool adapters and assistant-specific repository/wiring
- Result cards and assistant UI components
- Telemetry hooks
- Navigation targets or host callbacks emitted outward to `:WooCommerce`

### `:libs:ai-assistant:core`

Pure Kotlin/JVM library. The contract layer consumed by `:feature`:

- `ChatService` and completion-facing interfaces
- `AssistantEvent`, `AssistantMessage`, `ChatRequest`, `AssistantErrorKind`
- `JwtTokenProvider`
- `AssistantConfig` (pinned model / prompt / tool-catalog version triple)

No Android, Compose, Hilt, or OkHttp dependencies. Tests run as plain JVM tests.

### `:WooCommerce`

Owns only host/app concerns — no assistant feature logic:

- Entry point into the existing app surface
- Host-side navigation implementation
- App-specific DI bootstrap and feature flags
- Supplying the runtime environment the assistant module expects

## Future splits

New concerns stay inside the assistant module family, not in `:WooCommerce`. Candidates
when the time comes: `:libs:ai-assistant:cards`, `:libs:ai-assistant:tools`.
