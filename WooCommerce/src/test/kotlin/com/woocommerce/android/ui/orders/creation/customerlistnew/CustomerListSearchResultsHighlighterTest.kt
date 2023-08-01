package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Customer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomerListSearchResultsHighlighterTest {
    private val highlighter: CustomerListSearchResultsHighlighter = CustomerListSearchResultsHighlighter()

    @Test
    fun `given unmatched query, when invoke, then return empty range`() {
        // GIVEN
        val value = "value"
        val query = "query"

        // WHEN
        val result = highlighter(value, query)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Highlighted(value, 0, 0))
    }

    @Test
    fun `given matched query, when invoke, then return range`() {
        // GIVEN
        val value = "value"
        val query = "val"

        // WHEN
        val result = highlighter(value, query)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Highlighted(value, 0, 3))
    }

    @Test
    fun `given matched query with matching end, when invoke, then return range`() {
        // GIVEN
        val value = "value"
        val query = "lUe"

        // WHEN
        val result = highlighter(value, query)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Highlighted(value, 2, 5))
    }
}
