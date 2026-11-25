package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentController
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCardReaderPaymentProtectionTest {
    @get:Rule val coroutineRule = WooPosCoroutineTestRule()
    private val manager: CardReaderManager = mock {
        on {
            readerStatus
        }.thenReturn(MutableStateFlow(com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected()))
    }
    private val flags: FeatureFlagRepository = mock()
    private val facade by lazy { WooPosCardReaderFacade(manager, flags) }

    @Test
    fun `given an active payment, when a second collection starts, then it cannot acquire the reader`() {
        // GIVEN
        facade.beginPayment { false }

        // THEN
        assertThat(facade.beginPayment { true }).isNull()
        assertThat(facade.isPaymentPending).isTrue()
    }

    @Test
    fun `given failed cancellation, when cancelling again succeeds, then only confirmation releases the owner`() = runTest {
        // GIVEN
        var confirmed = false
        facade.beginPayment { confirmed }

        // WHEN
        facade.cancelPayment()

        // THEN
        assertThat(facade.paymentState.value).isInstanceOf(WooPosCardReaderFacade.PaymentState.Unresolved::class.java)
        assertThat(facade.beginPayment { true }).isNull()

        // WHEN
        confirmed = true
        facade.cancelPayment()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given success races cancellation, when its callback is pending, then another payment stays blocked`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Boolean>()
        val owner = requireNotNull(facade.beginPayment { gate.await() })
        val cancellation = async { facade.cancelPayment() }
        runCurrent()

        // WHEN
        facade.finishPayment(owner)

        // THEN
        assertThat(facade.isPaymentPending).isTrue()
        assertThat(facade.beginPayment { true }).isNull()

        // WHEN
        gate.complete(false)
        cancellation.await()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given cancellation is pending, when another background request arrives, then the owner cancels once`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Boolean>()
        var cancellations = 0
        facade.beginPayment {
            cancellations++
            gate.await()
        }
        val pending = async { facade.cancelPayment() }
        runCurrent()

        // WHEN
        facade.cancelPayment()

        // THEN
        assertThat(cancellations).isEqualTo(1)
        assertThat(facade.isPaymentPending).isTrue()
        gate.complete(true)
        pending.await()
    }

    @Test
    fun `given a newer payment, when an old success arrives, then the current owner stays active`() = runTest {
        // GIVEN
        val previous = requireNotNull(facade.beginPayment { true })
        facade.finishPayment(previous)
        val current = requireNotNull(facade.beginPayment { false })

        // WHEN
        facade.finishPayment(previous)

        // THEN
        assertThat((facade.paymentState.value as WooPosCardReaderFacade.PaymentState.Pending).owner).isSameAs(current)
    }

    @Test
    fun `given a tracked controller, when a definite failure arrives, then a new collection is allowed`() = runTest {
        // GIVEN
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(true)
        val states = MutableStateFlow<CardReaderPaymentOrRefundState>(CardReaderPaymentState.LoadingData {})
        val controller: CardReaderPaymentController = mock { on { paymentState }.thenReturn(states) }
        assertThat(facade.trackPayment(controller)).isTrue()
        runCurrent()

        // WHEN
        states.value = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.Cancelable(
            PaymentFlowError.Canceled, "", {},
        )
        runCurrent()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
        assertThat(
            facade.trackPayment(
                mock {
                    on { paymentState }.thenReturn(
                        MutableStateFlow(CardReaderPaymentState.LoadingData {})
                    )
                }
            )
        ).isTrue()
    }

    @Test
    fun `given a failed controller, when retry has not emitted a new state, then its owner stays active`() = runTest {
        // GIVEN
        whenever(flags.isEnabled(FeatureFlag.WOO_POS_ROLES)).thenReturn(true)
        val failed = CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment.Cancelable(
            PaymentFlowError.Canceled,
            "",
            {},
        )
        val states = MutableStateFlow<CardReaderPaymentOrRefundState>(failed)
        val controller: CardReaderPaymentController = mock { on { paymentState }.thenReturn(states) }

        // WHEN
        assertThat(facade.retryPayment(controller) {}).isTrue()
        runCurrent()

        // THEN
        assertThat(facade.isPaymentPending).isTrue()
        assertThat(facade.beginPayment { true }).isNull()

        // WHEN
        states.value = CardReaderPaymentState.LoadingData {}
        runCurrent()
        states.value = failed.copy(amountWithCurrencyLabel = "new attempt")
        runCurrent()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given cancellation is owned by the payment, when its UI caller ends, then confirmation still controls release`() = runTest {
        // GIVEN
        val confirmation = CompletableDeferred<Boolean>()
        facade.beginPayment { confirmation.await() }
        val caller = async { facade.cancelPayment() }
        runCurrent()

        // WHEN
        caller.cancelAndJoin()

        // THEN
        assertThat(facade.isPaymentPending).isTrue()
        assertThat(facade.beginPayment { true }).isNull()

        // WHEN
        confirmation.complete(true)
        runCurrent()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }

    @Test
    fun `given a missing cancel reply, when payment succeeds, then a later drained failure releases only that owner`() = runTest {
        // GIVEN
        var replyReceived = false
        lateinit var owner: WooPosCardReaderFacade.PaymentOwner
        owner = requireNotNull(
            facade.beginPayment {
                if (!replyReceived) facade.markCancellationUnconfirmed(owner)
                false
            }
        )
        facade.cancelPayment()

        // WHEN
        facade.finishPayment(owner)

        // THEN
        assertThat(facade.isPaymentPending).isTrue()
        assertThat(facade.beginPayment { true }).isNull()

        // WHEN
        replyReceived = true
        facade.cancelPayment()

        // THEN
        assertThat(facade.isPaymentPending).isFalse()
    }
}
