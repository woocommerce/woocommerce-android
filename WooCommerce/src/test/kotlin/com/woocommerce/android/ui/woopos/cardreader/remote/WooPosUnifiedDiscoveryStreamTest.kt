package com.woocommerce.android.ui.woopos.cardreader.remote

import app.cash.turbine.test
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReader
import com.woocommerce.android.cardreader.connection.CardReaderDiscoveryEvents
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover
import com.woocommerce.android.cardreader.connection.CardReaderTypesToDiscover.SpecificReaders.ExternalReaders
import com.woocommerce.android.cardreader.connection.ReaderType.ExternalReader.Chipper2X
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.util.siteIdHash
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.net.InetAddress

@ExperimentalCoroutinesApi
class WooPosUnifiedDiscoveryStreamTest {
    private val cardReaderManager: CardReaderManager = mock()
    private val remoteDiscovery: WooPosRemoteReaderDiscovery = mock()
    private val simulatedRemoteDiscovery: WooPosSimulatedRemoteReaderDiscovery = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { siteId = TABLET_SITE_ID })
    }
    private val logger: WooPosLogWrapper = mock()

    private val types: CardReaderTypesToDiscover = ExternalReaders(listOf(Chipper2X))

    @Test
    fun `when bluetooth and nsd emit, then both readers surface in the snapshot`() = runTest {
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
        val sut = WooPosUnifiedDiscoveryStream(
            cardReaderManager,
            remoteDiscovery,
            simulatedRemoteDiscovery,
            selectedSite,
            logger,
        )

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
    fun `when nsd reports a phone lost, then the phone is removed from the snapshot`() = runTest {
        // GIVEN
        whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
            flowOf(CardReaderDiscoveryEvents.Started)
        )
        val phone = phone(name = "Pixel 7")
        whenever(remoteDiscovery.discover()).thenReturn(
            flowOf(
                WooPosPhoneDiscoveryEvent.Added(phone),
                WooPosPhoneDiscoveryEvent.Removed(phone.serviceName),
            )
        )
        val sut = WooPosUnifiedDiscoveryStream(
            cardReaderManager,
            remoteDiscovery,
            simulatedRemoteDiscovery,
            selectedSite,
            logger,
        )

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

    @Test
    fun `given phone whose display name differs from service name, when removed by service name, then phone is dropped`() =
        runTest {
            // GIVEN
            whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
                flowOf(CardReaderDiscoveryEvents.Started)
            )
            val phone = phone(name = "Pixel 8", serviceName = "woopos-remote-a3f4")
            whenever(remoteDiscovery.discover()).thenReturn(
                flowOf(
                    WooPosPhoneDiscoveryEvent.Added(phone),
                    WooPosPhoneDiscoveryEvent.Removed("woopos-remote-a3f4"),
                )
            )
            val sut = WooPosUnifiedDiscoveryStream(
                cardReaderManager,
                remoteDiscovery,
                simulatedRemoteDiscovery,
                selectedSite,
                logger,
            )

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

    @Test
    fun `given phone advertises different site hash, when discovered, then phone is not surfaced`() = runTest {
        // GIVEN
        whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
            flowOf(CardReaderDiscoveryEvents.Started)
        )
        val mismatchedPhone = phone(name = "Pixel 7", siteHash = siteIdHash(OTHER_SITE_ID))
        whenever(remoteDiscovery.discover()).thenReturn(
            flowOf(WooPosPhoneDiscoveryEvent.Added(mismatchedPhone))
        )
        val sut = WooPosUnifiedDiscoveryStream(
            cardReaderManager,
            remoteDiscovery,
            simulatedRemoteDiscovery,
            selectedSite,
            logger,
        )

        // WHEN / THEN
        sut.discover(isSimulated = false, cardReaderTypesToDiscover = types).test {
            assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Started)
            awaitComplete()
        }
    }

    @Test
    fun `given phone advertises matching site hash, when discovered, then phone is surfaced`() = runTest {
        // GIVEN
        whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
            flowOf(CardReaderDiscoveryEvents.Started)
        )
        val matchingPhone = phone(name = "Pixel 7", siteHash = siteIdHash(TABLET_SITE_ID))
        whenever(remoteDiscovery.discover()).thenReturn(
            flowOf(WooPosPhoneDiscoveryEvent.Added(matchingPhone))
        )
        val sut = WooPosUnifiedDiscoveryStream(
            cardReaderManager,
            remoteDiscovery,
            simulatedRemoteDiscovery,
            selectedSite,
            logger,
        )

        // WHEN / THEN
        sut.discover(isSimulated = false, cardReaderTypesToDiscover = types).test {
            assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Started)
            val found = awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound
            assertThat(found.readers).containsExactly(matchingPhone)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given phone re-advertises after a session restart, when discovered, then only the new address is surfaced`() =
        runTest {
            // GIVEN
            whenever(cardReaderManager.discoverReaders(false, types)).thenReturn(
                flowOf(CardReaderDiscoveryEvents.Started)
            )
            val previousSession = phone(
                name = "Pixel 7",
                serviceName = "woopos-remote-2416",
                fingerprintBase64 = "AB4F",
                deviceId = "device-1",
                port = 35579,
            )
            val newSession = phone(
                name = "Pixel 7",
                serviceName = "woopos-remote-9c31",
                fingerprintBase64 = "CD8E",
                deviceId = "device-1",
                port = 35401,
                host = InetAddress.getByName("192.168.31.235"),
            )
            whenever(remoteDiscovery.discover()).thenReturn(
                flowOf(
                    WooPosPhoneDiscoveryEvent.Added(previousSession),
                    WooPosPhoneDiscoveryEvent.Added(newSession),
                )
            )
            val sut = WooPosUnifiedDiscoveryStream(
                cardReaderManager,
                remoteDiscovery,
                simulatedRemoteDiscovery,
                selectedSite,
                logger,
            )

            // WHEN / THEN
            sut.discover(isSimulated = false, cardReaderTypesToDiscover = types).test {
                assertThat(awaitItem()).isEqualTo(WooPosUnifiedDiscoveryEvent.Started)
                assertThat((awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound).readers)
                    .containsExactly(previousSession)
                assertThat((awaitItem() as WooPosUnifiedDiscoveryEvent.ReadersFound).readers)
                    .containsExactly(newSession)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun phone(
        name: String,
        siteHash: String = siteIdHash(TABLET_SITE_ID),
        serviceName: String = "woopos-remote-${name.hashCode().toString(16)}",
        fingerprintBase64: String = "AB4F",
        deviceId: String = "device-${name.hashCode().toString(16)}",
        port: Int = 9000,
        host: InetAddress = InetAddress.getLoopbackAddress(),
    ) = WooPosDiscoveredReader.Phone(
        serviceName = serviceName,
        deviceId = deviceId,
        name = name,
        host = host,
        port = port,
        fingerprintBase64 = fingerprintBase64,
        siteHash = siteHash,
    )

    private companion object {
        const val TABLET_SITE_ID = 123L
        const val OTHER_SITE_ID = 456L
    }
}
