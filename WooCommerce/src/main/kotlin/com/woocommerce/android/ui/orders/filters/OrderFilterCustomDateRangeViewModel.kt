package com.woocommerce.android.ui.orders.filters

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.filters.data.OrderFiltersRepository
import com.woocommerce.android.ui.orders.filters.model.OrderFilterEvent.OnDateRangeChanged
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
    private val dateUtils: DateUtils
) : ScopedViewModel(savedState) {
    val viewState = LiveDataDelegate(
        savedState,
        ViewState(
            startDateMillis = System.currentTimeMillis(),
            startDateDisplayValue = "StartTestDate",
            endDateMillis = System.currentTimeMillis(),
            endDateDisplayValue = "EndTestDate"
        )
    )
    private var _viewState by viewState

    fun onShowOrdersClicked() {
        TODO("Not yet implemented")
    }

    fun onBackPressed(): Boolean {
//        saveFiltersSelection()
        triggerEvent(OnDateRangeChanged)
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

    @Parcelize
    data class ViewState(
        val startDateMillis: Long,
        val startDateDisplayValue: String?,
        val endDateMillis: Long,
        val endDateDisplayValue: String?,
    ) : Parcelable

}
