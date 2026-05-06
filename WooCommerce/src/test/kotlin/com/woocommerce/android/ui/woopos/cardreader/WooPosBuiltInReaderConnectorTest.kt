package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository.LocationIdFetchingResult
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingState
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class WooPosBuiltInReaderConnectorTest {
    private val locationRepository: CardReaderLocationRepository = mock()
    private val onboardingChecker: CardReaderOnboardingChecker = mock()
    private val developerOptionsRepository: DeveloperOptionsRepository = mock {
        on { isSimulatedCardReaderEnabled() } doReturn false
    }
    private val logger: WooPosLogWrapper = mock()
    private val readerStatus = MutableStateFlow<CardReaderStatus>(CardReaderStatus.NotConnected())
    private val cardReaderManager: CardReaderManager = mock {
        on { this.readerStatus } doReturn readerStatus
    }
    private val resourceProvider: ResourceProvider = mock {
        on { isDarkMode() } doReturn false
    }
    private val fineLocationPermissionCheck: WooPosFineLocationPermissionCheck = mock {
        on { isGranted() } doReturn true
    }

    private val sut = WooPosBuiltInReaderConnector(
        cardReaderManager,
        locationRepository,
        onboardingChecker,
        developerOptionsRepository,
        resourceProvider,
        fineLocationPermissionCheck,
        logger,
    )

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        whenever(onboardingChecker.getOnboardingState()).thenReturn(
            CardReaderOnboardingState.OnboardingCompleted(
                preferredPlugin = PluginType.WOOCOMMERCE_PAYMENTS,
                version = null,
                countryCode = "US",
            )
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given reader already connected, when connect, then returns success without discovering`() = runTest {
        readerStatus.value = CardReaderStatus.Connected(mock())

        val result = sut.connect()

        assertThat(result.isSuccess).isTrue()
        verify(cardReaderManager, never()).discoverReaders(any(), any())
    }

    @Test
    fun `given fine location permission denied, when connect, then returns failure with permission exception`() = runTest {
        whenever(fineLocationPermissionCheck.isGranted()).thenReturn(false)

        val result = sut.connect()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(MissingFineLocationPermissionException::class.java)
        verify(cardReaderManager, never()).discoverReaders(any(), any())
    }

    @Test
    fun `given location fetch fails, when connect, then returns failure`() = runTest {
        whenever(locationRepository.getDefaultLocationId(any()))
            .thenReturn(LocationIdFetchingResult.Error.MissingAddress("plugin"))

        val result = sut.connect()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given no built-in reader discovered, when connect, then returns failure`() = runTest {
        whenever(locationRepository.getDefaultLocationId(any()))
            .thenReturn(LocationIdFetchingResult.Success("loc"))
        whenever(cardReaderManager.discoverReaders(any(), any()))
            .thenReturn(flowOf(CardReaderDiscoveryEvents.Failed("nope")))

        val result = sut.connect()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given reader discovered and connection succeeds, when connect, then returns success`() = runTest {
        val discoveredReader: CardReader = mock { on { id } doReturn "tap-to-pay" }
        whenever(locationRepository.getDefaultLocationId(any()))
            .thenReturn(LocationIdFetchingResult.Success("loc"))
        whenever(cardReaderManager.discoverReaders(any(), any()))
            .thenReturn(flowOf(CardReaderDiscoveryEvents.ReadersFound(listOf(discoveredReader))))
        whenever(cardReaderManager.startConnectionToReader(discoveredReader, "loc")).then {
            readerStatus.value = CardReaderStatus.Connected(mock())
        }

        val result = sut.connect()

        assertThat(result.isSuccess).isTrue()
        verify(cardReaderManager).startConnectionToReader(discoveredReader, "loc")
    }
}
