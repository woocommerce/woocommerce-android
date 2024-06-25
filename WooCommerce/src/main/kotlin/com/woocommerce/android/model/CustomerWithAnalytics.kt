package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewModelMapper
import com.woocommerce.android.util.AddressUtils
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

@Parcelize
data class CustomerWithAnalytics(
    val remoteCustomerId: Long,
    val firstName: String,
    val lastName: String,
    val username: String,
    val email: String,
    val phone: String,
    val lastActive: String?,
    val ordersCount: Int?,
    val totalSpend: String?,
    val averageOrderValue: String?,
    val registeredDate: String,
    val billingAddress: Address,
    val shippingAddress: Address
) : Parcelable {
    fun getFullName() = "$firstName $lastName"
    fun getShippingAddress(): String {
        val shippingName = "$firstName $lastName"
        val shippingAddress = this.shippingAddress.getEnvelopeAddress()
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(this.shippingAddress.country.code)
        return this.shippingAddress.getFullAddress(
            shippingName,
            shippingAddress,
            shippingCountry
        )
    }

    fun getBillingAddress(): String {
        val shippingName = "$firstName $lastName"
        val shippingAddress = this.billingAddress.getEnvelopeAddress()
        val shippingCountry = AddressUtils.getCountryLabelByCountryCode(this.billingAddress.country.code)
        return this.billingAddress.getFullAddress(
            shippingName,
            shippingAddress,
            shippingCountry
        )
    }
}

fun WCCustomerModel.toCustomerWithAnalytics(
    repository: CustomerListRepository,
    mapper: CustomerListViewModelMapper,
): CustomerWithAnalytics {
    val billingAddress = mapper.mapFromCustomerModelToBillingAddress(this)
    val shippingAddress = mapper.mapFromCustomerModelToShippingAddress(this)

    val shippingCountry = repository.getCountry(shippingAddress.country)
    val shippingState = repository.getState(shippingAddress.country, shippingAddress.state)

    val billingCountry = repository.getCountry(billingAddress.country)
    val billingState = repository.getState(billingAddress.country, billingAddress.state)

    return CustomerWithAnalytics(
        remoteCustomerId = this.remoteCustomerId,
        firstName = this.firstName,
        lastName = this.lastName,
        username = this.username,
        email = this.email,
        phone = this.billingPhone,
        lastActive = null,
        ordersCount = null,
        totalSpend = null,
        averageOrderValue = null,
        registeredDate = this.dateCreated,
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
    )
}
