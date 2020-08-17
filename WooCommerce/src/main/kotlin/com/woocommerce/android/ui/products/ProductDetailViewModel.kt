package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_AFFILIATE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.addNewItem
import com.woocommerce.android.extensions.clearList
import com.woocommerce.android.extensions.containsItem
import com.woocommerce.android.extensions.getList
import com.woocommerce.android.extensions.isEmpty
import com.woocommerce.android.extensions.isNotEqualTo
import com.woocommerce.android.extensions.isNumeric
import com.woocommerce.android.extensions.removeItem
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnAddProductImagesSelectedCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.TaxClass
import com.woocommerce.android.model.addTags
import com.woocommerce.android.model.sortCategories
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitExternalLink
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitImages
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitInventory
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitPricing
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDetail
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductTags
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitShipping
import com.woocommerce.android.ui.products.ProductImagesFragment.Companion
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductCategory
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
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.categories.ProductCategoryItemUiModel
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.products.settings.ProductVisibility
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.Optional
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Date
import kotlin.random.Random

@OpenClassOnDebug
class ProductDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    private val resources: ResourceProvider,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val productTagsRepository: ProductTagsRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val DEFAULT_ADD_NEW_PRODUCT_ID: Long = 0L
        private const val SEARCH_TYPING_DELAY_MS = 500L
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
        private const val DEFAULT_TEMP_ADD_PRODUCT_ID_MIN_RANGE: Long = 100L
        private const val DEFAULT_TEMP_ADD_PRODUCT_ID_MAX_RANGE: Long = 1000L
    }

    private val navArgs: ProductDetailFragmentArgs by savedState.navArgs()

    /**
     * Fetch product related properties (currency, product dimensions) for the site since we use this
     * variable in many different places in the product detail view such as pricing, shipping.
     */
    val parameters: SiteParameters by lazy {
        val params = savedState.get<SiteParameters>(KEY_PRODUCT_PARAMETERS) ?: loadParameters()
        savedState[KEY_PRODUCT_PARAMETERS] = params
        params
    }

    /**
     * Holds the latest list of product images for the product add flow.
     *
     * Is updated from the ProductImagesFragment
     * */
    var productAddImages: List<Product.Image> = mutableListOf()

    private var skuVerificationJob: Job? = null

    // view state for the product detail screen
    val productDetailViewStateData = LiveDataDelegate(savedState, ProductDetailViewState()) { old, new ->
        if (old?.productDraft != new.productDraft) {
            updateCards()
        }
    }
    private var viewState by productDetailViewStateData

    // view state for the product inventory screen
    val productInventoryViewStateData = LiveDataDelegate(savedState, ProductInventoryViewState())
    private var productInventoryViewState by productInventoryViewStateData

    // view state for the product pricing screen
    val productPricingViewStateData = LiveDataDelegate(savedState, ProductPricingViewState())
    private var productPricingViewState by productPricingViewStateData

    // view state for the product images screen
    val productImagesViewStateData = LiveDataDelegate(savedState, ProductImagesViewState())
    private var productImagesViewState by productImagesViewStateData

    // view state for the product categories screen
    val productCategoriesViewStateData = LiveDataDelegate(savedState, ProductCategoriesViewState())
    private var productCategoriesViewState by productCategoriesViewStateData

    // view state for the product tags screen
    final val productTagsViewStateData = LiveDataDelegate(savedState, ProductTagsViewState())
    private var productTagsViewState by productTagsViewStateData

    private val _productCategories = MutableLiveData<List<ProductCategory>>()
    val productCategories: LiveData<List<ProductCategory>> = _productCategories

    private val _productTags = MutableLiveData<List<ProductTag>>()
    val productTags: LiveData<List<ProductTag>> = _productTags

    private val _addedProductTags = MutableLiveData<MutableList<ProductTag>>()
    val addedProductTags: MutableLiveData<MutableList<ProductTag>> = _addedProductTags

    private val _productDetailCards = MutableLiveData<List<ProductPropertyCard>>()
    val productDetailCards: LiveData<List<ProductPropertyCard>> = _productDetailCards

    private val cardBuilder by lazy {
        ProductDetailCardBuilder(this, resources, currencyFormatter, parameters)
    }

    private val _productDetailBottomSheetList = MutableLiveData<List<ProductDetailBottomSheetUiItem>>()
    val productDetailBottomSheetList: LiveData<List<ProductDetailBottomSheetUiItem>> = _productDetailBottomSheetList

    private val productDetailBottomSheetBuilder by lazy {
        ProductDetailBottomSheetBuilder(resources)
    }

    init {
        start()
    }

    fun start() {
        EventBus.getDefault().register(this)
        when (navArgs.isAddProduct) {
            true -> startAddNewProduct()
            else -> loadProduct(navArgs.remoteProductId)
        }
    }

    private fun startAddNewProduct() {
        viewState = viewState.copy(
            isAddNewProduct = true
        )
    }

    fun getProduct() = viewState

    fun getRemoteProductId() = viewState.productDraft?.remoteId ?: 0L

    fun getTaxClassBySlug(slug: String): TaxClass? {
        return productPricingViewState.taxClassList?.filter { it.slug == slug }?.getOrNull(0)
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

    fun triggerProductAddImagesSelected(images: List<Uri>) =
        EventBus.getDefault().post(OnAddProductImagesSelectedCompletedEvent(images))

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
    fun onImageGalleryClicked(image: Product.Image) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        when (navArgs.isAddProduct) {
            true -> triggerEvent(ViewProductImages(DEFAULT_ADD_NEW_PRODUCT_ID, image, true))
            else -> {
                viewState.productDraft?.let {
                    triggerEvent(ViewProductImages(it.remoteId, image))
                }
            }
        }
    }

    /**
     * Called when the add image icon is clicked in Product detail screen
     */
    fun onAddImageClicked() {
        when (navArgs.isAddProduct) {
            true -> triggerEvent(ViewProductImageChooser(DEFAULT_ADD_NEW_PRODUCT_ID, isAddProduct = true))
            else -> {
                AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
                viewState.productDraft?.let {
                    triggerEvent(ViewProductImageChooser(it.remoteId))
                }
            }
        }
    }

    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product detail screen
     */
    fun onEditProductCardClicked(target: ProductNavigationTarget, stat: Stat? = null) {
        stat?.let { AnalyticsTracker.track(it) }
        triggerEvent(target)
    }

    /**
     * Called when the the Remove end date link is clicked
     */
    fun onRemoveEndDateClicked() {
        productPricingViewState = productPricingViewState.copy(saleEndDate = null)
        updateProductDraft(saleEndDate = Optional(null))
    }

    fun hasInventoryChanges() = viewState.storedProduct?.hasInventoryChanges(viewState.productDraft) ?: false

    fun hasPricingChanges() = viewState.storedProduct?.hasPricingChanges(viewState.productDraft) ?: false

    fun hasShippingChanges() = viewState.storedProduct?.hasShippingChanges(viewState.productDraft) ?: false

    fun hasImageChanges(): Boolean {
        return if (ProductImagesService.isUploadingForProduct(getRemoteProductId())) {
            true
        } else {
            viewState.storedProduct?.hasImageChanges(viewState.productDraft) ?: false
        }
    }

    fun hasCategoryChanges() = viewState.storedProduct?.hasCategoryChanges(viewState.productDraft) ?: false

    fun hasTagChanges() = viewState.storedProduct?.hasTagChanges(viewState.productDraft) ?: false

    fun hasSettingsChanges(): Boolean {
        return if (viewState.storedProduct?.hasSettingsChanges(viewState.productDraft) == true) {
            true
        } else {
            viewState.isPasswordChanged
        }
    }

    fun hasExternalLinkChanges() = viewState.storedProduct?.hasExternalLinkChanges(viewState.productDraft) ?: false

    fun hasChanges(): Boolean {
        return viewState.storedProduct?.let { product ->
            viewState.productDraft?.isSameProduct(product) == false
        } ?: false
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
                hasChanges = hasInventoryChanges()
            }
            is ExitPricing -> {
                eventName = Stat.PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = hasPricingChanges()
            }
            is ExitShipping -> {
                eventName = Stat.PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = hasShippingChanges()
            }
            is ExitImages -> {
                eventName = Stat.PRODUCT_IMAGE_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = hasImageChanges()
            }
            is ExitSettings -> {
                hasChanges = hasSettingsChanges()
            }
            is ExitExternalLink -> {
                hasChanges = hasExternalLinkChanges()
            }
            is ExitProductCategories -> {
                eventName = Stat.PRODUCT_CATEGORY_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = hasCategoryChanges()
            }
            is ExitProductTags -> {
                eventName = Stat.PRODUCT_TAG_SETTINGS_DONE_BUTTON_TAPPED
                hasChanges = hasTagChanges()
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

    fun onViewProductOnStoreLinkClicked(url: String) {
        AnalyticsTracker.track(PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED)
        triggerEvent(LaunchUrlInChromeTab(url))
    }

    fun onAffiliateLinkClicked(url: String) {
        AnalyticsTracker.track(PRODUCT_DETAIL_VIEW_AFFILIATE_TAPPED)
        triggerEvent(LaunchUrlInChromeTab(url))
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
        val isProductDetailUpdated = viewState.isProductUpdated ?: false

        val isProductSubDetailUpdated = viewState.productDraft?.let { draft ->
            viewState.productBeforeEnteringFragment?.isSameProduct(draft) == false ||
                viewState.isPasswordChanged
        } ?: false

        val isUploadingImages = ProductImagesService.isUploadingForProduct(getRemoteProductId())

        val isProductUpdated = when (event) {
            is ExitProductDetail -> isProductDetailUpdated || isUploadingImages
            is ExitImages -> isUploadingImages || hasImageChanges()
            else -> isProductDetailUpdated && isProductSubDetailUpdated
        }
        if (isProductUpdated && event.shouldShowDiscardDialog) {
            triggerEvent(ShowDiscardDialog(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    // discard changes made to the current screen
                    discardEditChanges(event)

                    // If user in Product detail screen, exit product detail,
                    // otherwise, redirect to Product Detail screen
                    if (event is ExitProductDetail) {
                        triggerEvent(ExitProduct)
                    } else {
                        triggerEvent(event)
                    }
                }
            ))
            return false
        } else if ((event is ExitProductDetail || event is ExitImages) && isUploadingImages) {
            // images can't be assigned to the product until they finish uploading so ask whether
            // to discard the uploading images
            triggerEvent(ShowDiscardDialog(
                messageId = string.discard_images_message,
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    ProductImagesService.cancel()
                    if (event is ExitProductDetail) {
                        triggerEvent(ExitProduct)
                    } else {
                        triggerEvent(event)
                    }
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
            val isOnSale = inputValue isNotEqualTo BigDecimal.ZERO
            updateProductDraft(salePrice = inputValue, isOnSale = isOnSale)
            productPricingViewState.copy(salePriceErrorMessage = 0)
        }
    }

    fun onProductTitleChanged(title: String) {
        updateProductDraft(title = title)
    }

    /**
     * Called before entering any product screen to save of copy of the product prior to the user making any
     * changes in that specific screen
     */
    fun updateProductBeforeEnteringFragment() {
        viewState.productBeforeEnteringFragment = viewState.productDraft ?: viewState.storedProduct
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
        isVirtual: Boolean? = null,
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
        menuOrder: Int? = null,
        categories: List<ProductCategory>? = null,
        tags: List<ProductTag>? = null,
        type: ProductType? = null
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
                isVirtual = isVirtual ?: product.isVirtual,
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
                categories = categories ?: product.categories,
                tags = tags ?: product.tags,
                type = type ?: product.type,
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
        productCategoriesRepository.onCleanup()
        productTagsRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun updateCards() {
        viewState.productDraft?.let {
            launch(dispatchers.computation) {
                val cards = cardBuilder.buildPropertyCards(it)
                withContext(dispatchers.main) {
                    _productDetailCards.value = cards
                }
            }
        }
        fetchBottomSheetList()
    }

    fun fetchBottomSheetList() {
        viewState.productDraft?.let {
            launch(dispatchers.computation) {
                val detailList = productDetailBottomSheetBuilder.buildBottomSheetList(it)
                withContext(dispatchers.main) {
                    _productDetailBottomSheetList.value = detailList
                    viewState = viewState.copy(showBottomSheetButton = detailList.isNotEmpty())
                }
            }
        }
    }

    fun onProductDetailBottomSheetItemSelected(uiItem: ProductDetailBottomSheetUiItem) {
        onEditProductCardClicked(uiItem.clickEvent, uiItem.stat)
    }

    /**
     * Called when discard is clicked on any of the product screens to restore the product to
     * the state it was in when the screen was first entered
     */
    private fun discardEditChanges(event: ProductExitEvent) {
        viewState = viewState.copy(productDraft = viewState.productBeforeEnteringFragment)

        if (event is ExitImages) {
            ProductImagesService.cancel()
        }

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

                val cachedVariationCount = productRepository.getCachedVariationCount(remoteProductId)
                if (shouldFetch || cachedVariationCount != productInDb.numVariations) {
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
    private fun loadParameters(): SiteParameters {
        val currencyCode = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode
        val gmtOffset = selectedSite.get().timezone?.toFloat() ?: 0f
        val (weightUnit, dimensionUnit) = wooCommerceStore.getProductSettings(selectedSite.get())?.let { settings ->
            return@let Pair(settings.weightUnit, settings.dimensionUnit)
        } ?: Pair(null, null)

        return SiteParameters(
            currencyCode,
            weightUnit,
            dimensionUnit,
            gmtOffset
        )
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
        if (ProductImagesService.isUploadingForProduct(remoteProductId)) {
            val uris = ProductImagesService.getUploadingImageUrisForProduct(remoteProductId)
            viewState = viewState.copy(uploadingImageUris = uris)
            productImagesViewState = productImagesViewState.copy(isUploadingImages = true)
        } else if (productImagesViewState.isUploadingImages) {
            viewState = viewState.copy(uploadingImageUris = emptyList())
            productImagesViewState = productImagesViewState.copy(isUploadingImages = false)
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
                viewState = viewState.copy(
                    productDraft = null,
                    productBeforeEnteringFragment = getProduct().storedProduct,
                    isProductUpdated = false
                )
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

        if (viewState.productBeforeEnteringFragment == null) {
            viewState = viewState.copy(
                productBeforeEnteringFragment = updatedDraft
            )
        }

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
        if (event.isCancelled) {
            viewState = viewState.copy(uploadingImageUris = emptyList())
        } else {
            loadProduct(event.remoteProductId)
        }

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
                addProductImageToDraft(media.toAppModel())
            }
        }
        checkImageUploads(getRemoteProductId())
    }

    /**
     * Finished selecting images for add product flow
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvenMainThread(event: OnAddProductImagesSelectedCompletedEvent) {
        val originalList = productImagesViewState.localSelectedUriImages?.toMutableList() ?: mutableListOf()
        originalList.addAll(event.images)
        viewState = viewState.copy(addProductLocalUris = originalList)
        productImagesViewState = productImagesViewState.copy(
            localSelectedUriImages = originalList
        )
    }

    fun transformToProductImages(uploadingImageUris: List<Uri>?): List<Product.Image> {
        productAddImages = uploadingImageUris?.let { list ->
            list.map { uri ->
                val tempId =
                    Random.nextLong(
                        DEFAULT_TEMP_ADD_PRODUCT_ID_MIN_RANGE,
                        DEFAULT_TEMP_ADD_PRODUCT_ID_MAX_RANGE
                    )
                return@map Product.Image(
                    id = tempId,
                    name = ProductImagesFragment.DEFAULT_TEMP_ADD_PRODUCT_IMAGE,
                    source = uri.toString(),
                    dateCreated = Date()
                )
            }
        } ?: run {
            listOf<Product.Image>()
        }
        return productAddImages
    }

    /**
     * Adds a single image to the list of product draft's images
     */
    fun addProductImageToDraft(image: Product.Image) {
        val imageList = ArrayList<Product.Image>().also {
            it.add(image)
        }
        addProductImageListToDraft(imageList)
    }

    /**
     * Adds multiple images to the list of product draft's images
     */
    fun addProductImageListToDraft(imageList: ArrayList<Product.Image>) {
        // add the existing images to the passed list only
        // if it does not exist in the list already
        viewState.productDraft?.let {
            val updatedImageList = (it.images + imageList).distinct().toList()

            // ...then update the draft's images  with the combined list
            updateProductDraft(images = updatedImageList)
        }
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

    fun fetchProductCategories() {
        if (_productCategories.value == null) {
            loadProductCategories()
        }
    }

    fun onAddCategoryButtonClicked() {
        triggerEvent(AddProductCategory)
    }

    fun onProductCategoryAdded(category: ProductCategory) {
        val selectedCategories = viewState.productDraft?.categories?.toMutableList() ?: mutableListOf()
        selectedCategories.add(category)
        updateProductDraft(categories = selectedCategories)
        refreshProductCategories()
    }

    /**
     * Refreshes the list of categories by calling the [loadProductCategories] method
     * which eventually checks, if there is anything new to fetch from the server
     *
     */
    fun refreshProductCategories() {
        productCategoriesViewState = productCategoriesViewState.copy(isRefreshing = true)
        loadProductCategories()
    }

    /**
     * Loads the list of categories from the database or from the server.
     * This depends on whether categories are stored in the database, and if any new ones are
     * required to be fetched.
     *
     * @param loadMore Whether to load more categories after the ones loaded
     */
    private fun loadProductCategories(loadMore: Boolean = false) {
        if (productCategoriesViewState.isLoading == true) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading product categories")
            return
        }

        if (loadMore && !productCategoriesRepository.canLoadMoreProductCategories) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more product categories")
            return
        }

        launch {
            val showSkeleton: Boolean
            if (loadMore) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the categories from the db and show them immediately
                val productsInDb = productCategoriesRepository.getProductCategoriesList()
                if (productsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    _productCategories.value = productsInDb
                    showSkeleton = productCategoriesViewState.isRefreshing == false
                }
            }
            productCategoriesViewState = productCategoriesViewState.copy(
                isLoading = true,
                isLoadingMore = loadMore,
                isSkeletonShown = showSkeleton,
                isEmptyViewVisible = false
            )
            fetchProductCategories(loadMore = loadMore)
        }
    }

    /**
     * Triggered when the user scrolls past the point of loaded categories
     * already displayed on the screen or on record.
     */
    fun onLoadMoreCategoriesRequested() {
        loadProductCategories(loadMore = true)
    }

    /**
     * This method is used to fetch the categories from the backend. It does not
     * check the database.
     *
     * @param loadMore Whether this is another page or the first one
     */
    private suspend fun fetchProductCategories(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            _productCategories.value = productCategoriesRepository.fetchProductCategories(loadMore = loadMore)

            productCategoriesViewState = productCategoriesViewState.copy(
                isLoading = true,
                canLoadMore = productCategoriesRepository.canLoadMoreProductCategories,
                isEmptyViewVisible = _productCategories.value?.isEmpty() == true
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productCategoriesViewState = productCategoriesViewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    /**
     * The method takes in a list of product categories and calculates the order and grouping of categories
     * by their parent ids. This creates a stable sorted list of categories by name. The returned list also
     * has margin data, which can be used to visually represent categories in a hierarchy under their
     * parent ids.
     *
     * @param product the product for which the categories are being styled
     * @param productCategories the list of product categories to sort and style
     * @return [List<ProductCategoryItemUiModel>] the sorted styled list of categories
     */
    fun sortAndStyleProductCategories(
        product: Product,
        productCategories: List<ProductCategory>
    ): List<ProductCategoryItemUiModel> {
        // Get the categories of the product
        val selectedCategories = product.categories

        // Sort all incoming categories by their parent
        val sortedList = productCategories.sortCategories(resources)

        // Mark the product categories as selected in the sorted list
        sortedList.map { productCategoryItemUiModel ->
            for (selectedCategory in selectedCategories) {
                if (productCategoryItemUiModel.category.name == selectedCategory.name) {
                    productCategoryItemUiModel.isSelected = true
                }
            }
        }

        return sortedList.toList()
    }

    fun onProductTagDoneMenuActionClicked() {
        val tags = _addedProductTags.getList()
        // check if there are tags entered that do not exist on the site. If so,
        // call the API to add the tags to the site first
        if (tags.isNotEmpty()) {
            productTagsViewState = productTagsViewState.copy(isProgressDialogShown = true)
            launch {
                val addedTags = productTagsRepository.addProductTags(tags.map { it.name })
                // if there are some tags that could not be added, display an error message
                if (addedTags.size < tags.size) {
                    triggerEvent(ShowSnackbar(string.product_add_tag_error))
                }

                // add the newly added tags to the product
                _addedProductTags.clearList()
                updateProductDraft(tags = addedTags.addTags(viewState.productDraft))

                // redirect to the product detail screen
                productTagsViewState = productTagsViewState.copy(isProgressDialogShown = false)
                onDoneButtonClicked(ExitProductTags(shouldShowDiscardDialog = false))
            }
        } else {
            // There are no newly added tags so redirect to the product detail screen
            onDoneButtonClicked(ExitProductTags(shouldShowDiscardDialog = false))
        }
    }

    /**
     * Method called when a tag is entered
     */
    fun onProductTagAdded(tagName: String) {
        // verify if the entered tagName exists for the site
        // It so, the tag should be added to the product directly
        productTagsRepository.getProductTagByName(tagName)?.let {
            onProductTagSelected(it)
        } ?: run {
            // Since the tag does not exist for the site, add the tag to
            // a list of newly added tags
            _addedProductTags.addNewItem(ProductTag(name = tagName))
            updateTagsMenuAction()
        }
    }

    /**
     * Method called when a tag is selected from the list of product tags
     */
    fun onProductTagSelected(tag: ProductTag) {
        updateProductDraft(tags = tag.addTag(viewState.productDraft))
        updateTagsMenuAction()
    }

    /**
     * Method called when a tag is removed from the product
     */
    fun onProductTagSelectionRemoved(tag: ProductTag) {
        // check if the tag is newly added. If so, remove it from the newly added tags
        if (_addedProductTags.containsItem(tag)) {
            _addedProductTags.removeItem(tag)
        } else {
            updateProductDraft(tags = tag.removeTag(viewState.productDraft))
        }
        updateTagsMenuAction()
    }

    private fun updateTagsMenuAction() {
        productTagsViewState = productTagsViewState.copy(
            shouldDisplayDoneMenuButton = viewState.productDraft?.tags?.isNotEmpty() == true ||
                !_addedProductTags.isEmpty()
        )
    }

    fun fetchProductTags() {
        if (_productTags.value == null) {
            loadProductTags()
        }
    }

    /**
     * Refreshes the list of tags by calling the [loadProductTags] method
     * which eventually checks, if there is anything new to fetch from the server
     *
     */
    fun refreshProductTags() {
        productTagsViewState = productTagsViewState.copy(isRefreshing = true)
        loadProductTags()
    }

    /**
     * Loads the list of tags from the database or from the server.
     * This depends on whether tags are stored in the database, and if any new ones are
     * required to be fetched.
     *
     * @param loadMore Whether to load more tags after the ones loaded
     */
    private fun loadProductTags(loadMore: Boolean = false) {
        if (productTagsViewState.isLoading == true) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading product tags")
            return
        }

        if (loadMore && !productTagsRepository.canLoadMoreProductTags) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more product tags")
            return
        }

        launch {
            val showSkeleton: Boolean
            if (loadMore) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the tags from the db and show them immediately
                val productTagsInDb = productTagsRepository.getProductTags()
                if (productTagsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    _productTags.value = productTagsInDb
                    showSkeleton = productTagsViewState.isRefreshing == false
                }
            }
            productTagsViewState = productTagsViewState.copy(
                isLoading = true,
                isLoadingMore = loadMore,
                isSkeletonShown = showSkeleton,
                isEmptyViewVisible = false
            )
            fetchProductTags(loadMore = loadMore)
        }
    }

    /**
     * Triggered when the user scrolls past the point of loaded tags
     * already displayed on the screen or on record.
     */
    fun onLoadMoreTagsRequested() {
        loadProductTags(loadMore = true)
    }

    /**
     * This method is used to fetch the tags from the backend. It does not
     * check the database.
     *
     * @param loadMore Whether this is another page or the first one
     */
    private suspend fun fetchProductTags(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            _productTags.value = productTagsRepository.fetchProductTags(loadMore = loadMore)

            productTagsViewState = productTagsViewState.copy(
                isLoading = true,
                canLoadMore = productTagsRepository.canLoadMoreProductTags,
                isEmptyViewVisible = _productTags.value?.isEmpty() == true
            )
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        productTagsViewState = productTagsViewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false
        )
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
        class ExitProductCategories(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductTags(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
    }

    data class LaunchUrlInChromeTab(val url: String) : Event()

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
        val draftPassword: String? = null,
        val showBottomSheetButton: Boolean? = null,
        val isAddNewProduct: Boolean? = null,
        val addProductLocalUris: List<Uri>? = null
    ) : Parcelable {
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
        val isUploadingImages: Boolean = false,
        val localSelectedUriImages: List<Uri>? = null
    ) : Parcelable

    @Parcelize
    data class ProductCategoriesViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable {
        val isAddCategoryButtonVisible: Boolean
            get() = isSkeletonShown == false
    }

    @Parcelize
    data class ProductTagsViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val shouldDisplayDoneMenuButton: Boolean? = null,
        val isProgressDialogShown: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDetailViewModel>
}
