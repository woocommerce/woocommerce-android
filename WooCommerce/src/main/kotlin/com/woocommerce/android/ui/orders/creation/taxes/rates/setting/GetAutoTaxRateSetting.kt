package com.woocommerce.android.ui.orders.creation.taxes.rates.setting

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRate
import com.woocommerce.android.ui.orders.creation.taxes.rates.TaxRateRepository
import javax.inject.Inject

class GetAutoTaxRateSetting @Inject constructor(
    private val selectedSite: SelectedSite,
    private val prefs: AppPrefs,
    private val taxRateRepository: TaxRateRepository
) {
    suspend operator fun invoke(): TaxRate? {
        val taxRateId = prefs.getAutoTaxRateId()
        return if (prefs.isAutoTaxRateEnabled() && taxRateId != -1L) {
            taxRateRepository.getTaxRate(
                selectedSite = selectedSite,
                taxRateId = taxRateId
            )
        } else {
            null
        }
    }
}
