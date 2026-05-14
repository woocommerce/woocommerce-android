package com.woocommerce.android.aiassistant.safety

import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.tools.orders.AIOrdersDataSource
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.OrderEntity

class OrdersConfirmationPreviewProviderTest {
    @Test
    fun `given order status update emails customer, when preview is built, then summary mentions notification`() =
        runTest {
            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "processing")
                },
            )

            assertThat(preview.summary).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_summary,
                    raw("42"),
                )
            )
            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_summary,
                    raw("42"),
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    value = raw("processing"),
                    label = label(R.string.ai_assistant_confirmation_field_status),
                ),
            )
        }

    @Test
    fun `given order update with large id, when preview is built, then id is preserved`() = runTest {
        val preview = preview(
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", 3_000_000_000L)
                put("status", "pending")
            },
        )

        assertThat(preview.message).isEqualTo(
            string(
                R.string.ai_assistant_confirmation_order_update_title,
                raw("3000000000"),
            )
        )
    }

    @Test
    fun `given single order update has note and email, when preview is built, then fields are included`() =
        runTest {
            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                    put("customer_note", "Thanks")
                    put("billing_email", "buyer@example.com")
                },
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title,
                    raw("42"),
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    value = raw("pending"),
                    label = label(R.string.ai_assistant_confirmation_field_status),
                ),
                ConfirmationPreviewField(
                    name = "customer_note",
                    value = raw("Thanks"),
                    label = label(R.string.ai_assistant_confirmation_field_customer_note),
                ),
                ConfirmationPreviewField(
                    name = "billing_email",
                    value = raw("buyer@example.com"),
                    label = label(R.string.ai_assistant_confirmation_field_billing_email),
                ),
            )
        }

    @Test
    fun `given order billing email update and current order, when preview is built, then before and after are included`() =
        runTest {
            val order = order(billingEmail = "old@example.com")
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrder(42L)).thenReturn(Result.success(order))

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("billing_email", "new@example.com")
                },
                dataSource = dataSource,
            )

            assertThat(preview.rows).containsExactly(
                ConfirmationPreviewField(
                    name = "billing_email",
                    label = label(R.string.ai_assistant_confirmation_field_billing_email),
                    value = raw("new@example.com"),
                    beforeValue = raw("old@example.com"),
                )
            )
        }

    @Test
    fun `given long order customer note update, when preview is built, then after value is capped without before`() =
        runTest {
            val longNote = "a".repeat(200)
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrder(42L)).thenReturn(Result.success(order(customerNote = "previous private note")))

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("customer_note", longNote)
                },
                dataSource = dataSource,
            )

            assertThat(preview.rows).containsExactly(
                ConfirmationPreviewField(
                    name = "customer_note",
                    label = label(R.string.ai_assistant_confirmation_field_customer_note),
                    value = raw("${"a".repeat(160)}..."),
                    beforeValue = null,
                )
            )
        }

    @Test
    fun `given bulk order status update emails customers, when preview is built, then summary mentions notification`() =
        runTest {
            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray((1..5).map { JsonPrimitive(it) }))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
            )

            assertThat(preview.isBulk).isTrue()
            assertThat(preview.message).isEqualTo(
                quantity(
                    quantity = 5,
                    singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                    multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    value = raw("completed"),
                    label = label(R.string.ai_assistant_confirmation_field_status),
                ),
            )
            assertThat(preview.bulkEntries).containsExactly(
                ConfirmationBulkEntry(1),
                ConfirmationBulkEntry(2),
                ConfirmationBulkEntry(3),
                ConfirmationBulkEntry(4),
                ConfirmationBulkEntry(5),
            )
        }

    @Test
    fun `given one order bulk status update emails customer, when preview is built, then singular summary is used`() =
        runTest {
            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(42))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
            )

            assertThat(preview.message).isEqualTo(
                quantity(
                    quantity = 1,
                    singular = R.string.ai_assistant_confirmation_orders_bulk_update_summary_single,
                    multiple = R.string.ai_assistant_confirmation_orders_bulk_update_summary_multiple,
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    value = raw("completed"),
                    label = label(R.string.ai_assistant_confirmation_field_status),
                ),
            )
        }

    @Test
    fun `given bulk order update has no status, when preview is built, then non-notifying title is used`() =
        runTest {
            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray((1..2).map { JsonPrimitive(it) }))
                    put("patch", buildJsonObject { put("customer_note", "Packed") })
                },
            )

            assertThat(preview.message).isEqualTo(
                quantity(
                    quantity = 2,
                    singular = R.string.ai_assistant_confirmation_orders_bulk_update_title_single,
                    multiple = R.string.ai_assistant_confirmation_orders_bulk_update_title_multiple,
                )
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "customer_note",
                    value = string(R.string.ai_assistant_confirmation_field_value_updated),
                    label = label(R.string.ai_assistant_confirmation_field_customer_note),
                ),
            )
        }

    @Test
    fun `given order update and current order, when preview is built, then before and after values are included`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrder(42L)).thenReturn(Result.success(order(status = "wc-pending")))

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "processing")
                },
                dataSource = dataSource,
            )

            assertThat(preview.rows).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    label = label(R.string.ai_assistant_confirmation_field_status),
                    value = raw("processing"),
                    beforeValue = raw("pending"),
                )
            )
            assertThat(preview.isBulk).isFalse()
        }

    @Test
    fun `given order update, when preview is built, then only order data source is fetched`() = runTest {
        val dataSource: AIOrdersDataSource = mock()
        whenever(dataSource.getOrder(42L)).thenReturn(Result.success(order(status = "wc-pending")))

        preview(
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", 42)
                put("status", "processing")
            },
            dataSource = dataSource,
        )

        verify(dataSource).getOrder(42L)
    }

    @Test
    fun `given order update has wrong-shaped primitive fields, when preview is built, then fields are omitted`() =
        runTest {
            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", buildJsonObject { put("value", "processing") })
                    put("billing_email", JsonArray(listOf(JsonPrimitive("buyer@example.com"))))
                },
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title,
                    raw("42"),
                )
            )
            assertThat(preview.fields).isEmpty()
        }

    @Test
    fun `given order update has wrong-shaped id, when preview is built, then fallback is used`() = runTest {
        val preview = preview(
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", buildJsonObject { put("value", 42) })
                put("status", "processing")
            },
        )

        assertThat(preview.message).isEqualTo(string(R.string.ai_assistant_confirmation_order_update_generic))
        assertThat(preview.fields).isEmpty()
    }

    @Test
    fun `given bulk order update has wrong-shaped ids, when preview is built, then fallback is used`() = runTest {
        val preview = preview(
            toolName = "orders_bulk_update",
            arguments = buildJsonObject {
                put("ids", buildJsonObject { put("value", 42) })
                put("patch", buildJsonObject { put("status", "completed") })
            },
        )

        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_orders_bulk_update_generic)
        )
        assertThat(preview.fields).isEmpty()
    }

    @Test
    fun `given bulk order update has non-numeric ids, when preview is built, then fallback is used`() = runTest {
        val preview = preview(
            toolName = "orders_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive("bad"))))
                put("patch", buildJsonObject { put("status", "completed") })
            },
        )

        assertThat(preview.message).isEqualTo(
            string(R.string.ai_assistant_confirmation_orders_bulk_update_generic)
        )
        assertThat(preview.fields).isEmpty()
    }

    @Test
    fun `given bulk order update has wrong-shaped patch fields, when preview is built, then fields are omitted`() =
        runTest {
            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
                    put(
                        "patch",
                        buildJsonObject {
                            put("status", JsonArray(listOf(JsonPrimitive("completed"))))
                            put("billing_email", buildJsonObject { put("value", "buyer@example.com") })
                        },
                    )
                },
            )

            assertThat(preview.message).isEqualTo(
                quantity(
                    quantity = 2,
                    singular = R.string.ai_assistant_confirmation_orders_bulk_update_title_single,
                    multiple = R.string.ai_assistant_confirmation_orders_bulk_update_title_multiple,
                )
            )
            assertThat(preview.fields).isEmpty()
        }

    private suspend fun preview(
        toolName: String,
        arguments: JsonObject,
        dataSource: AIOrdersDataSource? = null,
    ): ConfirmationPreview =
        OrdersConfirmationPreviewProvider(dataSource ?: failingDataSource()).buildPreview(context(toolName, arguments))

    private suspend fun failingDataSource(): AIOrdersDataSource {
        val dataSource: AIOrdersDataSource = mock()
        whenever(dataSource.getOrder(any())).thenReturn(Result.failure(IllegalStateException("No current order")))
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

    private fun order(
        status: String = "wc-pending",
        customerNote: String = "",
        billingEmail: String = "buyer@example.com",
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = 42L,
        status = status,
        customerNote = customerNote,
        billingEmail = billingEmail,
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
