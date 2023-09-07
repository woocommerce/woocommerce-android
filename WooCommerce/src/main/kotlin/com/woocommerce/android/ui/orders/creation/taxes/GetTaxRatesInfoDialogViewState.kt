package com.woocommerce.android.ui.orders.creation.taxes

import com.woocommerce.android.R
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.creation.OrderCreateEditRepository
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

class GetTaxRatesInfoDialogViewState @Inject constructor(
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
) {
    suspend operator fun invoke(taxLines: List<Order.TaxLine> = emptyList()): TaxRatesInfoDialogViewState {
        val taxLineNamesWithRates = taxLines.map { Pair(it.label, "${it.ratePercent}%") }
        val settingTextPostFix = if (taxLines.isNotEmpty()) ":" else "."
        val settingText = when (orderCreateEditRepository.getTaxBasedOnSetting()) {
            TaxBasedOnSetting.StoreAddress ->
                resourceProvider.getString(
                    R.string.tax_rates_info_dialog_tax_based_on_store_address,
                    settingTextPostFix
                )

            TaxBasedOnSetting.BillingAddress ->
                resourceProvider.getString(
                    R.string.tax_rates_info_dialog_tax_based_on_billing_address,
                    settingTextPostFix
                )

            TaxBasedOnSetting.ShippingAddress ->
                resourceProvider.getString(
                    R.string.tax_rates_info_dialog_tax_based_on_shipping_address,
                    settingTextPostFix
                )

            else -> ""
        }
        val taxRatesSettingsUrl =
            selectedSite.get().adminUrlOrDefault.slashJoin(TAX_BASED_ON_SETTING_ADMIN_URL)
        return TaxRatesInfoDialogViewState(
            taxBasedOnSettingText = settingText,
            taxLineTexts = taxLineNamesWithRates,
            taxRatesSettingsUrl = taxRatesSettingsUrl
        )
    }

    companion object {
        private const val TAX_BASED_ON_SETTING_ADMIN_URL =
            "admin.php?page=wc-settings&tab=tax&section=standard"
    }
}
