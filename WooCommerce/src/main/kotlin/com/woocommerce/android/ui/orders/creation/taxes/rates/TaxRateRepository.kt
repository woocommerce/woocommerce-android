package com.woocommerce.android.ui.orders.creation.taxes.rates

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.delay
import javax.inject.Inject

@Suppress("MagicNumber", "ForbiddenComment", "UnusedPrivateMember")
class TaxRateRepository @Inject constructor(private val selectedSite: SelectedSite) {
    suspend fun fetchTaxRates(): List<TaxRate> {
        // TODO: Remove mock data and implement endpoint call
        delay(1000)
        return listOf(
            TaxRate(
                id = 1,
                countryCode = "US",
                stateCode = "CA",
                postcode = "94016",
                city = "San Francisco",
                postCodes = null,
                cities = null,
                rate = "10",
                name = "Government Sales Tax",
                priority = 1,
                compound = false,
                shipping = false,
                order = 1,
                taxClass = "standard",
            ),
            TaxRate(
                id = 2,
                countryCode = "US",
                stateCode = "CA",
                postcode = "",
                city = "",
                postCodes = null,
                cities = null,
                rate = "10",
                name = "",
                priority = 1,
                compound = false,
                shipping = false,
                order = 1,
                taxClass = "standard",
            ),
            TaxRate(
                id = 2,
                countryCode = "",
                stateCode = "AU",
                postcode = "",
                city = "",
                postCodes = null,
                cities = null,
                rate = "10",
                name = "",
                priority = 1,
                compound = false,
                shipping = false,
                order = 1,
                taxClass = "standard",
            )
        )
    }
}
