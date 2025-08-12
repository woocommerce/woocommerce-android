package com.woocommerce.android.ui.woopos.settings.details.hardware

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination

@Composable
fun WooPosHardwareSettingsScreen(
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    viewModel: WooPosHardwareSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value)
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_hardware_category),
            style = WooPosTypography.Heading,
            modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value)
        )

        state.items.forEach { item ->
            HardwareSettingsMenuItem(
                item = item,
                onClick = {
                    when (item.titleRes) {
                        R.string.woopos_settings_hardware_barcode_scanners ->
                            onNavigate(WooPosSettingsDetailDestination.BarcodeScanners)

                        R.string.woopos_settings_hardware_card_readers ->
                            onNavigate(WooPosSettingsDetailDestination.CardReaders)
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
            .clickable { onClick() }
            .padding(vertical = WooPosSpacing.Medium.value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            WooPosText(
                text = stringResource(item.titleRes),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            item.subtitleRes?.let { subtitleRes ->
                WooPosText(
                    text = stringResource(subtitleRes),
                    style = WooPosTypography.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = WooPosSpacing.XSmall.value)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
