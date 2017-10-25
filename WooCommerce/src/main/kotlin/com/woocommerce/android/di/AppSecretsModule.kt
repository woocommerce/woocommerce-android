package com.woocommerce.android.di

import com.woocommerce.android.BuildConfig
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets

@Module
class AppSecretsModule {
    @Provides
    fun provideAppSecrets() = AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET)
}
