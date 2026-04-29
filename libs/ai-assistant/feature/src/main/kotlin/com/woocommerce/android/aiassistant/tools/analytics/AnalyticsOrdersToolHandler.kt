package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject

internal class AnalyticsOrdersToolHandler @Inject constructor(
    private val dataSource: AIAnalyticsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = "analytics_orders",
        description = "Order analytics for a date range. Returns totals and per-interval subtotals. " +
            "Prefer this over orders_list for aggregate order-count questions.",
        inputSchema = inputSchema {
            string("after", description = "Inclusive start date YYYY-MM-DD.", required = true)
            string("before", description = "Inclusive end date YYYY-MM-DD.", required = true)
            enum(
                "interval",
                values = AnalyticsInterval.values,
                description = "Bucketing interval; default 'day'.",
            )
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    override suspend fun execute(call: ToolCall): ToolResult {
        val args = call.parseArgs<Args>(json).getOrElse {
            return analyticsValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        val interval = AnalyticsInterval.fromValue(args.interval ?: AnalyticsInterval.DAY.value)
            ?: return analyticsValidationError(call.id, "interval must be one of ${AnalyticsInterval.values}")
        if (!validateAnalyticsDate(args.after) || !validateAnalyticsDate(args.before)) {
            return analyticsValidationError(call.id, "after and before must be YYYY-MM-DD")
        }
        validateAnalyticsDateRange(args.after, args.before, interval)?.let {
            return analyticsValidationError(call.id, it)
        }

        return dataSource.fetchOrdersStats(
            after = analyticsDateAfterBound(args.after),
            before = analyticsDateBeforeBound(args.before),
            interval = interval,
        ).fold(
            onSuccess = { stats ->
                ToolResult.Success(
                    toolCallId = call.id,
                    structured = analyticsStatsSummary(args.after, args.before, stats),
                )
            },
            onFailure = { ToolResult.TransportError(toolCallId = call.id, retryable = true) },
        )
    }

    @Serializable
    private data class Args(
        val after: String,
        val before: String,
        val interval: String? = null,
    )
}
