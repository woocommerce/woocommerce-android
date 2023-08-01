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

    companion object {
        const val SEARCH_MODE_VALUE_ALL = "all"
        const val SEARCH_MODE_VALUE_NAME = "name"
        const val SEARCH_MODE_VALUE_EMAIL = "email"
        const val SEARCH_MODE_VALUE_USERNAME = "username"

        private val supportedSearchModes = listOf(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = SEARCH_MODE_VALUE_NAME,
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = SEARCH_MODE_VALUE_EMAIL,
                isSelected = false,
            ),
            SearchMode(
                labelResId = R.string.order_creation_customer_search_username,
                searchParam = SEARCH_MODE_VALUE_USERNAME,
                isSelected = false,
            ),
        )
    }
}
