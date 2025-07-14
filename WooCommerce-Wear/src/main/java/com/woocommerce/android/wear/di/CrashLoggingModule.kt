package com.woocommerce.android.wear.di

import android.app.Application
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.woocommerce.android.wear.crashlogging.FluxCCrashLoggerImpl
import com.woocommerce.android.wear.crashlogging.WCWearCrashLoggingDataProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
        fun provideFluxCCrashLogger(crashLogging: CrashLogging): FluxCCrashLogger {
            return FluxCCrashLoggerImpl(crashLogging)
        }
    }

    @Binds
    abstract fun bindCrashLoggingDataProvider(dataProvider: WCWearCrashLoggingDataProvider): CrashLoggingDataProvider
}
