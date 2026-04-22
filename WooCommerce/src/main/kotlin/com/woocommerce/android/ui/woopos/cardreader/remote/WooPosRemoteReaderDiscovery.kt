package com.woocommerce.android.ui.woopos.cardreader.remote

import android.content.Context
import com.woocommerce.android.cardreader.remote.CardReaderRemoteDiscovery
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class WooPosRemoteReaderDiscovery @Inject constructor(
    private val remoteDiscovery: CardReaderRemoteDiscovery,
) {
    fun discover(): Flow<WooPosDiscoveredReader.Phone> =
        remoteDiscovery.discover().map { reader ->
            WooPosDiscoveredReader.Phone(
                name = reader.name,
                host = reader.host,
                port = reader.port,
                fingerprintBase64 = reader.fingerprintBase64,
            )
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
