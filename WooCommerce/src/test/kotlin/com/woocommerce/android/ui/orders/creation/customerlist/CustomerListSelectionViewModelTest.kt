package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class CustomerListSelectionViewModelTest : BaseUnitTest() {
    private val customerListRepository: CustomerListRepository = mock {
        on {
            searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
        }.thenReturn(Result.success(listOf(mock())))

        on { getCustomerList(any()) }.thenReturn(emptyList())
    }
    private val mockCustomer: CustomerListViewState.CustomerList.Item.Customer = mock()
    private val customerListViewModelMapper: CustomerListViewModelMapper = mock {
        on { mapFromWCCustomerToItem(any(), any(), any()) }.thenReturn(mockCustomer)
    }
    private val getSupportedSearchModes: CustomerListGetSupportedSearchModes = mock {
        on { invoke(true) }.thenReturn(
            listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = false,
                ),
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_email,
                    searchParam = "email",
                    isSelected = false,
                )
            )
        )
    }
    private val isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported = mock {
        on { invoke() }.thenReturn(true)
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val stringUtils: StringUtils = mock {
        on { isValidEmail(any(), any()) }.thenReturn(false)
    }

    @Test
    fun `when viewmodel init, then viewstate is updated with customers`() = testBlocking {
        // WHEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        // THEN
        assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
    }

    @Test
    fun `given advanced search mode, when viewmodel init, then loadCountries is called`() = testBlocking {
        // GIVEN
        whenever(isAdvancedSearchSupported.invoke()).thenReturn(true)

        // WHEN
        initViewModel()
        advanceUntilIdle()

        // THEN
        verify(customerListRepository).loadCountries()
    }

    @Test
    fun `given non advanced search mode, when viewmodel init, then loadCountries is called`() = testBlocking {
        // GIVEN
        whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)

        // WHEN
        initViewModel()
        advanceUntilIdle()

        // THEN
        verify(customerListRepository).loadCountries()
    }

    @Test
    fun `when viewmodel init, then viewstate is updated with search modes`() = testBlocking {
        // WHEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()

        // THEN
        assertThat(states.last().searchModes[0]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = "name",
                isSelected = true,
            )
        )
        assertThat(states.last().searchModes[1]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = "email",
                isSelected = false,
            )
        )
    }

    @Test
    fun `given advanced search mode , when viewmodel init, then advanced loading state emitted`() =
        testBlocking {
            // GIVEN
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(true)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(true)).thenReturn(searchModes)

            // WHEN
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()

            // THEN
            assertThat(states.last().searchModes).isEqualTo(searchModes)
            assertThat(states.last().showFab).isTrue()
            assertThat(states.last().searchFocused).isFalse()
            assertThat(states.last().searchHint).isEqualTo(R.string.order_creation_customer_search_hint)
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loading::class.java)
        }

    @Test
    fun `given advanced search mode, when viewmodel init, then all passed to model mapper`() = testBlocking {
        // GIVEN
        whenever(isAdvancedSearchSupported.invoke()).thenReturn(true)
        val searchModes = emptyList<SearchMode>()
        whenever(getSupportedSearchModes.invoke(true)).thenReturn(searchModes)

        // WHEN
        initViewModel()
        advanceUntilIdle()

        // THEN
        verify(customerListViewModelMapper).mapFromWCCustomerToItem(
            any(),
            eq(""),
            eq(CustomerListDisplayTextHandler.SearchType.ALL)
        )
    }

    @Test
    fun `given not advanced search mode and name selected, when on search query changed, then name passed to model mapper`() =
        testBlocking {
            // GIVEN
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(false)).thenReturn(searchModes)
            val query = "query"

            // WHEN
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged(query)
            advanceUntilIdle()

            // THEN
            verify(customerListViewModelMapper).mapFromWCCustomerToItem(
                any(),
                eq(query),
                eq(CustomerListDisplayTextHandler.SearchType.NAME)
            )
        }

    @Test
    fun `given not advanced search mode and email selected, when on search query changed, then email passed to model mapper`() =
        testBlocking {
            // GIVEN
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_email,
                    searchParam = "email",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(false)).thenReturn(searchModes)
            val query = "query"

            // WHEN
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged(query)
            advanceUntilIdle()

            // THEN
            verify(customerListViewModelMapper).mapFromWCCustomerToItem(
                any(),
                eq(query),
                eq(CustomerListDisplayTextHandler.SearchType.EMAIL)
            )
        }

    @Test
    fun `given not advanced search mode and username selected, when on search query changed, then username passed to model mapper`() =
        testBlocking {
            // GIVEN
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_username,
                    searchParam = "username",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(false)).thenReturn(searchModes)
            val query = "query"

            // WHEN
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged(query)
            advanceUntilIdle()

            // THEN
            verify(customerListViewModelMapper).mapFromWCCustomerToItem(
                any(),
                eq(query),
                eq(CustomerListDisplayTextHandler.SearchType.USERNAME)
            )
        }

    @Test
    fun `given not advanced search mode, when viewmodel init, then not advanced state emitted and customers not loaded`() =
        testBlocking {
            // GIVEN
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(false)).thenReturn(searchModes)

            // WHEN
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()

            // THEN
            verify(customerListRepository, never()).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )

            assertThat(states.last().searchModes).isEqualTo(searchModes)
            assertThat(states.last().searchModes).isEqualTo(searchModes)
            assertThat(states.last().searchHint).isEqualTo(R.string.order_creation_customer_search_old_wc_hint)
            assertThat(states.last().searchFocused).isTrue()
            val emptyState = states.last().body as CustomerListViewState.CustomerList.Empty
            assertThat(emptyState.message).isEqualTo(
                R.string.order_creation_customer_search_empty_on_old_version_wcpay
            )
            assertThat(emptyState.image).isEqualTo(R.drawable.img_search_suggestion)
            assertThat(emptyState.button?.text).isEqualTo(
                R.string.order_creation_customer_search_empty_add_details_manually
            )
        }

    @Test
    fun `given success returned from repo, when viewmodel init, then viewstate is updated with customers and first page true`() =
        testBlocking {
            // GIVEN
            whenever(
                customerListRepository.searchCustomerListWithEmail(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            ).thenReturn(Result.success((1..30).map { mock() }))

            // WHEN
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()

            // THEN
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            assertThat(
                (states.last().body as CustomerListViewState.CustomerList.Loaded).shouldResetScrollPosition
            ).isTrue()
        }

    @Test
    fun `given page number 1, when viewmodel init, then viewstate is updated to Loading state`() = testBlocking {
        // WHEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()

        // THEN
        assertThat(states.first().body).isInstanceOf(CustomerListViewState.CustomerList.Loading::class.java)
    }

    @Test
    fun `given page number 1 and cached customer, when viewmodel init, then viewstate is updated to Loaded state`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.getCustomerList(any())).thenReturn(listOf(mock()))

            // WHEN
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()

            // THEN
            assertThat((states.first().body as CustomerListViewState.CustomerList.Loaded).customers)
                .isEqualTo(listOf(mockCustomer))
        }

    @Test
    fun `given error from repo, when viewmodel init, then viewstate is updated with error state`() = testBlocking {
        // GIVEN
        whenever(
            customerListRepository.searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(Result.failure(Throwable()))

        // WHEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        // THEN
        assertThat(states.last().body)
            .isEqualTo(CustomerListViewState.CustomerList.Error(R.string.error_generic))
        verify(customerListRepository, never()).fetchAnalyticsEnabled()
        verify(analyticsTrackerWrapper, never()).track(
            eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN),
            any(),
        )
        verify(analyticsTrackerWrapper, never()).track(
            eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS),
            any(),
        )
    }

    @Test
    fun `given rest no route and Analytics is disabled, when viewmodel init, then cached customers are replaced`() =
        testBlocking {
            // GIVEN
            val cachedCustomers = CompletableDeferred<List<WCCustomerModel>>()
            whenever(customerListRepository.getCustomerList(any())).doSuspendableAnswer {
                cachedCustomers.await()
            }
            whenever(
                customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any())
            ).thenReturn(restNoRouteFailure())
            val settingsGate = CompletableDeferred<WooResult<Boolean>>()
            val settingsStarted = CompletableDeferred<Unit>()
            whenever(customerListRepository.fetchAnalyticsEnabled()).doSuspendableAnswer {
                settingsStarted.complete(Unit)
                settingsGate.await()
            }
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()

            // WHEN
            cachedCustomers.complete(listOf(WCCustomerModel()))
            advanceUntilIdle()
            assertThat(settingsStarted.isCompleted).isTrue()

            // THEN
            assertThat(states.map { it.body }).anyMatch {
                it is CustomerListViewState.CustomerList.Loaded
            }
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loading::class.java)
            assertThat(states).noneMatch {
                it.body is CustomerListViewState.CustomerList.AnalyticsUnavailable
            }
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN),
                any(),
            )

            settingsGate.complete(WooResult(false))
            advanceUntilIdle()

            assertThat(states.last().body)
                .isInstanceOf(CustomerListViewState.CustomerList.AnalyticsUnavailable::class.java)
            val body = states.last().body as CustomerListViewState.CustomerList.AnalyticsUnavailable
            assertThat(body.message).isEqualTo(R.string.customer_list_analytics_disabled_message)
            assertThat(body.canEnableAnalytics).isTrue()
            assertThat(states.map { it.body }).anyMatch {
                it is CustomerListViewState.CustomerList.Loaded
            }
            verify(customerListRepository).fetchAnalyticsEnabled()
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "disabled")
        }

    @Test
    fun `given another API not found code, when viewmodel init, then generic error is unchanged`() = testBlocking {
        // GIVEN
        whenever(
            customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any())
        ).thenReturn(apiFailure(apiErrorCode = "other_not_found"))

        // WHEN
        val viewModel = initViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value?.body)
            .isEqualTo(CustomerListViewState.CustomerList.Error(R.string.error_generic))
        verify(customerListRepository, never()).fetchAnalyticsEnabled()
    }

    @Test
    fun `given Analytics permission is denied, when rest no route occurs, then ask admin to enable without Enable`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute(
                settingsResult = WooResult(
                    WooError(
                        type = WooErrorType.AUTHORIZATION_REQUIRED,
                        original = BaseRequest.GenericErrorType.AUTHORIZATION_REQUIRED,
                        apiErrorCode = "woocommerce_rest_cannot_view",
                    )
                )
            )

            // WHEN
            val viewModel = initViewModel()
            advanceUntilIdle()

            // THEN
            val body = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable
            assertThat(body.message).isEqualTo(R.string.customer_list_analytics_permission_denied_message)
            assertThat(body.canEnableAnalytics).isFalse()
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "unknown")
        }

    @Test
    fun `given Analytics setting check fails, when rest no route occurs, then show unavailable with unknown cause`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute(
                settingsResult = WooResult(
                    WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)
                )
            )

            // WHEN
            val viewModel = initViewModel()
            advanceUntilIdle()

            // THEN
            val body = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable
            assertThat(body.message).isEqualTo(R.string.customer_list_analytics_unavailable_message)
            assertThat(body.canEnableAnalytics).isFalse()
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "unknown")
        }

    @Test
    fun `given initial settings check is pending, when query changes, then old classification is discarded`() =
        testBlocking {
            // GIVEN
            val settingsGate = CompletableDeferred<WooResult<Boolean>>()
            val settingsStarted = CompletableDeferred<Unit>()
            whenever(customerListRepository.fetchAnalyticsEnabled()).doSuspendableAnswer {
                settingsStarted.complete(Unit)
                settingsGate.await()
            }
            whenever(
                customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any())
            ).thenReturn(restNoRouteFailure(), Result.success(listOf(WCCustomerModel())))
            val viewModel = initViewModel()
            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()
            assertThat(settingsStarted.isCompleted).isTrue()

            viewModel.onSearchQueryChanged("new customer")
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, never()).searchCustomerListWithEmail(
                eq("new customer"),
                any(),
                any(),
                any(),
            )
            assertThat(viewModel.viewState.value?.searchQuery).isEqualTo("new customer")

            settingsGate.complete(WooResult(false))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            assertThat(states).noneMatch {
                it.body is CustomerListViewState.CustomerList.AnalyticsUnavailable
            }
            verify(customerListRepository).searchCustomerListWithEmail(
                eq("new customer"),
                eq(CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_ALL),
                any(),
                eq(1),
            )
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN),
                any(),
            )
        }

    @Test
    fun `given page two returns rest no route, when Retry is tapped, then replace list and reload page one`() =
        testBlocking {
            // GIVEN
            val retryGate = CompletableDeferred<Result<List<WCCustomerModel>>>()
            var customerRequestCount = 0
            whenever(customerListRepository.getCustomerList(any())).thenReturn(listOf(WCCustomerModel()))
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .doSuspendableAnswer {
                    when (customerRequestCount++) {
                        0 -> Result.success((1..30).map { WCCustomerModel() })
                        1 -> restNoRouteFailure()
                        else -> retryGate.await()
                    }
                }
            whenever(customerListRepository.fetchAnalyticsEnabled()).thenReturn(WooResult(true))
            val viewModel = initViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onEndOfListReached()
            advanceUntilIdle()

            // THEN
            val body = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable
            assertThat(body.message).isEqualTo(R.string.customer_list_analytics_unavailable_message)
            assertThat(body.canEnableAnalytics).isFalse()
            assertThat(viewModel.viewState.value?.showFab).isTrue()
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "other")

            viewModel.onEndOfListReached()
            advanceUntilIdle()
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(any(), any(), any(), any())

            viewModel.onRetryClicked()
            advanceUntilIdle()
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS),
                any(),
            )
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                eq(""),
                eq(CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_ALL),
                any(),
                eq(1),
            )
            verify(customerListRepository, times(1)).getCustomerList(any())

            retryGate.complete(Result.success(listOf(WCCustomerModel())))
            advanceUntilIdle()
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            assertAnalyticsReloadSuccess(source = "order_form", type = "retry")
        }

    @Test
    fun `given pagination settings check is pending, when search mode changes, then discard uncanceled classification`() =
        testBlocking {
            // GIVEN
            givenLegacySearchModes()
            whenever(customerListRepository.searchCustomerListWithEmail("customer", "name", 30, 1))
                .thenReturn(Result.success((1..30).map { WCCustomerModel() }))
            whenever(customerListRepository.searchCustomerListWithEmail("customer", "name", 30, 2))
                .thenReturn(restNoRouteFailure())
            whenever(customerListRepository.searchCustomerListWithEmail("customer", "email", 30, 1))
                .thenReturn(Result.success(listOf(WCCustomerModel())))
            val settingsGate = CompletableDeferred<WooResult<Boolean>>()
            val settingsStarted = CompletableDeferred<Unit>()
            var settingsReturned = false
            whenever(customerListRepository.fetchAnalyticsEnabled()).doSuspendableAnswer {
                settingsStarted.complete(Unit)
                settingsGate.await().also { settingsReturned = true }
            }
            val viewModel = initViewModel(
                searchQuery = "customer",
                searchModeId = R.string.order_creation_customer_search_name,
            )
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("customer")
            advanceUntilIdle()
            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onEndOfListReached()
            advanceUntilIdle()
            assertThat(settingsStarted.isCompleted).isTrue()
            viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_email)
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.value?.searchQuery).isEqualTo("customer")
            assertThat(viewModel.viewState.value?.searchModes?.single { it.isSelected }?.searchParam).isEqualTo("email")
            verify(customerListRepository, never()).searchCustomerListWithEmail("customer", "email", 30, 1)

            settingsGate.complete(WooResult(true))
            advanceUntilIdle()

            assertThat(settingsReturned).isTrue()
            assertThat(states).noneMatch {
                it.body is CustomerListViewState.CustomerList.AnalyticsUnavailable
            }
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            verify(customerListRepository).searchCustomerListWithEmail("customer", "email", 30, 1)
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN),
                any(),
            )
        }

    @Test
    fun `given Retry check is pending, when search mode changes, then cancel Retry before reloading`() =
        testBlocking {
            val retrySettingsGate = CompletableDeferred<WooResult<Boolean>>()
            val retrySettingsStarted = CompletableDeferred<Unit>()
            var settingsReadCount = 0
            whenever(customerListRepository.fetchAnalyticsEnabled()).doSuspendableAnswer {
                when (settingsReadCount++) {
                    0 -> WooResult(false)
                    1 -> {
                        retrySettingsStarted.complete(Unit)
                        retrySettingsGate.await()
                    }
                    else -> WooResult(true)
                }
            }
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any())).thenReturn(
                restNoRouteFailure(),
                restNoRouteFailure(),
                restNoRouteFailure(),
            )
            givenLegacySearchModes()
            val viewModel = initViewModel(
                searchQuery = "customer",
                searchModeId = R.string.order_creation_customer_search_name,
            )
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("customer")
            advanceUntilIdle()

            viewModel.onRetryClicked()
            advanceUntilIdle()
            assertThat(retrySettingsStarted.isCompleted).isTrue()
            viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_email)
            advanceUntilIdle()

            verify(customerListRepository, times(2)).searchCustomerListWithEmail("customer", "name", 30, 1)
            verify(customerListRepository).searchCustomerListWithEmail("customer", "email", 30, 1)
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS),
                any(),
            )
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "other")
        }

    @Test
    fun `given unavailable customers and nonempty search, when Retry succeeds, then retain query and track reload success`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute(
                settingsResult = WooResult(false),
                subsequentCustomerResults = listOf(Result.success(listOf(WCCustomerModel()))),
            )
            givenLegacySearchModes()
            val viewModel = initViewModel(
                searchQuery = "search customer",
                searchModeId = R.string.order_creation_customer_search_email,
            )
            viewModel.onSearchQueryChanged("search customer")
            advanceUntilIdle()

            // WHEN
            viewModel.onRetryClicked()
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                eq("search customer"),
                eq("email"),
                any(),
                eq(1),
            )
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            assertAnalyticsReloadSuccess(source = "order_form", type = "retry")
        }

    @Test
    fun `given Enable returns false, when confirmation completes, then keep Retry and show an error`() = testBlocking {
        // GIVEN
        givenMissingCustomerRoute(settingsResult = WooResult(false))
        whenever(customerListRepository.enableAnalytics()).thenReturn(WooResult(false))
        val viewModel = initViewModel()
        advanceUntilIdle()
        val originalBody = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable

        // WHEN
        viewModel.onEnableAnalyticsClicked()
        val dialog = viewModel.event.value as MultiLiveEvent.Event.ShowDialog
        dialog.positiveBtnAction?.onClick(null, 0)
        advanceUntilIdle()

        // THEN
        val body = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable
        assertThat(body.canEnableAnalytics).isTrue()
        assertThat(body).isEqualTo(originalBody)
        assertThat(viewModel.event.value)
            .isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.customer_list_enable_analytics_error))
        verify(analyticsTrackerWrapper, never()).track(
            eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS),
            any(),
        )
    }

    @Test
    fun `given old WooCommerce and a save in progress, when search is cleared, then preserve loading state and snackbar`() =
        testBlocking {
            // GIVEN
            givenLegacySearchModes()
            givenMissingCustomerRoute(settingsResult = WooResult(false))
            val writeGate = CompletableDeferred<WooResult<Boolean>>()
            whenever(customerListRepository.enableAnalytics()).doSuspendableAnswer { writeGate.await() }
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged("customer")
            advanceUntilIdle()

            // WHEN
            viewModel.onEnableAnalyticsClicked()
            val dialog = viewModel.event.value as MultiLiveEvent.Event.ShowDialog
            dialog.positiveBtnAction?.onClick(null, 0)
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)

            viewModel.onSearchQueryChanged("")

            // THEN
            assertThat(viewModel.viewState.value?.searchQuery).isEmpty()
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)

            writeGate.complete(WooResult(false))
            advanceUntilIdle()

            assertThat(viewModel.event.value)
                .isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.customer_list_enable_analytics_error))
            assertThat(viewModel.viewState.value?.searchQuery).isEmpty()
            verify(customerListRepository, times(1)).searchCustomerListWithEmail(any(), any(), any(), any())
        }

    @Test
    fun `given Analytics save is pending and query changes, when save succeeds, then reload latest query`() =
        testBlocking {
            givenMissingCustomerRoute(
                settingsResult = WooResult(false),
                subsequentCustomerResults = listOf(Result.success(listOf(WCCustomerModel()))),
            )
            val writeGate = CompletableDeferred<WooResult<Boolean>>()
            val writeStarted = CompletableDeferred<Unit>()
            whenever(customerListRepository.enableAnalytics()).doSuspendableAnswer {
                writeStarted.complete(Unit)
                writeGate.await()
            }
            val viewModel = initViewModel()
            advanceUntilIdle()

            viewModel.onEnableAnalyticsClicked()
            val dialog = viewModel.event.value as MultiLiveEvent.Event.ShowDialog
            assertThat(dialog.titleId).isEqualTo(R.string.customer_list_enable_analytics_confirmation_title)
            assertThat(dialog.messageId).isEqualTo(R.string.customer_list_enable_analytics_confirmation_message)
            assertThat(dialog.positiveButtonId)
                .isEqualTo(R.string.customer_list_enable_analytics_confirmation_action)
            assertThat(dialog.negativeButtonId).isEqualTo(R.string.cancel)
            verify(customerListRepository, never()).enableAnalytics()
            dialog.positiveBtnAction?.onClick(null, 0)
            advanceUntilIdle()
            assertThat(writeStarted.isCompleted).isTrue()
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)
            assertThat(viewModel.viewState.value?.showFab).isTrue()
            viewModel.onRetryClicked()
            viewModel.onEnableAnalyticsClicked()
            dialog.positiveBtnAction?.onClick(null, 0)
            advanceUntilIdle()
            verify(customerListRepository, times(1)).enableAnalytics()

            viewModel.onSearchQueryChanged("first query")
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("latest query")
            advanceUntilIdle()

            assertThat(viewModel.viewState.value?.searchQuery).isEqualTo("latest query")
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)
            verify(customerListRepository, times(1)).searchCustomerListWithEmail("", "all", 30, 1)

            writeGate.complete(WooResult(true))
            advanceUntilIdle()

            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(any(), any(), any(), any())
            verify(customerListRepository).searchCustomerListWithEmail("latest query", "all", 30, 1)
            assertAnalyticsReloadSuccess(source = "order_form", type = "enable_analytics")
            verify(customerListRepository, times(1)).enableAnalytics()
        }

    @Test
    fun `given Analytics save is pending and query is queued, when save fails, then fetch latest query`() =
        testBlocking {
            givenMissingCustomerRoute(
                settingsResult = WooResult(false),
                subsequentCustomerResults = listOf(Result.success(listOf(WCCustomerModel()))),
            )
            val writeGate = CompletableDeferred<WooResult<Boolean>>()
            val writeStarted = CompletableDeferred<Unit>()
            whenever(customerListRepository.enableAnalytics()).doSuspendableAnswer {
                writeStarted.complete(Unit)
                writeGate.await()
            }
            val viewModel = initViewModel()
            advanceUntilIdle()
            val states = viewModel.viewState.captureValues()

            viewModel.onEnableAnalyticsClicked()
            val dialog = viewModel.event.value as MultiLiveEvent.Event.ShowDialog
            dialog.positiveBtnAction?.onClick(null, 0)
            advanceUntilIdle()
            assertThat(writeStarted.isCompleted).isTrue()
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)

            viewModel.onSearchQueryChanged("first query")
            advanceUntilIdle()
            viewModel.onSearchQueryChanged("latest query")
            advanceUntilIdle()
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Loading)
            verify(customerListRepository, times(1)).searchCustomerListWithEmail("", "all", 30, 1)

            writeGate.complete(
                WooResult(
                    WooError(WooErrorType.API_ERROR, BaseRequest.GenericErrorType.NETWORK_ERROR)
                )
            )
            advanceUntilIdle()

            assertThat(viewModel.event.value)
                .isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.customer_list_enable_analytics_error))
            assertThat(
                states.count { state ->
                    state.body is CustomerListViewState.CustomerList.AnalyticsUnavailable
                }
            ).isGreaterThanOrEqualTo(2)
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(any(), any(), any(), any())
            verify(customerListRepository).searchCustomerListWithEmail("latest query", "all", 30, 1)
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS),
                any(),
            )
        }

    @Test
    fun `given enable reload still returns rest no route, when Retry follows, then copy override is request only`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute(
                settingsResult = WooResult(false),
                subsequentCustomerResults = listOf(restNoRouteFailure(), restNoRouteFailure()),
            )
            whenever(customerListRepository.enableAnalytics()).thenReturn(WooResult(true))
            val viewModel = initViewModel()
            advanceUntilIdle()

            // WHEN
            viewModel.onEnableAnalyticsClicked()
            val dialog = viewModel.event.value as MultiLiveEvent.Event.ShowDialog
            dialog.positiveBtnAction?.onClick(null, 0)
            advanceUntilIdle()

            // THEN
            val immediateBody = viewModel.viewState.value?.body as
                CustomerListViewState.CustomerList.AnalyticsUnavailable
            assertThat(immediateBody.message).isEqualTo(R.string.customer_list_analytics_unavailable_message)
            assertThat(immediateBody.canEnableAnalytics).isFalse()
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "disabled", count = 2)

            viewModel.onRetryClicked()
            advanceUntilIdle()
            val retryBody = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable
            assertThat(retryBody.message).isEqualTo(R.string.customer_list_analytics_disabled_message)
            assertThat(retryBody.canEnableAnalytics).isTrue()
            assertAnalyticsUnavailableEvent(source = "order_form", cause = "disabled", count = 3)
            verify(analyticsTrackerWrapper, never()).track(
                eq(AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS),
                any(),
            )
        }

    @Test
    fun `given repeated route failures, when Retry and search fail, then unavailable events count each result`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute(
                settingsResult = WooResult(false),
                subsequentCustomerResults = listOf(
                    restNoRouteFailure(),
                    restNoRouteFailure(),
                    restNoRouteFailure(),
                    Result.success(emptyList()),
                ),
            )
            val viewModel = initViewModel(allowCustomerCreation = false)
            advanceUntilIdle()
            assertAnalyticsUnavailableEvent(source = "order_list_filter", cause = "disabled", count = 1)

            // WHEN
            viewModel.onRetryClicked()
            advanceUntilIdle()
            assertAnalyticsUnavailableEvent(source = "order_list_filter", cause = "disabled", count = 2)
            viewModel.onRetryClicked()
            advanceUntilIdle()
            assertAnalyticsUnavailableEvent(source = "order_list_filter", cause = "disabled", count = 3)
            viewModel.onSearchQueryChanged("search query")
            advanceUntilIdle()

            // THEN
            assertAnalyticsUnavailableEvent(source = "order_list_filter", cause = "disabled", count = 4)
            assertThat(viewModel.viewState.value?.body).isEqualTo(
                CustomerListViewState.CustomerList.AnalyticsUnavailable(
                    message = R.string.customer_list_analytics_disabled_message,
                    canEnableAnalytics = true,
                )
            )
            verify(customerListRepository).searchCustomerListWithEmail(
                eq("search query"),
                eq(CustomerListGetSupportedSearchModes.SEARCH_MODE_VALUE_ALL),
                any(),
                eq(1),
            )
            viewModel.onRetryClicked()
            advanceUntilIdle()
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Empty::class.java)
            assertAnalyticsReloadSuccess(source = "order_list_filter", type = "retry")
        }

    @Test
    fun `given empty list from repo, when viewmodel init, then viewstate is updated with empty state`() = testBlocking {
        // GIVEN
        whenever(
            customerListRepository.searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
        ).thenReturn(Result.success(emptyList()))

        // WHEN
        val viewModel = initViewModel()
        val states = viewModel.viewState.captureValues()
        advanceUntilIdle()

        // THEN
        val last = states.last()
        assertThat(last.showFab).isEqualTo(false)
        val emptyState = last.body as CustomerListViewState.CustomerList.Empty
        assertThat(emptyState.message)
            .isEqualTo(R.string.order_creation_customer_search_empty)
        assertThat(emptyState.message).isEqualTo(R.string.order_creation_customer_search_empty)
        assertThat(emptyState.image).isEqualTo(R.drawable.img_empty_search)
        assertThat(emptyState.button?.text).isEqualTo(
            R.string.order_creation_customer_search_empty_add_details_manually
        )
    }

    @Test
    fun `given empty list from repo and email search, when viewmodel init, then viewstate is updated with empty state with button`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success(emptyList()))

            val email = "email@a8c.com"
            whenever(stringUtils.isValidEmail(email)).thenReturn(true)

            // WHEN
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged(email)
            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()

            // THEN
            val last = states.last()
            assertThat(last.showFab).isEqualTo(false)
            val emptyState = last.body as CustomerListViewState.CustomerList.Empty
            assertThat(emptyState.message)
                .isEqualTo(R.string.order_creation_customer_search_empty)
            assertThat(emptyState.message).isEqualTo(R.string.order_creation_customer_search_empty)
            assertThat(emptyState.image).isEqualTo(R.drawable.img_empty_search)
            assertThat(emptyState.button?.text).isEqualTo(
                R.string.order_creation_customer_search_empty_add_details_manually_with_email
            )
        }

    @Test
    fun `given empty list from repo and email search, when add customer manually clicked, then event has email`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success(emptyList()))

            val email = "email@a8c.com"
            whenever(stringUtils.isValidEmail(email)).thenReturn(true)

            // WHEN
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged(email)
            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()
            (states.last().body as CustomerListViewState.CustomerList.Empty).button!!.onClick()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(AddCustomer(email))
        }

    @Test
    fun `given empty list from repo and non email search, when add customer manually clicked, then event doesn't have email`() =
        testBlocking {
            // GIVEN
            whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success(emptyList()))

            val search = ""
            whenever(stringUtils.isValidEmail(search)).thenReturn(false)

            // WHEN
            val viewModel = initViewModel()
            viewModel.onSearchQueryChanged(search)
            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()
            (states.last().body as CustomerListViewState.CustomerList.Empty).button!!.onClick()

            // THEN
            assertThat(viewModel.event.value).isEqualTo(AddCustomer(null))
        }

    @Test
    fun `given search query, when onSearchQueryChanged is called, then update search query is updated and tracked`() =
        testBlocking {
            // GIVEN
            val searchQuery = "customer"
            val viewModel = initViewModel()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)

            // THEN
            assertThat(viewModel.viewState.value?.searchQuery).isEqualTo(searchQuery)
        }

    @Test
    fun `when onSearchQueryChanged is called, then tracked with all`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchQueryChanged("customer")

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "all"
            )
        )
    }

    @Test
    fun `when onSearchTypeChanged is called with email, then tracked with email`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_email)

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "email"
            )
        )
    }

    @Test
    fun `when onSearchTypeChanged is called with name, then tracked with name`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_name)

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "name"
            )
        )
    }

    @Test
    fun `when onSearchTypeChanged is called with username, then tracked with username`() = testBlocking {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onSearchTypeChanged(R.string.order_creation_customer_search_username)

        // THEN
        analyticsTrackerWrapper.track(
            AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH,
            mapOf(
                "search_type" to "username"
            )
        )
    }

    @Test
    fun `given search query and not advanced search, when onSearchQueryChanged is called, then search is invoked and viewstate updated with customers`() =
        testBlocking {
            // GIVEN
            val searchQuery = "customer"
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(false)).thenReturn(searchModes)
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(1)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
            assertThat((states.last().body as CustomerListViewState.CustomerList.Loaded).customers)
                .isEqualTo(listOf(mockCustomer))
        }

    @Test
    fun `given empty search and not advanced search, when onSearchQueryChanged is called, then search is not invoked and viewstate not advanced search`() =
        testBlocking {
            // GIVEN
            val searchQuery = ""
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(false)).thenReturn(searchModes)
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, never()).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
            assertThat((states.last().body as CustomerListViewState.CustomerList.Empty).message)
                .isEqualTo(R.string.order_creation_customer_search_empty_on_old_version_wcpay)
        }

    @Test
    fun `given empty search and advanced search, when onSearchQueryChanged is called, then search is invoked and viewstate updated`() =
        testBlocking {
            // GIVEN
            val searchQuery = ""
            whenever(isAdvancedSearchSupported.invoke()).thenReturn(true)
            val searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = true,
                )
            )
            whenever(getSupportedSearchModes.invoke(true)).thenReturn(searchModes)
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                eq(searchQuery),
                eq("all"),
                any(),
                any(),
            )
            assertThat((states.last().body as CustomerListViewState.CustomerList.Loaded).customers)
                .isEqualTo(listOf(mockCustomer))
        }

    @Test
    fun `given search query, when onSearchQueryChanged is called, then search is invoked and viewstate updated with customers`() =
        testBlocking {
            // GIVEN
            val searchQuery = "customer"
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged(searchQuery)
            advanceUntilIdle()

            // THEN
            assertThat((states.last().body as CustomerListViewState.CustomerList.Loaded).customers)
                .isEqualTo(listOf(mockCustomer))
        }

    @Test
    fun `given search type, when onSearchTypeChanged is called, then selected mode is changed`() =
        testBlocking {
            // GIVEN
            val searchTypeId = R.string.order_creation_customer_search_email
            val viewModel = initViewModel()

            // WHEN
            viewModel.onSearchTypeChanged(searchTypeId)

            // THEN
            assertThat(viewModel.viewState.value?.searchModes?.find { it.isSelected }?.labelResId).isEqualTo(
                searchTypeId
            )
            assertThat(viewModel.viewState.value?.searchModes?.find { it.isSelected }?.labelResId).isNotEqualTo(
                R.string.order_creation_customer_search_name
            )
        }

    @Test
    fun `given search type and empty search, when onSearchTypeChanged is called, then new customers are not loaded`() =
        testBlocking {
            // GIVEN
            val searchTypeId = R.string.order_creation_customer_search_email
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchTypeChanged(searchTypeId)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(1)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
        }

    @Test
    fun `given search type and search is not empty, when onSearchTypeChanged is called, then job canceled and new customers are loaded`() =
        testBlocking {
            // GIVEN
            val searchTypeId = R.string.order_creation_customer_search_email
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onSearchQueryChanged("customer")
            viewModel.onSearchTypeChanged(searchTypeId)
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
            assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
        }

    @Test
    fun `given less than page returned from repo, when onEndOfListReached, then next call doesn't load more customers`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()

            // WHEN
            viewModel.onEndOfListReached()
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(1)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun `given that one page size returned from repo, when onEndOfListReached, then next call load more customers`() =
        testBlocking {
            // GIVEN
            whenever(
                customerListRepository.searchCustomerListWithEmail(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            ).thenReturn(Result.success((1..30).map { mock() }))
            val viewModel = initViewModel()

            // WHEN
            viewModel.onEndOfListReached()
            advanceUntilIdle()

            // THEN
            verify(customerListRepository, times(2)).searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
        }

    @Test
    fun `given that one page size returned from repo, when onEndOfListReached, then loading item appended`() =
        testBlocking {
            // GIVEN
            whenever(
                customerListRepository.searchCustomerListWithEmail(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            ).thenReturn(Result.success((1..30).map { mock() }))
            val viewModel = initViewModel()

            val states = viewModel.viewState.captureValues()
            advanceUntilIdle()

            // WHEN
            viewModel.onEndOfListReached()

            // THEN
            assertThat((states.last().body as CustomerListViewState.CustomerList.Loaded).customers.last())
                .isInstanceOf(CustomerListViewState.CustomerList.Item.Loading::class.java)
            assertThat(
                (states.last().body as CustomerListViewState.CustomerList.Loaded).shouldResetScrollPosition
            ).isFalse()
        }

    @Test
    fun `given customer with remote id and fetching success, when onCustomerSelected, then customer fetched by id and passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(1L))
            }

            val returnedWcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(2L))
            }
            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(WooResult(returnedWcCustomer))

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(any()))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(any()))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, times(1)).fetchCustomerByRemoteId(1L)
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    Order.Customer(
                        customerId = 2L,
                        billingAddress = address,
                        shippingAddress = address,
                    )
                )
            )
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED,
                mapOf("is_customer_registered" to "true")
            )
        }

    @Test
    fun `given customer with remote id, when onCustomerSelected, then partialLoading state emitted`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(1L))
            }

            val returnedWcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(2L))
            }
            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(WooResult(returnedWcCustomer))

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(any()))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(any()))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            val states = viewModel.viewState.captureValues()

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            assertThat(states[0].partialLoading).isFalse
            assertThat(states[1].partialLoading).isTrue
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED,
                mapOf("is_customer_registered" to "true")
            )
        }

    @Test
    fun `given customer with remote id and fetching null, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(1L))
            }

            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(WooResult(null))

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(wcCustomer))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(wcCustomer))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, times(1)).fetchCustomerByRemoteId(1L)
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    Order.Customer(
                        customerId = 1L,
                        billingAddress = address,
                        shippingAddress = address,
                    )
                )
            )
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED,
                mapOf("is_customer_registered" to "true")
            )
        }

    @Test
    fun `given customer with remote id and fetching error, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(1L))
            }

            whenever(customerListRepository.fetchCustomerByRemoteId(any()))
                .thenReturn(
                    WooResult(
                        WooError(
                            WooErrorType.GENERIC_ERROR,
                            BaseRequest.GenericErrorType.NETWORK_ERROR
                        )
                    )
                )

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(wcCustomer))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(wcCustomer))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, times(1)).fetchCustomerByRemoteId(1L)
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    Order.Customer(
                        customerId = 1L,
                        billingAddress = address,
                        shippingAddress = address,
                    )
                )
            )
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED,
                mapOf("is_customer_registered" to "true")
            )
        }

    @Test
    fun `given customer without remote id, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(RemoteId(0L))
            }

            val billingAddress: OrderAddress.Billing = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }
            val shippingAddress: OrderAddress.Shipping = mock {
                on { country }.thenReturn("US")
                on { state }.thenReturn("CA")
            }

            whenever(customerListViewModelMapper.mapFromCustomerModelToBillingAddress(wcCustomer))
                .thenReturn(billingAddress)
            whenever(customerListViewModelMapper.mapFromCustomerModelToShippingAddress(wcCustomer))
                .thenReturn(shippingAddress)

            val state: Location = mock()
            whenever(customerListRepository.getState("US", "CA")).thenReturn(state)

            val country: Location = mock()
            whenever(customerListRepository.getCountry("US")).thenReturn(country)

            val address: Address = mock()
            whenever(customerListViewModelMapper.mapFromOrderAddressToAddress(any(), eq(country), eq(state)))
                .thenReturn(address)

            // WHEN
            viewModel.onCustomerSelected(wcCustomer)

            // THEN
            verify(customerListRepository, never()).fetchCustomerByRemoteId(any())
            assertThat(viewModel.event.value).isEqualTo(
                CustomerSelected(
                    Order.Customer(
                        customerId = 0L,
                        billingAddress = address,
                        shippingAddress = address,
                    )
                )
            )
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED,
                mapOf("is_customer_registered" to "false")
            )
        }

    @Test
    fun `when onAddCustomerClicked, then AddCustomer event triggered and event tracked`() {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onAddCustomerClicked()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADD_MANUALLY_TAPPED)
        assertThat(viewModel.event.value).isEqualTo(
            AddCustomer(null)
        )
    }

    @Test
    fun `when onNavigateBack, then exit emitted`() {
        // GIVEN
        val viewModel = initViewModel()

        // WHEN
        viewModel.onNavigateBack()

        // THEN
        assertThat(viewModel.event.value).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    private fun initViewModel(
        allowCustomerCreation: Boolean = true,
        allowGuests: Boolean = true,
        searchQuery: String? = null,
        searchModeId: Int? = null,
    ): CustomerListSelectionViewModel {
        val savedState = OrderCustomerListFragmentArgs(
            allowCustomerCreation = allowCustomerCreation,
            allowGuests = allowGuests
        ).toSavedStateHandle().apply {
            searchQuery?.let { this["search_query"] = it }
            searchModeId?.let { this["search_mode"] = it }
        }
        return CustomerListSelectionViewModel(
            savedState = savedState,
            repository = customerListRepository,
            mapper = customerListViewModelMapper,
            isAdvancedSearchSupported = isAdvancedSearchSupported,
            getSupportedSearchModes = getSupportedSearchModes,
            analyticsTracker = analyticsTrackerWrapper,
            stringUtils = stringUtils,
        )
    }

    private suspend fun givenMissingCustomerRoute(
        settingsResult: WooResult<Boolean>,
        subsequentCustomerResults: List<Result<List<WCCustomerModel>>> = emptyList(),
    ) {
        whenever(customerListRepository.searchCustomerListWithEmail(any(), any(), any(), any()))
            .thenReturn(restNoRouteFailure(), *subsequentCustomerResults.toTypedArray())
        whenever(customerListRepository.fetchAnalyticsEnabled()).thenReturn(settingsResult)
    }

    private suspend fun givenLegacySearchModes() {
        whenever(isAdvancedSearchSupported.invoke()).thenReturn(false)
        whenever(getSupportedSearchModes.invoke(false))
            .thenReturn(CustomerListGetSupportedSearchModes()(false))
    }

    private fun assertAnalyticsUnavailableEvent(
        source: String,
        cause: String,
        count: Int = 1,
    ) {
        verify(analyticsTrackerWrapper, times(count)).track(
            AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to source,
                AnalyticsTracker.KEY_CAUSE to cause,
            )
        )
    }

    private fun assertAnalyticsReloadSuccess(source: String, type: String) {
        verify(analyticsTrackerWrapper).track(
            AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_RELOAD_SUCCESS,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to source,
                AnalyticsTracker.KEY_TYPE to type,
            )
        )
    }

    private fun restNoRouteFailure() = apiFailure("rest_no_route")

    private fun apiFailure(apiErrorCode: String) = Result.failure<List<WCCustomerModel>>(
        WooException(
            WooError(
                type = WooErrorType.API_NOT_FOUND,
                original = BaseRequest.GenericErrorType.NOT_FOUND,
                apiErrorCode = apiErrorCode,
            )
        )
    )
}
