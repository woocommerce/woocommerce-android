package com.woocommerce.android.di

import android.content.Context
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import com.woocommerce.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.io.File
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ExperimentationModule {
    @Provides
    @Singleton
    fun provideExperimentationRepository(
        @ApplicationContext appContext: Context,
        appLogWrapper: AppLogWrapper,
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
    ) = VariationsRepository.create(
        platform = "woocommerceandroid",
        experiments = emptySet(),
        failFast = BuildConfig.DEBUG,
        logger = object : ExperimentLogger {
            override fun d(message: String) = appLogWrapper.d(AppLog.T.API, message)
            override fun e(message: String, throwable: Throwable?) = appLogWrapper.e(AppLog.T.API, message, throwable)
        },
        cacheDir = File(appContext.filesDir, "experiments"),
        coroutineScope = appCoroutineScope
    )
}
