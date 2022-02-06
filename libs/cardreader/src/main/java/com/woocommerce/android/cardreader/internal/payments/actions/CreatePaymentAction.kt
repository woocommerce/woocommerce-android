package com.woocommerce.android.cardreader.internal.payments.actions

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.LOG_TAG
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.internal.payments.MetaDataKeys
import com.woocommerce.android.cardreader.internal.payments.PaymentUtils
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.sendAndLog
import com.woocommerce.android.cardreader.internal.wrappers.PaymentIntentParametersFactory
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.PaymentInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class CreatePaymentAction(
    private val paymentIntentParametersFactory: PaymentIntentParametersFactory,
    private val terminal: TerminalWrapper,
    private val paymentUtils: PaymentUtils,
    private val logWrapper: LogWrapper,
    private val cardReaderConfigFactory: CardReaderConfigFactory
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
                        logWrapper.d(LOG_TAG, "Creating payment intent succeeded")
                        this@callbackFlow.sendAndLog(Success(paymentIntent), logWrapper)
                        this@callbackFlow.close()
                    }

                    override fun onFailure(e: TerminalException) {
                        logWrapper.d(LOG_TAG, "Creating payment intent failed")
                        this@callbackFlow.sendAndLog(Failure(e), logWrapper)
                        this@callbackFlow.close()
                    }
                }
            )
            awaitClose()
        }
    }

    private fun createParams(paymentInfo: PaymentInfo): PaymentIntentParameters {
        val amountInSmallestCurrencyUnit = paymentUtils.convertBigDecimalInDollarsToLongInCents(paymentInfo.amount)
        val cardReaderConfigFactory = cardReaderConfigFactory.getCardReaderConfigFor(paymentInfo.countryCode)
        val builder = paymentIntentParametersFactory.createBuilder(
            cardReaderConfigFactory.paymentMethodType
        )
            .setDescription(paymentInfo.paymentDescription)
            .setAmount(amountInSmallestCurrencyUnit)
            .setCurrency(paymentInfo.currency)
            .setMetadata(createMetaData(paymentInfo))
        with(paymentInfo) {
            customerId?.let { builder.setCustomer(it) }
            customerEmail?.takeIf { it.isNotEmpty() }?.let { builder.setReceiptEmail(it) }
            statementDescriptor?.takeIf { it.isNotEmpty() }?.let { builder.setStatementDescriptor(it) }
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

        val connectedReader = terminal.getConnectedReader()
        if (connectedReader != null) {
            val readerId = connectedReader.id
            if (readerId == null) {
                logWrapper.e(LOG_TAG, "collecting payment with reader without serial number")
            } else {
                map[MetaDataKeys.READER_ID.key] = readerId
            }

            map[MetaDataKeys.READER_MODEL.key] = connectedReader.type
        } else {
            logWrapper.e(LOG_TAG, "collecting payment with connected reader which is null")
        }

        map[MetaDataKeys.ORDER_ID.key] = paymentInfo.orderId.toString()

        // TODO cardreader Needs to be fixed when we add support for recurring payments
        map[MetaDataKeys.PAYMENT_TYPE.key] = MetaDataKeys.PaymentTypes.SINGLE.key
        return map
    }
}
