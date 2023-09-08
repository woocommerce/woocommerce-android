package com.woocommerce.android.ui.orders.creation.taxes.rates

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.taxes.WCTaxRestClient.TaxRateModel
import org.wordpress.android.fluxc.store.WCTaxStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaxRateRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val taxStore: WCTaxStore,
) {
    private val _taxRates: MutableStateFlow<List<TaxRate>> = MutableStateFlow(emptyList())
    val taxRates: Flow<List<TaxRate>> = _taxRates

    /**
     * Fetches the tax rates for the selected site.
     *
     * @param page The page number to fetch.
     * @param pageSize The number of items to fetch per page.
     *
     * @return A [Boolean] indicating whether more items can be fetched.
     */
    suspend fun fetchTaxRates(page: Int, pageSize: Int): Result<Boolean> {
        return taxStore.fetchTaxRateList(selectedSite.get(), page, pageSize).let { result ->
            if (result.isError) {
                Result.failure(WooException(result.error))
            } else {
                val taxRates = result.model!!.toAppModel()
                _taxRates.value += taxRates
                Result.success(taxRates.size == pageSize)
            }
        }
    }

    private fun Collection<TaxRateModel>.toAppModel() = map {
        TaxRate(
            id = it.id,
            name = it.name ?: "",
            rate = it.rate ?: "",
            shipping = it.shipping ?: false,
            compound = it.compound ?: false,
            order = it.order ?: 0,
            taxClass = it.taxClass ?: "",
            postcode = it.postcode ?: "",
            city = it.city ?: "",
            priority = it.priority ?: 0,
            countryCode = it.country ?: "",
            stateCode = it.state ?: "",
            postCodes = it.postCodes,
            cities = it.cities,
        )
    }
}
