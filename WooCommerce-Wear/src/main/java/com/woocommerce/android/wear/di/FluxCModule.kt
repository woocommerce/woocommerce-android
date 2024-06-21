package com.woocommerce.android.wear.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.di.WCDatabaseModule
import org.wordpress.android.fluxc.module.OkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        ReleaseNetworkModule::class,
        OkHttpClientModule::class,
        WCDatabaseModule::class
    ]
)
abstract class FluxCModule
