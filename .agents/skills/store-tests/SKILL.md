---
name: store-tests
description: Main app unit testing patterns (BaseUnitTest, BDD naming, mockito-kotlin, AssertJ, StateFlow/LiveData capture, coroutine dispatchers). Use when writing, editing, exploring, debugging, or reviewing unit tests in the store management app. For POS (WooPos*) tests, use the `pos` skill instead.
allowed-tools: Bash, Read, Write, Edit, Grep, Glob
user-invocable: true
---

# Store App Unit Tests

## Workflow

1. Read the class under test to understand its dependencies, public API, and state management
2. Find existing tests in the same package/feature for patterns to follow
3. Determine test location: mirror main source path under `src/test/`
4. Write the test class following the conventions below
5. Run the test: `./gradlew :WooCommerce:testWasabiDebugUnitTest --tests "*.ClassName"`
   - For libs: `./gradlew :libs:<module>:testDebugUnitTest --tests "*.ClassName"`

## Test Class Setup

### Main App (ViewModels, Repositories, Use Cases)

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

### Repository Tests (no ViewModel involved)

Simple repository tests may skip `BaseUnitTest` and use `runTest` directly.

## Naming

Backtick-wrapped BDD style:
- `` `given X, when Y, then Z` `` -- when preconditions matter
- `` `when X, then Y` `` -- when no preconditions needed

## Test Body

Use `// GIVEN`, `// WHEN`, `// THEN` comment sections. Omit sections that are empty (e.g., omit `// GIVEN` if setup handles everything):

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

## Assertions and Mocking

- **Assertions:** AssertJ only -- `assertThat(...).isEqualTo(...)`, `.isTrue()`, `.isInstanceOf(...)`, `.isNotEmpty`, `.doesNotContain(...)`
- **Mocking:** mockito-kotlin -- `mock()`, `whenever(...).thenReturn(...)`, `verify(...)`, `doReturn(...).whenever(...)`, `doSuspendableAnswer { ... }`
- NEVER use JUnit assertions (`assertEquals`, `assertTrue`)

### Mocking Gotchas

- `whenever(mock.method(any(), any()))` does NOT reliably match calls using Kotlin default parameters. Use exact values instead.
- This is critical for suspend functions returning `Result<T>` (inline class).
- Use `onBlocking` in mock {} blocks for suspend functions: `onBlocking { fetch() } doReturn result`

## Observing State in Tests

### LiveData -- captureValues

Use `captureValues()` and `runAndCaptureValues {}` from `com.woocommerce.android.util.LiveDataUtils`:

```kotlin
// Capture all emitted LiveData values
val states = viewModel.viewState.captureValues()
assertThat(states.last()).isInstanceOf(ViewState.Success::class.java)

// Capture LiveData values while performing an action
val states = viewModel.viewState.runAndCaptureValues {
    viewModel.onRetryClicked()
}
assertThat(states.last().items).isNotEmpty
```

### Flow -- runAndCaptureValues

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

## Helper Methods

Name private helpers with `given`/`when` prefixes to mirror BDD test names:

```kotlin
private suspend fun givenFetchReturns(result: Result<Unit>) {
    whenever(repository.fetch()).thenReturn(result)
}
private fun whenViewModelIsCreated() {
    viewModel = MyViewModel(repository, savedState)
}
```

## What to Test

- **ViewModels:** State changes after actions, events triggered (`ShowSnackbar`, `Exit`, navigation), analytics tracking, error handling
- **Repositories:** Data mapping from FluxC/network to domain models, error propagation
- **Use Cases:** Business logic, edge cases

## Test Data

- Use companion objects for constants and sample data
- Companion objects MUST be at the bottom of the class
- Use existing `*TestUtils` classes when available (e.g., `OrderTestUtils`, `ProductReviewTestUtils`)

## Full Examples

See [references/examples.md](references/examples.md) for complete test class examples covering ViewModel, Repository, POS, and nav args patterns.
