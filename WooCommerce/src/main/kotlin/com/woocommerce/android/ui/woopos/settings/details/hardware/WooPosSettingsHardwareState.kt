package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector
import com.woocommerce.android.R

data class HardwareSettingsItem(
    @StringRes val titleRes: Int,
    @StringRes val subtitleRes: Int,
    val icon: ImageVector,
)

data class WooPosHardwareSettingsState(
    val items: List<HardwareSettingsItem> = listOf(
        HardwareSettingsItem(
            titleRes = R.string.woopos_settings_hardware_barcode_scanners,
            subtitleRes = R.string.woopos_settings_hardware_barcode_scanners_subtitle,
            icon = Icons.Default.QrCodeScanner
        ),
        HardwareSettingsItem(
            titleRes = R.string.woopos_settings_hardware_card_readers,
            subtitleRes = R.string.woopos_settings_hardware_card_readers_subtitle,
            icon = Icons.Default.CreditCard
        )
    ),
)
