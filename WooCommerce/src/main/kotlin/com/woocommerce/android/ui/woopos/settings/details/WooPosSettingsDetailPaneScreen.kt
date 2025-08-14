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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
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
    ) {
        WooPosToolbar(
            modifier = Modifier
                .padding(
                    top = WooPosSpacing.Medium.value,
                    start = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.Medium.value,
                ),
            titleText = stringResource(state.currentDestination.titleRes),
            onBackClicked = if (state.canGoBack) onBack else null,
            titleStyle = WooPosTypography.BodyLarge,
            titleFontWeight = FontWeight.Normal
        )

        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                val isNavigatingForward = targetState.parentDestination == initialState
                val isCategorySwitch = initialState.parentDestination == null && targetState.parentDestination == null

                if (isCategorySwitch) {
                    fadeIn() togetherWith fadeOut()
                } else if (isNavigatingForward) {
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

                is WooPosSettingsDetailDestination.Store.Overview -> {
                    StoreDetailScreen()
                }
            }
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

@Composable
private fun StoreDetailScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(WooPosSpacing.Medium.value),
        contentAlignment = Alignment.Center
    ) {
        WooPosText(
            text = stringResource(R.string.woopos_settings_store_category),
            style = WooPosTypography.Heading
        )
    }
}
