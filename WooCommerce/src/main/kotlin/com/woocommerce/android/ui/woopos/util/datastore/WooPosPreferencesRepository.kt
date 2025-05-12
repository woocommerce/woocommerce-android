package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooPosPreferencesRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val dataStore: DataStore<Preferences>
) {
    private val recentProductSearchesSiteSpecificKey = buildSiteSpecificKey(RECENT_PRODUCT_SEARCHES_KEY)

    val recentProductSearches: Flow<List<String>> = dataStore.data
        .map { preferences ->
            val searchesString = preferences[recentProductSearchesSiteSpecificKey] ?: ""
            if (searchesString.isEmpty()) emptyList() else searchesString.split(",")
        }

    suspend fun addRecentProductSearch(search: String) {
        dataStore.edit { preferences ->
            val currentSearches = preferences[recentProductSearchesSiteSpecificKey]?.let {
                if (it.isEmpty()) emptyList() else it.split(",")
            } ?: emptyList()

            val updatedSearches = (listOf(search) + currentSearches)
                .distinct()
                .take(MAX_RECENT_SEARCHES)

            preferences[recentProductSearchesSiteSpecificKey] = updatedSearches.joinToString(",")
        }
    }

    private fun buildSiteSpecificKey(key: String): Preferences.Key<String> =
        stringPreferencesKey("${selectedSite.getOrNull()?.siteId}-$key")

    companion object {
        const val RECENT_PRODUCT_SEARCHES_KEY = "recent_product_searches_key"

        const val MAX_RECENT_SEARCHES = 10
    }
}
