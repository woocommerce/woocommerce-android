package com.woocommerce.android.ui.orders.creation.customerlist

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.customer.CustomerListViewModel
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

@HiltViewModel
class CustomerListSelectionViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: CustomerListRepository,
    private val mapper: CustomerListViewModelMapper,
    isAdvancedSearchSupported: CustomerListIsAdvancedSearchSupported,
    getSupportedSearchModes: CustomerListGetSupportedSearchModes,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    stringUtils: StringUtils,
) : CustomerListViewModel(
    savedState,
    repository,
    mapper,
    isAdvancedSearchSupported,
    getSupportedSearchModes,
    analyticsTracker,
    stringUtils
) {
    private var loadingMoreInfoAboutCustomerJob: Job? = null

    override fun onCustomerSelected(customerModel: WCCustomerModel) {
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
}
