package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import android.view.View
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.isNumeric
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitExternalLink
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitImages
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitInventory
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitPricing
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDetail
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitShipping
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCatalogVisibility
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductImageChooser
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductImages
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductMenuOrder
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSlug
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVisibility
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.products.settings.ProductVisibility
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.Optional
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.util.DateTimeUtils
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.Date

@OpenClassOnDebug
class ProductDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val SEARCH_TYPING_DELAY_MS = 500L
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }

    /**
     * Fetch product related properties (currency, product dimensions) for the site since we use this
     * variable in many different places in the product detail view such as pricing, shipping.
     */
    final val parameters: Parameters by lazy {
        val params = savedState.get<Parameters>(KEY_PRODUCT_PARAMETERS) ?: loadParameters()
        savedState[KEY_PRODUCT_PARAMETERS] = params
        params
    }

    private var skuVerificationJob: Job? = null

    // view state for the product detail screen
    final val productDetailViewStateData = LiveDataDelegate(savedState, ProductDetailViewState())
    private var viewState by productDetailViewStateData

    // view state for the product inventory screen
    final val productInventoryViewStateData = LiveDataDelegate(savedState, ProductInventoryViewState())
    private var productInventoryViewState by productInventoryViewStateData

    // view state for the product pricing screen
    final val productPricingViewStateData = LiveDataDelegate(savedState, ProductPricingViewState())
    private var productPricingViewState by productPricingViewStateData

    // view state for the product images screen
    final val productImagesViewStateData = LiveDataDelegate(savedState, ProductImagesViewState())
    private var productImagesViewState by productImagesViewStateData

    init {
        EventBus.getDefault().register(this)
    }

    fun getProduct() = viewState

    fun getRemoteProductId() = viewState.productDraft?.remoteId ?: 0L

    fun getTaxClassBySlug(slug: String): TaxClass? {
        return productPricingViewState.taxClassList?.filter { it.slug == slug }?.getOrNull(0)
    }

    fun start(remoteProductId: Long) {
        loadProduct(remoteProductId)
    }

    fun initialisePricing() {
        val decimals = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
                ?: DEFAULT_DECIMAL_PRECISION
        productPricingViewState = productPricingViewState.copy(
                currency = parameters.currencyCode,
                decimals = decimals,
                taxClassList = productRepository.getTaxClassesForSite(),
                regularPrice = viewState.storedProduct?.regularPrice,
                salePrice = viewState.storedProduct?.salePrice
        )
    }

    /**
     * Called when the Share menu button is clicked in Product detail screen
     */
    fun onShareButtonClicked() {
        viewState.productDraft?.let {
            triggerEvent(ShareProduct(it.permalink, it.name))
        }
    }

    /**
     * Called when an existing image is selected in Product detail screen
     */
    fun onImageGalleryClicked(image: Product.Image, selectedImage: WeakReference<View>) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.productDraft?.let {
            triggerEvent(ViewProductImages(it, image, selectedImage))
        }
    }

    /**
     * Called when the add image icon is clicked in Product detail screen
     */
    fun onAddImageClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.productDraft?.let {
            triggerEvent(ViewProductImageChooser(it.remoteId))
        }
    }

    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product detail screen
     */
    fun onEditProductCardClicked(target: ProductNavigationTarget) {
        triggerEvent(target)
    }

    /**
     * Called when the the Remove end date link is clicked
     */
    fun onRemoveEndDateClicked() {
        productPricingViewState = productPricingViewState.copy(saleEndDate = null)
        updateProductDraft(saleEndDate = Optional(null))
    }

    /**
     * Called when the DONE menu button is clicked in all of the product sub detail screen
     */
    fun onDoneButtonClicked(event: ProductExitEvent) {
        var eventName: Stat? = null
        var hasChanges = false
        when (event) {
            is ExitInventory -> {
                eventName = Stat.PRODUCT_INVENTORY_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = viewState.storedProduct?.hasInventoryChanges(viewState.productDraft) ?: false
            }
            is ExitPricing -> {
                eventName = Stat.PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = viewState.storedProduct?.hasPricingChanges(viewState.productDraft) ?: false
            }
            is ExitShipping -> {
                eventName = Stat.PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = viewState.storedProduct?.hasShippingChanges(viewState.productDraft) ?: false
            }
            is ExitImages -> {
                eventName = Stat.PRODUCT_IMAGE_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = viewState.storedProduct?.hasImageChanges(viewState.productDraft) ?: false
            }
            is ExitSettings -> {
                // TODO: eventName = ??
                hasChanges = if (viewState.storedProduct?.hasSettingsChanges(viewState.productDraft) == true) {
                    true
                } else {
                    viewState.isPasswordChanged
                }
            }
            is ExitExternalLink -> {
                hasChanges = viewState.storedProduct?.hasExternalLinkChanges(viewState.productDraft) ?: false
            }
        }
        eventName?.let { AnalyticsTracker.track(it, mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges)) }
        triggerEvent(event)
    }

    /**
     * Called when the UPDATE menu button is clicked in the product detail screen.
     * Displays a progress dialog and updates the product
     */
    fun onUpdateButtonClicked() {
        viewState.productDraft?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch { updateProduct(it) }
        }
    }

    /**
     * Called when the user taps the Product settings menu item
     */
    fun onSettingsButtonClicked() {
        viewState.productDraft?.let {
            triggerEvent(ViewProductSettings(it.remoteId))
        }
    }

    /**
     * Called when the user taps the status in product settings
     */
    fun onSettingsStatusButtonClicked() {
        viewState.productDraft?.let {
            triggerEvent(ViewProductStatus(it.status))
        }
    }

    /**
     * Called when the user taps the product catalog visibility in product settings
     */
    fun onSettingsCatalogVisibilityButtonClicked() {
        viewState.productDraft?.let {
            triggerEvent(ViewProductCatalogVisibility(it.catalogVisibility, it.isFeatured))
        }
    }

    /**
     * Called when the user taps the product visibility in product settings
     */
    fun onSettingsVisibilityButtonClicked() {
        val visibility = getProductVisibility()
        val password = viewState.draftPassword ?: viewState.storedPassword
        triggerEvent(ViewProductVisibility(visibility, password))
    }

    /**
     * Called when the user taps the product slug in product settings
     */
    fun onSettingsSlugButtonClicked() {
        viewState.productDraft?.let {
            triggerEvent(ViewProductSlug(it.slug))
        }
    }

    /**
     * Called when the user taps the product menu order in product settings
     */
    fun onSettingsMenuOrderButtonClicked() {
        viewState.productDraft?.let {
            triggerEvent(ViewProductMenuOrder(it.menuOrder))
        }
    }

    /**
     * Called when the sale start date is selected from the date picker in the pricing screen.
     * Keeps track of the start and end date value when scheduling a sale.
     */
    fun onStartDateChanged(newDate: Date) {
        // update end date to start date only if current end date < start date
        val saleEndDate = viewState.productDraft?.saleEndDateGmt
        if (saleEndDate?.before(newDate) == true) {
            updateProductDraft(saleEndDate = Optional(newDate))
            productPricingViewState = productPricingViewState.copy(saleEndDate = newDate)
        }
        updateProductDraft(saleStartDate = newDate)
        productPricingViewState = productPricingViewState.copy(saleStartDate = newDate)
    }

    /**
     * Called when the sale end date is selected from the date picker in the pricing screen.
     * Keeps track of the start and end date value when scheduling a sale.
     */
    fun onEndDateChanged(newDate: Date?) {
        // update start date to end date only if current start date > end date
        val saleStartDate = viewState.productDraft?.saleStartDateGmt
        if (newDate != null && saleStartDate?.after(newDate) == true) {
            updateProductDraft(saleStartDate = newDate)
            productPricingViewState = productPricingViewState.copy(saleStartDate = newDate)
        }
        updateProductDraft(saleEndDate = Optional(newDate))
        productPricingViewState = productPricingViewState.copy(saleEndDate = newDate)
    }

    /**
     * Method called when back button is clicked.
     *
     * Each product screen has it's own [ProductExitEvent]
     * Based on the exit event, the logic is to check if the discard dialog should be displayed.
     *
     * For all product sub-detail screens such as [ProductInventoryFragment] and [ProductPricingFragment],
     * the discard dialog should only be displayed if there are currently any changes made to the fields in the screen.
     *
     * For the product detail screen, the discard dialog should only be displayed if there are changes to the
     * [Product] model locally, that still need to be saved to the backend.
     */
    fun onBackButtonClicked(event: ProductExitEvent): Boolean {
        val isProductDetailUpdated = viewState.isProductUpdated

        val isProductSubDetailUpdated = viewState.productDraft?.let { draft ->
            viewState.productBeforeEnteringFragment?.isSameProduct(draft) == false ||
                    viewState.isPasswordChanged
        }

        val isProductUpdated = when (event) {
            is ExitProductDetail -> isProductDetailUpdated
            else -> isProductDetailUpdated == true && isProductSubDetailUpdated == true
        }
        if (isProductUpdated == true && event.shouldShowDiscardDialog) {
            triggerEvent(ShowDiscardDialog(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        // discard changes made to the current screen
                        discardEditChanges()

                        // If user in Product detail screen, exit product detail,
                        // otherwise, redirect to Product Detail screen
                        if (event is ExitProductDetail) {
                            triggerEvent(ExitProduct)
                        } else {
                            triggerEvent(Exit)
                        }
                    }
            ))
            return false
        } else if (event is ExitProductDetail && ProductImagesService.isUploadingForProduct(getRemoteProductId())) {
            // images can't be assigned to the product until they finish uploading so ask whether to discard images.
            triggerEvent(ShowDiscardDialog(
                    messageId = string.discard_images_message,
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(ExitProduct)
                    }
            ))
            return false
        } else {
            return true
        }
    }

    /**
     * Called when user modifies the SKU field. Currently checks if the entered sku is available
     * in the local db. Only if it is not available, the API verification call is initiated.
     */
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
     * Called when user modifies the Stock quantity field in the inventory screen.
     *
     * Currently checks if the entered stock quantity [text] is empty or contains '-' symbol.
     * Symbols are not supported when updating the stock quantity and displays an error
     * message to the UI if there are any unsupported symbols found in the [text]
     */
    fun onStockQuantityChanged(text: String) {
        val inputText = if (text.isEmpty()) "0" else text
        productInventoryViewState = if (inputText.isNumeric()) {
            updateProductDraft(stockQuantity = inputText)
            productInventoryViewState.copy(stockQuantityErrorMessage = 0)
        } else {
            productInventoryViewState.copy(
                    stockQuantityErrorMessage = string.product_inventory_update_stock_quantity_error
            )
        }
    }

    fun onRegularPriceEntered(inputValue: BigDecimal) {
        productPricingViewState = productPricingViewState.copy(regularPrice = inputValue)
        val salePrice = productPricingViewState.salePrice ?: BigDecimal.ZERO

        productPricingViewState = if (salePrice > inputValue) {
            productPricingViewState.copy(salePriceErrorMessage = string.product_pricing_update_sale_price_error)
        } else {
            updateProductDraft(regularPrice = inputValue)
            productPricingViewState.copy(salePriceErrorMessage = 0)
        }
    }

    fun onSalePriceEntered(inputValue: BigDecimal) {
        productPricingViewState = productPricingViewState.copy(salePrice = inputValue)
        val regularPrice = productPricingViewState.regularPrice ?: BigDecimal.ZERO

        productPricingViewState = if (inputValue > regularPrice) {
            productPricingViewState.copy(salePriceErrorMessage = string.product_pricing_update_sale_price_error)
        } else {
            updateProductDraft(salePrice = inputValue, isOnSale = true)
            productPricingViewState.copy(salePriceErrorMessage = 0)
        }
    }

    /**
     * Called before entering any product screen to save of copy of the product prior to the user making any
     * changes in that specific screen
     */
    fun updateProductBeforeEnteringFragment() {
        viewState.productBeforeEnteringFragment = viewState.productDraft
    }

    /**
     * Update all product fields that are edited by the user
     */
    fun updateProductDraft(
        description: String? = null,
        shortDescription: String? = null,
        title: String? = null,
        sku: String? = null,
        slug: String? = null,
        manageStock: Boolean? = null,
        stockStatus: ProductStockStatus? = null,
        soldIndividually: Boolean? = null,
        stockQuantity: String? = null,
        backorderStatus: ProductBackorderStatus? = null,
        regularPrice: BigDecimal? = null,
        salePrice: BigDecimal? = null,
        isOnSale: Boolean? = null,
        isSaleScheduled: Boolean? = null,
        saleStartDate: Date? = null,
        saleEndDate: Optional<Date>? = null,
        taxStatus: ProductTaxStatus? = null,
        taxClass: String? = null,
        length: Float? = null,
        width: Float? = null,
        height: Float? = null,
        weight: Float? = null,
        shippingClass: String? = null,
        images: List<Product.Image>? = null,
        shippingClassId: Long? = null,
        productStatus: ProductStatus? = null,
        catalogVisibility: ProductCatalogVisibility? = null,
        isFeatured: Boolean? = null,
        reviewsAllowed: Boolean? = null,
        purchaseNote: String? = null,
        externalUrl: String? = null,
        buttonText: String? = null,
        menuOrder: Int? = null
    ) {
        viewState.productDraft?.let { product ->
            val currentProduct = product.copy()
            val updatedProduct = product.copy(
                    description = description ?: product.description,
                    shortDescription = shortDescription ?: product.shortDescription,
                    name = title ?: product.name,
                    sku = sku ?: product.sku,
                    slug = slug ?: product.slug,
                    manageStock = manageStock ?: product.manageStock,
                    stockStatus = stockStatus ?: product.stockStatus,
                    soldIndividually = soldIndividually ?: product.soldIndividually,
                    backorderStatus = backorderStatus ?: product.backorderStatus,
                    stockQuantity = stockQuantity?.toInt() ?: product.stockQuantity,
                    images = images ?: product.images,
                    regularPrice = regularPrice ?: product.regularPrice,
                    salePrice = salePrice ?: product.salePrice,
                    isOnSale = isOnSale ?: product.isOnSale,
                    taxStatus = taxStatus ?: product.taxStatus,
                    taxClass = taxClass ?: product.taxClass,
                    length = length ?: product.length,
                    width = width ?: product.width,
                    height = height ?: product.height,
                    weight = weight ?: product.weight,
                    shippingClass = shippingClass ?: product.shippingClass,
                    shippingClassId = shippingClassId ?: product.shippingClassId,
                    isSaleScheduled = isSaleScheduled ?: product.isSaleScheduled,
                    status = productStatus ?: product.status,
                    catalogVisibility = catalogVisibility ?: product.catalogVisibility,
                    isFeatured = isFeatured ?: product.isFeatured,
                    reviewsAllowed = reviewsAllowed ?: product.reviewsAllowed,
                    purchaseNote = purchaseNote ?: product.purchaseNote,
                    externalUrl = externalUrl ?: product.externalUrl,
                    buttonText = buttonText ?: product.buttonText,
                    menuOrder = menuOrder ?: product.menuOrder,
                    saleEndDateGmt = if (isSaleScheduled == true ||
                            (isSaleScheduled == null && currentProduct.isSaleScheduled)) {
                        if (saleEndDate != null) saleEndDate.value else product.saleEndDateGmt
                    } else viewState.storedProduct?.saleEndDateGmt,
                    saleStartDateGmt = if (isSaleScheduled == true ||
                            (isSaleScheduled == null && currentProduct.isSaleScheduled)) {
                        saleStartDate ?: product.saleStartDateGmt
                    } else viewState.storedProduct?.saleStartDateGmt
            )
            viewState = viewState.copy(productDraft = updatedProduct)

            updateProductEditAction()
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    /**
     * Called when discard is clicked on any of the product screens to restore the product to
     * the state it was in when the screen was first entered
     */
    private fun discardEditChanges() {
        viewState = viewState.copy(productDraft = viewState.productBeforeEnteringFragment)

        // updates the UPDATE menu button in the product detail screen i.e. the UPDATE menu button
        // will only be displayed if there are changes made to the Product model.
        updateProductEditAction()
    }

    private fun loadProduct(remoteProductId: Long) {
        // Pre-load current site's tax class list for use in the product pricing screen
        launch(dispatchers.main) {
            productRepository.loadTaxClassesForSite()
        }

        launch {
            // fetch product
            val productInDb = productRepository.getProduct(remoteProductId)
            if (productInDb != null) {
                val shouldFetch = remoteProductId != getRemoteProductId()
                updateProductState(productInDb)

                val cachedVariantCount = productRepository.getCachedVariantCount(remoteProductId)
                if (shouldFetch || cachedVariantCount != productInDb.numVariations) {
                    fetchProduct(remoteProductId)
                    fetchProductPassword(remoteProductId)
                }
            } else {
                viewState = viewState.copy(isSkeletonShown = true)
                fetchProduct(remoteProductId)
            }
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    /**
     * Called from the product visibility settings fragment when the user updates
     * the product's visibility and/or password
     */
    fun updateProductVisibility(visibility: ProductVisibility, password: String?) {
        viewState = viewState.copy(draftPassword = password)

        when (visibility) {
            ProductVisibility.PUBLIC -> {
                updateProductDraft(productStatus = ProductStatus.PUBLISH)
            }
            ProductVisibility.PRIVATE -> {
                updateProductDraft(productStatus = ProductStatus.PRIVATE)
            }
            ProductVisibility.PASSWORD_PROTECTED -> {
                updateProductDraft(productStatus = ProductStatus.PUBLISH)
            }
        }
    }

    /**
     * Returns the draft visibility if a draft exists otherwise it returns the stored visibility.
     * The visibility is determined by the status and the password. If the password isn't empty, then
     * visibility is `PASSWORD_PROTECTED`. If there's no password and the product status is `PRIVATE`
     * then the visibility is `PRIVATE`, otherwise it's `PUBLIC`.
     */
    fun getProductVisibility(): ProductVisibility {
        val status = viewState.productDraft?.status ?: viewState.storedProduct?.status
        val password = viewState.draftPassword ?: viewState.storedPassword
        return when {
            password?.isNotEmpty() == true -> {
                ProductVisibility.PASSWORD_PROTECTED
            }
            status == ProductStatus.PRIVATE -> {
                ProductVisibility.PRIVATE
            }
            else -> {
                ProductVisibility.PUBLIC
            }
        }
    }

    /**
     * Sends a request to fetch the product's password
     */
    private suspend fun fetchProductPassword(remoteProductId: Long) {
        val password = productRepository.fetchProductPassword(remoteProductId)

        viewState = if (viewState.draftPassword == null) {
            viewState.copy(
                    storedPassword = password,
                    draftPassword = password
            )
        } else {
            viewState.copy(
                    storedPassword = password
            )
        }
    }

    /**
     * Loads the product dependencies for a site such as dimensions, currency or timezone
     */
    private fun loadParameters(): Parameters {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get())?.let { settings ->
            return@let Pair(settings.weightUnit, settings.dimensionUnit)
        } ?: Pair(null, null)

        return Parameters(currencyCode, weightUnit, dimensionUnit, gmtOffset)
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

    fun isUploadingImages(remoteProductId: Long) = ProductImagesService.isUploadingForProduct(remoteProductId)

    /**
     * Updates the UPDATE menu button in the product detail screen. UPDATE is only displayed
     * when there are changes made to the [Product] model and this can be verified by comparing
     * the viewState.product with viewState.storedProduct model.
     */
    private fun updateProductEditAction() {
        viewState.productDraft?.let { draft ->
            val isProductUpdated = viewState.storedProduct?.isSameProduct(draft) == false ||
                    viewState.isPasswordChanged
            viewState = viewState.copy(isProductUpdated = isProductUpdated)
        }
    }

    fun uploadProductImages(remoteProductId: Long, localUriList: ArrayList<Uri>) {
        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(string.network_activity_no_connectivity))
            return
        }
        if (ProductImagesService.isBusy()) {
            triggerEvent(ShowSnackbar(string.product_image_service_busy))
            return
        }
        productImagesServiceWrapper.uploadProductMedia(remoteProductId, localUriList)
    }

    /**
     * Checks whether product images are uploading and ensures the view state reflects any currently
     * uploading images
     */
    private fun checkImageUploads(remoteProductId: Long) {
        val isUploadingImages = ProductImagesService.isUploadingForProduct(remoteProductId)
        if (isUploadingImages != productImagesViewState.isUploadingImages) {
            val uris = ProductImagesService.getUploadingImageUrisForProduct(remoteProductId)
            viewState = viewState.copy(uploadingImageUris = uris)
            productImagesViewState = productImagesViewState.copy(isUploadingImages = isUploadingImages)
        }
    }

    /**
     * Updates the product to the backend only if network is connected.
     * Otherwise, an offline snackbar is displayed.
     */
    private suspend fun updateProduct(product: Product) {
        if (networkStatus.isConnected()) {
            if (productRepository.updateProduct(product)) {
                if (viewState.isPasswordChanged) {
                    val password = viewState.draftPassword
                    if (productRepository.updateProductPassword(product.remoteId, password)) {
                        viewState = viewState.copy(storedPassword = password)
                        triggerEvent(ShowSnackbar(string.product_detail_update_product_success))
                    } else {
                        triggerEvent(ShowSnackbar(string.product_detail_update_product_password_error))
                    }
                } else {
                    triggerEvent(ShowSnackbar(string.product_detail_update_product_success))
                }
                viewState = viewState.copy(productDraft = null, isProductUpdated = false)
                loadProduct(product.remoteId)
            } else {
                triggerEvent(ShowSnackbar(string.product_detail_update_product_error))
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        viewState = viewState.copy(isProgressDialogShown = false)
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
     * Fetch the shipping class name of a product based on the remote shipping class id
     */
    fun getShippingClassByRemoteShippingClassId(remoteShippingClassId: Long) =
            productRepository.getProductShippingClassByRemoteId(remoteShippingClassId)?.name
                    ?: viewState.productDraft?.shippingClass ?: ""

    private fun updateProductState(productToUpdateFrom: Product) {
        val updatedDraft = viewState.productDraft?.let { currentDraft ->
            if (viewState.storedProduct?.isSameProduct(currentDraft) == true) {
                productToUpdateFrom
            } else {
                productToUpdateFrom.mergeProduct(currentDraft)
            }
        } ?: productToUpdateFrom

        loadProductTaxAndShippingClassDependencies(updatedDraft)

        val weightWithUnits = updatedDraft.getWeightWithUnits(parameters.weightUnit)
        val sizeWithUnits = updatedDraft.getSizeWithUnits(parameters.dimensionUnit)

        viewState = viewState.copy(
                productDraft = updatedDraft,
                storedProduct = productToUpdateFrom,
                weightWithUnits = weightWithUnits,
                sizeWithUnits = sizeWithUnits,
                salePriceWithCurrency = formatCurrency(updatedDraft.salePrice, parameters.currencyCode),
                regularPriceWithCurrency = formatCurrency(updatedDraft.regularPrice, parameters.currencyCode),
                gmtOffset = parameters.gmtOffset
        )

        // make sure to remember uploading images
        checkImageUploads(getRemoteProductId())
    }

    private fun loadProductTaxAndShippingClassDependencies(product: Product) {
        launch {
            // Fetch current site's shipping class only if a shipping class is assigned to the product and if
            // the shipping class is not available in the local db
            val shippingClassId = product.shippingClassId
            if (shippingClassId != 0L && productRepository.getProductShippingClassByRemoteId(shippingClassId) == null) {
                productRepository.fetchProductShippingClassById(shippingClassId)
            }

            // Pre-load current site's tax class list for use in the product pricing screen
            productRepository.loadTaxClassesForSite()
        }
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    /**
     * The list of product images has started uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateStartedEvent) {
        checkImageUploads(event.remoteProductId)
    }

    /**
     * The list of product images has finished uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        loadProduct(event.remoteProductId)
        checkImageUploads(event.remoteProductId)
    }

    /**
     * A single product image has finished uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImageUploaded) {
        if (event.isError) {
            triggerEvent(ShowSnackbar(string.product_image_service_error_uploading))
        } else {
            event.media?.let { media ->
                val image = Product.Image(
                        id = media.mediaId,
                        name = media.fileName,
                        source = media.url,
                        dateCreated = DateTimeUtils.dateFromIso8601(media.uploadDate)
                )
                addProductImageToDraft(image)
            }
        }
        checkImageUploads(getRemoteProductId())
    }

    /**
     * Called after product image has been uploaded to add the uploaded image to the draft product
     */
    fun addProductImageToDraft(image: Product.Image) {
        // create a new image list and add the passed media first...
        val imageList = ArrayList<Product.Image>().also {
            it.add(image)
        }

        // ...then add the existing product images to the new list...
        viewState.productDraft?.let {
            imageList.addAll(it.images)
        }

        // ...and then update the draft with the new list
        updateProductDraft(images = imageList)
    }

    /**
     * Removes a single product image from the product draft
     */
    fun removeProductImageFromDraft(remoteMediaId: Long) {
        viewState.productDraft?.let { product ->
            val imageList = product.images.filter { it.id != remoteMediaId }
            updateProductDraft(images = imageList)
        }
    }

    /**
     * Sealed class that handles the back navigation for the product detail screens while providing a common
     * interface for managing them as a single type. Currently used in all the product sub detail screens when
     * back is clicked or DONE is clicked.
     *
     * Add a new class here for each new product sub detail screen to handle back navigation.
     */
    sealed class ProductExitEvent(val shouldShowDiscardDialog: Boolean = true) : Event() {
        class ExitProductDetail(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitInventory(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitPricing(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitShipping(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitImages(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitExternalLink(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitSettings(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
    }

    @Parcelize
    data class Parameters(
        val currencyCode: String?,
        val weightUnit: String?,
        val dimensionUnit: String?,
        val gmtOffset: Float
    ) : Parcelable

    /**
     * [productDraft] is used for the UI. Any updates to the fields in the UI would update this model.
     * [storedProduct] is the [Product] model that is fetched from the API and available in the local db.
     * This is read only and is not updated in any way. It is used in the product detail screen, to check
     * if we need to display the UPDATE menu button (which is only displayed if there are changes made to
     * any of the product fields).
     *
     * [isProductUpdated] is used to determine if there are any changes made to the product by comparing
     * [productDraft] and [storedProduct]. Currently used in the product detail screen to display or hide the UPDATE
     * menu button.
     *
     * When the user first enters the product detail screen, the [productDraft] and [storedProduct] are the same.
     * When a change is made to the product in the UI, the [productDraft] model is updated with whatever change
     * has been made in the UI.
     *
     * The [productBeforeEnteringFragment] is a copy of the product before a specific detail fragment is entered.
     * This is used when the user taps the back button to detect if any changes were made in that fragment, and
     * if so we ask whether the user wants to discard changes. If they do, then we reset [productDraft] back to
     * [productBeforeEnteringFragment] to restore it to the state it was when the fragment was entered.
     */
    @Parcelize
    data class ProductDetailViewState(
        var storedProduct: Product? = null,
        val productDraft: Product? = null,
        var productBeforeEnteringFragment: Product? = null,
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
        val storedPassword: String? = null,
        val draftPassword: String? = null
    ) : Parcelable {
        val isOnSale: Boolean
            get() = salePriceWithCurrency != null
        val isPasswordChanged: Boolean
            get() = storedPassword != draftPassword
    }

    @Parcelize
    data class ProductInventoryViewState(
        val skuErrorMessage: Int? = null,
        val stockQuantityErrorMessage: Int? = null
    ) : Parcelable

    @Parcelize
    data class ProductPricingViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION,
        val taxClassList: List<TaxClass>? = null,
        val saleStartDate: Date? = null,
        val saleEndDate: Date? = null,
        val salePriceErrorMessage: Int? = null,
        val regularPrice: BigDecimal? = null,
        val salePrice: BigDecimal? = null
    ) : Parcelable {
        val isRemoveMaxDateButtonVisible: Boolean
            get() = saleEndDate != null
    }

    @Parcelize
    data class ProductImagesViewState(
        val isUploadingImages: Boolean = false
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDetailViewModel>
}
