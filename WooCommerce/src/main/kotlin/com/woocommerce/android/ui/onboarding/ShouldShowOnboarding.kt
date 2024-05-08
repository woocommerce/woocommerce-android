package com.woocommerce.android.ui.onboarding

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_COMPLETED
import com.woocommerce.android.analytics.AnalyticsEvent.STORE_ONBOARDING_HIDE_LIST
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ABOUT_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ADD_FIRST_PRODUCT
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.CUSTOMIZE_DOMAIN
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.LOCAL_NAME_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.PAYMENTS
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // Needed to keep track of the pending tasks when tracking
class ShouldShowOnboarding @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private var pendingTasks: List<OnboardingTask> = emptyList()

    fun showForTasks(tasks: List<OnboardingTask>): Boolean {
        if (tasks.isEmpty()) return false

        pendingTasks = tasks.filter { !it.isComplete }

        val siteId = selectedSite.getSelectedSiteId()
        val areAllTaskCompleted = if (tasks.all { it.isComplete }) {
            if (appPrefsWrapper.getStoreOnboardingShown(siteId) && !appPrefsWrapper.isOnboardingCompleted(siteId)) {
                analyticsTrackerWrapper.track(stat = STORE_ONBOARDING_COMPLETED)
            }
            true
        } else {
            false
        }

        return if (!areAllTaskCompleted &&
            (FeatureFlag.DYNAMIC_DASHBOARD.isEnabled() || isOnboardingListSettingVisible())
        ) {
            appPrefsWrapper.setStoreOnboardingShown(siteId)
            true
        } else {
            false
        }
    }

    fun isOnboardingMarkedAsCompleted(): Boolean =
        appPrefsWrapper.isOnboardingCompleted(selectedSite.getSelectedSiteId())

    fun updateOnboardingVisibilitySetting(show: Boolean, source: Source) {
        analyticsTrackerWrapper.track(
            stat = STORE_ONBOARDING_HIDE_LIST,
            properties = mapOf(
                AnalyticsTracker.KEY_HIDE_ONBOARDING_SOURCE to source.name.lowercase(),
                AnalyticsTracker.KEY_HIDE_ONBOARDING_LIST_VALUE to !show,
                AnalyticsTracker.KEY_ONBOARDING_PENDING_TASKS to pendingTasks
                    .map { getTaskTrackingKey(it.type) }.joinToString()
            )
        )
        appPrefsWrapper.setOnboardingSettingVisibility(selectedSite.getSelectedSiteId(), show)
    }

    fun isOnboardingListSettingVisible() =
        appPrefsWrapper.getOnboardingSettingVisibility(selectedSite.getSelectedSiteId())

    private fun getTaskTrackingKey(type: OnboardingTaskType) =
        when (type) {
            ABOUT_YOUR_STORE -> AnalyticsTracker.VALUE_STORE_DETAILS
            ADD_FIRST_PRODUCT -> AnalyticsTracker.VALUE_PRODUCTS
            LAUNCH_YOUR_STORE -> AnalyticsTracker.VALUE_LAUNCH_SITE
            CUSTOMIZE_DOMAIN -> AnalyticsTracker.VALUE_ADD_DOMAIN
            WC_PAYMENTS,
            PAYMENTS -> AnalyticsTracker.VALUE_PAYMENTS
            LOCAL_NAME_STORE -> AnalyticsTracker.VALUE_LOCAL_NAME_STORE

            MOBILE_UNSUPPORTED -> null
        }

    enum class Source {
        ONBOARDING_LIST,
        SETTINGS
    }
}
