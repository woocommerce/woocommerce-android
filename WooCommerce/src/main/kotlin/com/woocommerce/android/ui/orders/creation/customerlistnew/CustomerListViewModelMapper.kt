package com.woocommerce.android.ui.orders.creation.customerlistnew

import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

class CustomerListViewModelMapper @Inject constructor() {
    fun mapFromWCCustomer(wcCustomerModel: WCCustomerModel) =
        CustomerListViewState.CustomerList.Item.Customer(
            remoteId = wcCustomerModel.remoteCustomerId,
            firstName = wcCustomerModel.firstName,
            lastName = wcCustomerModel.lastName,
            email = wcCustomerModel.email,
        )
}
