package com.woocommerce.android.aiassistant.core.loop

import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ToolCallAssemblerTest {
    private val assembler = ToolCallAssembler(assistantJsonForTests())

    @Test
    fun `given a single complete tool call, when assembled, then returns Success with correct ToolCall`() {
        val deltas = listOf(
            AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "list_orders", argumentsDelta = null),
            AssistantEvent.ToolCallDelta(index = 0, argumentsDelta = """{"status":"""),
            AssistantEvent.ToolCallDelta(index = 0, argumentsDelta = """"pending"}"""),
        )

        val results = assembler.assemble(deltas)

        assertThat(results).hasSize(1)
        val success = results[0] as ToolCallAssembler.AssemblyResult.Success
        assertThat(success.call.id).isEqualTo("call_1")
        assertThat(success.call.name).isEqualTo("list_orders")
        assertThat(success.call.arguments).isEqualTo(buildJsonObject { put("status", "pending") })
    }

    @Test
    fun `given two tool calls with different indices, when assembled, then returns two Success results`() {
        val deltas = listOf(
            AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "list_orders", argumentsDelta = "{}"),
            AssistantEvent.ToolCallDelta(index = 1, id = "call_2", name = "list_products", argumentsDelta = "{}"),
        )

        val results = assembler.assemble(deltas)

        assertThat(results).hasSize(2)
        assertThat(results.all { it is ToolCallAssembler.AssemblyResult.Success }).isTrue
    }

    @Test
    fun `given malformed JSON arguments, when assembled, then returns MalformedArguments`() {
        val deltas = listOf(
            AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "list_orders", argumentsDelta = "{broken"),
        )

        val results = assembler.assemble(deltas)

        assertThat(results).hasSize(1)
        val failure = results[0] as ToolCallAssembler.AssemblyResult.MalformedArguments
        assertThat(failure.callId).isEqualTo("call_1")
        assertThat(failure.toolName).isEqualTo("list_orders")
    }

    @Test
    fun `given no argumentsDelta at all, when assembled, then returns Success with empty JsonObject`() {
        val deltas = listOf(
            AssistantEvent.ToolCallDelta(index = 0, id = "call_1", name = "get_store_info", argumentsDelta = null),
        )

        val results = assembler.assemble(deltas)

        assertThat(results).hasSize(1)
        val success = results[0] as ToolCallAssembler.AssemblyResult.Success
        assertThat(success.call.arguments.isEmpty()).isTrue
    }

    @Test
    fun `given empty deltas list, when assembled, then returns empty list`() {
        val results = assembler.assemble(emptyList())
        assertThat(results).isEmpty()
    }

    @Test
    fun `given delta with missing id, when assembled, then returns MalformedArguments`() {
        val deltas = listOf(
            AssistantEvent.ToolCallDelta(index = 0, id = null, name = "list_orders", argumentsDelta = "{}"),
        )

        val results = assembler.assemble(deltas)

        assertThat(results).hasSize(1)
        assertThat(results[0]).isInstanceOf(ToolCallAssembler.AssemblyResult.MalformedArguments::class.java)
    }
}
