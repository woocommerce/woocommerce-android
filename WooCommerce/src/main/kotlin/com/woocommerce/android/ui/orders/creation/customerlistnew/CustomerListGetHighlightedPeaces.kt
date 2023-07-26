package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Customer
import javax.inject.Inject

class CustomerListGetHighlightedPeaces @Inject constructor() {
    operator fun invoke(data: Customer, query: String) =
        mutableListOf<Customer.HighlightedPeace>().apply {
            data.firstName.findMatch(query)?.let { add(Customer.HighlightedPeace.FirstName(it.first, it.second)) }
            data.lastName.findMatch(query)?.let { add(Customer.HighlightedPeace.LastName(it.first, it.second)) }
            data.email.findMatch(query)?.let { add(Customer.HighlightedPeace.Email(it.first, it.second)) }
            data.username.findMatch(query)?.let { add(Customer.HighlightedPeace.Username(it.first, it.second)) }
        }

    private fun String.findMatch(query: String): Pair<Int, Int>? {
        val index = indexOf(query, ignoreCase = true)
        return if (index != -1) {
            index to index + query.length
        } else {
            null
        }
    }
}
