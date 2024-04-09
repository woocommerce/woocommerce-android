package com.woocommerce.android.ui.login.sitecredentials.applicationpassword

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordTutorialViewModel @Inject constructor(
    val userAgent: UserAgent,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = ViewState()
    )
    val viewState = _viewState.asLiveData()

    fun onContinueClicked() {
        _viewState.update { it.copy(authorizationStarted = true) }
    }

    fun onContactSupportClicked() {
        triggerEvent(OnContactSupport)
    }

    fun onWebPageLoaded(url: String) {
        if (url.startsWith(REDIRECTION_URL)) {
            triggerEvent(ExitWithResult(url))
        }
    }

    fun onNavigationButtonClicked() {
        triggerEvent(ShowExitConfirmationDialog)
    }

    fun onExitConfirmed() {
        triggerEvent(ExitWithResult(null))
    }

    fun onWebViewDataAvailable(
        authorizationUrl: String?,
        errorMessage: Int?
    ) {
        _viewState.update {
            it.copy(
                authorizationUrl = authorizationUrl,
                errorMessage = errorMessage
            )
        }
    }

    object OnContactSupport : Event()
    object ShowExitConfirmationDialog : Event()

    @Parcelize
    data class ViewState(
        val authorizationStarted: Boolean = false,
        val authorizationUrl: String? = null,
        @StringRes val errorMessage: Int? = null,
    ) : Parcelable

    companion object {
        private const val REDIRECTION_URL = "woocommerce://login"
    }
}
