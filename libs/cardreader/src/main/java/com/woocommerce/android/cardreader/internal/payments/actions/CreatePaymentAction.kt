package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.payments.PaymentInfo
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
        val amountInSmallestCurrencyUnit = paymentUtils.convertBigDecimalInDollarsToLongInCents(paymentInfo.amount)
        val builder = paymentIntentParametersFactory.createBuilder()
            .setDescription(paymentInfo.paymentDescription)
            .setAmount(amountInSmallestCurrencyUnit)
            .setCurrency(paymentInfo.currency)
            .setMetadata(createMetaData(paymentInfo))
        with(paymentInfo) {
            customerId?.let { builder.setCustomer(it) }
            customerEmail?.takeIf { it.isNotEmpty() }?.let { builder.setReceiptEmail(it) }
        }
        return builder.build()
    }

    private fun createMetaData(paymentInfo: PaymentInfo): Map<String, String> {
        val map = mutableMapOf<String, String>()
        paymentInfo.storeName.takeUnless { it.isNullOrBlank() }
            ?.let { map[MetaDataKeys.STORE.key] = it }

        paymentInfo.customerName.takeUnless { it.isNullOrBlank() }
            ?.let { map[MetaDataKeys.CUSTOMER_NAME.key] = it }

        paymentInfo.customerEmail.takeUnless { it.isNullOrBlank() }
            ?.let { map[MetaDataKeys.CUSTOMER_EMAIL.key] = it }

        paymentInfo.siteUrl.takeUnless { it.isNullOrBlank() }
            ?.let { map[MetaDataKeys.SITE_URL.key] = it }

        paymentInfo.orderKey.takeUnless { it.isNullOrBlank() }
            ?.let { map[MetaDataKeys.ORDER_KEY.key] = it }

        val readerId = terminal.getConnectedReader()?.id
        if (readerId == null) {
            logWrapper.e("CardReader", "collecting payment with reader without serial number")
        } else {
            map[MetaDataKeys.READER_ID.key] = readerId
        }

        map[MetaDataKeys.ORDER_ID.key] = paymentInfo.orderId.toString()

        // TODO cardreader Needs to be fixed when we add support for recurring payments
        map[MetaDataKeys.PAYMENT_TYPE.key] = MetaDataKeys.PaymentTypes.SINGLE.key
        return map
    }
}
