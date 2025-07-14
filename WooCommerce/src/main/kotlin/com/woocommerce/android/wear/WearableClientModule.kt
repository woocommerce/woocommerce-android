package com.woocommerce.android.wear

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.Wearable
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class WearableClientModule {
    @Provides
    @Singleton
    fun provideDataClient(
        appContext: Context
    ): DataClient = Wearable.getDataClient(appContext)
}
