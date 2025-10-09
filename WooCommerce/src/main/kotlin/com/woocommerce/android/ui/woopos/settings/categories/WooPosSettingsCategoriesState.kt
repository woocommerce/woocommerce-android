package com.woocommerce.android.ui.woopos.settings.categories

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Store
import androidx.compose.ui.graphics.vector.ImageVector
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination

enum class WooPosSettingsCategory(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
    val rootDestination: WooPosSettingsDetailDestination,
    val isFixedAtBottom: Boolean = false
) {
    STORE(
        R.string.woopos_settings_store_category,
        R.string.woopos_settings_store_category_subtitle,
        Icons.Default.Store,
        WooPosSettingsDetailDestination.Store.Overview
    ),
    LOCAL_CATALOG(
        R.string.woopos_settings_local_catalog_category,
        R.string.woopos_settings_local_catalog_category_subtitle,
        Icons.Default.Inventory,
        WooPosSettingsDetailDestination.LocalCatalog.Overview
    ),
    HARDWARE(
        R.string.woopos_settings_hardware_category,
        R.string.woopos_settings_hardware_category_subtitle,
        Icons.Default.Hardware,
        WooPosSettingsDetailDestination.Hardware.Overview
    ),
    HELP(
        R.string.woopos_settings_help_category,
        R.string.woopos_settings_help_category_subtitle,
        Icons.AutoMirrored.Filled.Help,
        WooPosSettingsDetailDestination.Help.Overview,
        isFixedAtBottom = true
    )
}

data class WooPosSettingsCategoriesState(
    val categories: List<WooPosSettingsCategory> = WooPosSettingsCategory.entries
) {
    val scrollableCategories: List<WooPosSettingsCategory>
        get() = categories.filter { !it.isFixedAtBottom }
    val fixedCategories: List<WooPosSettingsCategory>
        get() = categories.filter { it.isFixedAtBottom }
}
