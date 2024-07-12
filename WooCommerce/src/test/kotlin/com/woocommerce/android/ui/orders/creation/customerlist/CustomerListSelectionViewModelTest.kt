package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
class CustomerListSelectionViewModelTest : BaseUnitTest() {
    private val customerListRepository: CustomerListRepository = mock {
        onBlocking {
            searchCustomerListWithEmail(
                any(),
                any(),
                any(),
                any(),
            )
        }.thenReturn(Result.success(listOf(mock())))

        onBlocking { getCustomerList(any()) }.thenReturn(emptyList())
    }
    private val mockCustomer: CustomerListViewState.CustomerList.Item.Customer = mock()
    private val customerListViewModelMapper: CustomerListViewModelMapper = mock {
        on { mapFromWCCustomerToItem(any(), any(), any()) }.thenReturn(mockCustomer)
    }
    private val getSupportedSearchModes: CustomerListGetSupportedSearchModes = mock {
        onBlocking { invoke(true) }.thenReturn(
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
        onBlocking { invoke() }.thenReturn(true)
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
        assertThat(states.last().body).isInstanceOf(CustomerListViewState.CustomerList.Error::class.java)
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
    fun `given empty list from repo and non email search, when add customer manually clicked, then event doesnt have email`() =
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
    fun `given less than page returned from repo, when onEndOfListReached, then next call doesnt load more customers`() =
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
                on { remoteCustomerId }.thenReturn(1L)
            }

            val returnedWcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(2L)
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
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer with remote id, when onCustomerSelected, then partialLoading state emitted`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
            }

            val returnedWcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(2L)
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
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer with remote id and fetching null, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
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
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer with remote id and fetching error, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(1L)
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
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        }

    @Test
    fun `given customer without remote id, when onCustomerSelected, then existing customer passed`() =
        testBlocking {
            // GIVEN
            val viewModel = initViewModel()
            val wcCustomer = mock<WCCustomerModel> {
                on { remoteCustomerId }.thenReturn(0L)
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
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
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

    private fun initViewModel() = CustomerListSelectionViewModel(
        savedState = CustomerListDialogFragmentArgs(
            allowCustomerCreation = true,
            allowGuests = true
        ).toSavedStateHandle(),
        repository = customerListRepository,
        mapper = customerListViewModelMapper,
        isAdvancedSearchSupported = isAdvancedSearchSupported,
        getSupportedSearchModes = getSupportedSearchModes,
        analyticsTracker = analyticsTrackerWrapper,
        stringUtils = stringUtils,
    )
}
