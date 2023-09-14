package com.woocommerce.android.ui.orders.creation.taxes.rates

import com.woocommerce.android.WooException
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.taxes.TaxRateEntity
import org.wordpress.android.fluxc.store.WCTaxStore
import javax.inject.Inject

class TaxRateRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val taxStore: WCTaxStore,
) {
    /**
     * Fetches the tax rates for the selected site.
     *
     * @param page The page number to fetch.
     * @param pageSize The number of items to fetch per page.
     *
     * @return A [Boolean] indicating whether more items can be fetched.
     */
    suspend fun fetchTaxRates(
        page: Int,
        pageSize: Int
    ): Result<Boolean> {
        return taxStore.fetchTaxRateList(selectedSite.get(), page, pageSize)
            .let { result ->
                if (result.isError) {
                    Result.failure(WooException(result.error))
                } else {
                    Result.success(result.model!!)
                }
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTaxRates(): Flow<List<TaxRate>> = taxStore.observeTaxRates(selectedSite.get()).map {
        it.map { taxRateEntity ->
            taxRateEntity.toAppModel()
        }
    }

    suspend fun getTaxRate(selectedSite: SelectedSite, taxRateId: Long): TaxRate? {
        return taxStore.getTaxRate(selectedSite.get(), taxRateId)?.toAppModel()
    }

    fun TaxRateEntity.toAppModel(): TaxRate =
        TaxRate(
            id = id.value,
            countryCode = country ?: "",
            stateCode = state ?: "",
            postcode = postcode ?: "",
            city = city ?: "",
            postCodes = null,
            cities = null,
            rate = rate ?: "",
            name = name ?: "",
            priority = 0,
            compound = false,
            shipping = false,
            order = 0,
            taxClass = taxClass ?: "",
        )
}
