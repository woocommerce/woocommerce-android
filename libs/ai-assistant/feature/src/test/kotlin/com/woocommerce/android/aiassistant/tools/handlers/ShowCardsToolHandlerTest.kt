package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardFamily
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardPayload
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolution
import com.woocommerce.android.aiassistant.tools.handlers.cards.ShowCardsResolver
import com.woocommerce.android.aiassistant.tools.handlers.cards.ValidatedRef
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ShowCardsToolHandlerTest {
    private val json = Json
    private val handler = ShowCardsToolHandler()

    @Test
    fun `show cards descriptor accepts references with Android v1 order and product families`() {
        val descriptor = handler.descriptor

        assertThat(descriptor.name).isEqualTo("show_cards")
        assertThat(descriptor.description).contains("order")
        assertThat(descriptor.description).contains("product")
        assertThat(descriptor.inputSchema.toString()).contains("references")
        assertThat(descriptor.inputSchema.toString()).contains("family")
        assertThat(descriptor.inputSchema.toString()).contains("id")
        assertThat(descriptor.inputSchema.toString()).contains("order")
        assertThat(descriptor.inputSchema.toString()).contains("product")
    }

    @Test
    fun `success structured contains compact result counts and ref lists`() = runTest {
        val result = executeShowCards(
            handler = handlerWith(FakeResolver.resolving(orderCard(id = "123"))),
            argumentsJson = """{"references":[{"family":"order","id":"123"}]}"""
        )

        val structured = assertSuccess(result).structured.jsonObject

        assertThat(structured.keys).containsExactly(
            "requested",
            "validated",
            "rendered",
            "resolved_refs",
            "missing_refs",
            "rejected_refs"
        )
        assertThat(structured["requested"]?.jsonPrimitive?.int).isEqualTo(1)
        assertThat(structured["validated"]?.jsonPrimitive?.int).isEqualTo(1)
        assertThat(structured["rendered"]?.jsonPrimitive?.int).isEqualTo(1)
    }

    private fun handlerWith(resolver: ShowCardsResolver) =
        ShowCardsToolHandler(resolver)

    private suspend fun executeShowCards(
        handler: ShowCardsToolHandler,
        argumentsJson: String,
    ): ToolResult = handler.execute(
        ToolCall(
            id = "call_1",
            name = "show_cards",
            arguments = json.parseToJsonElement(argumentsJson).jsonObject,
        )
    )

    private fun assertSuccess(result: ToolResult): ToolResult.Success {
        assertThat(result).isInstanceOf(ToolResult.Success::class.java)
        return result as ToolResult.Success
    }

    private fun orderCard(id: String): ShowCardsResolution.Resolved {
        val ref = ValidatedRef(index = 0, family = ShowCardFamily.Order, id = id)

        return ShowCardsResolution.Resolved(
            ref = ref,
            summary = buildJsonObject {
                put("id", id)
            },
            card = ShowCardPayload(
                family = "order",
                id = id,
                title = "#$id",
            )
        )
    }

    private class FakeResolver(
        private val resolutions: List<ShowCardsResolution>,
    ) : ShowCardsResolver {
        override suspend fun resolve(refs: List<ValidatedRef>): List<ShowCardsResolution> =
            refs.map { ref -> resolutions.first { it.ref.family == ref.family && it.ref.id == ref.id } }

        companion object {
            fun resolving(vararg resolutions: ShowCardsResolution): FakeResolver =
                FakeResolver(resolutions.toList())
        }
    }
}
