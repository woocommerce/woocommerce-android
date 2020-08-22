package com.woocommerce.android.ui.products

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class ParameterRepository @Inject constructor(
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    fun getParameters(key: String, savedState: SavedStateWithArgs): SiteParameters {
        val parameters = savedState.get<SiteParameters>(key) ?: loadParameters()
        savedState[key] = parameters
        return parameters
    }

    private fun loadParameters(): SiteParameters {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get()).let {
            Pair(it?.weightUnit, it?.dimensionUnit)
        }

        return SiteParameters(
            currencyCode,
            weightUnit,
            dimensionUnit,
            gmtOffset
        )
    }
}
