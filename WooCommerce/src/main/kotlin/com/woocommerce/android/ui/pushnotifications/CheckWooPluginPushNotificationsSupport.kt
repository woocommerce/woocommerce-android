package com.woocommerce.android.ui.pushnotifications

import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import javax.inject.Inject

class CheckWooPluginPushNotificationsSupport @Inject constructor(
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion
) {
    companion object {
        const val PUSH_NOTIFICATIONS_MIN_WC_VERSION = "10.6.0"
    }

    suspend operator fun invoke(): Result {
        val wcVersion = fetchActiveWCPluginVersion() ?: return Result.Error

        return if (wcVersion.isVersionAtLeast(PUSH_NOTIFICATIONS_MIN_WC_VERSION)) {
            Result.Compatible
        } else {
            Result.UpdateRequired
        }
    }

    sealed interface Result {
        data object Compatible : Result
        data object UpdateRequired : Result
        data object Error : Result
    }
}
