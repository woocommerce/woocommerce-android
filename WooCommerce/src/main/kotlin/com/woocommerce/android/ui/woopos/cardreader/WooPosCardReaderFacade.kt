package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCardReaderFacade @Inject constructor(
    private val cardReaderManager: CardReaderManager,
    private val featureFlags: FeatureFlagRepository,
) {
    val readerStatus: StateFlow<CardReaderStatus> = cardReaderManager.readerStatus
    val softwareUpdateAvailability: Flow<SoftwareUpdateAvailability> = cardReaderManager.softwareUpdateAvailability
    val batteryStatus: Flow<CardReaderBatteryStatus> = cardReaderManager.batteryStatus

    val processId: String = UUID.randomUUID().toString()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _paymentState = MutableStateFlow<PaymentState>(PaymentState.Idle)
    val paymentState = _paymentState.asStateFlow()
    val isPaymentPending: Boolean get() = paymentState.value is PaymentState.Pending

    internal fun trackPayment(controller: CardReaderPaymentController): Boolean {
        if (isPaymentPending) return false
        if (!featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES)) return true
        val owner = beginControllerPayment(controller) ?: return false
        observePayment(controller, owner)
        return true
    }

    fun retryPayment(controller: CardReaderPaymentController, retry: () -> Unit): Boolean {
        if (!featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES)) {
            retry()
            return true
        }
        if (isPaymentPending) return false
        val previous = controller.paymentState.value
        val owner = beginControllerPayment(controller) ?: return false
        retry()
        observePayment(controller, owner, previous)
        return true
    }

    private fun observePayment(
        controller: CardReaderPaymentController,
        owner: PaymentOwner,
        previous: CardReaderPaymentOrRefundState? = null,
    ) {
        owner.observer = scope.launch {
            controller.paymentState.dropWhile { it === previous }.takeWhile { payment ->
                when (payment) {
                    is CardReaderPaymentState.PaymentSuccessful -> finishPayment(owner, successful = true)
                    is CardReaderPaymentState.PaymentFailed -> {
                        if (payment.errorType == PaymentFlowError.CaptureOutcomeUnknown) {
                            markUnresolved(owner)
                        } else if (payment.errorType != PaymentFlowError.Canceled ||
                            (
                                _paymentState.value !is PaymentState.Cancelling &&
                                    _paymentState.value !is PaymentState.CancellationUnconfirmed
                                )
                        ) {
                            finishPayment(owner)
                        }
                    }
                    else -> Unit
                }
                (_paymentState.value as? PaymentState.Pending)?.owner === owner
            }.collect {}
        }
    }

    internal fun beginPayment(cancel: suspend () -> Boolean): PaymentOwner? {
        if (isPaymentPending) return null
        return PaymentOwner(cancel).also { _paymentState.value = PaymentState.Active(it) }
    }

    private fun beginControllerPayment(controller: CardReaderPaymentController): PaymentOwner? {
        lateinit var owner: PaymentOwner
        owner = beginPayment {
            controller.cancelForLock().also {
                if (controller.paymentState.value is CardReaderPaymentState.PaymentSuccessful) {
                    finishPayment(owner, successful = true)
                }
            }
        } ?: return null
        return owner
    }

    internal fun finishPayment(owner: PaymentOwner, successful: Boolean = false) {
        owner.successful = owner.successful || successful
        when (val current = _paymentState.value) {
            is PaymentState.Cancelling -> if (current.owner === owner) {
                _paymentState.value = current.copy(finished = true)
            }
            is PaymentState.CancellationUnconfirmed -> if (current.owner === owner) {
                _paymentState.value = current.copy(finished = true)
            }
            is PaymentState.Pending -> if (current.owner === owner) release(owner)
            PaymentState.Idle -> Unit
        }
    }

    internal fun markUnresolved(owner: PaymentOwner) {
        val current = _paymentState.value as? PaymentState.Pending ?: return
        if (current.owner !== owner) return
        when (current) {
            is PaymentState.Cancelling, is PaymentState.CancellationUnconfirmed -> Unit
            else -> _paymentState.value = PaymentState.Unresolved(owner)
        }
    }

    internal fun markCancellationUnconfirmed(owner: PaymentOwner) {
        val current = _paymentState.value as? PaymentState.Cancelling ?: return
        if (current.owner === owner) _paymentState.value = PaymentState.CancellationUnconfirmed(owner, current.finished)
    }

    suspend fun cancelPayment(): CancellationResult {
        val current = _paymentState.value as? PaymentState.Pending ?: return CancellationResult.CANCELLED
        if (current is PaymentState.Cancelling) return CancellationResult.UNRESOLVED
        val owner = current.owner
        _paymentState.value = PaymentState.Cancelling(
            owner, finished = (current as? PaymentState.CancellationUnconfirmed)?.finished == true,
        )
        owner.cancellation = scope.async { cancelOwnedPayment(owner) }
        owner.cancellation?.await()
        return when {
            isPaymentPending -> CancellationResult.UNRESOLVED
            owner.successful -> CancellationResult.COMPLETED
            else -> CancellationResult.CANCELLED
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun cancelOwnedPayment(owner: PaymentOwner) {
        var cancelled = false
        try {
            cancelled = owner.cancel()
        } catch (error: CancellationException) {
            markCancellationUnconfirmed(owner)
            throw error
        } catch (_: Exception) {
            // The reader must confirm cancellation before another collection can start.
        } finally {
            val pending = _paymentState.value as? PaymentState.Cancelling
            if (pending?.owner === owner) {
                if (cancelled || pending.finished) {
                    release(
                        owner
                    )
                } else {
                    _paymentState.value = PaymentState.Unresolved(owner)
                }
            }
        }
    }

    private fun release(owner: PaymentOwner) {
        owner.observer?.cancel()
        _paymentState.value = PaymentState.Idle
    }

    class PaymentOwner internal constructor(internal val cancel: suspend () -> Boolean) {
        internal var observer: Job? = null
        internal var cancellation: Deferred<Unit>? = null
        internal var successful = false
    }

    enum class CancellationResult { CANCELLED, COMPLETED, UNRESOLVED }

    sealed interface PaymentState {
        data object Idle : PaymentState
        sealed interface Pending : PaymentState { val owner: PaymentOwner }
        data class Active(override val owner: PaymentOwner) : Pending
        data class Cancelling(override val owner: PaymentOwner, val finished: Boolean) : Pending
        data class Unresolved(override val owner: PaymentOwner) : Pending
        data class CancellationUnconfirmed(override val owner: PaymentOwner, val finished: Boolean) : Pending
    }

    fun cancelReconnection() {
        cardReaderManager.cancelReconnection()
    }

    companion object {
        internal const val PAYMENT_PROCESS_ID_KEY = "woo_pos_payment_process_id"
    }
}
