package com.woocommerce.android.ui.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_HIDE_LIST
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.ShouldShowOnboarding.Source.ONBOARDING_LIST
import com.woocommerce.android.ui.onboarding.ShouldShowOnboarding.Source.SETTINGS
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ABOUT_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ADD_FIRST_PRODUCT
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ShouldShowOnboardingTest : BaseUnitTest() {
    private companion object {
        const val CURRENT_SITE_ID = 1234
        val ONBOARDING_TASK_INCOMPLETED_LIST = listOf(
            OnboardingTask(
                type = ABOUT_YOUR_STORE,
                isComplete = false,
                isVisible = true,
                isVisited = false
            ),
            OnboardingTask(
                type = ADD_FIRST_PRODUCT,
                isComplete = true,
                isVisible = true,
                isVisited = false
            )
        )
        val ONBOARDING_TASK_COMPLETED_LIST = listOf(
            OnboardingTask(
                type = ABOUT_YOUR_STORE,
                isComplete = true,
                isVisible = true,
                isVisited = false
            ),
            OnboardingTask(
                type = ADD_FIRST_PRODUCT,
                isComplete = true,
                isVisible = true,
                isVisited = false
            )
        )
    }

    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    val sut: ShouldShowOnboarding = ShouldShowOnboarding(
        appPrefsWrapper,
        selectedSite,
        analyticsTrackerWrapper
    )

    @Before
    fun setup() {
        whenever(selectedSite.getSelectedSiteId()).thenReturn(CURRENT_SITE_ID)
        givenStoreOnboardingHasBeenShownAtLeastOnce(shown = false)
    }

    @Test
    fun `when onboarding list of tasks is empty, then return false`() {
        val show = sut.showForTasks(emptyList())

        assertFalse(show)
    }

    @Test
    fun `when all tasks are completed, then return false`() {
        val show = sut.showForTasks(ONBOARDING_TASK_COMPLETED_LIST)

        assertFalse(show)
    }

    @Test
    fun `when all tasks are completed and onboarding has been shown at least once, then track onboarding completed`() {
        givenStoreOnboardingHasBeenShownAtLeastOnce(shown = true)

        sut.showForTasks(ONBOARDING_TASK_COMPLETED_LIST)

        verify(analyticsTrackerWrapper).track(STORE_ONBOARDING_COMPLETED)
    }

    @Test
    fun `when all tasks are completed and onboarding, then mark onboarding completed locally`() {
        givenStoreOnboardingHasBeenShownAtLeastOnce(shown = true)

        sut.showForTasks(ONBOARDING_TASK_COMPLETED_LIST)

        verify(appPrefsWrapper).updateOnboardingCompletedStatus(CURRENT_SITE_ID, true)
    }

    @Test
    fun `given onboarding is enabled from settings, when at least one task is incomplete, then return true`() {
        givenOnboardingIsEnabledFromSettings(enabled = true)

        val show = sut.showForTasks(ONBOARDING_TASK_INCOMPLETED_LIST)

        assertTrue(show)
    }

    @Test
    fun `given onboarding is enabled from settings, when at least one task is incomplete, then mark onboarding shown`() {
        givenOnboardingIsEnabledFromSettings(enabled = true)

        sut.showForTasks(ONBOARDING_TASK_INCOMPLETED_LIST)

        verify(appPrefsWrapper).setStoreOnboardingShown(CURRENT_SITE_ID)
    }

    @Test
    fun `given incomplete tasks, when hiding onboarding from the list, then track onboarding hidden from list`() {
        sut.showForTasks(ONBOARDING_TASK_INCOMPLETED_LIST)

        sut.updateOnboardingVisibilitySetting(show = false, source = ONBOARDING_LIST)

        verify(analyticsTrackerWrapper).track(
            stat = STORE_ONBOARDING_HIDE_LIST,
            properties = mapOf(
                AnalyticsTracker.KEY_HIDE_ONBOARDING_SOURCE to ONBOARDING_LIST.name.lowercase(),
                AnalyticsTracker.KEY_HIDE_ONBOARDING_LIST_VALUE to true,
                AnalyticsTracker.KEY_ONBOARDING_PENDING_TASKS to AnalyticsTracker.VALUE_STORE_DETAILS
            )
        )
    }

    @Test
    fun `given completed tasks, when onboarding enabled from settings, then track onboarding enabled from settings`() {
        sut.showForTasks(ONBOARDING_TASK_COMPLETED_LIST)

        sut.updateOnboardingVisibilitySetting(show = true, source = SETTINGS)

        verify(analyticsTrackerWrapper).track(
            stat = STORE_ONBOARDING_HIDE_LIST,
            properties = mapOf(
                AnalyticsTracker.KEY_HIDE_ONBOARDING_SOURCE to SETTINGS.name.lowercase(),
                AnalyticsTracker.KEY_HIDE_ONBOARDING_LIST_VALUE to false,
                AnalyticsTracker.KEY_ONBOARDING_PENDING_TASKS to ""
            )
        )
    }

    private fun givenStoreOnboardingHasBeenShownAtLeastOnce(shown: Boolean) {
        whenever(appPrefsWrapper.getStoreOnboardingShown(CURRENT_SITE_ID)).thenReturn(shown)
    }

    private fun givenOnboardingIsEnabledFromSettings(enabled: Boolean) {
        whenever(appPrefsWrapper.getOnboardingSettingVisibility(CURRENT_SITE_ID)).thenReturn(enabled)
    }
}
