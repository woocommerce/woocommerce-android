package com.woocommerce.android.di

import com.automattic.android.experimentation.ExPlat
import com.woocommerce.android.BuildConfig
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
    @Provides
    @Singleton
    fun provideExPlat(
        experimentStore: ExperimentStore,
        appLogWrapper: AppLogWrapper,
        @AppCoroutineScope appCoroutineScope: CoroutineScope,
    ) = ExPlat(
        platform = ExperimentStore.Platform.WOOCOMMERCE_ANDROID,
        experiments = emptySet(),
        experimentStore = experimentStore,
        appLogWrapper = appLogWrapper,
        coroutineScope = appCoroutineScope,
        isDebug = BuildConfig.DEBUG
    )
}
