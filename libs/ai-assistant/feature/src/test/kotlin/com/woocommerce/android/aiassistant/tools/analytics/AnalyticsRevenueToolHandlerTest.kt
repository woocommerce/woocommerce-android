package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
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
        val schema = handler.descriptor.inputSchema
        val properties = requireNotNull(schema["properties"]).jsonObject
        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }
        val intervalValues = requireNotNull(properties["interval"])
            .jsonObject
            .getValue("enum")
            .jsonArray
            .map { it.jsonPrimitive.content }

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
                .isEqualTo("12345.67")

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
    fun `given interval subtotals, when execute succeeds, then structured result includes revenue and order chart points`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal("2026-04-01", "10.00", orderCount = 1),
                        intervalSubtotal("2026-04-02", "-2.50", orderCount = 3),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartDates())
                .containsExactly("2026-04-01", "2026-04-02")
            assertThat(structured.revenueChartValues())
                .containsExactly(10.0, -2.5)
            assertThat(structured.orderChartDates())
                .containsExactly("2026-04-01", "2026-04-02")
            assertThat(structured.orderChartValues())
                .containsExactly(1.0, 3.0)
        }

    @Test
    fun `given empty intervals, when execute succeeds, then revenue and order charts are empty`() = runTest {
        givenRevenueStats(sampleStats(intervalSubtotals = emptyList()))

        val structured = whenRevenueToolExecutes()

        assertThat(structured.getValue("revenue_chart").jsonArray).isEmpty()
        assertThat(structured.getValue("order_chart").jsonArray).isEmpty()
    }

    @Test
    fun `given one interval, when execute succeeds, then revenue and order charts have one point`() = runTest {
        givenRevenueStats(
            sampleStats(intervalSubtotals = listOf(intervalSubtotal("2026-04-01", "10.00", orderCount = 1)))
        )

        val structured = whenRevenueToolExecutes()

        assertThat(structured.getValue("revenue_chart").jsonArray).hasSize(1)
        assertThat(structured.getValue("order_chart").jsonArray).hasSize(1)
    }

    @Test
    fun `given all zero intervals, when execute succeeds, then zero revenue and order chart values are retained`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal("2026-04-01", "0.00", orderCount = 0),
                        intervalSubtotal("2026-04-02", "0", orderCount = 0),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartValues()).containsExactly(0.0, 0.0)
            assertThat(structured.orderChartValues()).containsExactly(0.0, 0.0)
        }

    @Test
    fun `given negative revenue intervals, when execute succeeds, then negative revenue and order values are retained`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal("2026-04-01", "-5.00", orderCount = -1),
                        intervalSubtotal("2026-04-02", "-1.25", orderCount = -2),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartValues()).containsExactly(-5.0, -1.25)
            assertThat(structured.orderChartValues()).containsExactly(-1.0, -2.0)
        }

    @Test
    fun `given non numeric interval metrics, when execute succeeds, then malformed chart points are skipped per series`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal("2026-04-01", "n/a", orderCount = 3),
                        intervalSubtotal("2026-04-02", "8.50", orderCount = "n/a"),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartDates()).containsExactly("2026-04-02")
            assertThat(structured.orderChartDates()).containsExactly("2026-04-01")
        }

    @Test
    fun `given malformed interval date and missing date start, when execute succeeds, then both chart points are skipped`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal(
                            interval = "2026-99-99",
                            revenue = "10.00",
                            orderCount = 1,
                            dateStart = null,
                        ),
                        intervalSubtotal("2026-04-02", "8.50", orderCount = 2),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartDates()).containsExactly("2026-04-02")
            assertThat(structured.orderChartDates()).containsExactly("2026-04-02")
        }

    @Test
    fun `given weekly interval label with valid date start, when execute succeeds, then both chart points use date start`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal(
                            interval = "week-2026-15",
                            revenue = "50.00",
                            orderCount = 4,
                            dateStart = "2026-04-06 00:00:00",
                        ),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartDates()).containsExactly("2026-04-06")
            assertThat(structured.orderChartDates()).containsExactly("2026-04-06")
        }

    @Test
    fun `given monthly interval label with valid date start, when execute succeeds, then both chart points use date start`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal(
                            interval = "month-2026-04",
                            revenue = "75.00",
                            orderCount = 7,
                            dateStart = "2026-04-01 00:00:00",
                        ),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartDates()).containsExactly("2026-04-01")
            assertThat(structured.orderChartDates()).containsExactly("2026-04-01")
        }

    @Test
    fun `given non date interval label and invalid date start, when execute succeeds, then chart point is skipped`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal(
                            interval = "week-2026-15",
                            revenue = "50.00",
                            dateStart = "not-a-date",
                        ),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.getValue("revenue_chart").jsonArray).isEmpty()
            assertThat(structured.getValue("order_chart").jsonArray).isEmpty()
        }

    @Test
    fun `given missing interval order counts, when execute succeeds, then order chart is empty and revenue chart remains`() =
        runTest {
            givenRevenueStats(
                sampleStats(
                    intervalSubtotals = listOf(
                        intervalSubtotal("2026-04-01", "50.00", orderCount = null),
                        intervalSubtotal("2026-04-02", "75.00", orderCount = null),
                    )
                )
            )

            val structured = whenRevenueToolExecutes()

            assertThat(structured.revenueChartValues()).containsExactly(50.0, 75.0)
            assertThat(structured.getValue("order_chart").jsonArray).isEmpty()
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

    private fun JsonObject.revenueChartDates(): List<String> =
        getValue("revenue_chart").jsonArray.map {
            it.jsonObject.getValue("date").jsonPrimitive.content
        }

    private fun JsonObject.revenueChartValues(): List<Double> =
        getValue("revenue_chart").jsonArray.map {
            it.jsonObject.getValue("value").jsonPrimitive.double
        }

    private fun JsonObject.orderChartDates(): List<String> =
        getValue("order_chart").jsonArray.map {
            it.jsonObject.getValue("date").jsonPrimitive.content
        }

    private fun JsonObject.orderChartValues(): List<Double> =
        getValue("order_chart").jsonArray.map {
            it.jsonObject.getValue("value").jsonPrimitive.double
        }

    private fun sampleStats() = AnalyticsStats(
        totals = buildJsonObject {
            put("net_revenue", "12345.67")
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
                    }
                )
            },
        ),
    )

    private fun sampleStats(intervalSubtotals: List<JsonObject>) = AnalyticsStats(
        totals = buildJsonObject {
            put("net_revenue", "12345.67")
            put("orders_count", 42)
        },
        intervals = intervalSubtotals,
    )

    private fun intervalSubtotal(
        interval: String,
        revenue: String,
        orderCount: Any? = 1,
        dateStart: String? = "$interval 00:00:00",
    ) = buildJsonObject {
        put("interval", interval)
        dateStart?.let { put("date_start", it) }
        put(
            "subtotals",
            buildJsonObject {
                put("net_revenue", revenue)
                orderCount?.let { putAny("orders_count", it) }
            }
        )
    }

    private fun JsonObjectBuilder.putAny(name: String, value: Any) {
        when (value) {
            is Number -> put(name, value)
            is String -> put(name, value)
            is JsonElement -> put(name, value)
            else -> error("Unsupported test value $value")
        }
    }
}
