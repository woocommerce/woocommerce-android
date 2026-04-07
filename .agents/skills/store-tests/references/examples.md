# Test Examples

## ViewModel Test (StateFlow + LiveData patterns)

```kotlin
package com.woocommerce.android.ui.example

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

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
        // GIVEN
        setup()

        // WHEN
        val viewState = viewModel.viewState.captureValues().last()

        // THEN
        assertThat(viewState.items).isEqualTo(listOf(SAMPLE_ITEM))
    }

    @Test
    fun `given fetch fails, when retrying, then reload items`() = testBlocking {
        // GIVEN
        setup {
            whenever(repository.fetchItems())
                .thenReturn(Result.failure(Exception()))
                .thenReturn(Result.success(Unit))
        }

        // WHEN
        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onRetryClicked()
        }.last()

        // THEN
        assertThat(viewState.items).isNotEmpty
    }

    @Test
    fun `when item clicked, then track analytics event`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.onItemClicked(SAMPLE_ITEM)

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.EXAMPLE_ITEM_TAPPED)
    }

    @Test
    fun `when delete fails, then show error snackbar`() = testBlocking {
        // GIVEN
        setup {
            whenever(repository.deleteItem(any())).thenReturn(Result.failure(Exception()))
        }

        // WHEN
        val events = viewModel.event.runAndCaptureValues {
            viewModel.onDeleteClicked(SAMPLE_ITEM)
        }

        // THEN
        assertThat(events.last()).isInstanceOf(ShowSnackbar::class.java)
    }

    private companion object {
        val SAMPLE_ITEM = ExampleItem(id = 1L, name = "Test")
    }
}
```

## ViewModel Test with Nav Args

```kotlin
private val savedState = ExampleFragmentArgs(orderId = ORDER_ID).toSavedStateHandle()

private fun createViewModel() = ExampleViewModel(
    repository = repository,
    savedStateHandle = savedState
)
```

## Repository Test (no BaseUnitTest needed)

```kotlin
class ExampleRepositoryTest {
    private val store: ExampleStore = mock()
    private val selectedSite: SelectedSite = mock {
        on(it.get()).thenReturn(SiteModel())
    }
    private val repository = ExampleRepository(store, selectedSite)

    @Test
    fun `when fetch succeeds, then return mapped items`() = runTest {
        // GIVEN
        whenever(store.fetchItems(any())).thenReturn(WooResult(model = listOf(RAW_ITEM)))

        // WHEN
        val result = repository.fetchItems()

        // THEN
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(listOf(EXPECTED_ITEM))
    }
}
```

## Testing with CoroutineDispatchers Injection

When the class under test takes `CoroutineDispatchers`:

```kotlin
class ExampleViewModelTest : BaseUnitTest() {
    private fun createViewModel() = ExampleViewModel(
        dispatchers = coroutinesTestRule.testDispatchers,
        // ...
    )
}
```

## Helper Method Naming Convention

Use `given`/`when` prefixes for private setup helpers to mirror BDD test names:

```kotlin
private suspend fun givenFetchReturns(result: Result<Unit>) {
    whenever(repository.fetch()).thenReturn(result)
}

private fun givenNetworkIsConnected(connected: Boolean = true) {
    doReturn(connected).whenever(networkStatus).isConnected()
}

private fun whenViewModelIsCreated() {
    viewModel = MyViewModel(repository, savedState)
}
```
