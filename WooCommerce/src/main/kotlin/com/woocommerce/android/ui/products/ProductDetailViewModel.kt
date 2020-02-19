package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.formatDateToYYYYMMDDFormat
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShareProduct
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShowImageChooser
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductDetailEvent.ShowImages
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitInventory
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitPricing
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDetail
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitShipping
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val SEARCH_TYPING_DELAY_MS = 500L
    }

    private var remoteProductId = 0L
    final var parameters: Parameters? = null
        private set

    private var skuVerificationJob: Job? = null
    private var shippingClassLoadJob: Job? = null

    final val productDetailViewStateData = LiveDataDelegate(savedState, ProductDetailViewState())
    private var viewState by productDetailViewStateData

    final val productInventoryViewStateData = LiveDataDelegate(savedState, ProductInventoryViewState())
    private var productInventoryViewState by productInventoryViewStateData

    final val productPricingViewStateData = LiveDataDelegate(savedState, ProductPricingViewState())
    private var productPricingViewState by productPricingViewStateData

    // viewState for the shipping screen
    final val productShippingViewStateData = LiveDataDelegate(savedState, ProductShippingViewState())
    private var productShippingViewState by productShippingViewStateData

    // viewState for the shipping class screen
    final val productShippingClassViewStateData = LiveDataDelegate(savedState, ProductShippingClassViewState())
    private var productShippingClassViewState by productShippingClassViewStateData

    init {
        EventBus.getDefault().register(this)
    }

    fun getProduct() = viewState

    fun getTaxClassBySlug(slug: String): TaxClass? {
        return productPricingViewState.taxClassList?.filter { it.slug == slug }?.getOrNull(0)
    }

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
        checkUploads()
    }

    fun initialisePricing() {
        val decimals = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
                ?: DEFAULT_DECIMAL_PRECISION
        productPricingViewState = productPricingViewState.copy(
                currency = parameters?.currencyCode,
                decimals = decimals,
                taxClassList = productRepository.getTaxClassesForSite()
        )
    }

    fun onShareButtonClicked() {
        viewState.product?.let {
            triggerEvent(ShareProduct(it))
        }
    }

    fun onImageGalleryClicked(image: Product.Image) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.product?.let {
            triggerEvent(ShowImages(it, image))
        }
    }

    fun onAddImageClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        triggerEvent(ShowImageChooser)
    }

    fun onDoneButtonClicked(event: ProductExitEvent) {
        triggerEvent(event)
    }

    fun onUpdateButtonClicked() {
        viewState.product?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch { updateProduct(it) }
        }
    }

    fun onBackButtonClicked(event: ProductExitEvent): Boolean {
        val isProductUpdated = when (event) {
            is ExitProductDetail -> viewState.product?.let { viewState.storedProduct?.isSameProduct(it) == false }
            else -> viewState.product?.let { viewState.cachedProduct?.isSameProduct(it) == false }
        }
        return if (isProductUpdated == true && event.shouldShowDiscardDialog) {
            triggerEvent(ShowDiscardDialog(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        // discard changes made to the current screen
                        discardEditChanges(event)

                        // If user in Product detail screen, exit product detail,
                        // otherwise, redirect to Product Detail screen
                        if (event is ExitProductDetail) {
                            triggerEvent(ExitProductDetail(false))
                        } else {
                            triggerEvent(Exit)
                        }
                    }
            ))
            false
        } else {
            true
        }
    }

    fun onSkuChanged(sku: String) {
        // verify if the sku exists only if the text entered by the user does not match the sku stored locally
        if (sku.length > 2 && sku != viewState.storedProduct?.sku) {
            // reset the error message when the user starts typing again
            productInventoryViewState = productInventoryViewState.copy(skuErrorMessage = 0)

            // cancel any existing verification search, then start a new one after a brief delay
            // so we don't actually perform the fetch until the user stops typing
            skuVerificationJob?.cancel()
            skuVerificationJob = launch {
                delay(SEARCH_TYPING_DELAY_MS)

                // check if sku is available from local cache
                if (productRepository.geProductExistsBySku(sku)) {
                    productInventoryViewState = productInventoryViewState.copy(
                            skuErrorMessage = string.product_inventory_update_sku_error
                    )
                } else {
                    verifyProductExistsBySkuRemotely(sku)
                }
            }
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
        backorderStatus: ProductBackorderStatus? = null,
        regularPrice: BigDecimal? = null,
        salePrice: BigDecimal? = null,
        isSaleScheduled: Boolean? = null,
        dateOnSaleFrom: String? = null,
        dateOnSaleTo: String? = null,
        taxStatus: ProductTaxStatus? = null,
        taxClass: String? = null,
        length: Float? = null,
        width: Float? = null,
        height: Float? = null,
        weight: Float? = null,
        shippingClass: String? = null
    ) {
        viewState.product?.let { product ->
            val currentProduct = product.copy()
            val updatedProduct = product.copy(
                    description = description ?: product.description,
                    name = title ?: product.name,
                    sku = sku ?: product.sku,
                    manageStock = manageStock ?: product.manageStock,
                    stockStatus = stockStatus ?: product.stockStatus,
                    soldIndividually = soldIndividually ?: product.soldIndividually,
                    backorderStatus = backorderStatus ?: product.backorderStatus,
                    stockQuantity = stockQuantity?.toInt() ?: product.stockQuantity,
                    images = viewState.storedProduct?.images ?: product.images,
                    regularPrice = regularPrice ?: product.regularPrice,
                    salePrice = salePrice ?: product.salePrice,
                    taxStatus = taxStatus ?: product.taxStatus,
                    taxClass = taxClass ?: product.taxClass,
                    length = length ?: product.length,
                    width = width ?: product.width,
                    height = height ?: product.height,
                    weight = weight ?: product.weight,
                    shippingClass = shippingClass ?: product.shippingClass,
                    isSaleScheduled = isSaleScheduled ?: product.isSaleScheduled,
                    dateOnSaleToGmt = if (product.isSaleScheduled) {
                        dateOnSaleTo?.formatDateToYYYYMMDDFormat() ?: product.dateOnSaleToGmt
                    } else null,
                    dateOnSaleFromGmt = if (product.isSaleScheduled) {
                        dateOnSaleFrom?.formatDateToYYYYMMDDFormat() ?: product.dateOnSaleFromGmt
                    } else null
            )
            viewState = viewState.copy(cachedProduct = currentProduct, product = updatedProduct)

            updateProductEditAction()
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun discardEditChanges(event: ProductExitEvent) {
        when (event) {
            // discard inventory screen changes
            is ExitInventory -> {
                viewState.cachedProduct?.let {
                    val product = viewState.product?.copy(
                            sku = it.sku,
                            manageStock = it.manageStock,
                            stockStatus = it.stockStatus,
                            backorderStatus = it.backorderStatus,
                            soldIndividually = it.soldIndividually,
                            stockQuantity = it.stockQuantity
                    )
                    viewState = viewState.copy(
                            product = product,
                            cachedProduct = product
                    )
                }
            }
            // discard pricing screen changes
            is ExitPricing -> {
                viewState.cachedProduct?.let {
                    val product = viewState.product?.copy(
                            dateOnSaleFromGmt = it.dateOnSaleFromGmt,
                            dateOnSaleToGmt = it.dateOnSaleToGmt,
                            isSaleScheduled = it.isSaleScheduled,
                            taxClass = it.taxClass,
                            taxStatus = it.taxStatus,
                            regularPrice = it.regularPrice,
                            salePrice = it.salePrice
                    )
                    viewState = viewState.copy(
                            product = product,
                            cachedProduct = product
                    )
                }
            }
            // discard shipping screen changes
            is ExitShipping -> {
                viewState.cachedProduct?.let {
                    val product = viewState.product?.copy(
                            weight = it.weight,
                            height = it.height,
                            width = it.width,
                            length = it.length,
                            shippingClass = it.shippingClass
                    )
                    viewState = viewState.copy(
                            product = product,
                            cachedProduct = product
                    )
                }
            }
        }

        updateProductEditAction()
    }

    private fun loadProduct(remoteProductId: Long) {
        loadParameters()

        // Pre-load current site's tax class list for use in the product pricing screen
        launch(dispatchers.main) {
            productRepository.loadTaxClassesForSite()
        }

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
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get())?.let { settings ->
            return@let Pair(settings.weightUnit, settings.dimensionUnit)
        } ?: Pair(null, null)

        parameters = Parameters(currencyCode, weightUnit, dimensionUnit, gmtOffset)
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

    private fun updateProductEditAction() {
        viewState.product?.let {
            val isProductUpdated = viewState.storedProduct?.isSameProduct(it) == false
            viewState = viewState.copy(isProductUpdated = isProductUpdated)
        }
    }

    private fun checkUploads() {
        val uris = ProductImagesService.getUploadingImageUrisForProduct(remoteProductId)
        viewState = viewState.copy(uploadingImageUris = uris)
    }

    private suspend fun updateProduct(product: Product) {
        if (networkStatus.isConnected()) {
            if (productRepository.updateProduct(product)) {
                triggerEvent(ShowSnackbar(string.product_detail_update_product_success))
                viewState = viewState.copy(product = null, isProductUpdated = false, isProgressDialogShown = false)
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

    private suspend fun verifyProductExistsBySkuRemotely(sku: String) {
        // if the sku is not available display error
        val isSkuAvailable = productRepository.verifySkuAvailability(sku)
        val skuErrorMessage = if (isSkuAvailable == false) {
            string.product_inventory_update_sku_error
        } else 0
        productInventoryViewState = productInventoryViewState.copy(skuErrorMessage = skuErrorMessage)
    }

    /**
     * Load & fetch the shipping classed for the current site, optionally performing a "load more" to
     * load the next page of shipping classes
     */
    fun loadShippingClasses(loadMore: Boolean = false) {
        if (loadMore && !productRepository.canLoadMoreShippingClasses) {
            WooLog.d(T.PRODUCTS, "Can't load more product shipping classes")
            return
        }

        waitForExistingShippingClassFetch()

        shippingClassLoadJob = launch {
            if (loadMore) {
                productShippingClassViewState = productShippingClassViewState.copy(isLoadingMoreProgressShown = true)
            } else {
                // get cached shipping classes and only show loading progress the list is empty, otherwise show
                // them right away
                val cachedShippingClasses = productRepository.getProductShippingClassesForSite()
                if (cachedShippingClasses.isEmpty()) {
                    productShippingClassViewState =
                            productShippingClassViewState.copy(isLoadingProgressShown = true)
                } else {
                    productShippingClassViewState =
                            productShippingClassViewState.copy(shippingClassList = cachedShippingClasses)
                }
            }

            // fetch shipping classes from the backend
            val shippingClasses = productRepository.fetchShippingClassesForSite(loadMore)
            productShippingClassViewState = productShippingClassViewState.copy(
                    isLoadingProgressShown = false,
                    isLoadingMoreProgressShown = false,
                    shippingClassList = shippingClasses
            )
        }
    }

    /**
     * If shipping classes are already being fetch, wait for the current fetch to complete - this is
     * used above to avoid fetching multiple pages of shipping classes in unison
     */
    private fun waitForExistingShippingClassFetch() {
        if (shippingClassLoadJob?.isActive == true) {
            launch {
                try {
                    shippingClassLoadJob?.join()
                } catch (e: CancellationException) {
                    WooLog.d(
                            T.PRODUCTS,
                            "CancellationException while waiting for existing shipping class list fetch"
                    )
                }
            }
        }
    }

    @Parcelize
    data class ViewState(
        val showLoadingProgress: Boolean = false,
        val showLoadingMoreProgress: Boolean = false
    ) : Parcelable

    private fun updateProductState(storedProduct: Product) {
        val updatedProduct = viewState.product?.let {
            if (storedProduct.isSameProduct(it)) storedProduct else storedProduct.mergeProduct(viewState.product)
        } ?: storedProduct

        val weightWithUnits = if (updatedProduct.weight > 0) {
            "${format(updatedProduct.weight)}${parameters?.weightUnit ?: ""}"
        } else ""

        val hasLength = updatedProduct.length > 0
        val hasWidth = updatedProduct.width > 0
        val hasHeight = updatedProduct.height > 0
        val unit = parameters?.dimensionUnit ?: ""
        val sizeWithUnits = if (hasLength && hasWidth && hasHeight) {
            "${format(updatedProduct.length)} x ${format(updatedProduct.width)} x ${format(updatedProduct.height)} $unit"
        } else if (hasWidth && hasHeight) {
            "${format(updatedProduct.width)} x ${format(updatedProduct.height)} $unit"
        } else {
            ""
        }.trim()

        viewState = viewState.copy(
                product = updatedProduct,
                cachedProduct = viewState.cachedProduct ?: updatedProduct,
                storedProduct = storedProduct,
                weightWithUnits = weightWithUnits,
                sizeWithUnits = sizeWithUnits,
                priceWithCurrency = formatCurrency(updatedProduct.price, parameters?.currencyCode),
                salePriceWithCurrency = formatCurrency(updatedProduct.salePrice, parameters?.currencyCode),
                regularPriceWithCurrency = formatCurrency(updatedProduct.regularPrice, parameters?.currencyCode),
                gmtOffset = parameters?.gmtOffset ?: 0f,
                length = updatedProduct.length,
                width = updatedProduct.width,
                height = updatedProduct.height,
                weight = updatedProduct.weight,
                shippingClass = updatedProduct.shippingClass
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

    sealed class ProductExitEvent(val shouldShowDiscardDialog: Boolean = true) : Event() {
        class ExitProductDetail(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitInventory(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitPricing(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitShipping(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
    }

    @Parcelize
    data class Parameters(
        val currencyCode: String?,
        val weightUnit: String?,
        val dimensionUnit: String?,
        val gmtOffset: Float
    ) : Parcelable

    @Parcelize
    data class ProductDetailViewState(
        val product: Product? = null,
        var cachedProduct: Product? = null,
        var storedProduct: Product? = null,
        val isSkeletonShown: Boolean? = null,
        val uploadingImageUris: List<Uri>? = null,
        val isProgressDialogShown: Boolean? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val isProductUpdated: Boolean? = null,
        val gmtOffset: Float = 0f,
        val length: Float? = null,
        val width: Float? = null,
        val height: Float? = null,
        val weight: Float? = null,
        val shippingClass: String? = null
    ) : Parcelable

    @Parcelize
    data class ProductInventoryViewState(
        val skuErrorMessage: Int? = null
    ) : Parcelable

    @Parcelize
    data class ProductPricingViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION,
        val taxClassList: List<TaxClass>? = null
    ) : Parcelable

    @Parcelize
    data class ProductShippingViewState(
        val isProductUpdated: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class ProductShippingClassViewState(
        val isLoadingProgressShown: Boolean = false,
        val isLoadingMoreProgressShown: Boolean = false,
        val shippingClassList: List<ShippingClass>? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDetailViewModel>
}
