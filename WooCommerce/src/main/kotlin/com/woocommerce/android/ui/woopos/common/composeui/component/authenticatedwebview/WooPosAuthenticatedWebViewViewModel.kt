package com.woocommerce.android.ui.woopos.common.composeui.component.authenticatedwebview

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class WooPosAuthenticatedWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val webViewAuthenticator: WebViewAuthenticator,
    val userAgent: UserAgent,
) : ScopedViewModel(savedStateHandle)
