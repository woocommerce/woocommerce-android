package com.woocommerce.android.ui.login.storecreation.onboarding.launchstore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class LaunchStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val launchStoreOnboardingRepository: StoreOnboardingRepository,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {

    val viewState = MutableStateFlow(
        LaunchStoreState()
    ).asLiveData()

    data class LaunchStoreState(
        val isTrialPlan: Boolean = false,
        val isStoreLaunched: Boolean = false,
        val isLoading: Boolean = false,
        val siteUrl: String? = null
    )
}
