# POS — Unit Testing

Do NOT use main-app test patterns (`BaseUnitTest`, `testBlocking`, `captureValues`, `LiveData`) in POS tests.

## Conventions

- Backtick-wrapped BDD naming: `` `given X, when Y, then Z` `` or `` `when X, then Y` ``
- `// GIVEN`, `// WHEN`, `// THEN` comment sections in the test body (omit empty sections)
- AssertJ assertions only — **NEVER** use JUnit assertions (`assertEquals`, `assertTrue`)
- mockito-kotlin for mocking
- Companion objects for test constants — MUST be at the bottom of the class

### Mocking Gotchas

- `whenever(mock.method(any(), any()))` does NOT reliably match calls using Kotlin default parameters. Use exact values instead.
- This is critical for suspend functions returning `Result<T>` (inline class).

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
        // GIVEN
        whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onUIEvent(WooPosMyFeatureUIEvent.ActionClicked)

        // THEN
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    private fun createViewModel() = WooPosMyFeatureViewModel(
        parentToChildrenEventReceiver = parentToChildrenEventReceiver,
        childrenToParentEventSender = childrenToParentEventSender,
        analyticsTracker = analyticsTracker,
    )
}
```

## Mocking the Event Bus

**Default (no events):** `whenever(parentToChildrenEventReceiver.events).thenReturn(flowOf())`

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
// data object — use argThat since they are singletons
verify(analyticsTracker).track(argThat { this is WooPosAnalyticsEvent.Event.ActionTapped })

// data class — match by exact values
verify(analyticsTracker).track(
    WooPosAnalyticsEvent.Event.CheckoutTapped(productsInCart = 3, couponsInCart = 1)
)
```

## Timing and Coroutine Control

- Call `advanceUntilIdle()` after creating the ViewModel to let `init` coroutines run
- Use `advanceUntilIdle()` after emitting events to SharedFlows
- When tests need controlled timing, use `StandardTestDispatcher` instead of `UnconfinedTestDispatcher`

## Test File Location

Tests: `WooCommerce/src/test/kotlin/com/woocommerce/android/ui/woopos/`
