package com.woocommerce.android.ui.woopos.common.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.ui.woopos.util.ext.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WooPosDataStoreModule {

    private val Context.lockSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "woo_pos_lock_settings"
    )

    @Provides
    @Singleton
    @DataStoreQualifier(DataStoreType.WOO_POS_LOCK_SETTINGS)
    fun provideLockSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.lockSettingsDataStore

    @Provides
    @Singleton
    @DataStoreQualifier(DataStoreType.WOO_POS)
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}
