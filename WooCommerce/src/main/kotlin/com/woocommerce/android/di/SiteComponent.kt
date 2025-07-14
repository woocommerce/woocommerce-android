package com.woocommerce.android.di

import androidx.datastore.core.DataStore
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import dagger.BindsInstance
import dagger.hilt.DefineComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Qualifier
import javax.inject.Scope
import kotlin.annotation.AnnotationRetention.RUNTIME

@Scope
@MustBeDocumented
@Retention(value = RUNTIME)
annotation class SiteScope

@SiteScope
@DefineComponent(parent = SingletonComponent::class)
interface SiteComponent {
    @DefineComponent.Builder
    interface Builder {
        fun setSite(@BindsInstance site: SiteModel): Builder
        fun setCoroutineScope(@BindsInstance @SiteCoroutineScope scope: CoroutineScope): Builder
        fun build(): SiteComponent
    }
}

@InstallIn(SiteComponent::class)
@EntryPoint
interface SiteComponentEntryPoint {
    fun dashboardDataStore(): DataStore<DashboardDataModel>

    @SiteCoroutineScope
    fun siteCoroutineScope(): CoroutineScope
}

@Qualifier
@MustBeDocumented
@Retention(RUNTIME)
annotation class SiteCoroutineScope
