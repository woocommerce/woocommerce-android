package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductListViewModel.ProductListEvent.ScrollToTop
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus

@OpenClassOnDebug
class ProductCategoriesListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val networkStatus: NetworkStatus) : ScopedViewModel(savedState, dispatchers) {
    private val _productCategories = MutableLiveData<List<ProductCategory>>()
    val productCategories: LiveData<List<ProductCategory>> = _productCategories

    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateLiveData

    private var loadJob: Job? = null

    init {
        if (_productCategories.value == null) {
            loadProductCategories()
        }
        viewState = viewState.copy()
    }

    override fun onCleared() {
        super.onCleared()
        productCategoriesRepository.onCleanup()
    }

    private fun isLoading() = viewState.isLoading == true

    private fun isRefreshing() = viewState.isRefreshing == true

    fun onLoadMoreRequested() {
        loadProductCategories(loadMore = true)
    }

    final fun reloadProductsFromDb() {
        _productCategories.value = productCategoriesRepository.getProductCategoriesList()
    }

    final fun loadProductCategories(loadMore: Boolean = false, scrollToTop: Boolean = false) {
        if (isLoading()) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading product categories")
            return
        }

        if (loadMore && !productCategoriesRepository.canLoadMoreProductCategories) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more product categories")
            return
        }

        // if a fetch is already active, wait for it to finish before we start another one
        waitForExistingLoad()

        loadJob = launch {
            val showSkeleton: Boolean
            if (loadMore) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the products from the db and show them immediately
                val productsInDb = productCategoriesRepository.getProductCategoriesList()
                if (productsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    _productCategories.value = productsInDb
                    showSkeleton = !isRefreshing()
                }
            }
            viewState = viewState.copy(
                    isLoading = true,
                    isLoadingMore = loadMore,
                    isSkeletonShown = showSkeleton,
                    isEmptyViewVisible = false
            )
            fetchProductCategories(loadMore = loadMore, scrollToTop = scrollToTop)
        }
    }

    /**
     * If products are already being fetched, wait for the existing job to finish
     */
    private fun waitForExistingLoad() {
        if (loadJob?.isActive == true) {
            launch {
                try {
                    loadJob?.join()
                } catch (e: CancellationException) {
                    WooLog.d(WooLog.T.PRODUCTS, "CancellationException while waiting for existing fetch")
                }
            }
        }
    }

    fun refreshProductCategories(scrollToTop: Boolean = false) {
        viewState = viewState.copy(isRefreshing = true)
        loadProductCategories(scrollToTop = scrollToTop)
    }

    private suspend fun fetchProductCategories(
        loadMore: Boolean = false,
        scrollToTop: Boolean = false
    ) {
        if (networkStatus.isConnected()) {
            _productCategories.value = productCategoriesRepository.fetchProductCategories(loadMore)

            viewState = viewState.copy(
                    isLoading = true,
                    canLoadMore = productCategoriesRepository.canLoadMoreProductCategories,
                    isEmptyViewVisible = _productCategories.value?.isEmpty() == true)
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }

        viewState = viewState.copy(
                isSkeletonShown = false,
                isLoading = false,
                isLoadingMore = false,
                isRefreshing = false
        )

        if (scrollToTop) {
            triggerEvent(ScrollToTop)
        }
    }

    @Parcelize
    data class ViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    sealed class ProductCategoriesListEvent : Event() {
        object ScrollToTop : ProductCategoriesListEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductCategoriesListViewModel>
}
