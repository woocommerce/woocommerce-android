package com.woocommerce.android.wear.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.woocommerce.android.wear.datastore.DataStoreQualifier
import com.woocommerce.android.wear.datastore.DataStoreType.LOGIN
import com.woocommerce.android.wear.datastore.DataStoreType.ORDERS
import com.woocommerce.android.wear.datastore.DataStoreType.STATS
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
    @DataStoreQualifier(LOGIN)
    fun provideLoginDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile(LOGIN.typeName) },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(STATS)
    fun provideStatsDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile(STATS.typeName) },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )

    @Provides
    @Singleton
    @DataStoreQualifier(ORDERS)
    fun provideOrdersDataStore(
        appContext: Context,
        @AppCoroutineScope appCoroutineScope: CoroutineScope
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { appContext.preferencesDataStoreFile(ORDERS.typeName) },
        scope = CoroutineScope(appCoroutineScope.coroutineContext + Dispatchers.IO)
    )
}
