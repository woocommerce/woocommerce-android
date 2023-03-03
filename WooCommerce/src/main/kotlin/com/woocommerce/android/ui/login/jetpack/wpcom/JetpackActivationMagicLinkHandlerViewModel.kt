package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.jetpack.JetpackActivationRepository
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JetpackActivationMagicLinkHandlerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selectedSite: SelectedSite,
    jetpackActivationRepository: JetpackActivationRepository
) : JetpackActivationWPComPostLoginViewModel(savedStateHandle, selectedSite, jetpackActivationRepository) {
    private val navArgs: JetpackActivationMagicLinkHandlerFragmentArgs by savedStateHandle.navArgs()

    init {
        launch {
            onLoginSuccess(navArgs.jetpackStatus)
        }
    }
}
