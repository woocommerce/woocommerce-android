package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooPosPreferencesRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val recentProductSearches: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val searchesString = preferences[RECENT_PRODUCT_SEARCHES] ?: ""
            if (searchesString.isEmpty()) emptyList() else searchesString.split(",")
        }

    suspend fun addRecentProductSearch(search: String) {
        dataStore.edit { preferences ->
            val currentSearches = preferences[RECENT_PRODUCT_SEARCHES]?.let {
                if (it.isEmpty()) emptyList() else it.split(",")
            } ?: emptyList()

            val updatedSearches = (listOf(search) + currentSearches)
                .distinct()
                .take(MAX_RECENT_SEARCHES)

            preferences[RECENT_PRODUCT_SEARCHES] = updatedSearches.joinToString(",")
        }
    }

    companion object {
        val RECENT_PRODUCT_SEARCHES = stringPreferencesKey(
            "recent_product_searches"
        )

        const val MAX_RECENT_SEARCHES = 10
    }
}
