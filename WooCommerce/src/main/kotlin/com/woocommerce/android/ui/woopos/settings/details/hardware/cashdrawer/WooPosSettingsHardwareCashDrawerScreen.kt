package com.woocommerce.android.ui.woopos.settings.details.hardware.cashdrawer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerOpenResult
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosSettingsHardwareCashDrawerScreen(
    viewModel: WooPosSettingsHardwareCashDrawerViewModel = hiltViewModel(),
) {
    val savedName by viewModel.drawerName.collectAsState()
    val autoOpen by viewModel.autoOpen.collectAsState()
    val connected by viewModel.isConnected.collectAsState()
    val opening by viewModel.isOpening.collectAsState()
    val result by viewModel.lastResult.collectAsState()
    val selectedPrinter by viewModel.selectedPrinter.collectAsState()
    val discoveredPrinters by viewModel.discoveredPrinters.collectAsState()
    val searching by viewModel.isSearching.collectAsState()
    val connectionError by viewModel.connectionError.collectAsState()
    var name by rememberSaveable(savedName) { mutableStateOf(savedName.orEmpty()) }
    val context = LocalContext.current
    val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    val discoveryPermissions = if (Build.VERSION.SDK_INT >= 37 && context.applicationInfo.targetSdkVersion >= 37) {
        bluetoothPermissions + "android.permission.ACCESS_LOCAL_NETWORK"
    } else {
        bluetoothPermissions
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.all { it }) viewModel.discoverPrinters()
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = WooPosSpacing.Medium.value),
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { WooPosText(text = stringResource(R.string.woopos_cash_drawer_name), style = WooPosTypography.BodyMedium) },
            placeholder = {
                WooPosText(text = stringResource(R.string.woopos_cash_drawer_name_hint), style = WooPosTypography.BodyMedium)
            },
            supportingText = {
                WooPosText(text = stringResource(R.string.woopos_cash_drawer_name_help), style = WooPosTypography.BodySmall)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (name.trim() != savedName.orEmpty()) {
            WooPosOutlinedButtonSmall(
                text = stringResource(android.R.string.ok),
                onClick = { viewModel.setDrawerName(name) },
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                WooPosText(
                    text = stringResource(R.string.woopos_cash_drawer_auto_open),
                    style = WooPosTypography.BodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                WooPosText(
                    text = stringResource(R.string.woopos_cash_drawer_auto_open_help),
                    style = WooPosTypography.BodySmall,
                )
            }
            Switch(checked = autoOpen, onCheckedChange = viewModel::setAutoOpen)
        }

        WooPosText(
            text = stringResource(
                if (connected) R.string.woopos_cash_drawer_status_connected
                else R.string.woopos_cash_drawer_status_disconnected
            ),
            style = WooPosTypography.BodyMedium,
        )
        if (selectedPrinter != null) {
            WooPosText(
                text = stringResource(R.string.woopos_cash_drawer_selected_printer, selectedPrinter?.model.orEmpty(),
                    selectedPrinter?.identifier.orEmpty()),
                style = WooPosTypography.BodyMedium,
            )
            WooPosOutlinedButtonSmall(
                text = stringResource(R.string.woopos_cash_drawer_disconnect_printer),
                onClick = viewModel::disconnectPrinter,
            )
        }
        WooPosOutlinedButtonSmall(
            text = stringResource(R.string.woopos_cash_drawer_find_printers),
            onClick = {
                if (discoveryPermissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }) {
                    viewModel.discoverPrinters()
                } else {
                    permissionLauncher.launch(discoveryPermissions)
                }
            },
            state = if (searching) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
        )
        discoveredPrinters.forEach { printer ->
            WooPosOutlinedButtonSmall(
                text = stringResource(R.string.woopos_cash_drawer_connect_printer,
                    printer.model ?: printer.interfaceType, printer.identifier),
                onClick = { viewModel.connectPrinter(printer) },
            )
        }
        if (connectionError) {
            WooPosText(text = stringResource(R.string.woopos_cash_drawer_connection_failed),
                style = WooPosTypography.BodyMedium)
        }
        WooPosOutlinedButtonSmall(
            text = stringResource(R.string.woopos_cash_drawer_test_open),
            onClick = viewModel::testOpen,
            state = if (opening) WooPosButtonState.LOADING else if (connected) WooPosButtonState.ENABLED
                else WooPosButtonState.DISABLED,
        )
        if (result != null) {
            WooPosText(
                text = stringResource(
                    if (result == WooPosCashDrawerOpenResult.OPEN_REQUESTED) {
                        R.string.woopos_cash_drawer_test_open_success
                    } else {
                        R.string.woopos_cash_drawer_test_open_failed
                    }
                ),
                style = WooPosTypography.BodyMedium,
            )
        }
    }
}
