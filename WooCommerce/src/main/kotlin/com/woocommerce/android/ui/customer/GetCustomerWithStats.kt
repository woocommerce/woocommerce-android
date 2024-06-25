package com.woocommerce.android.ui.customer

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetCustomerWithStats @Inject constructor(
    private val customerRepository: CustomerListRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val getLocations: GetLocations,
    private val dateUtils: DateUtils
) {
    suspend operator fun invoke(remoteCustomerId: Long) = coroutineScope {
        val customerDeferred = async { customerRepository.fetchCustomerByRemoteId(remoteCustomerId) }
        val analyticsCustomerDeferred =
            async { customerRepository.fetchCustomerFromAnalyticsByRemoteId(remoteCustomerId) }
        val customer = customerDeferred.await().model
        val analyticsCustomer = analyticsCustomerDeferred.await().model
        return@coroutineScope when {
            customer != null && analyticsCustomer != null -> {
                val (billingCountry, billingState) = getLocations(customer.billingCountry, customer.billingState)
                val (shippingCountry, shippingState) = getLocations(customer.shippingCountry, customer.shippingState)
                val customerResult = CustomerWithAnalytics(
                    remoteCustomerId = remoteCustomerId,
                    firstName = customer.firstName,
                    lastName = customer.lastName,
                    username = customer.username,
                    email = customer.email,
                    phone = customer.billingPhone,
                    lastActive = dateUtils
                        .getShortMonthDayAndYearStringFromFullIsoDate(analyticsCustomer.dateLastActive).orEmpty(),
                    ordersCount = analyticsCustomer.ordersCount,
                    totalSpend = currencyFormatter.formatAmountWithCurrency(analyticsCustomer.totalSpend),
                    averageOrderValue = currencyFormatter.formatAmountWithCurrency(analyticsCustomer.avgOrderValue),
                    registeredDate = dateUtils
                        .getShortMonthDayAndYearStringFromFullIsoDate(analyticsCustomer.dateRegistered).orEmpty(),
                    billingAddress = Address(
                        company = customer.billingCompany,
                        firstName = customer.firstName,
                        lastName = customer.lastName,
                        phone = customer.billingPhone,
                        country = billingCountry,
                        state = billingState,
                        address1 = customer.billingAddress1,
                        address2 = customer.billingAddress2,
                        city = customer.billingCity,
                        postcode = customer.billingPostcode,
                        email = customer.billingEmail
                    ),
                    shippingAddress = Address(
                        company = customer.shippingCompany,
                        firstName = customer.firstName,
                        lastName = customer.lastName,
                        phone = "",
                        country = shippingCountry,
                        state = shippingState,
                        address1 = customer.shippingAddress1,
                        address2 = customer.shippingAddress2,
                        city = customer.shippingCity,
                        postcode = customer.shippingPostcode,
                        email = ""
                    )
                )
                Result.success(customerResult)
            }

            else -> {
                Result.failure(Exception("Couldn't load the customer"))
            }
        }
    }
}
