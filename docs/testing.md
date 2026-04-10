# Unit Testing

This project has two distinct testing setups — one for the **store management app** and one for **POS**. Use the correct one based on which part of the codebase you're testing.

- File path contains `woopos/` or class name starts with `WooPos` → **POS**
- Everything else → **Store Management**

## Shared Conventions

Both the store app and POS follow these conventions:

### Naming

Backtick-wrapped BDD style:
- `` `given X, when Y, then Z` `` — when preconditions matter
- `` `when X, then Y` `` — when no preconditions needed

Use `// GIVEN`, `// WHEN`, `// THEN` comment sections in the test body. Omit sections that are empty.

### Libraries

- **Assertions:** AssertJ only — **NEVER** use JUnit assertions (`assertEquals`, `assertTrue`)
- **Mocking:** mockito-kotlin

### Mocking Gotchas

- `whenever(mock.method(any(), any()))` does NOT reliably match calls using Kotlin default parameters. Use exact values instead.
- This is critical for suspend functions returning `Result<T>` (inline class).
- Use `onBlocking` in `mock {}` blocks for suspend functions: `onBlocking { fetch() } doReturn result`

### Conventions

- Name private helpers with `given`/`when` prefixes to mirror BDD test names
- Use companion objects for constants and sample data — MUST be at the bottom of the class
- Use existing `*TestUtils` classes when available (e.g., `OrderTestUtils`, `ProductReviewTestUtils`)

---

## Store App Tests

### Test Class Setup

Extend `BaseUnitTest` which provides `InstantTaskExecutorRule`, `CoroutineTestRule` with `UnconfinedTestDispatcher`, and `testBlocking {}`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyFeatureViewModelTest : BaseUnitTest() {
    // mock dependencies at class level
    private val repository: MyRepository = mock {
        onBlocking { fetch() } doReturn Result.success(Unit)
        on { observe() } doReturn flowOf(emptyList())
    }

    private lateinit var viewModel: MyFeatureViewModel

    // Use a setup function with prepareMocks lambda for per-test overrides
    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = MyFeatureViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository
        )
    }

    @Test
    fun `when initialized, then fetch data`() = testBlocking {
        setup()
        verify(repository).fetch()
    }
}
```

When the Fragment uses nav args, convert them to `SavedStateHandle`:
```kotlin
private val savedState = MyFragmentArgs(orderId = ORDER_ID).toSavedStateHandle()
```

When the class under test takes `CoroutineDispatchers`, inject test dispatchers:
```kotlin
MyUseCase(dispatchers = coroutinesTestRule.testDispatchers)
```

Repository tests may skip `BaseUnitTest` and use `runTest` directly.

### Observing State

#### LiveData — captureValues

Use `captureValues()` and `runAndCaptureValues {}` from `com.woocommerce.android.util.LiveDataUtils`:

```kotlin
val states = viewModel.viewState.captureValues()
assertThat(states.last()).isInstanceOf(ViewState.Success::class.java)

val states = viewModel.viewState.runAndCaptureValues {
    viewModel.onRetryClicked()
}
assertThat(states.last().items).isNotEmpty
```

#### Flow — runAndCaptureValues

Use `runAndCaptureValues {}` from `com.woocommerce.android.util.FlowUtils` (note: no `captureValues()` for Flow):

```kotlin
val states = viewModel.uiState.runAndCaptureValues {
    viewModel.onRetryClicked()
}
assertThat(states.last().items).isNotEmpty
```

#### Events (MultiLiveEvent)

```kotlin
val events = viewModel.event.runAndCaptureValues {
    viewModel.onDeleteClicked(item)
}
assertThat(events.last()).isInstanceOf(ShowSnackbar::class.java)
```

#### Time-dependent tests

Use `advanceTimeAndRun(durationMs)` from `com.woocommerce.android.util` (combines `advanceTimeBy` + `runCurrent`).

### Full Example

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ExampleViewModelTest : BaseUnitTest() {
    private val repository: ExampleRepository = mock {
        onBlocking { fetchItems() } doReturn Result.success(Unit)
        on { observeItems() } doReturn flowOf(listOf(SAMPLE_ITEM))
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private lateinit var viewModel: ExampleViewModel

    private suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = ExampleViewModel(
            savedStateHandle = SavedStateHandle(),
            repository = repository,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `when loading, then show items`() = testBlocking {
        setup()
        val viewState = viewModel.viewState.captureValues().last()
        assertThat(viewState.items).isEqualTo(listOf(SAMPLE_ITEM))
    }

    @Test
    fun `when item clicked, then track analytics event`() = testBlocking {
        setup()
        viewModel.onItemClicked(SAMPLE_ITEM)
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.EXAMPLE_ITEM_TAPPED)
    }

    @Test
    fun `when delete fails, then show error snackbar`() = testBlocking {
        setup {
            whenever(repository.deleteItem(any())).thenReturn(Result.failure(Exception()))
        }
        val events = viewModel.event.runAndCaptureValues {
            viewModel.onDeleteClicked(SAMPLE_ITEM)
        }
        assertThat(events.last()).isInstanceOf(ShowSnackbar::class.java)
    }

    private companion object {
        val SAMPLE_ITEM = ExampleItem(id = 1L, name = "Test")
    }
}
```

---

## POS Tests

Do NOT use main-app test patterns (`BaseUnitTest`, `testBlocking`, `captureValues`, `LiveData`) in POS tests.

### Test Class Setup

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

### Mocking the Event Bus

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

### Analytics Verification

Mock `WooPosAnalyticsTracker` (not `AnalyticsTrackerWrapper`):

```kotlin
// data object — use argThat since they are singletons
verify(analyticsTracker).track(argThat { this is WooPosAnalyticsEvent.Event.ActionTapped })

// data class — match by exact values
verify(analyticsTracker).track(
    WooPosAnalyticsEvent.Event.CheckoutTapped(productsInCart = 3, couponsInCart = 1)
)
```

### Timing and Coroutine Control

- Call `advanceUntilIdle()` after creating the ViewModel to let `init` coroutines run
- Use `advanceUntilIdle()` after emitting events to SharedFlows
- When tests need controlled timing, use `StandardTestDispatcher` instead of `UnconfinedTestDispatcher`

---

## Key Differences: Store vs POS

| Aspect | Store | POS |
|--------|-------|-----|
| Base class | `BaseUnitTest` | None |
| Coroutine rule | `CoroutineTestRule` (via BaseUnitTest) | `WooPosCoroutineTestRule` |
| Test scope | `testBlocking` | `runTest` |
| State observation | `captureValues()` / `runAndCaptureValues {}` | `viewModel.state.value` directly |
| Analytics mock | `AnalyticsTrackerWrapper` | `WooPosAnalyticsTracker` |
