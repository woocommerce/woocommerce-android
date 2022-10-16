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

    fun onGetStartedCLicked(email: String, password: String) {

    }

    object OnTermsOfServiceClicked : MultiLiveEvent.Event()
}
