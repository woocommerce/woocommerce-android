package com.woocommerce.android.util.crashlogging

import android.app.Application
import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.performance.PerformanceMonitoringRepositoryProvider
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.encryptedlogging.EncryptedLogging
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.locale.ContextBasedLocaleProvider
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class CrashLoggingModule {
    companion object {
        @Provides
        @Singleton
        fun provideCrashLogging(
            context: Application,
            crashLoggingDataProvider: CrashLoggingDataProvider,
            @AppCoroutineScope appScope: CoroutineScope
        ): CrashLogging {
            return CrashLoggingProvider.createInstance(context, crashLoggingDataProvider, appScope)
        }

        @Provides
        @Singleton
        fun provideEncryptedLogging(
            @ApplicationContext context: Context,
        ): EncryptedLogging =
            EncryptedLogging.getInstance(
                context,
                encryptedLoggingKey = BuildConfig.ENCRYPTED_LOGGING_KEY,
                clientSecret = BuildConfig.OAUTH_APP_SECRET,
            )

        @Provides
        fun provideFluxCCrashLogger(crashLogging: CrashLogging): FluxCCrashLogger {
            return FluxCCrashLoggerImpl(crashLogging)
        }

        @Provides
        @Singleton
        fun providePerformanceTransactionRepository(): PerformanceTransactionRepository {
            return PerformanceMonitoringRepositoryProvider.createInstance()
        }
    }

    @Binds
    abstract fun bindCrashLoggingDataProvider(dataProvider: WCCrashLoggingDataProvider): CrashLoggingDataProvider

    @Binds
    abstract fun bindLocaleProvider(contextBasedLocaleProvider: ContextBasedLocaleProvider): LocaleProvider
}
