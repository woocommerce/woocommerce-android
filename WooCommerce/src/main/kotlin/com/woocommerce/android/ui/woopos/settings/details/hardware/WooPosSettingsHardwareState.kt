package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.annotation.StringRes

data class HardwareSettingsItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

data class WooPosHardwareSettingsState(
    val items: List<HardwareSettingsItem>,
)
