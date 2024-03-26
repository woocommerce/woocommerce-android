package com.woocommerce.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.woocommerce.android.datastore.DataStoreType.ANALYTICS_CONFIGURATION
import com.woocommerce.android.datastore.DataStoreType.ANALYTICS_UI_CACHE
import com.woocommerce.android.datastore.DataStoreType.TRACKER
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.mystore.data.CustomDateRange
import com.woocommerce.android.ui.mystore.data.CustomDateRangeSerializer
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
}
