package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCardReaderPaymentProtectionTest {
    @get:Rule val coroutineRule = WooPosCoroutineTestRule()
    private val manager: CardReaderManager = mock {
        on { readerStatus }.thenReturn(MutableStateFlow(com.woocommerce.android.cardreader.connection.CardReaderStatus.NotConnected()))
    }
    private val flags: FeatureFlagRepository = mock()
    private val facade = WooPosCardReaderFacade(manager, flags)

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
        facade.beginPayment { cancellations++; gate.await() }
        val pending = async { facade.cancelPayment() }

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
}
