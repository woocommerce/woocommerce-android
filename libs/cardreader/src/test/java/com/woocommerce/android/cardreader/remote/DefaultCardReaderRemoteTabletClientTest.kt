package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.payments.PaymentInfo
import com.woocommerce.android.cardreader.payments.StatementDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

@ExperimentalCoroutinesApi
class DefaultCardReaderRemoteTabletClientTest : CardReaderBaseUnitTest() {
    private val tlsClient: CardReaderRemoteTlsClient = mock()
    private val logWrapper: LogWrapper = mock()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val client = DefaultCardReaderRemoteTabletClient(tlsClient, logWrapper, scope)

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var acceptedSocket: Socket? = null

    @After
    fun closeSockets() {
        runCatching { clientSocket?.close() }
        runCatching { acceptedSocket?.close() }
        runCatching { serverSocket?.close() }
    }

    @Test
    fun `given remote closes mid-connect, when connect, then Failed cause carries connect-failed message`() {
        runBlocking {
            // GIVEN
            val openedConnection = openConnectionPair()
            whenever(tlsClient.connect(any(), any(), any())).thenAnswer { openedConnection.first }
            openedConnection.second.close()

            // WHEN
            val outcome = client.connect(reader = aReader(), connectionToken = "tok", locationId = "loc")

            // THEN
            assertThat(outcome).isInstanceOf(ConnectOutcome.Failed::class.java)
            assertThat((outcome as ConnectOutcome.Failed).cause)
                .hasMessage("Could not connect to phone reader")
        }
    }

    @Test
    fun `given the phone refuses the socket, when connect, then Failed cause is not reported as a lost connection`() =
        testBlocking {
            // GIVEN
            whenever(tlsClient.connect(any(), any(), any())).thenAnswer { throw ConnectException("ECONNREFUSED") }

            // WHEN
            val outcome = client.connect(reader = aReader(), connectionToken = "tok", locationId = "loc")

            // THEN
            assertThat(outcome).isInstanceOf(ConnectOutcome.Failed::class.java)
            assertThat((outcome as ConnectOutcome.Failed).cause)
                .hasMessage("Could not connect to phone reader")
        }

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
        val outcome = client.collectPayment(paymentInfo = aPaymentInfo())

        // THEN
        assertThat(outcome).isInstanceOf(CollectPaymentOutcome.Failed::class.java)
    }

    @Test
    fun `given phone supports cancel, when collect is cancelled, then phone gets cancel with the collect request id`() {
        runBlocking {
            // GIVEN
            val phone = connectToPhone(supportsCancelPayment = true)
            val collect = launch(Dispatchers.IO) { client.collectPayment(aPaymentInfo()) }
            val request = withTimeout(WAIT_MILLIS) { phone.receive().first() }

            // WHEN
            collect.cancelAndJoin()

            // THEN
            val received = withTimeout(WAIT_MILLIS) { phone.receive().first() }
            assertThat(received).isEqualTo(CardReaderRemoteMessage.CancelPayment(request.requestId))
        }
    }

    @Test
    fun `given an older phone, when collect is cancelled, then nothing is sent to the phone`() {
        runBlocking {
            // GIVEN
            val phone = connectToPhone(supportsCancelPayment = false)
            val collect = launch(Dispatchers.IO) { client.collectPayment(aPaymentInfo()) }
            withTimeout(WAIT_MILLIS) { phone.receive().first() }

            // WHEN
            collect.cancelAndJoin()

            // THEN
            val received = withTimeoutOrNull(SILENCE_MILLIS) { phone.receive().first() }
            assertThat(received).isNull()
        }
    }

    private suspend fun connectToPhone(supportsCancelPayment: Boolean): CardReaderRemoteConnection = coroutineScope {
        val (tablet, phone) = openConnectionPair()
        whenever(tlsClient.connect(any(), any(), any())).thenAnswer { tablet }
        launch(Dispatchers.IO) {
            val request = phone.receive().first()
            phone.send(
                CardReaderRemoteMessage.ConnectAck(
                    requestId = request.requestId,
                    readerSerial = "S1",
                    supportsCancelPayment = supportsCancelPayment,
                )
            )
        }
        client.connect(reader = aReader(), connectionToken = "tok", locationId = "loc")
        phone
    }

    private fun openConnectionPair(): Pair<CardReaderRemoteConnection, CardReaderRemoteConnection> {
        val server = ServerSocket(0).also { serverSocket = it }
        val acceptThread = thread { acceptedSocket = server.accept() }
        val client = Socket("127.0.0.1", server.localPort).also { clientSocket = it }
        acceptThread.join()
        val accepted = requireNotNull(acceptedSocket)
        val openedClient = CardReaderRemoteConnection(client, mock<LogWrapper>(), ioDispatcher = Dispatchers.IO)
        val openedServer = CardReaderRemoteConnection(accepted, mock<LogWrapper>(), ioDispatcher = Dispatchers.IO)
        return openedClient to openedServer
    }

    private fun aReader() = DiscoveredRemoteReader(
        name = "Pixel",
        host = InetAddress.getLoopbackAddress(),
        port = 9000,
        fingerprintBase64 = "AB4F",
        deviceName = "Pixel",
        siteHash = "site-hash",
        deviceId = "test-device-id",
    )

    private fun aPaymentInfo() = PaymentInfo(
        paymentDescription = "desc",
        statementDescriptor = StatementDescriptor(null),
        orderId = 1L,
        amount = BigDecimal.ONE,
        currency = "USD",
        customerEmail = null,
        isPluginCanSendReceipt = false,
        customerName = null,
        storeName = null,
        siteUrl = null,
        orderKey = null,
        feeAmount = null,
        channel = null,
    )

    private companion object {
        const val WAIT_MILLIS = 5_000L
        const val SILENCE_MILLIS = 300L
    }
}
