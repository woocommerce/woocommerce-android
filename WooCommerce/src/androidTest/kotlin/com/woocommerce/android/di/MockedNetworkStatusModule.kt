package com.woocommerce.android.di

import android.content.Context
import com.nhaarman.mockito_kotlin.spy
import com.woocommerce.android.tools.NetworkStatus
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object MockedNetworkStatusModule {
    @JvmStatic
    @Provides
    @Singleton
    fun provideNetworkStatus(context: Context): NetworkStatus {
        return spy(NetworkStatus(context))
    }
}
