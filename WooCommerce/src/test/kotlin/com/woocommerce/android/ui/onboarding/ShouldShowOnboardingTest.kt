package com.woocommerce.android.ui.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_COMPLETED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
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
    fun `given onboarding is enabled from settings, when at least one task is incomplete, then return true`() {
        val show = sut.showForTasks(ONBOARDING_TASK_INCOMPLETED_LIST)

        assertTrue(show)
    }

    @Test
    fun `given onboarding is enabled from settings, when at least one task is incomplete, then mark onboarding shown`() {
        sut.showForTasks(ONBOARDING_TASK_INCOMPLETED_LIST)

        verify(appPrefsWrapper).setStoreOnboardingShown(CURRENT_SITE_ID)
    }

    private fun givenStoreOnboardingHasBeenShownAtLeastOnce(shown: Boolean) {
        whenever(appPrefsWrapper.getStoreOnboardingShown(CURRENT_SITE_ID)).thenReturn(shown)
    }
}
