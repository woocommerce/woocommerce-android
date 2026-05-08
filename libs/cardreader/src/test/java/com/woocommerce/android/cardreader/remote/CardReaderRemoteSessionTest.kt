package com.woocommerce.android.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.LogWrapper
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.RemoteTokenChannelProvider
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.remote.CardReaderRemoteMessage.ConnectRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
            )

            // THEN
            assertThat(session.state.value)
                .isInstanceOf(CardReaderRemoteSessionState.WaitingForPayment::class.java)
        }
}
