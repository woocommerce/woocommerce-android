package com.woocommerce.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import com.woocommerce.android.di.SiteComponent
import com.woocommerce.android.di.SiteCoroutineScope
import com.woocommerce.android.di.SiteScope
import com.woocommerce.android.ui.dashboard.data.DashboardSerializer
import com.woocommerce.android.ui.mystore.data.DashboardDataModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.wordpress.android.fluxc.model.SiteModel

@Module
@InstallIn(SiteComponent::class)
object DashboardDataStoreModule {
    @Provides
    @SiteScope
    fun provideDashboardDataStore(
        appContext: Context,
        @SiteCoroutineScope siteCoroutineScope: CoroutineScope,
        site: SiteModel
    ): DataStore<DashboardDataModel> = DataStoreFactory.create(
        produceFile = {
            appContext.dataStoreFile("dashboard_configuration_${site.id}")
        },
        scope = CoroutineScope(siteCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = DashboardSerializer
    )
}
