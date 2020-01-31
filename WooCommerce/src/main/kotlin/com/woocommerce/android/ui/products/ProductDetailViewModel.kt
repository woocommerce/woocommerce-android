package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShareProduct
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShowImageChooser
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShowImages
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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

    final val commonStateLiveData = LiveDataDelegate(savedState, CommonViewState())
    private var commonState by commonStateLiveData

    final val productDetailViewStateData = LiveDataDelegate(savedState, ProductDetailViewState())
    private var viewState by productDetailViewStateData

    final val productInventoryViewStateData = LiveDataDelegate(savedState, ProductInventoryViewState())
    private var productInventoryViewState by productInventoryViewStateData

    init {
        EventBus.getDefault().register(this)
    }

    fun getProduct() = commonState

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
        checkUploads()
    }

    fun onShareButtonClicked() {
        commonState.product?.let {
            triggerEvent(ShareProduct(it))
        }
    }

    fun onImageGalleryClicked(image: Product.Image) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        commonState.product?.let {
            triggerEvent(ShowImages(it, image))
        }
    }

    fun onAddImageClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        triggerEvent(ShowImageChooser)
    }

    fun onUpdateButtonClicked() {
        commonState.product?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch { updateProduct(it) }
        }
    }

    fun onBackButtonClicked(): Boolean {
        return if (commonState.isProductUpdated == true && commonState.shouldShowDiscardDialog) {
            triggerEvent(ShowDiscardDialog(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        commonState = commonState.copy(shouldShowDiscardDialog = false)
                        triggerEvent(Exit)
                    },
                    negativeBtnAction = DialogInterface.OnClickListener { _, _ ->
                        commonState = commonState.copy(shouldShowDiscardDialog = true)
                    }
            ))
            false
        } else {
            true
        }
    }

    /**
     * Update all product fields that are edited by the user
     */
    fun updateProductDraft(
        description: String? = null,
        title: String? = null,
        sku: String? = null,
        manageStock: Boolean? = null,
        stockStatus: ProductStockStatus? = null,
        soldIndividually: Boolean? = null,
        stockQuantity: String? = null,
        backorderStatus: ProductBackorderStatus? = null
    ) {
        description?.let {
            if (it != commonState.product?.description) {
                commonState.product?.description = it
            }
        }
        title?.let {
            if (it != commonState.product?.name) {
                commonState.product?.name = it
            }
        }
        sku?.let {
            if (it != commonState.product?.sku) {
                commonState.product?.sku = it
            }
        }
        manageStock?.let {
            if (it != commonState.product?.manageStock) {
                commonState.product?.manageStock = it
            }
        }
        stockStatus?.let {
            if (it != commonState.product?.stockStatus) {
                commonState.product?.stockStatus = it
            }
        }
        soldIndividually?.let {
            if (it != commonState.product?.soldIndividually) {
                commonState.product?.soldIndividually = it
            }
        }
        stockQuantity?.let {
            val quantity = it.toInt()
            if (quantity != commonState.product?.stockQuantity) {
                commonState.product?.stockQuantity = quantity
            }
        }
        backorderStatus?.let {
            if (it != commonState.product?.backorderStatus) {
                commonState.product?.backorderStatus = it
            }
        }

        commonState.product?.let {
            val isProductUpdated = commonState.storedProduct?.isSameProduct(it) == false
            commonState = commonState.copy(isProductUpdated = isProductUpdated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun loadProduct(remoteProductId: Long) {
        loadParameters()

        val shouldFetch = remoteProductId != this.remoteProductId
        this.remoteProductId = remoteProductId

        launch {
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                updateProductState(productInDb)

                val cachedVariantCount = productRepository.getCachedVariantCount(remoteProductId)
                if (shouldFetch || cachedVariantCount != productInDb.numVariations) {
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
                triggerEvent(ShowSnackbar(string.product_detail_fetch_product_error))
                triggerEvent(Exit)
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    fun isUploading() = ProductImagesService.isUploadingForProduct(remoteProductId)

    private fun checkUploads() {
        val uris = ProductImagesService.getUploadingImageUrisForProduct(remoteProductId)
        viewState = viewState.copy(uploadingImageUris = uris)
    }

    private suspend fun updateProduct(product: Product) {
        if (networkStatus.isConnected()) {
            if (productRepository.updateProduct(product)) {
                triggerEvent(ShowSnackbar(string.product_detail_update_product_success))
                viewState = viewState.copy(isProgressDialogShown = false)
                commonState = commonState.copy(product = null, isProductUpdated = false)
                loadProduct(remoteProductId)
            } else {
                triggerEvent(ShowSnackbar(string.product_detail_update_product_error))
                viewState = viewState.copy(isProgressDialogShown = false)
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            viewState = viewState.copy(isProgressDialogShown = false)
        }
    }

    private fun updateProductState(storedProduct: Product) {
        val weight = if (storedProduct.weight > 0) {
            "${format(storedProduct.weight)}${parameters?.weightUnit ?: ""}"
        } else ""

        val hasLength = storedProduct.length > 0
        val hasWidth = storedProduct.width > 0
        val hasHeight = storedProduct.height > 0
        val unit = parameters?.dimensionUnit ?: ""
        val size = if (hasLength && hasWidth && hasHeight) {
            "${format(storedProduct.length)} x ${format(storedProduct.width)} x ${format(storedProduct.height)} $unit"
        } else if (hasWidth && hasHeight) {
            "${format(storedProduct.width)} x ${format(storedProduct.height)} $unit"
        } else {
            ""
        }.trim()

        commonState = commonState.copy(
                product = storedProduct.mergeProduct(commonState.product),
                storedProduct = storedProduct,
                weightWithUnits = weight,
                sizeWithUnits = size,
                priceWithCurrency = formatCurrency(storedProduct.price, parameters?.currencyCode),
                salePriceWithCurrency = formatCurrency(storedProduct.salePrice, parameters?.currencyCode),
                regularPriceWithCurrency = formatCurrency(storedProduct.regularPrice, parameters?.currencyCode)
        )
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

    /**
     * This event may happen if the user uploads or removes an image from the images fragment and returns
     * to the detail fragment before the request completes
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImageUploaded) {
        if (event.isError) {
            triggerEvent(ShowSnackbar(string.product_image_service_error_uploading))
        } else {
            loadProduct(remoteProductId)
        }
        checkUploads()
    }

    sealed class ProductDetailEvent : Event() {
        data class ShareProduct(val product: Product) : ProductDetailEvent()
        data class ShowImages(val product: Product, val image: Product.Image) : ProductDetailEvent()
        object ShowImageChooser : ProductDetailEvent()
    }

    @Parcelize
    data class Parameters(
        val currencyCode: String?,
        val weightUnit: String?,
        val dimensionUnit: String?
    ) : Parcelable

    @Parcelize
    data class ProductDetailViewState(
        val isSkeletonShown: Boolean? = null,
        val uploadingImageUris: List<Uri>? = null,
        val isProgressDialogShown: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class ProductInventoryViewState(
        val skuErrorMessage: Int? = null
    ) : Parcelable

    @Parcelize
    data class CommonViewState(
        val product: Product? = null,
        var storedProduct: Product? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val isProductUpdated: Boolean? = null,
        val shouldShowDiscardDialog: Boolean = true
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDetailViewModel>
}
