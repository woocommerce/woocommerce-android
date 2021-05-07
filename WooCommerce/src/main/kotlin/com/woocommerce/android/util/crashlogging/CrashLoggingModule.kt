package com.woocommerce.android.util.crashlogging

import android.content.Context
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import com.woocommerce.android.util.locale.ContextBasedLocaleProvider
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class CrashLoggingModule {
    companion object {
        @Provides
        @Singleton
        fun provideCrashLogging(context: Context, crashLoggingDataProvider: CrashLoggingDataProvider): CrashLogging {
            return CrashLoggingProvider.createInstance(context, crashLoggingDataProvider)
        }
    }

    @Binds
    abstract fun bindCrashLoggingDataProvider(dataProvider: WCCrashLoggingDataProvider): CrashLoggingDataProvider

    @Binds
    abstract fun bindLocaleProvider(contextBasedLocaleProvider: ContextBasedLocaleProvider): LocaleProvider
}
