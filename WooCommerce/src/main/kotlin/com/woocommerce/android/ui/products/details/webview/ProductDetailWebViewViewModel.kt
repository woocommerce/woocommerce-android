package com.woocommerce.android.ui.products.details.webview

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class ProductDetailWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productDetailRepository: ProductDetailRepository,
    val webViewAuthenticator: WebViewAuthenticator,
    val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    val viewState: LiveData<ViewState> = TODO()

    data class ViewState(
        val urlToLoad: String,
        val title: String
    )
}
