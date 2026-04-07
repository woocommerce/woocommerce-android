---
name: pos-tests
description: POS unit testing patterns (WooPosCoroutineTestRule, runTest, advanceUntilIdle, mockito-kotlin, event bus mocking, analytics verification). Use when writing, editing, exploring, debugging, or reviewing unit tests for POS (WooPos*) code. NOT for main store app tests — use the `store-tests` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# POS Unit Tests

Do NOT use main-app test patterns (`BaseUnitTest`, `testBlocking`, `captureValues`, `LiveData`). POS tests have their own conventions.

## Workflow

1. Read the class under test to understand its dependencies and state management
2. Find existing tests in the same package for patterns to follow
3. Test location: mirror main source path under `src/test/` within `ui/woopos/`
4. Write the test class following the conventions below
5. Run the test: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.ClassName"`

## Test Class Setup

Use `WooPosCoroutineTestRule` and `runTest` — do NOT extend `BaseUnitTest`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class WooPosMyFeatureViewModelTest {
    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    @Test
    fun `when action clicked, then state updates`() = runTest {
        whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onUIEvent(WooPosMyFeatureUIEvent.ActionClicked)

        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    private fun createViewModel() = WooPosMyFeatureViewModel(
        parentToChildrenEventReceiver = parentToChildrenEventReceiver,
        childrenToParentEventSender = childrenToParentEventSender,
        analyticsTracker = analyticsTracker,
    )
}
```

## Key Differences from Store Tests

| Aspect | POS | Store |
|--------|-----|-------|
| Base class | None | `BaseUnitTest` |
| Coroutine rule | `WooPosCoroutineTestRule` | `CoroutineTestRule` (via BaseUnitTest) |
| Test scope | `runTest` | `testBlocking` |
| Default dispatcher | `UnconfinedTestDispatcher` | `UnconfinedTestDispatcher` |
| State observation | `viewModel.state.value` directly | `captureValues()` / `runAndCaptureValues {}` |
| Assertions | AssertJ | AssertJ |
| Mocking | mockito-kotlin | mockito-kotlin |

## Mocking the Event Bus

**Default (no events):** Return `flowOf()` for the receiver:
```kotlin
whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())
```

**Testing event handling:** Use `MutableSharedFlow` to emit events during the test:
```kotlin
val parentEvents = MutableSharedFlow<ParentToChildrenEvent>()
whenever(parentToChildrenEventReceiver.events).thenReturn(parentEvents)

val viewModel = createViewModel()
advanceUntilIdle()

parentEvents.emit(ParentToChildrenEvent.OrderSuccessfullyPaid)
advanceUntilIdle()

assertThat(viewModel.state.value).isInstanceOf(WooPosMyViewState.Success::class.java)
```

**Verifying events sent to parent:**
```kotlin
verify(childrenToParentEventSender).sendToParent(ChildToParentEvent.CheckoutClicked)
```

## Analytics Verification

Mock `WooPosAnalyticsTracker` (not `AnalyticsTrackerWrapper`):

```kotlin
// Simple event (data object)
verify(analyticsTracker).track(argThat { this is WooPosAnalyticsEvent.Event.ActionTapped })

// Event with properties (data class) — match by type or exact values
verify(analyticsTracker).track(
    WooPosAnalyticsEvent.Event.CheckoutTapped(productsInCart = 3, couponsInCart = 1)
)
```

Use `argThat { this is EventType }` for `data object` events since they are singletons.

## Timing and Coroutine Control

- Call `advanceUntilIdle()` after creating the ViewModel to let `init` coroutines run
- Use `advanceUntilIdle()` after emitting events to SharedFlows
- When tests need controlled timing, use `StandardTestDispatcher` instead of the default `UnconfinedTestDispatcher`
- With `StandardTestDispatcher`, use `advanceTimeBy(n)` to start coroutines before emitting to SharedFlows

## Naming

Same BDD backtick style as the store app:
- `` `given X, when Y, then Z` `` — when preconditions matter
- `` `when X, then Y` `` — when no preconditions needed

Use `// GIVEN`, `// WHEN`, `// THEN` comment sections in the test body.

## Assertions and Mocking

- **Assertions:** AssertJ only — `assertThat(...).isEqualTo(...)`, `.isTrue()`, `.isInstanceOf(...)`
- **Mocking:** mockito-kotlin — `mock()`, `whenever(...).thenReturn(...)`, `verify(...)`, `argThat { ... }`
- NEVER use JUnit assertions (`assertEquals`, `assertTrue`)

## Mocking Gotchas

- `whenever(mock.method(any(), any()))` does NOT reliably match calls using Kotlin default parameters. Use exact values instead.
- This is critical for suspend functions returning `Result<T>` (inline class).

## Test Data

- Use companion objects for constants and sample data
- Companion objects MUST be at the bottom of the class

## File Location

Tests: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/`
