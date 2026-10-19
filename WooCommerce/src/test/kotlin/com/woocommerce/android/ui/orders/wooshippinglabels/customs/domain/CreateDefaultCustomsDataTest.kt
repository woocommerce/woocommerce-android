package com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain

import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.store.WCDataStore
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CreateDefaultCustomsDataTest : BaseUnitTest() {
    private val dataStore: WCDataStore = mock()
    private val sut = CreateDefaultCustomsData(dataStore)

    @Test
    fun `when shippable item is a variation, then use variation id for customs product id`() = testBlocking {
        val item = ShippableItemModel(
            itemId = 1L,
            productId = PRODUCT_ID,
            title = "Product",
            price = BigDecimal.TEN,
            quantity = 1f,
            currency = "USD",
            length = 1f,
            width = 1f,
            height = 1f,
            weight = 1f,
            variationId = VARIATION_ID
        )

        val result = sut(listOf(item), OriginShippingAddress.EMPTY)

        assertThat(result.items.single().productID).isEqualTo(VARIATION_ID)
    }

    private companion object {
        const val PRODUCT_ID = 10L
        const val VARIATION_ID = 20L
    }
}
