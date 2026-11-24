package com.woocommerce.android.ui.woopos.settings.details

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.woocommerce.android.ui.woopos.settings.details.hardware.receiptprinter.WooPosSettingsHardwareReceiptPrinterScreen
import com.woocommerce.android.ui.woopos.settings.details.help.WooPosHelpDetailScreen
import com.woocommerce.android.ui.woopos.settings.details.localcatalog.WooPosSettingsLocalCatalogScreen
import com.woocommerce.android.ui.woopos.settings.details.payments.WooPosSettingsPaymentsScreen
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityScreen
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityState
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityViewModel
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreScreen

@Suppress("CyclomaticComplexMethod")
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
    val pinSetupRole = (state.dialogState as? WooPosSettingsDialogState.PinSetupDialog)?.role
    val transition = updateTransition(currentDestination, label = "settings_detail_animation")
    val securityViewModel = if (currentDestination is WooPosSettingsDetailDestination.Security ||
        transition.currentState is WooPosSettingsDetailDestination.Security
    ) {
        hiltViewModel<WooPosSettingsSecurityViewModel>()
    } else {
        null
    }
    val securityState = securityViewModel?.state?.collectAsState()?.value
    val visibleSecurityState = when {
        securityState == null -> WooPosSettingsSecurityState.Loading
        pinSetupRole == WooPosLocalRole.ADMIN && securityState is WooPosSettingsSecurityState.Protected ->
            WooPosSettingsSecurityState.Off
        else -> securityState
    }
    LaunchedEffect(currentDestination, pinSetupRole) {
        if (currentDestination is WooPosSettingsDetailDestination.Security && pinSetupRole == null) {
            securityViewModel?.refresh()
        }
    }

    BackHandler(enabled = registerBackHandler && showBack) {
        onBack()
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        DetailPaneHeader(
            destination = currentDestination,
            onBack = if (showBack) onBack else null,
            showHeading = currentDestination.showsHeading ||
                currentDestination is WooPosSettingsDetailDestination.Security &&
                visibleSecurityState is WooPosSettingsSecurityState.Protected,
        )

        transition.AnimatedContent(transitionSpec = { settingsDetailTransition() }) { destination ->
            when (destination) {
                is WooPosSettingsDetailDestination.Security.Overview -> {
                    WooPosSettingsSecurityScreen(
                        state = visibleSecurityState,
                        onSetUpPIN = onShowPinSetupDialog,
                        onProtectionTurnedOff = { securityViewModel?.onProtectionTurnedOff() },
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

                is WooPosSettingsDetailDestination.Hardware.ReceiptPrinters -> {
                    WooPosSettingsHardwareReceiptPrinterScreen()
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
private fun DetailPaneHeader(
    destination: WooPosSettingsDetailDestination,
    onBack: (() -> Unit)?,
    showHeading: Boolean,
) {
    if (!showHeading && onBack == null) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        return
    }
    WooPosToolbar(
        modifier = Modifier
            .padding(
                top = WooPosSpacing.None.value,
                start = WooPosSpacing.Medium.value,
                end = WooPosSpacing.Medium.value,
            ),
        titleText = if (showHeading) stringResource(destination.titleRes) else "",
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
