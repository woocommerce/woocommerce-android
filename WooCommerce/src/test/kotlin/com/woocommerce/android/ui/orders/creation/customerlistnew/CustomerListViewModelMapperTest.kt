package com.woocommerce.android.ui.orders.creation.customerlistnew

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

class CustomerListViewModelMapperTest {
    @Test
    fun `when mapFromWCCustomer, then return view model customer model`() {
        // GIVEN
        val wcCustomerModel: WCCustomerModel = mock {
            on { remoteCustomerId }.thenReturn(1)
            on { firstName }.thenReturn("firstName")
            on { lastName }.thenReturn("lastName")
            on { email }.thenReturn("email")
        }

        // WHEN
        val result = CustomerListViewModelMapper().mapFromWCCustomerToItem(wcCustomerModel)

        // THEN
        assertThat(result.remoteId).isEqualTo(1)
        assertThat(result.firstName).isEqualTo("firstName")
        assertThat(result.lastName).isEqualTo("lastName")
        assertThat(result.email).isEqualTo("email")
    }
}
