package com.woocommerce.android.ui.orders.wooshippinglabels.refund

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.formatToLocalizedMedium
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelRefundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val repository: WooShippingLabelRepository,
    configDataStore: WooShippingConfigDataStore,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
) : ScopedViewModel(savedState) {
    private val arguments: WooShippingLabelRefundFragmentArgs by savedState.navArgs()

    // Finding the shipping label from cached config data
    private val shippingLabelFlow = configDataStore.observeConfig(arguments.orderId)
        .map { it?.shippingLabelData?.currentOrderLabels?.find { it.labelId == arguments.labelId } }
        .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val isLoadingFlow = MutableStateFlow(false)

    val viewState = combine(shippingLabelFlow, isLoadingFlow) { label, isLoading ->
        if (isLoading) {
            ViewState.Loading
        } else {
            val purchaseDate = label?.createdDate?.let { date -> Date(date) }?.formatToLocalizedMedium()
            val refundableAmount = label?.refundableAmount?.let { amount ->
                label.currency?.let { currency -> currencyFormatter.formatCurrency(amount, currency) }
            }
            ViewState.DataState(purchaseDate, refundableAmount)
        }
    }.asLiveData()

    fun onRefundShippingLabelButtonClicked() {
        if (networkStatus.isConnected()) {
            isLoadingFlow.value = true
            launch {
                repository.refundLabel(selectedSite.get(), arguments.orderId, arguments.labelId)
                    .takeIf { it.isError.not() }?.let {
                        triggerEvent(ShowSnackbar(R.string.shipping_label_refund_success))
                        triggerEvent(ExitWithResult(arguments.labelId))
                    } ?: run {
                    triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_error))
                    isLoadingFlow.value = false
                }
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    fun onBackPressed() {
        if (isLoadingFlow.value) {
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
