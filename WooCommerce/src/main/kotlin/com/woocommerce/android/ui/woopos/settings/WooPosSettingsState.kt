package com.woocommerce.android.ui.woopos.settings

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosSettingsState(
    val selectedCategory: SettingsCategory = SettingsCategory.HARDWARE,
    val detailBackStack: List<SettingsDetailDestination> = listOf(SettingsDetailDestination.HardwareOverview)
)

enum class SettingsCategory(@StringRes val titleRes: Int) {
    HARDWARE(R.string.woopos_settings_hardware_category)
}

sealed class SettingsDetailDestination {
    data object HardwareOverview : SettingsDetailDestination()
    data object BarcodeScanners : SettingsDetailDestination()
    data object CardReaders : SettingsDetailDestination()
}