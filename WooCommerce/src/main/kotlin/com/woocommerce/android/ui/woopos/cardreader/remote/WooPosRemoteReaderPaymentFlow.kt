package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.di.PointOfSaleMode
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.cardreader.payment.TerminalPaymentIntentConfig
import com.woocommerce.android.ui.payments.cardreader.payment.TerminalPaymentPreparationResolver
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosRemoteReaderPaymentFlow @Inject constructor(
    private val cardReaderStore: CardReaderStore,
    private val remoteReaderSession: WooPosRemoteReaderSession,
    private val selectedSite: SelectedSite,
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val wooStore: WooCommerceStore,
    private val resourceProvider: ResourceProvider,
    private val logger: WooPosLogWrapper,
    @PointOfSaleMode private val paymentsFlowTracker: PaymentsFlowTracker,
    private val appPrefs: AppPrefs = AppPrefs,
) {
    private val terminalPaymentPreparationResolver = TerminalPaymentPreparationResolver(wooStore, appPrefs)

    suspend fun collect(order: Order, onCaptureStarting: suspend () -> Unit = {}): Result {
        val result = collectInternal(order, onCaptureStarting)
        when (result) {
            Result.Completed -> paymentsFlowTracker.trackPaymentSucceeded()
            is Result.Failed -> paymentsFlowTracker.trackPaymentFailed(result.message)
        }
        return result
    }

    private suspend fun collectInternal(order: Order, onCaptureStarting: suspend () -> Unit): Result {
        if (isSimulatedReaderConnected()) {
            return simulatePayment(onCaptureStarting)
        }

        val site = selectedSite.get()
        val countryCode = wooStore.getStoreCountryCode(site) ?: run {
            logger.e("Remote payment aborted: store country code unavailable")
            return Result.Failed(genericFailureMessage())
        }

        val paymentInfo = PaymentInfo(
            paymentDescription = cardReaderPaymentOrderHelper.getPaymentDescription(order),
            statementDescriptor = StatementDescriptor(
                appPrefs.getCardReaderStatementDescriptor(
                    localSiteId = site.id,
                    remoteSiteId = site.siteId,
                    selfHostedSiteId = site.selfHostedSiteId,
                )
            ),
            orderId = order.id,
            amount = order.total,
            currency = order.currency,
            orderKey = order.orderKey,
            customerEmail = order.billingAddress.email.ifEmpty { null },
            isPluginCanSendReceipt = paymentReceiptHelper.isPluginCanSendReceipt(site),
            customerName = "${order.billingAddress.firstName} ${order.billingAddress.lastName}"
                .trim()
                .ifBlank { null },
            storeName = site.name.ifEmpty { null },
            siteUrl = site.url.ifEmpty { null },
            countryCode = countryCode,
            feeAmount = TerminalPaymentIntentConfig.calculateApplicationFeeInCents(
                countryCode = countryCode,
                orderTotal = order.total,
                currencyCode = order.currency,
            ),
            channel = PaymentInfo.PaymentChannel.Pos,
            cardPresentCaptureMethod = TerminalPaymentIntentConfig.cardPresentCaptureMethod(countryCode),
            terminalPaymentPreparation = terminalPaymentPreparationResolver.resolve(
                countryCode = countryCode,
                site = site,
                onRouteCheckFailed = ::logTerminalPaymentPreparationRouteCheckFailed,
            ),
        )

        return collectRemotePayment(order.id, paymentInfo, onCaptureStarting)
    }

    private suspend fun collectRemotePayment(
        orderId: Long,
        paymentInfo: PaymentInfo,
        onCaptureStarting: suspend () -> Unit,
    ): Result {
        return when (val outcome = remoteReaderSession.sendCollectPayment(paymentInfo)) {
            is CollectPaymentOutcome.Success -> {
                onCaptureStarting()
                capture(orderId, outcome.paymentIntentId)
            }
            is CollectPaymentOutcome.Rejected -> {
                logger.e("Remote payment rejected: ${outcome.code} - ${outcome.description}")
                Result.Failed(genericFailureMessage())
            }
            CollectPaymentOutcome.TimedOut -> {
                logger.e("Remote payment timed out waiting for phone reader")
                Result.Failed(genericFailureMessage())
            }
            is CollectPaymentOutcome.Failed -> {
                logger.e("Remote payment failed - ${outcome.cause.message}", outcome.cause)
                Result.Failed(mapFailureToUserMessage(outcome.cause))
            }
        }
    }

    private fun mapFailureToUserMessage(cause: Throwable): String {
        val message = cause.message.orEmpty()
        return if (cause is IllegalStateException && message.contains(CONNECTION_LOST_MARKER, ignoreCase = true)) {
            resourceProvider.getString(R.string.woopos_remote_payment_failed_connection_lost)
        } else {
            genericFailureMessage()
        }
    }

    private fun isSimulatedReaderConnected(): Boolean =
        (remoteReaderSession.state.value as? WooPosRemoteReaderSession.State.Connected)?.reader?.isSimulated == true

    private fun genericFailureMessage(): String =
        resourceProvider.getString(R.string.woopos_remote_payment_failed_generic)

    private fun logTerminalPaymentPreparationRouteCheckFailed() {
        logger.d("Skipping terminal payment preparation because the WCPay endpoint could not be verified.")
    }

    private suspend fun capture(orderId: Long, paymentIntentId: String): Result =
        when (val response = cardReaderStore.capturePaymentIntent(orderId, paymentIntentId)) {
            is CapturePaymentResponse.Successful -> Result.Completed
            is CapturePaymentResponse.Error -> Result.Failed(response.message)
        }

    private suspend fun simulatePayment(onCaptureStarting: suspend () -> Unit): Result {
        onCaptureStarting()
        delay(SIMULATED_PAYMENT_DELAY_MS)
        return Result.Completed
    }

    sealed class Result {
        data object Completed : Result()
        data class Failed(val message: String) : Result()
    }

    private companion object {
        const val SIMULATED_PAYMENT_DELAY_MS = 1_500L
        const val CONNECTION_LOST_MARKER = "Connection to phone reader was lost"
    }
}
