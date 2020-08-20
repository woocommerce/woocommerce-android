package com.woocommerce.android.ui.products.variations

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_IMAGE_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductBackorderStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.variations.VariationNavigationTarget.ShowImage
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.Optional
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
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
    private var originalVariation: ProductVariation = navArgs.variation
        set(value) {
            // Update the cards (and the original SKU, so that that the "SKU error taken" is not shown unnecessarily
            if (field != value) {
                field = value
                updateCards(viewState.variation)
            }
        }

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_VARIATION_PARAMETERS, savedState)
    }

    // view state for the variation detail screen
    val variationViewStateData = LiveDataDelegate(savedState, VariationViewState(originalVariation)) { old, new ->
        if (old?.variation != new.variation) {
            updateCards(new.variation)
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
        showVariation(originalVariation.copy())
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
            ProductImagesService.isUploadingForProduct(viewState.variation.remoteVariationId) -> {
                // images can't be assigned to the product until they finish uploading so ask whether to discard images.
                triggerEvent(ShowDiscardDialog(
                    messageId = string.discard_images_message,
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(Exit)
                    }
                ))
            }
            viewState.variation != originalVariation -> {
                triggerEvent(ShowDiscardDialog(
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

    fun onImageClicked() {
        viewState.variation.image?.let {
            AnalyticsTracker.track(PRODUCT_VARIATION_IMAGE_TAPPED)
            triggerEvent(ShowImage(it))
        }
    }

    fun onVariationVisibilitySwitchChanged(isVisible: Boolean) {
        AnalyticsTracker.track(PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED)
        onVariationChanged(isVisible = isVisible)
    }

    fun onVariationChanged(
        remoteProductId: Long? = null,
        remoteVariationId: Long? = null,
        sku: String? = null,
        image: Product.Image? = null,
        regularPrice: BigDecimal? = null,
        salePrice: BigDecimal? = null,
        saleEndDate: Optional<Date>? = null,
        saleStartDate: Optional<Date>? = null,
        isSaleScheduled: Boolean? = null,
        stockStatus: ProductStockStatus? = null,
        backorderStatus: ProductBackorderStatus? = null,
        stockQuantity: Int? = null,
        optionName: String? = null,
        isPurchasable: Boolean? = null,
        isVirtual: Boolean? = null,
        isDownloadable: Boolean? = null,
        description: String? = null,
        isVisible: Boolean? = null,
        isStockManaged: Boolean? = null,
        shippingClass: String? = null,
        shippingClassId: Long? = null,
        length: Float? = null,
        width: Float? = null,
        height: Float? = null,
        weight: Float? = null
    ) {
        showVariation(viewState.variation.copy(
            remoteProductId = remoteProductId ?: viewState.variation.remoteProductId,
            remoteVariationId = remoteVariationId ?: viewState.variation.remoteVariationId,
            sku = sku ?: viewState.variation.sku,
            image = image ?: viewState.variation.image,
            regularPrice = regularPrice ?: viewState.variation.regularPrice,
            salePrice = salePrice ?: viewState.variation.salePrice,
            saleEndDateGmt = if (saleEndDate != null) saleEndDate.value else viewState.variation.saleEndDateGmt,
            saleStartDateGmt = if (saleStartDate != null) saleStartDate.value else viewState.variation.saleStartDateGmt,
            isSaleScheduled = isSaleScheduled ?: viewState.variation.isSaleScheduled,
            stockStatus = stockStatus ?: viewState.variation.stockStatus,
            backorderStatus = backorderStatus ?: viewState.variation.backorderStatus,
            stockQuantity = stockQuantity ?: viewState.variation.stockQuantity,
            optionName = optionName ?: viewState.variation.optionName,
            isPurchasable = isPurchasable ?: viewState.variation.isPurchasable,
            isVirtual = isVirtual ?: viewState.variation.isVirtual,
            isDownloadable = isDownloadable ?: viewState.variation.isDownloadable,
            description = description ?: viewState.variation.description,
            isVisible = isVisible ?: viewState.variation.isVisible,
            isStockManaged = isStockManaged ?: viewState.variation.isStockManaged,
            shippingClass = shippingClass ?: viewState.variation.shippingClass,
            shippingClassId = shippingClassId ?: viewState.variation.shippingClassId,
            length = length ?: viewState.variation.length,
            width = width ?: viewState.variation.width,
            height = height ?: viewState.variation.height,
            weight = weight ?: viewState.variation.weight
        ))
    }

    fun onUpdateButtonClicked() {
        viewState = viewState.copy(isProgressDialogShown = true)
        launch {
            updateVariation(viewState.variation)
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
                triggerEvent(ShowSnackbar(string.variation_detail_update_variation_error))
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
            showVariation(originalVariation)
        }
    }

    private suspend fun fetchVariation(remoteProductId: Long, remoteVariationId: Long) {
        if (networkStatus.isConnected()) {
            val fetchedVariation = variationRepository.fetchVariation(remoteProductId, remoteVariationId)
            if (fetchedVariation == null) {
                triggerEvent(ShowSnackbar(string.variation_detail_fetch_variation_error))
            } else {
                originalVariation = fetchedVariation
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        variationRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun updateCards(variation: ProductVariation) {
        launch {
            if (_variationDetailCards.value == null) {
                viewState = viewState.copy(isSkeletonShown = true)
            }
            _variationDetailCards.value = cardBuilder.buildPropertyCards(variation, originalVariation.sku)
            viewState = viewState.copy(isSkeletonShown = false)
        }
    }

    private fun showVariation(variation: ProductVariation) {
        viewState = viewState.copy(
            variation = variation,
            isDoneButtonVisible = variation != originalVariation
        )
    }

    /**
     * Fetch the shipping class name of a product based on the remote shipping class id
     */
    fun getShippingClassByRemoteShippingClassId(remoteShippingClassId: Long) =
        productRepository.getProductShippingClassByRemoteId(remoteShippingClassId)?.name
            ?: viewState.variation.shippingClass ?: ""

    @Parcelize
    data class VariationViewState(
        val variation: ProductVariation,
        val isDoneButtonVisible: Boolean? = null,
        val isSkeletonShown: Boolean? = null,
        val isProgressDialogShown: Boolean? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val gmtOffset: Float = 0f,
        val shippingClass: String? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<VariationDetailViewModel>
}
