package com.woocommerce.android.ui.woopos.cardreader.remote

import android.content.Context
import com.woocommerce.android.cardreader.remote.CardReaderRemoteDiscovery
import com.woocommerce.android.cardreader.remote.RemoteReaderDiscoveryEvent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

sealed class WooPosPhoneDiscoveryEvent {
    data class Added(val phone: WooPosDiscoveredReader.Phone) : WooPosPhoneDiscoveryEvent()
    data class Removed(val name: String) : WooPosPhoneDiscoveryEvent()
}

interface WooPosPhoneDiscoverySource {
    fun discover(): Flow<WooPosPhoneDiscoveryEvent>
}

class WooPosRemoteReaderDiscovery @Inject constructor(
    private val remoteDiscovery: CardReaderRemoteDiscovery,
) : WooPosPhoneDiscoverySource {
    override fun discover(): Flow<WooPosPhoneDiscoveryEvent> =
        remoteDiscovery.discover().map { event ->
            when (event) {
                is RemoteReaderDiscoveryEvent.Added -> WooPosPhoneDiscoveryEvent.Added(
                    WooPosDiscoveredReader.Phone(
                        name = event.reader.name,
                        host = event.reader.host,
                        port = event.reader.port,
                        fingerprintBase64 = event.reader.fingerprintBase64,
                    )
                )
                is RemoteReaderDiscoveryEvent.Removed -> WooPosPhoneDiscoveryEvent.Removed(event.name)
            }
        }
}

@Module
@InstallIn(SingletonComponent::class)
object WooPosRemoteReaderDiscoveryModule {
    @Provides
    @Singleton
    fun provideCardReaderRemoteDiscovery(
        @ApplicationContext context: Context,
    ): CardReaderRemoteDiscovery = CardReaderRemoteDiscovery.create(context)
}
