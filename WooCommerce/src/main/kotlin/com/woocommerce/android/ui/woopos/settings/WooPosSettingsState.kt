package com.woocommerce.android.ui.woopos.settings

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory

data class WooPosSettingsState(
    val selectedCategory: WooPosSettingsCategory = WooPosSettingsCategory.HARDWARE,
    val currentDestination: WooPosSettingsDetailDestination = selectedCategory.rootDestination
) {
    val canGoBack: Boolean
        get() = currentDestination.parentDestination != null
}

sealed class WooPosSettingsDetailDestination {
    @get:StringRes
    abstract val titleRes: Int
    abstract val parentDestination: WooPosSettingsDetailDestination?
    abstract val childDestinations: List<WooPosSettingsDetailDestination>

    sealed class Hardware : WooPosSettingsDetailDestination() {
        data object Overview : Hardware() {
            override val titleRes: Int = R.string.woopos_settings_hardware_category
            override val parentDestination: WooPosSettingsDetailDestination? = null
            override val childDestinations: List<Hardware> = listOf(BarcodeScanners, CardReaders)
        }

        data object BarcodeScanners : Hardware() {
            override val titleRes: Int = R.string.woopos_settings_hardware_barcode_scanners
            override val parentDestination: Hardware = Overview
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }

        data object CardReaders : Hardware() {
            override val titleRes: Int = R.string.woopos_settings_hardware_card_readers
            override val parentDestination: Hardware = Overview
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }
}
