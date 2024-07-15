package com.woocommerce.android.ui.woopos.home.cart

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
@ExperimentalCoroutinesApi
@Suppress("UnnecessaryAbstractClass")
@RunWith(MockitoJUnitRunner::class)
class WooPosCartViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    init {

        /**
         * This is a temporary workaround  to fix existing tests that were broken by
         * the change on kotlinx.coroutines 1.7.0 that causes tests that
         * throw exceptions to fail. Previously test methods that threw exceptions would not prevent
         * tests from passing, which was a bug in kotlinx.coroutines that has now been fixed. However,
         * significant number of our tests are currently failing because of this change.
         *
         * See the following issue for more details: https://github.com/Kotlin/kotlinx.coroutines/issues/1205.
         * The workaround below is taken from the related PR: https://github.com/Kotlin/kotlinx.coroutines/pull/3736
         * and is a solution suggested by JetBrains to disable the new behavior using non-public API
         * until we fix our tests. This should not be considered a long-term solution, rather a temporary hack.
         */

        Class.forName("kotlinx.coroutines.test.TestScopeKt")
            .getDeclaredMethod("setCatchNonTestRelatedExceptions", Boolean::class.java)
            .invoke(null, false)
    }

    @Rule
    @JvmField
    val rule2 = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule2 = CoroutineTestRule(testDispatcher)

    private fun testBlocking2(block: suspend TestScope.() -> Unit) =
        runTest(coroutinesTestRule2.testDispatcher) {
            block()
        }

    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock {
        on { events }.thenReturn(MutableSharedFlow())
    }
    private val getProductById: WooPosGetProductById = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(eq(R.string.woopos_items_in_cart), eq(1)) }.thenReturn("Items in cart: 1")
        on { getString(eq(R.string.woopos_items_in_cart), eq(2)) }.thenReturn("Items in cart: 2")
    }
    private val formatPrice: WooPosFormatPrice = mock {
        onBlocking { invoke(eq(BigDecimal("10.0"))) }.thenReturn("10.0$")
    }

    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = testBlocking2 {
        // GIVEN
        val product = WooPosCartListItem(
            id = WooPosCartListItem.Id(23L, 1),
            name = "title",
            price = "10.0$",
            imageUrl = "url"
        )

        val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
        whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
        whenever(getProductById(eq(product.id.productId))).thenReturn(
            generateProductWithFirstImage(product.id.productId)
        )
        val sut = createSut()
        val states = sut.state.captureValues()

        // WHEN
        parentToChildrenEventsMutableFlow.emit(
            ParentToChildrenEvent.ItemClickedInProductSelector(product.id.productId)
        )

        // THEN
        val itemsInCart = states.last().itemsInCart
        assertThat(itemsInCart).hasSize(1)
        assertThat(itemsInCart.first().id).isEqualTo(product.id)
    }

    @Test
    fun `given items in cart, when item remove button clicked in cart, then should remove item from cart`() =
        testBlocking2 {
            // GIVEN
            val product = WooPosCartListItem(
                id = WooPosCartListItem.Id(23L, 1),
                name = "title",
                price = "10.0$",
                imageUrl = "url"
            )

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product.id.productId))).thenReturn(
                generateProductWithFirstImage(product.id.productId)
            )
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.id.productId)
            )

            // WHEN
            sut.onUIEvent(WooPosCartUIEvent.ItemRemovedFromCart(product))

            // THEN
            val itemsInCartAfterRemoveClicked = states.last().itemsInCart
            assertThat(itemsInCartAfterRemoveClicked).isEmpty()
        }

    @Test
    fun `given empty cart in_progress, when vm created, then toolbar state should contain shopping cart empty itemsCart and no clear all button`() =
        testBlocking2 {
            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isEqualTo(R.drawable.ic_shopping_cart)
            assertThat(toolbar.itemsCount).isEmpty()
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Test
    fun `given non empty cart in_progress, when vm created, then toolbar state should contain shopping cart itemsCart title and no clear all`() =
        testBlocking2 {
            // GIVEN
            val product = WooPosCartListItem(
                id = WooPosCartListItem.Id(23L, 1),
                name = "title",
                price = "10.0$",
                imageUrl = "url"
            )

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product.id.productId))).thenReturn(
                generateProductWithFirstImage(product.id.productId)
            )

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.id.productId)
            )

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isEqualTo(R.drawable.ic_shopping_cart)
            assertThat(toolbar.itemsCount).isEqualTo("Items in cart: 1")
            assertThat(toolbar.isClearAllButtonVisible).isTrue()
        }

    @Test
    fun `given non empty cart checkout, when vm created, then toolbar state should contain back icon itemsCart title and no clear all`() =
        testBlocking2 {
            // GIVEN
            val product = WooPosCartListItem(
                id = WooPosCartListItem.Id(23L, 1),
                name = "title",
                price = "10.0$",
                imageUrl = "url"
            )

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product.id.productId))).thenReturn(
                generateProductWithFirstImage(product.id.productId)
            )

            // WHEN
            val sut = createSut()
            val states = sut.state.captureValues()

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product.id.productId)
            )

            sut.onUIEvent(WooPosCartUIEvent.CheckoutClicked)

            // THEN
            val toolbar = states.last().toolbar
            assertThat(toolbar.icon).isEqualTo(R.drawable.ic_back_24dp)
            assertThat(toolbar.itemsCount).isEqualTo("Items in cart: 1")
            assertThat(toolbar.isClearAllButtonVisible).isFalse()
        }

    @Test
    fun `given non empty cart in process, when 2 items added and the first removed and third item added, then third will have item number 3`() =
        testBlocking2 {
            // GIVEN
            val product1Id = 1L
            val product2Id = 2L
            val product3Id = 3L

            val parentToChildrenEventsMutableFlow = MutableSharedFlow<ParentToChildrenEvent>()
            whenever(parentToChildrenEventReceiver.events).thenReturn(parentToChildrenEventsMutableFlow)
            whenever(getProductById(eq(product1Id))).thenReturn(
                generateProductWithFirstImage(product1Id)
            )
            whenever(getProductById(eq(product2Id))).thenReturn(
                generateProductWithFirstImage(product2Id)
            )
            whenever(getProductById(eq(product3Id))).thenReturn(
                generateProductWithFirstImage(product3Id)
            )

            val sut = createSut()
            val states = sut.state.captureValues()

            // WHEN
            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product1Id)
            )
            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product2Id)
            )

            sut.onUIEvent(
                WooPosCartUIEvent.ItemRemovedFromCart(
                    WooPosCartListItem(
                        id = WooPosCartListItem.Id(product1Id, 1),
                        name = "title",
                        price = "10.0$",
                        imageUrl = "url"
                    )
                )
            )

            parentToChildrenEventsMutableFlow.emit(
                ParentToChildrenEvent.ItemClickedInProductSelector(product3Id)
            )

            // THEN
            val itemsInCart = states.last().itemsInCart
            assertThat(itemsInCart).hasSize(2)
            assertThat(itemsInCart[0].id.itemNumber).isEqualTo(2)
            assertThat(itemsInCart[1].id.itemNumber).isEqualTo(3)
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

    private fun generateProductWithFirstImage(productId: Long) =
        ProductTestUtils.generateProduct(
            productId = productId,
            productName = "title",
            amount = "10.0"
        ).copy(
            firstImageUrl = "url"
        )
}
