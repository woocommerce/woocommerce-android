package com.woocommerce.android.aiassistant.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.runtime.AssistantPendingConfirmation
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Rule
import org.junit.Test

class AssistantChatScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun givenMessages_whenScreenRenders_thenThreadShowsUserAndAssistantBubbles() {
        composeTestRule.setContent {
            AssistantChatScreen(
                state = AssistantUiState(
                    messages = listOf(
                        AssistantUiMessage("user-1", AssistantUiMessage.Role.USER, "Show my orders"),
                        AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, "Here are your orders"),
                    )
                ),
                inputText = "",
                onInputTextChange = {},
                onSendMessage = {},
                onCancelTurn = {},
                onRetry = {},
                onConfirmWrite = {},
                onCancelWrite = {},
            )
        }

        composeTestRule.onNodeWithTag(AssistantChatTestTags.THREAD).assertIsDisplayed()
        composeTestRule.onNodeWithText("Show my orders").assertIsDisplayed()
        composeTestRule.onNodeWithText("Here are your orders").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Assistant status: Ready").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("You: Show my orders").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Assistant: Here are your orders").assertIsDisplayed()
    }

    @Test
    fun givenStreamingBubble_whenTextUpdates_thenSameAssistantBubbleRendersUpdatedText() {
        val state = mutableStateOf(
            AssistantUiState(
                messages = listOf(
                    AssistantUiMessage("user-1", AssistantUiMessage.Role.USER, "Summarize sales"),
                    AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, "Sales are"),
                ),
                status = AssistantUiStatus.STREAMING,
            )
        )
        composeTestRule.setContent {
            AssistantChatScreen(
                state = state.value,
                inputText = "",
                onInputTextChange = {},
                onSendMessage = {},
                onCancelTurn = {},
                onRetry = {},
                onConfirmWrite = {},
                onCancelWrite = {},
            )
        }

        state.value = state.value.copy(
            messages = listOf(
                AssistantUiMessage("user-1", AssistantUiMessage.Role.USER, "Summarize sales"),
                AssistantUiMessage("assistant-1", AssistantUiMessage.Role.ASSISTANT, "Sales are up today"),
            )
        )
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithTag(AssistantChatTestTags.message("assistant-1")).assertCountEquals(1)
        composeTestRule.onNodeWithText("Sales are up today").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Assistant status: Working").assertIsDisplayed()
        composeTestRule.onNodeWithText("Stop").assertIsDisplayed()
    }

    @Test
    fun givenEmptyInput_whenScreenRenders_thenSendButtonIsDisabled() {
        composeTestRule.setContent {
            AssistantChatScreen(
                state = AssistantUiState(),
                inputText = "",
                onInputTextChange = {},
                onSendMessage = {},
                onCancelTurn = {},
                onRetry = {},
                onConfirmWrite = {},
                onCancelWrite = {},
            )
        }

        composeTestRule.onNodeWithText("Send").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun givenNonEmptyInput_whenScreenRenders_thenSendButtonIsEnabled() {
        composeTestRule.setContent {
            AssistantChatScreen(
                state = AssistantUiState(),
                inputText = "Show today's orders",
                onInputTextChange = {},
                onSendMessage = {},
                onCancelTurn = {},
                onRetry = {},
                onConfirmWrite = {},
                onCancelWrite = {},
            )
        }

        composeTestRule.onNodeWithText("Send").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun givenPendingConfirmation_whenScreenRenders_thenConfirmationControlsAreShown() {
        composeTestRule.setContent {
            AssistantChatScreen(
                state = AssistantUiState(
                    status = AssistantUiStatus.AWAITING_CONFIRMATION,
                    pendingConfirmation = AssistantPendingConfirmation(
                        id = "confirmation-1",
                        toolCall = ToolCall(
                            id = "call-1",
                            name = "orders_update",
                            arguments = buildJsonObject { put("id", 123) },
                        ),
                    ),
                ),
                inputText = "",
                onInputTextChange = {},
                onSendMessage = {},
                onCancelTurn = {},
                onRetry = {},
                onConfirmWrite = {},
                onCancelWrite = {},
            )
        }

        composeTestRule.onNodeWithText("Confirm orders_update?").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Assistant status: Review required").assertIsDisplayed()
    }

    @Test
    fun givenErrorWithRetry_whenScreenRenders_thenErrorAndRetryAreShown() {
        composeTestRule.setContent {
            AssistantChatScreen(
                state = AssistantUiState(
                    status = AssistantUiStatus.ERROR,
                    error = AssistantUiError.NETWORK,
                    canRetry = true,
                ),
                inputText = "",
                onInputTextChange = {},
                onSendMessage = {},
                onCancelTurn = {},
                onRetry = {},
                onConfirmWrite = {},
                onCancelWrite = {},
            )
        }

        composeTestRule.onNodeWithText("Network error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Assistant status: Needs attention").assertIsDisplayed()
    }
}
