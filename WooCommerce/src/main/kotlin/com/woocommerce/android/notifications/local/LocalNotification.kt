package com.woocommerce.android.notifications.local

import androidx.annotation.StringRes
import com.woocommerce.android.R
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

    val tag
        get() = "$type:$siteId"

    open fun getTitleString(resourceProvider: ResourceProvider) = resourceProvider.getString(title)

    open fun getDescriptionString(resourceProvider: ResourceProvider) = resourceProvider.getString(description)

    data class BlazeNoCampaignReminderNotification(
        override val siteId: Long,
        val daysToCampaignEnd: Int
    ) : LocalNotification(
        siteId = siteId,
        title = R.string.local_notification_blaze_no_campaign_reminder_title,
        description = R.string.local_notification_blaze_no_campaign_reminder_description,
        type = LocalNotificationType.BLAZE_NO_CAMPAIGN_REMINDER,
        delay = daysToCampaignEnd.toLong() + 30,
        delayUnit = TimeUnit.DAYS
    )
}
