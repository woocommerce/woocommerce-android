package com.woocommerce.android.ui.blaze.notification

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.daysLater
import com.woocommerce.android.notifications.local.LocalNotification.BlazeNoCampaignReminderNotification
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.notifications.local.LocalNotificationType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.CampaignStatusUi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Calendar
import java.util.concurrent.TimeUnit
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
            .filter { !appPrefsWrapper.isBlazeNoCampaignReminderShown }
            .distinctUntilChanged { old, new -> new.id == old.id }
            .collectLatest { observeBlazeCampaigns(it) }
    }

    private suspend fun observeBlazeCampaigns(site: SiteModel) {
        blazeCampaignsStore.observeBlazeCampaigns(site)
            .filter { it.isNotEmpty() }
            .collectLatest { processCampaigns(it) }
    }

    private fun processCampaigns(campaigns: List<BlazeCampaignModel>) {
        if (campaigns.isEmpty()) {
            // There are no campaigns. Skip scheduling the notification.
            return
        } else if (hasActiveEndlessCampaigns(campaigns)) {
            appPrefsWrapper.removeBlazeFirstTimeWithoutCampaign()
            localNotificationScheduler.cancelScheduledNotification(LocalNotificationType.BLAZE_NO_CAMPAIGN_REMINDER)
        } else if (campaigns.any { CampaignStatusUi.isActive(it.uiStatus) }) {
            // There are active limited campaigns.
            val latestEndTime = getLatestEndTimeOfActiveLimitedCampaigns(campaigns)
            scheduleNotification(latestEndTime)
        } else if (!appPrefsWrapper.existsBlazeFirstTimeWithoutCampaign() ||
            appPrefsWrapper.blazeFirstTimeWithoutCampaign > Calendar.getInstance().time.time
        ) {
            scheduleNotification(Calendar.getInstance().time.time)
        }
    }

    private fun hasActiveEndlessCampaigns(campaigns: List<BlazeCampaignModel>) = campaigns.any {
        it.isEndlessCampaign && CampaignStatusUi.isActive(it.uiStatus)
    }

    private fun getLatestEndTimeOfActiveLimitedCampaigns(campaigns: List<BlazeCampaignModel>): Long {
        val activeLimitedCampaigns = campaigns.filter {
            !it.isEndlessCampaign && CampaignStatusUi.isActive(it.uiStatus)
        }
        return activeLimitedCampaigns.maxOf { it.startTime.daysLater(it.durationInDays) }.time
    }

    private fun scheduleNotification(firstTimeWithoutCampaign: Long) {
        if (appPrefsWrapper.blazeFirstTimeWithoutCampaign == firstTimeWithoutCampaign) {
            // There is already a scheduled notification for firstTimeWithoutCampaign.
            return
        }
        appPrefsWrapper.blazeFirstTimeWithoutCampaign = firstTimeWithoutCampaign

        val notificationTime = firstTimeWithoutCampaign +
            TimeUnit.DAYS.toMillis(DAYS_DURATION_NO_CAMPAIGN_REMINDER_NOTIFICATION)

        val notification = BlazeNoCampaignReminderNotification(
            siteId = selectedSite.get().siteId,
            delay = notificationTime - Calendar.getInstance().time.time
        )

        localNotificationScheduler.scheduleNotification(notification)
    }

    companion object {
        private const val DAYS_DURATION_NO_CAMPAIGN_REMINDER_NOTIFICATION = 30L
    }
}
