package com.woocommerce.android.ui.woopos.cardreader.connection

import android.content.Context
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.ui.payments.cardreader.connect.CardReaderLocationRepository
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderOnboardingChecker
import com.woocommerce.android.ui.payments.tracking.CardReaderTrackingInfoKeeper
import com.woocommerce.android.ui.payments.tracking.PaymentsFlowTracker
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosDiscoveredReader
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosRemoteReaderSession
import com.woocommerce.android.ui.woopos.cardreader.remote.WooPosUnifiedDiscoveryStream
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.util.LocationUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.net.InetAddress

@ExperimentalCoroutinesApi
class WooPosCardReaderConnectionControllerTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val cardReaderManager: CardReaderManager = mock()
    private val locationRepository: CardReaderLocationRepository = mock()
    private val cardReaderOnboardingChecker: CardReaderOnboardingChecker = mock()
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()
    private val context: Context = mock()
    private val locationUtils: LocationUtils = mock()
    private val logger: WooPosLogWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val tracker: PaymentsFlowTracker = mock()
    private val cardReaderTrackingInfoKeeper: CardReaderTrackingInfoKeeper = mock()
    private val onboardingErrorMapper: WooPosOnboardingErrorMapper = mock()
    private val unifiedDiscoveryStream: WooPosUnifiedDiscoveryStream = mock()
    private val remoteReaderSession: WooPosRemoteReaderSession = mock()
    private val wooPosAnalyticsTracker: WooPosAnalyticsTracker = mock()

    private fun createController(scope: TestScope) = WooPosCardReaderConnectionController(
        cardReaderManager = cardReaderManager,
        locationRepository = locationRepository,
        cardReaderOnboardingChecker = cardReaderOnboardingChecker,
        developerOptionsRepository = developerOptionsRepository,
        dispatchers = coroutineTestRule.testDispatchers,
        scope = scope,
        context = context,
        locationUtils = locationUtils,
        logger = logger,
        appPrefsWrapper = appPrefsWrapper,
        tracker = tracker,
        cardReaderTrackingInfoKeeper = cardReaderTrackingInfoKeeper,
        onboardingErrorMapper = onboardingErrorMapper,
        unifiedDiscoveryStream = unifiedDiscoveryStream,
        remoteReaderSession = remoteReaderSession,
        wooPosAnalyticsTracker = wooPosAnalyticsTracker,
    )

    @Test
    fun `given scanning state, when explainer is shown, then explainer shown event tracked`() = runTest {
        // GIVEN
        val controller = createController(this)

        // WHEN
        controller.showRemoteTapToPayExplainer()
        advanceUntilIdle()

        // THEN
        verify(wooPosAnalyticsTracker).track(eq(WooPosAnalyticsEvent.Event.RemoteTapToPayExplainerShown))
    }

    @Test
    fun `given the phone was re-advertised on a new port, when refreshing its address, then the new entry is used`() {
        // GIVEN
        val previousSession = phone(deviceId = "device-1", port = 35579)
        val newSession = phone(deviceId = "device-1", port = 35401, fingerprintBase64 = "CD8E")

        // WHEN
        val refreshed = listOf(newSession).refreshAddressOf(previousSession)

        // THEN
        assertThat(refreshed).isEqualTo(newSession)
    }

    @Test
    fun `given the phone has not been re-advertised, when refreshing its address, then the known entry is kept`() {
        // GIVEN
        val knownPhone = phone(deviceId = "device-1", port = 35579)
        val anotherPhone = phone(deviceId = "device-2", port = 40100)

        // WHEN
        val refreshed = listOf(anotherPhone).refreshAddressOf(knownPhone)

        // THEN
        assertThat(refreshed).isEqualTo(knownPhone)
    }

    private fun phone(
        deviceId: String,
        port: Int,
        fingerprintBase64: String = "AB4F",
    ) = WooPosDiscoveredReader.Phone(
        serviceName = "woopos-remote-$fingerprintBase64",
        deviceId = deviceId,
        name = "Pixel 7",
        host = InetAddress.getLoopbackAddress(),
        port = port,
        fingerprintBase64 = fingerprintBase64,
        siteHash = "site-hash",
    )
}
