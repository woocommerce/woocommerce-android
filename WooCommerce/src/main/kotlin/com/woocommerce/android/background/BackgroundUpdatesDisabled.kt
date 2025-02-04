package com.woocommerce.android.background

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
import android.os.Build
import android.os.PowerManager
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

class BackgroundUpdatesDisabled @Inject constructor(
    private val appContext: Context,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    operator fun invoke() {
        // Checks user’s Data Saver settings
        val isNetworkRestricted =
            (appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.let {
                it.restrictBackgroundStatus == RESTRICT_BACKGROUND_STATUS_ENABLED
            } ?: false

        // Checks user’s Power Save Mode settings.
        val isPowerSaveModeEnabled =
            (appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false

        // Checks user’s background updates are disabled
        val isBackgroundUpdatesDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            (appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.isBackgroundRestricted ?: false
        } else {
            false
        }

        if (isNetworkRestricted || isPowerSaveModeEnabled || isBackgroundUpdatesDisabled) {
            analyticsTrackerWrapper.track(AnalyticsEvent.BACKGROUND_UPDATES_DISABLED)
        }
    }
}
