package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import javax.inject.Inject

class CustomerListGetSupportedSearchModes @Inject constructor(
    private val advancedSearchSupported: CustomerListIsAdvancedSearchSupported,
) {
    suspend operator fun invoke() = if (advancedSearchSupported()) {
        listOf(allSearchMode) + supportedSearchModes
    } else {
        supportedSearchModes
    }

    private val supportedSearchModes = listOf(
        SearchMode(
            labelResId = R.string.order_creation_customer_search_name,
            searchParam = "name",
            isSelected = false,
        ),
        SearchMode(
            labelResId = R.string.order_creation_customer_search_email,
            searchParam = "email",
            isSelected = false,
        ),
        SearchMode(
            labelResId = R.string.order_creation_customer_search_username,
            searchParam = "username",
            isSelected = false,
        ),
    )

    private val allSearchMode = SearchMode(
        labelResId = R.string.order_creation_customer_search_everything,
        searchParam = "all",
        isSelected = false,
    )
}
