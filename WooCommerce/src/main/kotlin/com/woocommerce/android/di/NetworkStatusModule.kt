package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.tools.NetworkStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class NetworkStatusModule {
    @Provides
    @Singleton
    fun provideNetworkStatus(context: Context) = NetworkStatus(context)
}
