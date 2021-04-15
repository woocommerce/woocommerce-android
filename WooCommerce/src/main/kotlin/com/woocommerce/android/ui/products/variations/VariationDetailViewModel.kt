package com.woocommerce.android.ui.products.variations

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateCompletedEvent
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImagesUpdateStartedEvent
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.ProductVariation.Option
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewImageGallery
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import java.math.BigDecimal
import java.util.Date

class VariationDetailViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val variationRepository: VariationDetailRepository,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val parameterRepository: ParameterRepository,
    private val resources: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_VARIATION_PARAMETERS = "key_variation_parameters"
    }

    private val navArgs: VariationDetailFragmentArgs by savedState.navArgs()

    private var originalVariation: ProductVariation? = null
        get() {
            if (field == null) {
                loadVariation(navArgs.remoteProductId, navArgs.remoteVariationId)
            }
            return field
        }
        set(value) {
            // Update the cards (and the original SKU, so that that the "SKU error taken" is not shown unnecessarily
            if (field != value && value != null) {
                field = value
                updateCards(value)
            }
        }

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_VARIATION_PARAMETERS, savedState)
    }

    // view state for the variation detail screen
    val variationViewStateData = LiveDataDelegate(savedState, VariationViewState()) { old, new ->
        if (old?.variation != new.variation) {
            new.variation?.let {
                updateCards(it)
            }
        }
    }
    private var viewState by variationViewStateData

    private val _variationDetailCards = MutableLiveData<List<ProductPropertyCard>>()
    val variationDetailCards: LiveData<List<ProductPropertyCard>> = _variationDetailCards

    private val cardBuilder by lazy {
        VariationDetailCardBuilder(
            this,
            resources,
            currencyFormatter,
            parameters
        )
    }

    init {
        EventBus.getDefault().register(this)

        viewState = viewState.copy(parentProduct = productRepository.getProduct(navArgs.remoteProductId))
        originalVariation?.let {
            showVariation(it.copy())
        }
    }

    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product variation screen
     */
    fun onEditVariationCardClicked(target: VariationNavigationTarget, stat: Stat? = null) {
        stat?.let { AnalyticsTracker.track(it) }
        triggerEvent(target)
    }

    fun onExit() {
        when {
            isUploadingImages(navArgs.remoteVariationId) -> {
                // images can't be assigned to the product until they finish uploading so ask whether to discard images.
                triggerEvent(ShowDialog.buildDiscardDialogEvent(
                    messageId = string.discard_images_message,
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(Exit)
                    }
                ))
            }
            viewState.variation != originalVariation -> {
                triggerEvent(ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(Exit)
                    }
                ))
            }
            else -> {
                triggerEvent(Exit)
            }
        }
    }

    fun onImageClicked(image: Image) {
        AnalyticsTracker.track(PRODUCT_VARIATION_IMAGE_TAPPED)
        triggerEvent(ViewImageGallery(navArgs.remoteVariationId, listOf(image)))
    }

    fun onAddImageButtonClicked() {
        AnalyticsTracker.track(PRODUCT_VARIATION_IMAGE_TAPPED)
        val images = viewState.variation?.image?.let { listOf(it) } ?: emptyList()
        triggerEvent(ViewImageGallery(navArgs.remoteVariationId, images, showChooser = true))
    }

    fun isUploadingImages(remoteId: Long) = ProductImagesService.isUploadingForProduct(remoteId)

    fun onVariationVisibilitySwitchChanged(isVisible: Boolean) {
        AnalyticsTracker.track(PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED)
        onVariationChanged(isVisible = isVisible)
    }

    fun onVariationChanged(
        remoteProductId: Long? = null,
        remoteVariationId: Long? = null,
        sku: String? = null,
        image: Image? = null,
        regularPrice: BigDecimal? = null,
        salePrice: BigDecimal? = null,
        saleEndDate: Date? = viewState.variation?.saleEndDateGmt,
        saleStartDate: Date? = viewState.variation?.saleStartDateGmt,
        isSaleScheduled: Boolean? = null,
        stockStatus: ProductStockStatus? = null,
        backorderStatus: ProductBackorderStatus? = null,
        stockQuantity: Double? = null,
        options: List<Option>? = null,
        isPurchasable: Boolean? = null,
        isVirtual: Boolean? = null,
        isDownloadable: Boolean? = null,
        description: String? = null,
        isVisible: Boolean? = null,
        isStockManaged: Boolean? = null,
        shippingClass: String? = null,
        shippingClassId: Long? = null,
        attributes: Array<VariantOption>? = null,
        length: Float? = null,
        width: Float? = null,
        height: Float? = null,
        weight: Float? = null
    ) {
        viewState.variation?.let { variation ->
            showVariation(
                variation.copy(
                    remoteProductId = remoteProductId ?: variation.remoteProductId,
                    remoteVariationId = remoteVariationId ?: variation.remoteVariationId,
                    sku = sku ?: variation.sku,
                    image = image ?: variation.image,
                    regularPrice = regularPrice ?: variation.regularPrice,
                    salePrice = salePrice ?: variation.salePrice,
                    saleEndDateGmt = saleEndDate,
                    saleStartDateGmt = saleStartDate,
                    isSaleScheduled = isSaleScheduled ?: variation.isSaleScheduled,
                    stockStatus = stockStatus ?: variation.stockStatus,
                    backorderStatus = backorderStatus ?: variation.backorderStatus,
                    stockQuantity = stockQuantity ?: variation.stockQuantity,
                    options = options ?: variation.options,
                    isPurchasable = isPurchasable ?: variation.isPurchasable,
                    isVirtual = isVirtual ?: variation.isVirtual,
                    isDownloadable = isDownloadable ?: variation.isDownloadable,
                    description = description ?: variation.description,
                    isVisible = isVisible ?: variation.isVisible,
                    isStockManaged = isStockManaged ?: variation.isStockManaged,
                    shippingClass = shippingClass ?: variation.shippingClass,
                    shippingClassId = shippingClassId ?: variation.shippingClassId,
                    attributes = attributes ?: variation.attributes,
                    length = length ?: variation.length,
                    width = width ?: variation.width,
                    height = height ?: variation.height,
                    weight = weight ?: variation.weight
                )
            )
        }
    }

    fun onUpdateButtonClicked() {
        viewState.variation?.let {
            viewState = viewState.copy(isProgressDialogShown = true)
            launch {
                updateVariation(it)
            }
        }
    }

    private suspend fun updateVariation(variation: ProductVariation) {
        if (networkStatus.isConnected()) {
            if (variationRepository.updateVariation(variation)) {
                originalVariation = variation
                showVariation(variation)
                loadVariation(variation.remoteProductId, variation.remoteVariationId)
                triggerEvent(ShowSnackbar(string.variation_detail_update_product_success))
            } else {
                if (
                    variation.image?.id == 0L &&
                    variationRepository.lastUpdateVariationErrorType == ProductErrorType.INVALID_VARIATION_IMAGE_ID
                ) {
                    triggerEvent(ShowSnackbar(string.variation_detail_update_variation_image_error))
                } else {
                    triggerEvent(ShowSnackbar(string.variation_detail_update_variation_error))
                }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        viewState = viewState.copy(isProgressDialogShown = false)
    }

    private fun loadVariation(remoteProductId: Long, remoteVariationId: Long) {
        launch {
            val variationInDb = variationRepository.getVariation(remoteProductId, remoteVariationId)
            if (variationInDb != null) {
                originalVariation = variationInDb
                fetchVariation(remoteProductId, remoteVariationId)
            } else {
                viewState = viewState.copy(isSkeletonShown = true)
                fetchVariation(remoteProductId, remoteVariationId)
            }
            viewState = viewState.copy(isSkeletonShown = false)

            // show the variation if we were able to get it, otherwise exit
            if (originalVariation != null) {
                showVariation(originalVariation!!)
            } else {
                triggerEvent(ShowSnackbar(string.variation_detail_fetch_variation_error))
                triggerEvent(Exit)
            }
        }
    }

    private suspend fun fetchVariation(remoteProductId: Long, remoteVariationId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedVariation = variationRepository.fetchVariation(remoteProductId, remoteVariationId)
            originalVariation = fetchedVariation
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        variationRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun updateCards(variation: ProductVariation) {
        launch {
            if (_variationDetailCards.value == null) {
                viewState = viewState.copy(isSkeletonShown = true)
            }
            _variationDetailCards.value = cardBuilder.buildPropertyCards(
                variation,
                variation.sku,
                viewState.parentProduct
            )
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    private fun showVariation(variation: ProductVariation) {
        viewState = viewState.copy(
            variation = variation,
            isDoneButtonVisible = variation != originalVariation
        )
    }

    fun getShippingClassByRemoteShippingClassId(remoteShippingClassId: Long) =
        productRepository.getProductShippingClassByRemoteId(remoteShippingClassId)?.name
            ?: viewState.variation?.shippingClass ?: ""

    /**
     * Checks whether product images are uploading and ensures the view state reflects any currently
     * uploading images
     */
    private fun checkImageUploads(remoteProductId: Long) {
        viewState = if (ProductImagesService.isUploadingForProduct(remoteProductId)) {
            val uri = ProductImagesService.getUploadingImageUris(remoteProductId)?.firstOrNull()
            viewState.copy(
                uploadingImageUri = uri,
                isDoneButtonEnabled = false
            )
        } else {
            viewState.copy(
                uploadingImageUri = null,
                isDoneButtonEnabled = true
            )
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
        if (event.isCancelled) {
            viewState = viewState.copy(
                uploadingImageUri = null,
                isDoneButtonEnabled = true
            )
        }
        checkImageUploads(event.id)
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
                viewState.variation?.let {
                    val variation = it.copy(image = media.toAppModel())
                    showVariation(variation)
                }
            }
        }
        checkImageUploads(navArgs.remoteVariationId)
    }

    @Parcelize
    data class VariationViewState(
        val variation: ProductVariation? = null,
        val isDoneButtonVisible: Boolean? = null,
        val isDoneButtonEnabled: Boolean? = null,
        val isSkeletonShown: Boolean? = null,
        val isProgressDialogShown: Boolean? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val gmtOffset: Float = 0f,
        val shippingClass: String? = null,
        val parentProduct: Product? = null,
        val uploadingImageUri: Uri? = null
    ) : Parcelable

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<VariationDetailViewModel>
}
