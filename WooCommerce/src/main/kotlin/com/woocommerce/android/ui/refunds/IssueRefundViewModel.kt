package com.woocommerce.android.ui.refunds

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.ui.orders.notes.OrderNoteRepository
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.SingleLiveEvent
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OpenClassOnDebug
class IssueRefundViewModel @AssistedInject constructor(
    dispatchers: CoroutineDispatchers,
    private val refundStore: WCRefundStore,
    private val orderStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider,
    private val noteRepository: OrderNoteRepository,
    private val gatewayStore: WCGatewayStore,
    @Assisted private val savedState: SavedStateHandle
) : ScopedViewModel(dispatchers) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val REFUND_TYPE_AMOUNT = "amount"
        private const val REFUND_TYPE_ITEMS = "items"
        private const val REFUND_METHOD_MANUAL = "manual"
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

    private val _refundMethod = MutableLiveData<String>()
    val refundMethod: LiveData<String> = _refundMethod

    private val _isManualRefundDescriptionVisible = MutableLiveData<Boolean>()
    val isManualRefundDescriptionVisible: LiveData<Boolean> = _isManualRefundDescriptionVisible

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
    private lateinit var gateway: PaymentGateway

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

        initializePaymentGateway()
    }

    fun resetEvents() {
        _showSnackbarMessage.reset()
        _showSnackbarMessageWithUndo.reset()
        _showValidationError.reset()
        _showRefundSummary.reset()
        _exitAfterRefund.reset()
    }

    private fun initializePaymentGateway() {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        val manualPaymentTitle = resourceProvider.getString(string.order_refunds_manual_refund)

        gateway = if (paymentGateway != null && paymentGateway.isEnabled) {
            val paymentTitle = if (paymentGateway.supportsRefunds)
                paymentGateway.title
            else
                "$manualPaymentTitle via ${paymentGateway.title}"

            _refundMethod.value = paymentTitle
            _isManualRefundDescriptionVisible.value = !paymentGateway.supportsRefunds

            paymentGateway
        } else {
            _refundMethod.value = manualPaymentTitle
            _isManualRefundDescriptionVisible.value = true

            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    fun onRefundEntered() {
        if (isInputValid()) {
            AnalyticsTracker.track(
                    CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED, mapOf(
                    AnalyticsTracker.KEY_REFUND_TYPE to REFUND_TYPE_AMOUNT,
                    AnalyticsTracker.KEY_ORDER_ID to order.remoteId
            ))
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
        AnalyticsTracker.track(CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED, mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.remoteId
        ))

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
                    AnalyticsTracker.track(Stat.REFUND_CREATE, mapOf(
                            AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                            AnalyticsTracker.KEY_REFUND_IS_FULL to (enteredAmount isEqualTo maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_TYPE to REFUND_TYPE_AMOUNT,
                            AnalyticsTracker.KEY_REFUND_METHOD to gateway.methodTitle,
                            AnalyticsTracker.KEY_REFUND_AMOUNT to enteredAmount.toString()
                    ))

                    val resultCall = async(dispatchers.main) {
                        return@async refundStore.createRefund(
                                selectedSite.get(),
                                order.remoteId,
                                enteredAmount,
                                reason,
                                gateway.supportsRefunds
                        )
                    }

                    val result = resultCall.await()
                    if (result.isError) {
                        AnalyticsTracker.track(Stat.REFUND_CREATE_FAILED, mapOf(
                                AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                                AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.toString(),
                                AnalyticsTracker.KEY_ERROR_DESC to result.error.message)
                        )

                        _showSnackbarMessage.value = resourceProvider.getString(
                                R.string.order_refunds_manual_refund_error
                        )
                    } else {
                        AnalyticsTracker.track(Stat.REFUND_CREATE_SUCCESS, mapOf(
                                AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                                AnalyticsTracker.KEY_ID to result.model?.id
                        ))

                        if (reason.isNotBlank()) {
                            noteRepository.createOrderNote(order.identifier, reason, true)
                        }

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
        AnalyticsTracker.track(CREATE_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED, mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.remoteId
        ))
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

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<IssueRefundViewModel>
}
