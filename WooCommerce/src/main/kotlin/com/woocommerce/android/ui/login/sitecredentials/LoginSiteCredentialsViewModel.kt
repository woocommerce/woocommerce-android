package com.woocommerce.android.ui.login.sitecredentials

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class LoginSiteCredentialsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val SITE_ADDRESS_KEY = "site-address"
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
    }

    private val isLoading = MutableStateFlow(false)

    val state = combine(
        flowOf(savedStateHandle.get<String>(SITE_ADDRESS_KEY)!!.removeSchemeAndSuffix()),
        savedStateHandle.getStateFlow(USERNAME_KEY, ""),
        savedStateHandle.getStateFlow(PASSWORD_KEY, ""),
        isLoading
    ) { siteAddress, username, password, isLoading ->
        LoginSiteCredentialsViewState(
            siteUrl = siteAddress,
            username = username,
            password = password,
            isLoading = isLoading
        )
    }.asLiveData()

    fun onUsernameChanged(username: String) {
        savedState[USERNAME_KEY] = username
    }

    fun onPasswordChanged(password: String) {
        savedState[PASSWORD_KEY] = password
    }

    fun onContinueClick() {
        TODO()
    }

    fun onResetPasswordClick() {
        TODO()
    }

    fun onBackClick() {
        triggerEvent(Exit)
    }

    private fun String.removeSchemeAndSuffix() = UrlUtils.removeScheme(UrlUtils.removeXmlrpcSuffix(this))

    @Parcelize
    data class LoginSiteCredentialsViewState(
        val siteUrl: String,
        val username: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        @StringRes val errorMessage: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isValid = username.isNotBlank() && password.isNotBlank()
    }
}
