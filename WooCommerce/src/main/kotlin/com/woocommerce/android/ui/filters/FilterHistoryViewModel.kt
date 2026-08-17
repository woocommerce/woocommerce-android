package com.woocommerce.android.ui.filters

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel backing the "Filter History" screen for both the order and product lists.
 * The concrete filter surface is passed as the [FilterHistoryFragmentArgs.filterType] navigation
 * argument, which also drives the analytics `source` property.
 *
 * The screen deals only in [SavedFilter]s (readable label + opaque payload); decoding the
 * payload back into a concrete filter selection is the caller's responsibility (see the order and
 * product filter screens). Applying a filter returns the chosen [SavedFilter] as a nav result under
 * [FILTER_HISTORY_RESULT_KEY].
 */
@HiltViewModel
class FilterHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FilterHistoryRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<FilterHistoryFragmentArgs>()
    private val filterType: FilterHistoryType get() = navArgs.filterType

    private val selectedFilter = MutableStateFlow<SavedFilter?>(null)
    private val showClearHistoryConfirmation = MutableStateFlow(false)

    val viewState = combine(
        repository.observeHistory(filterType),
        selectedFilter,
        showClearHistoryConfirmation
    ) { filters, selected, showConfirmation ->
        ViewState(
            filters = filters,
            selectedFilter = filters.firstOrNull { it.payload == selected?.payload },
            showClearHistoryConfirmation = showConfirmation
        )
    }.toStateFlow(ViewState())

    fun onFilterSelected(filter: SavedFilter) {
        selectedFilter.value = filter
    }

    fun onApplyClicked() {
        val filter = selectedFilter.value ?: return
        track(AnalyticsEvent.FILTER_HISTORY_PAST_FILTER_APPLIED)
        triggerEvent(ExitWithResult(filter))
    }

    fun onCancelClicked() {
        triggerEvent(Exit)
    }

    fun onDeleteFilter(filter: SavedFilter) {
        launch {
            repository.remove(filterType, filter)
            if (selectedFilter.value?.payload == filter.payload) {
                selectedFilter.value = null
            }
            track(AnalyticsEvent.FILTER_HISTORY_PAST_FILTER_REMOVED)
        }
    }

    fun onClearHistoryClicked() {
        showClearHistoryConfirmation.value = true
    }

    fun onClearHistoryDismissed() {
        showClearHistoryConfirmation.value = false
    }

    fun onClearHistoryConfirmed() {
        launch {
            repository.clear(filterType)
            selectedFilter.value = null
            showClearHistoryConfirmation.value = false
            track(AnalyticsEvent.FILTER_HISTORY_CLEARED)
        }
    }

    private fun track(event: AnalyticsEvent) {
        analyticsTrackerWrapper.track(event, mapOf(AnalyticsTracker.KEY_SOURCE to analyticsSource))
    }

    private val analyticsSource: String
        get() = when (filterType) {
            FilterHistoryType.ORDERS -> AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_ORDERS
            FilterHistoryType.PRODUCTS -> AnalyticsTracker.VALUE_FILTER_HISTORY_SOURCE_PRODUCTS
        }

    data class ViewState(
        val filters: List<SavedFilter> = emptyList(),
        val selectedFilter: SavedFilter? = null,
        val showClearHistoryConfirmation: Boolean = false
    ) {
        val isEmpty: Boolean get() = filters.isEmpty()
        val isApplyEnabled: Boolean get() = selectedFilter != null
    }

    companion object {
        const val FILTER_HISTORY_RESULT_KEY = "filter_history_result"
    }
}
