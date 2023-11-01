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
    private var isExiting = false

    val urlToLoad = navArgs.urlToLoad

    fun onUrlLoaded(url: String) {
        if (!isExiting && url.startsWith(JETPACK_PLANS_URL) || url.startsWith(MOBILE_REDIRECT)) {
            triggerEvent(ConnectionResult.Success)
            isExiting = true
        }
    }

    fun onUrlFailed(url: String, errorCode: Int?) {
        if (!isExiting && url.contains("wp-admin") && errorCode != null) {
            // This will happen when the site uses a custom admin URL, in addition to other eventual errors
            triggerEvent(ConnectionResult.Failure(errorCode))
            isExiting = true
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
