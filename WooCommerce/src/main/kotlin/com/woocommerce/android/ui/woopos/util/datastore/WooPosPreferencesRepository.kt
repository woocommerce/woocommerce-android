package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooPosPreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        val SIMPLE_PRODUCTS_ONLY_BANNER_HIDDEN_BY_USER = booleanPreferencesKey(
            "is_simple_products_only_banner_hidden_by_user"
        )
    }

    val isSimpleProductsOnlyBannerWasHiddenByUser: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[SIMPLE_PRODUCTS_ONLY_BANNER_HIDDEN_BY_USER] ?: false
        }

    suspend fun setSimpleProductsOnlyBannerWasHiddenByUser(shown: Boolean) {
        dataStore.edit { preferences ->
            preferences[SIMPLE_PRODUCTS_ONLY_BANNER_HIDDEN_BY_USER] = shown
        }
    }
}
