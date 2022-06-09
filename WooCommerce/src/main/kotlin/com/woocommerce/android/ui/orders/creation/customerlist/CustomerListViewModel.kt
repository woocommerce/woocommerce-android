package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    private val _customerList = MutableLiveData<List<WCCustomerModel>>()
    val customerList: LiveData<List<WCCustomerModel>> = _customerList

    val customerListViewStateLiveData = LiveDataDelegate(savedState, CustomerListViewState())
    private var customernListViewState by customerListViewStateLiveData

    private var searchJob: Job? = null

    fun onCustomerClick(customer: WCCustomerModel) {
        // TODO
    }

    fun onSearchQueryChanged(query: String) {
        if (query.length > 2) {
            // cancel any existing search, then start a new one after a brief delay so we don't
            // actually perform the fetch until the user stops typing
            searchJob?.cancel()
            searchJob = launch {
                delay(AppConstants.SEARCH_TYPING_DELAY_MS)
                customernListViewState = customernListViewState.copy(
                    isSkeletonShown = true,
                    isEmptyViewVisible = false,
                    searchQuery = query
                )
                fetchCustomerList()
            }
        } else {
            launch {
                searchJob?.cancelAndJoin()
                _customerList.value = emptyList()
                customernListViewState = customernListViewState.copy(isEmptyViewVisible = false)
            }
        }
    }

    private suspend fun fetchCustomerList() {
        if (!networkStatus.isConnected()) {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        } else if (customernListViewState.searchQuery.isNullOrEmpty()) {
            _customerList.value = emptyList()
        } else {
        }
            if (searchQuery.isNullOrEmpty()) {
                _productList.value = productRepository.fetchProductList(
                    loadMore,
                    excludedProductIds = excludedProductIds
                )
            } else {
                productRepository.searchProductList(
                    searchQuery,
                    loadMore,
                    excludedProductIds
                )?.let { fetchedProducts ->
                    // make sure the search query hasn't changed while the fetch was processing
                    if (searchQuery == productRepository.lastSearchQuery) {
                        if (loadMore) {
                            _productList.value = _productList.value.orEmpty() + fetchedProducts
                        } else {
                            _productList.value = fetchedProducts
                        }
                    } else {
                        WooLog.d(WooLog.T.PRODUCTS, "Search query changed")
                    }
                }
            }

            productSelectionListViewState = productSelectionListViewState.copy(
                isLoading = true,
                canLoadMore = productRepository.canLoadMoreProducts,
                isEmptyViewVisible = _productList.value?.isEmpty() == true
            )
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }

        productSelectionListViewState = productSelectionListViewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    @Parcelize
    data class CustomerListViewState(
        val isSkeletonShown: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null,
        val searchQuery: String? = null,
        val isSearchActive: Boolean? = null
    ) : Parcelable
}
