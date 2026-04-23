package com.woocommerce.android.cardreader.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.InetAddress

interface CardReaderRemoteDiscovery {
    fun discover(): Flow<RemoteReaderDiscoveryEvent>

    companion object {
        fun create(context: Context): CardReaderRemoteDiscovery =
            DefaultCardReaderRemoteDiscovery(CardReaderRemoteNsd(context))
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
)

internal class DefaultCardReaderRemoteDiscovery(
    private val nsd: CardReaderRemoteNsd,
) : CardReaderRemoteDiscovery {
    override fun discover(): Flow<RemoteReaderDiscoveryEvent> =
        nsd.discover().map { event ->
            when (event) {
                is CardReaderRemoteNsdEvent.Found -> {
                    val pairingCode = CardReaderRemoteFingerprint.pairingCodeFromBase64OrNull(
                        event.host.fingerprintBase64
                    )
                    Log.d(
                        "CardReaderRemoteDiscovery",
                        "Discovered phone ${event.host.name} fp=${event.host.fingerprintBase64} " +
                            "pairingCode=$pairingCode"
                    )
                    RemoteReaderDiscoveryEvent.Added(event.host.toPublic())
                }
                is CardReaderRemoteNsdEvent.Lost -> RemoteReaderDiscoveryEvent.Removed(event.serviceName)
            }
        }
}

private fun CardReaderRemoteResolvedHost.toPublic() =
    DiscoveredRemoteReader(
        name = name,
        host = host,
        port = port,
        fingerprintBase64 = fingerprintBase64,
        deviceName = deviceName,
    )
