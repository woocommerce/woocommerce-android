package com.woocommerce.android.aiassistant.headless

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.di.WCDatabaseModule
import org.wordpress.android.fluxc.module.MediaModule
import org.wordpress.android.fluxc.module.OkHttpClientModule

@Module(
    includes = [
        WooAiSmokeRobolectricNetworkModule::class,
        OkHttpClientModule::class,
        WCDatabaseModule::class,
        MediaModule::class,
    ],
)
@InstallIn(SingletonComponent::class)
abstract class WooAiSmokeFeatureRobolectricModule
