package com.woocommerce.android.ui.moremenu.customer

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListGetSupportedSearchModes
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModelMapper
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewState
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListDetailsViewModelTest : BaseUnitTest() {
    private val repository: CustomerListRepository = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    @Test
    fun `given Analytics route is missing, when More customers loads, then event source is customers`() = testBlocking {
        // GIVEN
        givenMissingCustomerRoute()

        // WHEN
        val viewModel = initViewModel()
        advanceUntilIdle()

        // THEN
        val body = viewModel.viewState.value?.body as CustomerListViewState.CustomerList.AnalyticsUnavailable
        assertThat(body.message).isEqualTo(R.string.customer_list_analytics_disabled_message)
        verify(analyticsTracker).track(
            AnalyticsEvent.CUSTOMER_LIST_ANALYTICS_UNAVAILABLE_ERROR_SHOWN,
            mapOf(
                AnalyticsTracker.KEY_SOURCE to "customers",
                AnalyticsTracker.KEY_CAUSE to "disabled",
            )
        )
    }

    @Test
    fun `given Analytics route is missing, when More customers loads, then Analytics is marked unavailable`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute()

            // WHEN
            val viewModel = initViewModel()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.value?.isAnalyticsUnavailable).isTrue()
        }

    @Test
    fun `given Analytics is unavailable, when Retry is loading, then Analytics stays unavailable`() = testBlocking {
        // GIVEN
        givenMissingCustomerRoute()
        val viewModel = initViewModel()
        advanceUntilIdle()
        val retryResult = CompletableDeferred<Result<List<WCCustomerModel>>>()
        whenever(repository.searchCustomerListWithEmail(any(), any(), any(), any()))
            .doSuspendableAnswer { retryResult.await() }

        // WHEN
        viewModel.onRetryClicked()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.viewState.value?.body).isEqualTo(CustomerListViewState.CustomerList.Loading)
        assertThat(viewModel.viewState.value?.isAnalyticsUnavailable).isTrue()
        retryResult.complete(Result.success(emptyList()))
    }

    @Test
    fun `given Analytics is unavailable, when Retry loads customers, then Analytics is no longer unavailable`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute()
            val viewModel = initViewModel()
            advanceUntilIdle()
            whenever(repository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success(listOf(WCCustomerModel())))

            // WHEN
            viewModel.onRetryClicked()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Loaded::class.java)
            assertThat(viewModel.viewState.value?.isAnalyticsUnavailable).isFalse()
        }

    @Test
    fun `given Analytics is unavailable, when Retry finds no customers, then Analytics is no longer unavailable`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute()
            val viewModel = initViewModel()
            advanceUntilIdle()
            whenever(repository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.success(emptyList()))

            // WHEN
            viewModel.onRetryClicked()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.value?.body)
                .isInstanceOf(CustomerListViewState.CustomerList.Empty::class.java)
            assertThat(viewModel.viewState.value?.isAnalyticsUnavailable).isFalse()
        }

    @Test
    fun `given Analytics is unavailable, when Retry fails with another error, then Analytics is no longer unavailable`() =
        testBlocking {
            // GIVEN
            givenMissingCustomerRoute()
            val viewModel = initViewModel()
            advanceUntilIdle()
            whenever(repository.searchCustomerListWithEmail(any(), any(), any(), any()))
                .thenReturn(Result.failure(Throwable()))

            // WHEN
            viewModel.onRetryClicked()
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.viewState.value?.body)
                .isEqualTo(CustomerListViewState.CustomerList.Error(R.string.error_generic))
            assertThat(viewModel.viewState.value?.isAnalyticsUnavailable).isFalse()
        }

    private suspend fun givenMissingCustomerRoute() {
        whenever(repository.getCustomerList(any())).thenReturn(emptyList())
        whenever(repository.searchCustomerListWithEmail(any(), any(), any(), any())).thenReturn(
            Result.failure(
                WooException(
                    WooError(
                        type = WooErrorType.API_NOT_FOUND,
                        original = BaseRequest.GenericErrorType.NOT_FOUND,
                        apiErrorCode = "rest_no_route",
                    )
                )
            )
        )
        whenever(repository.fetchAnalyticsEnabled()).thenReturn(WooResult(false))
    }

    private fun initViewModel() = CustomerListDetailsViewModel(
        savedState = SavedStateHandle(),
        repository = repository,
        mapper = mock<CustomerListViewModelMapper> {
            on { mapFromWCCustomerToItem(any(), any(), any()) }.thenReturn(
                CustomerListViewState.CustomerList.Item.Customer(
                    remoteId = 1L,
                    name = CustomerListViewState.CustomerList.Item.Customer.Text.Placeholder(""),
                    email = CustomerListViewState.CustomerList.Item.Customer.Text.Placeholder(""),
                    username = CustomerListViewState.CustomerList.Item.Customer.Text.Placeholder(""),
                    payload = WCCustomerModel(),
                )
            )
        },
        isAdvancedSearchSupported = mock {
            on { invoke() }.thenReturn(true)
        },
        getSupportedSearchModes = CustomerListGetSupportedSearchModes(),
        analyticsTracker = analyticsTracker,
        stringUtils = mock<StringUtils>(),
    )
}
