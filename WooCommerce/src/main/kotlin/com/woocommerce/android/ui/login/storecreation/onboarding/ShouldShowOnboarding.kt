package com.woocommerce.android.ui.login.storecreation.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_COMPLETED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

class ShouldShowOnboarding @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    operator fun invoke(tasks: List<OnboardingTask>): Boolean {
        if (tasks.isEmpty()) return false

        val siteId = selectedSite.getSelectedSiteId()
        val areAllTaskCompleted = if (tasks.all { it.isComplete }) {
            WooLog.d(WooLog.T.ONBOARDING, "All onboarding tasks are completed for siteId: $siteId")
            appPrefsWrapper.markAllOnboardingTasksCompleted(siteId)
            if (appPrefsWrapper.getStoreOnboardingShown(siteId)) {
                analyticsTrackerWrapper.track(stat = STORE_ONBOARDING_COMPLETED)
            }
            true
        } else false

        return if (!areAllTaskCompleted
            && appPrefsWrapper.getOnboardingSettingVisibility(siteId)
            && FeatureFlag.STORE_CREATION_ONBOARDING.isEnabled()
        ) {
            appPrefsWrapper.setStoreOnboardingShown(siteId)
            true
        } else false
    }

    fun isOnboardingMarkedAsCompleted(): Boolean =
        appPrefsWrapper.isOnboardingCompleted(selectedSite.getSelectedSiteId())

    fun updateOnboardingVisibilitySetting(show: Boolean) {
        appPrefsWrapper.setOnboardingSettingVisibility(selectedSite.getSelectedSiteId(), show)
    }
}
