package com.woocommerce.android.ui.woopos.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderConnectionDialog
import com.woocommerce.android.ui.woopos.cardreader.connection.WooPosCardReaderUpdateDialog
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.scanningsetup.WooPosScanningSetupDialog
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategoriesPaneScreen
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategoriesPaneScreenContent
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailPaneScreen
import com.woocommerce.android.ui.woopos.settings.details.localcatalog.WooPosSyncErrorDialog
import com.woocommerce.android.ui.woopos.settings.productinfo.WooPosSettingsProductInfoDialog
import com.woocommerce.android.ui.woopos.settings.productinfo.WooPosSettingsProductInfoDialogState
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout

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

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isPhoneLayout = remember(configuration) { context.isWooPosPhoneLayout() }

    if (isPhoneLayout) {
        WooPosSettingsPhoneContent(
            state = state,
            onBackClicked = backHandler,
            onCategorySelected = containerViewModel::onCategorySelected,
            onNavigate = containerViewModel::navigateToDetail,
            onBack = containerViewModel::navigateBack,
            onDismissDetail = containerViewModel::dismissDetail,
            onShowProductInfoDialog = containerViewModel::showProductInfoDialog,
            onShowScanningSetupDialog = containerViewModel::showScanningSetupDialog,
            onRetrySync = containerViewModel::onRetrySyncFromDialogClicked,
            onDismissDialog = containerViewModel::hideDialog,
            onNavigationEvent = onNavigationEvent
        )
    } else {
        BackHandler { backHandler() }

        WooPosSettingsTabletContent(
            state = state,
            onBackClicked = backHandler,
            onCategorySelected = containerViewModel::onCategorySelected,
            onNavigate = containerViewModel::navigateToDetail,
            onBack = containerViewModel::navigateBack,
            onShowProductInfoDialog = containerViewModel::showProductInfoDialog,
            onShowScanningSetupDialog = containerViewModel::showScanningSetupDialog,
            onRetrySync = containerViewModel::onRetrySyncFromDialogClicked,
            onDismissDialog = containerViewModel::hideDialog,
            onNavigationEvent = onNavigationEvent
        )
    }
}

@Composable
private fun WooPosSettingsPhoneContent(
    state: WooPosSettingsState,
    onBackClicked: () -> Unit,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    onDismissDetail: () -> Unit,
    onShowProductInfoDialog: () -> Unit,
    onShowScanningSetupDialog: () -> Unit,
    onRetrySync: () -> Unit,
    onDismissDialog: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    BackHandler(enabled = !state.showingDetail) {
        onBackClicked()
    }

    AnimatedContent(
        targetState = state.showingDetail,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "settings_phone_transition",
    ) { showingDetail ->
        if (!showingDetail) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceBright)
            ) {
                WooPosToolbar(
                    titleText = stringResource(R.string.woopos_settings_title),
                    onBackClicked = onBackClicked,
                )

                WooPosSettingsCategoriesPaneScreen(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = onCategorySelected,
                    showSelection = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = WooPosSpacing.Medium.value)
                )
            }
        } else {
            WooPosSettingsDetailPaneScreen(
                state = state,
                onNavigate = onNavigate,
                onBack = {
                    if (state.canGoBack) {
                        onBack()
                    } else {
                        onDismissDetail()
                    }
                },
                onShowProductInfoDialog = onShowProductInfoDialog,
                onShowScanningSetupDialog = onShowScanningSetupDialog,
                onNavigationEvent = onNavigationEvent,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                showBackOnRoot = true,
            )
        }
    }

    SettingsDialogs(
        dialogState = state.dialogState,
        onRetrySync = onRetrySync,
        onDismissDialog = onDismissDialog,
    )
}

@Composable
private fun WooPosSettingsTabletContent(
    state: WooPosSettingsState,
    onBackClicked: () -> Unit,
    onCategorySelected: (WooPosSettingsCategory) -> Unit,
    onNavigate: (WooPosSettingsDetailDestination) -> Unit,
    onBack: () -> Unit,
    onShowProductInfoDialog: () -> Unit,
    onShowScanningSetupDialog: () -> Unit,
    onRetrySync: () -> Unit,
    onDismissDialog: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(0.3f)
                .background(MaterialTheme.colorScheme.surfaceBright)
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
            onNavigationEvent = onNavigationEvent,
            modifier = Modifier
                .weight(0.7f)
                .background(MaterialTheme.colorScheme.surface)
        )
    }

    SettingsDialogs(
        dialogState = state.dialogState,
        onRetrySync = onRetrySync,
        onDismissDialog = onDismissDialog,
    )
}

@Composable
private fun SettingsDialogs(
    dialogState: WooPosSettingsDialogState,
    onRetrySync: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    WooPosSettingsProductInfoDialog(
        state = WooPosSettingsProductInfoDialogState,
        isVisible = dialogState is WooPosSettingsDialogState.ProductsInfoDialog,
        onDismissRequest = onDismissDialog
    )

    WooPosScanningSetupDialog(
        isVisible = dialogState is WooPosSettingsDialogState.ScanningSetupDialog,
        onDismissRequest = onDismissDialog
    )

    WooPosSyncErrorDialog(
        isVisible = dialogState is WooPosSettingsDialogState.SyncErrorDialog,
        onRetry = onRetrySync,
        onDismissRequest = onDismissDialog
    )

    if (dialogState is WooPosSettingsDialogState.CardReaderConnectionDialog) {
        WooPosCardReaderConnectionDialog(
            onDismiss = onDismissDialog,
            onConnectionSuccess = onDismissDialog
        )
    }

    if (dialogState is WooPosSettingsDialogState.CardReaderUpdateDialog) {
        WooPosCardReaderUpdateDialog(
            onDismiss = onDismissDialog,
            onUpdateComplete = onDismissDialog
        )
    }
}

private val previewScrollableCategories = listOf(
    WooPosSettingsCategory.STORE,
    WooPosSettingsCategory.HARDWARE,
    WooPosSettingsCategory.LOCAL_CATALOG,
)

private val previewFixedCategories = listOf(WooPosSettingsCategory.HELP)

@WooPosPreview
@Composable
fun WooPosSettingsScreenPreview() {
    val state = WooPosSettingsState()
    WooPosTheme {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .background(MaterialTheme.colorScheme.surfaceBright)
            ) {
                WooPosToolbar(
                    titleText = stringResource(R.string.woopos_settings_title),
                    onBackClicked = {},
                )

                WooPosSettingsCategoriesPaneScreenContent(
                    scrollableCategories = previewScrollableCategories,
                    fixedCategories = previewFixedCategories,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = {},
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .background(MaterialTheme.colorScheme.surface)
                    .fillMaxSize()
            ) {
                WooPosToolbar(
                    modifier = Modifier
                        .padding(
                            top = WooPosSpacing.None.value,
                            start = WooPosSpacing.Medium.value,
                            end = WooPosSpacing.Medium.value,
                        ),
                    titleText = stringResource(state.currentDestination.titleRes),
                    onBackClicked = null,
                    titleStyle = WooPosTypography.Heading,
                    titleFontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsPhoneScreenPreview() {
    val state = WooPosSettingsState()
    WooPosTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceBright)
        ) {
            WooPosToolbar(
                titleText = stringResource(R.string.woopos_settings_title),
                onBackClicked = {},
            )

            WooPosSettingsCategoriesPaneScreenContent(
                scrollableCategories = previewScrollableCategories,
                fixedCategories = previewFixedCategories,
                selectedCategory = state.selectedCategory,
                onCategorySelected = {},
                showSelection = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = WooPosSpacing.Medium.value),
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsPhoneDetailScreenPreview() {
    val state = WooPosSettingsState(
        selectedCategory = WooPosSettingsCategory.HARDWARE,
        currentDestination = WooPosSettingsDetailDestination.Hardware.Overview,
    )
    WooPosTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            WooPosToolbar(
                modifier = Modifier
                    .padding(
                        top = WooPosSpacing.None.value,
                        start = WooPosSpacing.Medium.value,
                        end = WooPosSpacing.Medium.value,
                    ),
                titleText = stringResource(state.currentDestination.titleRes),
                onBackClicked = {},
                titleStyle = WooPosTypography.Heading,
                titleFontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.size(WooPosSpacing.Medium.value))

            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
