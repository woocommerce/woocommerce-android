package com.woocommerce.android.cardreader.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.mockito.kotlin.mock
import com.woocommerce.android.cardreader.LogWrapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

class CardReaderRemoteConnectionTest {
    private lateinit var serverSocket: ServerSocket
    private lateinit var clientSocket: Socket
    private lateinit var acceptedSocket: Socket

    @Before
    fun setUp() {
        serverSocket = ServerSocket(0)
        val acceptThread = thread { acceptedSocket = serverSocket.accept() }
        clientSocket = Socket("127.0.0.1", serverSocket.localPort)
        acceptThread.join()
    }

    @After
    fun tearDown() {
        runCatching { clientSocket.close() }
        runCatching { acceptedSocket.close() }
        runCatching { serverSocket.close() }
    }

    @Test
    fun `given first match consumed, when next message arrives, then second first returns without delay`() =
        runBlocking {
            // GIVEN
            val sender = CardReaderRemoteConnection(acceptedSocket, mock<LogWrapper>(), ioDispatcher = Dispatchers.IO)
            val receiver = CardReaderRemoteConnection(clientSocket, mock<LogWrapper>(), ioDispatcher = Dispatchers.IO)
            val ack = CardReaderRemoteMessage.ConnectAck(requestId = "a", readerSerial = "S1")
            val result = CardReaderRemoteMessage.PaymentIntentResult(
                requestId = "b",
                paymentIntentId = "pi",
                status = "ok",
            )

            // WHEN
            launch { sender.send(ack) }
            val first = withTimeout(FIVE_SECONDS_MILLIS) {
                receiver.receive().first { it.requestId == "a" }
            }
            launch { sender.send(result) }
            val second = withTimeout(FIVE_SECONDS_MILLIS) {
                receiver.receive().first { it.requestId == "b" }
            }

            // THEN
            assertThat(first).isEqualTo(ack)
            assertThat(second).isEqualTo(result)
            sender.close()
            receiver.close()
        }

    @Test
    fun `given socket closes, when collect is in progress, then collect completes without hanging`() =
        runBlocking {
            // GIVEN
            val sender = CardReaderRemoteConnection(acceptedSocket, mock<LogWrapper>(), ioDispatcher = Dispatchers.IO)
            val receiver = CardReaderRemoteConnection(clientSocket, mock<LogWrapper>(), ioDispatcher = Dispatchers.IO)

            // WHEN
            val collectJob = async { receiver.receive().toList() }
            sender.close()

            // THEN
            withTimeout(FIVE_SECONDS_MILLIS) { collectJob.await() }
            receiver.close()
        }

    private companion object {
        const val FIVE_SECONDS_MILLIS = 5_000L
    }
}
