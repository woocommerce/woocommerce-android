package com.woocommerce.android.ui.onboarding

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTask
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ABOUT_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.ADD_FIRST_PRODUCT
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.CUSTOMIZE_DOMAIN
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.LAUNCH_YOUR_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.LOCAL_NAME_STORE
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.MOBILE_UNSUPPORTED
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.PAYMENTS
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS

fun OnboardingTask.toOnboardingTaskState() =
    when (type) {
        ABOUT_YOUR_STORE -> OnboardingTaskUi(AboutYourStoreTaskRes, isCompleted = isComplete)
        LAUNCH_YOUR_STORE -> OnboardingTaskUi(LaunchStoreTaskRes, isCompleted = isComplete)
        CUSTOMIZE_DOMAIN -> OnboardingTaskUi(CustomizeDomainTaskRes, isCompleted = isComplete)
        WC_PAYMENTS -> OnboardingTaskUi(SetupWooPaymentsTaskRes, isCompleted = isComplete)
        PAYMENTS -> OnboardingTaskUi(SetupPaymentsTaskRes, isCompleted = isComplete)
        ADD_FIRST_PRODUCT -> OnboardingTaskUi(AddProductTaskRes, isCompleted = isComplete)
        LOCAL_NAME_STORE -> OnboardingTaskUi(NameYourStoreTaskRes, isCompleted = isComplete)
        MOBILE_UNSUPPORTED -> error("Unknown task type is not allowed in UI layer")
    }

fun OnboardingTaskUi.toTrackingKey() =
    when (taskUiResources) {
        AboutYourStoreTaskRes -> AnalyticsTracker.VALUE_STORE_DETAILS
        is AddProductTaskRes -> AnalyticsTracker.VALUE_PRODUCTS
        CustomizeDomainTaskRes -> AnalyticsTracker.VALUE_ADD_DOMAIN
        LaunchStoreTaskRes -> AnalyticsTracker.VALUE_LAUNCH_SITE
        SetupPaymentsTaskRes -> AnalyticsTracker.VALUE_PAYMENTS
        SetupWooPaymentsTaskRes -> AnalyticsTracker.VALUE_WOO_PAYMENTS
        NameYourStoreTaskRes -> AnalyticsTracker.VALUE_LOCAL_NAME_STORE
    }
