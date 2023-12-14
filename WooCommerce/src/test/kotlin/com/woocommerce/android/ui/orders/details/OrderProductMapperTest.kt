package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.Test
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class OrderProductMapperTest : BaseUnitTest() {
    val sut = OrderProductMapper()

    private val groupedItemCondition: Condition<OrderProduct> =
        Condition({ item -> item is OrderProduct.GroupedProductItem }, "Grouped item")

    private val groupedItemCollapsedCondition: Condition<OrderProduct> =
        Condition(
            { item -> item is OrderProduct.GroupedProductItem && item.isExpanded.not() },
            "Grouped item expanded"
        )

    @Test
    fun `When there are NO parents then DON'T create grouped items`() {
        val numberOfItems = 10
        val items = createItemsList(numberOfItems)

        val result = sut.toOrderProducts(emptyList(), items)

        assertThat(result.size).isEqualTo(numberOfItems)

        assertThat(result).areNot(groupedItemCondition)
    }

    @Test
    fun `When there are 2 parents then create 2 grouped items`() {
        val numberOfItems = 10
        val numberOfParents = 2
        val items = createItemsList(numberOfItems, numberOfParents)

        val result = sut.toOrderProducts(emptyList(), items)

        assertThat(result).areExactly(numberOfParents, groupedItemCondition)
    }

    @Test
    fun `When we transform the items list then there are no duplicates`() {
        val numberOfItems = 10
        val numberOfParents = 2
        val items = createItemsList(numberOfItems, numberOfParents)

        val result = sut.toOrderProducts(emptyList(), items)

        val ids = mutableSetOf<Long>()
        result.forEach { item ->
            when (item) {
                is OrderProduct.ProductItem -> ids.add(item.product.itemId)
                is OrderProduct.GroupedProductItem -> {
                    ids.add(item.product.itemId)
                    item.children.forEach { child -> ids.add(child.product.itemId) }
                }
            }
        }
        assertThat(ids.size).isEqualTo(numberOfItems)
    }

    @Test
    fun `When we pass a list of current products then the is isExpanded value from current products is retained`() {
        val numberOfItems = 10
        val numberOfParents = 3
        val numberOfParentsExpanded = numberOfParents - 1
        val items = createItemsList(numberOfItems, numberOfParents)
        val current = sut.toOrderProducts(emptyList(), items)
        collapseItems(current, numberOfParentsExpanded)

        val result = sut.toOrderProducts(current, items)

        assertThat(result).areExactly(numberOfParentsExpanded, groupedItemCollapsedCondition)
    }

    private fun createItemsList(items: Int, parents: Int = 0): List<Order.Item> {
        if (items < parents * 2) error("the number of items must be greater than the number of parents")
        val parentsList = List(parents) { n -> createItemFromNumber(n) }
        val childrenList = List(items - parents) { n ->
            createItemFromNumber(
                parents + n,
                if (n < parents) n.toLong() else null
            )
        }
        return parentsList + childrenList
    }

    private fun createItemFromNumber(n: Int, parent: Long? = null): Order.Item {
        val total = BigDecimal.TEN * (n + 1).toBigDecimal()
        return Order.Item(
            itemId = n.toLong(),
            productId = n * 2L,
            name = "item $n",
            price = total,
            sku = "sku_$n",
            quantity = 1f,
            subtotal = total,
            totalTax = BigDecimal.ZERO,
            total = total,
            variationId = -1,
            attributesList = emptyList(),
            parent = parent
        )
    }

    private fun collapseItems(products: List<OrderProduct>, numberOfItemsToCollapse: Int) {
        var parentsExpandedCount = 0
        var i = 0
        while (i <= products.lastIndex && parentsExpandedCount < numberOfItemsToCollapse) {
            val item = products[i]
            if (item is OrderProduct.GroupedProductItem) {
                item.isExpanded = false
                parentsExpandedCount++
            }
            i++
        }
    }
}
