package com.woocommerce.android.cardreader.remote

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class CardReaderRemoteProtocolTest {
    private val protocol = CardReaderRemoteProtocol()

    @Test
    fun `given a ConnectRequest, when written and read back, then fields match`() {
        // GIVEN
        val original = CardReaderRemoteMessage.ConnectRequest(
            requestId = "req-1",
            connectionToken = "tok",
            locationId = "loc-xyz",
        )

        // WHEN
        val decoded = roundTrip(original)

        // THEN
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `given a ConnectAck, when written and read back, then fields match`() {
        // GIVEN
        val original = CardReaderRemoteMessage.ConnectAck(
            requestId = "req-2",
            readerSerial = "SN-42",
        )

        // WHEN
        val decoded = roundTrip(original)

        // THEN
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `given a CollectPaymentRequest, when written and read back, then fields match`() {
        // GIVEN
        val original = CardReaderRemoteMessage.CollectPaymentRequest(
            requestId = "req-3",
            paymentIntentClientSecret = "pi_client_secret",
        )

        // WHEN
        val decoded = roundTrip(original)

        // THEN
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `given a PaymentIntentResult, when written and read back, then fields match`() {
        // GIVEN
        val original = CardReaderRemoteMessage.PaymentIntentResult(
            requestId = "req-4",
            paymentIntentId = "pi_123",
            status = "requires_capture",
        )

        // WHEN
        val decoded = roundTrip(original)

        // THEN
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `given an ErrorMessage, when written and read back, then fields match`() {
        // GIVEN
        val original = CardReaderRemoteMessage.ErrorMessage(
            requestId = "req-5",
            code = "timeout",
            description = "Reader did not respond in time",
        )

        // WHEN
        val decoded = roundTrip(original)

        // THEN
        assertThat(decoded).isEqualTo(original)
    }

    private fun roundTrip(msg: CardReaderRemoteMessage): CardReaderRemoteMessage {
        val buffer = ByteArrayOutputStream()
        protocol.write(DataOutputStream(buffer), msg)
        return protocol.read(DataInputStream(ByteArrayInputStream(buffer.toByteArray())))
    }
}
