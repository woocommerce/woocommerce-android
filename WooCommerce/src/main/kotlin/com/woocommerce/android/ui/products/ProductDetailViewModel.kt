package com.woocommerce.android.ui.products

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_SHARE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.addNewItem
import com.woocommerce.android.extensions.clearList
import com.woocommerce.android.extensions.containsItem
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.getList
import com.woocommerce.android.extensions.isEmpty
import com.woocommerce.android.extensions.removeItem
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.addTags
import com.woocommerce.android.model.sortCategories
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitExternalLink
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDetail
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductTags
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductCategory
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCatalogVisibility
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductImageGallery
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductMenuOrder
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductSlug
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductStatus
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductVisibility
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.ui.products.categories.ProductCategoryItemUiModel
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.settings.ProductCatalogVisibility
import com.woocommerce.android.ui.products.settings.ProductVisibility
import com.woocommerce.android.ui.products.tags.ProductTagsRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.Optional
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.math.BigDecimal
import java.util.Date

@OpenClassOnDebug
class ProductDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val resources: ResourceProvider,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val productTagsRepository: ProductTagsRepository,
    private val prefs: AppPrefs
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
        const val DEFAULT_ADD_NEW_PRODUCT_ID: Long = 0L
    }

    private val navArgs: ProductDetailFragmentArgs by savedState.navArgs()

    /**
     * Fetch product related properties (currency, product dimensions) for the site since we use this
     * variable in many different places in the product detail view such as pricing, shipping.
     */
    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_PRODUCT_PARAMETERS, savedState)
    }

    // view state for the product detail screen
    val productDetailViewStateData = LiveDataDelegate(savedState, ProductDetailViewState()) { old, new ->
        if (old?.productDraft != new.productDraft) {
            new.productDraft?.let {
                updateCards(it)
            }
        }
    }
    private var viewState by productDetailViewStateData

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

    val isProductPublished: Boolean
        get() = viewState.productDraft?.status == ProductStatus.PUBLISH

    /**
     * Returns boolean value of [navArgs.isAddProduct] to determine if the view model was started for the **add** flow
     * */
    private val isAddFlowEntryPoint: Boolean
        get() = navArgs.isAddProduct

    /**
     * Validates if the view model was started for the **add** flow AND there is an already valid product id
     * value to check.
     *
     * [isAddFlowEntryPoint] can be TRUE/FALSE
     *
     * [viewState.productDraft.remoteId]
     * .can be [NULL] - no product draft available yet
     * .can be [DEFAULT_ADD_NEW_PRODUCT_ID] - navArgs.remoteProductId is set to default
     * .can be a valid [ID] - navArgs.remoteProductId was passed with a valid ID
     * */
    val isAddFlow: Boolean
        get() = isAddFlowEntryPoint && viewState.productDraft?.remoteId == DEFAULT_ADD_NEW_PRODUCT_ID

    /**
     * Returns boolean value of [navArgs.isTrashEnabled] to determine if the detail fragment should enable
     * trash menu. Always returns false when we're in the add flow.
     */
    val isTrashEnabled: Boolean
        get() = !isAddFlow && navArgs.isTrashEnabled && FeatureFlag.PRODUCT_RELEASE_M5.isEnabled()

    init {
        start()
    }

    fun start() {
        EventBus.getDefault().register(this)
        when (isAddFlowEntryPoint) {
            true -> startAddNewProduct()
            else -> loadRemoteProduct(navArgs.remoteProductId)
        }
    }

    private fun startAddNewProduct() {
        val preferredSavedType = prefs.getSelectedProductType()
        val defaultProductType = ProductType.fromString(preferredSavedType)
        val defaultProduct = ProductHelper.getDefaultNewProduct(type = defaultProductType)
        viewState = viewState.copy(productDraft = ProductHelper.getDefaultNewProduct(type = defaultProductType))
        updateProductState(defaultProduct)
    }

    fun getProduct() = viewState

    fun getRemoteProductId() = viewState.productDraft?.remoteId ?: DEFAULT_ADD_NEW_PRODUCT_ID

    /**
     * Called when the Share menu button is clicked in Product detail screen
     */
    fun onShareButtonClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_SHARE_BUTTON_TAPPED)
        viewState.productDraft?.let {
            triggerEvent(ShareProduct(it.permalink, it.name))
        }
    }

    /**
     * Called when the Trash menu item is clicked in Product detail screen
     */
    fun onTrashButtonClicked() {
        if (checkConnection() && !viewState.isConfirmingTrash) {
            triggerEvent(
                ShowDiscardDialog(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        viewState = viewState.copy(isConfirmingTrash = false)
                        viewState.productDraft?.let { product ->
                            triggerEvent(ExitWithResult(product.remoteId))
                        }
                    },
                    negativeBtnAction = DialogInterface.OnClickListener { _, _ ->
                        viewState = viewState.copy(isConfirmingTrash = false)
                    },
                    messageId = string.product_confirm_trash,
                    positiveButtonId = string.product_trash_yes,
                    negativeButtonId = string.cancel
                )
            )
        }
    }

    /**
     * Called when an existing image is selected in Product detail screen
     */
    fun onImageClicked(image: Product.Image) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.productDraft?.let {
            triggerEvent(ViewProductImageGallery(it.remoteId, it.images))
        }
        updateProductBeforeEnteringFragment()
    }

    /**
     * Called when the add image icon is clicked in Product detail screen
     */
    fun onAddImageButtonClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.productDraft?.let {
            triggerEvent(ViewProductImageGallery(it.remoteId, it.images, true))
        }
        updateProductBeforeEnteringFragment()
    }

    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product detail screen
     */
    fun onEditProductCardClicked(target: ProductNavigationTarget, stat: Stat? = null) {
        stat?.let { AnalyticsTracker.track(it) }
        triggerEvent(target)
        updateProductBeforeEnteringFragment()
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
            is ExitSettings -> {
                hasChanges = hasSettingsChanges()
            }
            is ExitExternalLink -> {
                eventName = Stat.EXTERNAL_PRODUCT_LINK_SETTINGS_DONE_BUTTON_TAPPED
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
     * Called when the UPDATE/PUBLISH menu button is clicked in the product detail screen.
     * Displays a progress dialog and updates/publishes the product
     */
    fun onUpdateButtonClicked() {
        when (isAddFlow) {
            true -> startPublishProduct()
            else -> startUpdateProduct()
        }
    }

    /**
     * Called when the "Save as draft" button is clicked in Product detail screen
     */
    fun onSaveAsDraftButtonClicked() {
        // TODO analytics
        updateProductDraft(productStatus = DRAFT)
        startPublishProduct()
    }

    private fun startUpdateProduct() {
        AnalyticsTracker.track(PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED)
        viewState.productDraft?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch { updateProduct(it) }
        }
    }

    private fun startPublishProduct(exitWhenDone: Boolean = false) {
        viewState.productDraft?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch {
                val isSuccess = addProduct(it)
                if (isSuccess && exitWhenDone) {
                    triggerEvent(ExitProduct)
                }
            }
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

    fun onViewProductOnStoreLinkClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED)
        viewState.productDraft?.permalink?.let { url ->
            triggerEvent(LaunchUrlInChromeTab(url))
        }
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
            is ExitProductDetail -> isProductDetailUpdated
            else -> isProductDetailUpdated && isProductSubDetailUpdated
        }
        if (isProductUpdated && event.shouldShowDiscardDialog) {
            val positiveAction = DialogInterface.OnClickListener { _, _ ->
                // discard changes made to the current screen
                discardEditChanges()

                // if the user is in Product detail screen, exit product detail,
                // otherwise, redirect to Product Detail screen
                if (event is ExitProductDetail) {
                    triggerEvent(ExitProduct)
                } else {
                    triggerEvent(event)
                }
            }

            // if the user is adding a product and this is product detail, include a "Save as draft" neutral
            // button in the discard dialog
            @StringRes val neutralBtnId: Int?
            val neutralAction = if (isAddFlow && event is ExitProductDetail) {
                neutralBtnId = string.product_detail_save_as_draft
                DialogInterface.OnClickListener { _, _ ->
                    updateProductDraft(productStatus = DRAFT)
                    startPublishProduct(exitWhenDone = true)
                }
            } else {
                neutralBtnId = null
                null
            }

            triggerEvent(ShowDiscardDialog(
                    positiveBtnAction = positiveAction,
                    neutralBtnAction = neutralAction
            ))
            return false
        } else if (event is ExitProductDetail && isUploadingImages) {
            // images can't be assigned to the product until they finish uploading so ask whether
            // to discard the uploading images
            triggerEvent(ShowDiscardDialog(
                    messageId = string.discard_images_message,
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        ProductImagesService.cancel()
                        triggerEvent(event)
                    }
            ))
            return false
        } else {
            return true
        }
    }

    fun onProductTitleChanged(title: String) {
        if (title != viewState.productDraft?.name?.fastStripHtml()) {
            updateProductDraft(title = title)
        }
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
        stockQuantity: Int? = null,
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
        type: ProductType? = null,
        groupedProductIds: List<Long>? = null
    ) {
        viewState.productDraft?.let { product ->
            val currentProduct = product.copy()
            val updatedProduct = product.copy(
                    description = description ?: product.description,
                    shortDescription = shortDescription ?: product.shortDescription,
                    name = title ?: product.name,
                    sku = sku ?: product.sku,
                    slug = slug ?: product.slug,
                    isStockManaged = manageStock ?: product.isStockManaged,
                    stockStatus = stockStatus ?: product.stockStatus,
                    isSoldIndividually = soldIndividually ?: product.isSoldIndividually,
                    backorderStatus = backorderStatus ?: product.backorderStatus,
                    stockQuantity = stockQuantity ?: product.stockQuantity,
                    images = images ?: product.images,
                    regularPrice = regularPrice ?: product.regularPrice,
                    salePrice = salePrice ?: product.salePrice,
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
                    groupedProductIds = groupedProductIds ?: product.groupedProductIds,
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

    private fun updateCards(product: Product) {
        _productDetailCards.value = cardBuilder.buildPropertyCards(product, viewState.storedProduct?.sku ?: "")
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
    private fun discardEditChanges() {
        viewState = viewState.copy(productDraft = viewState.productBeforeEnteringFragment)

        // updates the UPDATE menu button in the product detail screen i.e. the UPDATE menu button
        // will only be displayed if there are changes made to the Product model.
        updateProductEditAction()
    }

    fun checkConnection(): Boolean {
        return if (networkStatus.isConnected()) {
            true
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            false
        }
    }

    private fun loadRemoteProduct(remoteProductId: Long) {
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

    private suspend fun fetchProduct(remoteProductId: Long) {
        if (checkConnection()) {
            val fetchedProduct = productRepository.fetchProduct(remoteProductId)
            if (fetchedProduct != null) {
                updateProductState(fetchedProduct)
            } else {
                triggerEvent(ShowSnackbar(string.product_detail_fetch_product_error))
                triggerEvent(Exit)
            }
        } else {
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

    /**
     * Checks whether product images are uploading and ensures the view state reflects any currently
     * uploading images
     */
    private fun checkImageUploads(remoteProductId: Long) {
        viewState = if (ProductImagesService.isUploadingForProduct(remoteProductId)) {
            val uris = ProductImagesService.getUploadingImageUris(remoteProductId)
            viewState.copy(uploadingImageUris = uris)
        } else {
            viewState.copy(uploadingImageUris = emptyList())
        }
    }

    /**
     * Updates the product to the backend only if network is connected.
     * Otherwise, an offline snackbar is displayed.
     */
    private suspend fun updateProduct(product: Product) {
        if (checkConnection()) {
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
                loadRemoteProduct(product.remoteId)
            } else {
                triggerEvent(ShowSnackbar(string.product_detail_update_product_error))
            }
        }

        viewState = viewState.copy(isProgressDialogShown = false)
    }

    /**
     * Returns true if the product draft has a status of DRAFT
     */
    fun isDraftProduct() = viewState.productDraft?.status?.let { it == DRAFT } ?: false

    /**
     * Add a new product to the backend only if network is connected.
     * Otherwise, an offline snackbar is displayed. Returns true only
     * if product successfully added
     */
    private suspend fun addProduct(product: Product): Boolean {
        var isSuccess = false
        if (checkConnection()) {
            @StringRes val successId = if (isDraftProduct()) {
                string.product_detail_publish_product_draft_success
            } else {
                string.product_detail_publish_product_success
            }

            @StringRes val failId = if (isDraftProduct()) {
                string.product_detail_publish_product_draft_error
            } else {
                string.product_detail_publish_product_error
            }

            val result = productRepository.addProduct(product)
            isSuccess = result.first
            if (isSuccess) {
                triggerEvent(ShowSnackbar(successId))
                viewState = viewState.copy(
                    productDraft = null,
                    productBeforeEnteringFragment = getProduct().storedProduct,
                    isProductUpdated = false
                )
                val newProductRemoteId = result.second
                loadRemoteProduct(newProductRemoteId)
                triggerEvent(RefreshMenu)
            } else {
                triggerEvent(ShowSnackbar(failId))
            }
        }
        viewState = viewState.copy(isProgressDialogShown = false)
        return isSuccess
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

        viewState = viewState.copy(
                productDraft = updatedDraft,
                storedProduct = productToUpdateFrom
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

    /**
     * The list of product images has started uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateStartedEvent) {
        checkImageUploads(event.id)
    }

    /**
     * The list of product images has finished uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImagesUpdateCompletedEvent) {
        var productId = event.id
        if (event.isCancelled) {
            viewState = viewState.copy(uploadingImageUris = emptyList())
        } else {
            when (isAddFlow) {
                true -> productId = DEFAULT_ADD_NEW_PRODUCT_ID
                else -> loadRemoteProduct(event.id)
            }
        }
        checkImageUploads(productId)
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

    fun fetchProductCategories() {
        if (_productCategories.value == null) {
            loadProductCategories()
        }
    }

    fun onAddCategoryButtonClicked() {
        AnalyticsTracker.track(Stat.PRODUCT_CATEGORY_SETTINGS_ADD_BUTTON_TAPPED)
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
        class ExitExternalLink(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitSettings(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductCategories(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductTags(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
    }

    data class LaunchUrlInChromeTab(val url: String) : Event()
    object RefreshMenu : Event()

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
        val isProductUpdated: Boolean? = null,
        val storedPassword: String? = null,
        val draftPassword: String? = null,
        val showBottomSheetButton: Boolean? = null,
        val isConfirmingTrash: Boolean = false
    ) : Parcelable {
        val isPasswordChanged: Boolean
            get() = storedPassword != draftPassword
    }

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
