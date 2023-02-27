package com.woocommerce.android.ui.login.jetpack.wpcom

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.WPComLoginRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.util.GravatarUtils
import javax.inject.Inject

@HiltViewModel
class JetpackActivationWPComPasswordViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val wpComLoginRepository: WPComLoginRepository,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: JetpackActivationWPComPasswordFragmentArgs by savedStateHandle.navArgs()

    private val password = savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = "", key = "password")
    private val errorMessage =
        savedStateHandle.getStateFlow(scope = viewModelScope, initialValue = 0, key = "error-message")
    private val isLoadingDialogShown = MutableStateFlow(false)

    val viewState = combine(
        password,
        isLoadingDialogShown,
        errorMessage,
        flowOf(Pair(navArgs.emailOrUsername, avatarUrlFromEmail(navArgs.emailOrUsername)))
    ) { password, isLoadingDialogShown, errorMessage, (emailOrUsername, avatarUrl) ->
        ViewState(
            emailOrUsername = emailOrUsername,
            password = password,
            avatarUrl = avatarUrl,
            isJetpackInstalled = navArgs.jetpackStatus.isJetpackInstalled,
            isLoadingDialogShown = isLoadingDialogShown,
            errorMessage = errorMessage.takeIf { it != 0 }
        )
    }.asLiveData()

    fun onPasswordChanged(password: String) {
        errorMessage.value = 0
        this.password.value = password
    }

    fun onCloseClick() {
        triggerEvent(Exit)
    }

    fun onContinueClick() {
        TODO()
    }

    private fun avatarUrlFromEmail(email: String): String {
        val avatarSize = resourceProvider.getDimensionPixelSize(R.dimen.image_minor_100)
        return GravatarUtils.gravatarFromEmail(email, avatarSize, GravatarUtils.DefaultImage.STATUS_404)
    }

    data class ViewState(
        val emailOrUsername: String,
        val password: String,
        val avatarUrl: String,
        val isJetpackInstalled: Boolean,
        val isLoadingDialogShown: Boolean = false,
        val errorMessage: Int? = null
    ) {
        val enableSubmit = password.isNotBlank()
    }
}
