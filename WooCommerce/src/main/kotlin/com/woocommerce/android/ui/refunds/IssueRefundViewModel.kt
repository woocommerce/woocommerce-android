package com.woocommerce.android.ui.refunds

import android.os.Parcelable
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
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ExitAfterRefund
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.HideValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowSnackbar
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowValidationError
import com.woocommerce.android.ui.refunds.RefundProductListAdapter.RefundListItem
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
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

const val ORDER_ID_KEY = "ORDER_ID_KEY"

@OpenClassOnDebug
class IssueRefundViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateHandle,
    dispatchers: CoroutineDispatchers,
    private val orderStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val resourceProvider: ResourceProvider,
    private val noteRepository: OrderNoteRepository,
    private val gatewayStore: WCGatewayStore,
    private val refundStore: WCRefundStore
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val REFUND_TYPE_AMOUNT = "amount"
        private const val REFUND_TYPE_ITEMS = "items"
        private const val REFUND_METHOD_MANUAL = "manual"
    }

    final val commonStateLiveData = LiveDataDelegate(savedState, CommonViewState())
    final val refundSummaryStateLiveData = LiveDataDelegate(savedState, RefundSummaryViewState())
    final val refundByItemsStateLiveData = LiveDataDelegate(savedState, RefundByItemsViewState())
    final val refundByAmountStateLiveData = LiveDataDelegate(savedState, RefundByAmountViewState()) {
        updateScreenTitle()
    }

    private var commonState by commonStateLiveData
    private var refundByAmountState by refundByAmountStateLiveData
    private var refundByItemsState by refundByItemsStateLiveData
    private var refundSummaryState by refundSummaryStateLiveData

    private lateinit var order: Order
    private lateinit var maxRefund: BigDecimal
    private lateinit var formatCurrency: (BigDecimal) -> String
    private lateinit var gateway: PaymentGateway

    private var refundContinuation: Continuation<Boolean>? = null

    init {
        savedState.get<Long>(ORDER_ID_KEY)?.let { orderId ->
            initialize(orderId)
        }
    }

    fun initialize(orderId: Long) {
        if (this.isNotInitialized()) {
            val order = loadOrder(orderId)
            if (order != null) {
                this.order = order
                this.formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
                this.maxRefund = order.total - order.refundTotal
                this.gateway = loadPaymentGateway()

                updateRefundByAmountState(order)
                updateRefundByItemsState(order)
                updateRefundSummaryState()

                savedState[ORDER_ID_KEY] = orderId
            }
        } else {
            // If the ViewModel is already initialized it means it's being reused and we need to reset it.
            // This logic can be removed once the scope is properly attached to the nav graph
            val order = loadOrder(orderId)
            if (order != null) {
                this.order = order
                maxRefund = order.total - order.refundTotal

                resetLiveData()
                updateRefundByAmountState(order)
                updateRefundByItemsState(order)
                updateRefundSummaryState()
            }
        }
    }

    // Temporary workaround for the activity ViewModel scope
    private fun resetLiveData() {
        resetEvent()
        commonStateLiveData.reset()
        refundByAmountStateLiveData.reset()
        refundSummaryStateLiveData.reset()
        refundByItemsStateLiveData.reset()
    }

    private fun isNotInitialized(): Boolean = !this::order.isInitialized

    private fun loadOrder(orderId: Long): Order? =
            orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))?.toAppModel()

    private fun updateScreenTitle() {
        commonState = commonState.copy(
                screenTitle = resourceProvider.getString(
                        string.order_refunds_title_with_amount, formatCurrency(refundByAmountState.enteredAmount)
                )
        )
    }

    private fun updateRefundByAmountState(order: Order) {
        val decimals = wooStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
                ?: DEFAULT_DECIMAL_PRECISION

        refundByAmountState = refundByAmountState.copy(
                currency = order.currency,
                decimals = decimals,
                availableForRefund = resourceProvider.getString(R.string.order_refunds_available_for_refund,
                        formatCurrency(maxRefund))
        )
    }

    private fun updateRefundByItemsState(order: Order) {
        refundByItemsState = refundByItemsState.copy(
                currency = order.currency,
                items = order.items.map { RefundListItem(it) }
        )
    }

    private fun updateRefundSummaryState() {
        val manualRefundMethod = resourceProvider.getString(R.string.order_refunds_manual_refund)
        val paymentTitle: String
        val isManualRefund: Boolean
        if (gateway.isEnabled) {
            paymentTitle = if (gateway.supportsRefunds) gateway.title else "$manualRefundMethod via ${gateway.title}"
            isManualRefund = !gateway.supportsRefunds
        } else {
            paymentTitle = gateway.title
            isManualRefund = true
        }

        refundSummaryState = refundSummaryState.copy(
                refundMethod = paymentTitle,
                isMethodDescriptionVisible = isManualRefund
        )
    }

    private fun loadPaymentGateway(): PaymentGateway {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        return if (paymentGateway != null && paymentGateway.isEnabled) {
            paymentGateway
        } else {
            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    fun onNextButtonTapped() {
        if (isInputValid()) {
            refundSummaryStateLiveData.reset()
            refundSummaryState = refundSummaryState.copy(
                    isFormEnabled = true,
                    previouslyRefunded = formatCurrency(order.refundTotal),
                    refundAmount = formatCurrency(refundByAmountState.enteredAmount)
            )

            AnalyticsTracker.track(
                    CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED, mapOf(
                    AnalyticsTracker.KEY_REFUND_TYPE to REFUND_TYPE_AMOUNT,
                    AnalyticsTracker.KEY_ORDER_ID to order.remoteId
            ))

            triggerEvent(ShowRefundSummary)
        } else {
            showValidationState()
        }
    }

    fun onManualRefundAmountChanged(amount: BigDecimal) {
        if (refundByAmountState.enteredAmount != amount) {
            refundByAmountState = refundByAmountState.copy(enteredAmount = amount)
            showValidationState()
        }
    }

    fun onRefundConfirmed(reason: String) {
        AnalyticsTracker.track(CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED, mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.remoteId
        ))

        if (networkStatus.isConnected()) {
            triggerEvent(
                    ShowSnackbar(
                            resourceProvider.getString(
                                    R.string.order_refunds_amount_refund_progress_message,
                                    formatCurrency(refundByAmountState.enteredAmount)
                            ),
                            undoAction = {
                                AnalyticsTracker.track(
                                        CREATE_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED,
                                        mapOf(AnalyticsTracker.KEY_ORDER_ID to order.remoteId)
                                )
                                refundContinuation?.resume(true)
                            })
            )

            launch {
                refundSummaryState = refundSummaryState.copy(
                        isFormEnabled = false,
                        refundReason = reason
                )

                // pause here until the snackbar is dismissed to allow for undo action
                val wasRefundCanceled = waitForCancellation()
                if (!wasRefundCanceled) {
                    AnalyticsTracker.track(Stat.REFUND_CREATE, mapOf(
                            AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                            AnalyticsTracker.KEY_REFUND_IS_FULL to
                                    (refundByAmountState.enteredAmount isEqualTo maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_TYPE to REFUND_TYPE_AMOUNT,
                            AnalyticsTracker.KEY_REFUND_METHOD to gateway.methodTitle,
                            AnalyticsTracker.KEY_REFUND_AMOUNT to
                                    refundByAmountState.enteredAmount.toString()
                    ))

                    val resultCall = async(dispatchers.io) {
                        return@async refundStore.createRefund(
                                selectedSite.get(),
                                order.remoteId,
                                refundByAmountState.enteredAmount,
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

                        triggerEvent(
                                ShowSnackbar(
                                        resourceProvider.getString(R.string.order_refunds_amount_refund_error)
                                )
                        )
                    } else {
                        AnalyticsTracker.track(Stat.REFUND_CREATE_SUCCESS, mapOf(
                                AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                                AnalyticsTracker.KEY_ID to result.model?.id
                        ))

                        if (reason.isNotBlank()) {
                            noteRepository.createOrderNote(order.identifier, reason, true)
                        }

                        triggerEvent(
                                ShowSnackbar(
                                        resourceProvider.getString(R.string.order_refunds_amount_refund_successful)
                                )
                        )
                        triggerEvent(ExitAfterRefund)
                    }
                }
                refundSummaryState = refundSummaryState.copy(isFormEnabled = true)
            }
        } else {
            triggerEvent(ShowSnackbar(resourceProvider.getString(R.string.offline_error)))
        }
    }

    fun onProceedWithRefund() {
        refundContinuation?.resume(false)
    }

    fun onRefundQuantityTapped(productId: Long) {
        val refundItem = refundByItemsState.items?.firstOrNull { it.product.productId == productId }
        if (refundItem != null) {
            triggerEvent(ShowNumberPicker(refundItem))
        }
    }

    fun onRefundQuantityChanged(productId: Long, quantity: Int) {
        refundByItemsState = refundByItemsState.copy(items = refundByItemsState.items?.toMutableList()?.apply {
            val index = this.indexOfFirst { it.product.productId == productId }
            this[index] = this[index].copy(quantity = quantity)
        })
    }

    fun onSelectAllButtonTapped() {
        refundByItemsState.items?.forEach {
            onRefundQuantityChanged(it.product.productId, it.product.quantity.toInt())
        }
    }

    private suspend fun waitForCancellation(): Boolean {
        val wasRefundCanceled = suspendCoroutine<Boolean> {
            refundContinuation = it
        }
        refundContinuation = null
        return wasRefundCanceled
    }

    private fun validateInput(): InputValidationState {
        return when {
            refundByAmountState.enteredAmount > maxRefund -> return TOO_HIGH
            refundByAmountState.enteredAmount isEqualTo BigDecimal.ZERO -> TOO_LOW
            else -> VALID
        }
    }

    private fun showValidationState() {
        commonState = when (validateInput()) {
            TOO_HIGH -> {
                triggerEvent(ShowValidationError(resourceProvider.getString(R.string.order_refunds_refund_high_error)))
                commonState.copy(isNextButtonEnabled = false)
            }
            TOO_LOW -> {
                triggerEvent(ShowValidationError(resourceProvider.getString(R.string.order_refunds_refund_zero_error)))
                commonState.copy(isNextButtonEnabled = false)
            }
            VALID -> {
                triggerEvent(HideValidationError)
                commonState.copy(isNextButtonEnabled = true)
            }
        }
    }

    private fun isInputValid() = validateInput() == VALID

    private enum class InputValidationState {
        TOO_HIGH,
        TOO_LOW,
        VALID
    }

    @Parcelize
    data class RefundByAmountViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION,
        val availableForRefund: String? = null,
        val enteredAmount: BigDecimal = BigDecimal.ZERO
    ) : Parcelable

    @Parcelize
    data class RefundByItemsViewState(
        val currency: String? = null,
        val items: List<RefundListItem>? = null
    ) : Parcelable

    @Parcelize
    data class RefundSummaryViewState(
        val isFormEnabled: Boolean? = null,
        val previouslyRefunded: String? = null,
        val refundAmount: String? = null,
        val refundMethod: String? = null,
        val refundReason: String? = null,
        val isMethodDescriptionVisible: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class CommonViewState(
        val isNextButtonEnabled: Boolean? = null,
        val screenTitle: String? = null
    ) : Parcelable

    sealed class IssueRefundEvent : Event() {
        data class ShowSnackbar(val message: String, val undoAction: (() -> Unit)? = null) : IssueRefundEvent()
        data class ShowValidationError(val message: String) : IssueRefundEvent()
        data class ShowNumberPicker(val refundItem: RefundListItem) : IssueRefundEvent()
        object HideValidationError : IssueRefundEvent()
        object ShowRefundSummary : IssueRefundEvent()
        object ExitAfterRefund : IssueRefundEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<IssueRefundViewModel>
}
