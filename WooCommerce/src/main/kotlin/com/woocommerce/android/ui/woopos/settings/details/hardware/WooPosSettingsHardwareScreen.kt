package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
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
            .verticalScroll(rememberScrollState())
    ) {
        state.items.forEach { item ->
            WooPosSettingsDetailsMenuItem(
                icon = item.icon,
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

