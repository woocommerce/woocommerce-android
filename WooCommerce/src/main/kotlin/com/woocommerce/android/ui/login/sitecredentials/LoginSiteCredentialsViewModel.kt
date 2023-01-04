package com.woocommerce.android.ui.login.sitecredentials

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel
import javax.inject.Inject

class LoginSiteCredentialsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ScopedViewModel(savedStateHandle) {
}
