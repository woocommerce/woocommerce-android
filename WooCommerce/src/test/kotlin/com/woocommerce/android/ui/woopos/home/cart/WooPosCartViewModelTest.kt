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
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
        on { events }.thenReturn(MutableSharedFlow())
    }
    private val repository: WooPosCartRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(eq(R.string.woo_pos_items_in_cart), eq(1)) }.thenReturn("Items in cart: 1")
    }

    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = testBlocking {
        // GIVEN
        val product = WooPosCartListItem(id = 23L, name = "title")

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(repository.getProductById(eq(product.id))).thenReturn(
            ProductTestUtils.generateProduct(product.id)
        )
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.id)
        )

        // THEN
        val itemsInCart = states.last().itemsInCart
        assertEquals(1, itemsInCart.size)
        assertEquals(product.id, itemsInCart.first().id)
    }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should remove item from cart`() =
        testBlocking {
            // GIVEN
            val product = WooPosCartListItem(id = 23L, name = "title")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(repository.getProductById(eq(product.id))).thenReturn(
                ProductTestUtils.generateProduct(productId = product.id, productName = "title")
            )
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.id)
            )

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(product))

            // THEN
            val itemsInCartAfterRemoveClicked = states.last().itemsInCart
            assertEquals(0, itemsInCartAfterRemoveClicked.size)
        }

    @Test
    fun `given empty cart in_progress, when vm created, then toolbar state should contain shopping cart empty itemsCart and no clear all button`() =
        testBlocking {
            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            // THEN
            val toolbar = states.last().toolbar
            assertEquals(R.drawable.ic_shopping_cart, toolbar.icon)
            assertEquals("", toolbar.itemsCount)
            assertEquals(false, toolbar.isClearAllButtonVisible)
        }

    @Test
    fun `given non empty cart in_progress, when vm created, then toolbar state should contain shopping cart itemsCart title and no clear all`() =
        testBlocking {
            // GIVEN
            val product = WooPosCartListItem(id = 23L, name = "title")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(repository.getProductById(eq(product.id))).thenReturn(
                ProductTestUtils.generateProduct(productId = product.id, productName = "title")
            )

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.id)
            )

            // THEN
            val toolbar = states.last().toolbar
            assertEquals(R.drawable.ic_shopping_cart, toolbar.icon)
            assertEquals("Items in cart: 1", toolbar.itemsCount)
            assertEquals(true, toolbar.isClearAllButtonVisible)
        }

    @Test
    fun `given non empty cart checkout, when vm created, then toolbar state should contain back icon itemsCart title and no clear all`() =
        testBlocking {
            // GIVEN
            val product = WooPosCartListItem(id = 23L, name = "title")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(repository.getProductById(eq(product.id))).thenReturn(
                ProductTestUtils.generateProduct(productId = product.id, productName = "title")
            )

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.id)
            )

            sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

            // THEN
            val toolbar = states.last().toolbar
            assertEquals(R.drawable.ic_back_24dp, toolbar.icon)
            assertEquals("Items in cart: 1", toolbar.itemsCount)
            assertEquals(false, toolbar.isClearAllButtonVisible)
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
