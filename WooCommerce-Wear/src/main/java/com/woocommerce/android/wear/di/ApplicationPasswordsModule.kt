package com.woocommerce.android.wear.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsConfiguration(
        configuration: WooWearApplicationPasswordsConfiguration
    ): ApplicationPasswordsConfiguration
}

class WooWearApplicationPasswordsConfiguration @Inject constructor() : ApplicationPasswordsConfiguration {
    override val applicationName: String = ""

    override fun isEnabledForDirectAccess(): Boolean = false
    override suspend fun isEnabledForJetpackAccess(): Boolean = false
}
