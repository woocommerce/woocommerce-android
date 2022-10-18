package com.woocommerce.android.ui.login.signup

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class SignUpViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ScopedViewModel(savedStateHandle) {
    fun onBackPressed() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onTermsOfServiceClicked() {
        triggerEvent(OnTermsOfServiceClicked)
    }

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
}
