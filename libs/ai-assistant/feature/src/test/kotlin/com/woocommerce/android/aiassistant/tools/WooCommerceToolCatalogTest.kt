package com.woocommerce.android.aiassistant.tools

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewContext
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewProviderRegistryImpl
import com.woocommerce.android.aiassistant.safety.GenericSchemaConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.OrdersConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ProductVariationsConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.safety.ProductsConfirmationPreviewProvider
import com.woocommerce.android.aiassistant.tools.analytics.AnalyticsOrdersToolHandler
import com.woocommerce.android.aiassistant.tools.customers.CustomersListToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.ShowCardsToolHandler
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
import com.woocommerce.android.aiassistant.tools.orders.OrdersBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersGetToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersListToolHandler
import com.woocommerce.android.aiassistant.tools.orders.OrdersUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import com.woocommerce.android.aiassistant.tools.products.ProductVariationsToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductVariationsUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsBulkUpdateToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsGetToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsListToolHandler
import com.woocommerce.android.aiassistant.tools.products.ProductsUpdateToolHandler
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooCommerceToolCatalogTest {
    private val diagnosticsFactory = testToolFailureDiagnosticsFactory()
    private val ordersDataSource: AIOrdersDataSource = mock()
    private val productsDataSource: AIProductsDataSource = mock()
    private val variationsDataSource: AIProductVariationsDataSource = mock()

    private val importantUnsafeToolNames = setOf(
        "orders_update",
        "orders_bulk_update",
        "products_update",
        "products_bulk_update",
        "product_variations_update",
    )

    private val allHandlers: Set<AssistantToolHandler> = setOf(
        OrdersListToolHandler(mock(), mock(), diagnosticsFactory),
        OrdersGetToolHandler(mock(), mock(), diagnosticsFactory),
        OrdersUpdateToolHandler(mock(), mock(), diagnosticsFactory),
        OrdersBulkUpdateToolHandler(mock(), mock(), diagnosticsFactory),
        ProductsListToolHandler(mock(), mock(), diagnosticsFactory),
        ProductsGetToolHandler(mock(), mock(), diagnosticsFactory),
        ProductsUpdateToolHandler(mock(), mock(), diagnosticsFactory),
        ProductsBulkUpdateToolHandler(mock(), mock(), diagnosticsFactory),
        ProductVariationsToolHandler(mock(), mock(), diagnosticsFactory),
        ProductVariationsUpdateToolHandler(mock(), mock(), diagnosticsFactory),
        AnalyticsOrdersToolHandler(mock(), mock(), diagnosticsFactory),
        ShowCardsToolHandler(mock<ShowCardsResolver>()),
        CustomersListToolHandler(mock(), diagnosticsFactory),
    )

    @Test
    fun `when all handlers are aggregated, then 13 expected tool names are present`() {
        val names = allHandlers.map { it.descriptor.name }

        assertThat(names).containsExactlyInAnyOrder(
            "orders_list",
            "orders_get",
            "orders_update",
            "orders_bulk_update",
            "products_list",
            "products_get",
            "products_update",
            "products_bulk_update",
            "product_variations_list",
            "product_variations_update",
            "analytics_orders",
            "show_cards",
            "customers_list",
        )
    }

    @Test
    fun `when descriptors are inspected, then write tools are UNSAFE and read tools are SAFE`() {
        val byName = allHandlers.associateBy { it.descriptor.name }
        assertThat(byName.keys).contains("analytics_orders")
        assertThat(byName.keys).doesNotContain("analytics_revenue")
        val writeToolNames = setOf(
            "orders_update",
            "orders_bulk_update",
            "products_update",
            "products_bulk_update",
            "product_variations_update",
        )
        val bulkWriteToolNames = setOf("orders_bulk_update", "products_bulk_update")

        writeToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name should be UNSAFE")
                .isEqualTo(ToolSafetyLevel.UNSAFE)
        }

        bulkWriteToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name bulk writes should be UNSAFE")
                .isEqualTo(ToolSafetyLevel.UNSAFE)
        }

        val readToolNames = byName.keys - writeToolNames
        readToolNames.forEach { name ->
            assertThat(byName.getValue(name).descriptor.safetyLevel)
                .`as`("$name should be SAFE")
                .isEqualTo(ToolSafetyLevel.SAFE)
        }
    }

    @Test
    fun `when descriptors are inspected, then no deletion tools are present`() {
        val forbiddenNameParts = listOf("delete", "remove", "destructive")
        val names = allHandlers.map { it.descriptor.name }

        forbiddenNameParts.forEach { forbidden ->
            assertThat(names.filter { it.contains(forbidden) })
                .`as`("catalog should not contain $forbidden tools")
                .isEmpty()
        }
    }

    @Test
    fun `when descriptors are retrieved, then write-tool descriptions include allowlisted fields and constraints`() {
        val byName = allHandlers.associateBy { it.descriptor.name }

        val ordersUpdate = byName.getValue("orders_update").descriptor.description
        assertThat(ordersUpdate).contains("status")
        assertThat(ordersUpdate).contains("customer emails")

        val ordersBulkUpdate = byName.getValue("orders_bulk_update").descriptor.description
        assertThat(ordersBulkUpdate).contains("status")
        assertThat(ordersBulkUpdate).contains("Bulk writes require confirmation")

        val ordersList = byName.getValue("orders_list").descriptor.description
        assertThat(ordersList).contains("analytics_orders")
        assertThat(ordersList).doesNotContain("analytics_revenue")

        val productsUpdate = byName.getValue("products_update").descriptor.description
        assertThat(productsUpdate).contains("regular_price")
        assertThat(productsUpdate).contains("stock_quantity")

        val productsBulkUpdate = byName.getValue("products_bulk_update").descriptor.description
        assertThat(productsBulkUpdate).contains("regular_price")
        assertThat(productsBulkUpdate).contains("Bulk writes require confirmation")
    }

    @Test
    fun `when descriptors are inspected, then every UNSAFE tool has a confirmation preview path`() =
        runTest {
            stubSingleEntityLookupsToFail()
            val registry = previewRegistry()
            val unsafeDescriptors = allHandlers
                .map { it.descriptor }
                .filter { it.safetyLevel == ToolSafetyLevel.UNSAFE }

            val previews = unsafeDescriptors.map { descriptor ->
                registry.buildPreview(previewContextFor(descriptor))
            }

            assertThat(previews).hasSameSizeAs(unsafeDescriptors)
            previews.forEach { preview ->
                assertThat(preview.message).isNotNull()
            }
        }

    @Test
    fun `when important unsafe descriptors are inspected, then they use dedicated preview providers`() =
        runTest {
            val registry = previewRegistry()
            val descriptorsByName = allHandlers.map { it.descriptor }.associateBy { it.name }

            importantUnsafeToolNames.forEach { toolName ->
                val descriptor = requireNotNull(descriptorsByName[toolName]) {
                    "Missing important unsafe descriptor: $toolName"
                }
                val provider = registry.providerFor(previewContextFor(descriptor))

                assertThat(provider.key)
                    .`as`("$toolName should not use the generic provider")
                    .isIn(
                        "woocommerce_orders",
                        "woocommerce_products",
                        "woocommerce_product_variations",
                    )
                assertThat(provider.key).isNotEqualTo("generic_schema")
            }
        }

    // This assertion documents the current unsafe descriptor subset. If a future straightforward unsafe tool is
    // intentionally allowed to use the generic provider, update this policy set and keep the dedicated-provider
    // assertion scoped to workflows that are important, common, risky, or semantically complex.
    @Test
    fun `when current catalog unsafe descriptors are inspected, then important unsafe policy is explicit`() {
        val currentUnsafeToolNames = allHandlers
            .map { it.descriptor }
            .filter { it.safetyLevel == ToolSafetyLevel.UNSAFE }
            .map { it.name }
            .toSet()

        assertThat(currentUnsafeToolNames).isEqualTo(importantUnsafeToolNames)
    }

    private fun previewRegistry() = ConfirmationPreviewProviderRegistryImpl(
        setOf(
            OrdersConfirmationPreviewProvider(ordersDataSource),
            ProductsConfirmationPreviewProvider(productsDataSource),
            ProductVariationsConfirmationPreviewProvider(variationsDataSource),
            GenericSchemaConfirmationPreviewProvider(),
        )
    )

    private suspend fun stubSingleEntityLookupsToFail() {
        whenever(ordersDataSource.getOrder(42L))
            .thenReturn(Result.failure(IllegalStateException("not needed for path test")))
        whenever(productsDataSource.getProduct(7L))
            .thenReturn(Result.failure(IllegalStateException("not needed for path test")))
        whenever(variationsDataSource.getVariation(7L, 8L))
            .thenReturn(Result.failure(IllegalStateException("not needed for path test")))
    }

    private fun confirmationRequestFor(descriptor: ToolDescriptor) = ConfirmationRequest(
        id = "confirmation-${descriptor.name}",
        toolCallId = "call-${descriptor.name}",
        toolName = descriptor.name,
        arguments = argumentsForUnsafeTool(descriptor.name),
        safetyLevel = descriptor.safetyLevel,
    )

    private fun previewContextFor(descriptor: ToolDescriptor) = ConfirmationPreviewContext(
        request = confirmationRequestFor(descriptor),
        descriptor = descriptor,
    )

    private fun argumentsForUnsafeTool(toolName: String): JsonObject = when (toolName) {
        "orders_update" -> buildJsonObject {
            put("id", 42)
            put("status", "processing")
        }
        "orders_bulk_update" -> buildJsonObject {
            putJsonArray("ids") {
                add(1)
                add(2)
            }
            putJsonObject("patch") { put("status", "completed") }
        }
        "products_update" -> buildJsonObject {
            put("id", 7)
            put("regular_price", "24.99")
        }
        "products_bulk_update" -> buildJsonObject {
            putJsonArray("ids") {
                add(7)
                add(8)
            }
            putJsonObject("patch") { put("status", "draft") }
        }
        "product_variations_update" -> buildJsonObject {
            put("product_id", 7)
            put("id", 8)
            put("sku", "VAR-8")
        }
        else -> buildJsonObject { put("reason", "Preview") }
    }
}
