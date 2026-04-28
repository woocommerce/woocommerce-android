package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.InetAddress
import javax.inject.Inject

class WooPosSimulatedRemoteReaderDiscovery @Inject constructor(
    private val selectedSite: SelectedSite,
) : WooPosPhoneDiscoverySource {
    override fun discover(): Flow<WooPosPhoneDiscoveryEvent> = flow {
        val siteId = selectedSite.get().siteId
        delay(FIRST_PHONE_DELAY_MS)
        emit(
            WooPosPhoneDiscoveryEvent.Added(
                WooPosDiscoveredReader.Phone(
                    name = "Simulated Pixel 7",
                    host = InetAddress.getByName("127.0.0.1"),
                    port = 9000,
                    fingerprintBase64 = "SIM1",
                    siteId = siteId,
                    isSimulated = true,
                )
            )
        )
        delay(SECOND_PHONE_DELAY_MS)
        emit(
            WooPosPhoneDiscoveryEvent.Added(
                WooPosDiscoveredReader.Phone(
                    name = "Simulated Galaxy S24",
                    host = InetAddress.getByName("127.0.0.2"),
                    port = 9001,
                    fingerprintBase64 = "SIM2",
                    siteId = siteId,
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
