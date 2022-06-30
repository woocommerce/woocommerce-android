package com.woocommerce.android.ui.orders.creation.customerlist

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppConstants
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import javax.inject.Inject

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val customerListRepository: CustomerListRepository
) : ScopedViewModel(savedState) {
    private val _viewState = MutableLiveData<CustomerListViewState>()
    val viewState: LiveData<CustomerListViewState> = _viewState

    private var searchJob: Job? = null

    init {
        _viewState.value = CustomerListViewState()
        launch {
            customerListRepository.loadCountries()
        }
    }

    fun onCustomerClick(customerRemoteId: Long) {
        AnalyticsTracker.track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_ADDED)
        launch {
            customerListRepository.getCustomerByRemoteId(customerRemoteId)?.let { wcCustomer ->
                val shippingAddress = OrderAddress.Shipping(
                    company = wcCustomer.shippingCompany,
                    address1 = wcCustomer.shippingAddress1,
                    address2 = wcCustomer.shippingAddress2,
                    city = wcCustomer.shippingCity,
                    firstName = wcCustomer.shippingFirstName,
                    lastName = wcCustomer.shippingLastName,
                    country = wcCustomer.shippingCountry,
                    state = wcCustomer.shippingState,
                    postcode = wcCustomer.shippingPostcode,
                    phone = ""
                )
                val billingAddress = OrderAddress.Billing(
                    company = wcCustomer.billingCompany,
                    address1 = wcCustomer.billingAddress1,
                    address2 = wcCustomer.billingAddress2,
                    city = wcCustomer.billingCity,
                    firstName = wcCustomer.billingFirstName,
                    lastName = wcCustomer.billingLastName,
                    country = wcCustomer.billingCountry,
                    state = wcCustomer.billingState,
                    postcode = wcCustomer.billingPostcode,
                    phone = wcCustomer.billingPhone,
                    email = wcCustomer.billingEmail
                )

                val shippingCountry = customerListRepository.getCountry(shippingAddress.country)
                val shippingState = customerListRepository.getState(shippingAddress.country, shippingAddress.state)

                val billingCountry = customerListRepository.getCountry(billingAddress.country)
                val billingState = customerListRepository.getState(billingAddress.country, billingAddress.state)

                triggerEvent(
                    CustomerSelected(
                        shippingAddress = shippingAddress.toAddressModel(shippingCountry, shippingState),
                        billingAddress = billingAddress.toAddressModel(billingCountry, billingState)
                    )
                )
            }
        }
    }

    private fun OrderAddress.toAddressModel(
        country: Location,
        state: Location
    ): Address {
        return Address(
            company = company,
            lastName = lastName,
            firstName = firstName,
            address1 = address1,
            address2 = address2,
            email = if (this is OrderAddress.Billing) {
                this.email
            } else {
                ""
            },
            postcode = postcode,
            phone = phone,
            country = country,
            state = AmbiguousLocation.Defined(state),
            city = city
        )
    }

    fun onSearchQueryChanged(query: String?) {
        if (query != null && query.length > 2) {
            // cancel any existing search, then start a new one after a brief delay so we don't
            // actually perform the fetch until the user stops typing
            searchJob?.cancel()
            searchJob = launch {
                delay(AppConstants.SEARCH_TYPING_DELAY_MS)
                AnalyticsTracker.track(AnalyticsEvent.ORDER_CREATION_CUSTOMER_SEARCH)
                searchCustomerList(query)
            }
        } else {
            launch {
                searchJob?.cancelAndJoin()
                _viewState.value = _viewState.value?.copy(
                    customers = emptyList(),
                    searchQuery = "",
                )
            }
        }
    }

    private suspend fun searchCustomerList(query: String) {
        if (networkStatus.isConnected()) {
            _viewState.value = _viewState.value?.copy(
                isSkeletonShown = true,
                searchQuery = query
            )

            val customers = customerListRepository.searchCustomerList(query)?.map {
                it.toUiModel()
            } ?: emptyList()

            _viewState.value = _viewState.value?.copy(
                isSkeletonShown = false,
                customers = customers
            )
        } else {
            triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.offline_error))
        }
    }

    @Parcelize
    data class CustomerListItem(
        val remoteId: Long,
        val firstName: String,
        val lastName: String,
        val email: String,
        val avatarUrl: String
    ) : Parcelable

    @Parcelize
    data class CustomerListViewState(
        val customers: List<CustomerListItem> = emptyList(),
        val isSkeletonShown: Boolean = false,
        val searchQuery: String = ""
    ) : Parcelable

    data class CustomerSelected(
        val billingAddress: Address,
        val shippingAddress: Address
    ) : MultiLiveEvent.Event()
}

private fun WCCustomerModel.toUiModel() =
    CustomerListViewModel.CustomerListItem(
        remoteId = remoteCustomerId,
        firstName = firstName,
        lastName = lastName,
        email = email,
        avatarUrl = avatarUrl
    )
