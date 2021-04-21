package com.woocommerce.android.di

import android.content.Context
import com.automattic.android.tracks.CrashLogging.CrashLoggingDataProvider
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.CrashUtils
import dagger.Module
import dagger.Provides
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import java.util.Locale

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
    fun provideDefaultLocale(): Locale = Locale.getDefault()

    @Provides
    fun provideCrashUtils(): CrashLoggingDataProvider = CrashUtils

    @Provides
    fun providesAppPrefs(appContext: Context): AppPrefs {
        AppPrefs.init(appContext)
        return AppPrefs
    }
}
