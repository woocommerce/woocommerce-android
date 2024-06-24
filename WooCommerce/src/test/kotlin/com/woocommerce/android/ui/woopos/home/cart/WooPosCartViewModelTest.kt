package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
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
    private val resourceProvider: ResourceProvider = mock {
        on { getString(eq(R.string.woo_pos_items_in_cart), eq(1)) }.thenReturn("Items in cart: 1")
    }

    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = testBlocking {
        // GIVEN
        val product = WooPosCartListItem(productId = 23L, title = "title")

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(repository.getProductById(eq(product.productId))).thenReturn(
            ProductTestUtils.generateProduct(product.productId)
        )
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.productId)
        )

        // THEN
        val itemsInCart = states.last().itemsInCart
        assertEquals(1, itemsInCart.size)
        assertEquals(product.productId, itemsInCart.first().productId)
    }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should remove item from cart`() =
        testBlocking {
            // GIVEN
            val product = WooPosCartListItem(productId = 23L, title = "title")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(repository.getProductById(eq(product.productId))).thenReturn(
                ProductTestUtils.generateProduct(productId = product.productId, productName = "title")
            )
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.productId)
            )

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(product))

            // THEN
            val itemsInCartAfterRemoveClicked = states.last().itemsInCart
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
