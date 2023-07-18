package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

@HiltViewModel
@Suppress("EmptyFunctionBlock")
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: CustomerListRepository,
    private val mapper: CustomerListViewModelMapper,
) : ScopedViewModel(savedState) {
    private val _viewState = MutableStateFlow(
        CustomerListViewState(
            searchQuery = searchQuery,
            searchModes = selectSearchMode(selectedSearchMode.labelResId),
            body = CustomerListViewState.CustomerList.Loading
        )
    )
    val viewState: LiveData<CustomerListViewState> = _viewState.asLiveData()

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

    private var loadingFirstPageJob: Job? = null
    private var loadingMoreInfoAboutCustomerJob: Job? = null
    private val mutex = Mutex()

    init {
        launch {
            repository.loadCountries()
            loadCustomers(1)
        }
    }

    fun onCustomerSelected(customerModel: WCCustomerModel) {
        if (customerModel.remoteCustomerId > 0L) {
            // this customer is registered, so we may have more info on them
            tryLoadMoreInfo(customerModel)
        } else {
            openCustomerDetails(customerModel)
        }
    }

    fun onSearchQueryChanged(query: String) {
        with(query) {
            searchQuery = this
            _viewState.value = _viewState.value.copy(searchQuery = this)
        }

        loadAfterSearchChanged()
    }

    fun onSearchTypeChanged(searchTypeId: Int) {
        with(searchTypeId) {
            selectedSearchMode = supportedSearchModes.first { it.labelResId == this }.copy(isSelected = true)
            _viewState.value = _viewState.value.copy(
                searchModes = selectSearchMode(this)
            )
        }

        if (searchQuery.isNotEmpty()) loadAfterSearchChanged()
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onEndOfListReached() {
        launch { loadCustomers(paginationState.currentPage + 1) }
    }

    private fun loadAfterSearchChanged() {
        loadingFirstPageJob?.cancel()
        loadingFirstPageJob = launch { loadCustomers(1) }
    }

    private fun tryLoadMoreInfo(customerModel: WCCustomerModel) {
        loadingMoreInfoAboutCustomerJob?.cancel()
        loadingMoreInfoAboutCustomerJob = launch {
            _viewState.value = _viewState.value.copy(partialLoading = true)
            val result = repository.fetchCustomerByRemoteId(customerModel.remoteCustomerId)
            _viewState.value = _viewState.value.copy(partialLoading = false)
            if (result.isError || result.model == null) {
                // just use what we have
                openCustomerDetails(customerModel)
            } else {
                openCustomerDetails(result.model!!)
            }
        }
    }

    private suspend fun loadCustomers(page: Int) = mutex.withLock {
        if (page != 1 && !paginationState.hasNextPage) return
        if (page == 1) {
            _viewState.value = _viewState.value.copy(body = CustomerListViewState.CustomerList.Loading)
            // Add a delay to avoid multiple requests when the user types fast or switches search types
            delay(SEARCH_DELAY_MS)
        }
        val result = repository.searchCustomerListWithEmail(
            searchQuery = searchQuery,
            searchBy = selectedSearchMode.searchParam,
            pageSize = PAGE_SIZE,
            page = page
        )
        if (result.isFailure) {
            paginationState = PaginationState(1, false)
            _viewState.value = _viewState.value.copy(body = CustomerListViewState.CustomerList.Error)
        } else {
            val customers = result.getOrNull() ?: emptyList()
            val hasNextPage = customers.size == PAGE_SIZE
            paginationState = PaginationState(page, hasNextPage)
            handleSuccessfulResponse(customers, page == 1, hasNextPage)
        }
    }

    private fun handleSuccessfulResponse(
        customers: List<WCCustomerModel>,
        firstPageLoaded: Boolean,
        hasNextPage: Boolean
    ) {
        removeLoadingItemFromList()
        if (firstPageLoaded) {
            handleFirstPageLoaded(customers)
        } else {
            handleNextPageLoaded(customers)
        }
        if (hasNextPage) appendLoadingItemToList()
    }

    private fun handleNextPageLoaded(customers: List<WCCustomerModel>) {
        val currentBody = _viewState.value.body as CustomerListViewState.CustomerList.Loaded
        _viewState.value = _viewState.value.copy(
            body = currentBody.copy(
                customers = currentBody.customers + customers.map {
                    mapper.mapFromWCCustomerToItem(it)
                },
                firstPageLoaded = false,
            )
        )
    }

    private fun handleFirstPageLoaded(customers: List<WCCustomerModel>) {
        if (customers.isEmpty()) {
            _viewState.value = _viewState.value.copy(body = CustomerListViewState.CustomerList.Empty)
        } else {
            _viewState.value = _viewState.value.copy(
                body = CustomerListViewState.CustomerList.Loaded(
                    customers = customers.map {
                        mapper.mapFromWCCustomerToItem(it)
                    },
                    firstPageLoaded = true,
                )
            )
        }
    }

    private fun appendLoadingItemToList() {
        val currentBody = _viewState.value.body as? CustomerListViewState.CustomerList.Loaded ?: return
        _viewState.value = _viewState.value.copy(
            body = currentBody.copy(
                customers = currentBody.customers + CustomerListViewState.CustomerList.Item.Loading
            )
        )
    }

    private fun removeLoadingItemFromList() {
        val currentBody = _viewState.value.body as? CustomerListViewState.CustomerList.Loaded ?: return
        if (currentBody.customers.none { it is CustomerListViewState.CustomerList.Item.Loading }) return
        _viewState.value = _viewState.value.copy(
            body = currentBody.copy(
                customers = currentBody.customers.filterNot { it is CustomerListViewState.CustomerList.Item.Loading }
            )
        )
    }

    private fun openCustomerDetails(wcCustomer: WCCustomerModel) {
        val billingAddress = mapper.mapFromCustomerModelToBillingAddress(wcCustomer)
        val shippingAddress = mapper.mapFromCustomerModelToShippingAddress(wcCustomer)

        val shippingCountry = repository.getCountry(shippingAddress.country)
        val shippingState = repository.getState(shippingAddress.country, shippingAddress.state)

        val billingCountry = repository.getCountry(billingAddress.country)
        val billingState = repository.getState(billingAddress.country, billingAddress.state)

        triggerEvent(
            CustomerSelected(
                customerId = wcCustomer.remoteCustomerId,
                billingAddress = mapper.mapFromOrderAddressToAddress(billingAddress, billingCountry, billingState),
                shippingAddress = mapper.mapFromOrderAddressToAddress(shippingAddress, shippingCountry, shippingState),
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

        private const val SEARCH_DELAY_MS = 500L

        private const val PAGE_SIZE = 30

        private val supportedSearchModes = listOf(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_everything,
                searchParam = "all",
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = "name",
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = "email",
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
