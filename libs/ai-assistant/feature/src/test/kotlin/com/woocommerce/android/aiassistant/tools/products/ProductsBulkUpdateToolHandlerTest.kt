package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore

@OptIn(ExperimentalCoroutinesApi::class)
class ProductsBulkUpdateToolHandlerTest {

    private val dataSource: AIProductsDataSource = mock()
    private val handler = ProductsBulkUpdateToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "products_bulk_update", arguments = arguments)

    @Test
    fun `given descriptor, when inspected, then schema matches bulk update contract`() {
        val schema = handler.descriptor.inputSchema
        val properties = requireNotNull(schema["properties"]).jsonObject
        val ids = requireNotNull(properties["ids"]).jsonObject
        val patch = requireNotNull(properties["patch"]).jsonObject
        val patchProperties = requireNotNull(patch["properties"]).jsonObject

        assertThat(handler.descriptor.name).isEqualTo("products_bulk_update")
        assertThat(handler.descriptor.safetyLevel).isEqualTo(ToolSafetyLevel.UNSAFE)
        assertThat(requireNotNull(schema["additionalProperties"]).jsonPrimitive.content).isEqualTo("false")
        assertThat(requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content })
            .containsExactly("ids", "patch")
        assertThat(requireNotNull(ids["items"]).jsonObject["type"]?.jsonPrimitive?.content).isEqualTo("integer")
        assertThat(requireNotNull(ids["minItems"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(requireNotNull(ids["maxItems"]).jsonPrimitive.content).isEqualTo("100")
        assertThat(requireNotNull(patch["additionalProperties"]).jsonPrimitive.content).isEqualTo("false")
        assertThat(requireNotNull(patch["minProperties"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(patchProperties.keys).containsExactlyInAnyOrder(
            "name",
            "regular_price",
            "sale_price",
            "stock_quantity",
            "status",
        )
    }

    @Test
    fun `given empty ids, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", JsonArray(emptyList()))
                    put("patch", buildJsonObject { put("name", "Updated") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).bulkUpdateProducts(any(), any())
    }

    @Test
    fun `given empty patch, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { add(42) })
                    put("patch", buildJsonObject { })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).bulkUpdateProducts(any(), any())
    }

    @Test
    fun `given more than 100 ids, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { repeat(101) { add(it + 1) } })
                    put("patch", buildJsonObject { put("name", "Updated") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        verify(dataSource, never()).bulkUpdateProducts(any(), any())
    }

    @Test
    fun `given unsupported status, when execute is called, then ValidationError is returned`() = runTest {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { add(42) })
                    put("patch", buildJsonObject { put("status", "trash") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
    }

    @Test
    fun `given valid patch, when execute succeeds, then structured summary is returned`() = runTest {
        whenever(
            dataSource.bulkUpdateProducts(
                productIds = listOf(42L, 43L),
                update = AIProductsDataSource.ProductUpdate(
                    name = "Updated",
                    regularPrice = "12.50",
                    stockQuantity = 5,
                    status = "publish",
                )
            )
        ).thenReturn(
            Result.success(
                AIProductsDataSource.BulkUpdateResult(
                    updatedIds = listOf(42L, 43L),
                    failedProducts = emptyList(),
                )
            )
        )

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray {
                        add(42)
                        add(43)
                    })
                    put(
                        "patch",
                        buildJsonObject {
                            put("name", "Updated")
                            put("regular_price", "12.50")
                            put("stock_quantity", 5)
                            put("status", "publish")
                        }
                    )
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        val structured = (result as ToolResult.Success).structured.jsonObject
        assertThat(requireNotNull(structured["tool"]).jsonPrimitive.content).isEqualTo("products_bulk_update")
        assertThat(requireNotNull(structured["updated_count"]).jsonPrimitive.content).isEqualTo("2")
        assertThat(requireNotNull(structured["failed_count"]).jsonPrimitive.content).isEqualTo("0")
        assertThat(requireNotNull(structured["updated_ids"]).jsonArray.map { it.jsonPrimitive.long })
            .containsExactly(42L, 43L)
        assertThat(structured["failed"]).isNull()
    }

    @Test
    fun `given partial product failures, when execute succeeds, then structured failures are returned`() = runTest {
        whenever(
            dataSource.bulkUpdateProducts(
                productIds = listOf(42L, 43L),
                update = AIProductsDataSource.ProductUpdate(name = "Updated")
            )
        ).thenReturn(
            Result.success(
                AIProductsDataSource.BulkUpdateResult(
                    updatedIds = listOf(42L),
                    failedProducts = listOf(
                        WCProductStore.UpdateProductsResult.FailedProduct(
                            id = 43L,
                            errorCode = "woocommerce_rest_product_invalid_id",
                            errorMessage = "Invalid ID.",
                            errorStatus = 400,
                        )
                    ),
                )
            )
        )

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray {
                        add(42)
                        add(43)
                    })
                    put("patch", buildJsonObject { put("name", "Updated") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        val structured = (result as ToolResult.Success).structured.jsonObject
        assertThat(requireNotNull(structured["updated_count"]).jsonPrimitive.content).isEqualTo("1")
        assertThat(requireNotNull(structured["failed_count"]).jsonPrimitive.content).isEqualTo("1")
        val failed = requireNotNull(structured["failed"]).jsonArray.single().jsonObject
        assertThat(requireNotNull(failed["id"]).jsonPrimitive.long).isEqualTo(43L)
        assertThat(requireNotNull(failed["code"]).jsonPrimitive.content)
            .isEqualTo("woocommerce_rest_product_invalid_id")
        assertThat(requireNotNull(failed["message"]).jsonPrimitive.content).isEqualTo("Invalid ID.")
        assertThat(requireNotNull(failed["status"]).jsonPrimitive.content).isEqualTo("400")
    }

    @Test
    fun `given update fails, when execute is called, then retryable TransportError is returned`() = runTest {
        whenever(
            dataSource.bulkUpdateProducts(
                productIds = listOf(42L),
                update = AIProductsDataSource.ProductUpdate(name = "Updated")
            )
        ).thenReturn(Result.failure(RuntimeException("network error")))

        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("ids", buildJsonArray { add(42) })
                    put("patch", buildJsonObject { put("name", "Updated") })
                }
            )
        )

        assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
        assertThat((result as ToolResult.TransportError).retryable).isTrue
    }
}
