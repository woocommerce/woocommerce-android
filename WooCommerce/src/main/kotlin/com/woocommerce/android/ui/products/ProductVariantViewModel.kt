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
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewVariationImage
import com.woocommerce.android.ui.products.ProductNavigationTarget.ViewVariationImageChooser
import com.woocommerce.android.ui.products.ProductVariantViewModel.VariationExitEvent.ExitVariation
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.variations.ProductVariantCardBuilder
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal

@OpenClassOnDebug
class ProductVariantViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    private val resources: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }

    private val navArgs: ProductVariantFragmentArgs by savedState.navArgs()

    private val variant: ProductVariant
        get() = navArgs.variant

    /**
     * Fetch product related properties (currency, product dimensions) for the site since we use this
     * variable in many different places in the product detail view such as pricing, shipping.
     */
    private final val parameters: Parameters by lazy {
        val params = savedState.get<Parameters>(KEY_PRODUCT_PARAMETERS) ?: loadParameters()
        savedState[KEY_PRODUCT_PARAMETERS] = params
        params
    }

    // view state for the product detail screen
    final val variantViewStateData = LiveDataDelegate(savedState, VariantViewState()) { old, new ->
        if (old?.variant != new.variant) {
            updateCards()
        }
    }
    private var viewState by variantViewStateData

    // view state for the product images screen
    final val productImagesViewStateData = LiveDataDelegate(savedState, ProductImagesViewState())
    private var productImagesViewState by productImagesViewStateData

    private val _productDetailCards = MutableLiveData<List<ProductPropertyCard>>()
    val productDetailCards: LiveData<List<ProductPropertyCard>> = _productDetailCards

    private val cardBuilder by lazy {
        ProductVariantCardBuilder(this, resources, currencyFormatter, parameters)
    }

    private val variationId: Long
        get() = viewState.variant?.remoteVariationId ?: 0L

    init {
        EventBus.getDefault().register(this)

        displayVariation()
    }

    /**
     * Called when an existing image is selected in Product detail screen
     */
    fun onImageGalleryClicked(image: Product.Image) {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.variant?.let {
            triggerEvent(ViewVariationImage(it, image))
        }
    }

    /**
     * Called when the add image icon is clicked in Product detail screen
     */
    fun onAddImageClicked() {
        AnalyticsTracker.track(PRODUCT_DETAIL_IMAGE_TAPPED)
        viewState.variant?.let {
            triggerEvent(ViewVariationImageChooser(it.remoteVariationId))
        }
    }

    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product detail screen
     */
    fun onEditVariationCardClicked(target: ProductNavigationTarget, stat: Stat? = null) {
        stat?.let { AnalyticsTracker.track(it) }
        triggerEvent(target)
    }

    /**
     * Method called when back button is clicked.
     *
     * Each product screen has it's own [VariationExitEvent]
     * Based on the exit event, the logic is to check if the discard dialog should be displayed.
     *
     * For all product sub-detail screens such as [ProductInventoryFragment] and [ProductPricingFragment],
     * the discard dialog should only be displayed if there are currently any changes made to the fields in the screen.
     *
     * For the product detail screen, the discard dialog should only be displayed if there are changes to the
     * [Product] model locally, that still need to be saved to the backend.
     */
    fun onBackButtonClicked(event: VariationExitEvent): Boolean {
        return if (event is ExitVariation &&
            ProductImagesService.isUploadingForProduct(variant.remoteVariationId)) {
            // images can't be assigned to the product until they finish uploading so ask whether to discard images.
            triggerEvent(ShowDiscardDialog(
                    messageId = string.discard_images_message,
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(ExitProduct)
                    }
            ))
            false
        } else {
            true
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun updateCards() {
        viewState.variant?.let {
            launch(dispatchers.computation) {
                val cards = cardBuilder.buildPropertyCards(it)
                withContext(dispatchers.main) {
                    _productDetailCards.value = cards
                }
            }
        }
    }

    private fun displayVariation() {
        viewState = viewState.copy(variant = variant)
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

    fun isUploadingImages(remoteProductId: Long) = ProductImagesService.isUploadingForProduct(remoteProductId)

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
    private fun checkImageUploads(variationId: Long) {
        val isUploadingImages = ProductImagesService.isUploadingForProduct(variationId)
        if (isUploadingImages != productImagesViewState.isUploadingImages) {
            val uris = ProductImagesService.getUploadingImageUrisForProduct(variationId)
            viewState = viewState.copy(uploadingImageUris = uris)
            productImagesViewState = productImagesViewState.copy(isUploadingImages = isUploadingImages)
        }
    }

    private fun formatCurrency(amount: BigDecimal?, currencyCode: String?): String {
        return currencyCode?.let {
            currencyFormatter.formatCurrency(amount ?: BigDecimal.ZERO, it)
        } ?: amount.toString()
    }

    /**
     * Sealed class that handles the back navigation for the product detail screens while providing a common
     * interface for managing them as a single type. Currently used in all the product sub detail screens when
     * back is clicked or DONE is clicked.
     *
     * Add a new class here for each new product sub detail screen to handle back navigation.
     */
    sealed class VariationExitEvent(val shouldShowDiscardDialog: Boolean = true) : Event() {
        class ExitVariation(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
        class ExitInventory(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
        class ExitPricing(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
        class ExitShipping(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
        class ExitImages(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
        class ExitExternalLink(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
        class ExitSettings(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
    }

    data class LaunchUrlInChromeTab(val url: String) : Event()

    @Parcelize
    data class Parameters(
        val currencyCode: String?,
        val weightUnit: String?,
        val dimensionUnit: String?,
        val gmtOffset: Float
    ) : Parcelable

    @Parcelize
    data class VariantViewState(
        val variant: ProductVariant? = null,
        val isSkeletonShown: Boolean? = null,
        val uploadingImageUris: List<Uri>? = null,
        val isProgressDialogShown: Boolean? = null,
        val weightWithUnits: String? = null,
        val sizeWithUnits: String? = null,
        val priceWithCurrency: String? = null,
        val salePriceWithCurrency: String? = null,
        val regularPriceWithCurrency: String? = null,
        val gmtOffset: Float = 0f
    ) : Parcelable {
        val isOnSale: Boolean
            get() = salePriceWithCurrency != null
    }

    @Parcelize
    data class ProductImagesViewState(
        val isUploadingImages: Boolean = false
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductVariantViewModel>
}
