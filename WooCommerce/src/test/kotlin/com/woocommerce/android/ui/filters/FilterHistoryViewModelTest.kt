package com.woocommerce.android.ui.filters

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FilterHistoryViewModelTest : BaseUnitTest() {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val repository: FilterHistoryRepository = mock {
        on { observeHistory(any()) } doReturn flowOf(FILTERS)
    }

    private lateinit var viewModel: FilterHistoryViewModel

    private fun setup(
        filterType: FilterHistoryType = FilterHistoryType.ORDERS,
        prepareMocks: () -> Unit = {}
    ) {
        prepareMocks()
        viewModel = FilterHistoryViewModel(
            savedStateHandle = FilterHistoryFragmentArgs(filterType).toSavedStateHandle(),
            repository = repository,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `given saved filters, when observed, then they are exposed newest-first`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues { }.last()

        assertThat(state.filters).isEqualTo(FILTERS)
        assertThat(state.isEmpty).isFalse()
    }

    @Test
    fun `given no saved filters, when observed, then state is empty`() = testBlocking {
        setup {
            whenever(repository.observeHistory(any())).thenReturn(flowOf(emptyList()))
        }

        val state = viewModel.viewState.runAndCaptureValues { }.last()

        assertThat(state.isEmpty).isTrue()
        assertThat(state.isApplyEnabled).isFalse()
    }

    @Test
    fun `when a filter is selected, then it becomes selected and apply is enabled`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onFilterSelected(FILTER_2)
        }.last()

        assertThat(state.selectedFilter).isEqualTo(FILTER_2)
        assertThat(state.isApplyEnabled).isTrue()
    }

    @Test
    fun `given a selection, when apply is clicked, then applied is tracked and selection is returned`() = testBlocking {
        setup()
        viewModel.onFilterSelected(FILTER_2)

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onApplyClicked()
        }

        assertThat(events.last()).isInstanceOf(ExitWithResult::class.java)
        assertThat((events.last() as ExitWithResult<*>).data).isEqualTo(FILTER_2)
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FILTER_HISTORY_PAST_FILTER_APPLIED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_ORDERS)
        )
    }

    @Test
    fun `given products source, when apply is clicked, then source is products`() = testBlocking {
        setup(filterType = FilterHistoryType.PRODUCTS)
        viewModel.onFilterSelected(FILTER_1)

        viewModel.onApplyClicked()

        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FILTER_HISTORY_PAST_FILTER_APPLIED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_PRODUCTS)
        )
    }

    @Test
    fun `given no selection, when apply is clicked, then nothing is tracked`() = testBlocking {
        setup()

        viewModel.onApplyClicked()

        verify(analyticsTrackerWrapper, never()).track(any(), any())
    }

    @Test
    fun `when a filter is deleted, then it is removed and removed is tracked`() = testBlocking {
        setup()

        viewModel.onDeleteFilter(FILTER_1)

        verify(repository).remove(FilterHistoryType.ORDERS, FILTER_1)
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FILTER_HISTORY_PAST_FILTER_REMOVED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_ORDERS)
        )
    }

    @Test
    fun `when clear history is clicked, then the confirmation is shown`() = testBlocking {
        setup()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onClearHistoryClicked()
        }.last()

        assertThat(state.showClearHistoryConfirmation).isTrue()
    }

    @Test
    fun `when clear history is confirmed, then history is cleared and cleared is tracked`() = testBlocking {
        setup()

        viewModel.onClearHistoryConfirmed()

        verify(repository).clear(FilterHistoryType.ORDERS)
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.FILTER_HISTORY_CLEARED,
            mapOf(AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_ORDERS)
        )
    }

    @Test
    fun `when clear history is dismissed, then the confirmation is hidden`() = testBlocking {
        setup()
        viewModel.onClearHistoryClicked()

        val state = viewModel.viewState.runAndCaptureValues {
            viewModel.onClearHistoryDismissed()
        }.last()

        assertThat(state.showClearHistoryConfirmation).isFalse()
    }

    @Test
    fun `when cancel is clicked, then the screen exits`() = testBlocking {
        setup()

        val events = viewModel.event.runAndCaptureValues {
            viewModel.onCancelClicked()
        }

        assertThat(events.last()).isEqualTo(Exit)
    }

    private companion object {
        val FILTER_1 = SavedFilter(readableString = "Processing", payload = "status=processing")
        val FILTER_2 = SavedFilter(readableString = "Completed", payload = "status=completed")
        val FILTERS = listOf(FILTER_1, FILTER_2)
    }
}
