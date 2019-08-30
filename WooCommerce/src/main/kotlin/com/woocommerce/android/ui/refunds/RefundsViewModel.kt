package com.woocommerce.android.ui.refunds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named

@OpenClassOnDebug
class RefundsViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val refundStore: RefundsStore,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(mainDispatcher) {
    private val _showSnackbarMessage = SingleLiveEvent<Int>()
    val showSnackbarMessage: LiveData<Int> = _showSnackbarMessage

    private val _exit = SingleLiveEvent<Unit>()
    val exit: LiveData<Unit> = _exit

    private val _showValidationError = SingleLiveEvent<String>()
    val showValidationError: LiveData<String> = _showValidationError

    private val _availableForRefund = MutableLiveData<String>()
    val availableForRefund: LiveData<String> = _availableForRefund

    private val _formattedRefundAmount = MutableLiveData<String>()
    val formattedRefundAmount: LiveData<String> = _formattedRefundAmount

    private val _isNextButtonEnabled = MutableLiveData<Boolean>()
    val isNextButtonEnabled: LiveData<Boolean> = _isNextButtonEnabled

    private val _currencySymbol = MutableLiveData<String>()
    val currencySymbol: LiveData<String> = _currencySymbol

    final var enteredAmount: BigDecimal = BigDecimal.ZERO
        private set

    private lateinit var maxRefund: BigDecimal
    private lateinit var order: Order
    private lateinit var formatCurrency: (BigDecimal) -> String

    fun start(orderId: Long) {
        orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))?.toAppModel()?.let { order ->
            this.formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)

            maxRefund = order.total - order.refundTotal
            _availableForRefund.value = resourceProvider.getString(
                    R.string.order_refunds_available_for_refund,
                    formatCurrency(maxRefund)
            )
            _isNextButtonEnabled.value = false
            _formattedRefundAmount.value = formatCurrency(enteredAmount)
            _currencySymbol.value = order.currency
        }
    }

    fun onCloseClicked() {
        _exit.call()
    }

    fun onNextClicked() {
        if (enteredAmount > maxRefund) {
            _showValidationError.value = resourceProvider.getString(R.string.order_refunds_refund_high_error)
        }
    }

    fun onManualRefundAmountChanged(amount: BigDecimal) {
        enteredAmount = amount
        _formattedRefundAmount.value = formatCurrency(amount)
    }

    override fun onCleared() {
        super.onCleared()
    }
}
