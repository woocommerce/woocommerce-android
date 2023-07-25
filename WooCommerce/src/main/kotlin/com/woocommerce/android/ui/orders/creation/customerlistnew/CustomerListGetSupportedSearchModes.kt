package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import javax.inject.Inject

class CustomerListGetSupportedSearchModes @Inject constructor() {
    operator fun invoke(isAdvancedSearchSupported: Boolean) =
        if (isAdvancedSearchSupported) {
            emptyList()
        } else {
            supportedSearchModes
        }

    private companion object {
        val supportedSearchModes = listOf(
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
    }
}
