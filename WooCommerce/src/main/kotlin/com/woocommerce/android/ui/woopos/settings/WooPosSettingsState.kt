package com.woocommerce.android.ui.woopos.settings

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosSettingsState(
    val selectedCategory: WooPosSettingsCategory = WooPosSettingsCategory.HARDWARE,
    val detailBackStack: List<WooPosSettingsDetailDestination> = listOf(WooPosSettingsDetailDestination.HardwareOverview)
)

enum class WooPosSettingsCategory(@StringRes val titleRes: Int) {
    HARDWARE(R.string.woopos_settings_hardware_category)
}

sealed class WooPosSettingsDetailDestination {
    data object HardwareOverview : WooPosSettingsDetailDestination()
    data object BarcodeScanners : WooPosSettingsDetailDestination()
    data object CardReaders : WooPosSettingsDetailDestination()
}
