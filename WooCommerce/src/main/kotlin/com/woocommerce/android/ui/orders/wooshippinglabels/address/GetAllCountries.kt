package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class GetAllCountries @Inject constructor(
    private val dataStore: WCDataStore,
    private val site: SelectedSite,
    private val coroutineDispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(): Result<List<Location>> {
        return withContext(coroutineDispatchers.io) {
            val cachedCountries = dataStore.getCountries().map { it.toAppModel() }
            if (cachedCountries.isNotEmpty()) {
                Result.success(cachedCountries)
            } else {
                val siteModel = site.getOrNull() ?: return@withContext Result.failure(Exception("No site selected"))
                val result = dataStore.fetchCountriesAndStates(siteModel)
                val countries = result.model
                if (result.isError || countries == null) {
                    val error = result.error?.message ?: "Unknown error"
                    Result.failure(Exception(error))
                } else {
                    Result.success(countries.map { it.toAppModel() })
                }
            }
        }
    }
}
