package com.woocommerce.android.ui.blaze.notification

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.local.LocalNotification.BlazeAbandonedCampaignReminderNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

/**
 * A utility class that schedules a reminder after a campaign is abandoned.
 */
class AbandonedCampaignReminder @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val localNotificationScheduler: LocalNotificationScheduler,
) {
    private val notification
        get() = BlazeAbandonedCampaignReminderNotification(selectedSite.get().siteId)

    fun scheduleReminderIfNeeded() {
        if (!appPrefsWrapper.getBlazeCampaignCreated(selectedSite.get().siteId) &&
            !appPrefsWrapper.getBlazeAbandonedCampaignReminderShown(selectedSite.get().siteId)
        ) {
            localNotificationScheduler.scheduleNotification(notification)
        }
    }

    fun setBlazeCampaignCreated() {
        appPrefsWrapper.setBlazeCampaignCreated(selectedSite.get().siteId)
        localNotificationScheduler.cancelScheduledNotification(notification.tag)
    }
}
