package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIANTS_BULK_UPDATE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_VARIATION_VIEW_VARIATION_DETAIL_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.track
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationListViewModel.ViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * [Product] and [ProductVariation] are two models fetched in separate endpoints,
 * but to allow us to create and delete variations correctly, consistency between
 * site and app data around both models is necessary to handle the correct flow
 * to the user.
 *
 * This happens because when any change happens at the variation list
 * from a product, the [Product.numVariations] is also updated by the site,
 * causing the need to fetch the product data after that, allowing
 * us to be able to tell at any Product view if we shall make available
 * the first variation creation flow or just allow the user the access the variation
 * list view directly without affecting the ability of the Fragment to manage drafts.
 *
 * With that said, when we update the Variation list, we should also update the
 * [ViewState.parentProduct] so the correct information is returned [onExit]
 */

private const val BULK_UPDATE_VARIATIONS_LIMIT = 100

@HiltViewModel
class VariationListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val variationRepository: VariationRepository,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter,
    private val dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState) {
    private var remoteProductId = 0L

    private val _variationList = MutableLiveData<List<ProductVariation>>()
    val variationList: LiveData<List<ProductVariation>> = Transformations.map(_variationList) { variations ->
        val isEmpty = viewState.parentProduct?.variationEnabledAttributes?.isEmpty() == true
        variations.apply {
            viewState = viewState.copy(
                isEmptyViewVisible = isEmpty,
                isWarningVisible = !isEmpty && any { variation ->
                    variation.isVisible &&
                        variation.regularPrice.isNotSet() &&
                        variation.salePrice.isNotSet()
                }
            )
        }
    }

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private var loadingJob: Job? = null

    val isEmpty
        get() = _variationList.value?.isEmpty() ?: true

    fun start(remoteProductId: Long) {
        productRepository.getProduct(remoteProductId)?.let {
            viewState = viewState.copy(parentProduct = it)
            handleVariationLoading(remoteProductId)
        }
    }

    fun onLoadMoreRequested(remoteProductId: Long) {
        loadVariations(remoteProductId, loadMore = true)
    }

    fun onBulkUpdateClicked() {
        track(PRODUCT_VARIANTS_BULK_UPDATE_TAPPED)

        val variationsCount = viewState.parentProduct?.numVariations ?: return

        if (variationsCount > BULK_UPDATE_VARIATIONS_LIMIT) {
            triggerEvent(ShowBulkUpdateLimitExceededWarning)
        } else {
            viewState = viewState.copy(isBulkUpdateProgressDialogShown = true)
            viewModelScope.launch(dispatchers.io) {
                val variations = getAllVariations(remoteProductId)
                withContext(dispatchers.main) {
                    triggerEvent(ShowBulkUpdateAttrPicker(variations))
                    viewState = viewState.copy(isBulkUpdateProgressDialogShown = false)
                }
            }
        }
    }

    fun onItemClick(variation: ProductVariation) {
        track(PRODUCT_VARIATION_VIEW_VARIATION_DETAIL_TAPPED)
        triggerEvent(ShowVariationDetail(variation))
    }

    fun onCreateEmptyVariationClick() {
        trackWithProductId(AnalyticsEvent.PRODUCT_VARIATION_ADD_MORE_TAPPED)
        handleVariationCreation()
    }

    fun onCreateFirstVariationRequested() {
        trackWithProductId(AnalyticsEvent.PRODUCT_VARIATION_ADD_FIRST_TAPPED)
        viewState.parentProduct
            ?.variationEnabledAttributes
            ?.takeIf { it.isNotEmpty() }
            ?.let { handleVariationCreation(openVariationDetails = false) }
            ?: triggerEvent(ShowAddAttributeView)
    }

    fun onVariationDeleted(productID: Long, variationID: Long) = launch {
        variationList.value?.toMutableList()?.apply {
            find { it.remoteVariationId == variationID }
                ?.let { remove(it) }
        }?.toList().let { _variationList.value = it }

        productRepository.fetchProductOrLoadFromCache(productID)
            ?.let { viewState = viewState.copy(parentProduct = it) }
    }

    fun onExit() {
        triggerEvent(ExitWithResult(VariationListData(viewState.parentProduct?.numVariations)))
    }

    fun refreshVariations(remoteProductId: Long) {
        viewState = viewState.copy(isRefreshing = true)
        loadVariations(remoteProductId)
    }

    private fun handleVariationLoading(productID: Long) {
        viewState = viewState.copy(isSkeletonShown = true)
        loadVariations(productID)
    }

    private fun handleVariationCreation(
        openVariationDetails: Boolean = true
    ) = launch {
        viewState = viewState.copy(
            isProgressDialogShown = true,
            isEmptyViewVisible = false
        )

        viewState.parentProduct
            ?.createVariation()
            .takeIf { openVariationDetails }
            ?.let {
                triggerEvent(ShowSnackbar(string.variation_created_title))
                triggerEvent(ShowVariationDetail(it))
            }.also { viewState = viewState.copy(isProgressDialogShown = false) }
    }

    private suspend fun Product.createVariation() =
        variationRepository.createEmptyVariation(this)
            ?.copy(remoteProductId = remoteId)
            ?.apply { syncProductToVariations(remoteId) }

    private suspend fun syncProductToVariations(productID: Long) {
        loadVariations(productID, withSkeletonView = false)
        productRepository.fetchProductOrLoadFromCache(productID)
            ?.let { viewState = viewState.copy(parentProduct = it) }
    }

    private fun loadVariations(
        remoteProductId: Long,
        loadMore: Boolean = false,
        withSkeletonView: Boolean = true
    ) {
        if (loadMore && !variationRepository.canLoadMoreProductVariations) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more product variations")
            return
        }

        if (loadingJob?.isActive == true) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading product variations")
            return
        }

        this.remoteProductId = remoteProductId

        loadingJob = launch {
            viewState = viewState.copy(isLoadingMore = loadMore)
            if (!loadMore) {
                // if this is the initial load, first get the product variations from the db and if there are any show
                // them immediately, otherwise make sure the skeleton shows
                val variationsInDb = variationRepository.getProductVariationList(remoteProductId)
                if (variationsInDb.isNullOrEmpty()) {
                    viewState = viewState.copy(isSkeletonShown = withSkeletonView)
                } else {
                    _variationList.value = combineData(variationsInDb)
                }
            }

            fetchVariations(remoteProductId, loadMore = loadMore)
        }
    }

    private suspend fun fetchVariations(remoteProductId: Long, loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            val fetchedVariations = variationRepository.fetchProductVariations(remoteProductId, loadMore)
            if (fetchedVariations.isNullOrEmpty()) {
                if (!loadMore) {
                    _variationList.value = emptyList()
                    viewState = viewState.copy(isEmptyViewVisible = true, isVariationsOptionsMenuEnabled = false)
                }
            } else {
                _variationList.value = combineData(fetchedVariations)
                viewState = viewState.copy(isVariationsOptionsMenuEnabled = true)
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
        viewState = viewState.copy(
            isSkeletonShown = false,
            isRefreshing = false,
            isLoadingMore = false
        )
    }

    private suspend fun getAllVariations(remoteProductId: Long): Collection<ProductVariation> {
        while (variationRepository.canLoadMoreProductVariations) {
            variationRepository.fetchProductVariations(remoteProductId, true)
        }
        return variationRepository.getProductVariationList(remoteProductId)
    }

    private fun combineData(variations: List<ProductVariation>): List<ProductVariation> {
        val currencyCode = variationRepository.getCurrencyCode()
        variations.map { variation ->
            if (variation.isSaleInEffect) {
                variation.priceWithCurrency = currencyCode?.let {
                    currencyFormatter.formatCurrency(variation.salePrice!!, it)
                } ?: variation.salePrice!!.toString()
            } else if (variation.regularPrice.isSet()) {
                variation.priceWithCurrency = currencyCode?.let {
                    currencyFormatter.formatCurrency(variation.regularPrice!!, it)
                } ?: variation.regularPrice!!.toString()
            }
        }
        return variations
    }

    private fun trackWithProductId(event: AnalyticsEvent) {
        viewState.parentProduct?.let { track(event, mapOf(KEY_PRODUCT_ID to it.remoteId)) }
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val isWarningVisible: Boolean? = null,
        val isProgressDialogShown: Boolean? = null,
        val parentProduct: Product? = null,
        val isVariationsOptionsMenuEnabled: Boolean = false,
        val isBulkUpdateProgressDialogShown: Boolean = false,
    ) : Parcelable

    @Parcelize
    data class VariationListData(
        val currentVariationAmount: Int? = null
    ) : Parcelable

    data class ShowVariationDetail(val variation: ProductVariation) : Event()

    /**
     * Represents event responsible for displaying [VariationsBulkUpdateAttrPickerDialog].
     */
    data class ShowBulkUpdateAttrPicker(val variationsToUpdate: Collection<ProductVariation>) : Event()
    object ShowAddAttributeView : Event()

    /**
     * Informs about exceeded limit of 100 variations bulk update.
     */
    object ShowBulkUpdateLimitExceededWarning : Event()
}
