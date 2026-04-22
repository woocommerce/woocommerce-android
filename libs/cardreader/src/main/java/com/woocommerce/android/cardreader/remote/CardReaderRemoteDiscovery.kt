package com.woocommerce.android.cardreader.remote

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.net.InetAddress

interface CardReaderRemoteDiscovery {
    fun discover(): Flow<DiscoveredRemoteReader>

    companion object {
        fun create(context: Context): CardReaderRemoteDiscovery =
            DefaultCardReaderRemoteDiscovery(CardReaderRemoteNsd(context))
    }
}

data class DiscoveredRemoteReader(
    val name: String,
    val host: InetAddress,
    val port: Int,
    val fingerprintBase64: String,
)

internal class DefaultCardReaderRemoteDiscovery(
    private val nsd: CardReaderRemoteNsd,
) : CardReaderRemoteDiscovery {
    override fun discover(): Flow<DiscoveredRemoteReader> =
        nsd.discover().map { it.toPublic() }
}

private fun CardReaderRemoteResolvedHost.toPublic() =
    DiscoveredRemoteReader(
        name = name,
        host = host,
        port = port,
        fingerprintBase64 = fingerprintBase64,
    )
