package com.woocommerce.android.ui.woopos.settings

import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory

data class WooPosSettingsState(
    val selectedCategory: WooPosSettingsCategory = WooPosSettingsCategory.HARDWARE,
    val currentDestination: WooPosSettingsDetailDestination = selectedCategory.rootDestination
) {
    val canGoBack: Boolean
        get() = currentDestination.parentDestination != null
}

sealed class WooPosSettingsDetailDestination {
    abstract val parentDestination: WooPosSettingsDetailDestination?
    abstract val childDestinations: List<WooPosSettingsDetailDestination>

    sealed class Hardware : WooPosSettingsDetailDestination() {

        data object Overview : Hardware() {
            override val parentDestination: WooPosSettingsDetailDestination? = null
            override val childDestinations: List<Hardware> = listOf(BarcodeScanners, CardReaders)
        }

        data object BarcodeScanners : Hardware() {
            override val parentDestination: Hardware = Overview
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }

        data object CardReaders : Hardware() {
            override val parentDestination: Hardware = Overview
            override val childDestinations: List<WooPosSettingsDetailDestination> = emptyList()
        }
    }
}
