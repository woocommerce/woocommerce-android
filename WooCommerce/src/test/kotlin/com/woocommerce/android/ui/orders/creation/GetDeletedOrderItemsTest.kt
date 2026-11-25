package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationChanged
import org.wordpress.android.fluxc.store.WCProductStore.ProductError
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType

@OptIn(ExperimentalCoroutinesApi::class)
class GetDeletedOrderItemsTest : BaseUnitTest() {
    private val site = SiteModel()
    private val selectedSite: SelectedSite = mock { on { get() } doReturn site }
    private val productStore: WCProductStore = mock()

    private val sut = GetDeletedOrderItems(productStore, selectedSite)

    @Test
    fun `given one product was deleted, when checking items, then return only that item`() = testBlocking {
        val existing = Order.Item.EMPTY.copy(productId = 1, name = "Hoodie")
        val deleted = Order.Item.EMPTY.copy(productId = 2, name = "Beanie")
        whenever(productStore.fetchSingleProduct(argThat { remoteProductId == 1L })).thenReturn(OnProductChanged())
        whenever(productStore.fetchSingleProduct(argThat { remoteProductId == 2L }))
            .thenReturn(productChangedWithError(ProductErrorType.INVALID_PRODUCT_ID))

        val result = sut(listOf(existing, deleted))

        assertThat(result).containsExactly(deleted)
        verify(productStore).deleteProductLocally(site, 2)
        verify(productStore, never()).deleteProductLocally(site, 1)
    }

    @Test
    fun `given a variation was deleted, when checking items, then return it`() = testBlocking {
        val deleted = Order.Item.EMPTY.copy(productId = 1, variationId = 3, name = "Beanie - Red")
        whenever(productStore.fetchSingleVariation(site, 1, 3, "view")).thenReturn(
            OnVariationChanged().apply { error = ProductError(ProductErrorType.INVALID_PRODUCT_ID) }
        )

        val result = sut(listOf(deleted))

        assertThat(result).containsExactly(deleted)
        verify(productStore).deleteVariationLocally(site, 1, 3)
    }

    @Test
    fun `given a product check fails for another reason, when checking items, then return null`() = testBlocking {
        val item = Order.Item.EMPTY.copy(productId = 2, name = "Beanie")
        whenever(productStore.fetchSingleProduct(argThat { remoteProductId == 2L }))
            .thenReturn(productChangedWithError(ProductErrorType.GENERIC_ERROR))

        val result = sut(listOf(item))

        assertThat(result).isNull()
        verify(productStore, never()).deleteProductLocally(site, 2)
    }

    private fun productChangedWithError(type: ProductErrorType) =
        OnProductChanged().apply { error = ProductError(type) }
}
