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
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_PRODUCT_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_PRODUCT_PUBLISH_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_PRODUCT_SAVE_AS_DRAFT_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_PRODUCT_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_DETAIL_PRODUCT_DELETED
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
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductFile
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.model.addTags
import com.woocommerce.android.model.sortCategories
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitExternalLink
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAddAttribute
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductAttributeList
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductCategories
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductDownloads
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitProductTags
import com.woocommerce.android.ui.products.ProductDetailViewModel.ProductExitEvent.ExitSettings
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductAttribute
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductAttributeTerms
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductCategory
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductDownloadableFile
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ShareProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductCatalogVisibility
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadDetails
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewProductDownloadsSettings
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
import com.woocommerce.android.util.Optional
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.LaunchUrlInChromeTab
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
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
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import java.math.BigDecimal
import java.util.Collections
import java.util.Date
import java.util.Locale

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
    private val mediaFilesRepository: MediaFilesRepository,
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

    // view state for the product downloads screen
    final val productDownloadsViewStateData = LiveDataDelegate(savedState, ProductDownloadsViewState())
    private var productDownloadsViewState by productDownloadsViewStateData

    private val _productCategories = MutableLiveData<List<ProductCategory>>()
    val productCategories: LiveData<List<ProductCategory>> = _productCategories

    private val _productTags = MutableLiveData<List<ProductTag>>()
    val productTags: LiveData<List<ProductTag>> = _productTags

    private val _addedProductTags = MutableLiveData<MutableList<ProductTag>>()
    val addedProductTags: MutableLiveData<MutableList<ProductTag>> = _addedProductTags

    private val _attributeList = MutableLiveData<List<ProductAttribute>>()
    val attributeList: LiveData<List<ProductAttribute>> = _attributeList

    final val globalAttributeTermsViewStateData = LiveDataDelegate(savedState, GlobalAttributesTermsViewState())
    private var globalAttributesTermsViewState by globalAttributeTermsViewStateData

    private val _attributeTermsList = MutableLiveData<List<ProductAttributeTerm>>()
    val attributeTermsList: LiveData<List<ProductAttributeTerm>> = _attributeTermsList

    final val globalAttributeViewStateData = LiveDataDelegate(savedState, GlobalAttributesViewState())
    private var globalAttributesViewState by globalAttributeViewStateData

    private val _globalAttributeList = MutableLiveData<List<ProductGlobalAttribute>>()
    val globalAttributeList: LiveData<List<ProductGlobalAttribute>> = _globalAttributeList

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
        get() = !isAddFlow && navArgs.isTrashEnabled

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
        val defaultProduct = ProductHelper.getDefaultNewProduct(productType = defaultProductType)
        viewState = viewState.copy(productDraft = ProductHelper.getDefaultNewProduct(productType = defaultProductType))
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
                ShowDialog(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        AnalyticsTracker.track(PRODUCT_DETAIL_PRODUCT_DELETED)
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

    fun onProductDownloadClicked(file: ProductFile) {
        triggerEvent(
            ViewProductDownloadDetails(
                true,
                file
            )
        )
    }

    fun updateDownloadableFileInDraft(updatedFile: ProductFile) {
        viewState.productDraft?.let {
            val updatedDownloads = it.downloads.map { file ->
                if (file.id == updatedFile.id) updatedFile else file
            }
            updateProductDraft(
                downloads = updatedDownloads
            )
        }
    }

    fun addDownloadableFileToDraft(file: ProductFile) {
        viewState.productDraft?.let {
            val updatedDownloads = it.downloads + file
            updateProductDraft(
                downloads = updatedDownloads,
                // Make sure to mark the file as downloadable
                isDownloadable = true
            )
        }
    }

    fun swapDownloadableFiles(from: Int, to: Int) {
        viewState.productDraft?.let {
            val mutableDownloadsList = it.downloads.toMutableList()
            Collections.swap(mutableDownloadsList, from, to)
            updateProductDraft(downloads = mutableDownloadsList)
        }
    }

    fun deleteDownloadableFile(file: ProductFile) {
        viewState.productDraft?.let {
            val updatedDownloads = it.downloads - file
            updateProductDraft(downloads = updatedDownloads)
            // If the downloads list is empty now, go directly to the product details screen
            if (updatedDownloads.isEmpty()) triggerEvent(ExitProductDownloads(shouldShowDiscardDialog = false))
        }
    }

    fun onDownloadExpiryChanged(value: Int) {
        viewState.productDraft?.let {
            updateProductDraft(
                downloadExpiry = value
            )
        }
    }

    fun onDownloadLimitChanged(value: Long) {
        viewState.productDraft?.let {
            updateProductDraft(
                downloadLimit = value
            )
        }
    }

    fun onDownloadsSettingsClicked() {
        triggerEvent(ViewProductDownloadsSettings)
    }

    fun onAddDownloadableFileClicked() {
        triggerEvent(AddProductDownloadableFile)
    }

    fun uploadDownloadableFile(uri: Uri) {
        launch {
            viewState = viewState.copy(isUploadingDownloadableFile = true)
            productDownloadsViewState = productDownloadsViewState.copy(isUploadingDownloadableFile = true)
            try {
                val url = mediaFilesRepository.uploadFile(uri)
                showAddProductDownload(url)
            } catch (e: Exception) {
                triggerEvent(ShowSnackbar(string.product_downloadable_files_upload_failed))
            } finally {
                viewState = viewState.copy(isUploadingDownloadableFile = false)
                productDownloadsViewState = productDownloadsViewState.copy(isUploadingDownloadableFile = false)
            }
        }
    }

    fun showAddProductDownload(url: String) {
        triggerEvent(
            ViewProductDownloadDetails(
                isEditing = false,
                file = ProductFile(id = null, url = url, name = "")
            )
        )
    }

    fun hasExternalLinkChanges() = viewState.storedProduct?.hasExternalLinkChanges(viewState.productDraft) ?: false

    fun hasLinkedProductChanges() = viewState.storedProduct?.hasLinkedProductChanges(viewState.productDraft) ?: false

    fun hasDownloadsChanges(): Boolean {
        return viewState.storedProduct?.hasDownloadChanges(viewState.productDraft) ?: false
    }

    fun hasDownloadsSettingsChanges(): Boolean {
        return viewState.storedProduct?.let {
            it.downloadLimit != viewState.productDraft?.downloadLimit ||
                it.downloadExpiry != viewState.productDraft?.downloadExpiry ||
                it.isDownloadable != viewState.productDraft?.isDownloadable
        } ?: false
    }

    fun hasChanges(): Boolean {
        return viewState.storedProduct?.let { product ->
            viewState.productDraft?.isSameProduct(product) == false || viewState.isPasswordChanged
        } ?: false
    }

    /**
     * Called when the back= button is clicked in a product sub detail screen
     */
    fun onBackButtonClicked(event: ProductExitEvent) {
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
            is ExitProductAttributeList -> {
                // TODO: eventName
                hasChanges = hasAttributeChanges()
            }
            is ExitProductAddAttribute -> {
                // TODO: eventName
                hasChanges = hasAttributeChanges()
            }
        }
        eventName?.let { AnalyticsTracker.track(it, mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to hasChanges)) }
        triggerEvent(event)
    }

    /**
     * Method called when the back button in product detail is clicked. We show a discard dialog if any
     * changes have been made to the [Product] model locally that still need to be saved to the backend.
     */
    fun onBackButtonClickedProductDetail(): Boolean {
        val isProductDetailUpdated = viewState.isProductUpdated ?: false
        val isUploadingImages = ProductImagesService.isUploadingForProduct(getRemoteProductId())

        if (isProductDetailUpdated) {
            val positiveAction = DialogInterface.OnClickListener { _, _ ->
                // discard changes made to the product and exit product detail
                discardEditChanges()
                triggerEvent(ExitProduct)
            }

            // if the user is adding a product and this is product detail, include a "Save as draft" neutral
            // button in the discard dialog
            @StringRes val neutralBtnId: Int?
            val neutralAction = if (isAddFlow) {
                neutralBtnId = string.product_detail_save_as_draft
                DialogInterface.OnClickListener { _, _ ->
                    updateProductDraft(productStatus = DRAFT)
                    startPublishProduct(exitWhenDone = true)
                }
            } else {
                neutralBtnId = null
                null
            }

            triggerEvent(
                ShowDialog(
                    positiveBtnAction = positiveAction,
                    neutralBtnAction = neutralAction
                )
            )
            return false
        } else if (isUploadingImages) {
            // images can't be assigned to the product until they finish uploading so ask whether
            // to discard the uploading images
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                messageId = string.discard_images_message,
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    ProductImagesService.cancel()
                    triggerEvent(ExitProduct)
                }
            ))
            return false
        } else {
            return true
        }
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
            trackPublishing(it)

            viewState = viewState.copy(isProgressDialogShown = true)
            launch {
                val isSuccess = addProduct(it)
                if (isSuccess) {
                    AnalyticsTracker.track(ADD_PRODUCT_SUCCESS)

                    if (exitWhenDone) {
                        triggerEvent(ExitProduct)
                    }
                } else {
                    AnalyticsTracker.track(ADD_PRODUCT_FAILED)
                }
            }
        }
    }

    private fun trackPublishing(it: Product) {
        val properties = mapOf("product_type" to it.productType.value.toLowerCase(Locale.ROOT))
        val statId = if (it.status == DRAFT) ADD_PRODUCT_SAVE_AS_DRAFT_TAPPED else ADD_PRODUCT_PUBLISH_TAPPED
        AnalyticsTracker.track(statId, properties)
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
        type: String? = null,
        groupedProductIds: List<Long>? = null,
        upsellProductIds: List<Long>? = null,
        crossSellProductIds: List<Long>? = null,
        downloads: List<ProductFile>? = null,
        downloadLimit: Long? = null,
        downloadExpiry: Int? = null,
        isDownloadable: Boolean? = null,
        attributes: List<ProductAttribute>? = null
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
                    upsellProductIds = upsellProductIds ?: product.upsellProductIds,
                    crossSellProductIds = crossSellProductIds ?: product.crossSellProductIds,
                    saleEndDateGmt = if (isSaleScheduled == true ||
                            (isSaleScheduled == null && currentProduct.isSaleScheduled)) {
                        if (saleEndDate != null) saleEndDate.value else product.saleEndDateGmt
                    } else viewState.storedProduct?.saleEndDateGmt,
                    saleStartDateGmt = if (isSaleScheduled == true ||
                            (isSaleScheduled == null && currentProduct.isSaleScheduled)) {
                        saleStartDate ?: product.saleStartDateGmt
                    } else viewState.storedProduct?.saleStartDateGmt,
                    downloads = downloads ?: product.downloads,
                    downloadLimit = downloadLimit ?: product.downloadLimit,
                    downloadExpiry = downloadExpiry ?: product.downloadExpiry,
                    isDownloadable = isDownloadable ?: product.isDownloadable,
                    attributes = attributes ?: product.attributes
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
        mediaFilesRepository.onCleanup()
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
        _addedProductTags.clearList()

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
                if (productRepository.lastFetchProductErrorType == ProductErrorType.INVALID_PRODUCT_ID) {
                    triggerEvent(ShowSnackbar(string.product_detail_fetch_product_invalid_id_error))
                } else {
                    triggerEvent(ShowSnackbar(string.product_detail_fetch_product_error))
                }
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
     * Loads the attributes assigned to the draft product, used by the attribute list fragment
     */
    fun loadProductDraftAttributes() {
        _attributeList.value = getProductDraftAttributes()
    }

    /**
     * Returns the list of attributes assigned to the product
     */
    fun getProductDraftAttributes(): List<ProductAttribute> {
        return viewState.productDraft?.attributes ?: emptyList()
    }

    /**
     * Returns the list of term names for a specific attribute assigned to the product
     */
    fun getProductDraftAttributeTerms(attributeId: Long, attributeName: String): List<String> {
        val attributes = getProductDraftAttributes()
        attributes.forEach { attribute ->
            if (attribute.name == attributeName) {
                return attribute.terms
            }
        }
        return emptyList()
    }

    /**
     * Fetches terms for a global product attribute
     */
    fun fetchGlobalAttributeTerms(remoteAttributeId: Long) {
        launch {
            globalAttributesTermsViewState = globalAttributesTermsViewState.copy(isSkeletonShown = true)
            _attributeTermsList.value = productRepository.fetchGlobalAttributeTerms(remoteAttributeId)
            globalAttributesTermsViewState = globalAttributesTermsViewState.copy(isSkeletonShown = false)
        }
    }

    /**
     * Returns the draft attribute matching the passed id and name
     */
    private fun getDraftAttribute(attributeId: Long, attributeName: String): ProductAttribute? {
        return getProductDraftAttributes().firstOrNull {
            it.id == attributeId && it.name == attributeName
        }
    }

    /**
     * Adds a new term to a the product draft attributes
     */
    fun addAttributeTermToDraft(attributeId: Long, attributeName: String, termName: String) {
        val updatedTerms = ArrayList<String>()
        var isVisible = ProductAttribute.DEFAULT_VISIBLE
        var isVariation = ProductAttribute.DEFAULT_IS_VARIATION

        // find this attribute in the draft attributes
        getDraftAttribute(attributeId, attributeName)?.let { thisAttribute ->
            // make sure this term doesn't already exist in this attribute
            thisAttribute.terms.forEach {
                if (it.equals(termName, ignoreCase = true)) {
                    triggerEvent(ShowSnackbar(string.product_term_name_already_exists))
                    return
                }
            }

            // add its terms to our updated term list
            updatedTerms.addAll(thisAttribute.terms)
            isVisible = thisAttribute.isVisible
            isVariation = thisAttribute.isVariation
        }

        // add the passed term to our updated term list
        updatedTerms.add(termName)

        // get the current draft attributes
        val draftAttributes = getProductDraftAttributes()

        // create an updated list without this attribute, then add a new one with the updated terms
        ArrayList<ProductAttribute>().also { updatedAttributes ->
            updatedAttributes.addAll(draftAttributes.filterNot { attribute ->
                attribute.id == attributeId && attribute.name == attributeName
            })

            updatedAttributes.add(
                ProductAttribute(
                    id = attributeId,
                    name = attributeName,
                    terms = updatedTerms,
                    isVisible = isVisible,
                    isVariation = isVariation
                )
            )

            updateProductDraft(attributes = updatedAttributes)
        }
    }

    /**
     * Removes a term from the product draft attributes
     */
    fun removeAttributeTermFromDraft(attributeId: Long, attributeName: String, termName: String) {
        // find this attribute in the draft attributes
        val thisAttribute = getDraftAttribute(attributeId, attributeName)
        if (thisAttribute == null) {
            // TODO
            return
        }

        // created an updated list of terms without the passed one
        val updatedTerms = ArrayList<String>().also { terms ->
            terms.addAll(thisAttribute.terms.filterNot { it.equals(termName, ignoreCase = true) })
        }

        // get the current draft attributes
        val draftAttributes = getProductDraftAttributes()

        // create an updated list without this attribute, then add a new one with the updated terms
        val updatedAttributes = ArrayList<ProductAttribute>().also {
            draftAttributes.filter {
                it.id != attributeId && it.name != attributeName
            }
        }.also {
            if (updatedTerms.isNotEmpty()) {
                it.add(
                    ProductAttribute(
                        id = attributeId,
                        name = attributeName,
                        terms = updatedTerms,
                        isVisible = thisAttribute.isVisible,
                        isVariation = thisAttribute.isVariation
                    )
                )
            }
        }

        updateProductDraft(attributes = updatedAttributes)
    }

    /**
     * Saves any attribute changes to the backend
     */
    fun saveAttributeChanges() {
        if (hasAttributeChanges() && checkConnection()) {
            launch {
                viewState.productDraft?.attributes?.let { attributes ->
                    val result = productRepository.updateProductAttributes(getRemoteProductId(), attributes)
                    if (!result) {
                        triggerEvent(ShowSnackbar(string.product_attributes_error_saving))
                    }
                }
            }
        }
    }

    /**
     * Clears the global attribute terms
     */
    fun resetGlobalAttributeTerms() {
        _attributeTermsList.value = emptyList()
    }

    /**
     * User clicked an attribute in the attribute list fragment or the add attribute fragment
     */
    fun onAttributeListItemClick(attributeId: Long, attributeName: String) {
        triggerEvent(AddProductAttributeTerms(attributeId, attributeName))
    }

    /**
     * User tapped "Add attribute" on the attribute list fragment
     */
    fun onAddAttributeButtonClick() {
        triggerEvent(AddProductAttribute)
    }

    fun hasAttributeChanges() = viewState.storedProduct?.hasAttributeChanges(viewState.productDraft) ?: false

    /**
     * Used by the add attribute screen to fetch the list of store-wide product attributes
     */
    fun fetchGlobalAttributes() {
        launch {
            // load cached global attributes before fetching them, and only show skeleton if the
            // list is still empty
            _globalAttributeList.value = loadGlobalAttributes()
            if (_globalAttributeList.value?.isEmpty() == true) {
                globalAttributesViewState = globalAttributesViewState.copy(isSkeletonShown = true)
            }

            // now fetch from the backend
            _globalAttributeList.value = productRepository.fetchGlobalAttributes()
            globalAttributesViewState = globalAttributesViewState.copy(isSkeletonShown = false)
        }
    }

    fun loadGlobalAttributes(): List<ProductGlobalAttribute> =
        productRepository.getGlobalAttributes()

    /**
     * Returns true if an attribute with this name is assigned to the product draft
     */
    private fun containsAttributeName(attributeName: String): Boolean {
        viewState.productDraft?.attributes?.forEach {
            if (it.name.equals(attributeName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Called from the attribute list when the user enters a new attribute
     */
    fun addLocalAttribute(attributeName: String) {
        if (containsAttributeName(attributeName)) {
            triggerEvent(ShowSnackbar(string.product_attribute_name_already_exists))
            return
        }

        // get the list of current attributes
        val attributes = ArrayList<ProductAttribute>()
        viewState.productDraft?.attributes?.let {
            attributes.addAll(it)
        }

        // add the new one to the list
        attributes.add(
            ProductAttribute(
                id = 0L,
                name = attributeName,
                terms = emptyList(),
                isVisible = ProductAttribute.DEFAULT_VISIBLE,
                isVariation = ProductAttribute.DEFAULT_IS_VARIATION
            )
        )

        // update the draft with the new list
        updateProductDraft(attributes = attributes)

        // take the user to the add attribute terms screen
        triggerEvent(AddProductAttributeTerms(0L, attributeName))
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

    fun onProductTagsBackButtonClicked() {
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
                onBackButtonClicked(ExitProductTags(shouldShowDiscardDialog = false))
            }
        } else {
            // There are no newly added tags so redirect to the product detail screen
            onBackButtonClicked(ExitProductTags(shouldShowDiscardDialog = false))
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
            loadProductTags()
        }
    }

    /**
     * Method called when a tag is selected from the list of product tags
     */
    fun onProductTagSelected(tag: ProductTag) {
        updateProductDraft(tags = tag.addTag(viewState.productDraft))
        loadProductTags()
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
        loadProductTags()
    }

    /**
     * Sets the product tag list to the passed list with the current filter applied and already added tags removed
     * Returns a list of product tags with the passed filter applied
     */
    private fun filterProductTagList(productTags: List<ProductTag>) {
        val addedTags = ArrayList<ProductTag>().also {
            it.addAll(_addedProductTags.getList())
            viewState.productDraft?.tags?.let { draftTags ->
                it.addAll(draftTags)
            }
        }

        _productTags.value = if (productTagsViewState.currentFilter.isEmpty()) {
            productTags.filter { !addedTags.contains(it) }
        } else {
            productTags.filter {
                it.name.contains(
                    productTagsViewState.currentFilter,
                    ignoreCase = true
                ) && !addedTags.contains(it)
            }
        }
    }

    /**
     * Called when user types into product tag screen so we can provide live filtering
     */
    fun setProductTagsFilter(filter: String) {
        productTagsViewState = productTagsViewState.copy(currentFilter = filter)
        val productTags = productTagsRepository.getProductTags()
        filterProductTagList(productTags)

        // fetch from the backend when a filter exists in case not all tags have been fetched yet
        if (filter.isNotEmpty()) {
            launch {
                fetchProductTags(searchQuery = filter)
            }
        }
    }

    /**
     * Called when user exits the product tag fragment to clear the stored filter
     * (otherwise it will be retained when the user returns to the tag fragment)
     */
    fun clearProductTagsState() {
        productTagsViewState = productTagsViewState.copy(currentFilter = "")
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
    fun loadProductTags(loadMore: Boolean = false) {
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
                    filterProductTagList(productTagsInDb)
                    showSkeleton = false
                }
            }
            productTagsViewState = productTagsViewState.copy(
                isLoading = true,
                isLoadingMore = loadMore,
                isSkeletonShown = showSkeleton,
                isEmptyViewVisible = false
            )
            fetchProductTags(loadMore = loadMore, searchQuery = productTagsViewState.currentFilter)
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
     * @param searchQuery optional search query to fetch only matching tags
     */
    private suspend fun fetchProductTags(loadMore: Boolean = false, searchQuery: String? = null) {
        if (networkStatus.isConnected()) {
            val products = productTagsRepository.fetchProductTags(
                loadMore = loadMore,
                searchQuery = searchQuery
            )
            filterProductTagList(products)

            productTagsViewState = productTagsViewState.copy(
                isLoading = true,
                canLoadMore = productTagsRepository.canLoadMoreProductTags,
                isEmptyViewVisible = products.isEmpty() &&
                    _addedProductTags.isEmpty() &&
                    searchQuery.isNullOrEmpty()
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
        class ExitExternalLink(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitSettings(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductCategories(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductTags(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitLinkedProducts(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductDownloads(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductDownloadsSettings(shouldShowDiscardDialog: Boolean = true) :
            ProductExitEvent(shouldShowDiscardDialog)
        class ExitProductAttributeList(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(
            shouldShowDiscardDialog
        )
        class ExitProductAddAttribute(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(
            shouldShowDiscardDialog
        )
        class ExitProductAddAttributeTerms(shouldShowDiscardDialog: Boolean = true) : ProductExitEvent(
            shouldShowDiscardDialog
        )
    }

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
        val isConfirmingTrash: Boolean = false,
        val isUploadingDownloadableFile: Boolean? = null
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
        val isProgressDialogShown: Boolean? = null,
        val currentFilter: String = ""
    ) : Parcelable

    @Parcelize
    data class ProductDownloadsViewState(
        val isUploadingDownloadableFile: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class GlobalAttributesViewState(
        val isSkeletonShown: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class GlobalAttributesTermsViewState(
        val isSkeletonShown: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductDetailViewModel>
}
