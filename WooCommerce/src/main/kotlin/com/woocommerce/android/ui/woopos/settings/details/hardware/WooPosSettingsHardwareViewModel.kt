package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.lifecycle.ViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.featureflags.WooPosReceiptPrintingEnabled
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class WooPosSettingsHardwareViewModel @Inject constructor(
    isReceiptPrintingEnabled: WooPosReceiptPrintingEnabled,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WooPosHardwareSettingsState(
            items = listOfNotNull(
                HardwareSettingsItem(
                    titleRes = R.string.woopos_settings_hardware_card_readers,
                    subtitleRes = R.string.woopos_settings_hardware_card_readers_subtitle,
                ),
                HardwareSettingsItem(
                    titleRes = R.string.woopos_settings_hardware_barcode_scanners,
                    subtitleRes = R.string.woopos_settings_hardware_barcode_scanners_subtitle,
                ),
                HardwareSettingsItem(
                    titleRes = R.string.woopos_settings_hardware_receipt_printers,
                    subtitleRes = R.string.woopos_settings_hardware_receipt_printers_subtitle,
                ).takeIf { isReceiptPrintingEnabled() },
            )
        )
    )
    val state: StateFlow<WooPosHardwareSettingsState> = _state.asStateFlow()
}
