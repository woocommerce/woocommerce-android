# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.
Detailed patterns for specific tasks are in `.agents/skills/`.

## Cross-Tool Instructions

- Use `LOCAL_AGENT_NOTES.md` or `CLAUDE.local.md` for repository-local AI preferences when present
- `CLAUDE.local.md` is supported for backward compatibility

## Repository Layout

```
WooCommerce/                 Main Android app (MVVM, Compose UI)
  src/main/kotlin/com/woocommerce/android/
    analytics/               Event tracking (AnalyticsEvent, AnalyticsTracker)
    di/                      Hilt dependency injection modules
    extensions/              Kotlin extension functions (Context, Date, Flow, etc.)
    model/                   Domain models
    ui/                      Feature packages (orders, products, payments, etc.) + shared compose components
    viewmodel/               Base classes (ScopedViewModel, MultiLiveEvent)
  src/test/                  Unit tests (mirrors main source structure)

WooCommerce-Wear/            Wear OS companion app

libs/
  fluxc/                     Core networking + data layer (Room + WPCom REST)
  fluxc-annotations/         Annotation processing for FluxC
  fluxc-processor/           FluxC annotation processor
  fluxc-plugin/              FluxC plugin system
  fluxc-tests/               FluxC integration/unit test utilities
  cardreader/                Card reader for in-person payments
  login/                     Authentication and login flows
  pos/                       Point of sale functionality
  commons/                   Shared Compose components, utilities, and test fixtures
  apifaker/                  API mocking library for testing
  detektrules/               Custom detekt rules for this project

config/detekt/               Detekt configuration (detekt.yml, baseline.xml)
docs/                        Development guidelines
.buildkite/                  CI pipeline configuration
.agents/skills/              AI agent skills (shared across tools)
```

## Build Commands

- Build debug APK: `./gradlew :WooCommerce:assembleWasabiDebug`
- Install on device: `./gradlew :WooCommerce:installWasabiDebug`
- Clean build: `./gradlew clean`
- Run detekt (with auto-correct): `./gradlew detektAll --auto-correct`
- Android lint: `./gradlew lintWasabiRelease`

## Unit Test Commands

- All tests: `./gradlew testWasabiDebugUnitTest`
- Main app (single test): `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.TestName"`
- CardReader: `./gradlew :libs:cardreader:testDebugUnitTest --tests "*.TestName"`
- Login: `./gradlew :libs:login:testDebugUnitTest --tests "*.TestName"`
- FluxC: `./gradlew :libs:fluxc:testDebugUnitTest --tests "*.TestName"`
- POS: `./gradlew :libs:pos:testDebugUnitTest --tests "*.TestName"`
- Commons: `./gradlew :libs:commons:testDebugUnitTest --tests "*.TestName"`

## Architecture

The app has two distinct parts with **different architectures**. Identify which one you're working in before writing code — using the wrong patterns is a common mistake.

### Store Management (main app)

The store management app lets merchants manage orders, products, analytics, etc. from their phone. It uses MVVM with Fragments hosting Compose UI.

```
┌─────────────────────────────────┐
│  Fragment (hosts ComposeView)   │   UI layer — Jetpack Compose inside Fragments
├─────────────────────────────────┤
│  ViewModel (ScopedViewModel)    │   Presentation — state, events, business logic orchestration
├─────────────────────────────────┤
│  Repository / Use Case          │   Domain — coordinates data sources
├─────────────────────────────────┤
│  FluxC Store → REST API / Room  │   Data — networking + local persistence
└─────────────────────────────────┘
```

- Code: everything under `ui/` **except** `ui/woopos/`
- Navigation: XML nav graphs with `NavController`
- ViewModels extend `ScopedViewModel`
- Events via `triggerEvent()` / `MultiLiveEvent`
- Skills: `store-compose`, `store-viewmodel`, `store-analytics`, `store-tests`

### POS (Point of Sale)

POS is a tablet-only, landscape-only register interface for in-person sales. It runs in its own Activity with a completely separate architecture.

```
┌─────────────────────────────────┐
│  WooPosActivity (setContent)    │   100% Compose — no Fragments, no XML layouts
├─────────────────────────────────┤
│  ViewModel (plain ViewModel)    │   State via StateFlow, parent-child SharedFlow event bus
├─────────────────────────────────┤
│  Repository / Use Case          │   Domain — coordinates data sources
├─────────────────────────────────┤
│  FluxC Store → REST API / Room  │   Data — networking + local persistence
└─────────────────────────────────┘
```

- Code: `ui/woopos/` — all classes prefixed with `WooPos`
- Navigation: Compose Navigation (`NavHost`)
- ViewModels extend plain `ViewModel()` — NOT `ScopedViewModel`
- Events via parent-child SharedFlow bus
- Own design system: `WooPosTheme`, `WooPosSpacing`, `WooPosTypography`
- Skills: `pos`, `pos-analytics`, `pos-tests`

### How to tell which one you're in

- File path contains `woopos/` or class name starts with `WooPos` → **POS**
- Everything else → **Store Management**

If working in POS code: do NOT use `ScopedViewModel`, `triggerEvent()`, `MultiLiveEvent`, Fragments, or XML nav graphs — these are store-only patterns.

| Aspect | POS | Main App |
|--------|-----|----------|
| Base class | `ViewModel()` | `ScopedViewModel(savedStateHandle)` |
| Coroutines | `viewModelScope.launch {}` | `launch {}` (from CoroutineScope) |
| State | `StateFlow<T>` | `StateFlow<T>` or `LiveData<T>` |
| Events | Parent-child SharedFlow bus | `triggerEvent()` / `MultiLiveEvent` |
| Analytics | `WooPosAnalyticsTracker` | `AnalyticsTrackerWrapper` |
| Navigation | Compose Navigation | Fragment nav graphs |

### Shared across both

Both parts use Kotlin Coroutines, Hilt DI, and the same data layer:
- ViewModels MUST NOT import Android framework classes
- Data flows: FluxC DTOs → Room entities → domain models → ViewModels → Compose UI
- ViewModels never access Room or network directly — always through repositories

## Kotlin & Android Conventions

- MUST use Kotlin for all new code
- Max line length: 120 characters (exception: test names)
- No wildcard imports
- No `FIXME` — use `TODO` instead
- Comments should be very rare — only for complex business logic or intent. Keep existing comments when refactoring
- Constants: `UPPER_SNAKE_CASE`
- Companion objects at the bottom of the class
- Prefer `val` over `var`, immutable collections over mutable
- Use `sealed class` / `sealed interface` for restricted hierarchies
- Use `data class` for value types
- Avoid `!!` — use safe calls, `requireNotNull()`, or `checkNotNull()`
- Detekt rules enforced: see `config/detekt/detekt.yml`

## Git & PR Conventions

- Main branch: `trunk`
- Feature branches: `issue/ISSUEID-description` (e.g., `issue/1234-fix-order-list`)
- Commit messages: < 100 characters, focus on what was done
- MUST NOT include "Co-Authored-By" in commit messages
- MUST NOT mention that code was generated by AI or any AI tool
- Use skill for PR creation and editing

## Operational Rules

- MUST NOT run `./gradlew` commands in the background unless explicitly told to

## Common Pitfalls

- Don't import Android framework classes (Context, View, etc.) in ViewModels
- Don't force unwrap with `!!` — handle nullability properly
- Don't use `LiveData` for new state in ViewModels — prefer `StateFlow` (existing `LiveData` is fine)
- Don't use `remember` outside of `@Composable` functions
- Don't pass `ViewModel` instances between composables — pass state and callbacks instead
- Don't apply store patterns to POS or vice versa (see Architecture section above)

For app-specific pitfalls, see the relevant skills (`pos`, `store-compose`, `store-viewmodel`, etc.).

## Environment Setup

- Requires `secrets.properties` at `~/.configure/woocommerce-android/secrets/`
- Copy from `defaults.properties` and fill in OAuth2 credentials (see README)
- Java 21 (Amazon Corretto) required for remote build cache
- `local.properties` is auto-generated by Android Studio (contains SDK path)

## Further Reading

Detailed patterns and conventions are in the `docs/` folder and loaded on-demand via skills. See:
- `docs/store-compose.md` — Jetpack Compose guidelines (store app)
- `docs/store-viewmodel-patterns.md` — ViewModel patterns (store app)
- `docs/store-tracking-events.md` — Analytics tracking (store app)
- `docs/store-testing.md` — Unit testing (store app)
- `docs/pos-architecture.md` — POS architecture, ViewModel, design system
- `docs/pos-tracking-events.md` — Analytics tracking (POS)
- `docs/pos-testing.md` — Unit testing (POS)
- `docs/pull-request-guidelines.md` — PR conventions
- `docs/coding-style.md` — Kotlin coding style and detekt

@CONVENTION.md
