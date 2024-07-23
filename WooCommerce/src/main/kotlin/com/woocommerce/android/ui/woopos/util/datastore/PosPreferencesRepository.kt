package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PosPreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        val SIMPLE_PRODUCTS_ONLY_BANNER_SHOWN = booleanPreferencesKey("isSimpleProductsOnlyBannerShown")
    }

    val isSimpleProductsOnlyBannerShown: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SIMPLE_PRODUCTS_ONLY_BANNER_SHOWN] ?: false
        }

    suspend fun setSimpleProductsOnlyBannerShown(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[SIMPLE_PRODUCTS_ONLY_BANNER_SHOWN] = shown
        }
    }
}
