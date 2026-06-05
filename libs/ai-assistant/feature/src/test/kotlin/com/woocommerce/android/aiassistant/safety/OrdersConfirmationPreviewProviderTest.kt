package com.woocommerce.android.aiassistant.safety

import android.content.Context
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
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
import org.mockito.kotlin.never
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
    fun `given order update and current order has billing name, when preview is built, then title includes resolved name`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(
                dataSource.getOrders(listOf(42L))
            ).thenReturn(Result.success(cachedOrderLookup(order(billingFirstName = "Jane", billingLastName = "Doe"))))

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title_with_name,
                    raw("42"),
                    raw("Jane Doe"),
                )
            )
            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
        }

    @Test
    fun `given notifying order update and current order has billing name, when preview is built, then summary includes name`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(
                dataSource.getOrders(listOf(42L))
            ).thenReturn(Result.success(cachedOrderLookup(order(billingFirstName = "Jane", billingLastName = "Doe"))))

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "processing")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_summary_with_name,
                    raw("42"),
                    raw("Jane Doe"),
                )
            )
            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
        }

    @Test
    fun `given guest order update and current order has no billing name, when preview is built, then title includes guest`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(
                Result.success(cachedOrderLookup(order(customerId = 0L)))
            )

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title_with_name,
                    raw("42"),
                    raw("Guest"),
                )
            )
            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
        }

    @Test
    fun `given registered order update has email and no billing name, when preview is built, then title includes email`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(
                Result.success(cachedOrderLookup(order(customerId = 123L, billingEmail = "buyer@example.com")))
            )

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title_with_name,
                    raw("42"),
                    raw("buyer@example.com"),
                )
            )
            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
        }

    @Test
    fun `given registered order update has no billing name or email, when preview is built, then title includes customer id`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(
                Result.success(cachedOrderLookup(order(customerId = 123L, billingEmail = "")))
            )

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title_with_name,
                    raw("42"),
                    raw("Customer #123"),
                )
            )
            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
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
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(Result.success(cachedOrderLookup(order)))

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
            whenever(
                dataSource.getOrders(listOf(42L))
            ).thenReturn(Result.success(cachedOrderLookup(order(customerNote = "previous private note"))))

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
    fun `given bulk order update has resolved and unresolved ids, when preview is built, then entries preserve order and include available names`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(1L, 2L, 3L))).thenReturn(
                Result.success(
                    cachedOrderLookup(
                        order(orderId = 3L, billingFirstName = "Jane", billingLastName = "Doe"),
                        order(orderId = 1L, billingFirstName = "Sam", billingLastName = "Rivera"),
                    )
                )
            )

            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
                dataSource = dataSource,
            )

            assertThat(preview.bulkEntries).containsExactly(
                ConfirmationBulkEntry(1, "Sam Rivera"),
                ConfirmationBulkEntry(2),
                ConfirmationBulkEntry(3, "Jane Doe"),
            )
            assertThat(preview.bulkEntries.map { it.displayText }).containsExactly(
                "#1  Sam Rivera",
                "#2",
                "#3  Jane Doe",
            )
        }

    @Test
    fun `given bulk order update has unnamed guest and registered orders, when preview is built, then fallback names are used`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(1L, 2L, 3L))).thenReturn(
                Result.success(
                    cachedOrderLookup(
                        order(orderId = 1L, customerId = 0L, billingEmail = ""),
                        order(orderId = 2L, customerId = 123L, billingEmail = "buyer@example.com"),
                        order(orderId = 3L, customerId = 456L, billingEmail = ""),
                    )
                )
            )

            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
                dataSource = dataSource,
            )

            assertThat(preview.bulkEntries).containsExactly(
                ConfirmationBulkEntry(1, "Guest"),
                ConfirmationBulkEntry(2, "buyer@example.com"),
                ConfirmationBulkEntry(3, "Customer #456"),
            )
            assertThat(preview.bulkEntries.map { it.displayText }).containsExactly(
                "#1  Guest",
                "#2  buyer@example.com",
                "#3  Customer #456",
            )
            verify(dataSource).getOrders(listOf(1L, 2L, 3L))
            verify(dataSource, never()).getOrder(1L)
            verify(dataSource, never()).getOrder(2L)
            verify(dataSource, never()).getOrder(3L)
        }

    @Test
    fun `given bulk order update, when names are resolved, then one batch lookup is used for all ids`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(1L, 2L, 3L))).thenReturn(
                Result.success(cachedOrderLookup(order(orderId = 1L), order(orderId = 2L), order(orderId = 3L)))
            )

            preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2), JsonPrimitive(3))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
                dataSource = dataSource,
            )

            verify(dataSource).getOrders(listOf(1L, 2L, 3L))
            verify(dataSource, never()).getOrder(1L)
            verify(dataSource, never()).getOrder(2L)
            verify(dataSource, never()).getOrder(3L)
        }

    @Test
    fun `given single order update, when name is resolved, then single-id cache-first lookup is used`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(Result.success(cachedOrderLookup(order())))

            preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
        }

    @Test
    fun `given single order lookup returns a cache hit, when preview is built, then cached name is used`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(
                Result.success(cachedOrderLookup(order(billingFirstName = "Jane", billingLastName = "Doe")))
            )

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(
                    R.string.ai_assistant_confirmation_order_update_title_with_name,
                    raw("42"),
                    raw("Jane Doe"),
                )
            )
            verify(dataSource).getOrders(listOf(42L))
            verify(dataSource, never()).getOrder(42L)
        }

    @Test
    fun `given bulk order lookup returns cache hits, when preview is built, then cached names are used`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(1L, 2L))).thenReturn(
                Result.success(
                    cachedOrderLookup(
                        order(orderId = 1L, billingFirstName = "Jane", billingLastName = "Doe"),
                        order(orderId = 2L, billingFirstName = "Sam", billingLastName = "Rivera"),
                    )
                )
            )

            val preview = preview(
                toolName = "orders_bulk_update",
                arguments = buildJsonObject {
                    put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
                    put("patch", buildJsonObject { put("status", "completed") })
                },
                dataSource = dataSource,
            )

            assertThat(preview.bulkEntries).containsExactly(
                ConfirmationBulkEntry(1, "Jane Doe"),
                ConfirmationBulkEntry(2, "Sam Rivera"),
            )
            verify(dataSource).getOrders(listOf(1L, 2L))
            verify(dataSource, never()).getOrder(1L)
            verify(dataSource, never()).getOrder(2L)
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
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(
                Result.success(cachedOrderLookup(order(status = "wc-pending")))
            )

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
    fun `given cache-first order lookup fails, when single preview is built, then title is id only and fields remain`() =
        runTest {
            val dataSource: AIOrdersDataSource = mock()
            whenever(dataSource.getOrders(listOf(42L))).thenReturn(
                Result.failure(IllegalStateException("No current order"))
            )

            val preview = preview(
                toolName = "orders_update",
                arguments = buildJsonObject {
                    put("id", 42)
                    put("status", "pending")
                },
                dataSource = dataSource,
            )

            assertThat(preview.message).isEqualTo(
                string(R.string.ai_assistant_confirmation_order_update_title, raw("42"))
            )
            assertThat(preview.fields).containsExactly(
                ConfirmationPreviewField(
                    name = "status",
                    value = raw("pending"),
                    label = label(R.string.ai_assistant_confirmation_field_status),
                ),
            )
        }

    @Test
    fun `given bulk order lookup fails, when preview is built, then all entries are id only`() = runTest {
        val dataSource: AIOrdersDataSource = mock()
        whenever(dataSource.getOrders(listOf(1L, 2L))).thenReturn(
            Result.failure(IllegalStateException("No current orders"))
        )

        val preview = preview(
            toolName = "orders_bulk_update",
            arguments = buildJsonObject {
                put("ids", JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))))
                put("patch", buildJsonObject { put("status", "completed") })
            },
            dataSource = dataSource,
        )

        assertThat(preview.bulkEntries).containsExactly(
            ConfirmationBulkEntry(1),
            ConfirmationBulkEntry(2),
        )
        assertThat(preview.bulkEntries.map { it.displayText }).containsExactly("#1", "#2")
    }

    @Test
    fun `given order update, when preview is built, then only order data source is fetched`() = runTest {
        val dataSource: AIOrdersDataSource = mock()
        whenever(dataSource.getOrders(listOf(42L))).thenReturn(
            Result.success(cachedOrderLookup(order(status = "wc-pending")))
        )

        preview(
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", 42)
                put("status", "processing")
            },
            dataSource = dataSource,
        )

        verify(dataSource).getOrders(listOf(42L))
        verify(dataSource, never()).getOrder(42L)
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
        OrdersConfirmationPreviewProvider(appContext(), dataSource ?: failingDataSource())
            .buildPreview(context(toolName, arguments))

    private suspend fun failingDataSource(): AIOrdersDataSource {
        val dataSource: AIOrdersDataSource = mock()
        whenever(dataSource.getOrder(any())).thenReturn(Result.failure(IllegalStateException("No current order")))
        whenever(dataSource.getOrders(any())).thenReturn(
            Result.failure(IllegalStateException("No current orders"))
        )
        return dataSource
    }

    private fun appContext(): Context {
        val context: Context = mock()
        whenever(context.getString(R.string.ai_assistant_confirmation_order_guest_display_name)).thenReturn("Guest")
        whenever(
            context.getString(R.string.ai_assistant_confirmation_order_registered_customer_display_name, "123")
        ).thenReturn("Customer #123")
        whenever(
            context.getString(R.string.ai_assistant_confirmation_order_registered_customer_display_name, "456")
        ).thenReturn("Customer #456")
        return context
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
        orderId: Long = 42L,
        status: String = "wc-pending",
        customerNote: String = "",
        billingEmail: String = "buyer@example.com",
        billingFirstName: String = "",
        billingLastName: String = "",
        customerId: Long = 1L,
    ) = OrderEntity(
        localSiteId = LocalId(1),
        orderId = orderId,
        status = status,
        customerId = customerId,
        customerNote = customerNote,
        billingEmail = billingEmail,
        billingFirstName = billingFirstName,
        billingLastName = billingLastName,
    )

    private fun cachedOrderLookup(vararg orders: OrderEntity) = CachedLookupResult(
        items = orders.toList(),
        cacheHitCount = orders.size,
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
