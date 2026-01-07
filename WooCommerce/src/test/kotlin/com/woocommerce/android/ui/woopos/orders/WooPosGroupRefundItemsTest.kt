package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.test.Test

class WooPosGroupRefundItemsTest {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private lateinit var sut: WooPosGroupRefundItems

    private val testSite = SiteModel().apply { id = 1 }

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(testSite)
        whenever(wooCommerceStore.getSiteSettings(testSite)).thenReturn(
            Settings(
                currencyCode = "USD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                countryCode = "US",
                stateCode = "CA",
                address = "",
                address2 = "",
                city = "",
                postalCode = "",
                couponsEnabled = true
            )
        )
        sut = WooPosGroupRefundItems(selectedSite, wooCommerceStore)
    }

    private fun createRefundableItem(
        orderItemId: Long,
        productId: Long = 100L,
        variationId: Long = 0L,
        name: String = "Test Product",
        unitPrice: BigDecimal = BigDecimal("20.00"),
        unitTax: BigDecimal = BigDecimal("2.00"),
        rowIndex: Int = 0
    ) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = productId,
        variationId = variationId,
        name = name,
        unitPrice = unitPrice,
        unitTax = unitTax,
        formattedUnitPrice = "$$unitPrice",
        formattedUnitTax = "$$unitTax",
        rowIndex = rowIndex
    )

    private fun createOrderItem(
        itemId: Long,
        quantity: Float = 1f,
        price: BigDecimal = BigDecimal("20.00"),
        totalTax: BigDecimal = BigDecimal.ZERO,
        taxes: List<Order.LineTaxEntry> = emptyList()
    ) = Order.Item(
        itemId = itemId,
        productId = 100L,
        name = "Test Product",
        price = price,
        sku = "",
        quantity = quantity,
        subtotal = price.multiply(quantity.toBigDecimal()),
        subtotalTax = BigDecimal.ZERO,
        totalTax = totalTax,
        total = price.multiply(quantity.toBigDecimal()),
        variationId = 0L,
        attributesList = emptyList(),
        taxes = taxes
    )

    private fun createOrder(items: List<Order.Item>) =
        OrderTestUtils.generateTestOrder().copy(items = items)

    @Test
    fun `given empty list, when invoke called, then returns empty list`() {
        // GIVEN
        val refundableItems = emptyList<WooPosRefundableItem>()
        val order = createOrder(emptyList())

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given single item, when invoke called, then returns single refund item with quantity 1 and correct totals`() {
        // GIVEN
        val orderItem = createOrderItem(
            itemId = 123L,
            quantity = 5f,
            price = BigDecimal("20.00"),
            totalTax = BigDecimal("10.00"),
            taxes = listOf(
                Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("10.00"))
            )
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"))
        )
        val order = createOrder(listOf(orderItem))

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(1)
        assertThat(result[0].refundTotal).isEqualTo(BigDecimal("20.00"))
        assertThat(result[0].refundTax).hasSize(1)
        assertThat(result[0].refundTax!![0].taxRateId).isEqualTo(1L)
        assertThat(result[0].refundTax!![0].refundTotal).isEqualTo(BigDecimal("2.00")) // 10.00 / 5 * 1
    }

    @Test
    fun `given multiple items with same orderItemId, when invoke called, then groups them with correct quantity and totals`() {
        // GIVEN
        val orderItem = createOrderItem(
            itemId = 123L,
            quantity = 5f,
            price = BigDecimal("20.00"),
            totalTax = BigDecimal("10.00"),
            taxes = listOf(
                Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("10.00"))
            )
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"), rowIndex = 1),
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"), rowIndex = 2)
        )
        val order = createOrder(listOf(orderItem))

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(3)
        assertThat(result[0].refundTotal).isEqualTo(BigDecimal("60.00")) // 20 * 3
        assertThat(result[0].refundTax).hasSize(1)
        assertThat(result[0].refundTax!![0].taxRateId).isEqualTo(1L)
        assertThat(result[0].refundTax!![0].refundTotal).isEqualTo(BigDecimal("6.00")) // (10.00 / 5) * 3
    }

    @Test
    fun `given items with different orderItemIds, when invoke called, then creates separate refund items`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L),
            createRefundableItem(orderItemId = 456L),
            createRefundableItem(orderItemId = 789L)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L),
                createOrderItem(itemId = 456L),
                createOrderItem(itemId = 789L)
            )
        )

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(3)
        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(123L, 456L, 789L)
        result.forEach { item ->
            assertThat(item.quantity).isEqualTo(1)
        }
    }

    @Test
    fun `given mixed orderItemIds, when invoke called, then groups correctly by orderItemId`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, rowIndex = 0),
            createRefundableItem(orderItemId = 456L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, rowIndex = 1),
            createRefundableItem(orderItemId = 789L, rowIndex = 0),
            createRefundableItem(orderItemId = 456L, rowIndex = 1)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L, quantity = 5f),
                createOrderItem(itemId = 456L, quantity = 5f),
                createOrderItem(itemId = 789L, quantity = 5f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(3)

        val item123 = result.find { it.itemId == 123L }
        assertThat(item123).isNotNull
        assertThat(item123!!.quantity).isEqualTo(2)

        val item456 = result.find { it.itemId == 456L }
        assertThat(item456).isNotNull
        assertThat(item456!!.quantity).isEqualTo(2)

        val item789 = result.find { it.itemId == 789L }
        assertThat(item789).isNotNull
        assertThat(item789!!.quantity).isEqualTo(1)
    }

    @Test
    fun `given 5 items with same orderItemId, when invoke called, then returns single item with quantity 5`() {
        // GIVEN
        val refundableItems = (0..4).map { index ->
            createRefundableItem(orderItemId = 999L, rowIndex = index)
        }
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 999L, quantity = 10f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(999L)
        assertThat(result[0].quantity).isEqualTo(5)
    }

    @Test
    fun `given items with same product and same orderItemId, when invoke called, then groups by orderItemId`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, productId = 100L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, productId = 100L, rowIndex = 1)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L, quantity = 2f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(2)
    }

    @Test
    fun `given same product with different orderItemIds, when invoke called, then creates separate refund items`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, productId = 100L),
            createRefundableItem(orderItemId = 456L, productId = 100L)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L),
                createOrderItem(itemId = 456L)
            )
        )

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(123L, 456L)
        result.forEach { item ->
            assertThat(item.quantity).isEqualTo(1)
        }
    }

    @Test
    fun `given large number of items with same orderItemId, when invoke called, then groups all items correctly`() {
        // GIVEN
        val refundableItems = (0..99).map { index ->
            createRefundableItem(orderItemId = 1L, rowIndex = index)
        }
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 1L, quantity = 100f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(1L)
        assertThat(result[0].quantity).isEqualTo(100)
    }
}
