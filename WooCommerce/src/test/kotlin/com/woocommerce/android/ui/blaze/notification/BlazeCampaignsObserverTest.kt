package com.woocommerce.android.ui.blaze.notification

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.extensions.daysLater
import com.woocommerce.android.notifications.local.LocalNotificationScheduler
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.blaze.BlazeCampaignModel
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.util.Date

class BlazeCampaignsObserverTest {
    private val site = SiteModel().apply { siteId = 1 }
    private val selectedSite = mock<SelectedSite>()
    private val appPrefsWrapper = mock<AppPrefsWrapper>()
    private val blazeCampaignsStore = mock<BlazeCampaignsStore>()
    private val localNotificationScheduler = mock<LocalNotificationScheduler>()

    private lateinit var blazeCampaignsObserver: BlazeCampaignsObserver

    private fun initBlazeCampaignsObserver(
        notificationShownBefore: Boolean = false,
        campaigns: List<BlazeCampaignModel> = listOf(BLAZE_CAMPAIGN_MODEL)
    ) {
        whenever(selectedSite.observe()).thenReturn(flowOf(site))
        whenever(selectedSite.get()).thenReturn(site)
        whenever(appPrefsWrapper.getBlazeNoCampaignReminderShown(site.siteId)).thenReturn(notificationShownBefore)
        whenever(blazeCampaignsStore.observeBlazeCampaigns(site)).thenReturn(flowOf(campaigns))
        blazeCampaignsObserver = BlazeCampaignsObserver(
            selectedSite,
            appPrefsWrapper,
            blazeCampaignsStore,
            localNotificationScheduler
        )
    }

    @Test
    fun `when notification shown before, then don't schedule notification`() = runTest {
        initBlazeCampaignsObserver(notificationShownBefore = true)

        blazeCampaignsObserver.observeAndScheduleNotifications()

        verifyNoInteractions(localNotificationScheduler)
    }

    @Test
    fun `when no campaign, then don't schedule notification`() = runTest {
        initBlazeCampaignsObserver(campaigns = emptyList())

        blazeCampaignsObserver.observeAndScheduleNotifications()

        verifyNoInteractions(localNotificationScheduler)
    }

    @Test
    fun `when there are campaigns, schedule the notification`() = runTest {
        val campaign1 = BLAZE_CAMPAIGN_MODEL
        val campaign2 = BLAZE_CAMPAIGN_MODEL.copy(durationInDays = 8)
        val campaign3 = BLAZE_CAMPAIGN_MODEL.copy(uiStatus = "completed")
        val campaignList = listOf(campaign1, campaign2, campaign3)
        initBlazeCampaignsObserver(campaigns = campaignList)

        blazeCampaignsObserver.observeAndScheduleNotifications()

        verify(localNotificationScheduler).scheduleNotification(any())
    }

    @Test
    fun `when there is endless campaign, then don't schedule notification`() = runTest {
        initBlazeCampaignsObserver(campaigns = listOf(BLAZE_CAMPAIGN_MODEL.copy(isEndlessCampaign = true)))

        blazeCampaignsObserver.observeAndScheduleNotifications()

        verify(localNotificationScheduler).cancelScheduledNotification(any())
        verifyNoMoreInteractions(localNotificationScheduler)
    }

    companion object {
        private val BLAZE_CAMPAIGN_MODEL = BlazeCampaignModel(
            campaignId = "2",
            title = "title",
            imageUrl = "image_url",
            startTime = Date().daysLater(1),
            durationInDays = 7,
            uiStatus = "active",
            impressions = 2,
            clicks = 1,
            targetUrn = "urn:wpcom:post:1:1",
            totalBudget = 100.0,
            spentBudget = 1.0,
            isEndlessCampaign = false
        )
    }
}
