package com.woocommerce.android.ui.products.variations

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_VIEW_VARIATION_DETAIL_TAPPED
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.isNotSet
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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

    val viewStateLiveData = LiveDataDelegate(savedState,
        ViewState()
    )
    private var viewState by viewStateLiveData

    private var loadingJob: Job? = null

    fun start(remoteProductId: Long) {
        loadVariations(remoteProductId)
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

    private suspend fun fetchVariations(remoteProductId: Long, loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            val fetchedVariations = variationListRepository.fetchProductVariations(remoteProductId, loadMore)
            if (fetchedVariations.isNullOrEmpty()) {
                if (!loadMore) {
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
                isLoadingMore = false
        )
    }

    private fun combineData(variations: List<ProductVariation>): List<ProductVariation> {
        val currencyCode = variationListRepository.getCurrencyCode()
        variations.map { variation ->
            variation.regularPrice?.let { price ->
                variation.priceWithCurrency = currencyCode?.let {
                    currencyFormatter.formatCurrency(price, it)
                } ?: price.toString()
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
        val isWarningVisible: Boolean? = null
    ) : Parcelable

    data class ShowVariationDetail(val variation: ProductVariation) : Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<VariationListViewModel>
}
