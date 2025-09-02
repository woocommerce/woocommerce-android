package com.woocommerce.android.ui.woopos.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupDialog
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategoriesPaneScreen
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailPaneScreen
import com.woocommerce.android.ui.woopos.settings.productinfo.WooPosSettingsProductInfoDialog
import com.woocommerce.android.ui.woopos.settings.productinfo.WooPosSettingsProductInfoDialogState

@Composable
fun WooPosSettingsScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    val containerViewModel: WooPosSettingsViewModel = hiltViewModel()
    val state by containerViewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        containerViewModel.onSettingsOpened()
    }

    val backHandler = {
        containerViewModel.onSettingsClosed()
        onNavigationEvent(WooPosNavigationEvent.GoBack)
    }

    BackHandler { backHandler() }

    WooPosSettingsContent(
        state = state,
        onBackClicked = backHandler,
        onCategorySelected = containerViewModel::onCategorySelected,
        onNavigate = containerViewModel::navigateToDetail,
        onBack = containerViewModel::navigateBack,
        onShowProductInfoDialog = containerViewModel::showProductInfoDialog,
        onShowScanningSetupDialog = containerViewModel::showScanningSetupDialog,
        onDismissDialog = containerViewModel::hideDialog
    )
}

@Composable
private fun WooPosSettingsContent(
    state: WooPosSettingsState,
    onBackClicked: () -> Unit,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    onShowProductInfoDialog: () -> Unit,
    onShowScanningSetupDialog: () -> Unit,
    onDismissDialog: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(0.3f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WooPosToolbar(
                titleText = stringResource(R.string.woopos_settings_title),
                onBackClicked = onBackClicked,
            )

            WooPosSettingsCategoriesPaneScreen(
                selectedCategory = state.selectedCategory,
                onCategorySelected = onCategorySelected,
                modifier = Modifier.fillMaxSize()
            )
        }

        WooPosSettingsDetailPaneScreen(
            state = state,
            onNavigate = onNavigate,
            onBack = onBack,
            onShowProductInfoDialog = onShowProductInfoDialog,
            onShowScanningSetupDialog = onShowScanningSetupDialog,
            modifier = Modifier
                .weight(0.7f)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
        )
    }

    val dialogState = state.dialogState
    WooPosSettingsProductInfoDialog(
        state = WooPosSettingsProductInfoDialogState,
        isVisible = dialogState is WooPosSettingsDialogState.ProductsInfoDialog,
        onDismissRequest = onDismissDialog
    )

    WooPosScanningSetupDialog(
        isVisible = dialogState is WooPosSettingsDialogState.ScanningSetupDialog,
        onDismissRequest = onDismissDialog
    )
}

@WooPosPreview
@Composable
fun WooPosSettingsScreenPreview() {
    WooPosTheme {
        WooPosSettingsScreen(
            onNavigationEvent = {}
        )
    }
}
