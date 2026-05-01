package com.woocommerce.android.aiassistant.tools.handlers

import com.woocommerce.android.aiassistant.core.chat.ToolCall
import com.woocommerce.android.aiassistant.core.chat.ToolDescriptor
import com.woocommerce.android.aiassistant.core.chat.ToolResult
import com.woocommerce.android.aiassistant.core.chat.ToolSafetyLevel
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StubToolHandlerTest {

    @Test
    fun `given a stub handler, when execute is called, then ValidationError mentioning tool name is returned`() = runTest {
        val handler = object : StubToolHandler() {
            override val descriptor = ToolDescriptor(
                name = "sample_tool",
                description = "d",
                inputSchema = buildJsonObject { },
                safetyLevel = ToolSafetyLevel.SAFE,
            )
        }

        val result = handler.execute(ToolCall(id = "call_1", name = "sample_tool", arguments = buildJsonObject { }))

        assertThat(result).isInstanceOf(ToolResult.ValidationError::class.java)
        val error = result as ToolResult.ValidationError
        assertThat(error.toolCallId).isEqualTo("call_1")
        assertThat(error.reason).contains("sample_tool")
        assertThat(error.reason).contains("not yet implemented")
    }
}
