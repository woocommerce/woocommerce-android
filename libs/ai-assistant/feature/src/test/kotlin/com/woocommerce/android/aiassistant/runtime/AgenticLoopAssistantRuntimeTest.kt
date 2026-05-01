package com.woocommerce.android.aiassistant.runtime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantMessage
import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolRegistry
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import com.woocommerce.android.aiassistant.core.loop.AgenticLoop
import com.woocommerce.android.aiassistant.core.loop.CatalogSnapshot
import com.woocommerce.android.aiassistant.core.loop.LoopEvent
import com.woocommerce.android.aiassistant.core.loop.LoopOutcome
import com.woocommerce.android.aiassistant.core.loop.SessionContext
import com.woocommerce.android.aiassistant.core.loop.ToolCatalogSelector
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.core.safety.ConfirmationDecision
import com.woocommerce.android.aiassistant.core.safety.ConfirmationRequest
import com.woocommerce.android.aiassistant.core.safety.ConfirmationResult
import com.woocommerce.android.aiassistant.core.safety.SafetyDecision
import com.woocommerce.android.aiassistant.core.safety.SafetyOrchestrator
import com.woocommerce.android.aiassistant.safety.ConfirmationPreviewRenderer
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreview
import com.woocommerce.android.aiassistant.safety.RenderedConfirmationPreviewField
import com.woocommerce.android.aiassistant.safety.WooCommerceConfirmationPreviewBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AgenticLoopAssistantRuntimeTest {
    @Test
    fun `when agentic loop finishes with max iterations, then runtime preserves outcome`() = runTest {
        val updatedHistory = listOf(AssistantMessage.User("Hello"))
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(
                events = listOf(
                    LoopEvent.Finished(
                        outcome = LoopOutcome.MAX_ITERATIONS,
                        updatedHistory = updatedHistory,
                    )
                )
            ),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.Finished(
                outcome = LoopOutcome.MAX_ITERATIONS,
                updatedHistory = updatedHistory,
            )
        )
    }

    @Test
    fun `when loop requests confirmation, then runtime emits pending confirmation`() = runTest {
        val request = ConfirmationRequest(
            id = "confirmation-1",
            toolCallId = "call-1",
            toolName = "orders_update",
            arguments = buildJsonObject {
                put("id", 123)
                put("status", "processing")
            },
            safetyLevel = ToolSafetyLevel.UNSAFE,
        )
        val runtime = runtime(
            agenticLoop = FakeAgenticLoop(events = listOf(LoopEvent.ConfirmationRequested(request))),
        )

        val events = runtime.startTurn(givenTurnRequest()).toList()

        assertThat(events).containsExactly(
            AssistantRuntimeEvent.AwaitingConfirmation(
                AssistantPendingConfirmation(
                    id = "confirmation-1",
                    toolCall = ToolCall(
                        id = "call-1",
                        name = "orders_update",
                        arguments = buildJsonObject {
                            put("id", 123)
                            put("status", "processing")
                        },
                    ),
                    preview = RenderedConfirmationPreview(
                        message = "Set order #123 to processing (emails the customer)",
                        fields = listOf(
                            RenderedConfirmationPreviewField(
                                name = "status",
                                label = "Status",
                                value = "processing",
                            )
                        ),
                    ),
                )
            )
        )
    }

    @Test
    fun `when write is confirmed, then runtime resolves safety confirmation`() = runTest {
        val safetyOrchestrator = FakeSafetyOrchestrator()
        val runtime = runtime(safetyOrchestrator = safetyOrchestrator)

        val result = runtime.confirmWrite("confirmation-1")

        assertThat(result).isEqualTo(AssistantRuntimeConfirmationResult.Accepted)
        assertThat(safetyOrchestrator.results).containsExactly(
            ConfirmationResult("confirmation-1", ConfirmationDecision.CONFIRMED)
        )
    }

    @Test
    fun `when confirmation is missing, then runtime reports deferred confirmation`() = runTest {
        val runtime = runtime(safetyOrchestrator = FakeSafetyOrchestrator(resolveResult = false))

        val result = runtime.confirmWrite("missing")

        assertThat(result).isEqualTo(AssistantRuntimeConfirmationResult.Deferred)
    }

    @Test
    fun `when write is cancelled, then runtime cancels safety confirmation`() = runTest {
        val safetyOrchestrator = FakeSafetyOrchestrator()
        val runtime = runtime(safetyOrchestrator = safetyOrchestrator)

        runtime.cancelWrite("confirmation-1")

        assertThat(safetyOrchestrator.results).containsExactly(
            ConfirmationResult("confirmation-1", ConfirmationDecision.CANCELLED)
        )
    }

    private fun runtime(
        agenticLoop: AgenticLoop = FakeAgenticLoop(events = emptyList()),
        safetyOrchestrator: SafetyOrchestrator = FakeSafetyOrchestrator(),
    ) = AgenticLoopAssistantRuntime(
        agenticLoop = agenticLoop,
        toolRegistry = EmptyToolRegistry,
        toolCatalogSelector = PassThroughToolCatalogSelector,
        safetyOrchestrator = safetyOrchestrator,
        confirmationPreviewBuilder = WooCommerceConfirmationPreviewBuilder(),
        confirmationPreviewRenderer = ConfirmationPreviewRenderer(ApplicationProvider.getApplicationContext<Context>()),
    )

    private fun givenTurnRequest() = AssistantTurnRequest(
        conversationId = "conversation-1",
        siteId = 123L,
        toolScope = ToolScope.GLOBAL,
        userMessage = "Hello",
        history = emptyList(),
    )

    private class FakeAgenticLoop(
        private val events: List<LoopEvent>,
    ) : AgenticLoop {
        override fun runTurn(
            conversationId: String,
            userMessage: String,
            history: List<AssistantMessage>,
            context: SessionContext,
        ): Flow<LoopEvent> = flowOf(*events.toTypedArray())
    }

    private object EmptyToolRegistry : ToolRegistry {
        override fun descriptors(): List<ToolDescriptor> = emptyList()

        override suspend fun execute(call: ToolCall): ToolResult =
            error("Unexpected tool execution in runtime adapter test")
    }

    private object PassThroughToolCatalogSelector : ToolCatalogSelector {
        override fun select(scope: ToolScope, fullRegistry: List<ToolDescriptor>): CatalogSnapshot =
            CatalogSnapshot(scope = scope, tools = fullRegistry)
    }

    private class FakeSafetyOrchestrator(
        private val resolveResult: Boolean = true,
    ) : SafetyOrchestrator {
        val results = mutableListOf<ConfirmationResult>()

        override suspend fun evaluate(call: ToolCall, descriptor: ToolDescriptor): SafetyDecision =
            error("Unexpected safety evaluation in runtime adapter test")

        override suspend fun awaitResult(requestId: String): ConfirmationResult =
            error("Unexpected confirmation await in runtime adapter test")

        override fun resolve(result: ConfirmationResult): Boolean {
            results += result
            return resolveResult
        }

        override fun cancelPending(requestId: String): Boolean = false
    }
}
