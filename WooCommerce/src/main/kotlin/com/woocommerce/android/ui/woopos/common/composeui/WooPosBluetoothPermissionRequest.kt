package com.woocommerce.android.ui.woopos.common.composeui

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat

@Composable
fun rememberWooPosBluetoothPermissionRequest(
    onResult: (granted: Boolean, shouldShowRationale: Boolean) -> Unit,
): () -> Unit {
    val activity = LocalActivity.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val shouldShowRationale = if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.BLUETOOTH_SCAN
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            true
        }
        onResult(allGranted, shouldShowRationale)
    }
    return remember(launcher) { { launcher.launch(bluetoothPermissions()) } }
}

private fun bluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH)
    }
