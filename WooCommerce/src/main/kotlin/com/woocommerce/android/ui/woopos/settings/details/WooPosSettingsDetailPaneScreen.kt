package com.woocommerce.android.ui.woopos.settings.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
fun WooPosSettingsDetailPaneScreen(
    state: WooPosSettingsState,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDestination = state.currentDestination

    BackHandler(enabled = state.canGoBack) {
        onBack()
    }

    Column(
        modifier = modifier.fillMaxSize()
            .padding(WooPosSpacing.Medium.value)
            .statusBarsPadding()
    ) {
        DetailPaneToolbar(
            canGoBack = state.canGoBack,
            title = state.currentDestination.titleRes,
            onBack = onBack
        )

        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val isNavigatingForward = targetState.parentDestination == initialState
                if (isNavigatingForward) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
                        (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "settings_detail_animation"
        ) { destination ->
            when (destination) {
                is WooPosSettingsDetailDestination.Hardware.Overview -> {
                    WooPosHardwareSettingsScreen(onNavigate = onNavigate)
                }
                is WooPosSettingsDetailDestination.Hardware.BarcodeScanners -> {
                    BarcodeScannerDetailScreen()
                }
                is WooPosSettingsDetailDestination.Hardware.CardReaders -> {
                    CardReadersDetailScreen()
                }
            }
        }
    }
}

@Composable
private fun DetailPaneToolbar(
    canGoBack: Boolean,
    @androidx.annotation.StringRes title: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = WooPosSpacing.Small.value,
                bottom = WooPosSpacing.Medium.value
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (canGoBack) {
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

        WooPosText(
            text = stringResource(title),
            style = WooPosTypography.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = WooPosSpacing.XSmall.value)
        )
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
