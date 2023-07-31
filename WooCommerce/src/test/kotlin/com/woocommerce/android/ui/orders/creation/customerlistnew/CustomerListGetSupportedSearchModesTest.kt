package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListGetSupportedSearchModesTest : BaseUnitTest() {
    private val action = CustomerListGetSupportedSearchModes()

    @Test
    fun `given advanced search not supported, when action invoked, then 3 search modes returned`() = testBlocking {
        // GIVEN
        val isAdvancedSearchSupported = false

        // WHEN
        val result = action(isAdvancedSearchSupported)

        // THEN
        assertThat(result[0]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = "name",
                isSelected = false,
            )
        )
        assertThat(result[1]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = "email",
                isSelected = false,
            )
        )
        assertThat(result[2]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_username,
                searchParam = "username",
                isSelected = false,
            )
        )
    }

    @Test
    fun `given advanced search supported, when action invoked, then 0 search modes returned`() = testBlocking {
        // GIVEN
        val isAdvancedSearchSupported = true

        // WHEN
        val result = action(isAdvancedSearchSupported)

        // THEN
        assertThat(result).isEmpty()
    }
}
