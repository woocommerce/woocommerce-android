package com.woocommerce.android.ui.orders.creation.customerlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes.Companion.SEARCH_MODE_VALUE_ALL
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes.Companion.SEARCH_MODE_VALUE_EMAIL
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes.Companion.SEARCH_MODE_VALUE_NAME
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes.Companion.SEARCH_MODE_VALUE_USERNAME
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val stringUtils: StringUtils,
) : ScopedViewModel(savedState) {
    private val allowGuests = savedState.get<Boolean>("allowGuests") ?: false
    private val allowCustomerCreation = savedState.get<Boolean>("allowCustomerCreation") ?: false

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
            if (isAdvancedSearchSupported()) {
                _viewState.value = advancedSearchSupportedInitState()
                awaitAll(
                    async { loadCustomers(1) },
                    async { repository.loadCountries() },
                )
            } else {
                _viewState.value = advancedSearchNotSupportedInitState()
                repository.loadCountries()
            }
        }
    }

    fun onCustomerSelected(customerModel: WCCustomerModel) {
        analyticsTracker.track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        when {
            customerModel.remoteCustomerId > 0L -> {
                // this customer is registered, so we may have more info on them
                tryLoadMoreInfo(customerModel)
            }
            allowGuests -> {
                exitWithCustomer(customerModel)
            }
            else -> {
                triggerEvent(
                    ShowDialog(
                        messageId = R.string.customer_picker_guest_customer_not_allowed_message,
                        positiveButtonId = R.string.dialog_ok
                    )
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        with(query) {
            searchQuery = this
            _viewState.value = _viewState.value!!.copy(searchQuery = this)
        }

        loadIfNeededAfterSearchChanged()
    }

    fun onSearchTypeChanged(searchModeId: Int) {
        with(searchModeId) {
            selectedSearchModeId = this
            val supportedSearchModes = _viewState.value!!.searchModes
            _viewState.value = _viewState.value!!.copy(
                searchModes = supportedSearchModes.selectSearchMode(this)
            )
        }

        if (searchQuery.isNotEmpty()) loadIfNeededAfterSearchChanged()
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onAddCustomerClicked(email: String? = null) {
        analyticsTracker.track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADD_MANUALLY_TAPPED)
        triggerEvent(AddCustomer(email))
    }

    fun onEndOfListReached() {
        launch { loadCustomers(paginationState.currentPage + 1) }
    }

    private fun loadIfNeededAfterSearchChanged() {
        loadingFirstPageJob?.cancel()
        loadingFirstPageJob = launch {
            if (searchQuery.isNotEmpty() || isAdvancedSearchSupported()) {
                loadCustomers(1)
            } else {
                _viewState.value = advancedSearchNotSupportedInitState()
            }
        }
    }

    private fun tryLoadMoreInfo(customerModel: WCCustomerModel) {
        loadingMoreInfoAboutCustomerJob?.cancel()
        loadingMoreInfoAboutCustomerJob = launch {
            _viewState.value = _viewState.value!!.copy(partialLoading = true)
            val result = repository.fetchCustomerByRemoteId(customerModel.remoteCustomerId)
            _viewState.value = _viewState.value!!.copy(partialLoading = false)
            if (result.isError || result.model == null) {
                // just use what we have
                exitWithCustomer(customerModel)
            } else {
                exitWithCustomer(result.model!!)
            }
        }
    }

    private suspend fun loadCustomers(page: Int) = mutex.withLock {
        if (page != 1 && !paginationState.hasNextPage) return

        val searchBy = getSearchParam()

        if (page == 1) {
            if (searchQuery.isNotEmpty()) {
                _viewState.value = _viewState.value!!.copy(body = CustomerListViewState.CustomerList.Loading)
                // Add a delay to avoid multiple requests when the user types fast or switches search types
                delay(SEARCH_DELAY_MS)
                analyticsTracker.track(
                    AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
                    mapOf(
                        "search_type" to viewState.value?.searchModes?.firstOrNull { it.isSelected }?.searchParam
                    )
                )
            } else {
                val cachedCustomers = repository.getCustomerList(PAGE_SIZE)
                if (cachedCustomers.isNotEmpty()) {
                    handleFirstPageLoaded(
                        customers = cachedCustomers,
                        searchParam = searchBy
                    )
                } else {
                    _viewState.value = _viewState.value!!.copy(body = CustomerListViewState.CustomerList.Loading)
                    // Add a delay to avoid multiple requests when the user types fast or switches search types
                    delay(SEARCH_DELAY_MS)
                }
            }
        }

        val result = repository.searchCustomerListWithEmail(
            searchQuery = searchQuery,
            searchBy = searchBy,
            pageSize = PAGE_SIZE,
            page = page,
        )
        if (result.isFailure) {
            paginationState = PaginationState(1, false)
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Error(R.string.error_generic),
                showFab = allowCustomerCreation,
            )
        } else {
            val customers = result.getOrNull() ?: emptyList()
            val hasNextPage = customers.size == PAGE_SIZE
            paginationState = PaginationState(page, hasNextPage)
            handleSuccessfulResponse(customers, page == 1, hasNextPage, searchBy)
        }
    }

    private fun handleSuccessfulResponse(
        customers: List<WCCustomerModel>,
        firstPageLoaded: Boolean,
        hasNextPage: Boolean,
        searchParam: String,
    ) {
        removeLoadingItemFromList()
        if (firstPageLoaded) {
            handleFirstPageLoaded(customers, searchParam)
        } else {
            handleNextPageLoaded(customers, searchParam)
        }
        if (hasNextPage) appendLoadingItemToList()
    }

    private fun handleNextPageLoaded(customers: List<WCCustomerModel>, searchParam: String) {
        val currentBody = _viewState.value!!.body as CustomerListViewState.CustomerList.Loaded
        _viewState.value = _viewState.value!!.copy(
            body = currentBody.copy(
                customers = currentBody.customers + customers.map {
                    mapper.mapFromWCCustomerToItem(it, searchQuery, searchParamToSearchType(searchParam))
                },
                shouldResetScrollPosition = false,
            )
        )
    }

    private fun handleFirstPageLoaded(
        customers: List<WCCustomerModel>,
        searchParam: String,
    ) {
        if (customers.isEmpty()) {
            val searchQuery = searchQuery
            val isSearchQueryEmail = stringUtils.isValidEmail(searchQuery)
            val button = if (isSearchQueryEmail) {
                Button(
                    R.string.order_creation_customer_search_empty_add_details_manually_with_email,
                    onClick = { onAddCustomerClicked(searchQuery) }
                )
            } else {
                Button(
                    R.string.order_creation_customer_search_empty_add_details_manually,
                    onClick = { onAddCustomerClicked(null) }
                )
            }
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Empty(
                    R.string.order_creation_customer_search_empty,
                    R.drawable.img_empty_search,
                    button = button,
                ),
                showFab = false,
            )
        } else {
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Loaded(
                    customers = customers.map {
                        mapper.mapFromWCCustomerToItem(it, searchQuery, searchParamToSearchType(searchParam))
                    },
                    shouldResetScrollPosition = true,
                    showGuestChip = !allowGuests
                ),
                showFab = allowCustomerCreation,
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

    private fun exitWithCustomer(wcCustomer: WCCustomerModel) {
        val billingAddress = mapper.mapFromCustomerModelToBillingAddress(wcCustomer)
        val shippingAddress = mapper.mapFromCustomerModelToShippingAddress(wcCustomer)

        val shippingCountry = repository.getCountry(shippingAddress.country)
        val shippingState = repository.getState(shippingAddress.country, shippingAddress.state)

        val billingCountry = repository.getCountry(billingAddress.country)
        val billingState = repository.getState(billingAddress.country, billingAddress.state)

        triggerEvent(
            CustomerSelected(
                Order.Customer(
                    customerId = wcCustomer.remoteCustomerId,
                    firstName = wcCustomer.firstName,
                    lastName = wcCustomer.lastName,
                    email = wcCustomer.email,
                    billingAddress = mapper.mapFromOrderAddressToAddress(
                        billingAddress,
                        billingCountry,
                        billingState
                    ),
                    shippingAddress = mapper.mapFromOrderAddressToAddress(
                        shippingAddress,
                        shippingCountry,
                        shippingState
                    ),
                    username = wcCustomer.username
                )
            )
        )
    }

    private suspend fun getSearchParam() =
        if (isAdvancedSearchSupported()) {
            SEARCH_MODE_VALUE_ALL
        } else {
            _viewState.value!!.searchModes.first { it.isSelected }.searchParam
        }

    private fun searchParamToSearchType(searchParam: String) =
        when (searchParam) {
            SEARCH_MODE_VALUE_NAME -> CustomerListDisplayTextHandler.SearchType.NAME
            SEARCH_MODE_VALUE_EMAIL -> CustomerListDisplayTextHandler.SearchType.EMAIL
            SEARCH_MODE_VALUE_USERNAME -> CustomerListDisplayTextHandler.SearchType.USERNAME
            SEARCH_MODE_VALUE_ALL -> CustomerListDisplayTextHandler.SearchType.ALL
            else -> error("Unknown search param: $searchParam")
        }

    private fun advancedSearchNotSupportedInitState() = CustomerListViewState(
        searchHint = R.string.order_creation_customer_search_old_wc_hint,
        searchQuery = searchQuery,
        showFab = false,
        searchFocused = true,
        searchModes = getSupportedSearchModes(false).selectSearchMode(selectedSearchModeId),
        body = CustomerListViewState.CustomerList.Empty(
            R.string.order_creation_customer_search_empty_on_old_version_wcpay,
            R.drawable.img_search_suggestion,
            button = Button(
                R.string.order_creation_customer_search_empty_add_details_manually,
                onClick = { onAddCustomerClicked(null) }
            )
        )
    )

    private fun advancedSearchSupportedInitState() = CustomerListViewState(
        searchHint = R.string.order_creation_customer_search_hint,
        searchQuery = searchQuery,
        showFab = allowCustomerCreation,
        searchFocused = false,
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
