package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPComPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpComLoginRepository: WPComLoginRepository
) : ScopedViewModel(savedStateHandle) {

    val viewState: LiveData<ViewState>
        get() = TODO()

    fun onPasswordChanged(password: String) {
        TODO()
    }

    fun onCloseClick() {
        TODO()
    }

    fun onContinueClick() {
        TODO()
    }

    data class ViewState(
        val email: String,
        val password: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = password.isNotBlank()
    }
}
