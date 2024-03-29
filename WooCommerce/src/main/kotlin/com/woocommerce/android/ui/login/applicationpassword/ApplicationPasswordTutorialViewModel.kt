package com.woocommerce.android.ui.login.applicationpassword

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class ApplicationPasswordTutorialViewModel(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onContinueClicked() {
        triggerEvent(OnContinue)
    }

    fun onContactSupportClicked() {
        triggerEvent(OnContactSupport)
    }

    object OnContinue : MultiLiveEvent.Event()
    object OnContactSupport : MultiLiveEvent.Event()
}
