package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination

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
            HardwareSettingsMenuItem(
                item = item,
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

@Composable
private fun HardwareSettingsMenuItem(
    item: HardwareSettingsItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(28.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = WooPosSpacing.Medium.value)
        ) {
            WooPosText(
                text = stringResource(item.titleRes),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            WooPosText(
                text = stringResource(item.subtitleRes),
                style = WooPosTypography.BodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
            )
        }
    }
}
