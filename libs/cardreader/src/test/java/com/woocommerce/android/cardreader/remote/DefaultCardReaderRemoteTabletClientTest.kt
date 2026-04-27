package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.payments.PaymentInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.InetAddress

@ExperimentalCoroutinesApi
class DefaultCardReaderRemoteTabletClientTest : CardReaderBaseUnitTest() {
    private val tlsClient: CardReaderRemoteTlsClient = mock()
    private val client = DefaultCardReaderRemoteTabletClient(tlsClient)

    @Test
    fun `given tls handshake throws, when connect, then returns Failed with the cause`() = testBlocking {
        // GIVEN
        val cause = IllegalStateException("handshake failed")
        whenever(tlsClient.connect(any(), any(), any())).thenAnswer { throw cause }

        // WHEN
        val outcome = client.connect(reader = aReader(), connectionToken = "tok", locationId = "loc")

        // THEN
        assertThat(outcome).isInstanceOf(ConnectOutcome.Failed::class.java)
        assertThat((outcome as ConnectOutcome.Failed).cause).hasMessage("handshake failed")
    }

    @Test
    fun `given no active connection, when collectPayment, then returns Failed`() = testBlocking {
        // WHEN
        val outcome = client.collectPayment(paymentInfo = mock<PaymentInfo>())

        // THEN
        assertThat(outcome).isInstanceOf(CollectPaymentOutcome.Failed::class.java)
    }

    private fun aReader() = DiscoveredRemoteReader(
        name = "Pixel",
        host = InetAddress.getLoopbackAddress(),
        port = 9000,
        fingerprintBase64 = "AB4F",
        deviceName = "Pixel",
    )
}
