package com.woocommerce.android.ui.login.jetpack.sitecredentials

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class JetpackActivationSiteCredentialsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpApiSiteRepository: WPApiSiteRepository
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationSiteCredentialsFragmentArgs by savedStateHandle.navArgs()

    private val _viewState = savedStateHandle.getStateFlow(
        viewModelScope, JetpackActivationSiteCredentialsViewState(
            isJetpackInstalled = navArgs.isJetpackInstalled,
            siteUrl = navArgs.siteUrl
        )
    )
    val viewState = _viewState.asLiveData()

    fun onUsernameChanged(username: String) {
        _viewState.update { it.copy(username = username) }
    }

    fun onPasswordChanged(password: String) {
        _viewState.update { it.copy(password = password) }
    }

    fun onCloseClick() {
        TODO()
    }

    fun onContinueClick() {
        TODO()
    }

    @Parcelize
    data class JetpackActivationSiteCredentialsViewState(
        val isJetpackInstalled: Boolean,
        val siteUrl: String,
        val username: String = "",
        val password: String = "",
        @StringRes val errorMessage: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid = username.isNotBlank() && password.isNotBlank()
    }
}
