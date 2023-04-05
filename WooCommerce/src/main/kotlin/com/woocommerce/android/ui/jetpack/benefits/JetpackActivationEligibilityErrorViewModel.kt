package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppUrls.WOOCOMMERCE_USER_ROLES
import com.woocommerce.android.extensions.toCamelCase
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.UserRole
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.ui.jetpack.JetpackActivationEligibilityErrorFragmentArgs
import com.woocommerce.android.ui.jetpack.benefits.FetchJetpackStatus.JetpackStatusFetchResponse
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JetpackActivationEligibilityErrorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val fetchJetpackStatus: FetchJetpackStatus,
    private val userEligibilityFetcher: UserEligibilityFetcher
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationEligibilityErrorFragmentArgs by savedStateHandle.navArgs()
    private val isRetrying =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = false, key = "is-loading")

    val viewState = combine(
        flowOf(Pair(navArgs.username, navArgs.role)),
        isRetrying
    ) { (username, role), isRetrying ->
        ViewState(
            username = username,
            role = role.replace("_", " ").toCamelCase(),
            isRetrying = isRetrying
        )
    }.asLiveData()

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    fun onLearnMoreButtonClicked() {
        triggerEvent(OpenUrlEvent(WOOCOMMERCE_USER_ROLES))
    }

    fun onRetryButtonClicked() = launch {
        isRetrying.value = true
        val jetpackStatusResult = fetchJetpackStatus()
        handleJetpackStatusResult(jetpackStatusResult)
        isRetrying.value = false
    }

    private fun handleJetpackStatusResult(
        result: Result<Pair<JetpackStatus, JetpackStatusFetchResponse>>
    ) {
        result.fold(
            onSuccess = { pair ->
                if (pair.second == JetpackStatusFetchResponse.SUCCESS) {
                    triggerEvent(
                        StartJetpackActivationForApplicationPasswords(
                            siteUrl = selectedSite.get().url,
                            jetpackStatus = pair.first
                        )
                    )
                } else if (pair.second == JetpackStatusFetchResponse.NOT_FOUND) {
                    launch {
                        userEligibilityFetcher.fetchUserInfo().fold(
                            onSuccess = {
                                val hasInstallCapability = it.roles.contains(UserRole.Administrator)
                                if (hasInstallCapability) {
                                    triggerEvent(
                                        StartJetpackActivationForApplicationPasswords(
                                            siteUrl = selectedSite.get().url,
                                            jetpackStatus = pair.first
                                        )
                                    )
                                }
                            },
                            onFailure = { /* Still not eligible, keep showing error screen */ }
                        )
                    }
                }
            },
            onFailure = { /* Still not eligible, keep showing error screen */ }
        )
    }

    fun onHelpButtonClicked() {
        triggerEvent(NavigateToHelpScreen(HelpOrigin.JETPACK_ACTIVATION_USER_ELIGIBILITY_ERROR))
    }

    data class ViewState(
        val username: String,
        val role: String,
        val isRetrying: Boolean
    )

    data class StartJetpackActivationForApplicationPasswords(
        val siteUrl: String,
        val jetpackStatus: JetpackStatus
    ) : Event()

    data class OpenUrlEvent(val url: String) : Event()
}
