package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.payment.CardReaderPaymentOrderHelper
import com.woocommerce.android.ui.payments.receipt.PaymentReceiptHelper
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosRemoteReaderPaymentFlow @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val cardReaderStore: CardReaderStore,
    private val remoteReaderSession: WooPosRemoteReaderSession,
    private val selectedSite: SelectedSite,
    private val cardReaderPaymentOrderHelper: CardReaderPaymentOrderHelper,
    private val paymentReceiptHelper: PaymentReceiptHelper,
    private val wooStore: WooCommerceStore,
    private val appPrefs: AppPrefs = AppPrefs,
) {
    suspend fun collect(order: Order): Result {
        val connected = remoteReaderSession.state.value as? WooPosRemoteReaderSession.State.Connected
        if (connected?.reader?.isSimulated == true) return simulatePayment()

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

        return when (val createResult = cardReaderManager.createPaymentIntent(paymentInfo)) {
            is CreatePaymentIntentResult.Success -> handleIntentCreated(order.id, createResult.clientSecret)
            is CreatePaymentIntentResult.Failed -> Result.Failed(
                createResult.cause.message ?: "Failed to create payment intent"
            )
        }
    }

    private suspend fun handleIntentCreated(orderId: Long, clientSecret: String): Result =
        when (val outcome = remoteReaderSession.sendCollectPayment(clientSecret)) {
            is CollectPaymentOutcome.Success -> capture(orderId, outcome.paymentIntentId)
            is CollectPaymentOutcome.Rejected -> Result.Failed("${outcome.code}: ${outcome.description}")
            CollectPaymentOutcome.TimedOut -> Result.Failed("Timed out waiting for phone reader")
            is CollectPaymentOutcome.Failed -> Result.Failed(
                outcome.cause.message ?: "Remote collection failed"
            )
        }

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
    }
}
