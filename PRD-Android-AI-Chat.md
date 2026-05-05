# PRD — Android AI-Powered Troubleshooting (Chat with Support)

**Status:** Draft — active Android implementation plan
**Owner:** @iamgabrielma
**Last updated:** 2026-05-05
**Scope:** Hack week project. Local feature flag only. No remote flag, no rollout cohort, no dogfood gating, no success metrics.
**Repository note:** Dev-only local planning document. Keep this file untracked; do not commit it.

This document captures the iOS reference implementation, the existing Android scaffold, the architectural divergence between the two, and a proposed step-by-step PR plan to ship the feature on Android.

---

## 0. Android primer for iOS developers

Quick translations for the terms used throughout this doc. Skip if you already know Android.

| Android term | iOS analogue | What it actually is |
|---|---|---|
| **Repository** | A class like `SupportChatRepository` we'd write on iOS — a thin layer that hides where data comes from (network/disk/cache) from the ViewModel | NOT persistence by itself. It's a Kotlin class that *coordinates* data sources. It usually calls a network client and/or a DAO. On iOS the team often skips this and inlines logic; Android convention is to always have one. |
| **FluxC** | Yosemite | The shared store/networking layer used by the Android Woo + WP apps. Lives under `libs/fluxc/`. Has `Store` classes (like `JetpackAIStore`) that are roughly equivalent to Yosemite stores. |
| **Hilt** | Manual init / Swinject / `@Environment` injection | The dependency-injection framework. Annotations like `@Inject`, `@Singleton`, `@Module`, `@Provides`. When you see "Hilt wiring" it means "register the class so others can inject it". |
| **Room** | Core Data, but SQL-first | The local persistence library. You declare an `@Entity` (a Kotlin `data class` annotated as a table), a `@Dao` (interface with SQL methods), and migrations explicitly. Equivalent to iOS PR #16976's `StoredSupportChat`. |
| **DAO** | `NSFetchRequest` + helpers | Data Access Object. The interface where you write `@Query("SELECT ...")` and `@Insert`/`@Update` methods. Room generates the implementation. |
| **Retrofit** | URLSession + Codable, but declarative | HTTP client where you describe an API as a Kotlin interface (`@POST("...") suspend fun ...`). Returns parsed models directly. We may or may not use it for Odie — alternative is to add a method to FluxC's existing REST client. |
| **OkHttp** | URLSession | The HTTP client Retrofit sits on top of. Used directly only for streaming/SSE (which we don't need for Odie). |
| **Fragment** | UIViewController | A UI container hosted by an Activity. Smaller than an Activity, can be swapped in/out by the navigation graph. We'll have an `AiSupportChatFragment` like iOS has `SupportChatHostingController`. |
| **Activity** | UIWindow + root UIViewController | A top-level screen container. The whole app mostly lives inside `MainActivity`. Help has its own `HelpActivity`. |
| **ComposeView** | `UIHostingController` | The bridge that lets a Fragment host Jetpack Compose UI. You write Compose, wrap it in `ComposeView`, and the Fragment displays it. |
| **Jetpack Compose** | SwiftUI | The declarative UI framework. `@Composable fun MessageBubble(...)` is the equivalent of a SwiftUI `View`. `StateFlow<UiState>` collected in Compose is like `@Observable` / `@Published`. |
| **ViewModel** | `@Observable` view model class | Holds UI state and survives configuration changes (rotation). On Android it's a real base class (`androidx.lifecycle.ViewModel`). |
| **`ScopedViewModel`** | App-specific base class | The Woo Android base class on top of `ViewModel` — adds a `CoroutineScope`, a `triggerEvent()` channel for one-shot events (like navigation), and `SavedStateHandle` integration. iOS doesn't have a direct analogue; closest is your VM exposing `@Published` state plus an `AsyncStream` of events. |
| **`StateFlow<T>`** | `@Published var state: UiState` | Hot Kotlin coroutine flow that always has a current value. The standard way ViewModels expose state to Compose. |
| **`Flow<T>`** | `AsyncStream<T>` | Cold async stream. Used for one-off async sequences like "stream of SSE events". |
| **suspend function** | `async` function | A function that can pause without blocking a thread. Marked `suspend fun foo(...)`. |
| **Coroutine / `CoroutineScope`** | Swift `Task` / `TaskGroup` | Lightweight concurrency. `viewModelScope.launch { ... }` ≈ `Task { ... }` tied to the VM's lifecycle. |
| **Hilt qualifier** | A typealias used for DI disambiguation | An annotation like `@AssistantOkHttpClient` you tag on a binding so Hilt knows which one to inject when there are multiple `OkHttpClient`s. |
| **Multibinding / `@IntoSet`** | Registering things in a registry | A Hilt mechanism where many modules each contribute one entry into a `Set<T>`. Used for the AI tool registry — each tool handler binds itself with `@IntoSet`. |
| **Detekt** | SwiftLint | Static analysis / linting. Config under `config/detekt/`. |
| **WPCom-authenticated** | Same concept as iOS | The user signed in with a WordPress.com account (vs. just self-hosted creds). On Android: `selectedSite.isJetpackConnected` is the practical proxy. |
| **`SelectedSite`** | `ServiceLocator.stores.sessionManager.defaultSite` | Singleton holding the currently selected store. Inject it where needed. |
| **Trunk** | `main` / `develop` | The Android repo's main branch is literally named `trunk`. |

The general "where does code live" mental model:

```
Compose screen  ──collects──►  StateFlow<UiState>
       │                              ▲
       └──calls──►  ViewModel  ───────┘
                       │
                       ▼
                   Repository
                   /        \
                  ▼          ▼
            Retrofit/      Room DAO
            FluxC client   (local DB)
                  │
                  ▼
              Network
```

So when this doc says "Repository + chat-history persistence (Room)" in PR-2, it means:
- Write a `SupportChatRepository` Kotlin class (the coordinator) — this does NOT touch the database directly; it calls into the DAO.
- Add a Room `@Entity` + `@Dao` for the chat-history bookmarks (the actual persistence) — equivalent to iOS PR #16976's Core Data work.
- Wire both up via Hilt so the ViewModel (built in PR-3) can `@Inject` the repository.

---

## 1. References

### 1.1 Linear

- Project overview: https://linear.app/a8c/project/woo-mobile-ai-powered-troubleshooting-120ca040b453/overview
  - *Not fetched during investigation (auth-blocked). Acceptance criteria, rollout cohort, and remote-flag plan must be pulled manually before scoping milestones.*

### 1.2 iOS PRs (woocommerce/woocommerce-ios)

| # | Title | Notes |
|---|---|---|
| #16974 | [AI Support Chat] Feature flag | `FeatureFlag.aiSupportChat`, non-prod only |
| #16975 | [AI Support Chat] Networking layer | `SupportChatRemote` + response models + JSON mapper + tests |
| #16976 | [AI Support Chat] Storage Layer | Core Data entity `StoredSupportChat` (migration 136 → 137) |
| #16978 | Add Yosemite action and store for AI support chat | `SupportChatAction.sendMessage` + `SupportChatStore` |
| #16979 | Add AI support chat interface to Connectivity Tool | First user-visible UI; SwiftUI screen + VM + hosting controller |
| #16989 | [AI Support Chat] Store/access local AI chat history | Persist bookmarks, "Chat History" Help row, GET-resume |
| #16991 | Add `ai_skip` tag and attachments when escalating | Zendesk escalation tagging + transcript attachment |
| #16993 | [AI Support Chat] Gate UI elements to JP-connected only sites | Hide history row when not Jetpack-connected |
| #17004 | Add diagnostic tool for support chat | `SupportDiagnosticsService` + `SupportIssueType` |
| #17005 | Add issue picker and diagnostics flow to AI support chat | Wires diagnostics into chat; sealed `ChatMessage.Content` |

URL form: `https://github.com/woocommerce/woocommerce-ios/pull/{N}`.

### 1.3 Android PRs (woocommerce/woocommerce-android)

| Plan | # | Title | State | Notes |
|---|---:|---|---|---|
| PR-1 | [#15814](https://github.com/woocommerce/woocommerce-android/pull/15814) | [AI Support Chat] Feature flag + Help row + placeholder Fragment | Open — ready for review | `task/WOOMOB-2950` → `trunk`; closes WOOMOB-2950 |
| PR-2 | [#15815](https://github.com/woocommerce/woocommerce-android/pull/15815) | [AI Support Chat] Network layer for Odie chat endpoint | Open — ready for review | `task/WOOMOB-2951` → `task/WOOMOB-2950`; closes WOOMOB-2951 |
| PR-5a | [#15816](https://github.com/woocommerce/woocommerce-android/pull/15816) | [AI Support Chat] Add diagnostics service for AI Support Chat issue picker | Open — draft | `task/WOOMOB-2954` → `trunk`; closes WOOMOB-2954 |

---

## 2. iOS feature summary

An AI-powered support chat for merchants. Key characteristics:

- **Backend:** WordPress.com **Odie** assistant — `POST /wpcom/v2/odie/chat/{bot_slug}` (new chat) and `POST /wpcom/v2/odie/chat/{bot_slug}/{chat_id}` (follow-up). Resume via `GET /wpcom/v2/odie/chat/{bot_slug}/{chat_id}`.
- **Bot slug (default):** `woo-workflow-support_mobile_inapp`.
- **Wire format:** plain JSON request `{ message, context }`, full-thread JSON response. **No streaming, no SSE.**
- **All LLM concerns server-side** — the iOS app never talks to Anthropic/OpenAI directly.
- **Two entry points:** Help & Support (issue picker first) and Connectivity Tool (skips picker, prefilled context).
- **Diagnostics:** Targeted, sequential tests by issue type (orders, products, analytics, notifications, other) with in-app fix actions (`enableAnalytics`, `registerDevice`, `enableOrderNotifications`, `setupJetpack`, `openNotificationSettings`).
- **History:** Local bookmark only (`chatID`, `siteID`, `wpcomUserID`, `botSlug`, `title`, timestamps); transcripts re-fetched on resume.
- **Escalation to Zendesk:** Adds `in_app_support_escalate` + `ai_skip` tags, attaches connectivity log + chat transcript.
- **Gating:** Feature flag (non-prod) AND WPCom-authenticated AND (for some surfaces) Jetpack-connected.
- **No analytics events** were added on iOS — gap to close on Android.

### 2.1 iOS class graph (for porting reference)

| iOS class | Layer | Android equivalent |
|---|---|---|
| `SupportChatRemote` | Networking | Retrofit interface (or FluxC REST client) |
| `SupportChatResponse`, `SupportChatRole`, `SupportChatFlags`, `SupportChatSource` | Models | Kotlin `data class` / `enum` |
| `StoredSupportChat` (Core Data) | Storage | Room `@Entity` + DAO |
| `SupportChatStore` / `SupportChatAction` (Yosemite) | Dispatcher | Skip — fold into `SupportChatRepository` |
| `SupportChatViewModel` (`@Observable`) | Presentation | `ScopedViewModel` + `StateFlow<UiState>` |
| `SupportChatView` / `SupportChatMessageRow` (SwiftUI) | UI | Compose screen + bubble composables |
| `SupportChatHostingController` | Host | `Fragment` hosting `ComposeView` |
| `SupportDiagnosticsService` | Domain | `@Singleton` Hilt class |
| `SupportIssueType`, `EntryPoint`, `TestStatus` | Enums | Same Kotlin enums |
| `ChatMessage.Content` (sealed) | Model | `sealed interface MessageContent` |

### 2.2 Bot response shape (truncated)

```json
{
  "chat_id": 4242,
  "session_id": "...",
  "bot_slug": "woo-workflow-support_mobile_inapp",
  "messages": [{
    "message_id": 1,
    "role": "user|bot|unknown",
    "content": "<markdown>",
    "context": {
      "sources": [{"title","url","heading","content"}],
      "flags": {
        "forward_to_human_support": false,
        "canned_response": false,
        "logged_in": true,
        "branch": null
      }
    }
  }]
}
```

---

## 3. Android codebase investigation

### 3.1 Approach: mirror iOS (Odie) end-to-end

The plan is to mirror the iOS implementation as closely as Android conventions allow: hit `wpcom/v2/odie/chat/{slug}`, no streaming, no client-side LLM, no tool loop. All LLM/RAG/escalation logic lives server-side. We build a thin app stack — networking → repository → ViewModel → Compose — that matches the iOS PR plan one-to-one.

> **Note — out of scope:** A separate hack project by another team has landed a `:libs:ai-assistant:core` + `:libs:ai-assistant:feature` scaffold on `trunk` (an agentic, streaming, tool-using chat runtime over `wpcom/v2/jetpack-ai-query`). It is *not* used by this feature. We may consolidate the two stacks later, but for this hack week we ignore it entirely. **Do not import from `:libs:ai-assistant:*` in any of the PRs below.**

### 3.2 Reusable Android assets (relevant to this feature)

| Concern | Path |
|---|---|
| Help host (entry point) | `WooCommerce/src/main/kotlin/com/woocommerce/android/support/help/HelpActivity.kt` |
| Help origin enum | `WooCommerce/src/main/kotlin/com/woocommerce/android/support/help/HelpOrigin.kt` (already has `CONNECTIVITY_TOOL`) |
| Existing connectivity-troubleshooting flow | `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/troubleshooting/` |
| Settings entry pattern | `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/prefs/MainSettingsFragment.kt:218` |
| Feature flag enum | `WooCommerce/src/main/kotlin/com/woocommerce/android/util/FeatureFlag.kt` |
| Analytics events (reference for naming, even though we skip analytics this round) | `WooCommerce/src/main/kotlin/com/woocommerce/android/analytics/AnalyticsEvent.kt` |
| Strings | `WooCommerce/src/main/res/values/strings.xml` |
| Legacy single-shot AI (reference for FluxC AI calls) | `WooCommerce/src/main/kotlin/com/woocommerce/android/ai/AIRepository.kt` |
| FluxC Jetpack AI store / REST client | `libs/fluxc/.../store/jetpackai/JetpackAIStore.kt`, `.../jetpackai/JetpackAIRestClient.kt` — pattern reference for adding an Odie REST client |

### 3.3 Existing connectivity-troubleshooting flow (reuse for diagnostics)

`ui/troubleshooting/` already implements use cases that closely mirror iOS's `SupportDiagnosticsService` checks:

- `WPComConnectionCheckUseCase`
- `StoreConnectionCheckUseCase`
- `StoreOrdersCheckUseCase`
- `StoreProductsCheckUseCase`
- `JetpackErrorUtils`

These are excellent candidates to wrap as diagnostic primitives in PR-4 rather than re-implementing iOS's `SupportDiagnosticsService` from scratch.

---

## 4. Architecture summary

We mirror iOS PR-for-PR. App layering:

```
AiSupportChatScreen (Compose) ──collects──► StateFlow<UiState>
        │                                          ▲
        └──calls──► AiSupportChatViewModel ────────┘
                          │
                          ▼
                   SupportChatRepository
                   /                   \
                  ▼                     ▼
           Odie REST client        Room DAO
           (POST/GET                (chat-history
            /wpcom/v2/odie/...)      bookmarks)
```

iOS-to-Android mapping is in §2.1. No tool calling, no streaming, no client-side LLM. Single round-trip JSON per turn.

---

## 5. Proposed PR plan

Hack-week scoping: keep PRs small enough to land independently, but skip the things that don't matter for an internal demo (no remote flag, no analytics, no a11y/snapshot pass, no dogfood toggle). iOS shipped in 10 PRs; aim for fewer.

### PR-1 — Local feature flag + Help row

> **Status:** Open — ready for review: [#15814](https://github.com/woocommerce/woocommerce-android/pull/15814)

- Add a new `FeatureFlag.AI_SUPPORT_CHAT` entry (debug-only via `localValue = PackageUtils.isDebugBuild()`). Do **not** reuse the other team's `AI_ASSISTANT` flag — they are different products.
- Add `HelpOrigin.AI_TROUBLESHOOTING`.
- Add a Help row in `HelpActivity` gated on `featureFlagRepository.isEnabled(AI_SUPPORT_CHAT) && selectedSite.isJetpackConnected`. Tap → opens an empty placeholder Fragment for now.
- New strings: `ai_support_chat_help_row_title`, `_subtitle`.
- No network, no Compose, no persistence.

### PR-2 — Networking layer (Odie remote)

> **Status:** Open — ready for review, stacked on PR-1: [#15815](https://github.com/woocommerce/woocommerce-android/pull/15815)

*(Mirrors iOS PR #16975. Approach: **Option B — feature-local `*RestClient` using `WPComGsonRequestBuilder`**. See §9 for the architectural investigation and rationale.)*

- Retrofit interface or new method on the FluxC REST client for:
  - `POST /wpcom/v2/odie/chat/{bot_slug}` (new chat)
  - `POST /wpcom/v2/odie/chat/{bot_slug}/{chat_id}` (follow-up)
  - `GET /wpcom/v2/odie/chat/{bot_slug}/{chat_id}` (resume)
- Wire/domain models:
  - `SupportChatResponse`, `SupportChatMessage`, `SupportChatRole` (with unknown-tolerant decoding for forward compatibility), `SupportChatFlags`, `SupportChatSource`.
- Fixture-driven unit tests (mirror iOS `SupportChatRemoteTests`).
- No UI, no persistence, no repository yet — pure transport layer.

### PR-3 — Repository + chat-history persistence

- `SupportChatRepository` (`sendMessage`, `fetchChat`) on top of the PR-2 client.
- Room entity `SupportChatBookmark` + DAO + migration.
- `register/touch/load/delete` on the repo.
- Hilt wiring + unit tests.

### PR-4 — Compose chat shell from Help entry

> **"Compose chat shell" =** the visible UI of the chat screen, written in Jetpack Compose (Android's SwiftUI), but with **no diagnostics yet** — just the bare conversational surface. iOS analogue: PR #16979 (`SupportChatView` + `SupportChatViewModel` + `SupportChatHostingController`) minus diagnostics, history list, and escalation. "Shell" because it's the empty room before we move the diagnostics furniture in (PR-5) or wire history (PR-6) or escalation (PR-7).
>
> Concretely, after PR-4 a user can:
> 1. Tap the AI Troubleshooting row in Help.
> 2. See a greeting bubble.
> 3. Type a message, hit send, see it appear as a user bubble.
> 4. See the bot's reply rendered as markdown.
> 5. Continue the conversation (subsequent sends thread into the same `chat_id`).
> 6. The chat is saved as a bookmark in Room (so PR-6 can list it later).
>
> They CAN'T yet: pick an issue type, run diagnostics, see chat history, or escalate to human support.

- `AiSupportChatFragment` hosting Compose. *(Fragment ≈ UIViewController; it's the screen container the navigation graph opens.)* Replaces the placeholder Fragment from PR-1.
- `AiSupportChatViewModel : ScopedViewModel` with `StateFlow<UiState>`. *(VM owns the message list, in-flight state, error state — Compose collects the StateFlow and re-renders.)*
- Composables (each is a `@Composable fun`, the equivalent of a SwiftUI `View`):
  - `AiSupportChatScreen` — top-level screen, lays out the message list + input bar.
  - `MessageBubble` — one chat bubble (user vs. bot styling).
  - `InputBar` — text field + send button at the bottom.
  - `ErrorBanner` — shown when a send fails.
  - `TypingIndicator` — three-dots animation while waiting for the bot.
- Markdown rendering via `com.mikepenz:multiplatform-markdown-renderer-m3` — see §10 for the spike, integration sketch and risks.
- On send → call `SupportChatRepository.sendMessage(...)` from PR-3; on first successful response → persist bookmark via the same repo.
- Strings: `ai_support_chat_*` keys in `strings.xml`.

### PR-5a — Diagnostics domain core

> **Status:** Open — draft: [#15816](https://github.com/woocommerce/woocommerce-android/pull/15816)

- `SupportIssueType`, `TestStatus`, `DiagnosticTest`, `SuggestedFixAction`, `DiagnosticResult`.
- `SupportDiagnosticsService` — `@Singleton` orchestrator with `fun runDiagnostics(issueType): Flow<DiagnosticResult>`. Wraps the existing `ui/troubleshooting/useCases/*`. Stops at first failure, attaches a `SuggestedFixAction`.
- Pure-domain — no UI consumer, no chat flow wiring.
- Independent of PR-1/PR-2/PR-3/PR-4 (branched off `trunk`).

### PR-5b — Issue picker + diagnostic chat flow

- Sealed `MessageContent` (`Text`, `IssuePicker`, `DiagnosticsProgress`, `DiagnosticsSuccess`, `DiagnosticsFailure`).
- In-place progress bubble mutation as tests transition `Pending → Running → Passed/Failed`.
- Fix-action buttons that re-run the relevant test on completion. Possibly add richer fix actions (`EnableAnalytics`, `EnableOrderNotifications`, `SetupJetpack`, `RegisterDevice`) if time permits — `OpenNotificationSettings` and `RetryDiagnostics` already exist from PR-5a.
- `EntryPoint` enum (`HelpAndSupport`, `ConnectivityTool`, `ChatHistory`); Help entry shows picker first.
- `proceedToChat()` builds final `context` payload.
- Depends on PR-4 (needs the chat shell + `MessageContent` host).

### PR-6 — Chat history list + Connectivity Tool entry

- "Chat History" Help row, list, swipe-to-delete bookmark.
- Tap row → resume via `fetchChat`.
- Chat button in `TroubleshootConnectionScreen` (post-checks); skips picker, prefills connectivity context.

### PR-7 — Zendesk escalation

- `forward_to_human_support` → escalation banner.
- "Contact Support" → existing Zendesk form with `sourceTag = "in_app_support_escalate"`, `additionalTags = ["ai_skip"]`, attaches connectivity log + chat transcript (`.txt`).
- Transcript formatter mirroring iOS placeholders.

### Explicitly out of scope for hack week

- Remote feature flag entry. (Local debug-only flag is enough.)
- Analytics events. (Re-add later if the project graduates beyond hack week.)
- Accessibility pass / TalkBack labels.
- Snapshot / Paparazzi tests.
- Dogfood / rollout cohort wiring.
- Tablet/landscape polish.

---

## 6. Open questions

1. **Bot slug** — confirm `woo-workflow-support_mobile_inapp` is the slug for Android, or if a separate Android-specific slug is expected.
2. **Context payload** — Android equivalents for `ios_version`/`app_version` (`android_version`, `device_model`, `app_version`); confirm `context` schema isn't strict server-side.
3. ~~**Markdown library** — pick one and confirm bot output is inline-only (bold, links, lists). No code blocks?~~ **Resolved: see §10. Adopting `com.mikepenz:multiplatform-markdown-renderer-m3:0.36.0` in PR-4.**
4. **Feature gate predicate** — exact equivalent of iOS `!isAuthenticatedWithoutWPCom`. Likely `selectedSite.isJetpackConnected` (or WPCom). Confirm.
5. **Diagnostics parity** — map iOS-only services to Android equivalents (push registration, notification settings, Jetpack-install). Drop any that are too expensive for hack week.
6. **Zendesk SDK tags** — confirm `additionalTags` and chat-transcript attachment work via the Android Zendesk SDK we use today.

---

## 7. Effort estimate (hack week)

| PR | Est. | Notes |
|---|---|---|
| 1 | S | Flag + Help row + placeholder Fragment |
| 2 | M | Odie networking + models + fixture tests |
| 3 | M | Repo + Room migration + DAO |
| 4 | L | Compose chat screen + markdown + ViewModel |
| 5a | M | Diagnostics domain core (`SupportDiagnosticsService` + sealed types) |
| 5b | M | Sealed `MessageContent` + picker UX + fix-action buttons |
| 6 | M | History list + Connectivity Tool entry |
| 7 | M | Zendesk escalation + transcript |

S ≈ 0.5–1 day, M ≈ 1–3 days, L ≈ 3–5 days. **Hack-week target: PR-1 → PR-4 (working chat from Help, no diagnostics).** Stretch: PR-5a + PR-5b. Anything beyond is post-hack-week.

---

## 8. Suggested Linear issues to log

Hack-week ticket list. Titles use the iOS `[AI Support Chat]` prefix for parity. Skip everything related to remote flagging, dogfood, analytics, and rollout.

### Spikes (file first, all small)

- ~~[Spike] Confirm Odie API contract for Android~~ — skipped (hack-month project, no backend hand-off).
- ~~[Spike] Pick Compose Markdown library~~ — resolved in §10.

### Implementation (mirror §5)

- **[AI Support Chat] Local feature flag + Help row + placeholder Fragment** — PR-1 → [#15814](https://github.com/woocommerce/woocommerce-android/pull/15814) *(open, ready for review)*
- **[AI Support Chat] Networking layer (Odie remote)** — PR-2 → [#15815](https://github.com/woocommerce/woocommerce-android/pull/15815) *(open, ready for review; stacked on PR-1)*
- **[AI Support Chat] Repository + chat-history persistence (Room)** — PR-3
- **[AI Support Chat] Compose chat shell from Help entry** — PR-4 *(hack-week MVP target)*
- **[AI Support Chat] Diagnostics domain core** — PR-5a *(stretch)* → [#15816](https://github.com/woocommerce/woocommerce-android/pull/15816) *(open, draft)*
- **[AI Support Chat] Issue picker + diagnostic chat flow** — PR-5b *(stretch, depends on PR-4)*
- **[AI Support Chat] Chat history list + Connectivity Tool entry** — PR-6 *(post-hack-week)*
- **[AI Support Chat] Zendesk escalation with `ai_skip` tag and transcript** — PR-7 *(post-hack-week)*

### Risk / cross-cutting (file but don't block hack week)

- **[AI Support Chat] Token expiry and re-auth UX** — define behavior when WPCom session expires mid-chat.
- **[AI Support Chat] Offline + reconnection behavior** — error states and retry UX (iOS doesn't address this either).
- **[AI Support Chat] Markdown rendering audit** — safe link handling (Chrome custom tabs), no HTML execution.

Block relationships: Spikes block PR-1/PR-2; PR-2 blocks PR-3; PR-3 blocks PR-4; PR-4 blocks PR-5b/6/7. **PR-5a is independent** of the rest of the chain (branched off `trunk`).

---

## 9. PR-2 architectural investigation — networking layer

### 9.1 The question

For PR-2, where should the Odie REST client live? The Android codebase has multiple coexisting conventions for "I need to call a WPCom v2 endpoint", so this is not a paint-by-numbers decision.

### 9.2 Options considered

| | **A. New `*RestClient` + `*Store` inside `:libs:fluxc/`** | **B. Feature-local `*RestClient` under `WooCommerce/src/main/.../ui/aisupportchat/networking/`** | **C. Raw OkHttp + kotlinx.serialization** |
|---|---|---|---|
| Closest analogue | Canonical FluxC pattern (Networking + Store layer). Maps 1:1 to iOS Networking + Yosemite. | `SitePlanRestClient`, `SubscriptionRestClient`, `GiftCardRestClient`, `WooShippingLabelRestClient` — recent feature work. | `:libs:ai-assistant:feature` (`JetpackAiChatService`). |
| Auth | `BaseWPComRestClient` → standard WPCom OAuth bearer. | `BaseWPComRestClient` + `WPComGsonRequestBuilder` → same standard WPCom OAuth bearer. | Custom `OkHttpClient` + custom token provider. |
| JSON | Gson (FluxC convention). | Gson (FluxC convention). | kotlinx.serialization. |
| LOC for 3 endpoints | ~250–400 (RestClient + Store + Action + DTOs + dispatcher boilerplate). | ~150–230 (RestClient + DTOs only). | ~150–250 (Service + DTOs + custom error mapping). |
| Module change | Cross-module PR — touches `:libs:fluxc/`. | Single-module PR. | Single-module PR. |
| Couples to `:libs:ai-assistant:*`? | No. | No. | **Yes, transitively** — would either pull in that module or duplicate its primitives (qualifiers, OkHttp, JSON, JWT provider). PRD §3.1 forbids both. |

### 9.3 iOS evidence

Sources: PRs #16975 (Networking), #16978 (Yosemite Store + Action), #16989 (Storage + `fetchChat`), #17004 (diagnostics — unrelated to networking), #17005 (UI — unrelated to networking).

- **Module split:** `Modules/Sources/Networking/Remote/SupportChatRemote.swift` (transport + DTOs) → `Modules/Sources/Yosemite/Stores/SupportChatStore.swift` (action/store layer). Models declared in Networking are re-exported via typealiases from Yosemite.
- **Auth:** Standard `DotcomRequest(wordpressApiVersion: .wpcomMark2, …)` — same WPCom OAuth bearer used by every other authenticated remote in the app. No custom auth, no app-password fallback, no anonymous-auth path.
- **Mapper:** `Modules/Sources/Networking/Mapper/SupportChatResponseMapper.swift` — separate named mapper, not inline. `JSONDecoder` with `CodingKeys` enums for snake_case mapping.
- **Async style:** `async throws` at the remote, wrapped in `Task { … MainActor.run { completion(...) } }` at the Store boundary.
- **Forward-compat hooks:** `SupportChatRole` decodes unknown roles into a `.unknown` case. `SupportChatFlags` decodes each field with `try?` defaulting to `false`/`nil`. `SupportChatFlags.branch: String?` is opaque text.
- **Bot widening to all-users:** the only client lever is the `botSlug` injected into the ViewModel (`woo-workflow-support_mobile_inapp` → `woo-workflow-support_mobile_inapp_all_users` in PR #17005). **Networking and Yosemite layers are bot-agnostic — they treat `botSlug` purely as a path component.** No new auth path, no transport changes, no per-cohort plumbing in the networking layer.
- **Error taxonomy:** None bespoke. `SupportChatRemote` propagates whatever `enqueue` throws; tests assert against the shared `NetworkError.notFound()` / `NetworkError.timeout()` cases.

**Implication:** the iOS choice was Option-A-equivalent, but the auth/transport choices that motivated it are also available to us under Option B.

### 9.4 Android evidence (last 6–12 months)

- **Zero net-new RestClients added to `:libs:fluxc/` core in the last 6 months.** The only RestClient touched in that window was `ThemeRestClient.kt` (Java→Kotlin conversion, not new functionality). One net-new `BookingsRestClient` was added in 2025-09 — but inside `:libs:fluxc-plugin/`, not core.
- **Feature-level RestClients are now the norm.** Recent additions, all colocated with the feature, all extending `BaseWPComRestClient` and injecting `WPComGsonRequestBuilder` (or `WooNetwork` for Woo `wcshipping/v1` / `wc/v3`):
  - `WooCommerce/src/main/.../ui/plans/networking/SitePlanRestClient.kt` (136 LOC, 3 endpoints — most representative).
  - `WooCommerce/src/main/.../ui/orders/wooshippinglabels/networking/WooShippingLabelRestClient.kt` (351 LOC, ~14 calls).
  - `WooCommerce/src/main/.../network/subscription/SubscriptionRestClient.kt`.
  - `WooCommerce/src/main/.../network/giftcard/GiftCardRestClient.kt`.
  - `WooCommerce/src/main/.../network/environment/EnvironmentRestClient.kt`.
- **Older AI features** (`AIRepository` for product-description / sharing / thank-you-note) go through the FluxC `JetpackAIStore` — Option-A-style but predates the trend.
- **`:libs:ai-assistant:feature`** (the *other* team's hack project, WOOMOB-2887 → 2898) bypassed FluxC entirely with raw OkHttp + kotlinx.serialization. PRD §3.1 forbids us from coupling to that module.
- **JSON tooling:** FluxC = Gson exclusively (`grep -l "kotlinx.serialization" libs/fluxc/src/main/...` returns zero). kotlinx.serialization is only used inside `:libs:ai-assistant:feature`.

### 9.5 Decision: Option B

Adopt the **feature-local `*RestClient` pattern using `WPComGsonRequestBuilder` + `BaseWPComRestClient`**, colocated with the feature under `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/aisupportchat/networking/`.

Why:

1. **Matches the Android trend.** Recent feature work on Android is decisively away from cross-module FluxC additions. We follow the same path the rest of the team is taking.
2. **Mirrors iOS where it matters.** Auth and transport go through standard WPCom OAuth (same as iOS `DotcomRequest`). Bot widening to all-users handled identically — single `botSlug` constant, no networking change required.
3. **No coupling to `:libs:ai-assistant:*`.** Honors PRD §3.1.
4. **Minimal LOC.** ~150–200 LOC for the rest client + DTOs + tests. No dispatcher / `*Action` / Store boilerplate, no kotlinx.serialization dependency to introduce.
5. **Single-module PR.** Faster review cycle, no cross-module coordination.
6. **Forward-compat path is clear.** If the feature graduates and another module needs to consume the chat networking, lift the rest client into FluxC then — that's a refactor with a clear seam, not a redesign.

### 9.6 File plan for PR-2 (forward reference for the implementation step)

All paths under `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/aisupportchat/networking/` unless noted:

1. `SupportChatRestClient.kt` — `class SupportChatRestClient @Inject constructor(...) : BaseWPComRestClient(...)`. Three suspend methods: `sendMessage(botSlug, message, context)`, `sendFollowUpMessage(botSlug, chatId, message)`, `fetchChat(botSlug, chatId)`. Each returns a sealed `SupportChatResult` (Success / Error). Uses `wpComGsonRequestBuilder.syncPostRequest` / `syncGetRequest`.
2. `model/SupportChatResponse.kt` — wire DTO. `@SerializedName` per field. Mirrors iOS `SupportChatResponse`.
3. `model/SupportChatMessage.kt` — `message_id`, `role`, `content`, `context`.
4. `model/SupportChatRole.kt` — `enum class SupportChatRole { USER, BOT, UNKNOWN }` with a custom Gson `JsonDeserializer` (or `@JsonAdapter`) that decodes unknown values to `UNKNOWN`.
5. `model/SupportChatFlags.kt` + `model/SupportChatSource.kt` — supporting DTOs. `branch: String?` opaque. Boolean flags default to `false` if missing.
6. **Tests:** `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/aisupportchat/networking/SupportChatRestClientTest.kt` — `BaseUnitTest`, mockito-kotlin, mocks `WPComGsonRequestBuilder`. Covers send / follow-up / fetch request shapes, response decoding (sources, flags, missing optional fields, unknown role), and 404 / 408 error paths.
7. **Fixtures:** `WooCommerce/src/test/resources/support-chat/{support-chat-send-message,support-chat-forward-to-human,support-chat-unknown-role,support-chat-fetch-chat}.json` — copy verbatim from iOS `Modules/Tests/NetworkingTests/Resources/`.

Possibly one Hilt `@Module` if `BaseWPComRestClient` constructor visibility requires it; usually `@Inject constructor` resolves transitively (verify during implementation).

### 9.7 Reusable references (verified during research)

| Concern | Path |
|---|---|
| WPCom REST base class | `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpcom/BaseWPComRestClient.kt` |
| Gson + auth helper | `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpcom/WPComGsonRequestBuilder.kt` |
| Smallest WPCOMV2 example (110 LOC, 1 endpoint) | `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpcom/mobilepay/MobilePayRestClient.kt` |
| Larger WPCOMV2 example (418 LOC, 7 endpoints) | `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpcom/jetpackai/JetpackAIRestClient.kt` |
| **Pattern to copy** (3 endpoints, feature-module-resident, 136 LOC) | `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/plans/networking/SitePlanRestClient.kt` |

### 9.8 Glossary additions for §0

For the iOS reader, the new terms in this section are:

| Term | iOS analogue | What it is |
|---|---|---|
| `BaseWPComRestClient` | `Networking` module's request infra (`enqueue` + `DotcomRequest`) | Kotlin abstract base class living in `:libs:fluxc/`. Subclasses get WPCom OAuth, JSON parsing via Gson, error mapping, and access to `WPComGsonRequestBuilder`. The Android equivalent of inheriting from a "Remote" base on iOS. |
| `WPComGsonRequestBuilder` | Inline `enqueue`/`URLSession` wrappers | Kotlin helper: `syncPostRequest(...)`, `syncGetRequest(...)`. Returns sealed `Response<T>` (Success / Error). The thing you actually call from a `*RestClient`. |
| "Feature module" | App layer (`WooCommerce/Classes/...`) | The app target — `:WooCommerce`. Contrast with the FluxC library module `:libs:fluxc/`. Recent Android convention is to put feature-specific REST clients here, not in FluxC. |

---

## 10. Markdown library spike (PR-4)

### 10.1 Question

Which Compose markdown library should we adopt to render bot replies in the AI Support Chat bubbles?

### 10.2 Existing usage

The codebase does **not** currently render markdown anywhere. The only mentions of "markdown" are an `AIRepository.kt` comment about stripping ```` ``` ```` JSON fences from OpenAI responses, and an unrelated `_wpcom_is_markdown` product-meta key in fluxc tests. Greenfield — pick on technical merit.

Project anchors the integration must respect:
- Theme: `WooCommerce/src/main/kotlin/com/woocommerce/android/ui/compose/theme/Theme.kt` (`WooTheme`, Material3-backed).
- Link helper: `WooCommerce/src/main/kotlin/com/woocommerce/android/util/ChromeCustomTabUtils.kt` — `ChromeCustomTabUtils.launchUrl(context, url)`.
- Compose BOM `2025.11.00`, Material3 `1.4.0`, Kotlin `2.3.21` with the `org.jetbrains.kotlin.plugin.compose` plugin (`gradle/libs.versions.toml`).

### 10.3 Candidates compared

| Criterion | jeziellago/compose-markdown | **mikepenz/multiplatform-markdown-renderer** | halilibo/compose-richtext (richtext-commonmark) | Markwon (noties) raw |
|---|---|---|---|---|
| Latest release | `0.5.7` (2024) | **`0.36.0` (2025, very active)** | `1.0.0-alpha02` (2024, slow) | `4.6.2` (2023, maintenance) |
| Stars (rough) | ~600 | ~900 | ~1.5k | ~9k |
| Platform | AndroidX-only | KMP (Android target works on `androidx.compose.ui`) | KMP | Android Views (no Compose) |
| Engine | Wraps Markwon + `AndroidView(TextView)` | **Pure Compose**, JetBrains `markdown` parser | Pure Compose, commonmark-java | Native Android Views |
| Bundle impact | Medium (~600 KB aar+deps) | Small/medium (~300–400 KB) | Medium (~500 KB) | Medium (~400 KB) but adds AndroidView interop cost |
| Inline subset support | Yes, full | Yes, full + easy to disable code/tables | Yes | Yes |
| Link interception | `onLinkClicked: (String) -> Unit` callback | **`LocalUriHandler` override** (Compose-idiomatic) | `LocalUriHandler` override | Custom `LinkResolver` on builder |
| Theming hook | Inherits `MaterialTheme`; `style: TextStyle` param | **First-class `markdownTypography()` + `markdownColor()` from `MaterialTheme`** | `RichTextStyle` from MaterialTheme | None for Compose; needs wrapper |
| Compose 2.x compatibility | OK (no compiler coupling — uses `AndroidView`) | OK — releases track current Compose BOM | Stale; alpha API churn risk | N/A (not Compose) |
| License | Apache-2.0 | Apache-2.0 | Apache-2.0 | Apache-2.0 |
| Known limitations | Hidden `TextView` underneath — limits selection/semantics/animation parity | Doesn't render every GFM extension (fine for our subset) | Less actively maintained | No Compose support out of the box |

### 10.4 Decision: `mikepenz/multiplatform-markdown-renderer`

Adopt **`com.mikepenz:multiplatform-markdown-renderer-m3:0.36.0`** (the Android artifact resolves through KMP gradle metadata; works on `androidx.compose.ui` despite the "multiplatform" name).

Why:

1. **Pure Compose** — no `AndroidView`/`TextView` indirection inside chat bubbles. Selection, semantics, animations, and previews behave like the rest of our Compose UI.
2. **Native Material3 theming hook** — `markdownColor()` and `markdownTypography()` accept values straight from `MaterialTheme.colorScheme` / `MaterialTheme.typography`, slotting into `WooTheme` with zero custom drawing.
3. **Compose-idiomatic link interception** via `LocalUriHandler` — no bespoke callback API.
4. **Active maintenance** (releases through 2025), tracks current Compose BOM, no known issues with Kotlin `2.3.21` + compose-compiler-plugin.
5. **Disabling code blocks / tables is trivial** if the bot ever emits them — register no-op `components` overrides — but per the iOS evidence the bot stays inline-only.

`jeziellago/compose-markdown` is the runner-up and a reasonable fallback if mikepenz' library hits a surprise during PR-4. Smaller surface area, but the `TextView` underbelly is a long-term liability for a chat surface where we may want streaming/typing animations.

### 10.5 Integration sketch (PR-4 reference)

```kotlin
// WooCommerce/src/main/kotlin/.../ui/aisupportchat/BotMessageBubble.kt
@Composable
fun BotMessageBubble(content: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val chromeUriHandler = remember(context) {
        UriHandler { url -> ChromeCustomTabUtils.launchUrl(context, url) }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        CompositionLocalProvider(LocalUriHandler provides chromeUriHandler) {
            Markdown(
                content = content,
                colors = markdownColor(text = MaterialTheme.colorScheme.onSurface),
                typography = markdownTypography(
                    text = MaterialTheme.typography.bodyMedium,
                    link = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}
```

Gradle additions for PR-4 (in `WooCommerce/build.gradle` plus a version entry in `gradle/libs.versions.toml`):

```kotlin
implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.36.0")
// Skip the -android artifact — we don't need image rendering and want to avoid surprise network calls from bot text.
```

### 10.6 Risks / followups

- **If the bot ever emits code blocks or tables**: mikepenz renders them with monospace/box styling that may look off in a bubble. Mitigation: register no-op `components` overrides in a `markdownComponents { ... }` block, or fall back to `jeziellago/compose-markdown`.
- **Image syntax (`![]()`)**: mikepenz tries to load via Coil if the `-android` artifact is on the classpath. Per PRD, no images expected; omit the `-android` artifact to avoid surprise network calls.
- **Streaming partial markdown**: when typing/streaming is added in a later PR, recomposing the full `Markdown(content)` on every token is fine for short bubbles but worth profiling. If choppy, debounce or split rendered/streaming spans.
- **Selection / copy**: confirm long-press copy works during PR-4 QA — both candidates have had selection bugs historically.
- **License**: Apache-2.0, no a8c policy concern.
