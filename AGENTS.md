# AGENTS.md

This file provides guidance to AI agents when working with code in this repository.

## Repository Layout

```
WooCommerce/                 Main Android app (MVVM, Compose UI)
  src/main/kotlin/com/woocommerce/android/
    analytics/               Event tracking (AnalyticsEvent, AnalyticsTracker)
    di/                      Hilt dependency injection modules
    extensions/              Feature packages (orders, products, payments, etc.)
    model/                   Domain models
    ui/                      Shared UI (compose/components, compose/theme)
    viewmodel/               Base classes (ScopedViewModel, MultiLiveEvent)
  src/test/                  Unit tests (mirrors main source structure)

WooCommerce-Wear/            Wear OS companion app

libs/
  fluxc/                     Core networking + data layer (Room + WPCom REST)
  fluxc-annotations/         Annotation processing for FluxC
  fluxc-processor/           FluxC annotation processor
  fluxc-plugin/              FluxC plugin system
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

This project uses **MVVM** with Kotlin Coroutines and Hilt DI. Key boundaries:

- Compose screens live inside Fragments (1:1 relationship)
- ViewModels MUST NOT import Android framework classes directly
- Navigation uses XML nav graphs with `NavController`
- Data layer uses FluxC with a plugin-based architecture

### ScopedViewModel Pattern

All ViewModels extend `ScopedViewModel` which provides:
- Coroutine scope via `viewModelScope` (use `launch {}` for async work)
- One-shot events via `triggerEvent()` + `MultiLiveEvent`
- State conversion helpers (`toStateFlow()`)

Typical ViewModel structure:

```kotlin
@HiltViewModel
class MyFeatureViewModel @Inject constructor(
    private val repository: MyRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {

    private val _viewState = MutableStateFlow(MyViewState())
    val viewState = _viewState.asLiveData()

    fun onActionClicked() {
        analyticsTrackerWrapper.track(AnalyticsEvent.MY_FEATURE_ACTION_TAPPED)
        launch {
            repository.doSomething()
                .onSuccess { result ->
                    _viewState.update { it.copy(data = result) }
                }
                .onFailure { error ->
                    triggerEvent(ShowSnackbar(R.string.error_message))
                }
        }
    }
}
```

### Fragment ↔ Compose Pattern

Fragments create a `ComposeView` in `onCreateView` with proper lifecycle disposal:

```kotlin
@AndroidEntryPoint
class MyFeatureFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                WooThemeWithBackground {
                    MyFeatureScreen(
                        onBackClick = { findNavController().navigateUp() }
                    )
                }
            }
        }
    }
}
```

### Hilt Dependency Injection

- ViewModels: `@HiltViewModel` + `@Inject constructor`
- `SavedStateHandle` MUST be the last constructor parameter
- All dependencies injected via constructor, never accessed from service locators
- DI modules live in `WooCommerce/src/main/kotlin/com/woocommerce/android/di/`

### Entity / Data Flow

FluxC network DTOs → Room entities → domain models exposed to ViewModels.
ViewModels never interact with Room or network directly — always through repositories.

## Kotlin & Android Conventions

- MUST use Kotlin for all new code
- Max line length: 120 characters (exception: test names can be longer)
- No wildcard imports
- No `FIXME` — use `TODO` instead
- Comments should be very rare — only when they explain complex business logic or intent. Also keep existing comments when applying any refactor
- Constants: `UPPER_SNAKE_CASE`
- Companion objects MUST be placed at the bottom of the class
- Avoid reflection in new code
- Prefer `val` over `var`, immutable collections over mutable
- Use `sealed class` / `sealed interface` for restricted hierarchies
- Use `data class` for value types
- Avoid `!!` (force unwrap) — use safe calls, `requireNotNull()`, or `checkNotNull()`
- Detekt rules enforced: see `config/detekt/detekt.yml` for customizations

## Jetpack Compose

- Use Jetpack Compose for all new UI
- `@Composable` functions returning Unit: PascalCase noun names (not verbs)
- MUST accept `Modifier` as the first optional parameter, named `modifier`
- State hoisting: state up, events down (via lambdas)
- MUST NOT acquire ViewModels inside composables — inject as parameter with default value
- Wrap content in a container (`Column`, `Row`, `Box`) — no top-level emission
- Use `WooTheme` or `WooThemeWithBackground` as the root theme
- `remember {}` all `mutableStateOf` / `derivedStateOf`
- Pass only immutable types as parameters (no `MutableList`, `MutableState`, etc.)
- Use `by` property delegates for state: `var foo by rememberSaveable { mutableStateOf(1) }`

## Testing Patterns

- MUST extend `BaseUnitTest` (from `libs/commons/src/testFixtures/`)
  - Sets up `InstantTaskExecutorRule` and `CoroutineTestRule` with `UnconfinedTestDispatcher`
  - Provides `testBlocking {}` helper (wraps `runTest`)
- Naming: `` `given X, when Y, then Z` `` or `` `when X, then Y` `` (backtick-wrapped, BDD style)
- Body structure with comment sections:

```kotlin
@Test
fun `given user is logged in, when refresh is pulled, then data reloads`() = testBlocking {
    // GIVEN
    whenever(repository.isLoggedIn()).thenReturn(true)

    // WHEN
    viewModel.onPullToRefresh()

    // THEN
    assertThat(viewModel.viewState.value?.isLoading).isFalse()
    verify(repository).fetchData()
}
```

- Assertions: AssertJ (`assertThat(...).isEqualTo(...)`)
- Mocking: mockito-kotlin (`mock()`, `whenever(...).thenReturn(...)`, `verify(...)`)
- Compose UI tests: `ComposeTestRule` with finders/assertions/actions
  - MUST use `waitUntil` for async — NEVER use `Thread.sleep`
- What to test: ViewModels (state changes + events), Repositories (data flow), FluxC stores (action handling)
- Test file locations mirror main source: `WooCommerce/src/test/`, `libs/<module>/src/test/`

## Analytics

- Add events to `AnalyticsEvent` enum in `WooCommerce/.../analytics/AnalyticsEvent.kt`. **Exception:** POS uses its own `WooPosAnalyticsEvent`
- Track via injected `AnalyticsTrackerWrapper` — MUST NOT use the `AnalyticsTracker` singleton directly. **Exception:** POS uses its own `WooPosAnalyticsTracker`
- Properties as `Map<String, *>` with key constants from `AnalyticsTracker.KEY_*`
- Enum constants: `UPPER_SNAKE_CASE` (e.g., `PRODUCT_DETAIL_LOADED`)
- Naming token suffixes: `_tapped`, `_loaded`, `_failed`, `_success`, `_selected`, `_toggled`, `_open`
- Use `siteless = true` for events that don't require site context (login, signup)

## Git & PR Conventions

- Main branch: `trunk`
- Feature branches: `issue/ISSUEID-description` (e.g., `issue/1234-fix-order-list`)
- Commit messages: < 100 characters, focus on what was done
- MUST NOT include "Co-Authored-By" in commit messages
- MUST NOT mention that code was generated by AI or any AI tool
- PR template (`.github/PULL_REQUEST_TEMPLATE.md`): Description, Test Steps, Images/gif, RELEASE-NOTES.txt checkbox
- Labels: at least one from Type / Status / Priority / Feature categories

## Operational Rules

- MUST NOT run `./gradlew` commands in the background unless explicitly told to

## Common Pitfalls

- Don't import Android framework classes (Context, View, etc.) in ViewModels
- Don't use `Thread.sleep` in Compose tests — use `waitUntil`
- Don't use `LiveData` for new state in ViewModels — prefer `StateFlow` (existing `LiveData` is fine)
- Don't force unwrap with `!!` — handle nullability properly
- Don't create new XML layouts for Compose screens — use `ComposeView` in Fragment
- Don't use `remember` outside of `@Composable` functions
- Don't pass `ViewModel` instances between composables — pass state and callbacks instead. Exception: a single top-level wrapper composable that accepts the ViewModel, observes its state, and delegates to a stateless overload is acceptable.

## Environment Setup

- Requires `secrets.properties` at `~/.configure/woocommerce-android/secrets/`
- Copy from `defaults.properties` and fill in OAuth2 credentials (see README)
- Java 21 (Amazon Corretto) required for remote build cache
- `local.properties` is auto-generated by Android Studio (contains SDK path)

## Skills Reference

AI agents can use the following skills for common workflows:

| Skill | Triggers |
|-------|----------|
| `review` | "review changes", "check my code", "quality check" |
| `pr` | "create PR", "open PR", "pull request" |
| `verify-on-device` | "verify on emulator", "test on device", "take screenshot" |

## Further Reading

@docs/compose.md
@docs/coding-style.md
@docs/tracking-events.md
@docs/pull-request-guidelines.md
@CONVENTION.md
