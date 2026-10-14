package com.woocommerce.android.ui.customer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.customerlist.AddCustomer
import com.woocommerce.android.ui.orders.creation.customerlist.Button
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListDisplayTextHandler
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListIsAdvancedSearchSupported
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModelMapper
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewState
import com.woocommerce.android.ui.orders.creation.customerlist.PaginationState
import com.woocommerce.android.ui.orders.creation.customerlist.SearchMode
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@Suppress("LongParameterList")
abstract class CustomerListViewModel(
    savedState: SavedStateHandle,
    private val repository: CustomerListRepository,
    private val mapper: CustomerListViewModelMapper,
    private val isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported,
    private val getSupportedSearchModes: CustomerListGetSupportedSearchModes,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val stringUtils: StringUtils,
    private val trackingSource: String,
) : ScopedViewModel(savedState) {
    protected val allowGuests = savedState.get<Boolean>("allowGuests") ?: false
    protected val allowCustomerCreation = savedState.get<Boolean>("allowCustomerCreation") ?: false

    @Volatile
    protected var paginationState = PaginationState(1, true)

    protected var searchQuery: String
        get() = savedState.get<String>(SEARCH_QUERY_KEY) ?: ""
        set(value) {
            savedState[SEARCH_QUERY_KEY] = value
        }
    protected var selectedSearchModeId: Int?
        get() = savedState.get<Int>(SEARCH_MODE_KEY)
        set(value) {
            savedState[SEARCH_MODE_KEY] = value
        }

    @Suppress("PropertyName", "VariableNaming")
    protected val _viewState = MutableLiveData<CustomerListViewState>()
    val viewState: LiveData<CustomerListViewState> = _viewState

    private var loadingFirstPageJob: Job? = null
    private val mutex = Mutex()
    private var canShowCachedCustomers = true

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

    abstract fun onCustomerSelected(customerModel: WCCustomerModel)

    fun onSearchQueryChanged(query: String) {
        with(query) {
            searchQuery = this
            _viewState.value = _viewState.value!!.copy(searchQuery = this)
        }

        reloadCustomers()
    }

    fun onSearchTypeChanged(searchModeId: Int) {
        with(searchModeId) {
            selectedSearchModeId = this
            val supportedSearchModes = _viewState.value!!.searchModes
            _viewState.value = _viewState.value!!.copy(
                searchModes = supportedSearchModes.selectSearchMode(this)
            )
        }

        if (searchQuery.isNotEmpty()) reloadCustomers()
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onRetryClicked() {
        if (_viewState.value?.body !is CustomerListViewState.CustomerList.AnalyticsUnavailable) return

        showLoading()
        reloadCustomers(ReloadTrigger.RETRY)
    }

    fun onEnableAnalyticsClicked() {
        val body = _viewState.value?.body as? CustomerListViewState.CustomerList.AnalyticsUnavailable ?: return
        if (!body.canEnableAnalytics) return

        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.customer_list_enable_analytics_confirmation_title,
                messageId = R.string.customer_list_enable_analytics_confirmation_message,
                positiveButtonId = R.string.customer_list_enable_analytics_confirmation_action,
                negativeButtonId = R.string.cancel,
                positiveBtnAction = { _, _ -> enableAnalyticsAndReload() },
            )
        )
    }

    fun onAddCustomerClicked(email: String? = null) {
        analyticsTracker.track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADD_MANUALLY_TAPPED)
        triggerEvent(AddCustomer(email))
    }

    fun onEndOfListReached() {
        launch { loadCustomers(paginationState.currentPage + 1) }
    }

    private suspend fun loadCustomers(page: Int, reloadTrigger: ReloadTrigger? = null): Unit = mutex.withLock {
        if (page != 1 && !paginationState.hasNextPage) return

        val requestedQuery = searchQuery
        val requestedSearchModeId = selectedSearchModeId
        if (page == 1 && requestedQuery.isEmpty() && !isAdvancedSearchSupported()) {
            _viewState.value = advancedSearchNotSupportedInitState()
            return
        }

        val searchBy = getSearchParam(requestedSearchModeId)

        if (page == 1 && reloadTrigger == null) prepareFirstPage(requestedQuery, searchBy)

        val result = repository.searchCustomerListWithEmail(
            searchQuery = requestedQuery,
            searchBy = searchBy,
            pageSize = PAGE_SIZE,
            page = page,
        )
        if (result.isFailure) {
            paginationState = PaginationState(1, false)
            handleRequestFailure(
                error = result.exceptionOrNull(),
                requestedQuery = requestedQuery,
                requestedSearchModeId = requestedSearchModeId,
                reloadTrigger = reloadTrigger,
            )
            return
        }

        val customers = result.getOrNull() ?: emptyList()
        val hasNextPage = customers.size == PAGE_SIZE
        canShowCachedCustomers = true
        paginationState = PaginationState(page, hasNextPage)
        handleSuccessfulResponse(customers, page == 1, hasNextPage, searchBy)
        if (page == 1 && reloadTrigger != null) {
            analyticsTracker.track(
                AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS,
                mapOf(
                    AnalyticsTracker.KEY_SOURCE to trackingSource,
                    AnalyticsTracker.KEY_TYPE to reloadTrigger.trackingValue,
                )
            )
        }
    }

    private suspend fun prepareFirstPage(requestedQuery: String, searchBy: String) {
        if (requestedQuery.isNotEmpty()) {
            showLoading()
            // Add a delay to avoid multiple requests when the user types fast or switches search types
            delay(SEARCH_DELAY_MS)
            analyticsTracker.track(
                AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
                mapOf(
                    "search_type" to viewState.value?.searchModes?.firstOrNull { it.isSelected }?.searchParam
                )
            )
        } else if (canShowCachedCustomers) {
            val cachedCustomers = repository.getCustomerList(PAGE_SIZE)
            if (cachedCustomers.isNotEmpty()) {
                handleFirstPageLoaded(cachedCustomers, searchBy)
            } else {
                showLoading()
                // Add a delay to avoid multiple requests when the user types fast or switches search types
                delay(SEARCH_DELAY_MS)
            }
        } else {
            showLoading()
        }
    }

    private suspend fun handleRequestFailure(
        error: Throwable?,
        requestedQuery: String,
        requestedSearchModeId: Int?,
        reloadTrigger: ReloadTrigger?,
    ) {
        if ((error as? WooException)?.error?.apiErrorCode == REST_NO_ROUTE_ERROR_CODE) {
            handleMissingCustomerRoute(requestedQuery, requestedSearchModeId, reloadTrigger)
        } else {
            _viewState.value = _viewState.value!!.copy(
                body = CustomerListViewState.CustomerList.Error(R.string.error_generic),
                showFab = allowCustomerCreation,
            )
        }
    }

    private suspend fun handleMissingCustomerRoute(
        requestedQuery: String,
        requestedSearchModeId: Int?,
        reloadTrigger: ReloadTrigger?,
    ) {
        if (!isCurrentRequest(requestedQuery, requestedSearchModeId)) return

        canShowCachedCustomers = false
        _viewState.value = _viewState.value!!.copy(
            body = CustomerListViewState.CustomerList.Loading,
            showFab = allowCustomerCreation,
        )

        val settingsResult = repository.fetchAnalyticsEnabled()
        if (!isCurrentRequest(requestedQuery, requestedSearchModeId)) return

        showAnalyticsUnavailable(settingsResult, reloadTrigger)
    }

    private fun showAnalyticsUnavailable(settingsResult: WooResult<Boolean>, reloadTrigger: ReloadTrigger?) {
        val isAnalyticsDisabled = !settingsResult.isError && settingsResult.model == false
        val isAnalyticsEnabled = !settingsResult.isError && settingsResult.model == true
        val isPermissionDenied = settingsResult.isError &&
            settingsResult.error.apiErrorCode == ANALYTICS_PERMISSION_DENIED_ERROR_CODE
        val cause = when {
            isAnalyticsDisabled -> "disabled"
            isAnalyticsEnabled -> AnalyticsTracker.VALUE_OTHER
            else -> "unknown"
        }
        val justEnabledAnalytics = reloadTrigger == ReloadTrigger.ENABLE_ANALYTICS
        val message = when {
            justEnabledAnalytics -> R.string.customer_list_analytics_unavailable_message
            isAnalyticsDisabled -> R.string.customer_list_analytics_disabled_message
            isPermissionDenied -> R.string.customer_list_analytics_permission_denied_message
            else -> R.string.customer_list_analytics_unavailable_message
        }
        _viewState.value = _viewState.value!!.copy(
            body = CustomerListViewState.CustomerList.AnalyticsUnavailable(
                message = message,
                canEnableAnalytics = isAnalyticsDisabled && !justEnabledAnalytics,
            ),
            showFab = allowCustomerCreation,
        )
        analyticsTracker.track(
            AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to trackingSource,
                AnalyticsTracker.KEY_CAUSE to cause,
            )
        )
    }

    private fun showLoading() {
        _viewState.value = _viewState.value!!.copy(body = CustomerListViewState.CustomerList.Loading)
    }

    private fun enableAnalyticsAndReload() {
        val body = _viewState.value?.body as? CustomerListViewState.CustomerList.AnalyticsUnavailable ?: return
        if (!body.canEnableAnalytics) return

        showLoading()
        loadingFirstPageJob?.cancel()
        launch {
            mutex.withLock {
                val result = repository.enableAnalytics()
                if (!result.isError && result.model == true) {
                    reloadCustomers(ReloadTrigger.ENABLE_ANALYTICS)
                } else {
                    _viewState.value = _viewState.value!!.copy(body = body)
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.customer_list_enable_analytics_error))
                }
            }
        }
    }

    private fun reloadCustomers(reloadTrigger: ReloadTrigger? = null) {
        loadingFirstPageJob?.cancel()
        loadingFirstPageJob = launch {
            loadCustomers(page = 1, reloadTrigger = reloadTrigger)
        }
    }

    private fun isCurrentRequest(requestedQuery: String, requestedSearchModeId: Int?) =
        searchQuery == requestedQuery &&
            (requestedQuery.isEmpty() || selectedSearchModeId == requestedSearchModeId)

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
            val button = when {
                !allowCustomerCreation -> null
                isSearchQueryEmail -> Button(
                    R.string.order_creation_customer_search_empty_add_details_manually_with_email,
                    onClick = { onAddCustomerClicked(searchQuery) }
                )

                else -> Button(
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
                    shouldResetScrollPosition = true
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

    private suspend fun getSearchParam(searchModeId: Int?) =
        if (isAdvancedSearchSupported()) {
            CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_ALL
        } else {
            _viewState.value!!.searchModes.firstOrNull { it.labelResId == searchModeId }?.searchParam
                ?: _viewState.value!!.searchModes.first { it.isSelected }.searchParam
        }

    private fun searchParamToSearchType(searchParam: String) =
        when (searchParam) {
            CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_NAME ->
                CustomerListDisplayTextHandler.SearchType.NAME
            CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_EMAIL ->
                CustomerListDisplayTextHandler.SearchType.EMAIL
            CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_USERNAME ->
                CustomerListDisplayTextHandler.SearchType.USERNAME
            CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_ALL -> CustomerListDisplayTextHandler.SearchType.ALL
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
            button = if (allowCustomerCreation) {
                Button(
                    R.string.order_creation_customer_search_empty_add_details_manually,
                    onClick = { onAddCustomerClicked(null) }
                )
            } else {
                null
            }
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

    private enum class ReloadTrigger(val trackingValue: String) {
        RETRY("retry"),
        ENABLE_ANALYTICS("enable_analytics"),
    }

    private companion object {
        private const val SEARCH_QUERY_KEY = "search_query"
        private const val SEARCH_MODE_KEY = "search_mode"

        private const val SEARCH_DELAY_MS = 500L

        private const val PAGE_SIZE = 30
        private const val REST_NO_ROUTE_ERROR_CODE = "rest_no_route"
        private const val ANALYTICS_PERMISSION_DENIED_ERROR_CODE = "woocommerce_rest_cannot_view"
    }
}
