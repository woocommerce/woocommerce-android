package com.woocommerce.android.ui.woopos.settings.categories

import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.settings.SettingsCategory

data class SettingsCategoryItem(
    val category: SettingsCategory,
    @StringRes val titleRes: Int,
    val isEnabled: Boolean = true
)

data class WooPosSettingsCategoriesState(
    val categories: List<SettingsCategoryItem> = listOf(
        SettingsCategoryItem(
            category = SettingsCategory.HARDWARE,
            titleRes = SettingsCategory.HARDWARE.titleRes
        )
    )
)