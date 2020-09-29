package com.woocommerce.android.di

import android.content.Context
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets

@Module
class AppConfigModule {
    companion object {
        private const val USER_AGENT_APPNAME = "wc-android"
    }

    @Provides
    fun provideAppSecrets() = AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET)

    @Provides
    fun provideUserAgent(appContext: Context) = UserAgent(appContext, USER_AGENT_APPNAME)

    @Provides
    fun providesAppPrefs(appContext: Context): AppPrefs {
        AppPrefs.init(appContext)
        return AppPrefs
    }
}
