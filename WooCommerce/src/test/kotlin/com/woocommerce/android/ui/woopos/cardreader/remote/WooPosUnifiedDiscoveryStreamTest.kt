package com.woocommerce.android.ui.woopos.cardreader.remote

import app.cash.turbine.test
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders.ExternalReaders
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.Chipper2X
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.net.InetAddress

@ExperimentalCoroutinesApi
class WooPosUnifiedDiscoveryStreamTest {
    private val cardReaderManager: CardReaderManager = mock()
    private val remoteDiscovery: WooPosRemoteReaderDiscovery = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val logger: WooPosLogWrapper = mock()

    private val types: CardReaderTypesToDiscover = ExternalReaders(listOf(Chipper2X))

    @Test
    fun `given flag off, when bluetooth and nsd emit, then only bluetooth readers surface`() = runTest {
        // GIVEN
        val bluetoothReader: CardReader = mock { on { id }.thenReturn("chipper-1") }
        whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
            flowOf(
                CardReaderDiscoveryEvents.Started,
                CardReaderDiscoveryEvents.ReadersFound(listOf(bluetoothReader)),
                CardReaderDiscoveryEvents.Succeeded,
            )
        )
        whenever(remoteDiscovery.discover()).thenReturn(
            flowOf(WooPosPhoneDiscoveryEvent.Added(phone(name = "Pixel 7")))
        )
        whenever(featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)).thenReturn(false)
        val sut = WooPosUnifiedDiscoveryStream(cardReaderManager, remoteDiscovery, featureFlagRepository, logger)

        // WHEN / THEN
        sut.discover(isSimulated = false, cardReaderTypesToDiscover = types).test {
            assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Started)
            val found = awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound
            assertThat(found.readers).containsExactly(WooPosDiscoveredReader.Bluetooth(bluetoothReader))
            assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Succeeded)
            awaitComplete()
        }
    }

    @Test
    fun `given flag on, when bluetooth and nsd emit, then both readers surface in the snapshot`() = runTest {
        // GIVEN
        val bluetoothReader: CardReader = mock { on { id }.thenReturn("chipper-1") }
        whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
            flowOf(
                CardReaderDiscoveryEvents.Started,
                CardReaderDiscoveryEvents.ReadersFound(listOf(bluetoothReader)),
            )
        )
        val phone = phone(name = "Pixel 7")
        whenever(remoteDiscovery.discover()).thenReturn(flowOf(WooPosPhoneDiscoveryEvent.Added(phone)))
        whenever(featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)).thenReturn(true)
        val sut = WooPosUnifiedDiscoveryStream(cardReaderManager, remoteDiscovery, featureFlagRepository, logger)

        // WHEN / THEN
        sut.discover(isSimulated = false, cardReaderTypesToDiscover = types).test {
            assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Started)
            val btOnly = awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound
            assertThat(btOnly.readers).containsExactly(WooPosDiscoveredReader.Bluetooth(bluetoothReader))
            val combined = awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound
            assertThat(combined.readers).containsExactly(
                WooPosDiscoveredReader.Bluetooth(bluetoothReader),
                phone,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given flag on, when nsd reports a phone lost, then the phone is removed from the snapshot`() = runTest {
        // GIVEN
        whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
            flowOf(CardReaderDiscoveryEvents.Started)
        )
        val phone = phone(name = "Pixel 7")
        whenever(remoteDiscovery.discover()).thenReturn(
            flowOf(
                WooPosPhoneDiscoveryEvent.Added(phone),
                WooPosPhoneDiscoveryEvent.Removed(phone.name),
            )
        )
        whenever(featureFlagRepository.isEnabled(FeatureFlag.REMOTE_TAP_TO_PAY)).thenReturn(true)
        val sut = WooPosUnifiedDiscoveryStream(cardReaderManager, remoteDiscovery, featureFlagRepository, logger)

        // WHEN / THEN
        sut.discover(isSimulated = false, cardReaderTypesToDiscover = types).test {
            assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Started)
            val withPhone = awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound
            assertThat(withPhone.readers).containsExactly(phone)
            val withoutPhone = awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound
            assertThat(withoutPhone.readers).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun phone(name: String) = WooPosDiscoveredReader.Phone(
        name = name,
        host = InetAddress.getLoopbackAddress(),
        port = 9000,
        fingerprintBase64 = "AB4F",
    )
}
