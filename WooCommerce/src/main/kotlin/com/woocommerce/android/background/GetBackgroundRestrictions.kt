package com.woocommerce.android.background

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
import android.os.Build
import android.os.PowerManager
import javax.inject.Inject

class GetBackgroundRestrictions @Inject constructor(
    private val appContext: Context
) {
    operator fun invoke() = BackgroundRestrictions(
        isDataSaverEnabled = (appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
            ?.let { it.restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED } ?: false,

        isPowerSaveModeEnabled = (appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager)
            ?.isPowerSaveMode ?: false,

        isBackgroundRestricted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isBackgroundRestricted ?: false
        } else {
            false
        }
    )

    data class BackgroundRestrictions(
        val isDataSaverEnabled: Boolean,
        val isPowerSaveModeEnabled: Boolean,
        val isBackgroundRestricted: Boolean
    ) {
        val isAnyRestrictionActive: Boolean
            get() = isDataSaverEnabled || isPowerSaveModeEnabled || isBackgroundRestricted
    }
}
