package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.domain.GetTrackingForFilterSelection
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnDateRangeChanged
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnShowOrders
import com.woocommerce.android.ui.orders.filters.model.toDisplayDateRange
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
        ViewState(
            startDateMillis = 0,
            startDateDisplayValue = "",
            endDateMillis = 0,
            endDateDisplayValue = ""
        )
    )
    private var _viewState by viewState

    init {
        val selectedDateRangeMillis = orderFilterRepository.getCustomDateRangeFilter()

        _viewState = _viewState.copy(
            startDateMillis = selectedDateRangeMillis.first,
            startDateDisplayValue = selectedDateRangeMillis.first.toDisplayDate(),
            endDateMillis = selectedDateRangeMillis.second,
            endDateDisplayValue = selectedDateRangeMillis.second.toDisplayDate()
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
            toDisplayDateRange(
                _viewState.startDateMillis,
                _viewState.endDateMillis,
                dateUtils
            )
        triggerEvent(OnDateRangeChanged(dateRangeDisplayValue))
        return false
    }

    fun onDateRangeSelected(startMillis: Long, endMillis: Long) {
        _viewState = _viewState.copy(
            startDateMillis = startMillis,
            startDateDisplayValue = dateUtils.toDisplayDate(startMillis),
            endDateMillis = endMillis,
            endDateDisplayValue = dateUtils.toDisplayDate(endMillis)
        )
    }

    private fun Long.toDisplayDate() =
        when {
            this > 0 -> dateUtils.toDisplayDate(this)
            else -> ""
        }

    private fun trackFilterSelection() {
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.ORDERS_LIST_FILTER,
            getTrackingForFilterSelection()
        )
    }

    @Parcelize
    data class ViewState(
        val startDateMillis: Long,
        val startDateDisplayValue: String?,
        val endDateMillis: Long,
        val endDateDisplayValue: String?,
    ) : Parcelable
}
