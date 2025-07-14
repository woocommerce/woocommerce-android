package com.woocommerce.android.wear.di

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
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
    fun provideMessageClient(
        appContext: Context
    ): MessageClient = Wearable.getMessageClient(appContext)

    @Provides
    @Singleton
    fun provideCapabilityClient(
        appContext: Context
    ): CapabilityClient = Wearable.getCapabilityClient(appContext)

    @Provides
    @Singleton
    fun provideDataClient(
        appContext: Context
    ): DataClient = Wearable.getDataClient(appContext)
}
