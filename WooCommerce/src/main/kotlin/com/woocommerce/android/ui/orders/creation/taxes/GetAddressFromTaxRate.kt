package com.woocommerce.android.ui.orders.creation.taxes

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRate
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class GetAddressFromTaxRate @Inject constructor(
    private val dataStore: WCDataStore,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun Address.invoke(taxRate: TaxRate): Address {
        dataStore.fetchCountriesAndStates(selectedSite.get())
        val country: Location = dataStore.getCountries().first {
            it.code == taxRate.countryCode
        }.toAppModel()
        val state: Location = dataStore.getStates(country.code).first {
            it.code == taxRate.stateCode
        }.toAppModel()
        val city = if (taxRate.city.isNotNullOrEmpty()) {
            taxRate.city
        } else {
            taxRate.cities?.first() ?: ""
        }
        val postCode = if (taxRate.postcode.isNotNullOrEmpty()) {
            taxRate.postcode
        } else {
            taxRate.postCodes?.first() ?: ""
        }
        return copy(
            city = city,
            state = AmbiguousLocation.Defined(state),
            country = country,
            postcode = postCode
        )
    }
}