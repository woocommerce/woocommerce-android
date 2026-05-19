package com.woocommerce.android.ui.payments.refunds

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_NOTE_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_NOTE_ADD_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.REFUND_CREATE
import com.woocommerce.android.analytics.AnalyticsEvent.REFUND_CREATE_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.REFUND_CREATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isCashPayment
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.PaymentMethodType
import com.woocommerce.android.ui.payments.toCardBrandDisplayName
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class RefundSummaryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val orderDetailRepository: OrderDetailRepository,
    private val paymentChargeRepository: PaymentChargeRepository,
    private val resourceProvider: ResourceProvider,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val gatewayStore: WCGatewayStore,
    private val refundStore: WCRefundStore,
    private val coroutineDispatchers: CoroutineDispatchers
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val REFUND_METHOD_MANUAL = "manual"
    }

    private val navArgs: RefundSummaryFragmentArgs by savedStateHandle.navArgs()

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val refundSummaryStateLiveData = LiveDataDelegate(savedState, RefundSummaryViewState())
    private var refundSummaryState by refundSummaryStateLiveData

    private val order: Deferred<Order> by lazy {
        viewModelScope.async {
            requireNotNull(orderDetailRepository.getOrderById(navArgs.orderId))
        }
    }

    private val gateway: Deferred<PaymentGateway> by lazy {
        viewModelScope.async {
            loadPaymentGateway(order.await())
        }
    }

    private var cardType = PaymentMethodType.CARD_PRESENT
    private var refundJob: Job? = null
    val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    init {
        launch {
            val order = order.await()
            gateway.await() // Ensure gateway is initialized
            initRefundSummaryState(order)
        }
    }

    fun onRefundIssued(reason: String) = launch {
        val order = order.await()
        gateway.await() // Ensure gateway is initialized
        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id
            )
        )

        refundSummaryState = refundSummaryState.copy(
            refundReason = reason
        )

        triggerEvent(
            ShowRefundConfirmation(
                resourceProvider.getString(
                    R.string.order_refunds_title_with_amount,
                    formatCurrency(refundSummaryState.refundAmount!!, order.currency)
                ),
                resourceProvider.getString(R.string.order_refunds_confirmation),
                resourceProvider.getString(R.string.order_refunds_refund)
            )
        )
    }

    fun onRefundConfirmed(wasConfirmed: Boolean) {
        if (wasConfirmed) {
            if (networkStatus.isConnected()) {
                refundJob = launch {
                    yield() // ensure the navigation component completes navigation before proceeding
                    val order = order.await()
                    refundSummaryState = refundSummaryState.copy(
                        isFormEnabled = false
                    )
                    if (isInteracRefund()) {
                        triggerEvent(NavigateToCardReaderScreen(order.id, refundSummaryState.refundAmount!!))
                    } else {
                        val formattedAmount = formatCurrency(refundSummaryState.refundAmount!!, order.currency)
                        triggerEvent(
                            ShowSnackbar(
                                R.string.order_refunds_amount_refund_progress_message,
                                arrayOf(formattedAmount)
                            )
                        )
                        refund()
                    }

                    analyticsTrackerWrapper.track(
                        REFUND_CREATE,
                        mapOf(
                            AnalyticsTracker.KEY_ORDER_ID to order.id,
                            AnalyticsTracker.KEY_REFUND_IS_FULL to
                                (refundSummaryState.refundAmount isEqualTo order.maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_METHOD to gateway.await().methodTitle,
                            AnalyticsTracker.KEY_AMOUNT to refundSummaryState.refundAmount.toString()
                        )
                    )
                    refundSummaryState = refundSummaryState.copy(isFormEnabled = true)
                }
            } else {
                triggerEvent(ShowSnackbar(R.string.offline_error))
            }
        }
    }

    /**
     * Checks if the refund summary button label should be enabled. If the max length for the text field is
     * surpassed, the button should be disabled until the text is brought within the maximum length.
     */
    fun onRefundSummaryTextChanged(maxLength: Int, currLength: Int) {
        refundSummaryState = refundSummaryState.copy(isSummaryTextTooLong = currLength > maxLength)
    }

    private suspend fun initRefundSummaryState(order: Order) {
        if (refundSummaryStateLiveData.hasInitialValue) {
            val refundAmount = navArgs.refundAmount.toBigDecimal()
            refundSummaryState = refundSummaryState.copy(
                refundAmount = refundAmount,
                refundAmountFormatted = formatCurrency(refundAmount, order.currency),
                previouslyRefunded = formatCurrency(order.refundTotal, order.currency),
                isFormEnabled = true
            )

            val paymentGateway = gateway.await()
            val manualRefundMethod = resourceProvider.getString(R.string.order_refunds_manual_refund)
            if (!order.paymentMethod.isCashPayment && (!paymentGateway.isEnabled || !paymentGateway.supportsRefunds)) {
                val paymentTitle = if (paymentGateway.title.isNotBlank()) {
                    resourceProvider.getString(R.string.order_refunds_method, manualRefundMethod, paymentGateway.title)
                } else {
                    manualRefundMethod
                }
                updateRefundSummaryState(paymentTitle, isMethodDescriptionVisible = true)
            } else {
                enrichRefundMethodWithCardDetails(order, paymentGateway.title.ifBlank { paymentGateway.methodTitle })
            }
        }
    }

    private fun enrichRefundMethodWithCardDetails(order: Order, refundMethod: String) {
        val chargeId = order.chargeId
        if (chargeId != null) {
            loadCardDetails(chargeId, refundMethod)
        } else {
            updateRefundSummaryState(refundMethod, isMethodDescriptionVisible = false)
        }
    }

    private fun loadCardDetails(chargeId: String, refundMethod: String) {
        launch {
            refundSummaryState = refundSummaryState.copy(isFetchingCardData = true)
            val result = paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)
            refundSummaryState = refundSummaryState.copy(isFetchingCardData = false)
            when (result) {
                is PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success -> {
                    cardType = PaymentMethodType.fromValue(result.paymentMethodType ?: "card_present")
                    val refundMethodWithCard = result.run {
                        val brand = result.cardBrand.toCardBrandDisplayName()
                        val last4 = result.cardLast4.orEmpty()
                        val creditCardRefundDefaultText =
                            resourceProvider.getString(R.string.order_refunds_credit_card_refund)
                        "${refundMethod.ifBlank { creditCardRefundDefaultText }} ($brand **** $last4)"
                    }
                    updateRefundSummaryState(refundMethodWithCard, isMethodDescriptionVisible = false)
                }

                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error -> {
                    cardType = PaymentMethodType.CARD_PRESENT
                    updateRefundSummaryState(refundMethod, isMethodDescriptionVisible = false)
                }
            }
        }
    }

    private fun updateRefundSummaryState(refundMethod: String, isMethodDescriptionVisible: Boolean) {
        refundSummaryState = refundSummaryState.copy(
            refundMethod = refundMethod,
            isMethodDescriptionVisible = isMethodDescriptionVisible
        )
    }

    private suspend fun loadPaymentGateway(order: Order): PaymentGateway = withContext(coroutineDispatchers.io) {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        return@withContext if (paymentGateway != null && paymentGateway.isEnabled) {
            paymentGateway
        } else {
            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    /* This method does the actual refund in case of non-interac refund. In case of Interac refund, the actual
   refund happens on the client-side and this method updates the WCPay backend about the refund success status and
   does not process the refund itself.

   For non-Interac refund -> Process the refund (Entire refund logic lives in the backend)
   For Interac refund -> Update the backend of the successful refund. The actual refund happens on the client-side */
    fun refund() {
        triggerUIMessageIfRefundIsInterac()
        launch {
            val order = order.await()
            val result = initiateRefund(order)
            if (result.isError) {
                trackRefundError(order, result)
                triggerUIMessage()
            } else {
                trackRefundSuccess(order, result)
                updateRefundSummaryStateWithOrderNote(order)
                triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_successful))
                triggerEvent(Exit)
            }
        }
    }

    private fun triggerUIMessageIfRefundIsInterac() {
        if (isInteracRefund()) {
            triggerEvent(ShowSnackbar(R.string.card_reader_interac_refund_notifying_backend_about_successful_refund))
        }
    }

    private suspend fun initiateRefund(order: Order): WooResult<WCRefundModel> {
        return refundStore.createItemsRefund(
            site = selectedSite.get(),
            orderId = order.id,
            amount = refundSummaryState.refundAmount,
            reason = refundSummaryState.refundReason ?: "",
            restockItems = true,
            autoRefund = gateway.await().supportsRefunds,
            items = navArgs.refundItems.map { it.toDataModel() }
        )
    }

    private fun trackRefundError(order: Order, result: WooResult<WCRefundModel>) {
        analyticsTrackerWrapper.track(
            REFUND_CREATE_FAILED,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id,
                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.toString(),
                AnalyticsTracker.KEY_ERROR_DESC to result.error.message
            )
        )
    }

    private fun triggerUIMessage() {
        if (isInteracRefund()) {
            triggerEvent(
                ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund_failed
                )
            )
        } else {
            triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_error))
        }
    }

    private fun trackRefundSuccess(order: Order, result: WooResult<WCRefundModel>) {
        analyticsTrackerWrapper.track(
            REFUND_CREATE_SUCCESS,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id,
                AnalyticsTracker.KEY_ID to result.model?.id
            )
        )
    }

    private suspend fun updateRefundSummaryStateWithOrderNote(order: Order) {
        refundSummaryState.refundReason?.let { reason ->
            if (reason.isNotBlank()) {
                addOrderNote(order, reason)
            }
        }
    }

    private suspend fun addOrderNote(order: Order, reason: String) {
        val note = OrderNote(note = reason, isCustomerNote = false)
        orderDetailRepository.addOrderNote(order.id, note).fold(
            onSuccess = {
                analyticsTrackerWrapper.track(ORDER_NOTE_ADD_SUCCESS)
            },
            onFailure = {
                analyticsTrackerWrapper.track(
                    ORDER_NOTE_ADD_FAILED,
                    prepareTracksEventsDetails(it as WooException)
                )
            }
        )
    }

    private fun formatCurrency(amount: BigDecimal, currency: String): String {
        return currencyFormatter.buildBigDecimalFormatter(currency)(amount)
    }

    private fun prepareTracksEventsDetails(exception: WooException) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to exception.error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to exception.error.message
    )

    private fun isInteracRefund() = cardType == PaymentMethodType.INTERAC_PRESENT

    @Parcelize
    data class RefundSummaryViewState(
        val isFormEnabled: Boolean? = null,
        val previouslyRefunded: String? = null,
        val refundAmount: BigDecimal? = null,
        val refundAmountFormatted: String? = null,
        val refundMethod: String? = null,
        val refundReason: String? = null,
        val isMethodDescriptionVisible: Boolean? = null,
        val isSummaryTextTooLong: Boolean = false,
        val isFetchingCardData: Boolean = false,
    ) : Parcelable {
        val isSubmitButtonEnabled: Boolean
            get() = !isSummaryTextTooLong && !isFetchingCardData
    }

    data class NavigateToCardReaderScreen(val orderId: Long, val refundAmount: BigDecimal) : IssueRefundEvent()
}
