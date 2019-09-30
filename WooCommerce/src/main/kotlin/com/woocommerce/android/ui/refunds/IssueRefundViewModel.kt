package com.woocommerce.android.ui.refunds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_REFUND_AMOUNT_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.BG_THREAD
import com.woocommerce.android.di.UI_THREAD
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.RefundsStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OpenClassOnDebug
class IssueRefundViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val backgroundDispatcher: CoroutineDispatcher,
    private val refundStore: RefundsStore,
    private val orderStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(mainDispatcher) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
    }

    // region LiveData
    private val _showSnackbarMessage = SingleLiveEvent<String>()
    val showSnackbarMessage: LiveData<String> = _showSnackbarMessage

    private val _showSnackbarMessageWithUndo = SingleLiveEvent<String>()
    val showSnackbarMessageWithUndo: LiveData<String> = _showSnackbarMessageWithUndo

    private val _showValidationError = SingleLiveEvent<String>()
    val showValidationError: LiveData<String> = _showValidationError

    private val _showRefundSummary = SingleLiveEvent<Unit>()
    val showRefundSummary: LiveData<Unit> = _showRefundSummary

    private val _exitAfterRefund = SingleLiveEvent<Boolean>()
    val exitAfterRefund: LiveData<Boolean> = _exitAfterRefund

    private val _isNextButtonEnabled = MutableLiveData<Boolean>()
    val isNextButtonEnabled: LiveData<Boolean> = _isNextButtonEnabled

    private val _isSummaryFormEnabled = MutableLiveData<Boolean>()
    val isSummaryFormEnabled: LiveData<Boolean> = _isSummaryFormEnabled

    private val _availableForRefund = MutableLiveData<String>()
    val availableForRefund: LiveData<String> = _availableForRefund

    private val _screenTitle = MutableLiveData<String>()
    val screenTitle: LiveData<String> = _screenTitle

    private val _formattedRefundAmount = MutableLiveData<String>()
    val formattedRefundAmount: LiveData<String> = _formattedRefundAmount

    private val _previousRefunds = MutableLiveData<String>()
    val previousRefunds: LiveData<String> = _previousRefunds

    private val _currencySettings = MutableLiveData<CurrencySettings>()
    val currencySettings: LiveData<CurrencySettings> = _currencySettings
    // endregion

    final var enteredAmount: BigDecimal = BigDecimal.ZERO
        private set(value) {
            field = value
            val formattedAmount = formatCurrency(enteredAmount)

            _screenTitle.value = resourceProvider.getString(
                    R.string.order_refunds_title_with_amount,
                    formattedAmount
            )
            _formattedRefundAmount.value = formattedAmount
        }

    private lateinit var order: Order
    private lateinit var maxRefund: BigDecimal
    private lateinit var formatCurrency: (BigDecimal) -> String
    private var refundContinuation: Continuation<Boolean>? = null

    fun start(orderId: Long) {
        orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))?.toAppModel()?.let { order ->
            this.order = order
            this.formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)

            maxRefund = order.total - order.refundTotal
            _availableForRefund.value = resourceProvider.getString(
                    R.string.order_refunds_available_for_refund,
                    formatCurrency(maxRefund)
            )
            _previousRefunds.value = formatCurrency(order.refundTotal)

            val decimals = wooStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
            _currencySettings.value = CurrencySettings(order.currency, decimals ?: DEFAULT_DECIMAL_PRECISION)
        }
        enteredAmount = BigDecimal.ZERO
    }

    fun onRefundEntered() {
        if (isInputValid()) {
            AnalyticsTracker.track(ADD_ORDER_REFUND_AMOUNT_NEXT_BUTTON_TAPPED)
            _showRefundSummary.call()
        } else {
            showValidationState()
        }
    }

    fun onManualRefundAmountChanged(amount: BigDecimal) {
        if (enteredAmount != amount) {
            enteredAmount = amount
            showValidationState()
        }
    }

    fun onRefundConfirmed(reason: String) {
        AnalyticsTracker.track(ADD_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED)

        if (networkStatus.isConnected()) {
            _showSnackbarMessageWithUndo.value = resourceProvider.getString(
                    R.string.order_refunds_manual_refund_progress_message,
                    formatCurrency(enteredAmount)
            )

            launch {
                _isSummaryFormEnabled.value = false

                // pause here until the snackbar is dismissed to allow for undo action
                val wasRefundCanceled = waitForCancellation()
                if (!wasRefundCanceled) {
                    // TODO: Update this once the item & automatic refunds are supported
                    AnalyticsTracker.track(Stat.REFUND_CREATE, mapOf(
                            AnalyticsTracker.KEY_ID to order.remoteOrderId,
                            AnalyticsTracker.KEY_REFUND_IS_FULL to (enteredAmount isEqualTo maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_TYPE to "amount",
                            AnalyticsTracker.KEY_REFUND_METHOD to "manual",
                            AnalyticsTracker.KEY_REFUND_AMOUNT to enteredAmount.toString()
                    ))

                    val resultCall = async(backgroundDispatcher) {
                        return@async refundStore.createRefund(
                                selectedSite.get(),
                                order.remoteOrderId,
                                enteredAmount,
                                reason
                        )
                    }
                    val result = resultCall.await()

                    if (result.isError) {
                        AnalyticsTracker.track(Stat.REFUND_CREATE_FAILED, mapOf(
                                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                                AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.toString(),
                                AnalyticsTracker.KEY_ERROR_DESC to result.error.message)
                        )

                        _showSnackbarMessage.value = resourceProvider.getString(
                                R.string.order_refunds_manual_refund_error
                        )
                    } else {
                        AnalyticsTracker.track(Stat.REFUND_CREATE_SUCCESS, mapOf(
                                AnalyticsTracker.KEY_ID to result.model?.id
                        ))

                        _showSnackbarMessage.value = resourceProvider.getString(
                                R.string.order_refunds_manual_refund_successful
                        )
                        _exitAfterRefund.value = !result.isError
                    }
                }
                _isSummaryFormEnabled.value = true
            }
        } else {
            _showSnackbarMessage.value = resourceProvider.getString(R.string.offline_error)
        }
    }

    private suspend fun waitForCancellation(): Boolean {
        val wasRefundCanceled = suspendCoroutine<Boolean> {
            refundContinuation = it
        }
        refundContinuation = null
        return wasRefundCanceled
    }

    fun onUndoTapped() {
        AnalyticsTracker.track(ADD_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED)
        refundContinuation?.resume(true)
    }

    fun onProceedWithRefund() {
        refundContinuation?.resume(false)
    }

    private fun validateInput(): InputValidationState {
        return when {
            enteredAmount > maxRefund -> return TOO_HIGH
            enteredAmount isEqualTo BigDecimal.ZERO -> TOO_LOW
            else -> VALID
        }
    }

    private fun showValidationState() {
        when (validateInput()) {
            TOO_HIGH -> {
                _showValidationError.value = resourceProvider.getString(R.string.order_refunds_refund_high_error)
                _isNextButtonEnabled.value = false
            }
            TOO_LOW -> {
                _showValidationError.value = resourceProvider.getString(R.string.order_refunds_refund_zero_error)
                _isNextButtonEnabled.value = false
            }
            VALID -> {
                _showValidationError.value = null
                _isNextButtonEnabled.value = true
            }
        }
    }

    private fun isInputValid() = validateInput() == VALID

    enum class InputValidationState { TOO_HIGH, TOO_LOW, VALID }

    data class CurrencySettings(val currency: String, val decimals: Int)
}
