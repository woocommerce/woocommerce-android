package com.woocommerce.android.di

import android.os.Build
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.applicationpasswords.ApplicationPasswordsUnavailableNotifier
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.module.ApplicationPasswordClientId
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsUnavailableListener

@Module
@InstallIn(SingletonComponent::class)
interface ApplicationPasswordsModule {
    @Binds
    fun bindApplicationPasswordsListener(
        notifier: ApplicationPasswordsUnavailableNotifier
    ): ApplicationPasswordsUnavailableListener

    companion object {
        @Provides
        @ApplicationPasswordClientId
        fun providesApplicationPasswordClientId() = "${BuildConfig.APPLICATION_ID}.app-client.${Build.DEVICE}"
    }
}
