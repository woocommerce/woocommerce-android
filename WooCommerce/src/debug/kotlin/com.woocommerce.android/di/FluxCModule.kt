package com.woocommerce.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.module.DebugOkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        ReleaseNetworkModule::class,
        DebugOkHttpClientModule::class
    ]
)
abstract class FluxCModule
