package com.woocommerce.android.wear.di

import android.content.Context
import android.icu.text.CompactDecimalFormat
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.wear.system.ConnectionStatus
import com.woocommerce.android.wear.ui.login.LoginRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import java.util.Locale
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class AppConfigModule {
    companion object {
        private const val USER_AGENT_APPNAME = "wc-android-wear"
    }

    @Provides
    fun provideAppSecrets() = AppSecrets(BuildConfig.OAUTH_APP_ID, BuildConfig.OAUTH_APP_SECRET)

    @Provides
    fun provideUserAgent(appContext: Context) = UserAgent(appContext, USER_AGENT_APPNAME)

    @Provides
    @Singleton
    fun provideConnectionStatus(
        appContext: Context,
        loginRepository: LoginRepository
    ) = ConnectionStatus(appContext, loginRepository)

    @Provides
    fun provideDefaultLocale(): Locale = Locale.getDefault()

    @Provides
    fun provideCompactDecimalFormat(locale: Locale) = CompactDecimalFormat.getInstance(
        locale,
        CompactDecimalFormat.CompactStyle.SHORT
    )
}
