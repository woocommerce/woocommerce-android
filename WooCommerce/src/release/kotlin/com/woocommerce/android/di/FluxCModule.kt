package com.woocommerce.android.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule

@InstallIn(SingletonComponent::class)
@Module(
    includes = [
        ReleaseNetworkModule::class,
        ReleaseOkHttpClientModule::class
    ]
)
abstract class FluxCModule
