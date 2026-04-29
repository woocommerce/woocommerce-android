package com.woocommerce.android.notifications.push

import com.woocommerce.android.extensions.isVersionAtLeast
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject

class CheckWooPluginPushNotificationsSupport @Inject constructor(
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion,
    private val getWooCorePluginCachedVersion: GetWooCorePluginCachedVersion
) {
    companion object {
        const val PUSH_NOTIFICATIONS_MIN_WC_VERSION = "10.8.0"
    }

    suspend operator fun invoke(forceRefresh: Boolean): Result {
        val wcVersion = if (forceRefresh) {
            fetchActiveWCPluginVersion()
        } else {
            getWooCorePluginCachedVersion()
        } ?: return Result.Error

        return if (wcVersion.isVersionAtLeast(PUSH_NOTIFICATIONS_MIN_WC_VERSION)) {
            Result.Compatible
        } else {
            Result.UpdateRequired(currentVersion = wcVersion)
        }
    }

    sealed interface Result {
        data object Compatible : Result
        data class UpdateRequired(val currentVersion: String) : Result
        data object Error : Result
    }
}
