package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ToolCallArgsTest {
    @Serializable
    private data class Args(
        @SerialName("search") val search: String? = null,
        @SerialName("limit") val limit: Int = 20,
    )

    @Serializable
    private data class RequiredArgs(@SerialName("id") val id: Long)

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `given valid arguments, when parseArgs is called, then a typed value is returned`() {
        val call = ToolCall(
            id = "1",
            name = "x",
            arguments = buildJsonObject {
                put("search", "alice")
                put("limit", 5)
            }
        )
        val result = call.parseArgs<Args>(json)
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrThrow()).isEqualTo(Args(search = "alice", limit = 5))
    }

    @Test
    fun `given missing optional arguments, when parseArgs is called, then defaults are used`() {
        val call = ToolCall(id = "1", name = "x", arguments = buildJsonObject { })
        val parsed = call.parseArgs<Args>(json).getOrThrow()
        assertThat(parsed.search).isNull()
        assertThat(parsed.limit).isEqualTo(20)
    }

    @Test
    fun `given a wrong type, when parseArgs is called, then a failure is returned`() {
        val call = ToolCall(
            id = "1",
            name = "x",
            arguments = buildJsonObject {
                put("limit", JsonPrimitive("not-a-number"))
            }
        )
        assertThat(call.parseArgs<Args>(json).isFailure).isTrue
    }

    @Test
    fun `given unknown keys, when parseArgs is called, then they are ignored`() {
        val call = ToolCall(
            id = "1",
            name = "x",
            arguments = buildJsonObject {
                put("search", "alice")
                put("extra", "ignored")
            }
        )
        assertThat(call.parseArgs<Args>(json).getOrThrow().search).isEqualTo("alice")
    }

    @Test
    fun `given a missing required field, when parseArgs is called, then a failure is returned`() {
        val call = ToolCall(id = "1", name = "x", arguments = buildJsonObject { })
        assertThat(call.parseArgs<RequiredArgs>(json).isFailure).isTrue()
    }
}
