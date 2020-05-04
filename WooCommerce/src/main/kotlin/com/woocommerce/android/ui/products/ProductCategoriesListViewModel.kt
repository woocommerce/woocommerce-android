package com.woocommerce.android.ui.products

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.tools.NetworkStatus
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

@OpenClassOnDebug
class ProductCategoriesListViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    private val _productCategories = MutableLiveData<List<ProductCategory>>()

    final val viewStateLiveData = LiveDataDelegate(savedState, ProductCategoriesViewState())
    private var viewState by viewStateLiveData

    // The job used to load categories
    // this can be used to cancel the fetch, when refreshing
    private var loadCategoriesJob: Job? = null

    init {
        // Loads the categories into state on viewmodel attach
        if (_productCategories.value == null) {
            loadProductCategories()
        }
        viewState = viewState.copy()
    }

    override fun onCleared() {
        super.onCleared()
        productCategoriesRepository.onCleanup()
    }

    // Helper function to know whether there's already fetch in queue
    private fun isLoading() = viewState.isLoading == true

    // Helper function to know whether the view is already refreshing
    private fun isRefreshing() = viewState.isRefreshing == true

    final fun reloadProductsFromDb() {
        _productCategories.value = productCategoriesRepository.getProductCategoriesList()
    }

    /**
     * Loads all the product categories from either the database or the server. Note that this method
     * does not load more categories. It opts to fetch all the categories at once.
     *
     * It will take into account whether a fetch for all categories is already active.
     */
    final fun loadProductCategories() {
        if (isLoading()) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading product categories")
            return
        }

        // if a fetch is already active, wait for it to finish before we start another one
        waitForExistingLoad()

        loadCategoriesJob = launch {
            val showSkeleton: Boolean
            // if this is the initial load, first get the categories from the db and show them immediately
            val productsInDb = productCategoriesRepository.getProductCategoriesList()
            if (productsInDb.isEmpty()) {
                showSkeleton = true
            } else {
                _productCategories.value = productsInDb
                showSkeleton = !isRefreshing()
            }
            viewState = viewState.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    isSkeletonShown = showSkeleton,
                    isEmptyViewVisible = false
            )
            fetchProductCategories()
        }
    }

    /**
     * If categories are already being fetched, wait for the existing job to finish
     */
    private fun waitForExistingLoad() {
        if (loadCategoriesJob?.isActive == true) {
            launch {
                try {
                    loadCategoriesJob?.join()
                } catch (e: CancellationException) {
                    WooLog.d(WooLog.T.PRODUCTS, "CancellationException while waiting for existing fetch")
                }
            }
        }
    }

    fun refreshProductCategories(scrollToTop: Boolean = false) {
        viewState = viewState.copy(isRefreshing = true)
        loadProductCategories()
    }

    /**
     * The helper method that calls the repository's fetchAll categories method. This method will not
     * load more categories, but instead fetches all categories on a site in a single fetch. This is
     * required because we want to display all the categories for parent selection to the user.
     */
    private suspend fun fetchProductCategories() {
        if (networkStatus.isConnected()) {
            _productCategories.value = productCategoriesRepository.fetchAllProductCategories()

            viewState = viewState.copy(
                    isLoading = true,
                    canLoadMore = false,
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
    }

    @Parcelize
    data class ProductCategoriesViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val isNewCategoryAdded: Boolean? = null
    ) : Parcelable

    sealed class ProductCategoriesListEvent : Event() {
        object ScrollToTop : ProductCategoriesListEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ProductCategoriesListViewModel>
}
