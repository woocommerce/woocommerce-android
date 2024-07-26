package com.woocommerce.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.woocommerce.android.datastore.DataStoreType.ANALYTICS_CONFIGURATION
import com.woocommerce.android.datastore.DataStoreType.ANALYTICS_UI_CACHE
import com.woocommerce.android.datastore.DataStoreType.DASHBOARD_STATS
import com.woocommerce.android.datastore.DataStoreType.TOP_PERFORMER_PRODUCTS
import com.woocommerce.android.datastore.DataStoreType.TRACKER
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.dashboard.data.CustomDateRangeSerializer
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataStoreModule {
    @Provides
    @Singleton
    @DataStoreQualifier(TRACKER)
    fun provideTrackerDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("tracker")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(ANALYTICS_UI_CACHE)
    fun provideAnalyticsDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("analytics")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(ANALYTICS_CONFIGURATION)
    fun provideAnalyticsConfigurationDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("analytics_configuration")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(DASHBOARD_STATS)
    fun provideCustomDateRangeDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<CustomDateRange> = DataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("custom_date_range_configuration")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = CustomDateRangeSerializer
    )

    @Provides
    @Singleton
    @DataStoreQualifier(TOP_PERFORMER_PRODUCTS)
    fun provideTopPerformersCustomDateRangeDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<CustomDateRange> = DataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("top_performers_custom_date_range_configuration")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = CustomDateRangeSerializer
    )

    @Provides
    @Singleton
    @DataStoreQualifier(DataStoreType.COUPONS)
    fun provideCouponsCustomDateRangeDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<CustomDateRange> = DataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("dashboard_coupons_custom_date_range_configuration")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = CustomDateRangeSerializer
    )

    @Provides
    @Singleton
    @DataStoreQualifier(DataStoreType.LAST_UPDATE)
    fun provideLastUpdateDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ) = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("update")
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )
}
