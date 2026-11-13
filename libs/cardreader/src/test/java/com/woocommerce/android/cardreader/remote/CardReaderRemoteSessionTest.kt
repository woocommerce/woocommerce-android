package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.RemoteTokenChannelProvider
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.payments.CreatePaymentIntentResult
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class CardReaderRemoteSessionTest : CardReaderBaseUnitTest() {

    private val cardReaderManager: CardReaderManager = mock()
    private val logWrapper: LogWrapper = mock()

    @Test
    fun `given ConnectRequest arrives, when reader connect succeeds, then state transitions to WaitingForPayment`() =
        testBlocking {
            // GIVEN
            val reader: CardReader = mock { on { id } doReturn "reader-serial" }
            whenever(
                cardReaderManager.discoverReaders(any(), any())
            ).thenReturn(flowOf(CardReaderDiscoveryEvents.ReadersFound(listOf(reader))))

            val tokenProvider: RemoteTokenChannelProvider = mock()
            val connection: CardReaderRemoteConnection = mock()
            val session = CardReaderRemoteSession(
                context = mock(),
                cardReaderManager = cardReaderManager,
                logWrapper = logWrapper,
                tlsServerFactory = mock(),
                nsdFactory = mock(),
                remoteTokenProviderFactory = { tokenProvider },
            )

            // WHEN
            session.handleMessage(
                message = ConnectRequest(
                    requestId = "req-1",
                    connectionToken = "tok",
                    locationId = "loc-xyz",
                ),
                accepted = connection,
                tokenProvider = tokenProvider,
                collectQueue = CardReaderRemoteCollectQueue(this, logWrapper),
            )

            // THEN
            assertThat(session.state.value)
                .isInstanceOf(CardReaderRemoteSessionState.WaitingForPayment::class.java)
        }

    @Test
    fun `given a collect waits for a card, when the tablet cancels it, then the collect is cancelled`() = testBlocking {
        // GIVEN
        val messages = Channel<CardReaderRemoteMessage>(Channel.UNLIMITED)
        val collectWasCancelled = stubCollectThatWaitsForCard()
        val loop = launch { aSession().runProtocolLoop(aConnection(messages), mock()) }
        messages.send(aCollectRequest("collect-1"))

        // WHEN
        messages.send(CardReaderRemoteMessage.CancelPayment("collect-1"))

        // THEN
        assertThat(collectWasCancelled.isCompleted).isTrue()
        messages.close()
        loop.join()
    }

    @Test
    fun `given a collect waits for a card, when the tablet cancels another one, then the collect keeps waiting`() =
        testBlocking {
            // GIVEN
            val messages = Channel<CardReaderRemoteMessage>(Channel.UNLIMITED)
            val collectWasCancelled = stubCollectThatWaitsForCard()
            val loop = launch { aSession().runProtocolLoop(aConnection(messages), mock()) }
            messages.send(aCollectRequest("collect-1"))

            // WHEN
            messages.send(CardReaderRemoteMessage.CancelPayment("collect-0"))

            // THEN
            assertThat(collectWasCancelled.isCompleted).isFalse()
            messages.close()
            loop.join()
        }

    @Test
    fun `given a collect waits for a card, when the tablet disconnects, then the collect is cancelled`() =
        testBlocking {
            // GIVEN
            val messages = Channel<CardReaderRemoteMessage>(Channel.UNLIMITED)
            val collectWasCancelled = stubCollectThatWaitsForCard()
            val loop = launch { aSession().runProtocolLoop(aConnection(messages), mock()) }
            messages.send(aCollectRequest("collect-1"))

            // WHEN
            messages.close()
            loop.join()

            // THEN
            assertThat(collectWasCancelled.isCompleted).isTrue()
        }

    private suspend fun stubCollectThatWaitsForCard(): CompletableDeferred<Unit> {
        val cancelled = CompletableDeferred<Unit>()
        whenever(cardReaderManager.createPaymentIntent(any()))
            .thenReturn(CreatePaymentIntentResult.Success(paymentIntentId = "pi", clientSecret = "secret"))
        whenever(cardReaderManager.retrieveAndCollectPayment(any(), any())).doSuspendableAnswer {
            try {
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        return cancelled
    }

    private fun aConnection(messages: Channel<CardReaderRemoteMessage>): CardReaderRemoteConnection = mock {
        on { receive() } doReturn messages.receiveAsFlow()
    }

    private fun aSession() = CardReaderRemoteSession(
        context = mock(),
        cardReaderManager = cardReaderManager,
        logWrapper = logWrapper,
        tlsServerFactory = mock(),
        nsdFactory = mock(),
        remoteTokenProviderFactory = { mock() },
    )

    private fun aCollectRequest(requestId: String) = CardReaderRemoteMessage.CollectPaymentRequest(
        requestId = requestId,
        paymentDescription = "desc",
        statementDescriptorRaw = null,
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
        countryCode = "US",
        cardPresentCaptureMethod = null,
        terminalPaymentPreparation = null,
    )
}
