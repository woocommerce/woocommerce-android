package com.woocommerce.android.ui.woopos.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooPermissionUtils
import dagger.Reusable
import javax.inject.Inject

@Reusable
class WooPosPermissionUtils @Inject constructor(
    private val context: Context
) {
    fun showAppSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun showBluetoothSettings() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            WooLog.e(WooLog.T.POS, "Bluetooth settings activity not found.", e)
        }
    }

    fun hasBluetoothScanAndConnectPermissions(): Boolean =
        WooPermissionUtils.hasBluetoothScanPermission(context) &&
            WooPermissionUtils.hasBluetoothConnectPermission(context)
}
