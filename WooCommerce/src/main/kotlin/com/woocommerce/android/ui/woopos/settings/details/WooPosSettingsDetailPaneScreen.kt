package com.woocommerce.android.ui.woopos.settings.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsState
import com.woocommerce.android.ui.woopos.settings.details.hardware.WooPosHardwareSettingsScreen
import com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner.WooPosSettingsHardwareBarcodeScannerScreen
import com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader.WooPosSettingsHardwareCardReaderScreen
import com.woocommerce.android.ui.woopos.settings.details.help.WooPosHelpDetailScreen
import com.woocommerce.android.ui.woopos.settings.details.localcatalog.WooPosSettingsLocalCatalogScreen
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreScreen

@Composable
fun WooPosSettingsDetailPaneScreen(
    state: WooPosSettingsState,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    onShowProductInfoDialog: () -> Unit,
    onShowScanningSetupDialog: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentDestination = state.currentDestination
    val showBack = state.canGoBack

    BackHandler(enabled = showBack) {
        onBack()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        WooPosToolbar(
            modifier = Modifier
                .padding(
                    top = WooPosSpacing.None.value,
                    start = WooPosSpacing.Medium.value,
                    end = WooPosSpacing.Medium.value,
                ),
            titleText = stringResource(state.currentDestination.titleRes),
            onBackClicked = if (showBack) onBack else null,
            titleStyle = WooPosTypography.Heading,
            titleFontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

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
                    WooPosSettingsHardwareBarcodeScannerScreen(
                        onShowScanningSetupDialog = onShowScanningSetupDialog
                    )
                }

                is WooPosSettingsDetailDestination.Hardware.CardReaders -> {
                    WooPosSettingsHardwareCardReaderScreen()
                }

                is WooPosSettingsDetailDestination.Store.Overview -> {
                    WooPosSettingsStoreScreen(onNavigationEvent = onNavigationEvent)
                }

                is WooPosSettingsDetailDestination.LocalCatalog.Overview -> {
                    WooPosSettingsLocalCatalogScreen()
                }

                is WooPosSettingsDetailDestination.Help.Overview -> {
                    WooPosHelpDetailScreen(onShowProductInfoDialog = onShowProductInfoDialog)
                }
            }
        }
    }
}
