package com.woocommerce.android.cardreader.remote

import android.content.Context
import com.woocommerce.android.cardreader.LogWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.InetAddress

interface CardReaderRemoteDiscovery {
    fun discover(): Flow<RemoteReaderDiscoveryEvent>

    companion object {
        fun create(context: Context, logWrapper: LogWrapper): CardReaderRemoteDiscovery =
            DefaultCardReaderRemoteDiscovery(CardReaderRemoteNsd(context), logWrapper)
    }
}

sealed class RemoteReaderDiscoveryEvent {
    data class Added(val reader: DiscoveredRemoteReader) : RemoteReaderDiscoveryEvent()
    data class Removed(val name: String) : RemoteReaderDiscoveryEvent()
}

data class DiscoveredRemoteReader(
    val name: String,
    val host: InetAddress,
    val port: Int,
    val fingerprintBase64: String,
    val deviceName: String?,
    val siteHash: String,
    val deviceId: String,
)

internal class DefaultCardReaderRemoteDiscovery(
    private val nsd: CardReaderRemoteNsd,
    private val logWrapper: LogWrapper,
) : CardReaderRemoteDiscovery {
    override fun discover(): Flow<RemoteReaderDiscoveryEvent> =
        nsd.discover().map { event ->
            when (event) {
                is CardReaderRemoteNsdEvent.Found -> {
                    val pairingCode = CardReaderRemoteFingerprint.pairingCodeFromBase64OrNull(
                        event.host.fingerprintBase64
                    )
                    logWrapper.d(
                        TAG,
                        "Discovered phone ${event.host.name} " +
                            "fp=${event.host.fingerprintBase64.takeLast(FINGERPRINT_LOG_SUFFIX_LENGTH)} " +
                            "pairingCode=$pairingCode"
                    )
                    RemoteReaderDiscoveryEvent.Added(event.host.toPublic())
                }
                is CardReaderRemoteNsdEvent.Lost -> RemoteReaderDiscoveryEvent.Removed(event.serviceName)
            }
        }

    private companion object {
        const val TAG = "CardReaderRemoteDiscovery"
        const val FINGERPRINT_LOG_SUFFIX_LENGTH = 8
    }
}

private fun CardReaderRemoteResolvedHost.toPublic() =
    DiscoveredRemoteReader(
        name = name,
        host = host,
        port = port,
        fingerprintBase64 = fingerprintBase64,
        deviceName = deviceName,
        siteHash = siteHash,
        deviceId = deviceId,
    )
