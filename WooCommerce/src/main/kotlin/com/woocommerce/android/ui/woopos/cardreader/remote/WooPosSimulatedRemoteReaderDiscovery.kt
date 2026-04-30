package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.siteIdHash
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.InetAddress
import javax.inject.Inject

class WooPosSimulatedRemoteReaderDiscovery @Inject constructor(
    private val selectedSite: SelectedSite,
) : WooPosPhoneDiscoverySource {
    override fun discover(): Flow<WooPosPhoneDiscoveryEvent> = flow {
        val siteHash = siteIdHash(selectedSite.get().remoteId().value)
        delay(FIRST_PHONE_DELAY_MS)
        emit(
            WooPosPhoneDiscoveryEvent.Added(
                WooPosDiscoveredReader.Phone(
                    serviceName = "woopos-remote-sim1",
                    name = "Simulated Pixel 7",
                    host = InetAddress.getByName("127.0.0.1"),
                    port = 9000,
                    fingerprintBase64 = "SIM1",
                    siteHash = siteHash,
                    isSimulated = true,
                )
            )
        )
        delay(SECOND_PHONE_DELAY_MS)
        emit(
            WooPosPhoneDiscoveryEvent.Added(
                WooPosDiscoveredReader.Phone(
                    serviceName = "woopos-remote-sim2",
                    name = "Simulated Galaxy S24",
                    host = InetAddress.getByName("127.0.0.2"),
                    port = 9001,
                    fingerprintBase64 = "SIM2",
                    siteHash = siteHash,
                    isSimulated = true,
                )
            )
        )
    }

    private companion object {
        const val FIRST_PHONE_DELAY_MS = 500L
        const val SECOND_PHONE_DELAY_MS = 1_000L
    }
}
