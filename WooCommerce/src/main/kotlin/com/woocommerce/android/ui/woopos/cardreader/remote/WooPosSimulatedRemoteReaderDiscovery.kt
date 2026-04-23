package com.woocommerce.android.ui.woopos.cardreader.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.InetAddress
import javax.inject.Inject

class WooPosSimulatedRemoteReaderDiscovery @Inject constructor() : WooPosPhoneDiscoverySource {
    override fun discover(): Flow<WooPosPhoneDiscoveryEvent> = flow {
        delay(FIRST_PHONE_DELAY_MS)
        emit(WooPosPhoneDiscoveryEvent.Added(simulatedPhones[0]))
        delay(SECOND_PHONE_DELAY_MS)
        emit(WooPosPhoneDiscoveryEvent.Added(simulatedPhones[1]))
    }

    private companion object {
        const val FIRST_PHONE_DELAY_MS = 500L
        const val SECOND_PHONE_DELAY_MS = 1_000L

        val simulatedPhones = listOf(
            WooPosDiscoveredReader.Phone(
                name = "Simulated Pixel 7",
                host = InetAddress.getLoopbackAddress(),
                port = 9000,
                fingerprintBase64 = "SIM1",
            ),
            WooPosDiscoveredReader.Phone(
                name = "Simulated Galaxy S24",
                host = InetAddress.getLoopbackAddress(),
                port = 9001,
                fingerprintBase64 = "SIM2",
            ),
        )
    }
}
