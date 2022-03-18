package com.woocommerce.android.ui.orders.creation

import org.junit.Before
import org.junit.Test

class OrderCreationViewModelTest {
    private lateinit var sut: OrderCreationViewModel

    @Before
    fun setUp() {
    }

    @Test
    fun `when initializing the view model, then register the orderDraft flowState`() {

    }

    @Test
    fun `when decreasing product quantity to zero, then call the full product view`() {

    }

    @Test
    fun `when adding the very same product, then increase item quantity by one`() {

    }

    @Test
    fun `when adding customer address with empty shipping, then set shipping as billing`() {

    }

    @Test
    fun `when creating the order fails, then trigger Snackbar with fail message`() {

    }

    @Test
    fun `when creating the order succeed, then call Order details view`() {

    }

    @Test
    fun `when hitting the back button with changes done, then trigger discard warning dialog`() {

    }

    @Test
    fun `when editing a fee, then reuse the existent one with different value`() {

    }

    @Test
    fun `when editing a shipping fee, then reuse the existent one with different value`() {

    }
}
