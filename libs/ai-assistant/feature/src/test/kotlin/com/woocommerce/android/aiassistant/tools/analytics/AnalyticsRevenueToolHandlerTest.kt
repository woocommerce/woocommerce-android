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
}
