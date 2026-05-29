package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.testToolFailureDiagnosticsFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCStatsStore

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsOrdersToolHandlerTest {

    private val dataSource: AIAnalyticsDataSource = mock()
    private val handler = AnalyticsOrdersToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
        diagnosticsFactory = testToolFailureDiagnosticsFactory(),
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "analytics_orders", arguments = arguments)

    @Test
    fun `when descriptor is inspected, then after and before are required and interval is constrained`() {
        val description = handler.descriptor.description
        val schema = handler.descriptor.inputSchema
        val properties = requireNotNull(schema["properties"]).jsonObject
        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }
        val intervalValues = requireNotNull(properties["interval"])
            .jsonObject
            .getValue("enum")
            .jsonArray
            .map { it.jsonPrimitive.content }
        val compareToValues = requireNotNull(properties["compare_to"])
            .jsonObject
            .getValue("enum")
            .jsonArray
            .map { it.jsonPrimitive.content }

        assertThat(description).contains("sales")
        assertThat(description).contains("revenue")
        assertThat(description).contains("order")
        assertThat(description).contains("average order value")
        assertThat(description).contains("not customer cohorts")
        assertThat(description).contains("new customers")
        assertThat(description).contains("analytics_orders")
        assertThat(description).contains("show_cards")
        assertThat(description).contains("grouping grain with a date window")
        assertThat(description).contains("interval follows the grouping grain")
        assertThat(description).contains("card_id starts with analytics_orders")
        assertThat(description).contains("do not stop with prose")
        assertThat(description).contains("family analytics_stats")
        assertThat(description).contains("exact card_id")
        assertThat(description).contains("card_id")
        assertThat(required).containsExactly("after", "before")
        assertThat(intervalValues).containsExactly("hour", "day", "week", "month", "year")
        assertThat(compareToValues).containsExactly("previous_period")
    }

    @Test
    fun `given valid dates and week interval, when execute is called, then summary keeps totals and interval count`() =
        runTest {
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.WEEK,
                )
            ).thenReturn(Result.success(sampleStats()))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                        put("interval", "week")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val structured = (result as ToolResult.Success).structured.jsonObject
            assertThat(structured.getValue("after").jsonPrimitive.content).isEqualTo("2026-04-01")
            assertThat(structured.getValue("before").jsonPrimitive.content).isEqualTo("2026-04-30")
            assertThat(structured.getValue("interval").jsonPrimitive.content).isEqualTo("week")
            assertThat(structured.getValue("card_id").jsonPrimitive.content).isEqualTo(
                "analytics_orders:after:2026-04-01:before:2026-04-30:interval:week"
            )
            assertThat(structured.getValue("interval_count").jsonPrimitive.int).isEqualTo(1)
            assertThat(structured.getValue("totals").jsonObject.getValue("orders_count").jsonPrimitive.int)
                .isEqualTo(42)
            val totals = structured.getValue("totals").jsonObject
            assertThat(totals.getValue("total_sales").jsonPrimitive.content).isEqualTo("170.35")
            assertThat(totals.getValue("net_revenue").jsonPrimitive.content).isEqualTo("120.15")
            assertThat(totals.getValue("orders_count").jsonPrimitive.int).isEqualTo(42)
            assertThat(totals.getValue("avg_order_value").jsonPrimitive.content).isEqualTo("85.30")
        }

    @Test
    fun `given previous period comparison, when execute succeeds, then previous totals are returned`() =
        runTest {
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-05-01T00:00:00",
                    before = "2026-05-07T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(Result.success(sampleStats()))
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-04-24T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(Result.success(previousStats()))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-05-01")
                        put("before", "2026-05-07")
                        put("compare_to", "previous_period")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val structured = (result as ToolResult.Success).structured.jsonObject

            assertThat(structured.getValue("interval").jsonPrimitive.content).isEqualTo("day")
            assertThat(
                structured.getValue("previous_period_totals")
                    .jsonObject
                    .getValue("orders_count")
                    .jsonPrimitive
                    .int
            ).isEqualTo(24)
            verify(dataSource).fetchOrdersStats(
                after = "2026-04-24T00:00:00",
                before = "2026-04-30T23:59:59",
                interval = AnalyticsInterval.DAY,
            )
        }

    @Test
    fun `given previous period fetch fails, when primary succeeds, then partial primary result is returned`() =
        runTest {
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-05-01T00:00:00",
                    before = "2026-05-07T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(Result.success(sampleStats()))
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-04-24T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(Result.failure(RuntimeException("comparison failed")))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-05-01")
                        put("before", "2026-05-07")
                        put("compare_to", "previous_period")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val structured = (result as ToolResult.Success).structured.jsonObject
            assertThat(structured).doesNotContainKey("previous_period_totals")
            assertThat(structured.getValue("previous_period_partial").jsonPrimitive.boolean).isTrue()
            assertThat(structured.getValue("previous_period_warning").jsonPrimitive.content)
                .contains("could not be fetched")
        }

    @Test
    fun `given unsupported compare_to, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                        put("compare_to", "last_year")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            assertThat((result as ToolResult.ValidationError).reason).contains("compare_to")
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given unknown argument, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                        put("orderby", "orders_count")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            assertThat((result as ToolResult.ValidationError).reason).contains("Unsupported analytics_orders argument")
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given interval omitted, when execute is called, then day interval is used`() =
        runTest {
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(Result.success(sampleStats()))

            handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                    }
                )
            )

            verify(dataSource).fetchOrdersStats(
                after = "2026-04-01T00:00:00",
                before = "2026-04-30T23:59:59",
                interval = AnalyticsInterval.DAY,
            )
        }

    @Test
    fun `given required dates missing, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(toolCall(buildJsonObject { }))

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given invalid interval, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                        put("interval", "quarter")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given reversed date range, when execute is called, then ValidationError is returned before data source call`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-30")
                        put("before", "2026-04-01")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given day interval over one hundred buckets, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-01-01")
                        put("before", "2026-04-11")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            assertThat((result as ToolResult.ValidationError).reason)
                .contains("coarser interval or shorter range")
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given week interval over one hundred calendar buckets, when execute is called, then ValidationError is returned`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-01-06")
                        put("before", "2027-12-06")
                        put("interval", "week")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            assertThat((result as ToolResult.ValidationError).reason)
                .contains("coarser interval or shorter range")
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given data source fails, when execute is called, then lossy diagnostics are returned`() =
        runTest {
            whenever(
                dataSource.fetchOrdersStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                )
            ).thenReturn(
                Result.failure(OnChangedException(WCStatsStore.OrderStatsError(message = "network error")))
            )

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            result as ToolResult.TransportError
            assertThat(result.retryable).isTrue
            assertThat(result.diagnostics.tool?.toolName).isEqualTo("analytics_orders")
            assertThat(result.diagnostics.transport).isNull()
        }

    private fun sampleStats() = AnalyticsStats(
        totals = buildJsonObject {
            put("total_sales", "170.35")
            put("gross_sales", "190.00")
            put("net_revenue", "120.15")
            put("orders_count", 42)
            put("avg_order_value", "85.30")
        },
        intervals = listOf(
            buildJsonObject {
                put("interval", "week-2026-15")
                put("date_start", "2026-04-07 00:00:00")
                put(
                    "subtotals",
                    buildJsonObject {
                        put("total_sales", "50.00")
                        put("net_revenue", "35.00")
                        put("orders_count", 12)
                        put("avg_order_value", "80.10")
                    }
                )
            },
        ),
    )

    private fun previousStats() = AnalyticsStats(
        totals = buildJsonObject {
            put("orders_count", 24)
            put("avg_order_value", "72.15")
        },
        intervals = emptyList(),
    )
}
