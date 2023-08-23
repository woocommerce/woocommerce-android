package com.woocommerce.android.ui.orders.creation.taxes

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @param taxBasedOnSettingText The text to display for the tax based on setting
 * @param taxLineTexts A tax line names with their corresponding tax rates to display below @param taxBasedOnSettingText
 */
@Parcelize
data class TaxRatesInfoDialogViewState(
    val taxBasedOnSettingText: String,
    val taxLineTexts: List<Pair<String, String>>,
    val taxRatesSettingsUrl: String?
) : Parcelable
