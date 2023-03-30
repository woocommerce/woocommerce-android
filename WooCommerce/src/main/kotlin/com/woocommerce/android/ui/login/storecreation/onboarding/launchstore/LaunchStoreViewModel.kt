package com.woocommerce.android.ui.login.storecreation.onboarding.launchstore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.extensions.isFreeTrial
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.Error
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.LaunchStoreError.ALREADY_LAUNCHED
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.LaunchStoreError.GENERIC_ERROR
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.Success
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
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
            isTrialPlan = selectedSite.get().isFreeTrial,
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
            when (result) {
                Success -> _viewState.value = _viewState.value.copy(isStoreLaunched = true)
                is Error -> {
                    when (result.type) {
                        ALREADY_LAUNCHED -> triggerEvent(
                            ShowDialog(
                                titleId = R.string.store_onboarding_store_already_launched_error_title,
                                messageId = R.string.store_onboarding_store_already_launched_error_description,
                                positiveButtonId = R.string.store_onboarding_launch_store_ok
                            )
                        )
                        GENERIC_ERROR -> triggerEvent(
                            ShowDialog(
                                titleId = R.string.store_onboarding_launch_store_generic_error_title,
                                messageId = R.string.store_onboarding_launch_store_generic_error_description,
                                positiveButtonId = R.string.try_again,
                                positiveBtnAction = { _, _ -> launchStore() },
                                negativeButtonId = R.string.cancel
                            )
                        )
                    }
                }
            }
        }
        _viewState.value = _viewState.value.copy(isLoading = false)
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
