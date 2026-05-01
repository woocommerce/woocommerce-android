package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

class WooCommerceConfirmationSnapshotResolverTest {
    private val ordersDataSource: AIOrdersDataSource = mock()
    private val productsDataSource: AIProductsDataSource = mock()
    private val variationsDataSource: AIProductVariationsDataSource = mock()

    private val resolver = WooCommerceConfirmationSnapshotResolver(
        ordersDataSource = ordersDataSource,
        productsDataSource = productsDataSource,
        variationsDataSource = variationsDataSource,
    )

    @Test
    fun `given order update, when resolving snapshot, then current order fields are returned`() = runTest {
        val order = OrderEntity(localSiteId = LocalId(1), orderId = 42L, status = "wc-pending")
        val request = confirmationRequest(
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", 42)
                put("status", "processing")
            },
        )
        val orderDataSource: AIOrdersDataSource = mock()

        val resolver = WooCommerceConfirmationSnapshotResolver(
            ordersDataSource = orderDataSource,
            productsDataSource = productsDataSource,
            variationsDataSource = variationsDataSource,
        )
        whenever(orderDataSource.getOrder(42L)).thenReturn(Result.success(order))

        val snapshot = resolver.resolve(request)

        assertThat(requireNotNull(snapshot).currentValues).isEqualTo(mapOf("status" to "pending"))
    }

    @Test
    fun `given product update, when resolving snapshot, then current product fields are returned`() = runTest {
        val product = WCProductModel(
            remoteId = RemoteId(7L),
            regularPrice = "19.99",
            salePrice = "",
            stockQuantity = 5.5,
            status = "publish",
            name = "Current name",
        )
        val productDataSource: AIProductsDataSource = mock()
        val resolver = WooCommerceConfirmationSnapshotResolver(
            ordersDataSource = ordersDataSource,
            productsDataSource = productDataSource,
            variationsDataSource = variationsDataSource,
        )
        whenever(productDataSource.getProduct(7L)).thenReturn(Result.success(product))

        val snapshot = resolver.resolve(
            confirmationRequest(
                toolName = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", "24.99")
                },
            )
        )

        assertThat(requireNotNull(snapshot).currentValues).containsEntry("regular_price", "19.99")
        assertThat(requireNotNull(snapshot).currentValues).containsEntry("name", "Current name")
        assertThat(requireNotNull(snapshot).currentValues).containsEntry("stock_quantity", "5.5")
    }

    @Test
    fun `given variation update, when resolving snapshot, then current variation fields are returned`() = runTest {
        val variation = WCProductVariationModel(
            remoteProductId = RemoteId(7L),
            remoteVariationId = RemoteId(8L),
            sku = "VAR-7",
            regularPrice = "19.99",
            salePrice = "",
            stockQuantity = 3.5,
            stockStatus = "instock",
            status = "publish",
        )
        val variationDataSource: AIProductVariationsDataSource = mock()
        val resolver = WooCommerceConfirmationSnapshotResolver(
            ordersDataSource = ordersDataSource,
            productsDataSource = productsDataSource,
            variationsDataSource = variationDataSource,
        )
        whenever(variationDataSource.getVariation(7L, 8L)).thenReturn(Result.success(variation))

        val snapshot = resolver.resolve(
            confirmationRequest(
                toolName = "product_variations_update",
                arguments = buildJsonObject {
                    put("product_id", 7)
                    put("id", 8)
                    put("sku", "VAR-8")
                },
            )
        )

        assertThat(requireNotNull(snapshot).currentValues).containsEntry("sku", "VAR-7")
        assertThat(requireNotNull(snapshot).currentValues).containsEntry("stock_quantity", "3.5")
        assertThat(requireNotNull(snapshot).currentValues).containsEntry("stock_status", "instock")
    }

    @Test
    fun `given bulk update, when resolving snapshot, then no fetch is performed`() = runTest {
        val snapshot = resolver.resolve(
            confirmationRequest(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                    put("patch", buildJsonObject { put("status", "draft") })
                },
            )
        )

        assertThat(snapshot).isNull()
        verify(ordersDataSource, never()).getOrder(org.mockito.kotlin.any())
        verify(productsDataSource, never()).getProduct(org.mockito.kotlin.any())
        verify(variationsDataSource, never()).getVariation(org.mockito.kotlin.any(), org.mockito.kotlin.any())
    }

    private fun confirmationRequest(
        toolName: String,
        arguments: kotlinx.serialization.json.JsonObject,
    ) = ConfirmationRequest(
        id = "confirmation-1",
        toolCallId = "call-1",
        toolName = toolName,
        arguments = arguments,
        safetyLevel = ToolSafetyLevel.UNSAFE,
    )
}
