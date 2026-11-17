package com.woocommerce.android.ui.woopos.common.composeui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

@Composable
fun rememberWooPosBluetoothPermissionLauncher(
    onResult: (granted: Boolean, shouldShowRationale: Boolean) -> Unit,
): ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>> {
    val context = LocalContext.current
    return rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        val shouldShowRationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.shouldShowRequestPermissionRationale(
                context as Activity,
                Manifest.permission.BLUETOOTH_SCAN
            ) || ActivityCompat.shouldShowRequestPermissionRationale(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            true
        }
        onResult(allGranted, shouldShowRationale)
    }
}
