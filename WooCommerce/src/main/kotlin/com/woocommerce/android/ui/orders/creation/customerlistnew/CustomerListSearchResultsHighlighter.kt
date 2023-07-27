package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Customer
import javax.inject.Inject

class CustomerListSearchResultsHighlighter @Inject constructor() {
    operator fun invoke(value: String, query: String) =
        value.findMatch(query).let { Customer.Text.Highlighted(value, it.first, it.second) }

    private fun String.findMatch(query: String): Pair<Int, Int> {
        if (query.length < 2) return 0 to 0

        val index = indexOf(query, ignoreCase = true)
        return if (index != -1) {
            index to index + query.length
        } else {
            0 to 0
        }
    }
}
