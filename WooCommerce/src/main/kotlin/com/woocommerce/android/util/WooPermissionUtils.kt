package com.woocommerce.android.util

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build.VERSION_CODES
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
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

    fun hasFineLocationPermission(context: Context) = context.checkIfPermissionGiven(ACCESS_FINE_LOCATION)

    fun shouldShowFineLocationPermissionRationale(activity: Activity): Boolean {
        if (hasFineLocationPermission(activity)) return false

        return ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)
    }

    fun hasBluetoothScanPermission(context: Context) =
        SystemVersionUtils.isAtMostR() || context.checkIfPermissionGiven(BLUETOOTH_SCAN)

    fun hasBluetoothConnectPermission(context: Context) =
        SystemVersionUtils.isAtMostR() || context.checkIfPermissionGiven(BLUETOOTH_CONNECT)

    fun requestFineLocationPermission(requestPermissionLauncher: ActivityResultLauncher<String>) {
        requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
    }

    fun requestScanAndConnectBluetoothPermission(launcher: ActivityResultLauncher<Array<String>>) {
        launcher.launch(arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT))
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun hasNotificationsPermission(context: Context) = context.checkIfPermissionGiven(POST_NOTIFICATIONS)

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun requestNotificationsPermission(launcher: ActivityResultLauncher<String>) {
        launcher.launch(POST_NOTIFICATIONS)
    }

    @RequiresApi(VERSION_CODES.TIRAMISU)
    fun shouldShowNotificationsRationale(activity: Activity): Boolean {
        if (hasFineLocationPermission(activity)) return false

        return ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS)
    }

    private fun Context.checkIfPermissionGiven(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
