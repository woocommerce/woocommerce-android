package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.remote.CollectPaymentOutcome
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.InetAddress

@ExperimentalCoroutinesApi
class WooPosRemoteReaderPaymentFlowTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val connectedPhone = WooPosDiscoveredReader.Phone(
        name = "Pixel 7",
        host = InetAddress.getByName("127.0.0.1"),
        port = 4444,
        fingerprintBase64 = "AAAA1234AB4F",
    )

    private val session: WooPosRemoteReaderSession = mock {
        on { state }.thenReturn(
            MutableStateFlow(
                WooPosRemoteReaderSession.State.Connected(reader = connectedPhone, readerSerial = "SN-1")
            )
        )
    }
    private val cardReaderStore: CardReaderStore = mock()
    private val flow = WooPosRemoteReaderPaymentFlow(
        session = session,
        cardReaderStore = cardReaderStore,
        logger = mock<WooPosLogWrapper>(),
    )

    @Test
    fun `given PaymentIntentResult with requires_capture, when collectAndCapture, then capture path is invoked`() =
        runTest {
            // GIVEN
            whenever(session.sendCollectPayment(any(), any())).thenReturn(
                CollectPaymentOutcome.Success(paymentIntentId = "pi_123", status = "requires_capture")
            )
            whenever(cardReaderStore.capturePaymentIntent(any(), any()))
                .thenReturn(CapturePaymentResponse.Successful.Success)

            // WHEN
            val result = flow.collectAndCapture(paymentIntentClientSecret = "pi_123_secret_abc", orderId = 42L)

            // THEN
            verify(cardReaderStore).capturePaymentIntent(42L, "pi_123")
            assertThat(result).isEqualTo(WooPosRemoteReaderPaymentFlow.Result.Captured("pi_123"))
        }

    @Test
    fun `given rejected CollectPaymentOutcome, when collectAndCapture, then capture is not invoked`() = runTest {
        // GIVEN
        val rejection = CollectPaymentOutcome.Rejected(code = "card_declined", description = "Declined")
        whenever(session.sendCollectPayment(any(), any())).thenReturn(rejection)

        // WHEN
        val result = flow.collectAndCapture(paymentIntentClientSecret = "pi_123_secret_abc", orderId = 42L)

        // THEN
        verify(cardReaderStore, never()).capturePaymentIntent(any(), any())
        assertThat(result).isEqualTo(WooPosRemoteReaderPaymentFlow.Result.CollectFailed(rejection))
    }
}
