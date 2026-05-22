package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.orders.WooPosLoadPaymentGateway
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import javax.inject.Inject

interface WooPosRefundSubmissionProcessor {
    fun submit(request: WooPosRefundSubmissionRequest): Flow<WooPosRefundSubmissionState>
}

data class WooPosRefundSubmissionRequest(
    val order: Order,
    val refundAmount: BigDecimal,
    val refundReason: String,
    val refundItems: List<RefundRequestItem>,
) {
    val orderId: Long = order.id
}

sealed class WooPosRefundSubmissionState {
    data object Processing : WooPosRefundSubmissionState()
    data object Success : WooPosRefundSubmissionState()
    data class Failure(val message: String) : WooPosRefundSubmissionState()
}

class WooPosRefundSubmissionProcessorImpl @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val loadPaymentGateway: WooPosLoadPaymentGateway,
    private val resourceProvider: ResourceProvider,
) : WooPosRefundSubmissionProcessor {
    @Suppress("TooGenericExceptionCaught")
    override fun submit(request: WooPosRefundSubmissionRequest): Flow<WooPosRefundSubmissionState> = flow {
        try {
            WooLog.i(
                WooLog.T.POS,
                "WooPosRefund: submission started " +
                    "orderId=${request.orderId}, " +
                    "amount=${request.refundAmount}, " +
                    "itemCount=${request.refundItems.size}"
            )

            emit(WooPosRefundSubmissionState.Processing)

            val paymentGatewayResult = loadPaymentGateway(request.order)
            if (paymentGatewayResult.isFailure) {
                WooLog.e(
                    WooLog.T.POS,
                    "WooPosRefund: failed to load payment gateway orderId=${request.orderId}",
                    paymentGatewayResult.exceptionOrNull()
                )
                emit(
                    WooPosRefundSubmissionState.Failure(
                        message = resourceProvider.getString(R.string.woopos_refund_error_gateway_not_found),
                    )
                )
                return@flow
            }
            val paymentGateway = paymentGatewayResult.getOrThrow()

            val result = refundStore.createItemsRefund(
                site = selectedSite.get(),
                orderId = request.orderId,
                amount = request.refundAmount,
                reason = request.refundReason,
                restockItems = true,
                autoRefund = paymentGateway.supportsRefunds,
                items = request.refundItems
            )

            if (result.isError) {
                emit(
                    WooPosRefundSubmissionState.Failure(
                        message = result.error.message ?: resourceProvider.getString(R.string.error_generic),
                    )
                )
            } else {
                emit(WooPosRefundSubmissionState.Success)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            WooLog.e(
                WooLog.T.POS,
                "WooPosRefund: submission failed unexpectedly orderId=${request.orderId}",
                exception
            )
            emit(
                WooPosRefundSubmissionState.Failure(
                    message = resourceProvider.getString(R.string.error_generic),
                )
            )
        }
    }
}
