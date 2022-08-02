package com.woocommerce.android.util.crashlogging

import android.app.Application
import android.util.Base64
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.automattic.android.tracks.crashlogging.performance.PerformanceMonitoringRepositoryProvider
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.goterl.lazysodium.utils.Key
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.locale.ContextBasedLocaleProvider
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.logging.FluxCCrashLogger
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLoggingKey
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
        fun provideEncryptedLoggingKey(): EncryptedLoggingKey {
            return EncryptedLoggingKey(Key.fromBytes(Base64.decode(BuildConfig.ENCRYPTED_LOGGING_KEY, Base64.DEFAULT)))
        }

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
