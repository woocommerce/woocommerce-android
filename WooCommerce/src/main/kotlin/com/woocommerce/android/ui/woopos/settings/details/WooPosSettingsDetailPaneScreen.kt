package com.woocommerce.android.ui.woopos.settings.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsState
import com.woocommerce.android.ui.woopos.settings.details.hardware.WooPosHardwareSettingsScreen

@Composable
fun WooPosSettingsDetailPane(
    state: WooPosSettingsState,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDestination = state.detailBackStack.lastOrNull()
    val canGoBack = state.detailBackStack.size > 1

    BackHandler(enabled = canGoBack) {
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (canGoBack) {
            DetailPaneToolbar(onBack = onBack)
        }

        AnimatedContent(
            targetState = currentDestination,
            label = "settings_detail_animation"
        ) { destination ->
            when (destination) {
                is WooPosSettingsDetailDestination.HardwareOverview -> {
                    WooPosHardwareSettingsScreen(onNavigate = onNavigate)
                }
                is WooPosSettingsDetailDestination.BarcodeScanners -> {
                    BarcodeScannerDetailScreen()
                }
                is WooPosSettingsDetailDestination.CardReaders -> {
                    CardReadersDetailScreen()
                }
                null -> {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun DetailPaneToolbar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.woopos_settings_back_content_description),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun BarcodeScannerDetailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_barcode_scanner_detail_title),
            style = WooPosTypography.Heading
        )
    }
}

@Composable
private fun CardReadersDetailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_card_reader_detail_title),
            style = WooPosTypography.Heading
        )
    }
}
