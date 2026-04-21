package com.woocommerce.android.ui.readermode

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.RemoteTokenChannelProvider
import com.woocommerce.android.cardreader.remote.RemoteReaderConnection
import com.woocommerce.android.cardreader.remote.RemoteReaderMessage.ConnectRequest
import com.woocommerce.android.ui.payments.cardreader.payment.RemoteTapToPayWaitingForPayment
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState
import com.woocommerce.android.ui.payments.cardreader.payment.remote.RemoteTapToPayReaderStateBridge
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CardReaderModeSessionTest : BaseUnitTest() {

    private val bridge: RemoteTapToPayReaderStateBridge = mock()
    private val cardReaderManager: CardReaderManager = mock()

    @Test
    fun `given ConnectRequest arrives, when reader connect succeeds, then bridge is pushed WaitingForPayment`() =
        testBlocking {
            // GIVEN
            val reader: CardReader = mock { on { id } doReturn "reader-serial" }
            whenever(
                cardReaderManager.discoverReaders(any(), any())
            ).thenReturn(flowOf(CardReaderDiscoveryEvents.ReadersFound(listOf(reader))))

            val tokenProvider: RemoteTokenChannelProvider = mock()
            val connection: RemoteReaderConnection = mock()
            val session = CardReaderModeSession(
                appContext = mock(),
                cardReaderManager = cardReaderManager,
                bridge = bridge,
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
            val captor = argumentCaptor<ViewState>()
            verify(bridge, atLeastOnce()).push(captor.capture())
            assertThat(captor.allValues).anyMatch { it is RemoteTapToPayWaitingForPayment }
        }
}
