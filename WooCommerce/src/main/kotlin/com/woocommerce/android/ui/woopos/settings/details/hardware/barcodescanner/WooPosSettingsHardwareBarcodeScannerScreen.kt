package com.woocommerce.android.ui.woopos.settings.details.hardware.barcodescanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.settings.details.WooPosSettingsDetailsMenuItem
import com.woocommerce.android.util.ChromeCustomTabUtils
import kotlinx.coroutines.flow.collectLatest

@Composable
fun WooPosSettingsHardwareBarcodeScannerScreen(
    onShowScanningSetupDialog: () -> Unit,
    viewModel: WooPosSettingsHardwareBarcodeScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.showScanningSetupDialog.collectLatest {
            onShowScanningSetupDialog()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.openUrl.collectLatest { url ->
            ChromeCustomTabUtils.launchUrl(context, url, enableSlideAnimation = true)
        }
    }

    WooPosSettingsHardwareBarcodeScannerContent(
        onSetupScannerClicked = { viewModel.onSetupScannerClicked() },
        onDocumentationClicked = { viewModel.onDocumentationClicked() }
    )
}

@Composable
fun WooPosSettingsHardwareBarcodeScannerContent(
    onSetupScannerClicked: () -> Unit,
    onDocumentationClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        WooPosSettingsDetailsMenuItem(
            icon = Icons.Default.Settings,
            title = stringResource(R.string.woopos_settings_barcode_scanner_setup_title),
            subtitle = stringResource(R.string.woopos_settings_barcode_scanner_setup_subtitle),
            onClick = onSetupScannerClicked
        )

        WooPosSettingsDetailsMenuItem(
            icon = Icons.Default.Description,
            title = stringResource(R.string.woopos_settings_barcode_scanner_documentation_title),
            subtitle = stringResource(R.string.woopos_settings_barcode_scanner_documentation_subtitle),
            onClick = onDocumentationClicked
        )
    }
}

@WooPosPreview
@Composable
fun WooPosSettingsHardwareBarcodeScannerScreenWithoutScannerPreview() {
    WooPosTheme {
        WooPosSettingsHardwareBarcodeScannerContent(
            onSetupScannerClicked = { },
            onDocumentationClicked = { }
        )
    }
}
