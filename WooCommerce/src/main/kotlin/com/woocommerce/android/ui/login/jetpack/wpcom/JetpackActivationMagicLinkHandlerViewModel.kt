package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMagicLinkHandlerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackActivationRepository: JetpackActivationRepository
) : JetpackActivationWPComPostLoginViewModel(savedStateHandle, selectedSite, jetpackActivationRepository) {
    private val navArgs: JetpackActivationMagicLinkHandlerFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = MutableStateFlow(ViewState.Loading)
    val viewState = _viewState.asLiveData()

    init {
        continueLogin()
    }

    fun continueLogin() = launch {
        _viewState.value = ViewState.Loading
        onLoginSuccess(navArgs.jetpackStatus).onFailure {
            _viewState.value = ViewState.Error
        }
    }

    fun onCloseClick() {
        // Disable back button when fetching sites
        if (viewState.value == ViewState.Loading) return
        triggerEvent(Exit)
    }

    enum class ViewState {
        Loading, Error
    }
}
