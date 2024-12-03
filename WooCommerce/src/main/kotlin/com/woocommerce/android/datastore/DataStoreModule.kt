package com.woocommerce.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.datastore.DataStoreType.ANALYTICS_CONFIGURATION
import com.woocommerce.android.datastore.DataStoreType.ANALYTICS_UI_CACHE
import com.woocommerce.android.datastore.DataStoreType.COUPONS
import com.woocommerce.android.datastore.DataStoreType.DASHBOARD_STATS
import com.woocommerce.android.datastore.DataStoreType.LAST_UPDATE
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
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("tracker")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${TRACKER.name}")
            emptyPreferences()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(ANALYTICS_UI_CACHE)
    fun provideAnalyticsDataStore(
        appContext: Context,
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("analytics")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${ANALYTICS_UI_CACHE.name}")
            emptyPreferences()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(ANALYTICS_CONFIGURATION)
    fun provideAnalyticsConfigurationDataStore(
        appContext: Context,
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("analytics_configuration")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${ANALYTICS_CONFIGURATION.name}")
            emptyPreferences()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(DASHBOARD_STATS)
    fun provideCustomDateRangeDataStore(
        appContext: Context,
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<CustomDateRange> = DataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("custom_date_range_configuration")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${DASHBOARD_STATS.name}")
            CustomDateRange.getDefaultInstance()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = CustomDateRangeSerializer
    )

    @Provides
    @Singleton
    @DataStoreQualifier(TOP_PERFORMER_PRODUCTS)
    fun provideTopPerformersCustomDateRangeDataStore(
        appContext: Context,
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<CustomDateRange> = DataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("top_performers_custom_date_range_configuration")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${TOP_PERFORMER_PRODUCTS.name}")
            CustomDateRange.getDefaultInstance()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = CustomDateRangeSerializer
    )

    @Provides
    @Singleton
    @DataStoreQualifier(COUPONS)
    fun provideCouponsCustomDateRangeDataStore(
        appContext: Context,
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<CustomDateRange> = DataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("dashboard_coupons_custom_date_range_configuration")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${COUPONS.name}")
            CustomDateRange.getDefaultInstance()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO),
        serializer = CustomDateRangeSerializer
    )

    @Provides
    @Singleton
    @DataStoreQualifier(LAST_UPDATE)
    fun provideLastUpdateDataStore(
        appContext: Context,
        crashLogging: CrashLogging,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ) = PreferenceDataStoreFactory.create(
        produceFile = {
            appContext.preferencesDataStoreFile("update")
        },
        corruptionHandler = ReplaceFileCorruptionHandler {
            crashLogging.recordEvent("Corrupted data store. DataStore Type: ${LAST_UPDATE.name}")
            emptyPreferences()
        },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )
}
