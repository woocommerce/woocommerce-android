package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.callable.PaymentIntentCallback
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.PaymentIntentParameters
import com.stripe.stripeterminal.model.external.TerminalException
import com.woocommerce.android.cardreader.PaymentInfo
import com.woocommerce.android.cardreader.internal.payments.MetaDataKeys
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.wrappers.LogWrapper
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CreatePaymentAction(
    private val paymentIntentParametersFactory: PaymentIntentParametersFactory,
    private val terminal: TerminalWrapper,
    private val paymentUtils: PaymentUtils,
    private val logWrapper: LogWrapper
) {
    sealed class CreatePaymentStatus {
        data class Success(val paymentIntent: PaymentIntent) : CreatePaymentStatus()
        data class Failure(val exception: TerminalException) : CreatePaymentStatus()
    }

    fun createPaymentIntent(paymentInfo: PaymentInfo): Flow<CreatePaymentStatus> {
        return callbackFlow {
            terminal.createPaymentIntent(
                createParams(paymentInfo),
                object : PaymentIntentCallback {
                    override fun onSuccess(paymentIntent: PaymentIntent) {
                        logWrapper.d("CardReader", "Creating payment intent succeeded")
                        this@callbackFlow.sendBlocking(Success(paymentIntent))
                        this@callbackFlow.close()
                    }

                    override fun onFailure(e: TerminalException) {
                        logWrapper.d("CardReader", "Creating payment intent failed")
                        this@callbackFlow.sendBlocking(Failure(e))
                        this@callbackFlow.close()
                    }
                }
            )
            awaitClose()
        }
    }

    private fun createParams(paymentInfo: PaymentInfo): PaymentIntentParameters {
        val amountInSmallestCurrencyUnit = paymentUtils.convertBigDecimalInDollarsToIntegerInCents(paymentInfo.amount)
        val builder = paymentIntentParametersFactory.createBuilder()
            .setDescription(paymentInfo.paymentDescription)
            .setAmount(amountInSmallestCurrencyUnit)
            .setCurrency(paymentInfo.currency)
            .setMetadata(createMetaData(paymentInfo))
        paymentInfo.customerEmail?.takeIf { it.isNotEmpty() }?.let { builder.setReceiptEmail(it) }
        return builder.build()
    }

    private fun createMetaData(paymentInfo: PaymentInfo): Map<String, String> =
        mapOf(
            MetaDataKeys.STORE.key to paymentInfo.storeName.orEmpty(),
            MetaDataKeys.CUSTOMER_NAME.key to paymentInfo.customerName.orEmpty(),
            MetaDataKeys.CUSTOMER_EMAIL.key to paymentInfo.customerEmail.orEmpty(),
            MetaDataKeys.SITE_URL.key to paymentInfo.siteUrl.orEmpty(),
            MetaDataKeys.ORDER_ID.key to paymentInfo.orderId.toString(),
            // TODO cardreader Needs to be fixed when we add support for recurring payments
            MetaDataKeys.PAYMENT_TYPE.key to MetaDataKeys.PaymentTypes.SINGLE.key,
        )
}
