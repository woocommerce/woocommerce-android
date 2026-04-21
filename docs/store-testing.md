# Store App — Unit Testing

## Conventions

- Backtick-wrapped BDD naming: `` `given X, when Y, then Z` `` or `` `when X, then Y` ``
- `// GIVEN`, `// WHEN`, `// THEN` comment sections in the test body (omit empty sections)
- AssertJ assertions only — **NEVER** use JUnit assertions (`assertEquals`, `assertTrue`)
- mockito-kotlin for mocking
- Companion objects for test constants — MUST be at the bottom of the class
- Name private helpers with `given`/`when` prefixes to mirror BDD test names
- Use existing `*TestUtils` classes when available (e.g., `OrderTestUtils`, `ProductReviewTestUtils`)

### Mocking Gotchas

- `whenever(mock.method(any(), any()))` does NOT reliably match calls using Kotlin default parameters. Use exact values instead.
- This is critical for suspend functions returning `Result<T>` (inline class).
- Use `onBlocking` in `mock {}` blocks for suspend functions: `onBlocking { fetch() } doReturn result`

## Test Class Setup

Extend `BaseUnitTest` which provides `InstantTaskExecutorRule`, `CoroutineTestRule` with `UnconfinedTestDispatcher`, and `testBlocking {}`:

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class MyFeatureViewModelTest : BaseUnitTest() {
    private val repository: MyRepository = mock {
        onBlocking { fetch() } doReturn Result.success(Unit)
        on { observe() } doReturn flowOf(emptyList())
    }

    private lateinit var viewModel: MyFeatureViewModel

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

## Observing State

### LiveData — captureValues

Use `captureValues()` and `runAndCaptureValues {}` from `com.woocommerce.android.util.LiveDataUtils`:

```kotlin
val states = viewModel.viewState.captureValues()
assertThat(states.last()).isInstanceOf(ViewState.Success::class.java)

val states = viewModel.viewState.runAndCaptureValues {
    viewModel.onRetryClicked()
}
assertThat(states.last().items).isNotEmpty
```

### Flow — runAndCaptureValues

Use `runAndCaptureValues {}` from `com.woocommerce.android.util.FlowUtils` (note: no `captureValues()` for Flow):

```kotlin
val states = viewModel.uiState.runAndCaptureValues {
    viewModel.onRetryClicked()
}
assertThat(states.last().items).isNotEmpty
```

### Events (MultiLiveEvent)

```kotlin
val events = viewModel.event.runAndCaptureValues {
    viewModel.onDeleteClicked(item)
}
assertThat(events.last()).isInstanceOf(ShowSnackbar::class.java)
```

### Time-dependent tests

Use `advanceTimeAndRun(durationMs)` from `com.woocommerce.android.util` (combines `advanceTimeBy` + `runCurrent`).

## Full Example

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
