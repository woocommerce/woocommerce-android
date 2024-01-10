package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.configuration.ConfigurationType
import com.woocommerce.android.ui.orders.creation.configuration.ProductConfiguration
import com.woocommerce.android.ui.orders.creation.configuration.ProductRules
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AdjustProductQuantityTest : BaseUnitTest() {
    private val adjustProductQuantity = AdjustProductQuantity()

    @Test
    fun `increase quantity for product using item id`() {
        val itemId = simpleItemID
        val quantityToAdd = 2

        val updatedOrder = adjustProductQuantity(order, itemId, quantityToAdd)

        val items = updatedOrder.items.associateBy { it.itemId }

        assertThat(items.getValue(simpleItemID).quantity).isEqualTo(simpleItem.quantity + quantityToAdd)
    }

    @Test
    fun `decrease quantity for product using item id`() {
        val itemId = simpleItemID
        val quantityToDecrease = -2

        val updatedOrder = adjustProductQuantity(order, itemId, quantityToDecrease)

        val items = updatedOrder.items.associateBy { it.itemId }

        assertThat(items.getValue(simpleItemID).quantity).isEqualTo(simpleItem.quantity + quantityToDecrease)
    }

    @Test
    fun `increase quantity for product using product`() {
        val quantityToAdd = 2

        val updatedOrder = adjustProductQuantity(order, simpleProduct, quantityToAdd)

        val items = updatedOrder.items.associateBy { it.itemId }

        assertThat(items.getValue(simpleItemID).quantity).isEqualTo(simpleItem.quantity + quantityToAdd)
    }

    @Test
    fun `decrease quantity for product using product`() {
        val quantityToDecrease = -2

        val updatedOrder = adjustProductQuantity(order, simpleProduct, quantityToDecrease)

        val items = updatedOrder.items.associateBy { it.itemId }

        assertThat(items.getValue(simpleItemID).quantity).isEqualTo(simpleItem.quantity + quantityToDecrease)
    }

    @Test
    fun `increase quantity for bundle product`() {
        val quantityToAdd = 2

        val updatedOrder = adjustProductQuantity(order, bundleProduct, quantityToAdd)

        val items = updatedOrder.items.associateBy { it.itemId }

        // Assert that we remove (quantity == 0) the bundle item and its children
        assertThat(items.getValue(bundleItemID).quantity).isEqualTo(0f)
        assertThat(items.getValue(bundleChildItemID).quantity).isEqualTo(0f)

        // Assert that a new item is created
        items.getValue(notSyncedItemID).let { notSyncedBundleItem ->
            assertThat(notSyncedBundleItem.quantity).isEqualTo(bundleItem.quantity + quantityToAdd)
        }
    }

    @Test
    fun `decrease quantity for bundle product`() {
        val quantityToDecrease = -2

        val updatedOrder = adjustProductQuantity(order, bundleProduct, quantityToDecrease)

        val items = updatedOrder.items.associateBy { it.itemId }

        // Assert that we remove (quantity == 0) the bundle item and its children
        assertThat(items.getValue(bundleItemID).quantity).isEqualTo(0f)
        assertThat(items.getValue(bundleChildItemID).quantity).isEqualTo(0f)

        // Assert that a new item is created
        items.getValue(notSyncedItemID).let { notSyncedBundleItem ->
            assertThat(notSyncedBundleItem.quantity).isEqualTo(bundleItem.quantity + quantityToDecrease)
        }
    }

    @Test
    fun `increase quantity for not synced product using item id`() {
        val itemId = notSyncedItemID
        val quantityToAdd = 2
        val orderWithNotSyncedProduct = order.copy(items = order.items + notSyncedItem)

        val updatedOrder = adjustProductQuantity(orderWithNotSyncedProduct, itemId, quantityToAdd)

        val items = updatedOrder.items.associateBy { it.itemId }

        // Check that the quantity is not updated. This will prevent a race condition when the item syncs and get
        // a valid item ID while the product quantity is updating.
        assertThat(items.getValue(notSyncedItemID).quantity).isEqualTo(notSyncedItem.quantity)
    }

    @Test
    fun `decrease quantity for not synced product using item id`() {
        val itemId = notSyncedItemID
        val quantityToDecrease = -2
        val orderWithNotSyncedProduct = order.copy(items = order.items + notSyncedItem)

        val updatedOrder = adjustProductQuantity(orderWithNotSyncedProduct, itemId, quantityToDecrease)

        val items = updatedOrder.items.associateBy { it.itemId }

        // Check that the quantity is not updated. This will prevent a race condition when the item syncs and get
        // a valid item ID while the product quantity is updating.
        assertThat(items.getValue(notSyncedItemID).quantity).isEqualTo(notSyncedItem.quantity)
    }

    @Test
    fun `increase quantity for not synced product using product`() {
        val quantityToAdd = 2
        val orderWithNotSyncedProduct = order.copy(items = order.items + notSyncedItem)

        val updatedOrder = adjustProductQuantity(orderWithNotSyncedProduct, notSyncedProduct, quantityToAdd)

        val items = updatedOrder.items.associateBy { it.itemId }

        // Check that the quantity is not updated. This will prevent a race condition when the item syncs and get
        // a valid item ID while the product quantity is updating.
        assertThat(items.getValue(notSyncedItemID).quantity).isEqualTo(notSyncedItem.quantity)
    }

    @Test
    fun `decrease quantity for not synced product using product`() {
        val quantityToDecrease = -2
        val orderWithNotSyncedProduct = order.copy(items = order.items + notSyncedItem)

        val updatedOrder = adjustProductQuantity(orderWithNotSyncedProduct, notSyncedProduct, quantityToDecrease)

        val items = updatedOrder.items.associateBy { it.itemId }

        // Check that the quantity is not updated. This will prevent a race condition when the item syncs and get
        // a valid item ID while the product quantity is updating.
        assertThat(items.getValue(notSyncedItemID).quantity).isEqualTo(notSyncedItem.quantity)
    }

    private val notSyncedItemID = 0L
    private val notSyncedItem = Order.Item(
        itemId = notSyncedItemID,
        productId = 12L,
        name = "A test",
        price = BigDecimal("10"),
        sku = "",
        quantity = 4f,
        subtotal = BigDecimal("10"),
        totalTax = BigDecimal.ZERO,
        total = BigDecimal("10"),
        variationId = 0,
        attributesList = emptyList()
    )

    private val simpleItemID = 1L
    private val simpleItem = Order.Item(
        itemId = simpleItemID,
        productId = 12L,
        name = "A test",
        price = BigDecimal("10"),
        sku = "",
        quantity = 4f,
        subtotal = BigDecimal("10"),
        totalTax = BigDecimal.ZERO,
        total = BigDecimal("10"),
        variationId = 0,
        attributesList = emptyList()
    )

    private val bundleItemID = 2L
    private val bundleItem = createBundleItem()

    private val bundleChildItemID = 3L
    private val bundleChildItem = Order.Item(
        itemId = bundleChildItemID,
        productId = 16L,
        name = "A test",
        price = BigDecimal("10"),
        sku = "",
        quantity = 4f,
        subtotal = BigDecimal("10"),
        totalTax = BigDecimal.ZERO,
        total = BigDecimal("10"),
        variationId = 0,
        attributesList = emptyList(),
        parent = 2L
    )

    private fun createBundleItem(): Order.Item {
        val productRule = ProductRules.Builder().apply {
            productType = ProductType.BUNDLE
            setChildOptional(3L)
        }.build()

        val configuration = ProductConfiguration(
            rules = productRule,
            configurationType = ConfigurationType.BUNDLE,
            configuration = mutableMapOf(),
            childrenConfiguration = mutableMapOf()
        )

        return Order.Item(
            itemId = bundleItemID,
            productId = 14L,
            name = "A test",
            price = BigDecimal("10"),
            sku = "",
            quantity = 4f,
            subtotal = BigDecimal("10"),
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("10"),
            variationId = 0,
            attributesList = emptyList(),
            configuration = configuration
        )
    }

    private val order = Order.getEmptyOrder(Date(), Date())
        .copy(items = listOf(simpleItem, bundleItem, bundleChildItem))

    private val defaultProductInfo = ProductInfo(
        imageUrl = "",
        isStockManaged = false,
        stockQuantity = 0.0,
        stockStatus = ProductStockStatus.InStock,
        productType = ProductType.SIMPLE,
        isConfigurable = false,
        pricePreDiscount = "10",
        priceTotal = "10",
        priceSubtotal = "10",
        discountAmount = "0",
        priceAfterDiscount = "10",
        hasDiscount = false,
    )

    private val notSyncedProduct = OrderCreationProduct.ProductItem(
        item = notSyncedItem,
        productInfo = defaultProductInfo
    )

    private val simpleProduct = OrderCreationProduct.ProductItem(
        item = simpleItem,
        productInfo = defaultProductInfo
    )

    private val childrenProduct = OrderCreationProduct.ProductItem(
        item = bundleChildItem,
        productInfo = defaultProductInfo
    )

    private val bundleProduct = OrderCreationProduct.GroupedProductItemWithRules(
        item = bundleItem,
        rules = bundleItem.configuration!!.rules,
        configuration = bundleItem.configuration!!,
        productInfo = defaultProductInfo.copy(isConfigurable = true, productType = ProductType.BUNDLE),
        children = listOf(childrenProduct)
    )
}
