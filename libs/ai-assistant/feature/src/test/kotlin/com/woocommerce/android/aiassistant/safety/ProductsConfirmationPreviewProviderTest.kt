package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.aiassistant.tools.products.AIProductsDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductModel

class ProductsConfirmationPreviewProviderTest {
    @Test
    fun `given product price and stock update, when preview is built, then title omits field summary`() = runTest {
        val preview = preview(
            toolName = "products_update",
            arguments = buildJsonObject {
                put("id", 7)
                put("regular_price", "24.99")
                put("stock_quantity", 100)
            },
        )

        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_product_update_title, raw("7"))
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "regular_price",
                value = raw("24.99"),
                label = label(R.string.ai_assistant_confirmation_field_regular_price),
            ),
            ConfirmationPreviewField(
                name = "stock_quantity",
                value = raw("100"),
                label = label(R.string.ai_assistant_confirmation_field_stock_quantity),
            ),
        )
    }

    @Test
    fun `given product update has unsupported stock status, when preview is built, then it is ignored`() =
        runTest {
            val preview = preview(
                toolName = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", "24.99")
                    put("stock_status", "instock")
                },
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_update_title,
                    raw("7"),
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "regular_price",
                    value = raw("24.99"),
                    label = label(R.string.ai_assistant_confirmation_field_regular_price),
                ),
            )
        }

    @Test
    fun `given bulk product update, when preview is built, then count and fields are included`() = runTest {
        val preview = preview(
            toolName = "products_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                put(
                    "patch",
                    buildJsonObject {
                        put("regular_price", "19.99")
                        put("status", "draft")
                    },
                )
            },
        )

        assertThat(preview.message).isEqualTo(
            quantity(
                quantity = 2,
                singular = R.string.ai_assistant_confirmation_products_bulk_update_title_single,
                multiple = R.string.ai_assistant_confirmation_products_bulk_update_title_multiple,
            )
        )
        assertThat(preview.fields).containsExactly(
            ConfirmationPreviewField(
                name = "regular_price",
                value = raw("19.99"),
                label = label(R.string.ai_assistant_confirmation_field_regular_price),
            ),
            ConfirmationPreviewField(
                name = "status",
                value = raw("draft"),
                label = label(R.string.ai_assistant_confirmation_field_status),
            ),
        )
        assertThat(preview.bulkEntries).containsExactly(
            ConfirmationBulkEntry(7),
            ConfirmationBulkEntry(8),
        )
    }

    @Test
    fun `given bulk product update has resolved and unresolved ids, when preview is built, then entries preserve order and include available names`() =
        runTest {
            val dataSource: AIProductsDataSource = mock()
            whenever(dataSource.getProducts(listOf(7L, 8L, 9L))).thenReturn(
                Result.success(
                    cachedProductLookup(
                        product(remoteId = 9L, name = "Beanie"),
                        product(remoteId = 7L, name = "Classic T-Shirt"),
                    )
                )
            )

            val preview = preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8), JsonPrimitive(9))))
                    put("patch", buildJsonObject { put("status", "draft") })
                },
                dataSource = dataSource,
            )

            assertThat(preview.bulkEntries).containsExactly(
                ConfirmationBulkEntry(7, "Classic T-Shirt"),
                ConfirmationBulkEntry(8),
                ConfirmationBulkEntry(9, "Beanie"),
            )
            assertThat(preview.bulkEntries.map { it.displayText }).containsExactly(
                "#7  Classic T-Shirt",
                "#8",
                "#9  Beanie",
            )
        }

    @Test
    fun `given bulk product update, when names are resolved, then one batch lookup is used for all ids`() =
        runTest {
            val dataSource: AIProductsDataSource = mock()
            whenever(dataSource.getProducts(listOf(7L, 8L, 9L))).thenReturn(
                Result.success(
                    cachedProductLookup(product(remoteId = 7L), product(remoteId = 8L), product(remoteId = 9L))
                )
            )

            preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8), JsonPrimitive(9))))
                    put("patch", buildJsonObject { put("status", "draft") })
                },
                dataSource = dataSource,
            )

            verify(dataSource).getProducts(listOf(7L, 8L, 9L))
            verify(dataSource, never()).getProduct(7L)
            verify(dataSource, never()).getProduct(8L)
            verify(dataSource, never()).getProduct(9L)
        }

    @Test
    fun `given bulk product lookup returns cache hits, when preview is built, then cached names are used`() =
        runTest {
            val dataSource: AIProductsDataSource = mock()
            whenever(dataSource.getProducts(listOf(7L, 8L))).thenReturn(
                Result.success(
                    cachedProductLookup(
                        product(remoteId = 7L, name = "Classic T-Shirt"),
                        product(remoteId = 8L, name = "Beanie"),
                    )
                )
            )

            val preview = preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                    put("patch", buildJsonObject { put("status", "draft") })
                },
                dataSource = dataSource,
            )

            assertThat(preview.bulkEntries).containsExactly(
                ConfirmationBulkEntry(7, "Classic T-Shirt"),
                ConfirmationBulkEntry(8, "Beanie"),
            )
            verify(dataSource).getProducts(listOf(7L, 8L))
            verify(dataSource, never()).getProduct(7L)
            verify(dataSource, never()).getProduct(8L)
        }

    @Test
    fun `given product update and current product, when preview is built, then before and after values are included`() =
        runTest {
            val dataSource: AIProductsDataSource = mock()
            whenever(dataSource.getProduct(7L)).thenReturn(
                Result.success(
                    product(
                        name = "Classic T-Shirt",
                        regularPrice = "19.99",
                    )
                )
            )

            val preview = preview(
                toolName = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", "24.99")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_update_title_with_name,
                    raw("Classic T-Shirt"),
                    raw("7"),
                )
            )
            assertThat(preview.rows).containsExactly(
                ConfirmationPreviewField(
                    name = "regular_price",
                    label = label(R.string.ai_assistant_confirmation_field_regular_price),
                    value = raw("24.99"),
                    beforeValue = raw("19.99"),
                )
            )
        }

    @Test
    fun `given product lookup fails, when single preview is built, then title is id only and fields remain`() =
        runTest {
            val dataSource: AIProductsDataSource = mock()
            whenever(dataSource.getProduct(7L)).thenReturn(
                Result.failure(IllegalStateException("No current product"))
            )

            val preview = preview(
                toolName = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", "24.99")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(R.string.ai_assistant_confirmation_product_update_title, raw("7"))
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "regular_price",
                    value = raw("24.99"),
                    label = label(R.string.ai_assistant_confirmation_field_regular_price),
                ),
            )
        }

    @Test
    fun `given bulk product lookup fails, when preview is built, then all entries are id only`() = runTest {
        val dataSource: AIProductsDataSource = mock()
        whenever(dataSource.getProducts(listOf(7L, 8L))).thenReturn(
            Result.failure(IllegalStateException("No current products"))
        )

        val preview = preview(
            toolName = "products_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                put("patch", buildJsonObject { put("status", "draft") })
            },
            dataSource = dataSource,
        )

        assertThat(preview.bulkEntries).containsExactly(
            ConfirmationBulkEntry(7),
            ConfirmationBulkEntry(8),
        )
        assertThat(preview.bulkEntries.map { it.displayText }).containsExactly("#7", "#8")
    }

    @Test
    fun `given product update and current product has padded name, when preview is built, then title trims name`() =
        runTest {
            val dataSource: AIProductsDataSource = mock()
            whenever(dataSource.getProduct(7L)).thenReturn(Result.success(product(name = "  Classic T-Shirt  ")))

            val preview = preview(
                toolName = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", "24.99")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_update_title_with_name,
                    raw("Classic T-Shirt"),
                    raw("7"),
                )
            )
        }

    @Test
    fun `given product update, when preview is built, then only product data source is fetched`() = runTest {
        val dataSource: AIProductsDataSource = mock()
        whenever(dataSource.getProduct(7L)).thenReturn(Result.success(product()))

        preview(
            toolName = "products_update",
            arguments = buildJsonObject {
                put("id", 7)
                put("regular_price", "24.99")
            },
            dataSource = dataSource,
        )

        verify(dataSource).getProduct(7L)
    }

    @Test
    fun `given product update has wrong-shaped primitive fields, when preview is built, then fields are omitted`() =
        runTest {
            val preview = preview(
                toolName = "products_update",
                arguments = buildJsonObject {
                    put("id", 7)
                    put("regular_price", buildJsonObject { put("value", "24.99") })
                    put("stock_quantity", JsonArray(listOf(JsonPrimitive(100))))
                    put("status", "draft")
                },
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_update_title,
                    raw("7"),
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    value = raw("draft"),
                    label = label(R.string.ai_assistant_confirmation_field_status),
                ),
            )
        }

    @Test
    fun `given bulk product update has wrong-shaped ids, when preview is built, then fallback is used`() =
        runTest {
            val preview = preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", buildJsonObject { put("value", 7) })
                    put("patch", buildJsonObject { put("regular_price", "19.99") })
                },
            )

            assertThat(preview.message).isEqualTo(
                string(R.string.ai_assistant_confirmation_products_bulk_update_generic)
            )
            assertThat(preview.fields).isEmpty()
        }

    @Test
    fun `given bulk product update has non-numeric ids, when preview is built, then fallback is used`() =
        runTest {
            val preview = preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive("bad"))))
                    put("patch", buildJsonObject { put("regular_price", "19.99") })
                },
            )

            assertThat(preview.message).isEqualTo(
                string(R.string.ai_assistant_confirmation_products_bulk_update_generic)
            )
            assertThat(preview.fields).isEmpty()
        }

    @Test
    fun `given bulk product update has wrong-shaped patch fields, when preview is built, then fields are omitted`() =
        runTest {
            val preview = preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7), JsonPrimitive(8))))
                    put(
                        "patch",
                        buildJsonObject {
                            put("regular_price", JsonArray(listOf(JsonPrimitive("19.99"))))
                            put("stock_quantity", buildJsonObject { put("value", 100) })
                        },
                    )
                },
            )

            assertThat(preview.message).isEqualTo(
                quantity(
                    quantity = 2,
                    singular = R.string.ai_assistant_confirmation_products_bulk_update_title_single,
                    multiple = R.string.ai_assistant_confirmation_products_bulk_update_title_multiple,
                )
            )
            assertThat(preview.fields).isEmpty()
        }

    @Test
    fun `given bulk product update with a single id, when preview is built, then before values stay absent`() =
        runTest {
            val preview = preview(
                toolName = "products_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(7))))
                    put(
                        "patch",
                        buildJsonObject {
                            put("status", "draft")
                        },
                    )
                },
            )

            assertThat(preview.isBulk).isTrue()
            assertThat(preview.rows.single().beforeValue).isNull()
        }

    private suspend fun preview(
        toolName: String,
        arguments: JsonObject,
        dataSource: AIProductsDataSource? = null,
    ): ConfirmationPreview =
        ProductsConfirmationPreviewProvider(
            dataSource ?: failingDataSource()
        ).buildPreview(context(toolName, arguments))

    private suspend fun failingDataSource(): AIProductsDataSource {
        val dataSource: AIProductsDataSource = mock()
        whenever(dataSource.getProduct(any())).thenReturn(
            Result.failure(IllegalStateException("No current product"))
        )
        whenever(dataSource.getProducts(any())).thenReturn(
            Result.failure(IllegalStateException("No current products"))
        )
        return dataSource
    }

    private fun context(
        toolName: String,
        arguments: JsonObject,
    ) = ConfirmationPreviewContext(
        request = ConfirmationRequest(
            id = "confirmation-1",
            toolCallId = "call-1",
            toolName = toolName,
            arguments = arguments,
            safetyLevel = ToolSafetyLevel.UNSAFE,
        ),
        descriptor = ToolDescriptor(
            name = toolName,
            description = "$toolName descriptor",
            inputSchema = buildJsonObject {},
            safetyLevel = ToolSafetyLevel.UNSAFE,
        ),
    )

    private fun product(
        remoteId: Long = 7L,
        name: String = "Current name",
        regularPrice: String = "19.99",
        salePrice: String = "",
        stockQuantity: Double = 5.5,
        status: String = "publish",
    ) = WCProductModel(
        remoteId = RemoteId(remoteId),
        regularPrice = regularPrice,
        salePrice = salePrice,
        stockQuantity = stockQuantity,
        status = status,
        name = name,
    )

    private fun cachedProductLookup(vararg products: WCProductModel) = CachedLookupResult(
        items = products.toList(),
        cacheHitCount = products.size,
        cacheMissCount = 0,
        fetchAttempted = false,
        fetchFailed = false,
    )

    private fun label(id: Int) = string(id)

    private fun raw(value: String) = ConfirmationPreviewText.Raw(value)

    private fun string(
        id: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Resource(id, args.toList())

    private fun quantity(
        quantity: Int,
        singular: Int,
        multiple: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Quantity(quantity, singular, multiple, args.toList())
}
