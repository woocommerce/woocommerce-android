package com.woocommerce.android.ui.orders.creation


import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Companion.EMPTY_BIG_DECIMAL
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class ListItemMapperTest : BaseUnitTest() {
    private val sut = ListItemMapper(mock(), mock())

    @Test
    fun `given item with empty subtotal, when converted to JSON map, then should exclude subtotal prop`() = testBlocking {
        val item = createItem().copy(subtotal = EMPTY_BIG_DECIMAL)

        val rawListItem: Map<String, Any> = sut.toRawListItem(item)

        assertFalse(rawListItem.containsKey("subtotal"))
    }

    @Test
    fun `given item with empty total, when converted to JSON map, then should exclude total prop`() = testBlocking {
        val item = createItem().copy(total = EMPTY_BIG_DECIMAL)

        val rawListItem: Map<String, Any> = sut.toRawListItem(item)

        assertFalse(rawListItem.containsKey("total"))
    }

    @Test
    fun `given item with non-empty total and subtotal, when converted to JSON map, then should contain all required props`() = testBlocking {
        val item = createItem()

        val rawListItem: Map<String, Any> = sut.toRawListItem(item)

        assert(rawListItem.containsKey("subtotal"))
        assert(rawListItem.containsKey("total"))
        assert(rawListItem.containsKey("id"))
        assert(rawListItem.containsKey("name"))
        assert(rawListItem.containsKey("product_id"))
        assert(rawListItem.containsKey("variation_id"))
        assert(rawListItem.containsKey("quantity"))
        assert(rawListItem.containsKey("quantity"))
    }

    private fun createItem() = Order.Item(
        itemId = 1,
        name = "Test Item",
        productId = 2,
        variationId = 3,
        quantity = 4F,
        subtotal = 1000.toBigDecimal(),
        total = 1100.toBigDecimal(),
        price = 2.toBigDecimal(),
        sku = "",
        attributesList = emptyList(),
        totalTax = 1.toBigDecimal(),
    )
}