package com.woocommerce.android.aiassistant.core.safety

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SafetyOrchestratorImplTest {
    @Test
    fun `given safe descriptor, when evaluated, then tool executes without pending confirmation`() = runTest {
        val orchestrator = orchestrator()
        val call = toolCall(name = "orders_list")

        val decision = orchestrator.evaluate(call, descriptor("orders_list", ToolSafetyLevel.SAFE))

        assertThat(decision).isEqualTo(SafetyDecision.Execute)
        assertThat(orchestrator.confirm("missing")).isFalse
    }

    @Test
    fun `given unsafe descriptor, when evaluated, then request preserves ids name safety and args`() = runTest {
        val orchestrator = orchestrator()
        val arguments = buildJsonObject {
            put("id", 42)
            put("status", "processing")
        }
        val call = toolCall(name = "orders_update", arguments = arguments)

        val decision = orchestrator.evaluate(call, descriptor("orders_update", ToolSafetyLevel.UNSAFE))

        val request = (decision as SafetyDecision.RequireConfirmation).request
        assertThat(request.id).isNotBlank
        assertThat(request.toolCallId).isEqualTo("call_1")
        assertThat(request.toolName).isEqualTo("orders_update")
        assertThat(request.arguments).isSameAs(arguments)
        assertThat(request.safetyLevel).isEqualTo(ToolSafetyLevel.UNSAFE)
    }

    @Test
    fun `given pending request, when confirmed by id, then await returns confirmed and pending is removed`() = runTest {
        val orchestrator = orchestrator()
        val decision = orchestrator.evaluate(
            toolCall(name = "orders_update"),
            descriptor("orders_update", ToolSafetyLevel.UNSAFE),
        ) as SafetyDecision.RequireConfirmation

        val awaited = async { orchestrator.awaitResult(decision.request.id) }
        runCurrent()
        val resolved = orchestrator.confirm(decision.request.id)

        assertThat(resolved).isTrue
        assertThat(awaited.await()).isEqualTo(
            ConfirmationResult(decision.request.id, ConfirmationDecision.CONFIRMED),
        )
        assertThat(orchestrator.confirm(decision.request.id)).isFalse
    }

    @Test
    fun `given pending request, when cancelled by id, then await returns cancelled and pending is removed`() = runTest {
        val orchestrator = orchestrator()
        val decision = orchestrator.evaluate(
            toolCall(name = "orders_update"),
            descriptor("orders_update", ToolSafetyLevel.UNSAFE),
        ) as SafetyDecision.RequireConfirmation

        val awaited = async { orchestrator.awaitResult(decision.request.id) }
        runCurrent()
        val resolved = orchestrator.cancel(decision.request.id)

        assertThat(resolved).isTrue
        assertThat(awaited.await()).isEqualTo(
            ConfirmationResult(decision.request.id, ConfirmationDecision.CANCELLED),
        )
        assertThat(orchestrator.cancel(decision.request.id)).isFalse
    }

    @Test
    fun `given request is confirmed before await starts, when awaited, then confirmed result is returned`() = runTest {
        val orchestrator = orchestrator()
        val decision = orchestrator.evaluate(
            toolCall(name = "orders_update"),
            descriptor("orders_update", ToolSafetyLevel.UNSAFE),
        ) as SafetyDecision.RequireConfirmation

        val resolved = orchestrator.confirm(decision.request.id)
        val result = orchestrator.awaitResult(decision.request.id)

        assertThat(resolved).isTrue
        assertThat(result).isEqualTo(
            ConfirmationResult(decision.request.id, ConfirmationDecision.CONFIRMED),
        )
        assertThat(orchestrator.confirm(decision.request.id)).isFalse
    }

    @Test
    fun `given pending request without awaiter, when pending is cancelled, then request is removed`() = runTest {
        val orchestrator = orchestrator()
        val decision = orchestrator.evaluate(
            toolCall(name = "orders_update"),
            descriptor("orders_update", ToolSafetyLevel.UNSAFE),
        ) as SafetyDecision.RequireConfirmation

        val cancelled = orchestrator.cancelPending(decision.request.id)
        val result = orchestrator.awaitResult(decision.request.id)

        assertThat(cancelled).isTrue
        assertThat(result).isEqualTo(
            ConfirmationResult(decision.request.id, ConfirmationDecision.CANCELLED),
        )
        assertThat(orchestrator.confirm(decision.request.id)).isFalse
    }

    @Test
    fun `given unknown request id, when resolved, then no pending request resumes`() = runTest {
        val orchestrator = orchestrator()

        val resolved = orchestrator.resolve(
            ConfirmationResult("missing", ConfirmationDecision.CONFIRMED),
        )

        assertThat(resolved).isFalse
    }

    @Test
    fun `when safety levels are inspected, then only safe and unsafe levels exist`() {
        assertThat(ToolSafetyLevel.entries).containsExactly(
            ToolSafetyLevel.SAFE,
            ToolSafetyLevel.UNSAFE,
        )
    }

    private fun toolCall(
        name: String,
        arguments: kotlinx.serialization.json.JsonObject = buildJsonObject { },
    ) = ToolCall(
        id = "call_1",
        name = name,
        arguments = arguments,
    )

    private fun descriptor(name: String, safetyLevel: ToolSafetyLevel) = ToolDescriptor(
        name = name,
        description = "test",
        inputSchema = buildJsonObject { },
        safetyLevel = safetyLevel,
    )

    private fun orchestrator() = SafetyOrchestratorImpl()
}
