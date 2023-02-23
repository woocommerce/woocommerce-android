package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPComEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationWPComEmailFragmentArgs by savedStateHandle.navArgs()

    private val email = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = "", key = "email")
    private val errorMessage = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "error-message")
    private val isLoadingDialogShown = MutableStateFlow(false)

    val viewState = combine(email, isLoadingDialogShown, errorMessage) { email, isLoadingDialogShown, errorMessage ->
        ViewState(
            email = email,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            errorMessage = errorMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onEmailChanged(email: String) {
        this.email.value = email
    }

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onContinueClick() {
        TODO()
    }

    data class ViewState(
        val email: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = email.isNotBlank()
    }
}
