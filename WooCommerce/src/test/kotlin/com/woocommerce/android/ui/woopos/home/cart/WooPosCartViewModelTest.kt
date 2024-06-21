package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCartViewModelTest : BaseUnitTest() {
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val repository: WooPosCartRepository = mock()
    private val resourceProvider: ResourceProvider = mock()

    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = testBlocking {
        val product = WooPosCartListItem(productId = 23L, title = "title")
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ItemClickedInProductSelector(product.productId))

        )
        whenever(repository.getProductById(eq(product.productId))).thenReturn(
            ProductTestUtils.generateProduct(product.productId)
        )
        val sut = createSut()
        advanceUntilIdle()

        val itemsInCart = sut.state.value.itemsInCart
        assertEquals(1, itemsInCart.size)
        assertEquals(product.productId, itemsInCart.first().productId)
    }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should remove item from cart`() =
        testBlocking {
            val product = WooPosCartListItem(productId = 23L, title = "title")
            whenever(parentToChildrenEventReceiver.events)
                .thenReturn(flowOf(ParentToChildrenEvent.ItemClickedInProductSelector(product.productId)))
            whenever(repository.getProductById(eq(product.productId))).thenReturn(
                ProductTestUtils.generateProduct(productId = product.productId, productName = "title")
            )
            val sut = createSut()
            advanceUntilIdle()
            val itemsInCart = sut.state.value.itemsInCart
            assertEquals(1, itemsInCart.size)
            assertEquals(product.productId, itemsInCart.first().productId)

            sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(product))

            val itemsInCartAfterRemoveClicked = sut.state.value.itemsInCart
            assertEquals(0, itemsInCartAfterRemoveClicked.size)
        }

    private fun createSut(): WooPosCartViewModel {
        return WooPosCartViewModel(
            childrenToParentEventSender,
            parentToChildrenEventReceiver,
            repository,
            resourceProvider,
            savedState
        )
    }
}
