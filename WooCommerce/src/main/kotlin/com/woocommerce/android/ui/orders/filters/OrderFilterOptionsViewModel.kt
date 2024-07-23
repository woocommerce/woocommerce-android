package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.orders.filters.data.DateRange
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.CUSTOMER
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.DATE_RANGE
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.ORDER_STATUS
import com.woocommerce.android.ui.orders.filters.data.OrderListFilterCategory.PRODUCT
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnFilterOptionsSelectionUpdated
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.ShowCustomDateRangePicker
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel
import com.woocommerce.android.ui.orders.filters.model.OrderFilterOptionUiModel.Companion.DEFAULT_ALL_KEY
import com.woocommerce.android.ui.orders.filters.model.clearAllFilterSelections
import com.woocommerce.android.ui.orders.filters.model.isAnyFilterOptionSelected
import com.woocommerce.android.ui.orders.filters.model.markOptionAllIfNothingSelected
import com.woocommerce.android.ui.orders.filters.model.toDisplayDateRange
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderFilterOptionsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val orderFilterRepository: OrderFiltersRepository,
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection,
    private val dateUtils: DateUtils
) : ScopedViewModel(savedState) {
    private val arguments: OrderFilterOptionsFragmentArgs by savedState.navArgs()
    private val categoryKey = arguments.filterCategory.categoryKey

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewState = LiveDataDelegate(
        savedState,
        ViewState(
            arguments.filterCategory.orderFilterOptions,
            getOrderFilterOptionsTitle(arguments.filterCategory.categoryKey)
        )
    )
    private var _viewState by viewState

    fun onFilterOptionSelected(selectedOrderFilterOption: OrderFilterOptionUiModel) {
        when (categoryKey) {
            ORDER_STATUS -> updateOrderStatusSelectedFilters(selectedOrderFilterOption)
            DATE_RANGE -> updateDateRangeFilters(selectedOrderFilterOption)
            PRODUCT -> error("Product filter option is not supported")
            CUSTOMER -> error("Customer filter not supported in this screen")
        }
    }

    fun onShowOrdersClicked() {
        saveFiltersSelection()
        trackFilterSelection()
        triggerEvent(OnShowOrders)
    }

    fun onBackPressed(): Boolean {
        saveFiltersSelection()
        val updatedCategory = arguments.filterCategory.copy(
            orderFilterOptions = _viewState.filterOptions
        )
        triggerEvent(OnFilterOptionsSelectionUpdated(updatedCategory))
        return false
    }

    private fun trackFilterSelection() {
        if (_viewState.filterOptions.isAnyFilterOptionSelected()) {
            AnalyticsTracker.track(
                AnalyticsEvent.ORDERS_LIST_FILTER,
                getTrackingForFilterSelection()
            )
        }
    }

    private fun saveFiltersSelection() {
        val newSelectedFilters = _viewState.filterOptions
            .filter { it.isSelected && it.key != DEFAULT_ALL_KEY }
            .map { it.key }
        orderFilterRepository.setSelectedFilters(categoryKey, newSelectedFilters)
    }

    private fun updateOrderStatusSelectedFilters(orderStatusClicked: OrderFilterOptionUiModel) {
        when (orderStatusClicked.key) {
            DEFAULT_ALL_KEY -> _viewState = _viewState.copy(
                filterOptions = _viewState.filterOptions.clearAllFilterSelections()
            )
            else -> uncheckFilterOptionAll()
        }
        updateSelectedFilterValues(orderStatusClicked)
    }

    private fun updateDateRangeFilters(dateRangeOptionClicked: OrderFilterOptionUiModel) {
        if (DateRange.fromValue(dateRangeOptionClicked.key) == DateRange.CUSTOM_RANGE) {
            val selectedCustomDateRange = orderFilterRepository.getCustomDateRangeFilter()
            triggerEvent(
                ShowCustomDateRangePicker(selectedCustomDateRange.first, selectedCustomDateRange.second)
            )
        }
        _viewState = _viewState.copy(
            filterOptions = _viewState.filterOptions.clearAllFilterSelections()
        )
        updateSelectedFilterValues(dateRangeOptionClicked)
    }

    private fun updateSelectedFilterValues(selectedFilterOption: OrderFilterOptionUiModel) {
        _viewState.filterOptions.let { filterOptions ->
            var updatedOptionList = filterOptions.map {
                when (selectedFilterOption.key) {
                    it.key -> updateFilterOptionsSelectedValue(categoryKey, selectedFilterOption)
                    else -> it
                }
            }
            updatedOptionList = updatedOptionList.markOptionAllIfNothingSelected()
            _viewState = _viewState.copy(filterOptions = updatedOptionList)
        }
    }

    private fun updateFilterOptionsSelectedValue(
        category: OrderListFilterCategory,
        filterOptionClicked: OrderFilterOptionUiModel
    ) = if (filterOptionClicked.key == DEFAULT_ALL_KEY || category == DATE_RANGE) {
        filterOptionClicked.copy(isSelected = true)
    } else {
        filterOptionClicked.copy(isSelected = !filterOptionClicked.isSelected)
    }

    private fun uncheckFilterOptionAll() {
        _viewState = _viewState.copy(
            filterOptions = _viewState.filterOptions
                .map {
                    when (it.key) {
                        DEFAULT_ALL_KEY -> it.copy(isSelected = false)
                        else -> it
                    }
                }
        )
    }

    private fun getOrderFilterOptionsTitle(categoryKey: OrderListFilterCategory) =
        when (categoryKey) {
            ORDER_STATUS -> resourceProvider.getString(string.orderfilters_filter_order_status_options_title)
            DATE_RANGE -> resourceProvider.getString(R.string.orderfilters_filter_date_range_options_title)
            PRODUCT -> error("Product filter option is not supported")
            CUSTOMER -> error("Customer filter not supported in this screen")
        }

    fun onCustomDateRangeChanged(startMillis: Long, endMillis: Long) {
        orderFilterRepository.setCustomDateRange(startMillis, endMillis)
        val dateRangeDisplayValue = toDisplayDateRange(startMillis, endMillis, dateUtils)
        _viewState = _viewState.copy(
            filterOptions = _viewState.filterOptions
                .map {
                    if (it.isSelected && DateRange.fromValue(it.key) == DateRange.CUSTOM_RANGE) {
                        it.copy(displayValue = dateRangeDisplayValue)
                    } else {
                        it
                    }
                }
        )
    }

    @Parcelize
    data class ViewState(
        val filterOptions: List<OrderFilterOptionUiModel>,
        val title: String
    ) : Parcelable
}
