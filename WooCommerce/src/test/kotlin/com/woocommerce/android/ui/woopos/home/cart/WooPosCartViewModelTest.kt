package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosCartViewModelTest : BaseUnitTest() {
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver = mock()
    private val repository: WooPosCartRepository = mock()
    private val productStore: WCProductStore = mock()
    private val site: SelectedSite = mock {
        onBlocking { get() }.thenReturn(SiteModel())
    }
    private val savedState: SavedStateHandle = SavedStateHandle()

    @Test
    fun `given empty cart, when product clicked in product selector, then should add product to cart`() = testBlocking {
        val product = WooPosCartListItem(productId = 23L, title = "title")
        whenever(parentToChildrenEventReceiver.events).thenReturn(
            flowOf(ParentToChildrenEvent.ItemClickedInProductSelector(product.productId))

        )
        whenever(productStore.getProductByRemoteId(any(), eq(product.productId))).thenReturn(
            WCProductModel(product.productId.toInt()).apply {
                name = product.title
            }
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
            whenever(productStore.getProductByRemoteId(any(), eq(product.productId))).thenReturn(
                WCProductModel(product.productId.toInt()).apply {
                    name = product.title
                }
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
            productStore,
            site,
            repository,
            savedState
        )
    }
}
