package com.woocommerce.android.ui.woopos.settings.categories

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination

enum class WooPosSettingsCategory(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val rootDestination: WooPosSettingsDetailDestination
) {
    HARDWARE(
        R.string.woopos_settings_hardware_category,
        R.string.woopos_settings_hardware_category_subtitle,
        Icons.Default.Hardware,
        WooPosSettingsDetailDestination.Hardware.Overview
    ),
    STORE(
        R.string.woopos_settings_store_category,
        R.string.woopos_settings_store_category_subtitle,
        Icons.Default.Store,
        WooPosSettingsDetailDestination.Store.Overview
    )
}

data class WooPosSettingsCategoriesState(
    val categories: List<WooPosSettingsCategory> = WooPosSettingsCategory.entries
)
