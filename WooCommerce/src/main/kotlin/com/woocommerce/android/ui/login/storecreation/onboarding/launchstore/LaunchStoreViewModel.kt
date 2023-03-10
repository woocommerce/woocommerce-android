package com.woocommerce.android.ui.login.storecreation.onboarding.launchstore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.extensions.isCurrentPlanEcommerceTrial
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.UserAgent
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class LaunchStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val launchStoreOnboardingRepository: StoreOnboardingRepository,
    private val selectedSite: SelectedSite,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    private companion object {
        const val PLANS_URL = "https://wordpress.com/plans/"
    }

    private val _viewState = MutableStateFlow(
        LaunchStoreState(
            isTrialPlan = selectedSite.get().isCurrentPlanEcommerceTrial,
            isStoreLaunched = false,
            isLoading = false,
            siteUrl = selectedSite.get().url,
            displayUrl = selectedSite.get().getAbsoluteUrl()
        )
    )
    val viewState = _viewState.asLiveData()

    fun launchStore() {
        _viewState.value = _viewState.value.copy(isLoading = true)
        launch {
            val result = launchStoreOnboardingRepository.launchStore()
            when {
                result.isFailure -> TODO()
                result.isSuccess -> _viewState.value = _viewState.value.copy(isStoreLaunched = true)
            }
        }
        _viewState.value = _viewState.value.copy(isLoading = true)
    }

    fun onUpgradePlanBannerClicked() {
        triggerEvent(
            UpgradeToEcommercePlan(
                url = PLANS_URL + selectedSite.get().siteId
            )
        )
    }

    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun shareStoreUrl() {
        triggerEvent(ShareStoreUrl(selectedSite.get().url))
    }

    private fun SiteModel.getAbsoluteUrl(): String = runCatching { URL(url).host }.getOrDefault("")

    data class LaunchStoreState(
        val isTrialPlan: Boolean,
        val isStoreLaunched: Boolean,
        val isLoading: Boolean,
        val siteUrl: String,
        val displayUrl: String
    )

    data class UpgradeToEcommercePlan(val url: String) : MultiLiveEvent.Event()
    data class ShareStoreUrl(val url: String) : MultiLiveEvent.Event()
}
