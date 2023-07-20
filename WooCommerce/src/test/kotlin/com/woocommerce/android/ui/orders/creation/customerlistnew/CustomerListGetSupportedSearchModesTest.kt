package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerListGetSupportedSearchModesTest : BaseUnitTest() {

    private val advancedSearchSupported: CustomerListIsAdvancedSearchSupported = mock()
    private val action = CustomerListGetSupportedSearchModes(advancedSearchSupported)

    @Test
    fun `given advanced search not supported, when action invoked, then 3 search modes returned`() = testBlocking {
        // GIVEN
        whenever(advancedSearchSupported()).thenReturn(false)

        // WHEN
        val result = action()

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
    fun `given advanced search supported, when action invoked, then 4 search modes returned`() = testBlocking {
        // GIVEN
        whenever(advancedSearchSupported()).thenReturn(true)

        // WHEN
        val result = action()

        // THEN
        assertThat(result[0]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_everything,
                searchParam = "all",
                isSelected = false,
            )
        )
        assertThat(result[1]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_name,
                searchParam = "name",
                isSelected = false,
            )
        )
        assertThat(result[2]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_email,
                searchParam = "email",
                isSelected = false,
            )
        )
        assertThat(result[3]).isEqualTo(
            SearchMode(
                labelResId = R.string.order_creation_customer_search_username,
                searchParam = "username",
                isSelected = false,
            )
        )
    }
}
