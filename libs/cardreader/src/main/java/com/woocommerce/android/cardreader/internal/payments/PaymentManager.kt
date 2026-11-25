package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.Charge
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.PaymentIntentStatus
import com.stripe.stripeterminal.external.models.PaymentIntentStatus.CANCELED
import com.stripe.stripeterminal.external.models.PaymentMethod
import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicReference

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
    @Volatile private var activePayment: ActivePayment? = null

    @Suppress("TooGenericExceptionCaught")
    fun acceptPayment(paymentInfo: PaymentInfo): Flow<CardPaymentStatus> = flow {
        val attempt = ActivePayment().also { activePayment = it }
        val paymentIntent = try {
            if (isInvalidState(paymentInfo)) null else createPaymentIntent(paymentInfo)
        } catch (error: Exception) {
            attempt.intent.complete(Result.failure(error))
            throw error
        }
        attempt.intent.complete(Result.success(paymentIntent))
        if (attempt.cancelRequested) return@flow
        if (paymentIntent?.status != PaymentIntentStatus.REQUIRES_PAYMENT_METHOD) {
            return@flow
        }
        processPaymentIntent(paymentInfo, paymentIntent).collect { emit(it) }
    }

    fun retryPayment(orderId: Long, paymentData: PaymentData): Flow<CardPaymentStatus> {
        val intent = (paymentData as PaymentDataImpl).paymentIntent
        activePayment = ActivePayment().apply { this.intent.complete(Result.success(intent)) }
        return processPaymentIntent(orderId, intent)
    }

    suspend fun cancelActivePaymentAndAwait(): Boolean {
        val attempt = activePayment ?: return false
        val confirmation = CompletableDeferred<Boolean>()
        while (true) {
            val current = attempt.cancellation.get()
            if (current?.isActive == true) return current.await()
            if (attempt.cancellation.compareAndSet(current, confirmation)) break
        }
        attempt.cancelRequested = true
        return try {
            cancelAttempt(attempt).also { confirmation.complete(it) }
        } finally {
            confirmation.complete(false)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun cancelAttempt(attempt: ActivePayment): Boolean {
        return try {
            val intent = attempt.intent.await().getOrThrow() ?: return true
            if (!canCancelAttempt(attempt)) return false
            val commandCancelled = terminalWrapper.cancelPaymentCommandAndAwait()
            if (!commandCancelled || !canCancelAttempt(attempt)) return false
            if (intent.status == CANCELED) true else terminalWrapper.cancelPayment(intent).status == CANCELED
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            isCancelledIntent(attempt)
        }
    }

    private fun canCancelAttempt(attempt: ActivePayment): Boolean = activePayment === attempt &&
        attempt.captureOutcome !in setOf(CaptureOutcome.IN_PROGRESS, CaptureOutcome.CAPTURED)

    @Suppress("TooGenericExceptionCaught")
    private suspend fun isCancelledIntent(attempt: ActivePayment): Boolean {
        val secret = attempt.intent.await().getOrNull()?.clientSecret ?: return false
        return try {
            terminalWrapper.retrievePaymentIntent(secret).status == CANCELED && activePayment === attempt
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            false
        }
    }

    private class ActivePayment {
        val intent = CompletableDeferred<Result<PaymentIntent?>>()
        val cancellation = AtomicReference<CompletableDeferred<Boolean>?>(null)

        @Volatile var cancelRequested = false

        @Volatile var captureOutcome: CaptureOutcome? = null
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun createPaymentIntentOnly(paymentInfo: PaymentInfo): CreatePaymentIntentResult {
        val attempt = ActivePayment().also { activePayment = it }
        validateRemoteState(paymentInfo)?.let {
            attempt.intent.complete(Result.success(null))
            return CreatePaymentIntentResult.Failed(it)
        }
        return try {
            when (val result = createPaymentAction.createPaymentIntent(paymentInfo)) {
                is Success -> {
                    attempt.intent.complete(Result.success(result.paymentIntent))
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
                is Failure -> {
                    attempt.intent.complete(Result.success(null))
                    CreatePaymentIntentResult.Failed(result.exception)
                }
            }
        } catch (cancel: CancellationException) {
            attempt.intent.complete(Result.failure(cancel))
            throw cancel
        } catch (cause: Exception) {
            attempt.intent.complete(Result.failure(cause))
            CreatePaymentIntentResult.Failed(cause)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun retrieveAndCollectPayment(clientSecret: String, paymentInfo: PaymentInfo): RetrieveAndCollectResult {
        val attempt = activePayment ?: ActivePayment().also { activePayment = it }
        return try {
            val retrieved = terminalWrapper.retrievePaymentIntent(clientSecret)
            attempt.intent.complete(Result.success(retrieved))
            if (attempt.cancelRequested) return remoteCancellationResult(attempt)
            val processed = processPaymentIntentForRemoteReader(paymentInfo, retrieved, attempt)
            val id = processed.id
            val status = processed.status?.name?.lowercase()
            if (id == null || status == null) {
                RetrieveAndCollectResult.Failed(
                    IllegalStateException("processPaymentIntent returned null id or status"),
                )
            } else {
                RetrieveAndCollectResult.Success(
                    paymentIntentId = id,
                    status = status,
                    paymentMethodType = determinePaymentMethodType(processed),
                )
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (cause: Exception) {
            attempt.intent.complete(Result.failure(cause))
            if (attempt.cancelRequested) {
                remoteCancellationResult(attempt)
            } else {
                RetrieveAndCollectResult.Failed(cause)
            }
        }
    }

    private suspend fun remoteCancellationResult(attempt: ActivePayment): RetrieveAndCollectResult {
        val confirmed = attempt.cancellation.get()?.await() == true
        return RetrieveAndCollectResult.Failed(
            TerminalException(
                if (confirmed) TerminalErrorCode.CANCELED else TerminalErrorCode.CANCEL_FAILED,
                if (confirmed) "Payment cancellation confirmed" else "Payment cancellation unconfirmed",
            )
        )
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
        return paymentIntent.latestCharge?.receiptUrl ?: run {
            emit(
                PaymentFailed(
                    CardPaymentStatus.CardPaymentStatusErrorType.CaptureOutcomeUnknown,
                    PaymentDataImpl(paymentIntent),
                    "ReceiptUrl not available",
                )
            )
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
                onProcessingFailure(collectedPaymentIntent, result.exception)
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
                onProcessingFailure(paymentIntent, result.exception)
            }
            is ProcessPaymentIntentStatus.Success -> {
                val paymentMethodType = determinePaymentMethodType(result.paymentIntent)
                emit(ProcessingPaymentCompleted(paymentMethodType))
                result.paymentIntent
            }
        }
    }

    private suspend fun FlowCollector<CardPaymentStatus>.onProcessingFailure(
        original: PaymentIntent,
        error: TerminalException,
    ): PaymentIntent {
        val updated = error.paymentIntent
        if (updated?.status == PaymentIntentStatus.REQUIRES_CAPTURE ||
            updated?.status == PaymentIntentStatus.SUCCEEDED
        ) {
            return updated
        }
        val unknown = updated == null && error.errorCode in setOf(
            TerminalErrorCode.REQUEST_TIMED_OUT,
            TerminalErrorCode.STRIPE_API_CONNECTION_ERROR,
            TerminalErrorCode.STRIPE_API_ERROR,
            TerminalErrorCode.STRIPE_API_RESPONSE_DECODING_ERROR,
            TerminalErrorCode.BLUETOOTH_DISCONNECTED,
            TerminalErrorCode.USB_DISCONNECTED,
        )
        emit(
            if (unknown) {
                PaymentFailed(
                    CardPaymentStatus.CardPaymentStatusErrorType.CaptureOutcomeUnknown,
                    PaymentDataImpl(original),
                    error.errorMessage
                )
            } else {
                errorMapper.mapTerminalError(original, error)
            }
        )
        return original
    }

    // Stripe new SDk now has paymentIntent.id as nullable to support offline payment. But we don't support offline yet
    // so adding paymentIntent.id!! here for now. Code need to be refactored once we support offline payment.
    private suspend fun FlowCollector<CardPaymentStatus>.capturePayment(
        receiptUrl: String,
        orderId: Long,
        cardReaderStore: CardReaderStore,
        paymentIntent: PaymentIntent
    ) {
        val attempt = activePayment
        attempt?.captureOutcome = CaptureOutcome.IN_PROGRESS
        emit(CapturingPayment)
        when (val captureResponse = cardReaderStore.capturePaymentIntent(orderId, paymentIntent.id!!)) {
            is CapturePaymentResponse.Successful -> {
                attempt?.captureOutcome = CaptureOutcome.CAPTURED
                emit(PaymentCompleted(receiptUrl))
            }
            is CapturePaymentResponse.Error -> {
                val outcome = checkCaptureOutcome(paymentIntent, captureResponse)
                attempt?.captureOutcome = outcome
                when (outcome) {
                    CaptureOutcome.CAPTURED -> emit(PaymentCompleted(receiptUrl))
                    CaptureOutcome.NOT_CAPTURED -> {
                        emit(errorMapper.mapCapturePaymentError(paymentIntent, captureResponse))
                    }
                    CaptureOutcome.UNKNOWN ->
                        emit(errorMapper.mapUnknownCaptureOutcomeError(paymentIntent, captureResponse))
                    CaptureOutcome.IN_PROGRESS -> error("Capture response must have a terminal outcome")
                }
            }
        }
    }

    /* Ambiguous capture errors don't reveal whether the backend actually captured the payment. Refreshing the
    intent and finding it SUCCEEDED means the funds were captured, so the payment is reported as completed to
    prevent a retry leading to a double charge. When the intent can't be refreshed (e.g. the network is down), its
    status can't confirm the backend call went through (single-step terminal payments are SUCCEEDED before the
    capture call), or the request was lost, timed out, cut off by a gateway or failed unexpectedly on the server so
    the capture may still be running, the outcome is reported as unknown so the merchant doesn't take another
    payment. */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun checkCaptureOutcome(
        paymentIntent: PaymentIntent,
        error: CapturePaymentResponse.Error,
    ): CaptureOutcome {
        val isAmbiguousError = error is CapturePaymentResponse.Error.GenericError ||
            error is CapturePaymentResponse.Error.ServerError ||
            error is CapturePaymentResponse.Error.NetworkError ||
            error is CapturePaymentResponse.Error.GatewayError
        if (!isAmbiguousError) return CaptureOutcome.NOT_CAPTURED
        val clientSecret = paymentIntent.clientSecret
        if (paymentIntent.status != PaymentIntentStatus.REQUIRES_CAPTURE || clientSecret == null) {
            return CaptureOutcome.UNKNOWN
        }
        return try {
            when (terminalWrapper.retrievePaymentIntent(clientSecret).status) {
                PaymentIntentStatus.SUCCEEDED -> CaptureOutcome.CAPTURED
                PaymentIntentStatus.CANCELED,
                PaymentIntentStatus.REQUIRES_PAYMENT_METHOD -> CaptureOutcome.NOT_CAPTURED
                PaymentIntentStatus.REQUIRES_CAPTURE -> if (error.mayStillBeCapturing()) {
                    CaptureOutcome.UNKNOWN
                } else {
                    CaptureOutcome.NOT_CAPTURED
                }
                else -> CaptureOutcome.UNKNOWN
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (ignored: Exception) {
            CaptureOutcome.UNKNOWN
        }
    }

    private fun CapturePaymentResponse.Error.mayStillBeCapturing() =
        this is CapturePaymentResponse.Error.NetworkError ||
            this is CapturePaymentResponse.Error.GatewayError ||
            this is CapturePaymentResponse.Error.ServerError

    private enum class CaptureOutcome {
        IN_PROGRESS,
        CAPTURED,
        NOT_CAPTURED,
        UNKNOWN,
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
        paymentIntent: PaymentIntent,
        attempt: ActivePayment,
    ): PaymentIntent {
        checkRemoteAttempt(attempt)
        if (paymentInfo.terminalPaymentPreparation == PaymentInfo.TerminalPaymentPreparation.NONE) {
            return terminalWrapper.processPaymentIntent(paymentIntent)
        }

        val collectedPaymentIntent = terminalWrapper.collectPaymentMethod(paymentIntent)
        checkRemoteAttempt(attempt)
        when (val result = prepareTerminalPaymentIfNeeded(paymentInfo, collectedPaymentIntent)) {
            TerminalPaymentPreparationResult.Prepared -> Unit
            is TerminalPaymentPreparationResult.Failed -> throw IllegalStateException(result.message)
        }
        checkRemoteAttempt(attempt)
        return terminalWrapper.confirmPaymentIntent(collectedPaymentIntent)
    }

    private fun checkRemoteAttempt(attempt: ActivePayment) {
        if (attempt.cancelRequested || activePayment !== attempt) {
            throw TerminalException(TerminalErrorCode.CANCEL_FAILED, "Payment owner changed or requested cancellation")
        }
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
            paymentIntent.latestCharge?.paymentMethodDetails?.interacPresentDetails != null
    }

    private fun isEftposAuPayment(paymentIntent: PaymentIntent): Boolean {
        return paymentIntent.paymentMethod?.cardPresentDetails?.canProcessAsEftposAu() == true ||
            paymentIntent.latestCharge
                ?.paymentMethodDetails
                ?.cardPresentDetails
                ?.canProcessAsEftposAu() == true
    }

    private fun PaymentMethod.CardPresent.canProcessAsEftposAu(): Boolean {
        return brand.isEftposAuNetwork() || networks?.available?.any { it.isEftposAuNetwork() } == true
    }

    private fun Charge.CardPresent.canProcessAsEftposAu(): Boolean {
        return brand.isEftposAuNetwork() || network.isEftposAuNetwork()
    }

    private fun String?.isEftposAuNetwork() = this?.equals(EFTPOS_AU_NETWORK, ignoreCase = true) == true

    private fun isSingleStepTerminalPaymentSuccessful(paymentIntent: PaymentIntent): Boolean {
        return paymentIntent.status == PaymentIntentStatus.SUCCEEDED &&
            (isInteracPayment(paymentIntent) || isEftposAuPayment(paymentIntent))
    }

    private fun determinePaymentMethodType(paymentIntent: PaymentIntent): PaymentMethodType {
        val charge = paymentIntent.latestCharge
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
