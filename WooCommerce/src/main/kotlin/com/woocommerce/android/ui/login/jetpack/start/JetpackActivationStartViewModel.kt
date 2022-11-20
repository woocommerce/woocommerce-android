package com.woocommerce.android.ui.login.jetpack.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackActivationStartViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationStartFragmentArgs by savedStateHandle.navArgs()

    val viewState: LiveData<JetpackActivationState> = MutableLiveData(
        JetpackActivationState(
            url = navArgs.siteUrl,
            isJetpackInstalled = navArgs.isJetpackInstalled
        )
    )

    fun onHelpButtonClick() {
        triggerEvent(NavigateToHelpScreen)
    }

    fun onBackButtonClick() {
        triggerEvent(Exit)
    }

    data class JetpackActivationState(
        val url: String,
        val isJetpackInstalled: Boolean,
    )

    object NavigateToHelpScreen : MultiLiveEvent.Event()
}
