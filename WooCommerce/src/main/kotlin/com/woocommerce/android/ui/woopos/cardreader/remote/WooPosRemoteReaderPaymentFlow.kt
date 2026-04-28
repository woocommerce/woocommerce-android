package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
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
    private val appPrefs: AppPrefs = AppPrefs,
) {
    suspend fun collect(order: Order, onCaptureStarting: suspend () -> Unit = {}): Result {
        val connected = remoteReaderSession.state.value as? WooPosRemoteReaderSession.State.Connected
        if (connected?.reader?.isSimulated == true) {
            onCaptureStarting()
            return simulatePayment()
        }

        val site = selectedSite.get()
        val countryCode = wooStore.getStoreCountryCode(site)
            ?: return Result.Failed("Store country code unavailable")

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
            feeAmount = if (countryCode == CANADA_COUNTRY_CODE) CANADA_FEE_FLAT_IN_CENTS else null,
            channel = PaymentInfo.PaymentChannel.Pos,
        )

        return when (val outcome = remoteReaderSession.sendCollectPayment(paymentInfo)) {
            is CollectPaymentOutcome.Success -> {
                onCaptureStarting()
                capture(order.id, outcome.paymentIntentId)
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

    private fun genericFailureMessage(): String =
        resourceProvider.getString(R.string.woopos_remote_payment_failed_generic)

    private suspend fun capture(orderId: Long, paymentIntentId: String): Result =
        when (val response = cardReaderStore.capturePaymentIntent(orderId, paymentIntentId)) {
            is CapturePaymentResponse.Successful -> Result.Completed
            is CapturePaymentResponse.Error -> Result.Failed(response.message)
        }

    private suspend fun simulatePayment(): Result {
        delay(SIMULATED_PAYMENT_DELAY_MS)
        return Result.Completed
    }

    sealed class Result {
        data object Completed : Result()
        data class Failed(val message: String) : Result()
    }

    private companion object {
        const val CANADA_COUNTRY_CODE = "CA"
        const val CANADA_FEE_FLAT_IN_CENTS = 15L
        const val SIMULATED_PAYMENT_DELAY_MS = 1_500L
        const val CONNECTION_LOST_MARKER = "Connection to phone reader was lost"
    }
}
