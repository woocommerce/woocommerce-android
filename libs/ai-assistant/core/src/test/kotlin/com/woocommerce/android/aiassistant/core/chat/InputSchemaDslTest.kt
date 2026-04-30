package com.woocommerce.android.aiassistant.core.chat

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class InputSchemaDslTest {
    @Test
    fun `given a schema with one optional string, when building the schema, then the JSON shape is correct`() {
        val schema: JsonObject = inputSchema {
            string("search", description = "Filter by customer name")
        }
        assertThat(schema["type"]?.jsonPrimitive?.contentOrNull).isEqualTo("object")
        val props = requireNotNull(schema["properties"]).jsonObject
        val search = requireNotNull(props["search"]).jsonObject
        assertThat(search["type"]?.jsonPrimitive?.contentOrNull).isEqualTo("string")
        assertThat(search["description"]?.jsonPrimitive?.contentOrNull).isEqualTo("Filter by customer name")
        assertThat(schema["required"]).isNull()
    }

    @Test
    fun `given a required integer property, when building the schema, then it appears in the required array`() {
        val schema = inputSchema {
            integer("order_id", description = "Order ID", required = true)
        }
        val required = requireNotNull(schema["required"]).jsonArray.map { it.jsonPrimitive.content }
        assertThat(required).containsExactly("order_id")
    }

    @Test
    fun `given an enum property, when building the schema, then values are listed under enum`() {
        val schema = inputSchema {
            enum("status", values = listOf("pending", "completed"), description = "Status")
        }
        val status = requireNotNull(requireNotNull(schema["properties"]).jsonObject["status"]).jsonObject
        val values = requireNotNull(status["enum"]).jsonArray.map { it.jsonPrimitive.content }
        assertThat(values).containsExactly("pending", "completed")
    }

    @Test
    fun `given a boolean property, when building the schema, then type is boolean`() {
        val schema = inputSchema { boolean("force", description = "Skip cache") }
        val force = requireNotNull(requireNotNull(schema["properties"]).jsonObject["force"]).jsonObject
        assertThat(force["type"]?.jsonPrimitive?.content).isEqualTo("boolean")
    }

    @Test
    fun `given an array property, when building the schema, then type is array with item type`() {
        val schema = inputSchema { array("include", itemType = "integer", description = "IDs to include") }
        val include = requireNotNull(requireNotNull(schema["properties"]).jsonObject["include"]).jsonObject
        assertThat(include["type"]?.jsonPrimitive?.content).isEqualTo("array")
        assertThat(requireNotNull(include["items"]).jsonObject["type"]?.jsonPrimitive?.content).isEqualTo("integer")
    }

    @Test
    fun `given any schema, when building the schema, then additionalProperties is false`() {
        val schema = inputSchema { string("q", description = "query") }
        assertThat(schema["additionalProperties"]?.jsonPrimitive?.content).isEqualTo("false")
    }
}
