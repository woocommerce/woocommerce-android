package com.woocommerce.android.ui.login.storecreation.onboarding

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class StoreOnboardingViewModelTest {

    private val savedState: SavedStateHandle = SavedStateHandle()
    private val onboardingRepository: StoreOnboardingRepository = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val viewModel = StoreOnboardingViewModel(
        savedState,
        onboardingRepository,
        analyticsTrackerWrapper
    )
}

