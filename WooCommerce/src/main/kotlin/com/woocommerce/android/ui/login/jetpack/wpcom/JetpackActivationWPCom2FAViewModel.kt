package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPCom2FAViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackAccountRepository: JetpackActivationRepository,
) : JetpackActivationWPComPostLoginViewModel(savedStateHandle, selectedSite, jetpackAccountRepository) {
    private val isSMSLoadingDialogShown = MutableStateFlow(false)

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onSMSLinkClick() {
        TODO()
    }

    fun onContinueClick() {
        TODO()
    }
}
