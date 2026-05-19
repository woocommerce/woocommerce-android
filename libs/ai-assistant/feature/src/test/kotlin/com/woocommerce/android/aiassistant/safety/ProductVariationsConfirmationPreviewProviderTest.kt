package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.tools.products.AIProductVariationsDataSource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCProductVariationModel

class ProductVariationsConfirmationPreviewProviderTest {
    @Test
    @Suppress("LongMethod")
    fun `given product variation update, when preview is built, then product and variation ids are included`() =
        runTest {
            val preview = preview(
                arguments = buildJsonObject {
                    put("product_id", 7)
                    put("id", 8)
                    put("regular_price", "19.99")
                    put("sale_price", "")
                    put("stock_quantity", 3)
                    put("stock_status", "instock")
                    put("sku", "VAR-8")
                    put("status", "publish")
                },
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_variation_update_title,
                    raw("8"),
                    raw("7"),
                )
            )
            assertThat(preview.fields).containsExactlyElementsOf(expectedProductVariationFields())
        }

    @Test
    fun `given variation update and current variation, when preview is built, then before and after values are included`() =
        runTest {
            val dataSource: AIProductVariationsDataSource = mock()
            whenever(dataSource.getVariation(7L, 8L)).thenReturn(Result.success(variation(sku = "VAR-7")))

            val preview = preview(
                arguments = buildJsonObject {
                    put("product_id", 7)
                    put("id", 8)
                    put("sku", "VAR-8")
                },
                dataSource = dataSource,
            )

            assertThat(preview.rows).containsExactly(
                ConfirmationPreviewField(
                    name = "sku",
                    label = label(R.string.ai_assistant_confirmation_field_sku),
                    value = raw("VAR-8"),
                    beforeValue = raw("VAR-7"),
                )
            )
        }

    @Test
    fun `given variation update and current variation has attributes, when preview is built, then title includes options`() =
        runTest {
            val dataSource: AIProductVariationsDataSource = mock()
            whenever(dataSource.getVariation(7L, 8L)).thenReturn(
                Result.success(
                    variation(attributes = """[{"name":"Size","option":"M"},{"name":"Color","option":"Red"}]""")
                )
            )

            val preview = preview(
                arguments = buildJsonObject {
                    put("product_id", 7)
                    put("id", 8)
                    put("regular_price", "19.99")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_variation_update_title_with_name,
                    raw("M, Red"),
                    raw("8"),
                    raw("7"),
                )
            )
        }

    @Test
    fun `given variation update and current variation has sku but no attributes, when preview is built, then title includes sku`() =
        runTest {
            val dataSource: AIProductVariationsDataSource = mock()
            whenever(dataSource.getVariation(7L, 8L)).thenReturn(
                Result.success(variation(sku = "VAR-8", attributes = "[]"))
            )

            val preview = preview(
                arguments = buildJsonObject {
                    put("product_id", 7)
                    put("id", 8)
                    put("regular_price", "19.99")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_product_variation_update_title_with_name,
                    raw("VAR-8"),
                    raw("8"),
                    raw("7"),
                )
            )
        }

    @Test
    fun `given variation update, when preview is built, then only variation data source is fetched`() = runTest {
        val dataSource: AIProductVariationsDataSource = mock()
        whenever(dataSource.getVariation(7L, 8L)).thenReturn(Result.success(variation()))

        preview(
            arguments = buildJsonObject {
                put("product_id", 7)
                put("id", 8)
                put("sku", "VAR-8")
            },
            dataSource = dataSource,
        )

        verify(dataSource).getVariation(7L, 8L)
    }

    @Test
    fun `given variation update has wrong-shaped product id, when preview is built, then fallback is used`() =
        runTest {
            val preview = preview(
                arguments = buildJsonObject {
                    put("product_id", buildJsonObject { put("value", 7) })
                    put("id", 8)
                    put("sku", "VAR-8")
                },
            )

            assertThat(preview.message).isEqualTo(
                string(R.string.ai_assistant_confirmation_product_variation_update_generic)
            )
            assertThat(preview.fields).isEmpty()
        }

    @Test
    fun `given variation update has wrong-shaped variation id, when preview is built, then fallback is used`() =
        runTest {
            val preview = preview(
                arguments = buildJsonObject {
                    put("product_id", 7)
                    put("id", buildJsonObject { put("value", 8) })
                    put("sku", "VAR-8")
                },
            )

            assertThat(preview.message).isEqualTo(
                string(R.string.ai_assistant_confirmation_product_variation_update_generic)
            )
            assertThat(preview.fields).isEmpty()
        }

    private suspend fun preview(
        arguments: JsonObject,
        dataSource: AIProductVariationsDataSource? = null,
    ): ConfirmationPreview =
        ProductVariationsConfirmationPreviewProvider(dataSource ?: failingDataSource()).buildPreview(
            context(arguments)
        )

    private suspend fun failingDataSource(): AIProductVariationsDataSource {
        val dataSource: AIProductVariationsDataSource = mock()
        whenever(dataSource.getVariation(any(), any())).thenReturn(
            Result.failure(IllegalStateException("No current variation"))
        )
        return dataSource
    }

    private fun context(arguments: JsonObject) = ConfirmationPreviewContext(
        request = ConfirmationRequest(
            id = "confirmation-1",
            toolCallId = "call-1",
            toolName = "product_variations_update",
            arguments = arguments,
            safetyLevel = ToolSafetyLevel.UNSAFE,
        ),
        descriptor = ToolDescriptor(
            name = "product_variations_update",
            description = "product_variations_update descriptor",
            inputSchema = buildJsonObject {},
            safetyLevel = ToolSafetyLevel.UNSAFE,
        ),
    )

    private fun variation(
        sku: String = "VAR-7",
        regularPrice: String = "19.99",
        salePrice: String = "",
        stockQuantity: Double = 3.5,
        stockStatus: String = "instock",
        status: String = "publish",
        attributes: String = "",
    ) = WCProductVariationModel(
        remoteProductId = RemoteId(7L),
        remoteVariationId = RemoteId(8L),
        sku = sku,
        regularPrice = regularPrice,
        salePrice = salePrice,
        stockQuantity = stockQuantity,
        stockStatus = stockStatus,
        status = status,
        attributes = attributes,
    )

    private fun label(id: Int) = string(id)

    private fun raw(value: String) = ConfirmationPreviewText.Raw(value)

    private fun string(
        id: Int,
        vararg args: ConfirmationPreviewText,
    ) = ConfirmationPreviewText.Resource(id, args.toList())

    private fun expectedProductVariationFields() = listOf(
        ConfirmationPreviewField(
            name = "regular_price",
            value = raw("19.99"),
            label = label(R.string.ai_assistant_confirmation_field_regular_price),
        ),
        ConfirmationPreviewField(
            name = "sale_price",
            value = string(R.string.ai_assistant_confirmation_field_value_off),
            label = label(R.string.ai_assistant_confirmation_field_sale_price),
        ),
        ConfirmationPreviewField(
            name = "stock_quantity",
            value = raw("3"),
            label = label(R.string.ai_assistant_confirmation_field_stock_quantity),
        ),
        ConfirmationPreviewField(
            name = "stock_status",
            value = raw("instock"),
            label = label(R.string.ai_assistant_confirmation_field_stock_status),
        ),
        ConfirmationPreviewField(
            name = "status",
            value = raw("publish"),
            label = label(R.string.ai_assistant_confirmation_field_status),
        ),
        ConfirmationPreviewField(
            name = "sku",
            value = raw("VAR-8"),
            label = label(R.string.ai_assistant_confirmation_field_sku),
        ),
    )
}
