package com.woocommerce.android.ui.customer

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalytics
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import javax.inject.Inject

class GetCustomerWithStats @Inject constructor(
    private val customerRepository: CustomerListRepository,
    private val currencyFormatter: CurrencyFormatter,
    private val getLocations: GetLocations,
    private val dateUtils: DateUtils
) {
    suspend operator fun invoke(remoteCustomerId: Long, analyticsCustomerId: Long?): Result<CustomerWithAnalytics> {
        val customer = if (remoteCustomerId != 0L) {
            customerRepository.getCustomerByRemoteId(remoteCustomerId)
        } else {
            null
        }

        val analyticsCustomer = analyticsCustomerId?.let {
            customerRepository.getCustomerByAnalyticsCustomerId(it)
        }

        return mergeResults(customer, analyticsCustomer)
    }

    private fun mergeResults(
        customer: WCCustomerModel?,
        analyticsCustomer: WCCustomerFromAnalytics?
    ): Result<CustomerWithAnalytics> {
        return when {
            customer != null && analyticsCustomer != null -> {
                val customerResult = customerWithBothValues(customer, analyticsCustomer)
                Result.success(customerResult)
            }

            customer != null && analyticsCustomer == null -> {
                val customerResult = customerWithOutAnalytics(customer)
                Result.success(customerResult)
            }

            customer == null && analyticsCustomer != null -> {
                val customerResult = analyticsWithoutCustomer(analyticsCustomer)
                Result.success(customerResult)
            }

            else -> {
                Result.failure(Exception("Couldn't load the customer"))
            }
        }
    }

    private fun customerWithBothValues(
        customer: WCCustomerModel,
        analyticsCustomer: WCCustomerFromAnalytics
    ): CustomerWithAnalytics {
        val (billingCountry, billingState) = getLocations(customer.billingCountry, customer.billingState)
        val (shippingCountry, shippingState) = getLocations(customer.shippingCountry, customer.shippingState)
        return CustomerWithAnalytics(
            remoteCustomerId = customer.remoteCustomerId,
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
            ),
            analyticsCustomerId = analyticsCustomer.id
        )
    }

    private fun customerWithOutAnalytics(customer: WCCustomerModel): CustomerWithAnalytics {
        val (billingCountry, billingState) = getLocations(customer.billingCountry, customer.billingState)
        val (shippingCountry, shippingState) = getLocations(customer.shippingCountry, customer.shippingState)
        return CustomerWithAnalytics(
            remoteCustomerId = customer.remoteCustomerId,
            firstName = customer.firstName,
            lastName = customer.lastName,
            username = customer.username,
            email = customer.email,
            phone = customer.billingPhone,
            lastActive = "",
            ordersCount = 0,
            totalSpend = currencyFormatter.formatAmountWithCurrency(0.0),
            averageOrderValue = currencyFormatter.formatAmountWithCurrency(0.0),
            registeredDate = dateUtils
                .getShortMonthDayAndYearStringFromFullIsoDate(customer.dateCreated).orEmpty(),
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
            ),
            analyticsCustomerId = null
        )
    }

    private fun analyticsWithoutCustomer(analyticsCustomer: WCCustomerFromAnalytics): CustomerWithAnalytics {
        val (country, state) = getLocations(analyticsCustomer.country, analyticsCustomer.state)
        val nameSplit = analyticsCustomer.name.split(" ")
        val firstName = nameSplit.firstOrNull().orEmpty()
        val lastName = nameSplit.lastOrNull().orEmpty()
        return CustomerWithAnalytics(
            remoteCustomerId = analyticsCustomer.userId,
            firstName = firstName,
            lastName = lastName,
            username = analyticsCustomer.username,
            email = analyticsCustomer.email,
            phone = "",
            lastActive = dateUtils.getShortMonthDayAndYearStringFromFullIsoDate(
                analyticsCustomer.dateLastActive
            ).orEmpty(),
            ordersCount = analyticsCustomer.ordersCount,
            totalSpend = currencyFormatter.formatAmountWithCurrency(analyticsCustomer.totalSpend),
            averageOrderValue = currencyFormatter.formatAmountWithCurrency(analyticsCustomer.avgOrderValue),
            registeredDate = dateUtils
                .getShortMonthDayAndYearStringFromFullIsoDate(analyticsCustomer.dateRegistered).orEmpty(),
            billingAddress = Address(
                company = "",
                firstName = firstName,
                lastName = lastName,
                phone = "",
                country = country,
                state = state,
                address1 = "",
                address2 = "",
                city = analyticsCustomer.city,
                postcode = analyticsCustomer.postcode,
                email = analyticsCustomer.email
            ),
            shippingAddress = Address(
                company = "",
                firstName = firstName,
                lastName = lastName,
                phone = "",
                country = country,
                state = state,
                address1 = "",
                address2 = "",
                city = analyticsCustomer.city,
                postcode = analyticsCustomer.postcode,
                email = analyticsCustomer.email
            ),
            analyticsCustomerId = analyticsCustomer.id
        )
    }
}
