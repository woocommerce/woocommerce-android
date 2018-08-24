package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.tools.NetworkStatus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class NetworkStatusModule {
    @Provides
    @Singleton
    fun provideNetworkStatus(context: Context) = NetworkStatus(context)
}
