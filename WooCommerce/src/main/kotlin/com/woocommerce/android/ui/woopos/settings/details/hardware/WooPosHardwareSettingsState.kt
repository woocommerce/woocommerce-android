package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class HardwareSettingsItem(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int? = null
)

data class WooPosHardwareSettingsState(
    val items: List<HardwareSettingsItem> = listOf(
        HardwareSettingsItem(
            id = "barcode_scanners",
            titleRes = R.string.woopos_settings_hardware_barcode_scanners,
            subtitleRes = R.string.woopos_settings_hardware_barcode_scanners_subtitle
        ),
        HardwareSettingsItem(
            id = "card_readers",
            titleRes = R.string.woopos_settings_hardware_card_readers,
            subtitleRes = R.string.woopos_settings_hardware_card_readers_subtitle
        )
    ),
    val isLoading: Boolean = false
)