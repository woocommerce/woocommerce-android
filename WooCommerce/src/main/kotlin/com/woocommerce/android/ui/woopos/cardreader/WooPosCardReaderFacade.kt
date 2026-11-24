package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.dropWhile
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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
        val owner = beginPayment(controller::cancelForLock) ?: return false
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
        val owner = beginPayment(controller::cancelForLock) ?: return false
        retry()
        observePayment(controller, owner, previous)
        return true
    }

    private fun observePayment(
        controller: CardReaderPaymentController,
        owner: PaymentOwner,
        previous: com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState? = null,
    ) {
        owner.observer = scope.launch {
            controller.paymentState.dropWhile { it === previous }.takeWhile { payment ->
                when (payment) {
                    is CardReaderPaymentState.PaymentSuccessful -> finishPayment(owner)
                    is CardReaderPaymentState.PaymentFailed -> {
                        if (payment.errorType == PaymentFlowError.CaptureOutcomeUnknown) markUnresolved(owner)
                        else if (_paymentState.value !is PaymentState.Cancelling ||
                            payment.errorType != PaymentFlowError.Canceled
                        ) finishPayment(owner)
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

    internal fun finishPayment(owner: PaymentOwner) {
        when (val current = _paymentState.value) {
            is PaymentState.Cancelling -> if (current.owner === owner) {
                _paymentState.value = current.copy(finished = true)
            }
            is PaymentState.Pending -> if (current.owner === owner) release(owner)
            PaymentState.Idle -> Unit
        }
    }

    internal fun markUnresolved(owner: PaymentOwner) {
        val current = _paymentState.value
        if (current is PaymentState.Pending && current.owner === owner && current !is PaymentState.Cancelling) {
            _paymentState.value = PaymentState.Unresolved(owner)
        }
    }

    suspend fun cancelPayment() {
        val current = _paymentState.value as? PaymentState.Pending ?: return
        if (current is PaymentState.Cancelling) return
        val owner = current.owner
        _paymentState.value = PaymentState.Cancelling(owner, finished = false)
        owner.cancellation = scope.async { cancelOwnedPayment(owner) }
        owner.cancellation?.await()
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun cancelOwnedPayment(owner: PaymentOwner) {
        var cancelled = false
        try {
            cancelled = owner.cancel()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            // The reader must confirm cancellation before another collection can start.
        } finally {
            val pending = _paymentState.value as? PaymentState.Cancelling
            if (pending?.owner === owner) {
                if (cancelled || pending.finished) release(owner) else _paymentState.value = PaymentState.Unresolved(owner)
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
    }

    sealed interface PaymentState {
        data object Idle : PaymentState
        sealed interface Pending : PaymentState { val owner: PaymentOwner }
        data class Active(override val owner: PaymentOwner) : Pending
        data class Cancelling(override val owner: PaymentOwner, val finished: Boolean) : Pending
        data class Unresolved(override val owner: PaymentOwner) : Pending
    }

    fun cancelReconnection() {
        cardReaderManager.cancelReconnection()
    }

    companion object {
        internal const val PAYMENT_PROCESS_ID_KEY = "woo_pos_payment_process_id"
    }

}
