package com.woocommerce.android.ui.products

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
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariant
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductNavigationTarget.ExitProduct
import com.woocommerce.android.ui.products.ProductVariantViewModel.VariationExitEvent.ExitVariation
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDiscardDialog
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.store.WooCommerceStore

class ProductVariantViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val productRepository: ProductDetailRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
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
     * variable in many different places in the product variant view such as pricing, shipping.
     */
    private final val parameters: SiteParameters by lazy {
        val params = savedState.get<SiteParameters>(KEY_PRODUCT_PARAMETERS) ?: loadParameters()
        savedState[KEY_PRODUCT_PARAMETERS] = params
        params
    }

    // view state for the variant detail screen
    final val variantViewStateData = LiveDataDelegate(savedState, VariantViewState()) { old, new ->
        if (old?.variant != new.variant) {
            updateCards()
        }
    }
    private var viewState by variantViewStateData

    private val _variantDetailCards = MutableLiveData<List<ProductPropertyCard>>()
    val variantDetailCards: LiveData<List<ProductPropertyCard>> = _variantDetailCards

    private val cardBuilder by lazy {
        ProductVariantCardBuilder(this, resources, currencyFormatter, parameters)
    }

    init {
        displayVariation()
    }

    fun getShippingClassByRemoteShippingClassId(remoteShippingClassId: Long) =
        productRepository.getProductShippingClassByRemoteId(remoteShippingClassId)?.name
            ?: viewState.shippingClass ?: ""

    private suspend fun loadShippingClassDependencies() {
        // Fetch current site's shipping class only if a shipping class is assigned to the product and if
        // the shipping class is not available in the local db
        val shippingClassId = variant.shippingClassId
        if (shippingClassId != 0L && productRepository.getProductShippingClassByRemoteId(shippingClassId) == null) {
            productRepository.fetchProductShippingClassById(shippingClassId)
        }
    }

    // TODO: This will be used in edit mode
    /**
     * Called when the any of the editable sections (such as pricing, shipping, inventory)
     * is selected in Product variant screen
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
     * For the product variant screen, the discard dialog should only be displayed if there are changes to the
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

    fun onVariantImageClicked() {
        variant.image?.let {
            AnalyticsTracker.track(PRODUCT_VARIATION_IMAGE_TAPPED)
            triggerEvent(ShowVariantImage(it))
        }
    }

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
        EventBus.getDefault().unregister(this)
    }

    private fun updateCards() {
        viewState.variant?.let {
            launch {
                if (_variantDetailCards.value == null) {
                    viewState = viewState.copy(isSkeletonShown = true)
                }
                val cards = withContext(dispatchers.io) {
                    loadShippingClassDependencies()
                    cardBuilder.buildPropertyCards(it)
                }
                _variantDetailCards.value = cards
                viewState = viewState.copy(isSkeletonShown = false)
            }
        }
    }

    private fun displayVariation() {
        viewState = viewState.copy(variant = variant)
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

    /**
     * Sealed class that handles the back navigation for the product variant screens while providing a common
     * interface for managing them as a single type. Currently used in all the product sub detail screens when
     * back is clicked or DONE is clicked.
     *
     * Add a new class here for each new product sub detail screen to handle back navigation.
     */
    sealed class VariationExitEvent(val shouldShowDiscardDialog: Boolean = true) : Event() {
        class ExitVariation(shouldShowDiscardDialog: Boolean = true) : VariationExitEvent(shouldShowDiscardDialog)
    }

    data class ShowVariantImage(val image: Product.Image) : Event()

    @Parcelize
    data class VariantViewState(
        val variant: ProductVariant? = null,
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
    interface Factory : ViewModelAssistedFactory<ProductVariantViewModel>
}
