package com.woocommerce.android.ui.blaze.notification

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.daysLater
import com.woocommerce.android.notifications.local.LocalNotification.BlazeNoCampaignReminderNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.blaze.BlazeCampaignsDao
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Calendar
import javax.inject.Inject

/**
 * A utility class that can observe changes in Blaze campaigns for the selected site and schedule local notifications
 * for Blaze.
 */
class BlazeCampaignsObserver @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val localNotificationScheduler: LocalNotificationScheduler,
) {
    suspend fun observeAndScheduleNotifications() {
        selectedSite.observe()
            .filterNotNull()
            .filter { !appPrefsWrapper.getBlazeNoCampaignReminderShown(it.siteId) }
            .distinctUntilChanged { old, new -> new.id == old.id }
            .collectLatest { observeBlazeCampaigns(it) }
    }

    private suspend fun observeBlazeCampaigns(site: SiteModel) {
        blazeCampaignsStore.observeBlazeCampaigns(site)
            .filter { it.isNotEmpty() }
            .collectLatest { scheduleNotification(it) }
    }

    private fun scheduleNotification(campaigns: List<BlazeCampaignsDao.BlazeCampaignEntity>) {
        if (campaigns.isEmpty()) {
            // There are no campaigns. Skip scheduling the notification.
            return
        }

        val delayForNotification = calculateDelayForNotification(campaigns)

        localNotificationScheduler.scheduleNotification(
            BlazeNoCampaignReminderNotification(selectedSite.get().siteId, delayForNotification)
        )
    }

    private fun calculateDelayForNotification(campaigns: List<BlazeCampaignsDao.BlazeCampaignEntity>): Long {
        val latestEndTime = campaigns.maxOf { it.startTime.daysLater(it.durationInDays) }
        val notificationTime = latestEndTime.daysLater(DAYS_DURATION_NO_CAMPAIGN_REMINDER_NOTIFICATION)
        return notificationTime.time - Calendar.getInstance().time.time
    }

    companion object {
        const val DAYS_DURATION_NO_CAMPAIGN_REMINDER_NOTIFICATION = 30
    }
}
