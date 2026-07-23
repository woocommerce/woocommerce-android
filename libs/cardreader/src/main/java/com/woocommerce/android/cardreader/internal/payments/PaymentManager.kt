package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.CardPresentDetails
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentStatus
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.CANCELED
import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.CardReaderStore.PreparePaymentResponse
import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForSupportedCountry
import com.woocommerce.android.cardreader.internal.payments.actions.CancelPaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Failure
import com.woocommerce.android.cardreader.internal.payments.actions.CreatePaymentAction.CreatePaymentStatus.Success
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction
import com.woocommerce.android.cardreader.internal.payments.actions.ProcessPaymentIntentAction.ProcessPaymentIntentStatus
import com.woocommerce.android.cardreader.internal.wrappers.TerminalWrapper
import com.woocommerce.android.cardreader.payments.CardPaymentStatus
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CapturingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.InitializingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentCompleted
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentFailed
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPayment
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.ProcessingPaymentCompleted
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.payments.PaymentData
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.RetrieveAndCollectResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

private const val EFTPOS_AU_NETWORK = "eftpos_au"

@Suppress("LongParameterList")
internal class PaymentManager(
    private val terminalWrapper: TerminalWrapper,
    private val cardReaderStore: CardReaderStore,
    private val createPaymentAction: CreatePaymentAction,
    private val processPaymentIntentAction: ProcessPaymentIntentAction,
    private val cancelPaymentAction: CancelPaymentAction,
    private val paymentUtils: PaymentUtils,
    private val errorMapper: PaymentErrorMapper,
    private val cardReaderConfigFactory: CardReaderConfigFactory,
) {
    fun acceptPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus> = flow {
        if (isInvalidState(paymentInfo)) return@flow

        val paymentIntent = createPaymentIntent(paymentInfo)
        if (paymentIntent?.status != PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            return@flow
        }
        processPaymentIntent(paymentInfo, paymentIntent).collect { emit(it) }
    }

    fun retryPayment(orderId: Long, paymentData: PaymentData) =
        processPaymentIntent(orderId, (paymentData as PaymentDataImpl).paymentIntent)

    @Suppress("TooGenericExceptionCaught")
    suspend fun createPaymentIntentOnly(paymentInfo: PaymentInfo): CreatePaymentIntentResult {
        validateRemoteState(paymentInfo)?.let { return CreatePaymentIntentResult.Failed(it) }
        return try {
            when (val result = createPaymentAction.createPaymentIntent(paymentInfo)) {
                is Success -> {
                    val id = result.paymentIntent.id
                    val secret = result.paymentIntent.clientSecret
                    if (id == null || secret == null) {
                        CreatePaymentIntentResult.Failed(
                            IllegalStateException("createPaymentIntent returned null id or clientSecret"),
                        )
                    } else {
                        CreatePaymentIntentResult.Success(paymentIntentId = id, clientSecret = secret)
                    }
                }
                is Failure -> CreatePaymentIntentResult.Failed(result.exception)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (cause: Exception) {
            CreatePaymentIntentResult.Failed(cause)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun retrieveAndCollectPayment(clientSecret: String, paymentInfo: PaymentInfo): RetrieveAndCollectResult =
        try {
            val retrieved = terminalWrapper.retrievePaymentIntent(clientSecret)
            val processed = processPaymentIntentForRemoteReader(paymentInfo, retrieved)
            val id = processed.id
            val status = processed.status?.name?.lowercase()
            if (id == null || status == null) {
                RetrieveAndCollectResult.Failed(
                    IllegalStateException("processPaymentIntent returned null id or status"),
                )
            } else {
                RetrieveAndCollectResult.Success(paymentIntentId = id, status = status)
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (cause: Exception) {
            RetrieveAndCollectResult.Failed(cause)
        }

    private fun validateRemoteState(paymentInfo: PaymentInfo): Throwable? {
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(paymentInfo.countryCode)
        return when {
            cardReaderConfig !is CardReaderConfigForSupportedCountry ||
                !paymentUtils.isSupportedCurrency(paymentInfo.currency, cardReaderConfig) ->
                IllegalStateException(
                    "Unsupported country/currency: ${paymentInfo.countryCode}/${paymentInfo.currency}"
                )
            !terminalWrapper.isInitialized() -> IllegalStateException("Reader not connected")
            else -> null
        }
    }

    fun cancelPayment(paymentData: PaymentData) {
        val paymentIntent = (paymentData as PaymentDataImpl).paymentIntent
        /* If the paymentIntent is in REQUIRES_CAPTURE state the app should not cancel the payment intent as it
        doesn't know if it was already captured or not during one of the previous attempts to capture it. */
        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_PAYMENT_METHOD ||
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CONFIRMATION
        ) {
            cancelPaymentAction.cancelPayment(paymentIntent)
        }
    }

    private fun processPaymentIntent(orderId: Long, data: PaymentIntent) = flow {
        var paymentIntent = data
        if (paymentIntent.status == null || paymentIntent.status == CANCELED) {
            emit(errorMapper.mapError(errorMessage = "Cannot retry paymentIntent with status ${paymentIntent.status}"))
            return@flow
        }

        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_PAYMENT_METHOD ||
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CONFIRMATION
        ) {
            emit(ProcessingPayment)
            paymentIntent = processPaymentWithoutPreparation(paymentIntent)
        }

        /*
            Single-step terminal payments, such as Interac and EFTPOS-routed card-present payments, may already be
            successful after processing. In that case, the backend capture endpoint is still called to record the
            completed payment on the order, rather than to capture funds.

            Other card-present payments are expected to reach REQUIRES_CAPTURE and are captured by the backend.
         */

        if (
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CAPTURE ||
            isSingleStepTerminalPaymentSuccessful(paymentIntent)
        ) {
            retrieveReceiptUrl(paymentIntent)?.let { receiptUrl ->
                capturePayment(receiptUrl, orderId, cardReaderStore, paymentIntent)
            }
        }
    }

    private fun processPaymentIntent(paymentInfo: PaymentInfo, data: PaymentIntent) = flow {
        var paymentIntent = data
        if (paymentIntent.status == null || paymentIntent.status == CANCELED) {
            emit(errorMapper.mapError(errorMessage = "Cannot retry paymentIntent with status ${paymentIntent.status}"))
            return@flow
        }

        if (paymentIntent.status == PaymentIntentStatus.REQUIRES_PAYMENT_METHOD ||
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CONFIRMATION
        ) {
            paymentIntent = processPayment(paymentInfo, paymentIntent)
        }

        if (
            paymentIntent.status == PaymentIntentStatus.REQUIRES_CAPTURE ||
            isSingleStepTerminalPaymentSuccessful(paymentIntent)
        ) {
            retrieveReceiptUrl(paymentIntent)?.let { receiptUrl ->
                capturePayment(receiptUrl, paymentInfo.orderId, cardReaderStore, paymentIntent)
            }
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.retrieveReceiptUrl(
        paymentIntent: PaymentIntent
    ): String? {
        return paymentIntent.getCharges().takeIf { it.isNotEmpty() }?.get(0)?.receiptUrl ?: run {
            emit(PaymentFailed(Generic, null, "ReceiptUrl not available"))
            null
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.createPaymentIntent(paymentInfo: PaymentInfo): PaymentIntent? {
        emit(InitializingPayment)
        return when (val result = createPaymentAction.createPaymentIntent(paymentInfo)) {
            is Failure -> {
                emit(errorMapper.mapTerminalError(null, result.exception))
                null
            }
            is Success -> result.paymentIntent
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.processPayment(
        paymentInfo: PaymentInfo,
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        emit(ProcessingPayment)
        if (paymentInfo.terminalPaymentPreparation == PaymentInfo.TerminalPaymentPreparation.NONE) {
            return processPaymentWithoutPreparation(paymentIntent)
        }

        val collectedPaymentIntent = when (
            val result = processPaymentIntentAction.collectPaymentMethod(paymentIntent)
        ) {
            is ProcessPaymentIntentStatus.Failure -> {
                emit(errorMapper.mapTerminalError(paymentIntent, result.exception))
                return paymentIntent
            }
            is ProcessPaymentIntentStatus.Success -> result.paymentIntent
        }

        when (val result = prepareTerminalPaymentIfNeeded(paymentInfo, collectedPaymentIntent)) {
            TerminalPaymentPreparationResult.Prepared -> Unit
            is TerminalPaymentPreparationResult.Failed -> {
                emit(errorMapper.mapError(collectedPaymentIntent, result.message))
                return collectedPaymentIntent
            }
        }

        return when (val result = processPaymentIntentAction.confirmPaymentIntent(collectedPaymentIntent)) {
            is ProcessPaymentIntentStatus.Failure -> {
                emit(errorMapper.mapTerminalError(collectedPaymentIntent, result.exception))
                collectedPaymentIntent
            }
            is ProcessPaymentIntentStatus.Success -> {
                val paymentMethodType = determinePaymentMethodType(result.paymentIntent)
                emit(ProcessingPaymentCompleted(paymentMethodType))
                result.paymentIntent
            }
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.processPaymentWithoutPreparation(
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        return when (val result = processPaymentIntentAction.processPaymentIntent(paymentIntent)) {
            is ProcessPaymentIntentStatus.Failure -> {
                emit(errorMapper.mapTerminalError(paymentIntent, result.exception))
                paymentIntent
            }
            is ProcessPaymentIntentStatus.Success -> {
                val paymentMethodType = determinePaymentMethodType(result.paymentIntent)
                emit(ProcessingPaymentCompleted(paymentMethodType))
                result.paymentIntent
            }
        }
    }

    // Stripe new SDk now has paymentIntent.id as nullable to support offline payment. But we don't support offline yet
    // so adding paymentIntent.id!! here for now. Code need to be refactored once we support offline payment.
    private suspend fun FlowCollector<CardPaymentStatus>.capturePayment(
        receiptUrl: String,
        orderId: Long,
        cardReaderStore: CardReaderStore,
        paymentIntent: PaymentIntent
    ) {
        emit(CapturingPayment)
        when (val captureResponse = cardReaderStore.capturePaymentIntent(orderId, paymentIntent.id!!)) {
            is CapturePaymentResponse.Successful -> emit(PaymentCompleted(receiptUrl))
            is CapturePaymentResponse.Error -> {
                if (wasCapturedDespiteError(paymentIntent, captureResponse)) {
                    emit(PaymentCompleted(receiptUrl))
                } else {
                    emit(errorMapper.mapCapturePaymentError(paymentIntent, captureResponse))
                }
            }
        }
    }

    /* Ambiguous capture errors don't reveal whether the backend actually captured the payment. Refreshing the
    intent and finding it SUCCEEDED means the funds were captured, so the payment is reported as completed to
    prevent a retry leading to a double charge. Intents that are already SUCCEEDED before the capture call
    (single-step terminal payments) are excluded as their status can't confirm the backend call went through. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun wasCapturedDespiteError(
        paymentIntent: PaymentIntent,
        error: CapturePaymentResponse.Error,
    ): Boolean {
        val isAmbiguousError = error is CapturePaymentResponse.Error.GenericError ||
            error is CapturePaymentResponse.Error.ServerError ||
            error is CapturePaymentResponse.Error.NetworkError
        if (!isAmbiguousError || paymentIntent.status != PaymentIntentStatus.REQUIRES_CAPTURE) return false
        val clientSecret = paymentIntent.clientSecret ?: return false
        return try {
            terminalWrapper.retrievePaymentIntent(clientSecret).status == PaymentIntentStatus.SUCCEEDED
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (ignored: Exception) {
            false
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.isInvalidState(paymentInfo: PaymentInfo): Boolean {
        val cardReaderConfig = cardReaderConfigFactory.getCardReaderConfigFor(paymentInfo.countryCode)
        return when {
            cardReaderConfig !is CardReaderConfigForSupportedCountry ||
                !paymentUtils.isSupportedCurrency(paymentInfo.currency, cardReaderConfig) -> {
                emit(errorMapper.mapError(errorMessage = "Unsupported currency: ${paymentInfo.currency}"))
                true
            }
            !terminalWrapper.isInitialized() -> {
                emit(errorMapper.mapError(errorMessage = "Reader not connected"))
                true
            }
            else -> false
        }
    }

    private suspend fun processPaymentIntentForRemoteReader(
        paymentInfo: PaymentInfo,
        paymentIntent: PaymentIntent
    ): PaymentIntent {
        if (paymentInfo.terminalPaymentPreparation == PaymentInfo.TerminalPaymentPreparation.NONE) {
            return terminalWrapper.processPaymentIntent(paymentIntent)
        }

        val collectedPaymentIntent = terminalWrapper.collectPaymentMethod(paymentIntent)
        when (val result = prepareTerminalPaymentIfNeeded(paymentInfo, collectedPaymentIntent)) {
            TerminalPaymentPreparationResult.Prepared -> Unit
            is TerminalPaymentPreparationResult.Failed -> throw IllegalStateException(result.message)
        }
        return terminalWrapper.confirmPaymentIntent(collectedPaymentIntent)
    }

    private suspend fun prepareTerminalPaymentIfNeeded(
        paymentInfo: PaymentInfo,
        paymentIntent: PaymentIntent
    ): TerminalPaymentPreparationResult {
        val shouldPrepare = when (paymentInfo.terminalPaymentPreparation) {
            PaymentInfo.TerminalPaymentPreparation.NONE -> false
            PaymentInfo.TerminalPaymentPreparation.CANADA_INTERAC -> isInteracPayment(paymentIntent)
            PaymentInfo.TerminalPaymentPreparation.AUSTRALIA_CARD_PRESENT -> isEftposAuPayment(paymentIntent)
        }
        if (!shouldPrepare) return TerminalPaymentPreparationResult.Prepared

        val paymentIntentId = paymentIntent.id
            ?: return TerminalPaymentPreparationResult.Failed("PaymentIntent id not available")

        return when (val response = cardReaderStore.preparePaymentIntent(paymentInfo.orderId, paymentIntentId)) {
            PreparePaymentResponse.Success -> TerminalPaymentPreparationResult.Prepared
            is PreparePaymentResponse.Error -> TerminalPaymentPreparationResult.Failed(response.message)
        }
    }

    private fun isInteracPayment(paymentIntent: PaymentIntent): Boolean {
        return paymentIntent.paymentMethod?.interacPresentDetails != null ||
            paymentIntent.getCharges().firstOrNull()?.paymentMethodDetails?.interacPresentDetails != null
    }

    private fun isEftposAuPayment(paymentIntent: PaymentIntent): Boolean {
        return paymentIntent.paymentMethod?.cardPresentDetails?.canProcessAsEftposAu() == true ||
            paymentIntent.getCharges().firstOrNull()
                ?.paymentMethodDetails
                ?.cardPresentDetails
                ?.canProcessAsEftposAu() == true
    }

    private fun CardPresentDetails.canProcessAsEftposAu(): Boolean {
        return brand?.equals(EFTPOS_AU_NETWORK, ignoreCase = true) == true ||
            network?.equals(EFTPOS_AU_NETWORK, ignoreCase = true) == true ||
            networks?.available?.any { it.equals(EFTPOS_AU_NETWORK, ignoreCase = true) } == true
    }

    private fun isSingleStepTerminalPaymentSuccessful(paymentIntent: PaymentIntent): Boolean {
        return paymentIntent.status == PaymentIntentStatus.SUCCEEDED &&
            (isInteracPayment(paymentIntent) || isEftposAuPayment(paymentIntent))
    }

    private fun determinePaymentMethodType(paymentIntent: PaymentIntent): PaymentMethodType {
        val charge = paymentIntent.getCharges().firstOrNull()
        return when {
            paymentIntent.paymentMethod?.interacPresentDetails != null -> PaymentMethodType.INTERAC_PRESENT
            paymentIntent.paymentMethod?.cardPresentDetails != null -> PaymentMethodType.CARD_PRESENT
            charge?.paymentMethodDetails?.interacPresentDetails != null -> PaymentMethodType.INTERAC_PRESENT
            charge?.paymentMethodDetails?.cardPresentDetails != null -> PaymentMethodType.CARD_PRESENT
            else -> PaymentMethodType.UNKNOWN
        }
    }

    private sealed class TerminalPaymentPreparationResult {
        data object Prepared : TerminalPaymentPreparationResult()
        data class Failed(val message: String) : TerminalPaymentPreparationResult()
    }
}

internal data class PaymentDataImpl(val paymentIntent: PaymentIntent) : PaymentData
