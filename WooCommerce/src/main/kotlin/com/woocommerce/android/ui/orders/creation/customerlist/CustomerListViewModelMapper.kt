package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListDisplayTextHandler.CustomerParam
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.OrderAddress
import javax.inject.Inject

class CustomerListViewModelMapper @Inject constructor(
    private val textHandler: CustomerListDisplayTextHandler,
) {
    fun mapFromWCCustomerToItem(
        wcCustomerModel: WCCustomerModel,
        searchQuery: String,
        searchType: CustomerListDisplayTextHandler.SearchType,
    ) = CustomerListViewState.CustomerList.Item.Customer(
        remoteId = wcCustomerModel.remoteCustomerId,
        name = textHandler(CustomerParam.Name(wcCustomerModel.fullName), searchQuery, searchType),
        email = textHandler(CustomerParam.Email(wcCustomerModel.email), searchQuery, searchType),
        username = textHandler(CustomerParam.Username(wcCustomerModel.username), searchQuery, searchType),

        payload = wcCustomerModel,
    )

    fun mapFromOrderAddressToAddress(
        address: OrderAddress,
        country: Location,
        state: Location
    ) = Address(
        company = address.company,
        lastName = address.lastName,
        firstName = address.firstName,
        address1 = address.address1,
        address2 = address.address2,
        email = if (address is OrderAddress.Billing) {
            address.email
        } else {
            ""
        },
        postcode = address.postcode,
        phone = address.phone,
        country = country,
        state = if (state == Location.EMPTY) AmbiguousLocation.EMPTY else AmbiguousLocation.Defined(state),
        city = address.city
    )

    fun mapFromCustomerModelToShippingAddress(wcCustomer: WCCustomerModel) =
        OrderAddress.Shipping(
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

    fun mapFromCustomerModelToBillingAddress(wcCustomer: WCCustomerModel) =
        OrderAddress.Billing(
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

    private val WCCustomerModel.fullName: String
        get() = "$firstName $lastName"
}
