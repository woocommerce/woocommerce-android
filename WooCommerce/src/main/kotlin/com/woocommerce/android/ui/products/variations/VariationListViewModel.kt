package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_VARIATION_DETAIL_TAPPED
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.extensions.isSet
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class VariationListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val variationListRepository: VariationListRepository,
    private val productRepository: ProductDetailRepository,
    private val networkStatus: NetworkStatus,
    private val currencyFormatter: CurrencyFormatter
) : ScopedViewModel(savedState, dispatchers) {
    private var remoteProductId = 0L

    private val _variationList = MutableLiveData<List<ProductVariation>>()
    val variationList: LiveData<List<ProductVariation>> = Transformations.map(_variationList) { variations ->
        val anyWithoutPrice = variations.any { it.isVisible && it.regularPrice.isNotSet() && it.salePrice.isNotSet() }
        viewState = viewState.copy(isWarningVisible = anyWithoutPrice)
        variations
    }

    val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private var loadingJob: Job? = null

    fun start(remoteProductId: Long, createNewVariation: Boolean = false) {
        productRepository.getProduct(remoteProductId).let {
            viewState = viewState.copy(parentProduct = it)
            when (createNewVariation) {
                true -> createEmptyVariation(it)
                else -> loadVariations(remoteProductId)
            }
        }
    }

    fun refreshVariations(remoteProductId: Long) {
        viewState = viewState.copy(isRefreshing = true)
        loadVariations(remoteProductId)
    }

    fun onLoadMoreRequested(remoteProductId: Long) {
        loadVariations(remoteProductId, loadMore = true)
    }

    override fun onCleared() {
        super.onCleared()
        variationListRepository.onCleanup()
    }

    fun onItemClick(variation: ProductVariation) {
        AnalyticsTracker.track(PRODUCT_VARIATION_VIEW_VARIATION_DETAIL_TAPPED)
        triggerEvent(ShowVariationDetail(variation))
    }

    fun onAddEditAttributesClick() {
        // TODO: tracks event
        triggerEvent(ShowAttributeList)
    }

    fun onCreateFirstVariationRequested() {
        // TODO: tracks event
        triggerEvent(ShowAddAttributeView)
    }

    fun onCreateEmptyVariationRequested() {
        // TODO: tracks event
        viewState = viewState.copy(isProgressDialogShown = true)
        viewState.parentProduct?.let {
            createEmptyVariation(
                product = it,
                withLoadingDialog = true
            )
        }
    }

    fun onVariationDeleted(productIdOfDeletedVariation: Long) {
        viewState = viewState.copy(isSkeletonShown = true)
        refreshParentProduct(productIdOfDeletedVariation)
    }

    fun onExit() {
        triggerEvent(ExitWithResult(VariationListData(viewState.parentProduct?.numVariations)))
    }

    private fun createEmptyVariation(product: Product?, withLoadingDialog: Boolean = false) = launch {
        viewState = viewState.copy(
            isSkeletonShown = withLoadingDialog.not(),
            isEmptyViewVisible = false
        )

        product?.apply { variationListRepository.createEmptyVariation(this) }
            ?.also { refreshParentProduct(product.remoteId) }
    }


    private fun refreshParentProduct(productID: Long) = launch {
        productRepository.fetchProduct(productID)
            ?.let { viewState.copy(parentProduct = it) }
            ?.let { viewState = it }
            ?.also { refreshVariations(productID) }
            ?: viewState.copy(
                isSkeletonShown = false,
                isProgressDialogShown = false
            ).let { viewState = it }
    }

    private fun loadVariations(remoteProductId: Long, loadMore: Boolean = false) {
        if (loadMore && !variationListRepository.canLoadMoreProductVariations) {
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
                val variationsInDb = variationListRepository.getProductVariationList(remoteProductId)
                if (variationsInDb.isNullOrEmpty()) {
                    viewState = viewState.copy(isSkeletonShown = true)
                } else {
                    _variationList.value = combineData(variationsInDb)
                }
            }

            fetchVariations(remoteProductId, loadMore = loadMore)
        }
    }

    fun isEmpty() = _variationList.value?.isEmpty() ?: true

    private suspend fun fetchVariations(remoteProductId: Long, loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            val fetchedVariations = variationListRepository.fetchProductVariations(remoteProductId, loadMore)
            if (fetchedVariations.isNullOrEmpty()) {
                if (!loadMore) {
                    _variationList.value = emptyList()
                    viewState = viewState.copy(isEmptyViewVisible = true)
                }
            } else {
                _variationList.value = combineData(fetchedVariations)
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
        viewState = viewState.copy(
            isSkeletonShown = false,
            isRefreshing = false,
            isLoadingMore = false,
            isProgressDialogShown = false
        )
    }

    private fun combineData(variations: List<ProductVariation>): List<ProductVariation> {
        val currencyCode = variationListRepository.getCurrencyCode()
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

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val isWarningVisible: Boolean? = null,
        val isProgressDialogShown: Boolean? = null,
        val parentProduct: Product? = null
    ) : Parcelable

    @Parcelize
    data class VariationListData(
        val currentVariationAmount: Int? = null
    ) : Parcelable

    data class ShowVariationDetail(val variation: ProductVariation) : Event()
    object ShowAddAttributeView : Event()
    object ShowAttributeList : Event()

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<VariationListViewModel>
}
