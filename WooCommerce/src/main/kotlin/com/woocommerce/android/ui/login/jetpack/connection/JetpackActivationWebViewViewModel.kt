package com.woocommerce.android.ui.login.jetpack.connection

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationWebViewFragmentArgs by savedStateHandle.navArgs()

    val urlToLoad = navArgs.urlToLoad

    fun onUrlLoaded(url: String) {
        TODO()
    }

    fun onDismiss() {
        triggerEvent(Exit)
    }
}
