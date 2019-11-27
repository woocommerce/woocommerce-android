package com.woocommerce.android.ui.products

import android.os.Parcelable
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShareProduct
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.math.roundToInt

@OpenClassOnDebug
class ProductDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore
) : ScopedViewModel(savedState, dispatchers) {
    private var remoteProductId = 0L
    private var parameters: Parameters? = null

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
    }

    fun onShareButtonClicked() {
        viewState.product?.let {
            triggerEvent(ShareProduct(it))
        }
    }

    fun onUpdateButtonClicked() {
        viewState.product?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch { updateProduct(it) }
        }
    }

    fun updateProductDraft(description: String?) {
        description?.let {
            viewState.product?.description = it
            viewState = viewState.copy(isProductUpdated = true)
        }
    }

    override fun onCleared() {
        super.onCleared()

        productRepository.onCleanup()
    }

    private fun loadProduct(remoteProductId: Long) {
        loadParameters()

        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId

        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                updateProductState(productInDb)
                if (shouldFetch) {
                    fetchProduct(remoteProductId)
                }
            } else {
                viewState = viewState.copy(isSkeletonShown = true)
                fetchProduct(remoteProductId)
            }
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    private fun loadParameters() {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get())?.let { settings ->
            return@let Pair(settings.weightUnit, settings.dimensionUnit)
        } ?: Pair(null, null)

        parameters = Parameters(currencyCode, weightUnit, dimensionUnit)
    }

    private suspend fun fetchProduct(remoteProductId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedProduct = productRepository.fetchProduct(remoteProductId)
            if (fetchedProduct != null) {
                updateProductState(fetchedProduct)
            } else {
                triggerEvent(ShowSnackbar(R.string.product_detail_fetch_product_error))
                triggerEvent(Exit)
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    private suspend fun updateProduct(product: Product) {
        if (networkStatus.isConnected()) {
            if (productRepository.updateProduct(product)) {
                triggerEvent(ShowSnackbar(R.string.product_detail_update_product_success))
                viewState = viewState.copy(isProgressDialogShown = false, isProductUpdated = false, product = null)
                loadProduct(remoteProductId)
            } else {
                triggerEvent(ShowSnackbar(R.string.product_detail_update_product_error))
                viewState = viewState.copy(isProgressDialogShown = false)
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            viewState = viewState.copy(isProgressDialogShown = false)
        }
    }

    private fun updateProductState(product: Product) {
        val weight = if (product.weight > 0) "${format(product.weight)}${parameters?.weightUnit ?: ""}" else ""

        val hasLength = product.length > 0
        val hasWidth = product.width > 0
        val hasHeight = product.height > 0
        val unit = parameters?.dimensionUnit ?: ""
        val size = if (hasLength && hasWidth && hasHeight) {
            "${format(product.length)} x ${format(product.width)} x ${format(product.height)} $unit"
        } else if (hasWidth && hasHeight) {
            "${format(product.width)} x ${format(product.height)} $unit"
        } else {
            ""
        }.trim()

        viewState = viewState.copy(
                product = combineData(storedProduct = product),
                weightWithUnits = weight,
                sizeWithUnits = size,
                priceWithCurrency = formatCurrency(product.price, parameters?.currencyCode),
                salePriceWithCurrency = formatCurrency(product.salePrice, parameters?.currencyCode),
                regularPriceWithCurrency = formatCurrency(product.regularPrice, parameters?.currencyCode),
                storedProduct = product
        )
    }

    private fun combineData(storedProduct: Product): Product {
        return viewState.product?.let {
            storedProduct.copy(description = it.description)
        } ?: storedProduct
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    private fun format(number: Float): String {
        val int = number.roundToInt()
        return if (number != int.toFloat()) {
            number.toString()
        } else {
            int.toString()
        }
    }

    sealed class ProductDetailEvent : Event() {
        data class ShareProduct(val product: Product) : ProductDetailEvent()
    }

    @Parcelize
    data class Parameters(
        val currencyCode: String?,
        val weightUnit: String?,
        val dimensionUnit: String?
    ) : Parcelable

    @Parcelize
    data class ViewState(
        val product: Product? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val isSkeletonShown: Boolean? = null,
        val isProgressDialogShown: Boolean? = null,
        val isProductUpdated: Boolean? = null,
        var storedProduct: Product? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDetailViewModel>
}
