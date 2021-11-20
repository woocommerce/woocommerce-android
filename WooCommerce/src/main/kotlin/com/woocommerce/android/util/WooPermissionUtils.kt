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

    fun hasBluetoothRequiredPermissions(context: Context): Boolean {
        return if (SystemVersionUtils.isAtLeastS()) {
            context.isGranted(permission.ACCESS_FINE_LOCATION) &&
                context.isGranted(permission.BLUETOOTH_CONNECT) &&
                context.isGranted(permission.BLUETOOTH_SCAN)
        } else {
            context.isGranted(permission.ACCESS_FINE_LOCATION)
        }
    }

    fun requestBluetoothPermissions(context: Context, requestPermissionLauncher: ActivityResultLauncher<Array<String>>) {
        val deniedPermissions = mutableListOf<String>()
        if (!context.isGranted(permission.ACCESS_FINE_LOCATION)) {
            deniedPermissions.add(permission.ACCESS_FINE_LOCATION)
        }
        if (SystemVersionUtils.isAtLeastS()) {
            if (!context.isGranted(permission.BLUETOOTH_CONNECT)) {
                deniedPermissions.add(permission.BLUETOOTH_CONNECT)
            }
            if (!context.isGranted(permission.BLUETOOTH_SCAN)) {
                deniedPermissions.add(permission.BLUETOOTH_SCAN)
            }
        }
        requestPermissionLauncher.launch(deniedPermissions.toTypedArray())
    }

    private fun Context.isGranted(permission:String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
