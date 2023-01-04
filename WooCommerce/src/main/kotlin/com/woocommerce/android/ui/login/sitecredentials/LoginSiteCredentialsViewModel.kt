package com.woocommerce.android.ui.login.sitecredentials

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class LoginSiteCredentialsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
    companion object {
        const val SITE_ADDRESS_KEY = "site-address"
        const val USERNAME_KEY = "username"
        const val PASSWORD_KEY = "password"
    }
}
