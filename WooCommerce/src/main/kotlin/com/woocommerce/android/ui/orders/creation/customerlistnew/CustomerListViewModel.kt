package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: CustomerListRepository,
    private val mapper: CustomerListViewModelMapper,
    private val isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported,
    private val getSupportedSearchModes: CustomerListGetSupportedSearchModes,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    @Volatile
    private var paginationState = PaginationState(1, true)

    private var searchQuery: String
        get() = savedState.get<String>(SEARCH_QUERY_KEY) ?: ""
        set(value) {
            savedState[SEARCH_QUERY_KEY] = value
        }
    private var selectedSearchModeId: Int?
        get() = savedState.get<Int>(SEARCH_MODE_KEY)
        set(value) {
            savedState[SEARCH_MODE_KEY] = value
        }

    private val _viewState = MutableLiveData<CustomerListViewState>()
    val viewState: LiveData<CustomerListViewState> = _viewState

    private var loadingFirstPageJob: Job? = null
    private var loadingMoreInfoAboutCustomerJob: Job? = null
    private val mutex = Mutex()

    init {
        launch {
            repository.loadCountries()
            if (isAdvancedSearchSupported()) {
                _viewState.value = advancedSearchSupportedInitState()
                loadCustomers(1)
            } else {
                _viewState.value = advancedSearchNotSupportedInitState()
            }
        }
    }

    fun onCustomerSelected(customerModel: WCCustomerModel) {
        analyticsTrackerWrapper.track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
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
            _viewState.value = _viewState.value!!.copy(searchQuery = this)
        }

        launch {
            if (query.isNotEmpty() || isAdvancedSearchSupported()) {
                loadAfterSearchChanged()
            } else {
                _viewState.value = advancedSearchNotSupportedInitState()
            }
        }
    }

    fun onSearchTypeChanged(searchModeId: Int) {
        with(searchModeId) {
            selectedSearchModeId = this
            val supportedSearchModes = _viewState.value!!.searchModes
            _viewState.value = _viewState.value!!.copy(
                searchModes = supportedSearchModes.selectSearchMode(this)
            )
        }

        if (searchQuery.isNotEmpty()) loadAfterSearchChanged()
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onAddCustomerClicked() {
        triggerEvent(AddCustomer)
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
            _viewState.value = _viewState.value!!.copy(partialLoading = true)
            val result = repository.fetchCustomerByRemoteId(customerModel.remoteCustomerId)
            _viewState.value = _viewState.value!!.copy(partialLoading = false)
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
            _viewState.value = _viewState.value!!.copy(body = CustomerListViewState.CustomerList.Loading)
            // Add a delay to avoid multiple requests when the user types fast or switches search types
            delay(SEARCH_DELAY_MS)
            if (searchQuery.isNotEmpty()) {
                analyticsTrackerWrapper.track(
                    AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
                    mapOf(
                        "search_type" to viewState.value?.searchModes?.firstOrNull { it.isSelected }?.searchParam
                    )
                )
            }
        }
        val result = repository.searchCustomerListWithEmail(
            searchQuery = searchQuery,
            searchBy = getSearchParam(),
            pageSize = PAGE_SIZE,
            page = page
        )
        if (result.isFailure) {
            paginationState = PaginationState(1, false)
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Error(R.string.error_generic)
            )
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
        val currentBody = _viewState.value!!.body as CustomerListViewState.CustomerList.Loaded
        _viewState.value = _viewState.value!!.copy(
            body = currentBody.copy(
                customers = currentBody.customers + customers.map {
                    mapper.mapFromWCCustomerToItem(it)
                },
                shouldResetScrollPosition = false,
            )
        )
    }

    private fun handleFirstPageLoaded(customers: List<WCCustomerModel>) {
        if (customers.isEmpty()) {
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Empty(R.string.order_creation_customer_search_empty)
            )
        } else {
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Loaded(
                    customers = customers.map {
                        mapper.mapFromWCCustomerToItem(it)
                    },
                    shouldResetScrollPosition = true,
                )
            )
        }
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
        if (currentBody.customers.none { it is CustomerListViewState.CustomerList.Item.Loading }) return
        _viewState.value = _viewState.value!!.copy(
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

    private suspend fun getSearchParam() =
        if (isAdvancedSearchSupported()) {
            "all"
        } else {
            _viewState.value!!.searchModes.first { it.isSelected }.searchParam
        }

    private fun advancedSearchNotSupportedInitState() = CustomerListViewState(
        searchQuery = searchQuery,
        searchModes = getSupportedSearchModes(false).selectSearchMode(selectedSearchModeId),
        body = CustomerListViewState.CustomerList.Empty(
            R.string.order_creation_customer_search_empty_on_old_version_wcpay
        )
    )

    private fun advancedSearchSupportedInitState() = CustomerListViewState(
        searchQuery = searchQuery,
        searchModes = getSupportedSearchModes(true).selectSearchMode(selectedSearchModeId),
        body = CustomerListViewState.CustomerList.Loading
    )

    private fun List<SearchMode>.selectSearchMode(searchTypeId: Int?) =
        when {
            isEmpty() -> emptyList()
            searchTypeId == null -> listOf(first().copy(isSelected = true)) + drop(1)
            else -> map { it.copy(isSelected = it.labelResId == searchTypeId) }
        }

    private companion object {
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val SEARCH_MODE_KEY = "search_mode"

        private const val SEARCH_DELAY_MS = 500L

        private const val PAGE_SIZE = 30
    }
}
