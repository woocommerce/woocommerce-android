package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordTutorialViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onContinueClicked() {
        triggerEvent(OnContinue)
    }

    fun onContactSupportClicked() {
        triggerEvent(OnContactSupport)
    }

    fun onWebPageLoaded(url: String) {
        triggerEvent(ExitWithResult(url))
    }

    object OnContinue : MultiLiveEvent.Event()
    object OnContactSupport : MultiLiveEvent.Event()
}
