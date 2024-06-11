package com.woocommerce.android.notifications.local

import androidx.annotation.StringRes
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.concurrent.TimeUnit

sealed class LocalNotification(
    open val siteId: Long,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val type: LocalNotificationType,
    val delay: Long,
    val delayUnit: TimeUnit
) {
    open val data: String? = null
    val id = type.hashCode()

    abstract fun getDescriptionString(resourceProvider: ResourceProvider): String

    open fun getTitleString(resourceProvider: ResourceProvider): String {
        return resourceProvider.getString(title)
    }
}
