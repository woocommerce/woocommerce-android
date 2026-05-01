package com.woocommerce.android.ui.woopos.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
}
