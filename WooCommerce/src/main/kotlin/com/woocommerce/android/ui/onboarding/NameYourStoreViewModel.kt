package com.woocommerce.android.ui.onboarding

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NameYourStoreViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    private val onboardingRepository: StoreOnboardingRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: NameYourStoreDialogFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableLiveData(
        NameYourStoreDialogState(
            enteredSiteTitle = selectedSite.get().name,
            isLoading = false,
            isError = false
        )
    )
    val viewState = _viewState

    fun saveSiteTitle(siteTitle: String) {
        launch {
            _viewState.value = _viewState.value?.copy(isLoading = true, isError = false)
            onboardingRepository.saveSiteTitle(siteTitle).fold(
                onSuccess = {
                    if (navArgs.fromOnboarding) {
                        triggerEvent(ShowSnackbar(R.string.store_onboarding_name_your_store_dialog_success))
                    } else {
                        triggerEvent(ShowSnackbar(R.string.settings_name_your_store_dialog_success))
                    }

                    triggerEvent(Exit)
                },
                onFailure = {
                    triggerEvent(ShowSnackbar(R.string.store_onboarding_name_your_store_dialog_failure))
                    _viewState.value = _viewState.value?.copy(isError = true)
                }
            )
            _viewState.value = _viewState.value?.copy(isLoading = false)
        }
    }

    fun onNameYourStoreDismissed() {
        triggerEvent(Exit)
    }

    fun onSiteTitleInputChanged(input: String) {
        _viewState.value = _viewState.value?.copy(enteredSiteTitle = input, isError = false)
    }

    data class NameYourStoreDialogState(
        val enteredSiteTitle: String,
        val isLoading: Boolean,
        val isError: Boolean
    )
}
