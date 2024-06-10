package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.products.WooPosProductsListItem
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCartViewModelTest : BaseUnitTest() {
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() {
        val product = WooPosProductsListItem(productId = 1L, title = "", imageUrl = "")
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ItemClickedInProductSelector(product))

        )
        val sut = createSut()

        val itemsInCart = sut.state.value.itemsInCart
        assertEquals(1, itemsInCart.size)
        assertEquals(product, itemsInCart.first())
    }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should be cart`() = testBlocking {
        val product = WooPosProductsListItem(productId = 1L, title = "", imageUrl = "")
        whenever(
            parentToChildrenEventReceiver.events
        ).thenReturn(flowOf(ParentToChildrenEvent.ItemClickedInProductSelector(product)))

        val sut = createSut()
        val itemsInCart = sut.state.value.itemsInCart
        assertEquals(1, itemsInCart.size)
        assertEquals(product, itemsInCart.first())

        sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(product))

        val itemsInCartAfterRemoveClicked = sut.state.value.itemsInCart
        assertEquals(0, itemsInCartAfterRemoveClicked.size)
    }

    private fun createSut(): WooPosCartViewModel {
        return WooPosCartViewModel(
            childrenToParentEventSender,
            parentToChildrenEventReceiver,
            savedState
        )
    }
}
