package com.woocommerce.android.ui.woopos.settings.categories

import com.woocommerce.android.ui.woopos.settings.WooPosSettingsCategory

data class WooPosSettingsCategoriesState(
    val categories: List<WooPosSettingsCategory> = WooPosSettingsCategory.entries
)
