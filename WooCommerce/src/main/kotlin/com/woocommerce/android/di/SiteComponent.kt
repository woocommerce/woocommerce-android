package com.woocommerce.android.di

import androidx.datastore.core.DataStore
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import kotlinx.coroutines.CoroutineScope
import javax.inject.Scope
import kotlin.annotation.AnnotationRetention.RUNTIME

@Scope
@MustBeDocumented
@Retention(value = RUNTIME)
annotation class SiteScope

@InstallIn(SiteComponent::class)
@EntryPoint
interface SiteComponentEntryPoint {
    fun dashboardDataStore(): DataStore<DashboardDataModel>

    @SiteCoroutineScope
    fun siteCoroutineScope(): CoroutineScope
}
