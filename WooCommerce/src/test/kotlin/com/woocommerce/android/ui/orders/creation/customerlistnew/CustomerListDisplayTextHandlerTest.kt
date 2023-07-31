package com.woocommerce.android.ui.orders.creation.customerlistnew

import com.woocommerce.android.R
import com.woocommerce.android.ui.orders.creation.customerlistnew.CustomerListViewState.CustomerList.Item.Customer
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CustomerListDisplayTextHandlerTest {
    private val highlighter: CustomerListSearchResultsHighlighter = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val handler = CustomerListDisplayTextHandler(highlighter, resourceProvider)

    @Test
    fun `given search mode all, when invoke on name, then returned highlighted name field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.ALL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Name("name")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("name", 0, 4)
        whenever(highlighter.invoke("name", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(highlighted)
    }

    @Test
    fun `given search mode all, when invoke on email, then returned highlighted email field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.ALL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Email("email")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("email", 0, 5)
        whenever(highlighter.invoke("email", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(highlighted)
    }

    @Test
    fun `given search mode all, when invoke on username, then returned highlighted username field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.ALL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Username("username")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("username", 0, 8)
        whenever(highlighter.invoke("· username", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(highlighted)
    }

    @Test
    fun `given search mode name, when invoke on name, then returned highlighted name field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.NAME
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Name("name")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("name", 0, 4)
        whenever(highlighter.invoke("name", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(highlighted)
    }

    @Test
    fun `given search mode username, when invoke on username, then returned highlighted username with dot field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.USERNAME
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Username("username")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("username", 0, 8)
        whenever(highlighter.invoke("· username", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(highlighted)
    }

    @Test
    fun `given search mode email, when invoke on email, then returned highlighted email field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.EMAIL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Email("email")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("email", 0, 5)
        whenever(highlighter.invoke("email", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(highlighted)
    }

    @Test
    fun `given search mode email, when invoke on name, then returned not highlighted email field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.EMAIL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Name("name")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("email", 0, 5)
        whenever(highlighter.invoke("email", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Highlighted("name", 0, 0))
    }

    @Test
    fun `given search mode name, when invoke on username, then returned not highlighted name field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.NAME
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Username("username")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("name", 0, 4)
        whenever(highlighter.invoke("name", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Highlighted("· username", 0, 0))
    }

    @Test
    fun `given search mode username, when invoke on email, then returned not highlighted username field`() {
        // WHEN
        val searchType = CustomerListDisplayTextHandler.SearchType.USERNAME
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Email("email")
        val matchBy = "matchBy"
        val highlighted = Customer.Text.Highlighted("username", 0, 8)
        whenever(highlighter.invoke("· username", matchBy)).thenReturn(highlighted)

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Highlighted("email", 0, 0))
    }

    @Test
    fun `given empty name customer param, when invoke, then return placeholder from resources`() {
        // GIVEN
        val searchType = CustomerListDisplayTextHandler.SearchType.ALL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Name("")
        val matchBy = "matchBy"
        whenever(
            resourceProvider.getString(R.string.order_creation_customer_search_empty_name)
        ).thenReturn("placeholder")

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Placeholder("placeholder"))
    }

    @Test
    fun `given empty email customer param, when invoke, then return placeholder from resources`() {
        // GIVEN
        val searchType = CustomerListDisplayTextHandler.SearchType.ALL
        val searchParam = CustomerListDisplayTextHandler.CustomerParam.Email("")
        val matchBy = "matchBy"
        whenever(
            resourceProvider.getString(R.string.order_creation_customer_search_empty_email)
        ).thenReturn("placeholder")

        // WHEN
        val result = handler(searchParam, matchBy, searchType)

        // THEN
        assertThat(result).isEqualTo(Customer.Text.Placeholder("placeholder"))
    }
}
