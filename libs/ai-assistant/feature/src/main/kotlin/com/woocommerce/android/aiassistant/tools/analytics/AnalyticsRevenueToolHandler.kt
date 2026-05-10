package com.woocommerce.android.aiassistant.tools.analytics

import com.woocommerce.android.aiassistant.core.chat.AssistantToolHandler
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.chat.inputSchema
import com.woocommerce.android.aiassistant.core.chat.parseArgs
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.tools.handlers.cards.AnalyticsStatsCardId
import com.woocommerce.android.aiassistant.tools.handlers.cards.AnalyticsStatsKind
import com.woocommerce.android.aiassistant.tools.handlers.cards.toSyntheticId
import com.woocommerce.android.aiassistant.tools.validateAllowedArguments
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject

internal const val ANALYTICS_REVENUE_TOOL_NAME = "analytics_revenue"

internal class AnalyticsRevenueToolHandler @Inject constructor(
    private val dataSource: AIAnalyticsDataSource,
    @AiAssistantJson private val json: Json,
) : AssistantToolHandler {

    override val descriptor = ToolDescriptor(
        name = ANALYTICS_REVENUE_TOOL_NAME,
        description = "Revenue analytics for a date range. Returns totals and per-interval subtotals. " +
            "Prefer this over orders_list for aggregate revenue questions. For breakdown requests, set the " +
            "interval parameter directly to the implied dimension. When a request combines a grouping grain " +
            "with a date window, interval follows the grouping grain. Revenue/sales stats are card-backed: " +
            "after any successful call for a revenue or sales stats question, do not stop with prose; call " +
            "show_cards with family analytics_stats and the exact card_id returned by this tool. " +
            "Revenue card_id starts with analytics_revenue and uses currency:none when currency is omitted.",
        inputSchema = inputSchema {
            string("after", description = "Inclusive start date YYYY-MM-DD.", required = true)
            string("before", description = "Inclusive end date YYYY-MM-DD.", required = true)
            enum(
                "interval",
                values = AnalyticsInterval.values,
                description = "Bucketing interval; default 'day'.",
            )
            string("currency", description = "Optional ISO currency override.")
            enum(
                "compare_to",
                values = listOf(COMPARE_TO_PREVIOUS_PERIOD),
                description = "Optional comparison range. Use previous_period to include previous period totals.",
            )
        },
        safetyLevel = ToolSafetyLevel.SAFE,
    )

    @Suppress("ReturnCount")
    override suspend fun execute(call: ToolCall): ToolResult {
        validateAllowedArguments(call.arguments, ALLOWED_ARGUMENTS, ANALYTICS_REVENUE_TOOL_NAME).getOrElse {
            return analyticsValidationError(call.id, it.message ?: "Unsupported arguments")
        }
        val args = call.parseArgs<Args>(json).getOrElse {
            return analyticsValidationError(call.id, "Invalid arguments: ${it.message}")
        }
        val interval = AnalyticsInterval.fromValue(args.interval ?: AnalyticsInterval.DAY.value)
            ?: return analyticsValidationError(call.id, "interval must be one of ${AnalyticsInterval.values}")
        if (args.compareTo != null && args.compareTo != COMPARE_TO_PREVIOUS_PERIOD) {
            return analyticsValidationError(call.id, "compare_to must be previous_period")
        }
        val afterDate = parseAnalyticsDate(args.after)
        val beforeDate = parseAnalyticsDate(args.before)
        if (afterDate == null || beforeDate == null) {
            return analyticsValidationError(call.id, "after and before must be YYYY-MM-DD")
        }
        validateAnalyticsDateRange(afterDate, beforeDate, interval)?.let {
            return analyticsValidationError(call.id, it)
        }

        val currency = normaliseCurrency(args.currency)
        val cardId = analyticsRevenueCardId(args, interval, currency)
        val stats = dataSource.fetchRevenueStats(
            after = analyticsDateAfterBound(args.after),
            before = analyticsDateBeforeBound(args.before),
            interval = interval,
            currency = currency,
        ).getOrElse {
            return ToolResult.TransportError(toolCallId = call.id, retryable = true)
        }
        val previousPeriodStats = if (args.compareTo == COMPARE_TO_PREVIOUS_PERIOD) {
            val (previousAfter, previousBefore) = previousPeriodFor(afterDate, beforeDate)
            dataSource.fetchRevenueStats(
                after = analyticsDateAfterBound(previousAfter),
                before = analyticsDateBeforeBound(previousBefore),
                interval = interval,
                currency = currency,
            )
        } else {
            null
        }
        val previousPeriodTotals = previousPeriodStats?.getOrNull()?.totals as? JsonObject
        val previousPeriodPartial = previousPeriodStats?.isFailure == true

        return ToolResult.Success(
            toolCallId = call.id,
            structured = analyticsStatsSummary(
                after = args.after,
                before = args.before,
                interval = interval,
                stats = stats,
                cardId = cardId,
                currency = currency,
                previousPeriodTotals = previousPeriodTotals,
                previousPeriodPartial = previousPeriodPartial,
                previousPeriodWarning = PREVIOUS_PERIOD_WARNING.takeIf { previousPeriodPartial },
            ),
        )
    }

    private fun analyticsRevenueCardId(
        args: Args,
        interval: AnalyticsInterval,
        currency: String?,
    ) = AnalyticsStatsCardId(
        kind = AnalyticsStatsKind.Revenue,
        after = args.after,
        before = args.before,
        interval = interval,
        currency = currency,
    ).toSyntheticId()

    @Serializable
    private data class Args(
        val after: String,
        val before: String,
        val interval: String? = null,
        val currency: String? = null,
        @SerialName("compare_to")
        val compareTo: String? = null,
    )

    private companion object {
        const val COMPARE_TO_PREVIOUS_PERIOD = "previous_period"
        const val PREVIOUS_PERIOD_WARNING = "Previous period totals could not be fetched."

        val ALLOWED_ARGUMENTS = setOf("after", "before", "interval", "currency", "compare_to")
    }
}
