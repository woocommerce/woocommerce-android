package com.woocommerce.android.ui.payments.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STATE_OFF
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STATE_ON
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SimplePaymentsViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val simplePaymentsRepository: SimplePaymentsRepository,
    private val networkStatus: NetworkStatus,
    private val orderCreateEditRepository: OrderCreateEditRepository
) : ScopedViewModel(savedState) {
    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal var viewState by viewStateLiveData

    private val navArgs: SimplePaymentsFragmentArgs by savedState.navArgs()

    private val order: Order
        get() = navArgs.order

    val orderDraft
        get() = order.copy(
            total = viewState.orderTotal,
            customerNote = viewState.customerNote
        )

    // accessing feesLines[0] should be safe to do since a fee line is passed by FluxC when creating the order, but we
    // check for an empty list here to simplify our test. note the single fee line is the only way to get the price w/o
    // taxes, and FluxC sets the tax status to "taxable" so when the order is created core automatically sets the total
    // tax if the store has taxes enabled.
    private val feeLineTotal: BigDecimal
        get() = if (order.feesLines.isNotEmpty()) {
            order.feesLines[0].total
        } else {
            BigDecimal.ZERO
        }

    init {
        val hasTaxes = order.totalTax > BigDecimal.ZERO
        updateViewState(hasTaxes)
    }

    private fun updateViewState(chargeTaxes: Boolean) {
        viewState = if (chargeTaxes) {
            viewState.copy(
                chargeTaxes = true,
                orderSubtotal = feeLineTotal,
                orderTaxes = order.taxLines,
                orderTotal = order.total,
                customerNote = order.customerNote,
            )
        } else {
            viewState.copy(
                chargeTaxes = false,
                orderSubtotal = feeLineTotal,
                orderTotal = feeLineTotal,
                customerNote = order.customerNote,
            )
        }
    }

    fun onChargeTaxesChanged(chargeTaxes: Boolean) {
        val properties = if (chargeTaxes) {
            mapOf(KEY_STATE to VALUE_STATE_ON)
        } else {
            mapOf(KEY_STATE to VALUE_STATE_OFF)
        }
        AnalyticsTracker.track(AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_TAXES_TOGGLED, properties)
        updateViewState(chargeTaxes = chargeTaxes)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(ShowCustomerNoteEditor)
    }

    fun onCustomerNoteChanged(customerNote: String) {
        AnalyticsTracker.track(AnalyticsEvent.SIMPLE_PAYMENTS_FLOW_NOTE_ADDED)
        viewState = viewState.copy(customerNote = customerNote)
    }

    fun onBillingEmailChanged(email: String) {
        viewState = viewState.copy(billingEmail = email.trim())
    }

    fun onDoneButtonClicked() {
        if (!networkStatus.isConnected()) {
            AnalyticsTracker.track(
                AnalyticsEvent.PAYMENTS_FLOW_FAILED,
                mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW),
            )
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
            return
        }

        val isEmailValid = viewState.billingEmail.isEmpty() || StringUtils.isValidEmail(viewState.billingEmail)
        if (!isEmailValid) {
            viewState = viewState.copy(isBillingEmailValid = false)
            return
        }

        viewState = viewState.copy(isBillingEmailValid = true)

        launch {
            viewState = viewState.copy(isLoading = true)
            simplePaymentsRepository.updateSimplePayment(
                order.id,
                order.feesTotal.toString(),
                viewState.customerNote,
                viewState.billingEmail,
                viewState.chargeTaxes
            ).collectUpdate()
        }
    }

    private suspend fun Flow<UpdateOrderResult>.collectUpdate() {
        collect { result ->
            if (result.event.isError) {
                recordUpdateSimplePaymentError(result)
            }

            if (result is UpdateOrderResult.RemoteUpdateResult) {
                viewState = viewState.copy(isLoading = false)
                if (result.event.isError) {
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.simple_payments_update_error))
                } else {
                    triggerEvent(ShowPaymentMethodSelectionScreen)
                }
            }
        }
    }

    fun deleteDraftOrder(order: Order) {
        launch(Dispatchers.IO) {
            orderCreateEditRepository.deleteDraftOrder(order)
        }
    }

    fun onBackButtonClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.PAYMENTS_FLOW_CANCELED,
            mapOf(AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW)
        )
        triggerEvent(CancelSimplePayment)
    }

    private fun recordUpdateSimplePaymentError(result: UpdateOrderResult) {
        AnalyticsTracker.track(
            AnalyticsEvent.PAYMENTS_FLOW_FAILED,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_SOURCE_SUMMARY,
                AnalyticsTracker.KEY_FLOW to AnalyticsTracker.VALUE_SIMPLE_PAYMENTS_FLOW,
            )
        )
        result.event.error?.let {
            WooLog.e(WooLog.T.ORDERS, "Simple payment update failed with ${it.message}")
        }
    }

    @Parcelize
    data class ViewState(
        val chargeTaxes: Boolean = false,
        val orderSubtotal: BigDecimal = BigDecimal.ZERO,
        val orderTaxes: List<Order.TaxLine> = emptyList(),
        val orderTotal: BigDecimal = BigDecimal.ZERO,
        val customerNote: String = "",
        val billingEmail: String = "",
        val isBillingEmailValid: Boolean = true,
        val isLoading: Boolean = false,
    ) : Parcelable

    object ShowCustomerNoteEditor : MultiLiveEvent.Event()
    object ShowPaymentMethodSelectionScreen : MultiLiveEvent.Event()
    object CancelSimplePayment : MultiLiveEvent.Event()
}
