package com.woocommerce.android.aiassistant.ui

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.woocommerce.android.aiassistant.R
import com.woocommerce.android.aiassistant.core.chat.AssistantError
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.ui.cards.AssistantCard
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AssistantUiStateTest {
    @Test
    fun `given inline error is on assistant bubble, when resolving text color, then use error color`() {
        val colorScheme = lightColorScheme(
            error = Color.Red,
            onErrorContainer = Color.White,
            surface = Color.White,
        )

        assertThat(colorScheme.assistantInlineErrorTextColor()).isEqualTo(Color.Red)
        assertThat(colorScheme.assistantInlineErrorTextColor()).isNotEqualTo(colorScheme.onErrorContainer)
    }

    @Test
    fun `given transient assistant errors, when checking retry action support, then retry is supported`() {
        val retryableErrors = listOf(
            AssistantError.Network,
            AssistantError.Timeout,
            AssistantError.RateLimit,
        )

        retryableErrors.forEach { error ->
            assertThat(error.supportsRetryAction()).isTrue()
        }
    }

    @Test
    fun `given assistant errors, when mapping to message resources, then product copy resources are returned`() {
        assertThat(AssistantError.Network.toMessageRes()).isEqualTo(R.string.assistant_chat_error_network)
        assertThat(AssistantError.Timeout.toMessageRes()).isEqualTo(R.string.assistant_chat_error_timeout)
        assertThat(AssistantError.RateLimit.toMessageRes()).isEqualTo(R.string.assistant_chat_error_rate_limit)
        assertThat(AssistantError.Auth.toMessageRes()).isEqualTo(R.string.assistant_chat_error_auth)
        assertThat(AssistantError.UpstreamFailure.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_upstream_failure)
        assertThat(AssistantError.ToolFailed(toolName = "orders_update").toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_tool_failed)
        assertThat(AssistantError.InvalidToolCall(toolName = "orders_update").toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_invalid_tool_call)
        assertThat(AssistantError.OutcomeUnknown(toolName = "orders_update").toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_outcome_unknown)
        assertThat(AssistantError.Cancelled.toMessageRes()).isEqualTo(R.string.assistant_chat_error_cancelled)
        assertThat(AssistantError.Unknown().toMessageRes()).isEqualTo(R.string.assistant_chat_error_unknown)
    }

    @Test
    fun `given raw throwable messages, when mapping errors, then raw messages are not used`() {
        val rawCause = IllegalStateException("raw upstream token abc123")

        assertThat(AssistantError.Unknown(cause = rawCause).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_unknown)
        assertThat(AssistantError.ToolFailed(toolName = "orders_update", cause = rawCause).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_tool_failed)
    }

    @Test
    fun `given target bottom extends below viewport, when checking pin state, then target is not pinned`() {
        assertThat(
            isRenderedTargetPinnedToViewportEnd(
                distanceFromViewportEnd = -1,
                bottomPinThresholdPx = 48,
            )
        ).isFalse()
    }

    @Test
    fun `given target bottom is near viewport end, when checking pin state, then target is pinned`() {
        assertThat(
            isRenderedTargetPinnedToViewportEnd(
                distanceFromViewportEnd = 48,
                bottomPinThresholdPx = 48,
            )
        ).isTrue()
    }

    @Test
    fun `given target bottom is far above viewport end, when checking pin state, then target is not pinned`() {
        assertThat(
            isRenderedTargetPinnedToViewportEnd(
                distanceFromViewportEnd = 49,
                bottomPinThresholdPx = 48,
            )
        ).isFalse()
    }

    @Test
    fun `given error state with no inline message error, when checking fallback, then fallback is visible`() {
        val state = AssistantUiState(
            status = AssistantUiStatus.ERROR,
            error = AssistantUiError.MAX_ITERATIONS,
            messages = listOf(
                AssistantUiMessage(
                    id = "message-1",
                    role = AssistantUiMessage.Role.USER,
                    text = "Hello",
                ),
                AssistantUiMessage(
                    id = "message-2",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                ),
            ),
        )

        assertThat(state.shouldShowFallbackError).isTrue()
        assertThat(requireNotNull(state.error).toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_max_iterations)
    }

    @Test
    fun `given error state with inline message error, when checking fallback, then fallback is hidden`() {
        val state = AssistantUiState(
            status = AssistantUiStatus.ERROR,
            error = AssistantUiError.NETWORK,
            messages = listOf(
                AssistantUiMessage(
                    id = "message-1",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    text = "",
                    error = AssistantMessageError(
                        error = AssistantError.Network,
                        canRetry = true,
                    ),
                ),
            ),
        )

        assertThat(state.shouldShowFallbackError).isFalse()
    }

    @Test
    fun `given streaming with active empty assistant message, when checking typing indicator, then it is visible`() {
        val state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, ""),
            ),
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = "assistant-1",
        )

        assertThat(state.shouldShowTypingIndicator).isTrue()
    }

    @Test
    fun `given streaming with active tool activity but no text, when checking typing indicator, then it is visible`() {
        val state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage(
                    id = "assistant-1",
                    role = AssistantUiMessage.Role.ASSISTANT,
                    segments = listOf(
                        AssistantUiSegment.ToolActivity(
                            AssistantToolActivity(toolCallId = "call-1", toolName = "orders_list"),
                        ),
                    ),
                ),
            ),
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = "assistant-1",
        )

        assertThat(state.shouldShowTypingIndicator).isTrue()
    }

    @Test
    fun `given streaming with active assistant message that has streamed text, when checking typing indicator, then it is hidden`() {
        val state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, "Sales are up today."),
            ),
            status = AssistantUiStatus.STREAMING,
            activeAssistantMessageId = "assistant-1",
        )

        assertThat(state.shouldShowTypingIndicator).isFalse()
    }

    @Test
    fun `given idle status, when checking typing indicator, then it is hidden`() {
        val state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, ""),
            ),
            status = AssistantUiStatus.IDLE,
            activeAssistantMessageId = null,
        )

        assertThat(state.shouldShowTypingIndicator).isFalse()
    }

    @Test
    fun `given awaiting confirmation, when checking typing indicator, then it is hidden`() {
        val state = AssistantUiState(
            messages = listOf(
                AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, ""),
            ),
            status = AssistantUiStatus.AWAITING_CONFIRMATION,
            activeAssistantMessageId = "assistant-1",
        )

        assertThat(state.shouldShowTypingIndicator).isFalse()
    }

    @Test
    fun `given known tool names, when resolving activity label, then humanized labels are returned`() {
        val expectedLabels = mapOf(
            "orders_list" to R.string.assistant_chat_tool_activity_orders_read,
            "orders_get" to R.string.assistant_chat_tool_activity_orders_read,
            "orders_update" to R.string.assistant_chat_tool_activity_orders_write,
            "orders_bulk_update" to R.string.assistant_chat_tool_activity_orders_write,
            "products_list" to R.string.assistant_chat_tool_activity_products_read,
            "products_get" to R.string.assistant_chat_tool_activity_products_read,
            "product_variations_list" to R.string.assistant_chat_tool_activity_products_read,
            "products_update" to R.string.assistant_chat_tool_activity_products_write,
            "products_bulk_update" to R.string.assistant_chat_tool_activity_products_write,
            "product_variations_update" to R.string.assistant_chat_tool_activity_products_write,
            "analytics_orders" to R.string.assistant_chat_tool_activity_analytics,
            "analytics_revenue" to R.string.assistant_chat_tool_activity_analytics,
            "customers_list" to R.string.assistant_chat_tool_activity_customers,
        )

        expectedLabels.forEach { (toolName, labelRes) ->
            assertThat(AssistantToolActivity("call-1", toolName).labelRes())
                .describedAs(toolName)
                .isEqualTo(labelRes)
        }
    }

    @Test
    fun `given unknown tool name, when resolving activity label, then generic label is returned`() {
        assertThat(AssistantToolActivity("call-1", "private_internal_tool").labelRes())
            .isEqualTo(R.string.assistant_chat_tool_activity_generic)
    }

    @Test
    fun `given ui errors, when mapping to message resources, then fallback copy resources are returned`() {
        assertThat(AssistantUiError.CONFIRMATION_DEFERRED.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_confirmation_deferred)
        assertThat(AssistantUiError.MAX_ITERATIONS.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_max_iterations)
        assertThat(AssistantUiError.CANCELLED.toMessageRes())
            .isEqualTo(R.string.assistant_chat_error_cancelled)
    }

    @Test
    fun `when status is streaming, then turn is active`() {
        val state = AssistantUiState(status = AssistantUiStatus.STREAMING)

        assertThat(state.isTurnActive).isTrue()
    }

    @Test
    fun `when status is awaiting confirmation, then turn is active`() {
        val state = AssistantUiState(status = AssistantUiStatus.AWAITING_CONFIRMATION)

        assertThat(state.isTurnActive).isTrue()
    }

    @Test
    fun `when turn is streaming, then stop control is visible`() {
        val state = AssistantUiState(status = AssistantUiStatus.STREAMING)

        assertThat(state.shouldShowStopControl).isTrue()
    }

    @Test
    fun `when turn is awaiting confirmation, then stop control is hidden`() {
        val state = AssistantUiState(status = AssistantUiStatus.AWAITING_CONFIRMATION)

        assertThat(state.shouldShowStopControl).isFalse()
    }

    @Test
    fun `when status is idle, then turn is not active`() {
        val state = AssistantUiState(status = AssistantUiStatus.IDLE)

        assertThat(state.isTurnActive).isFalse()
    }

    @Test
    fun `when status is error, then turn is not active`() {
        val state = AssistantUiState(status = AssistantUiStatus.ERROR)

        assertThat(state.isTurnActive).isFalse()
    }

    @Test
    fun `given confirmation card state, when resolving chrome resources, then eyebrow and icon are mapped`() {
        assertThat(AssistantConfirmationCardState.PENDING.eyebrowRes())
            .isEqualTo(R.string.assistant_confirmation_eyebrow_pending)
        assertThat(AssistantConfirmationCardState.PENDING.iconRes())
            .isEqualTo(R.drawable.ic_assistant_confirmation_pending)
        assertThat(AssistantConfirmationCardState.CONFIRMED.eyebrowRes())
            .isEqualTo(R.string.assistant_confirmation_eyebrow_confirmed)
        assertThat(AssistantConfirmationCardState.CONFIRMED.iconRes())
            .isEqualTo(R.drawable.ic_assistant_confirmation_confirmed)
        assertThat(AssistantConfirmationCardState.CANCELLED.eyebrowRes())
            .isEqualTo(R.string.assistant_confirmation_eyebrow_cancelled)
        assertThat(AssistantConfirmationCardState.CANCELLED.iconRes())
            .isEqualTo(R.drawable.ic_assistant_confirmation_cancelled)
    }

    @Test
    fun `given assistant cards, when card group segment is created, then cards are preserved`() {
        val card = orderCard()

        val segment: AssistantUiSegment = AssistantUiSegment.CardGroup(listOf(card))

        assertThat(segment).isEqualTo(AssistantUiSegment.CardGroup(listOf(card)))
    }

    @Test
    fun `given assistant message, when text and card group segments are used, then segment order is preserved`() {
        val card = orderCard()

        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(
                AssistantUiSegment.Text("Here is the order."),
                AssistantUiSegment.CardGroup(listOf(card)),
            ),
        )

        assertThat(message.segments).containsExactly(
            AssistantUiSegment.Text("Here is the order."),
            AssistantUiSegment.CardGroup(listOf(card)),
        )
    }

    @Test
    fun `given final card group before text, when ordering segments, then text renders before card group`() {
        val cardGroup = AssistantUiSegment.CardGroup(listOf(orderCard()))
        val text = AssistantUiSegment.Text("Here is the order.")
        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(cardGroup, text),
        )

        val orderedSegments = message.orderedSegments(AssistantUiState(status = AssistantUiStatus.IDLE))

        assertThat(orderedSegments).containsExactly(text, cardGroup)
    }

    @Test
    fun `given final text card group text, when ordering segments, then text renders before card group`() {
        val firstText = AssistantUiSegment.Text("Here are your matching orders.")
        val cardGroup = AssistantUiSegment.CardGroup(listOf(orderCard()))
        val secondText = AssistantUiSegment.Text("I found the most recent one first.")
        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(firstText, cardGroup, secondText),
        )

        val orderedSegments = message.orderedSegments(AssistantUiState(status = AssistantUiStatus.IDLE))

        assertThat(orderedSegments).containsExactly(firstText, secondText, cardGroup)
    }

    @Test
    fun `given final card-only message, when ordering segments, then card group remains visible`() {
        val cardGroup = AssistantUiSegment.CardGroup(listOf(orderCard()))
        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(cardGroup),
        )

        val orderedSegments = message.orderedSegments(AssistantUiState(status = AssistantUiStatus.IDLE))

        assertThat(orderedSegments).containsExactly(cardGroup)
    }

    @Test
    fun `given final text card group and confirmation, when ordering segments, then confirmation is not moved after cards`() {
        val text = AssistantUiSegment.Text("Review this change before applying it.")
        val cardGroup = AssistantUiSegment.CardGroup(listOf(orderCard()))
        val confirmationCard = AssistantUiSegment.ConfirmationCard(confirmationCard())
        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(text, cardGroup, confirmationCard),
        )

        val orderedSegments = message.orderedSegments(AssistantUiState(status = AssistantUiStatus.IDLE))

        assertThat(orderedSegments).containsExactly(text, confirmationCard, cardGroup)
    }

    @Test
    fun `given streaming assistant message with card group, when ordering segments, then card group is hidden`() {
        val text = AssistantUiSegment.Text("I found this order.")
        val cardGroup = AssistantUiSegment.CardGroup(listOf(orderCard()))
        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(text, cardGroup),
        )

        val orderedSegments = message.orderedSegments(
            AssistantUiState(
                status = AssistantUiStatus.STREAMING,
                activeAssistantMessageId = "message-1",
            )
        )

        assertThat(orderedSegments).containsExactly(text)
    }

    @Test
    fun `given non-streaming error and finished states, when ordering segments, then card groups are visible`() {
        val cardGroup = AssistantUiSegment.CardGroup(listOf(orderCard()))
        val text = AssistantUiSegment.Text("Here is the order.")
        val message = AssistantUiMessage(
            id = "message-1",
            role = AssistantUiMessage.Role.ASSISTANT,
            segments = listOf(cardGroup, text),
        )

        listOf(
            AssistantUiState(status = AssistantUiStatus.ERROR, activeAssistantMessageId = "message-1"),
            AssistantUiState(
                status = AssistantUiStatus.ERROR,
                error = AssistantUiError.CANCELLED,
                activeAssistantMessageId = "message-1",
            ),
            AssistantUiState(status = AssistantUiStatus.IDLE, activeAssistantMessageId = null),
        ).forEach { state ->
            assertThat(message.orderedSegments(state))
                .describedAs(state.status.name)
                .containsExactly(text, cardGroup)
        }
    }

    private fun orderCard() = AssistantCard.Order(
        remoteOrderId = 123L,
        number = "#1001",
        status = "processing",
        total = "12.34",
        currency = "USD",
        customerName = "Jane Doe",
        date = "2026-05-01T10:00:00Z",
    )

    private fun confirmationCard() = AssistantConfirmationCard(
        confirmationId = "confirmation-1",
        toolCall = ToolCall(
            id = "tool-call-1",
            name = "orders_update",
            arguments = buildJsonObject { },
        ),
        state = AssistantConfirmationCardState.PENDING,
    )
}
