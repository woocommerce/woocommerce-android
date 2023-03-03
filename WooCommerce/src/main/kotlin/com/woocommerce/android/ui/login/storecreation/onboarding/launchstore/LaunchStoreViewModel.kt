package com.woocommerce.android.ui.login.storecreation.onboarding.launchstore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LaunchStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val launchStoreOnboardingRepository: StoreOnboardingRepository,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedStateHandle) {

    val _viewState = MutableStateFlow(
        LaunchStoreState(
            siteUrl = selectedSite.get().url
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


    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    data class LaunchStoreState(
        val isTrialPlan: Boolean = false,
        val isStoreLaunched: Boolean = false,
        val isLoading: Boolean = false,
        val siteUrl: String
    )
}
