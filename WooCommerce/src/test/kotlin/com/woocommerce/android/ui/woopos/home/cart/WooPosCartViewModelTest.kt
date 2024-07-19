package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class WooPosCartViewModelTest {

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
        on { events }.thenReturn(MutableSharedFlow())
    }
    private val getProductById: WooPosGetProductById = mock()
    private val resourceProvider: ResourceProvider = mock {
        on {
            getQuantityString(
                quantity = eq(1),
                default = eq(R.string.woopos_items_in_cart_multiple),
                zero = eq(R.string.woopos_items_in_cart_multiple),
                one = eq(R.string.woopos_items_in_cart),
            )
        }.thenReturn("Item in cart: 1")
    }
    private val formatPrice: WooPosFormatPrice = mock {
        onBlocking { invoke(eq(BigDecimal("10.0"))) }.thenReturn("10.0$")
    }

    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
        )

        // THEN
        val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat(itemsInCart.first().id.productId).isEqualTo(product.remoteId)
    }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should remove item from cart`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
        )

        // WHEN
        sut.onUIEvent(
            WooPosCartUIEvent.ItemRemovedFromCart(
                WooPosCartState.Body.WithItems.Item(
                    id = WooPosCartState.Body.WithItems.Item.Id(productId = product.remoteId, itemNumber = 1),
                    name = product.name,
                    price = "10.0$",
                    imageUrl = product.firstImageUrl
                )
            )
        )

        // THEN
        val itemsInCartAfterRemoveClicked =
            (states.last().body as? WooPosCartState.Body.WithItems)?.itemsInCart ?: emptyList()
        assertThat(itemsInCartAfterRemoveClicked).isEmpty()
    }

    @Test
    fun `given empty cart in_progress, when vm created, then toolbar state should contain shopping cart empty itemsCart and no clear all button`() =
        runTest {
            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isNull()
            assertThat(toolbar.itemsCount).isNull()
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Test
    fun `given non empty cart in_progress, when vm created, then toolbar state should contain shopping cart itemsCart title and no clear all`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product.remoteId))).thenReturn(product)

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
            )

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isNull()
            assertThat(toolbar.itemsCount).isEqualTo("Item in cart: 1")
            assertThat(toolbar.isClearAllButtonVisible).isTrue()
        }

    @Test
    fun `given non empty cart checkout, when vm created, then toolbar state should contain back icon itemsCart title and no clear all`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product.remoteId))).thenReturn(product)

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
            )

            sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isEqualTo(R.drawable.ic_back_24dp)
            assertThat(toolbar.itemsCount).isEqualTo("Item in cart: 1")
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Test
    fun `given non empty cart in process, when 2 items added and the first removed and third item added, then third will have item number 3`() =
        runTest {
            // GIVEN
            val product1 = ProductTestUtils.generateProduct(
                productId = 1L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")
            val product2 = ProductTestUtils.generateProduct(
                productId = 2L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")
            val product3 = ProductTestUtils.generateProduct(
                productId = 3L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product1.remoteId))).thenReturn(product1)
            whenever(getProductById(eq(product2.remoteId))).thenReturn(product2)
            whenever(getProductById(eq(product3.remoteId))).thenReturn(product3)

            val sut = createSut()
            val states = sut.state.captureValues()

            // WHEN
            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product1.remoteId)
            )
            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product2.remoteId)
            )

            sut.onUIEvent(
                WooPosCartUIEvent.ItemRemovedFromCart(
                    WooPosCartState.Body.WithItems.Item(
                        id = WooPosCartState.Body.WithItems.Item.Id(productId = product1.remoteId, itemNumber = 1),
                        name = product1.name,
                        price = "10.0$",
                        imageUrl = product1.firstImageUrl
                    )
                )
            )

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product3.remoteId)
            )

            // THEN
            val itemsInCart = (states.last().body as WooPosCartState.Body.WithItems).itemsInCart
            assertThat(itemsInCart).hasSize(2)
            assertThat(itemsInCart[0].id.itemNumber).isEqualTo(2)
            assertThat(itemsInCart[1].id.itemNumber).isEqualTo(3)
        }

    @Test
    fun `given empty cart, when created, then state should be empty`() = runTest {
        // WHEN
        val sut = createSut()
        val states = sut.state.captureValues()

        // THEN
        assertThat(states).hasSize(2)
        assertThat(states.last().body).isInstanceOf(WooPosCartState.Body.Empty::class.java)
        assertThat(states.last().cartStatus).isEqualTo(WooPosCartStatus.EMPTY)
    }

    @Test
    fun `given non-empty cart, when all items removed, then state should be empty`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
        )

        // WHEN
        sut.onUIEvent(WooPosCartUIEvent.ClearAllClicked)

        // THEN
        assertThat(states).hasSizeGreaterThan(1)
        val finalState = states.last()
        assertThat(finalState.body).isInstanceOf(WooPosCartState.Body.Empty::class.java)
        assertThat(finalState.cartStatus).isEqualTo(WooPosCartStatus.EMPTY)
    }

    @Test
    fun `given non-empty cart, when all items removed individually, then state should be empty`() = runTest {
        // GIVEN
        val product = ProductTestUtils.generateProduct(
            productId = 23L,
            productName = "title",
            amount = "10.0"
        ).copy(firstImageUrl = "url")

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(getProductById(eq(product.remoteId))).thenReturn(product)
        val sut = createSut()
        val states = sut.state.captureValues()

        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
        )

        // WHEN
        sut.onUIEvent(
            WooPosCartUIEvent.ItemRemovedFromCart(
                WooPosCartState.Body.WithItems.Item(
                    id = WooPosCartState.Body.WithItems.Item.Id(productId = product.remoteId, itemNumber = 1),
                    name = product.name,
                    price = "10.0$",
                    imageUrl = product.firstImageUrl
                )
            )
        )

        // THEN
        assertThat(states).hasSizeGreaterThan(1)
        val finalState = states.last()
        assertThat(finalState.body).isInstanceOf(WooPosCartState.Body.Empty::class.java)
        assertThat(finalState.cartStatus).isEqualTo(WooPosCartStatus.EMPTY)
    }

    @Test
    fun `given non-empty cart, when vm initialized and all items removed, then toolbar state should reflect empty cart`() =
        runTest {
            // GIVEN
            val product = ProductTestUtils.generateProduct(
                productId = 23L,
                productName = "title",
                amount = "10.0"
            ).copy(firstImageUrl = "url")

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product.remoteId))).thenReturn(product)
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.remoteId)
            )

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.ClearAllClicked)

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isNull()
            assertThat(toolbar.itemsCount).isNull()
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    private fun createSut(): WooPosCartViewModel {
        return WooPosCartViewModel(
            childrenToParentEventSender,
            parentToChildrenEventReceiver,
            getProductById,
            resourceProvider,
            formatPrice,
            savedState
        )
    }
}
