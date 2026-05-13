package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.payments.PaymentInfo
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
            paymentDescription = "Order #123",
            statementDescriptorRaw = "STORE",
            orderId = 123L,
            amount = java.math.BigDecimal("12.34"),
            currency = "usd",
            customerEmail = "customer@example.com",
            isPluginCanSendReceipt = true,
            customerName = "Jane Doe",
            storeName = "Test Store",
            siteUrl = "https://example.com",
            orderKey = "wc_order_key",
            feeAmount = 15L,
            countryCode = "US",
            cardPresentCaptureMethod = PaymentInfo.CardPresentCaptureMethod.MANUAL_PREFERRED,
            terminalPaymentPreparation = PaymentInfo.TerminalPaymentPreparation.AUSTRALIA_CARD_PRESENT,
        )

        // WHEN
        val decoded = roundTrip(original)

        // THEN
        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `given legacy CollectPaymentRequest, when read back, then payment preparation is absent`() {
        // GIVEN
        val json = """
            {
              "requestId": "req-legacy",
              "paymentDescription": "Order #123",
              "statementDescriptorRaw": "STORE",
              "orderId": 123,
              "amount": 12.34,
              "currency": "usd",
              "customerEmail": "customer@example.com",
              "isPluginCanSendReceipt": true,
              "customerName": "Jane Doe",
              "storeName": "Test Store",
              "siteUrl": "https://example.com",
              "orderKey": "wc_order_key",
              "feeAmount": 15,
              "countryCode": "US",
              "type": "collect_payment"
            }
        """.trimIndent()

        // WHEN
        val decoded = readRaw(json)

        // THEN
        assertThat(decoded).isInstanceOf(CardReaderRemoteMessage.CollectPaymentRequest::class.java)
        assertThat((decoded as CardReaderRemoteMessage.CollectPaymentRequest).terminalPaymentPreparation).isNull()
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

    private fun readRaw(json: String): CardReaderRemoteMessage {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val buffer = ByteArrayOutputStream()
        DataOutputStream(buffer).use { output ->
            output.writeInt(bytes.size)
            output.write(bytes)
        }
        return protocol.read(DataInputStream(ByteArrayInputStream(buffer.toByteArray())))
    }
}
