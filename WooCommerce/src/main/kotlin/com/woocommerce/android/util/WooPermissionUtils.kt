package com.woocommerce.android.util

import android.Manifest.permission
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object WooPermissionUtils {
    /*
     * open the device's settings page for this app so the user can edit permissions
     */
    fun showAppSettings(context: Context, openInNewStack: Boolean = true) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        if (openInNewStack) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun hasFineLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBluetoothPermissions(context: Context) =
        ContextCompat.checkSelfPermission(
            context,
            permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            context,
            permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED

    fun requestFineLocationPermission(requestPermissionLauncher: ActivityResultLauncher<String>) {
        requestPermissionLauncher.launch(permission.ACCESS_FINE_LOCATION)
    }
}
