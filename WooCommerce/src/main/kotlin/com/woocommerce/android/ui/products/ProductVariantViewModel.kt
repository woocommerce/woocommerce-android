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
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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

class ProductVariantViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val selectedSite: SelectedSite,
    private val variationRepository: VariationDetailRepository,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val resources: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_VARIATION_PARAMETERS = "key_variation_parameters"
    }

    private val navArgs: ProductVariantFragmentArgs by savedState.navArgs()
    private var originalVariation: ProductVariant = navArgs.variant

    private val parameters: SiteParameters by lazy {
        val params = savedState.get<SiteParameters>(KEY_VARIATION_PARAMETERS) ?: loadParameters()
        savedState[KEY_VARIATION_PARAMETERS] = params
        params
    }

    // view state for the variant detail screen
    val variantViewStateData = LiveDataDelegate(savedState, VariantViewState(originalVariation)) { old, new ->
        if (old?.variation != new.variation) {
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
        showVariation(originalVariation.copy())
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

    fun onVariantImageClicked() {
        viewState.variation.image?.let {
            AnalyticsTracker.track(PRODUCT_VARIATION_IMAGE_TAPPED)
            triggerEvent(ShowVariantImage(it))
        }
    }

    fun onVariantVisibilityChanged(isVisible: Boolean) {
        showVariation(viewState.variation.copy(isVisible = isVisible))
    }

    fun onUpdateButtonClicked() {
        viewState = viewState.copy(isProgressDialogShown =  true)
        launch {
            updateVariation(viewState.variation)
        }
    }

    private suspend fun updateVariation(variation: ProductVariant) {
        if (networkStatus.isConnected()) {
            if (variationRepository.updateVariation(variation)) {
                loadVariation(variation.remoteProductId, variation.remoteVariationId)
            } else {
                triggerEvent(ShowSnackbar(string.product_detail_update_product_error))
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

    private fun updateCards() {
        viewState.variation.let {
            launch {
                if (_variantDetailCards.value == null) {
                    viewState = viewState.copy(isSkeletonShown = true)
                }
                val cards = withContext(dispatchers.io) {
                    cardBuilder.buildPropertyCards(it)
                }
                _variantDetailCards.value = cards
                viewState = viewState.copy(isSkeletonShown = false)
            }
        }
    }

    private fun showVariation(variation: ProductVariant) {
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

    data class ShowVariantImage(val image: Product.Image) : Event()

    @Parcelize
    data class VariantViewState(
        val variation: ProductVariant,
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
    interface Factory : ViewModelAssistedFactory<ProductVariantViewModel>
}
