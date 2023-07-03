package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Loading.mapFromWCCustomer
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@Suppress("UnusedPrivateMember", "EmptyFunctionBlock")
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val customerListRepository: CustomerListRepository
) : ScopedViewModel(savedState) {
    private val _viewState = MutableLiveData<CustomerListViewState>()
    val viewState: LiveData<CustomerListViewState> = _viewState

    private var paginationState = PaginationState(1, true)

    private var searchQuery: String
        get() = savedState.get<String>(SEARCH_QUERY_KEY) ?: ""
        set(value) {
            savedState[SEARCH_QUERY_KEY] = value
        }
    private var selectedSearchMode: SearchMode
        get() = savedState.get<SearchMode>(SEARCH_MODE_KEY) ?: supportedSearchModes.first()
        set(value) {
            savedState[SEARCH_MODE_KEY] = value
        }

    init {
        launch {
            loadFirstPage()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCustomerSelected(customerId: Long) {
    }

    fun onSearchQueryChanged(query: String) {
        with(query) {
            searchQuery = this
            _viewState.value = _viewState.value!!.copy(searchQuery = this)
        }
        loadFirstPage()
    }

    fun onSearchTypeChanged(searchTypeId: Int) {
        with(searchTypeId) {
            selectedSearchMode = supportedSearchModes.first { it.labelResId == this }
                .copy(isSelected = true)
            _viewState.value = _viewState.value!!.copy(
                searchModes = selectSearchMode(this)
            )
        }

        loadFirstPage()
    }

    fun onNavigateBack() {
    }

    fun onEndOfListReached() {
        appendLoadingItemToList()
        loadCustomers(paginationState.currentPage + 1)
        removeLoadingItemFromList()
    }

    private fun loadCustomers(page: Int) {
        if (!paginationState.hasNextPage) return

        launch {
            val result = customerListRepository.searchCustomerListWithEmail(
                searchQuery = searchQuery,
                searchBy = selectedSearchMode.searchParam,
                pageSize = PAGE_SIZE,
                page = page
            )

            if (result.isFailure) {
                paginationState = PaginationState(1, false)
                _viewState.value = _viewState.value!!.copy(body = CustomerListViewState.CustomerList.Error)
            } else {
                val customers = result.getOrNull() ?: emptyList()

                paginationState = PaginationState(page, customers.size == PAGE_SIZE)

                _viewState.value = _viewState.value!!.copy(
                    body = CustomerListViewState.CustomerList.Loaded(
                        customers = customers.map {
                            mapFromWCCustomer(it)
                        }
                    )
                )
            }
        }
    }

    private fun loadFirstPage() {
        _viewState.value = CustomerListViewState(
            searchQuery = searchQuery,
            searchModes = selectSearchMode(selectedSearchMode.labelResId),
            body = CustomerListViewState.CustomerList.Loading
        )

        loadCustomers(1)
    }

    private fun appendLoadingItemToList() {
        val currentBody = _viewState.value!!.body as? CustomerListViewState.CustomerList.Loaded ?: return
        _viewState.value = _viewState.value!!.copy(
            body = currentBody.copy(
                customers = currentBody.customers + CustomerListViewState.CustomerList.Item.Loading
            )
        )
    }

    private fun removeLoadingItemFromList() {
        val currentBody = _viewState.value!!.body as? CustomerListViewState.CustomerList.Loaded ?: return
        _viewState.value = _viewState.value!!.copy(
            body = currentBody.copy(
                customers = currentBody.customers.filterNot { it is CustomerListViewState.CustomerList.Item.Loading }
            )
        )
    }

    private fun selectSearchMode(searchTypeId: Int) =
        supportedSearchModes.map {
            it.copy(isSelected = it.labelResId == searchTypeId)
        }

    private companion object {
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val SEARCH_MODE_KEY = "search_mode"

        private const val PAGE_SIZE = 30

        private val supportedSearchModes = listOf(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_everything,
                searchParam = "all",
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = "email",
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = "name",
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_username,
                searchParam = "username",
                isSelected = false,
            ),
        )
    }
}
