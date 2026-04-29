package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.cardreader.CardReaderStore
import com.woocommerce.android.cardreader.remote.CardReaderRemoteTabletClient
import com.woocommerce.android.cardreader.remote.ConnectOutcome
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository.LocationIdFetchingResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.InetAddress

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRemoteReaderSessionTest {
    @Rule @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val cardReaderStore: CardReaderStore = mock()
    private val locationRepository: CardReaderLocationRepository = mock()
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()
    private val client: CardReaderRemoteTabletClient = mock {
        on { connectionClosed }.thenReturn(MutableStateFlow(false))
    }
    private val clientProvider: WooPosRemoteReaderClientProvider = mock {
        on { create() }.thenReturn(client)
    }
    private val logger: WooPosLogWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) }.thenReturn("")
    }

    @Test
    fun `given simulated reader, when connect, then state is Connected`() = runTest {
        // GIVEN
        val session = createSession()
        val phone = phone(isSimulated = true)

        // WHEN
        val state = session.connect(phone)
        advanceUntilIdle()

        // THEN
        assertThat(state).isInstanceOf(WooPosRemoteReaderSession.State.Connected::class.java)
        assertThat(session.state.value).isInstanceOf(WooPosRemoteReaderSession.State.Connected::class.java)
    }

    @Test
    fun `given fetchConnectionToken returns blank, when connect, then state is Failed`() = runTest {
        // GIVEN
        whenever(cardReaderStore.fetchConnectionToken()).thenReturn("")
        val session = createSession()

        // WHEN
        val state = session.connect(phone(isSimulated = false))

        // THEN
        assertThat(state).isInstanceOf(WooPosRemoteReaderSession.State.Failed::class.java)
    }

    @Test
    fun `given client connect succeeds, when connect, then state is Connected`() = runTest {
        // GIVEN
        whenever(cardReaderStore.fetchConnectionToken()).thenReturn("tok")
        whenever(cardReaderOnboardingChecker.getOnboardingState()).thenReturn(
            CardReaderOnboardingState.OnboardingCompleted(
                preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
                version = null,
                countryCode = "US",
            )
        )
        whenever(locationRepository.getDefaultLocationId(any())).thenReturn(
            LocationIdFetchingResult.Success("loc_123")
        )
        whenever(client.connect(any(), any(), any(), any())).thenReturn(
            ConnectOutcome.Success(readerSerial = "S1")
        )
        val session = createSession()

        // WHEN
        val state = session.connect(phone(isSimulated = false))

        // THEN
        assertThat(state).isInstanceOf(WooPosRemoteReaderSession.State.Connected::class.java)
    }

    @Test
    fun `when disconnect, then state is Idle`() = runTest {
        // GIVEN
        val session = createSession()

        // WHEN
        session.disconnect()

        // THEN
        assertThat(session.state.value).isEqualTo(WooPosRemoteReaderSession.State.Idle)
    }

    private fun createSession() = WooPosRemoteReaderSession(
        cardReaderStore = cardReaderStore,
        locationRepository = locationRepository,
        cardReaderOnboardingChecker = cardReaderOnboardingChecker,
        clientProvider = clientProvider,
        logger = logger,
        resourceProvider = resourceProvider,
    )

    private fun phone(isSimulated: Boolean) = WooPosDiscoveredReader.Phone(
        name = "Pixel",
        host = InetAddress.getLoopbackAddress(),
        port = 9000,
        fingerprintBase64 = "AB4F",
        isSimulated = isSimulated,
    )
}
