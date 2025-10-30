package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem

@Composable
fun WooPosHardwareSettingsScreen(
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    viewModel: WooPosSettingsHardwareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
    ) {
        state.items.forEach { item ->
            WooPosSettingsDetailsMenuItem(
                modifier = Modifier.padding(horizontal = WooPosSpacing.Medium.value),
                title = stringResource(item.titleRes),
                subtitle = stringResource(item.subtitleRes),
                onClick = {
                    when (item.titleRes) {
                        R.string.woopos_settings_hardware_barcode_scanners ->
                            onNavigate(WooPosSettingsDetailDestination.Hardware.BarcodeScanners)

                        R.string.woopos_settings_hardware_card_readers ->
                            onNavigate(WooPosSettingsDetailDestination.Hardware.CardReaders)
                    }
                }
            )
        }
    }
}
