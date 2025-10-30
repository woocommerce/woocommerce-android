package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class HardwareSettingsItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
)

data class WooPosHardwareSettingsState(
    val items: List<HardwareSettingsItem> = listOf(
        HardwareSettingsItem(
            titleRes = R.string.woopos_settings_hardware_barcode_scanners,
            subtitleRes = R.string.woopos_settings_hardware_barcode_scanners_subtitle,
        ),
        HardwareSettingsItem(
            titleRes = R.string.woopos_settings_hardware_card_readers,
            subtitleRes = R.string.woopos_settings_hardware_card_readers_subtitle,
        )
    ),
)
