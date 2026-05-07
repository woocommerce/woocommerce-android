package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsRevenueToolHandlerTest {

    private val dataSource: AIAnalyticsDataSource = mock()
    private val handler = AnalyticsRevenueToolHandler(
        dataSource = dataSource,
        json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
            explicitNulls = false
        },
    )

    private fun toolCall(arguments: JsonObject): ToolCall =
        ToolCall(id = "call-1", name = "analytics_revenue", arguments = arguments)

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

        assertThat(description).contains("show_cards")
        assertThat(description).contains("Revenue/sales stats are card-backed")
        assertThat(description).contains("do not stop with prose")
        assertThat(description).contains("family analytics_stats")
        assertThat(description).contains("analytics_stats")
        assertThat(description).contains("same after/before/interval/currency")
        assertThat(description).contains("currency:none")
        assertThat(required).containsExactly("after", "before")
        assertThat(intervalValues).containsExactly("hour", "day", "week", "month", "year")
    }

    @Test
    fun `given valid dates, when execute is called, then summary keeps totals and interval subtotals`() =
        runTest {
            whenever(
                dataSource.fetchRevenueStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                    currency = null,
                )
            ).thenReturn(Result.success(sampleStats()))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.Success::class.java)
            val structured = (result as ToolResult.Success).structured.jsonObject
            assertThat(structured.getValue("after").jsonPrimitive.content).isEqualTo("2026-04-01")
            assertThat(structured.getValue("before").jsonPrimitive.content).isEqualTo("2026-04-30")
            assertThat(structured.getValue("interval_count").jsonPrimitive.int).isEqualTo(2)
            assertThat(structured.getValue("totals").jsonObject.getValue("net_revenue").jsonPrimitive.content)
                .isEqualTo("120.15")

            val firstInterval = structured.getValue("interval_subtotals").jsonArray.first().jsonObject
            assertThat(firstInterval.keys).containsExactlyInAnyOrder("interval", "date_start", "subtotals")
            assertThat(firstInterval.getValue("subtotals").jsonObject.getValue("orders_count").jsonPrimitive.int)
                .isEqualTo(5)
        }

    @Test
    fun `given currency with whitespace, when execute is called, then trimmed currency is passed to data source`() =
        runTest {
            whenever(
                dataSource.fetchRevenueStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.WEEK,
                    currency = "USD",
                )
            ).thenReturn(Result.success(sampleStats()))

            handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                        put("interval", "week")
                        put("currency", " USD ")
                    }
                )
            )

            verify(dataSource).fetchRevenueStats(
                after = "2026-04-01T00:00:00",
                before = "2026-04-30T23:59:59",
                interval = AnalyticsInterval.WEEK,
                currency = "USD",
            )
        }

    @Test
    fun `given blank currency, when execute is called, then currency is omitted`() =
        runTest {
            whenever(
                dataSource.fetchRevenueStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                    currency = null,
                )
            ).thenReturn(Result.success(sampleStats()))

            handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                        put("currency", "   ")
                    }
                )
            )

            verify(dataSource).fetchRevenueStats(
                after = "2026-04-01T00:00:00",
                before = "2026-04-30T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = null,
            )
        }

    @Test
    fun `given invalid date, when execute is called, then ValidationError is returned before data source call`() =
        runTest {
            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-99-99")
                        put("before", "2026-04-30")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
            verifyNoInteractions(dataSource)
        }

    @Test
    fun `given data source fails, when execute is called, then retryable TransportError is returned`() =
        runTest {
            whenever(
                dataSource.fetchRevenueStats(
                    after = "2026-04-01T00:00:00",
                    before = "2026-04-30T23:59:59",
                    interval = AnalyticsInterval.DAY,
                    currency = null,
                )
            ).thenReturn(Result.failure(IllegalStateException("network error")))

            val result = handler.execute(
                toolCall(
                    buildJsonObject {
                        put("after", "2026-04-01")
                        put("before", "2026-04-30")
                    }
                )
            )

            assertThat(result).isInstanceOf(ToolResult.TransportError::class.java)
            assertThat((result as ToolResult.TransportError).retryable).isTrue
        }

    @Test
    fun `given interval subtotals, when execute succeeds, then structured result does not include Android chart arrays`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal("2026-04-01", totalSales = "10.00", netRevenue = "8.00"),
                        intervalSubtotal("2026-04-02", totalSales = "-2.50", netRevenue = "-3.50"),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.keys).doesNotContain(
                "revenue_chart",
                "order_chart",
                "total_sales_chart",
                "net_sales_chart",
            )
            assertThat(structured.getValue("interval_subtotals").jsonArray).hasSize(2)
        }

    @Test
    fun `given total sales gross sales and net revenue, when execute succeeds, then fields are preserved in totals`() =
        runTest {
            givenRevenueStats(sampleStats())

            val structured = whenRevenueToolExecutes()
            val totals = structured.getValue("totals").jsonObject

            assertThat(totals.getValue("total_sales").jsonPrimitive.content).isEqualTo("170.35")
            assertThat(totals.getValue("gross_sales").jsonPrimitive.content).isEqualTo("190.00")
            assertThat(totals.getValue("net_revenue").jsonPrimitive.content).isEqualTo("120.15")
        }

    @Test
    fun `given interval subtotals with date start, when execute succeeds, then date start remains available for card parser`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal(
                            interval = "week-2026-15",
                            totalSales = "50.00",
                            netRevenue = "35.00",
                            dateStart = "2026-04-06 00:00:00",
                        ),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()
            val firstInterval = structured.getValue("interval_subtotals").jsonArray.first().jsonObject

            assertThat(firstInterval.getValue("interval").jsonPrimitive.content).isEqualTo("week-2026-15")
            assertThat(firstInterval.getValue("date_start").jsonPrimitive.content).isEqualTo("2026-04-06 00:00:00")
        }

    private suspend fun givenRevenueStats(stats: AnalyticsStats) {
        whenever(
            dataSource.fetchRevenueStats(
                after = "2026-04-01T00:00:00",
                before = "2026-04-30T23:59:59",
                interval = AnalyticsInterval.DAY,
                currency = "USD",
            )
        ).thenReturn(Result.success(stats))
    }

    private suspend fun whenRevenueToolExecutes(): JsonObject {
        val result = handler.execute(
            toolCall(
                buildJsonObject {
                    put("after", "2026-04-01")
                    put("before", "2026-04-30")
                    put("currency", "USD")
                }
            )
        )
        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        return (result as ToolResult.Success).structured.jsonObject
    }

    private fun sampleStats() = AnalyticsStats(
        totals = buildJsonObject {
            put("total_sales", "170.35")
            put("gross_sales", "190.00")
            put("net_revenue", "120.15")
            put("orders_count", 42)
        },
        intervals = listOf(
            buildJsonObject {
                put("interval", "2026-04-01")
                put("date_start", "2026-04-01 00:00:00")
                put("extra", "ignored")
                put(
                    "subtotals",
                    buildJsonObject {
                        put("orders_count", 5)
                        put("total_sales", "100.00")
                        put("net_revenue", "80.00")
                    }
                )
            },
            buildJsonObject {
                put("interval", "2026-04-02")
                put("date_start", "2026-04-02 00:00:00")
                put(
                    "subtotals",
                    buildJsonObject {
                        put("orders_count", 7)
                        put("total_sales", "70.35")
                        put("net_revenue", "40.15")
                    }
                )
            },
        ),
    )

    private fun sampleStats(intervalSubtotals: List<JsonObject>) = AnalyticsStats(
        totals = buildJsonObject {
            put("total_sales", "170.35")
            put("gross_sales", "190.00")
            put("net_revenue", "120.15")
            put("orders_count", 42)
        },
        intervals = intervalSubtotals,
    )

    private fun intervalSubtotal(
        interval: String,
        totalSales: String,
        netRevenue: String,
        dateStart: String? = "$interval 00:00:00",
    ) = buildJsonObject {
        put("interval", interval)
        dateStart?.let { put("date_start", it) }
        put(
            "subtotals",
            buildJsonObject {
                put("total_sales", totalSales)
                put("net_revenue", netRevenue)
            }
        )
    }
}
