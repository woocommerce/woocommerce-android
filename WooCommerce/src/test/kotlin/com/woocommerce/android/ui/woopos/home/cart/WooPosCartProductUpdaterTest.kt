package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.math.BigDecimal
import kotlin.test.Test

@ExperimentalCoroutinesApi
class WooPosCartProductUpdaterTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(eq(R.string.woopos_cart_changes_in_the_cart)) }.thenReturn("Changes made to items in cart")
    }
    private val formatPrice: WooPosFormatPrice = mock {
        onBlocking { invoke(any()) }.thenReturn("10.0$")
    }

    private val updater = WooPosCartProductUpdater(
        childrenToParentEventSender = childrenToParentEventSender,
        resourceProvider = resourceProvider,
        formatPrice = formatPrice
    )

    @Test
    fun `given cart with simple product, when updated product info available, then product is updated`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Old Name",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(
                itemsInCart = listOf(simpleProduct)
            )
        )
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Name",
            price = BigDecimal("10.0"),
            quantity = 1f
        )

        // WHEN
        val result = updater.invoke(cartState, listOf(updatedInfo))

        // THEN
        val resultingCartItems = (result.body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(resultingCartItems).hasSize(1)
        val updatedItem = resultingCartItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem.name).isEqualTo("Updated Name")
        assertThat(updatedItem.price).isEqualTo("10.0$")
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
    }

    @Test
    fun `given cart with variation product, when updated product info available, then product is updated`() = runTest {
        // GIVEN
        val variationProduct = WooPosCartItemViewState.Product.Variation(
            itemNumber = 1,
            id = 1L,
            variationId = 2L,
            name = "Old Variation",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(
                itemsInCart = listOf(variationProduct)
            )
        )
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Variation(
            id = 1L,
            variationId = 2L,
            name = "Updated Variation",
            price = BigDecimal("10.0"),
            quantity = 1f
        )

        // WHEN
        val result = updater.invoke(cartState, listOf(updatedInfo))

        // THEN
        val resultingCartItems = (result.body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(resultingCartItems).hasSize(1)
        val updatedItem = resultingCartItems[0] as WooPosCartItemViewState.Product.Variation
        assertThat(updatedItem.name).isEqualTo("Updated Variation")
        assertThat(updatedItem.price).isEqualTo("10.0$")
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
    }

    @Test
    fun `given cart with product, when product no longer available, then product is marked as not existing`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product Name",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(
                itemsInCart = listOf(simpleProduct)
            )
        )

        // WHEN
        val result = updater.invoke(cartState, emptyList())

        // THEN
        val resultingCartItems = (result.body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(resultingCartItems).hasSize(1)
        val updatedItem = resultingCartItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem.productDoesNotExist).isTrue()
        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
    }

    @Test
    fun `given cart with multiple items, when updating only some products, then only those are updated`() = runTest {
        // GIVEN
        val simpleProduct1 = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product 1",
            price = "9.0$",
            imageUrl = "url1",
            description = null
        )
        val simpleProduct2 = WooPosCartItemViewState.Product.Simple(
            itemNumber = 2,
            id = 2L,
            name = "Product 2",
            price = "19.0$",
            imageUrl = "url2",
            description = null
        )
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(
                itemsInCart = listOf(simpleProduct1, simpleProduct2)
            )
        )
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product 1",
            price = BigDecimal("10.0"),
            quantity = 1f
        )

        // WHEN
        val result = updater.invoke(cartState, listOf(updatedInfo))

        // THEN
        val resultingCartItems = (result.body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(resultingCartItems).hasSize(2)

        val updatedItem1 = resultingCartItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem1.name).isEqualTo("Updated Product 1")
        assertThat(updatedItem1.price).isEqualTo("10.0$")

        val updatedItem2 = resultingCartItems[1] as WooPosCartItemViewState.Product.Simple
        assertThat(updatedItem2.productDoesNotExist).isTrue()

        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
    }

    @Test
    fun `given cart with multiple identical products, when updated product info available, then update respects quantities`() = runTest {
        // GIVEN
        val simpleProduct = WooPosCartItemViewState.Product.Simple(
            itemNumber = 1,
            id = 1L,
            name = "Product",
            price = "9.0$",
            imageUrl = "url",
            description = null
        )
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.WithItems(
                itemsInCart = listOf(simpleProduct, simpleProduct)
            )
        )
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product",
            price = BigDecimal("10.0"),
            quantity = 1f
        )

        // WHEN
        val result = updater.invoke(cartState, listOf(updatedInfo))

        // THEN
        val resultingCartItems = (result.body as WooPosCartState.Body.WithItems).itemsInCart
        assertThat(resultingCartItems).hasSize(2)

        val firstItem = resultingCartItems[0] as WooPosCartItemViewState.Product.Simple
        assertThat(firstItem.name).isEqualTo("Updated Product")
        assertThat(firstItem.price).isEqualTo("10.0$")
        assertThat(firstItem.productDoesNotExist).isFalse()

        val secondItem = resultingCartItems[1] as WooPosCartItemViewState.Product.Simple
        assertThat(secondItem.productDoesNotExist).isTrue()

        verify(childrenToParentEventSender).sendToParent(any<ChildToParentEvent.ToastMessageDisplayed>())
    }

    @Test
    fun `given cart with body not as WithItems, when updating products, then original state is returned`() = runTest {
        // GIVEN
        val cartState = WooPosCartState(
            body = WooPosCartState.Body.Empty
        )
        val updatedInfo = ParentToChildrenEvent.OrderCreated.ProductInfo.Simple(
            id = 1L,
            name = "Updated Product",
            price = BigDecimal("10.0"),
            quantity = 1f
        )

        // WHEN
        val result = updater.invoke(cartState, listOf(updatedInfo))

        // THEN
        assertThat(result).isEqualTo(cartState)
    }
}
