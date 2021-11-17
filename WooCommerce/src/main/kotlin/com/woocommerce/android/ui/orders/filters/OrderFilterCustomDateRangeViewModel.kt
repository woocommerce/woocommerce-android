package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnDateRangeChanged
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.dateRangeToDisplayValue
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class OrderFilterCustomDateRangeViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderFilterRepository: OrderFiltersRepository,
    private val getTrackingForFilterSelection: GetTrackingForFilterSelection,
    private val dateUtils: DateUtils
) : ScopedViewModel(savedState) {

    val viewState = LiveDataDelegate(
        savedState,
        ViewState()
    )
    private var _viewState by viewState

    init {
        val selectedDateRangeMillis = orderFilterRepository.getCustomDateRangeFilter()
        val startDateDisplayValue = selectedDateRangeMillis.first?.let {
            dateUtils.toDisplayDateFormat(it)
        } ?: ""
        val endDateDisplayValue = selectedDateRangeMillis.second?.let {
            dateUtils.toDisplayDateFormat(it)
        } ?: ""
        _viewState = _viewState.copy(
            startDateMillis = selectedDateRangeMillis.first,
            startDateDisplayValue = startDateDisplayValue,
            endDateMillis = selectedDateRangeMillis.second,
            endDateDisplayValue = endDateDisplayValue
        )
    }

    fun onShowOrdersClicked() {
        orderFilterRepository.setCustomDateRange(_viewState.startDateMillis, _viewState.endDateMillis)
        trackFilterSelection()
        triggerEvent(OnShowOrders)
    }

    fun onBackPressed(): Boolean {
        orderFilterRepository.setCustomDateRange(_viewState.startDateMillis, _viewState.endDateMillis)
        val dateRangeDisplayValue =
            dateRangeToDisplayValue(
                _viewState.startDateMillis,
                _viewState.endDateMillis,
                dateUtils
            )
        triggerEvent(OnDateRangeChanged(dateRangeDisplayValue))
        return false
    }

    fun onStartDateSelected(startDateMillis: Long) {
        _viewState = _viewState.copy(
            startDateMillis = startDateMillis,
            startDateDisplayValue = dateUtils.toDisplayDateFormat(startDateMillis)
        )
    }

    fun onEndDateSelected(endDateMillis: Long) {
        _viewState = _viewState.copy(
            endDateMillis = endDateMillis,
            endDateDisplayValue = dateUtils.toDisplayDateFormat(endDateMillis)
        )
    }

    private fun trackFilterSelection() {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.ORDERS_LIST_FILTER,
            getTrackingForFilterSelection()
        )
    }

    @Parcelize
    data class ViewState(
        val startDateMillis: Long? = null,
        val startDateDisplayValue: String? = "",
        val endDateMillis: Long? = null,
        val endDateDisplayValue: String? = "",
    ) : Parcelable
}
