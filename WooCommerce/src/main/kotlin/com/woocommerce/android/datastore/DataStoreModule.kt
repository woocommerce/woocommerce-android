package com.woocommerce.android.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.woocommerce.android.datastore.DataStoreType.TRACKER
import com.woocommerce.android.di.AppCoroutineScope
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
}
