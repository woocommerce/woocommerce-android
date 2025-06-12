package com.woocommerce.android.ui.orders.wooshippinglabels.refund

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToLocalizedMedium
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelRefundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
) : ScopedViewModel(savedState) {
    private val arguments: WooShippingLabelRefundFragmentArgs by savedState.navArgs()

    private val _viewState: MutableStateFlow<ViewState> = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState.Loading
    )
    val viewState = _viewState.asLiveData()

    init {
        loadDataState()
    }

    private fun loadDataState() {
        _viewState.update { viewState ->
            val currency = arguments.shipment.items.firstOrNull()?.currency
                ?: return@update viewState
            ViewState.DataState(
                purchaseDate = arguments.shipment.purchaseDate?.formatToLocalizedMedium(),
                refundableAmount = currencyFormatter.formatCurrency(
                    arguments.shipment.refundableAmount,
                    currency
                )
            )
        }
    }

    fun onRefundShippingLabelButtonClicked() {
        if (networkStatus.isConnected()) {
            _viewState.update { ViewState.Loading }
            launch {
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    fun onBackPressed() {
        if (_viewState.value is ViewState.Loading) {
            triggerEvent(ShowSnackbar(R.string.order_refunds_refund_in_progress))
        } else {
            triggerEvent(Exit)
        }
    }

    @Parcelize
    sealed class ViewState : Parcelable {
        data object Loading : ViewState()
        data class DataState(
            val purchaseDate: String? = null,
            val refundableAmount: String? = null
        ) : ViewState()
    }
}
