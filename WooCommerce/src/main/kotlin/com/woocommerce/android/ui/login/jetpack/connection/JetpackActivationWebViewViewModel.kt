package com.woocommerce.android.ui.login.jetpack.connection

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        @VisibleForTesting
        const val JETPACK_PLANS_URL = "https://wordpress.com/jetpack/connect/plans"

        @VisibleForTesting
        const val MOBILE_REDIRECT = "woocommerce://jetpack-connected"
    }

    private val navArgs: JetpackActivationWebViewFragmentArgs by savedStateHandle.navArgs()

    val urlToLoad = navArgs.urlToLoad

    fun onUrlLoaded(url: String) {
        if (url.startsWith(JETPACK_PLANS_URL) || url.startsWith(MOBILE_REDIRECT)) {
            triggerEvent(ConnectionResult.Success)
        }
    }

    fun onDismiss() {
        triggerEvent(ConnectionResult.Cancel)
    }

    sealed class ConnectionResult : Event(), Parcelable {
        @Parcelize
        object Success : ConnectionResult()

        @Parcelize
        object Cancel : ConnectionResult()

        @Parcelize
        data class Failure(val errorCode: Int) : ConnectionResult()
    }
}
