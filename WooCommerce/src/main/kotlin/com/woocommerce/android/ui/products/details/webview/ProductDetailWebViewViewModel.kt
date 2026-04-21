package com.woocommerce.android.ui.products.details.webview

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.liveData
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.webview.WebViewAuthenticator
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import javax.inject.Inject

@HiltViewModel
class ProductDetailWebViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val productDetailRepository: ProductDetailRepository,
    private val selectedSite: SelectedSite,
    val webViewAuthenticator: WebViewAuthenticator,
    val userAgent: UserAgent
) : ScopedViewModel(savedStateHandle) {
    private val navArgs by savedStateHandle.navArgs<ProductDetailWebViewFragmentArgs>()

    val viewState: LiveData<ViewState> = liveData {
        val product = productDetailRepository.getProductAsync(navArgs.remoteProductId) ?: run {
            navigateBack()
            return@liveData
        }

        val urlToLoad = buildProductUrl(product)
        emit(
            ViewState(
                urlToLoad = urlToLoad,
                title = product.name,
                onBackClick = ::navigateBack
            )
        )
    }

    private fun buildProductUrl(product: Product): String {
        val site = selectedSite.get()
        return site.adminUrlOrDefault.slashJoin(BOOKABLE_SERVICE_PATH).slashJoin("${product.remoteId}")
    }

    private fun navigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    data class ViewState(
        val urlToLoad: String,
        val title: String,
        val onBackClick: () -> Unit
    )

    companion object {
        const val BOOKABLE_SERVICE_PATH = "admin.php?page=next-admin&p=/woocommerce/services/edit"
    }
}
