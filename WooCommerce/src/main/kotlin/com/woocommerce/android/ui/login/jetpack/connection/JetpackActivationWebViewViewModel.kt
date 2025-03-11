package com.woocommerce.android.ui.login.jetpack.connection

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
        private const val JETPACK_PLANS_URL = "https://wordpress.com/jetpack/connect/plans"
        private const val MOBILE_REDIRECT = "woocommerce://jetpack-connected"
        private const val JETPACK_AUTHORIZE_URL = "jetpack.wordpress.com/jetpack.authorize"
    }

    private val navArgs: JetpackActivationWebViewFragmentArgs by savedStateHandle.navArgs()
    private var isExiting = false

    var urlToLoad by mutableStateOf(navArgs.urlToLoad)
        private set

    fun onUrlLoaded(url: String) {
        if (isExiting) return

        if (url.contains(JETPACK_AUTHORIZE_URL) && !navArgs.urlToLoad.contains(JETPACK_AUTHORIZE_URL)) {
            // The initial URL was not the authorize URL, which happens when we need to handle the site registration
            // and this means the user is not authenticated to WordPress.com yet.
            // By updating the URL to the authorize URL, we will then use the WebViewAuthenticator and will be able to
            // handle the authentication process.
            urlToLoad = url
        } else if (url.startsWith(JETPACK_PLANS_URL) || url.startsWith(MOBILE_REDIRECT)) {
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
