package com.woocommerce.android.ui.woopos.settings.categories

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination

enum class WooPosSettingsCategory(
    @StringRes val titleRes: Int,
    val rootDestination: WooPosSettingsDetailDestination
) {
    HARDWARE(
        R.string.woopos_settings_hardware_category,
        WooPosSettingsDetailDestination.Hardware.Overview
    )
}

data class WooPosSettingsCategoriesState(
    val categories: List<WooPosSettingsCategory> = WooPosSettingsCategory.entries
)
