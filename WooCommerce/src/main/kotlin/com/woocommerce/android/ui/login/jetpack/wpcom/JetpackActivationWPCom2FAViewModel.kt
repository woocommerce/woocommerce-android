package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.login.R
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPCom2FAViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackAccountRepository: JetpackActivationRepository,
) : JetpackActivationWPComPostLoginViewModel(savedStateHandle, selectedSite, jetpackAccountRepository) {
    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onSMSLinkClick() {
        triggerEvent(ShowSnackbar(R.string.requesting_otp))
    }

    fun onContinueClick() {
        TODO()
    }
}
