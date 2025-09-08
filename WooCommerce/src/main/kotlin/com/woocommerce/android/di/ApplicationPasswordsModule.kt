package com.woocommerce.android.di

import com.woocommerce.android.applicationpasswords.ApplicationPasswordsNotifier
import com.woocommerce.android.applicationpasswords.WooApplicationPasswordsConfiguration
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsListener(
        notifier: ApplicationPasswordsNotifier
    ): ApplicationPasswordsListener

    @Binds
    fun bindApplicationPasswordsConfiguration(
        configuration: WooApplicationPasswordsConfiguration
    ): ApplicationPasswordsConfiguration
}
