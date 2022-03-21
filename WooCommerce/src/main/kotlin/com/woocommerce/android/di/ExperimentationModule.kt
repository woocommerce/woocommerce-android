package com.woocommerce.android.di

import com.automattic.android.experimentation.ExPlat
import com.automattic.android.experimentation.Experiment
import com.woocommerce.android.BuildConfig
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.store.ExperimentStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
class ExperimentationModule {
    // TODO Suppress can be removed after the first Experiment is added
    @Suppress("UnusedPrivateMember")
    @Provides
    @Singleton
    fun provideExperiments(exPlat: ExPlat): Set<Experiment> = setOf()

    @Provides
    @Singleton
    fun provideExPlat(
        experiments: Lazy<Set<Experiment>>,
        experimentStore: ExperimentStore,
        appLogWrapper: AppLogWrapper,
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
    ) = ExPlat(
        platform = ExperimentStore.Platform.WOOCOMMERCE_ANDROID,
        experiments = experiments,
        experimentStore = experimentStore,
        appLogWrapper = appLogWrapper,
        coroutineScope = appCoroutineScope,
        isDebug = BuildConfig.DEBUG
    )
}
