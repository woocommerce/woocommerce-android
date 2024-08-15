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

    open fun getTitleString(resourceProvider: ResourceProvider) = resourceProvider.getString(title)

    open fun getDescriptionString(resourceProvider: ResourceProvider) = resourceProvider.getString(description)
}
