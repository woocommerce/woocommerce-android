package com.woocommerce.android.ui.products.variations

import android.content.DialogInterface
import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ID
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.SubscriptionPeriod
import com.woocommerce.android.model.SubscriptionProductVariation
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.media.getMediaUploadErrorMessage
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.canDisplayMessage
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.subscriptions.resetSubscriptionLengthIfThePeriodOrIntervalChanged
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewImageGallery
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ViewMediaUploadErrors
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.Optional
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class VariationDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val variationRepository: VariationDetailRepository,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val parameterRepository: ParameterRepository,
    private val mediaFileUploadHandler: MediaFileUploadHandler,
    private val resources: ResourceProvider,
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_VARIATION_PARAMETERS = "key_variation_parameters"
        private const val KEY_ORIGINAL_VARIATION = "key_original_variation"
    }

    private val navArgs: VariationDetailFragmentArgs by savedState.navArgs()

    private var originalVariation: ProductVariation? = savedState.get<ProductVariation>(KEY_ORIGINAL_VARIATION)
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
                savedState[KEY_ORIGINAL_VARIATION] = value
                updateCards(value)
            }
        }

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_VARIATION_PARAMETERS, savedState)
    }

    // view state for the variation detail screen
    val variationViewStateData = LiveDataDelegate(savedState, VariationViewState()) { old, new ->
        new.variation?.takeIf { it != old?.variation }
            ?.let {
                updateCards(it)
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
            parameters,
        )
    }

    init {
        viewState = viewState.copy(parentProduct = productRepository.getProduct(navArgs.remoteProductId))
        originalVariation?.let {
            showVariation(it.copy())
        }

        observeImageUploadEvents()
    }

    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product variation screen
     */
    fun onEditVariationCardClicked(target: VariationNavigationTarget, stat: AnalyticsEvent? = null) {
        stat?.let { AnalyticsTracker.track(it) }
        triggerEvent(target)
    }

    fun onDeleteVariationClicked() {
        triggerEvent(
            Event.ShowDialogFragment(
                messageId = string.variation_confirm_delete,
                positiveButtonId = string.delete,
                negativeButtonId = string.cancel
            )
        )
    }

    fun onDeleteVariationConfirmed() {
        AnalyticsTracker.track(
            AnalyticsEvent.PRODUCT_VARIATION_REMOVE_BUTTON_TAPPED,
            mapOf(KEY_PRODUCT_ID to viewState.parentProduct?.remoteId)
        )
        viewState = viewState.copy(isConfirmingDeletion = false)
        deleteVariation()
    }

    fun onDeleteVariationCancelled() {
        viewState = viewState.copy(isConfirmingDeletion = false)
    }

    fun onExit() {
        when {
            isUploadingImages() -> {
                // images can't be assigned to the product until they finish uploading so ask whether to discard images.
                triggerEvent(
                    Event.ShowDialog.buildDiscardDialogEvent(
                        messageId = string.discard_images_message,
                        positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                            mediaFileUploadHandler.cancelUpload(navArgs.remoteVariationId)
                            triggerEvent(Event.Exit)
                        }
                    )
                )
            }

            viewState.variation != originalVariation -> {
                triggerEvent(
                    Event.ShowDialog.buildDiscardDialogEvent(
                        positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                            triggerEvent(Event.Exit)
                        }
                    )
                )
            }

            else -> {
                triggerEvent(Event.Exit)
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

    fun isUploadingImages() = viewState.uploadingImageUri != null

    fun onVariationVisibilitySwitchChanged(isVisible: Boolean) {
        AnalyticsTracker.track(PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED)
        onVariationChanged(isVisible = isVisible)
    }

    @Suppress("ComplexMethod")
    fun onVariationChanged(
        remoteProductId: Long? = null,
        remoteVariationId: Long? = null,
        sku: String? = null,
        image: Optional<Image>? = null,
        regularPrice: BigDecimal? = viewState.variation?.regularPrice,
        salePrice: BigDecimal? = viewState.variation?.salePrice,
        saleEndDate: Date? = viewState.variation?.saleEndDateGmt,
        saleStartDate: Date? = viewState.variation?.saleStartDateGmt,
        isSaleScheduled: Boolean? = null,
        stockStatus: ProductStockStatus? = null,
        backorderStatus: ProductBackorderStatus? = null,
        stockQuantity: Double? = null,
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
        weight: Float? = null,
        minAllowedQuantity: Int? = null,
        maxAllowedQuantity: Int? = null,
        groupOfQuantity: Int? = null
    ) {
        viewState.variation?.let { variation ->
            showVariation(
                variation.copy(
                    remoteProductId = remoteProductId ?: variation.remoteProductId,
                    remoteVariationId = remoteVariationId ?: variation.remoteVariationId,
                    sku = sku ?: variation.sku,
                    image = if (image != null) image.value else variation.image,
                    regularPrice = regularPrice,
                    salePrice = salePrice,
                    saleEndDateGmt = saleEndDate,
                    saleStartDateGmt = saleStartDate,
                    isSaleScheduled = isSaleScheduled ?: variation.isSaleScheduled,
                    stockStatus = stockStatus ?: variation.stockStatus,
                    backorderStatus = backorderStatus ?: variation.backorderStatus,
                    stockQuantity = stockQuantity ?: variation.stockQuantity,
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
                    weight = weight ?: variation.weight,
                    minAllowedQuantity = minAllowedQuantity ?: variation.minAllowedQuantity,
                    maxAllowedQuantity = maxAllowedQuantity ?: variation.maxAllowedQuantity,
                    groupOfQuantity = groupOfQuantity ?: variation.groupOfQuantity,
                )
            )
        }
    }

    fun onVariationSubscriptionChanged(
        price: BigDecimal? = (viewState.variation as? SubscriptionProductVariation)?.subscriptionDetails?.price,
        period: SubscriptionPeriod? = null,
        periodInterval: Int? = null,
        signUpFee: BigDecimal? = (viewState.variation as? SubscriptionProductVariation)?.subscriptionDetails?.signUpFee,
        length: Int? = null,
        trialLength: Int? = null,
        trialPeriod: SubscriptionPeriod? = null,
    ) {
        viewState.variation?.let { variation ->
            val subscription = (variation as? SubscriptionProductVariation)?.subscriptionDetails ?: return
            // The length ranges depend on the subscription period (days,weeks,months,years) and interval. If these
            // change we need to reset the length to "Never expire". This replicates web behavior
            val updatedLength = subscription.resetSubscriptionLengthIfThePeriodOrIntervalChanged(
                period,
                periodInterval,
                length
            )
            val updatedSubscription = subscription.copy(
                price = price ?: subscription.price,
                period = period ?: subscription.period,
                periodInterval = periodInterval ?: subscription.periodInterval,
                signUpFee = signUpFee,
                length = updatedLength,
                trialLength = trialLength ?: subscription.trialLength,
                trialPeriod = trialPeriod ?: subscription.trialPeriod
            )
            val updatedVariation = variation.copy(subscriptionDetails = updatedSubscription)
            showVariation(updatedVariation)
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
            val result = variationRepository.updateVariation(variation)
            if (!result.isError) {
                originalVariation = variation
                showVariation(variation)
                loadVariation(variation.remoteProductId, variation.remoteVariationId)
                triggerEvent(Event.ShowSnackbar(string.variation_detail_update_product_success))
            } else if (variation.image?.id == 0L && result.error.type == ProductErrorType.INVALID_VARIATION_IMAGE_ID) {
                triggerEvent(Event.ShowSnackbar(string.variation_detail_update_variation_image_error))
            } else if (result.error.canDisplayMessage) {
                triggerEvent(ShowUpdateVariationError(result.error.message))
            } else {
                triggerEvent(Event.ShowSnackbar(string.variation_detail_update_variation_error))
            }
        } else {
            triggerEvent(Event.ShowSnackbar(string.offline_error))
        }

        viewState = viewState.copy(isProgressDialogShown = false)
    }

    private fun deleteVariation() = launch {
        viewState = viewState.copy(isDeleteDialogShown = true)
        viewState.parentProduct?.remoteId?.let { productID ->
            viewState.variation?.let { variation ->
                variationRepository.deleteVariation(productID, variation.remoteVariationId)
                    .also { handleVariationDeletion(it, productID) }
            }
        }
    }

    private fun handleVariationDeletion(deleted: Boolean, productID: Long) {
        if (deleted) {
            triggerEvent(
                Event.ExitWithResult(
                    viewState.variation?.let { variation ->
                        DeletedVariationData(
                            productID,
                            variation.remoteVariationId
                        )
                    }
                )
            )
        } else if (deleted.not() && networkStatus.isConnected().not()) {
            triggerEvent(Event.ShowSnackbar(string.offline_error))
        }

        viewState = viewState.copy(isDeleteDialogShown = false)
    }

    private fun loadVariation(remoteProductId: Long, remoteVariationId: Long) {
        launch {
            val variationInDb = variationRepository.getVariation(remoteProductId, remoteVariationId)
            if (variationInDb != null) {
                originalVariation = variationInDb
                showVariation(variationInDb)
                fetchVariation(remoteProductId, remoteVariationId)
            } else {
                viewState = viewState.copy(isSkeletonShown = true)
                fetchVariation(remoteProductId, remoteVariationId)
            }
            viewState = viewState.copy(isSkeletonShown = false)

            // show the variation if we were able to get it, otherwise exit
            originalVariation?.let {
                showVariation(it)
            } ?: run {
                triggerEvent(Event.ShowSnackbar(string.variation_detail_fetch_variation_error))
                triggerEvent(Event.Exit)
            }
        }
    }

    private suspend fun fetchVariation(remoteProductId: Long, remoteVariationId: Long) {
        if (networkStatus.isConnected()) {
            variationRepository.fetchVariation(remoteProductId, remoteVariationId)
            originalVariation = variationRepository.getVariation(remoteProductId, remoteVariationId)
        } else {
            triggerEvent(Event.ShowSnackbar(string.offline_error))
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        mediaFileUploadHandler.cancelUpload(navArgs.remoteVariationId)
    }

    private fun updateCards(variation: ProductVariation) {
        launch {
            if (_variationDetailCards.value == null) {
                viewState = viewState.copy(isSkeletonShown = true)
            }
            if (variation is SubscriptionProductVariation && variation.subscriptionDetails == null) {
                // If this is a newly created subscription variation either from scratch or after changing the product
                // type, then we need to set the default subscription details
                showVariation(
                    variation = variation.copy(
                        subscriptionDetails = ProductHelper.getDefaultSubscriptionDetails()
                            .copy(price = variation.regularPrice)
                    )
                )
            } else {
                _variationDetailCards.value = cardBuilder.buildPropertyCards(
                    variation,
                    variation.sku,
                    viewState.parentProduct
                )
                viewState = viewState.copy(isSkeletonShown = false)
            }
        }
    }

    private fun showVariation(variation: ProductVariation) {
        viewState = viewState.copy(
            variation = variation,
            isDoneButtonVisible = variation != originalVariation
        )
    }

    private fun observeImageUploadEvents() {
        mediaFileUploadHandler.observeCurrentUploads(navArgs.remoteVariationId)
            .onEach {
                viewState = viewState.copy(
                    uploadingImageUri = it.firstOrNull()?.toUri(),
                    isDoneButtonEnabled = it.isEmpty()
                )
            }
            .launchIn(this)

        mediaFileUploadHandler.observeSuccessfulUploads(navArgs.remoteVariationId)
            .onEach { media ->
                viewState.variation?.let {
                    val variation = it.copy(image = media.toAppModel())
                    showVariation(variation)
                }
            }
            .launchIn(this)

        mediaFileUploadHandler.observeCurrentUploadErrors(navArgs.remoteVariationId)
            .onEach { errorList ->
                if (errorList.isEmpty()) {
                    triggerEvent(HideImageUploadErrorSnackbar)
                } else {
                    triggerEvent(
                        Event.ShowActionSnackbar(
                            message = resources.getMediaUploadErrorMessage(errorList.size),
                            actionText = resources.getString(string.details),
                        ) {
                            triggerEvent(ViewMediaUploadErrors(navArgs.remoteVariationId))
                        }
                    )
                }
            }
            .launchIn(this)
    }

    fun onSubscriptionExpirationChanged(selectedExpirationValue: Int) {
        onVariationSubscriptionChanged(length = selectedExpirationValue)
    }

    object HideImageUploadErrorSnackbar : Event()

    data class ShowUpdateVariationError(val message: String) : Event()

    @Parcelize
    data class VariationViewState(
        val variation: ProductVariation? = null,
        val isDoneButtonVisible: Boolean? = null,
        val isDoneButtonEnabled: Boolean? = null,
        val isSkeletonShown: Boolean? = null,
        val isProgressDialogShown: Boolean? = null,
        val isDeleteDialogShown: Boolean? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val gmtOffset: Float = 0f,
        val shippingClass: String? = null,
        val parentProduct: Product? = null,
        val uploadingImageUri: Uri? = null,
        val isConfirmingDeletion: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class DeletedVariationData(
        val productID: Long,
        val variationID: Long
    ) : Parcelable
}
