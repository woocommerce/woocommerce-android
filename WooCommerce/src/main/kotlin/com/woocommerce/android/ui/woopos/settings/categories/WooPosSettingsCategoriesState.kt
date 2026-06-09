package com.woocommerce.android.ui.woopos.settings.categories

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination
import kotlinx.parcelize.Parcelize

@Parcelize
enum class WooPosSettingsCategory(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int?,
    val rootDestination: WooPosSettingsDetailDestination,
    @DrawableRes val icon: Int? = null,
    val isFixedAtBottom: Boolean = false
) : Parcelable {
    STORE(
        R.string.woopos_settings_store_category,
        R.string.woopos_settings_store_category_subtitle,
        WooPosSettingsDetailDestination.Store.Overview
    ),
    HARDWARE(
        R.string.woopos_settings_hardware_category,
        R.string.woopos_settings_hardware_category_subtitle,
        WooPosSettingsDetailDestination.Hardware.Overview
    ),
    LOCAL_CATALOG(
        R.string.woopos_settings_local_catalog_category,
        R.string.woopos_settings_local_catalog_category_subtitle,
        WooPosSettingsDetailDestination.LocalCatalog.Overview
    ),
    HELP(
        titleRes = R.string.woopos_settings_help_category,
        subtitleRes = null,
        WooPosSettingsDetailDestination.Help.Overview,
        R.drawable.ic_help_24dp,
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
