package com.woocommerce.android.ui.woopos.settings.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
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
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDetailDestination
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsDialogState
import com.woocommerce.android.ui.woopos.settings.WooPosSettingsState
import com.woocommerce.android.ui.woopos.settings.details.hardware.WooPosHardwareSettingsScreen
import com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner.WooPosSettingsHardwareBarcodeScannerScreen
import com.woocommerce.android.ui.woopos.settings.details.hardware.cardreader.WooPosSettingsHardwareCardReaderScreen
import com.woocommerce.android.ui.woopos.settings.details.help.WooPosHelpDetailScreen
import com.woocommerce.android.ui.woopos.settings.details.localcatalog.WooPosSettingsLocalCatalogScreen
import com.woocommerce.android.ui.woopos.settings.details.payments.WooPosSettingsPaymentsScreen
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityScreen
import com.woocommerce.android.ui.woopos.settings.details.security.wooPosSettingsSecurityShowsHeading
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreScreen

@Composable
fun WooPosSettingsDetailPaneScreen(
    state: WooPosSettingsState,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    onShowProductInfoDialog: () -> Unit,
    onShowPinSetupDialog: (WooPosLocalRole) -> Unit,
    onShowScanningSetupDialog: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    modifier: Modifier = Modifier,
    showBackOnRoot: Boolean = false,
    registerBackHandler: Boolean = true,
) {
    val currentDestination = state.currentDestination
    val showBack = state.canGoBack || showBackOnRoot

    BackHandler(enabled = registerBackHandler && showBack) {
        onBack()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        DetailPaneHeader(destination = currentDestination, onBack = if (showBack) onBack else null)

        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = { settingsDetailTransition() },
            label = "settings_detail_animation"
        ) { destination ->
            when (destination) {
                is WooPosSettingsDetailDestination.Security.Overview -> {
                    WooPosSettingsSecurityScreen(
                        isPINSetupVisible = state.dialogState is WooPosSettingsDialogState.PinSetupDialog,
                        onSetUpPIN = onShowPinSetupDialog,
                    )
                }

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

                is WooPosSettingsDetailDestination.Payments.Overview -> {
                    WooPosSettingsPaymentsScreen()
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

@Composable
private fun DetailPaneHeader(destination: WooPosSettingsDetailDestination, onBack: (() -> Unit)?) {
    val hasHeading = destination.showsHeading ||
        destination is WooPosSettingsDetailDestination.Security && wooPosSettingsSecurityShowsHeading()
    if (!hasHeading && onBack == null) return
    WooPosToolbar(
        modifier = Modifier
            .padding(
                top = WooPosSpacing.None.value,
                start = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value,
            ),
        titleText = if (hasHeading) stringResource(destination.titleRes) else "",
        onBackClicked = onBack,
        titleStyle = WooPosTypography.Heading,
        titleFontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))
}

private fun AnimatedContentTransitionScope<WooPosSettingsDetailDestination>.settingsDetailTransition():
    ContentTransform {
    val isNavigatingForward = targetState.parentDestination == initialState
    val isCategorySwitch = initialState.parentDestination == null && targetState.parentDestination == null

    return if (isCategorySwitch) {
        fadeIn() togetherWith fadeOut()
    } else if (isNavigatingForward) {
        (slideInHorizontally(initialOffsetX = { it }) + fadeIn()) togetherWith
            (slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
    } else {
        (slideInHorizontally(initialOffsetX = { -it }) + fadeIn()) togetherWith
            (slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
    }
}
